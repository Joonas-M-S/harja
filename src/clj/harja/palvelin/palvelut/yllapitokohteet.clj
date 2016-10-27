(ns harja.palvelin.palvelut.yllapitokohteet
  "Ylläpitokohteiden palvelut"
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [com.stuartsierra.component :as component]
            [harja.domain
             [oikeudet :as oikeudet]
             [skeema :refer [Toteuma validoi]]
             [tierekisteri :as tr]]
            [harja.geo :as geo]
            [harja.kyselyt
             [konversio :as konv]
             [yllapitokohteet :as q]]
            [harja.palvelin.komponentit.http-palvelin
             :refer
             [julkaise-palvelu poista-palvelut]]
            [harja.palvelin.palvelut.yha :as yha]
            [taoensso.timbre :as log]
            [hiccup.core :refer [html]]
            [harja.tyokalut.functor :refer [fmap]]
            [harja.kyselyt.tieverkko :as tieverkko]
            [harja.domain.tiemerkinta :as tm-domain]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.paallystys :as paallystys-q]
            [clj-time.coerce :as c]
            [harja.palvelin.komponentit.fim :as fim]
            [harja.palvelin.integraatiot.sahkoposti :as sahkoposti]
            [harja.fmt :as fmt]
            [harja.domain.tierekisteri :as tierekisteri]
            [harja.pvm :as pvm]
            [clojure.java.io :as io]
            [harja.tyokalut.html :as html-tyokalut])
  (:use [slingshot.slingshot :only [try+ throw+]]
        org.httpkit.fake))

(defn- tarkista-urakkatyypin-mukainen-kirjoitusoikeus [db user urakka-id]
  (let [urakan-tyyppi (:tyyppi (first (urakat-q/hae-urakan-tyyppi db urakka-id)))]
    (case urakan-tyyppi
      "paallystys"
      (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
      "paikkaus"
      (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id))))

(defn- tarkista-urakkatyypin-mukainen-lukuoikeus [db user urakka-id]
  (let [urakan-tyyppi (:tyyppi (first (urakat-q/hae-urakan-tyyppi db urakka-id)))]
    (case urakan-tyyppi
      "paallystys"
      (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kohdeluettelo-paallystyskohteet user urakka-id)
      "paikkaus"
      (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kohdeluettelo-paikkauskohteet user urakka-id))))

(defn vaadi-yllapitokohde-kuuluu-urakkaan [db urakka-id yllapitokohde]
  "Tarkistaa, että ylläpitokohde kuuluu annettuun urakkaan tai annettu urakka on merkitty
   suorittavaksi tiemerkintäurakakaksi. Jos kumpikaan ei ole totta, heittää poikkeuksen."
  (let [kohteen-urakka (:id (first (q/hae-yllapitokohteen-urakka-id db {:id yllapitokohde})))
        kohteen-suorittava-tiemerkintaurakka (:id (first (q/hae-yllapitokohteen-suorittava-tiemerkintaurakka-id
                                                           db
                                                           {:id yllapitokohde})))]
    (when (and (not= kohteen-urakka urakka-id)
               (not= kohteen-suorittava-tiemerkintaurakka urakka-id))
      (throw (SecurityException. (str "Ylläpitokohde " yllapitokohde " ei kuulu valittuun urakkaan "
                                      urakka-id " vaan urakkaan " kohteen-urakka
                                      ", eikä valittu urakka myöskään ole kohteen suorittava tiemerkintäurakka"))))))

(defn- laske-osien-pituudet
  "Hakee tieverkosta osien pituudet tielle"
  [db yllapitokohteet]
  (fmap
    (fn [osat]
      (let [tie (:tr-numero (first osat))
            osat (into #{}
                       (comp (mapcat (juxt :tr-alkuosa :tr-loppuosa))
                             (remove nil?))
                       osat)
            min-osa (reduce min 1 osat)
            max-osa (reduce max 1 osat)]
        (into {}
              (map (juxt :osa :pituus))
              (tieverkko/hae-osien-pituudet db tie min-osa max-osa))))
    (group-by :tr-numero yllapitokohteet)))

(defn hae-urakan-yllapitokohteet [db user {:keys [urakka-id sopimus-id]}]
  (tarkista-urakkatyypin-mukainen-lukuoikeus db user urakka-id)
  (log/debug "Haetaan urakan ylläpitokohteet.")
  (jdbc/with-db-transaction [db db]
    (let [vastaus (into []
                        (comp (map #(konv/string-polusta->keyword % [:paallystysilmoitus-tila]))
                              (map #(konv/string-polusta->keyword % [:paikkausilmoitus-tila]))
                              (map #(konv/string-polusta->keyword % [:yllapitokohdetyyppi]))
                              (map #(assoc % :kohdeosat
                                             (into []
                                                   paallystys-q/kohdeosa-xf
                                                   (q/hae-urakan-yllapitokohteen-yllapitokohdeosat
                                                     db {:urakka urakka-id
                                                         :sopimus sopimus-id
                                                         :yllapitokohde (:id %)})))))
                        (q/hae-urakan-sopimuksen-yllapitokohteet db {:urakka urakka-id
                                                                     :sopimus sopimus-id}))
          osien-pituudet-tielle (laske-osien-pituudet db vastaus)
          vastaus (mapv #(assoc %
                          :pituus
                          (tr/laske-tien-pituus (osien-pituudet-tielle (:tr-numero %)) %))
                        vastaus)]

      (log/debug "Ylläpitokohteet saatu: " (count vastaus) " kpl")
      vastaus)))

(defn hae-urakan-yllapitokohteet-lomakkeelle [db user {:keys [urakka-id sopimus-id]}]
  (tarkista-urakkatyypin-mukainen-lukuoikeus db user urakka-id)
  (log/debug "Haetaan urakan ylläpitokohteet laatupoikkeamalomakkeelle")
  (jdbc/with-db-transaction [db db]
    (let [vastaus (q/hae-urakan-yllapitokohteet-lomakkeelle db {:urakka urakka-id
                                                                :sopimus sopimus-id})]
      (log/debug "Ylläpitokohteet saatu: " (count vastaus) " kpl")
      vastaus)))

(defn hae-urakan-yllapitokohdeosat [db user {:keys [urakka-id sopimus-id yllapitokohde-id]}]
  (log/debug "Haetaan urakan ylläpitokohdeosat. Urakka-id " urakka-id ", sopimus-id: " sopimus-id ", yllapitokohde-id: " yllapitokohde-id)
  (tarkista-urakkatyypin-mukainen-lukuoikeus db user urakka-id)
  (let [vastaus (into []
                      paallystys-q/kohdeosa-xf
                      (q/hae-urakan-yllapitokohteen-yllapitokohdeosat db {:urakka urakka-id
                                                                          :sopimus sopimus-id
                                                                          :yllapitokohde yllapitokohde-id}))]
    (log/debug "Ylläpitokohdeosat saatu: " (pr-str vastaus))
    vastaus))

(defn- hae-urakkatyyppi [db urakka-id]
  (keyword (:tyyppi (first (q/hae-urakan-tyyppi db {:urakka urakka-id})))))

(defn hae-urakan-aikataulu [db user {:keys [urakka-id sopimus-id]}]
  (assert (and urakka-id sopimus-id) "anna urakka-id ja sopimus-id")
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Haetaan aikataulutiedot urakalle: " urakka-id)
  (jdbc/with-db-transaction [db db]
    (case (hae-urakkatyyppi db urakka-id)
      :paallystys
      (q/hae-paallystysurakan-aikataulu db {:urakka urakka-id :sopimus sopimus-id})
      :tiemerkinta
      (q/hae-tiemerkintaurakan-aikataulu db {:suorittava_tiemerkintaurakka urakka-id}))))

(defn hae-tiemerkinnan-suorittavat-urakat [db user {:keys [urakka-id]}]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Haetaan tiemerkinnän suorittavat urakat.")
  (q/hae-tiemerkinnan-suorittavat-urakat db))

(defn hae-tiemerkinnan-yksikkohintaiset-tyot [db user {:keys [urakka-id]}]
  (assert urakka-id "anna urakka-id")
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-suunnittelu-yksikkohintaisettyot user urakka-id)
  (log/debug "Haetaan yksikköhintaiset työt tiemerkintäurakalle: " urakka-id)
  (jdbc/with-db-transaction [db db]
    (into []
          (map #(konv/string->keyword % :hintatyyppi))
          (q/hae-tiemerkintaurakan-yksikkohintaiset-tyot db {:suorittava_tiemerkintaurakka urakka-id}))))

(defn tallenna-tiemerkinnan-yksikkohintaiset-tyot [db user {:keys [urakka-id kohteet]}]
  (assert urakka-id "anna urakka-id")
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-suunnittelu-yksikkohintaisettyot user urakka-id)
  (log/debug "Tallennetaan yksikköhintaiset työt " kohteet " tiemerkintäurakalle: " urakka-id)
  (jdbc/with-db-transaction [db db]
    (doseq [{:keys [hinta hintatyyppi muutospvm id] :as kohde} kohteet]
      (if-let [tiedot (first (q/hae-yllapitokohteen-tiemerkintaurakan-yksikkohintaiset-tyot
                               db
                               {:yllapitokohde id}))]
        (q/paivita-tiemerkintaurakan-yksikkohintaiset-tyot<! db {:hinta hinta
                                                                 :hintatyyppi (when hintatyyppi (name hintatyyppi))
                                                                 :muutospvm muutospvm
                                                                 :yllapitokohde id})
        (q/luo-tiemerkintaurakan-yksikkohintaiset-tyot<! db {:hinta hinta
                                                             :hintatyyppi (when hintatyyppi (name hintatyyppi))
                                                             :muutospvm muutospvm
                                                             :yllapitokohde id})))
    (hae-tiemerkinnan-yksikkohintaiset-tyot db user {:urakka-id urakka-id})))

(defn- tiemerkinnan-sahkopostiviesti [{:keys [paallystysurakka-nimi kohde-nimi kohde-osoite
                                              kohde-valmis-tiemerkintaan-pvm ilmoittaja
                                              tiemerkintaurakka-nimi] :as tiedot}]
  (html
    [:div
     [:p (format "Kohde '%s' on ilmoitettu olevan valmis tiemerkintään %s."
                 (or kohde-nimi (tierekisteri/tierekisteriosoite-tekstina kohde-osoite))
                 (fmt/pvm kohde-valmis-tiemerkintaan-pvm))]
     (html-tyokalut/taulukko [["Kohde" kohde-nimi]
                              ["TR-osoite" (tierekisteri/tierekisteriosoite-tekstina
                                             kohde-osoite
                                             {:teksti-tie? false})]
                              ["Valmis tiemerkintään" (fmt/pvm kohde-valmis-tiemerkintaan-pvm)]
                              ["Tiemerkinnän suorittaja" tiemerkintaurakka-nimi]
                              ["Ilmoittaja" ilmoittaja]
                              ["Ilmoittajan urakka" paallystysurakka-nimi]])]))

(defn- laheta-sahkoposti-tiemerkitsijoille [{:keys [fim email
                                                    paallystysurakka-nimi kohde-nimi kohde-osoite
                                                    kohde-valmis-tiemerkintaan-pvm ilmoittaja
                                                    tiemerkintaurakka-id tiemerkintaurakka-nimi
                                                    tiemerkintaurakka-sampo-id] :as tiedot}]
  (let [ilmoituksen-saajat (fim/hae-urakan-kayttajat-jotka-roolissa
                             fim
                             tiemerkintaurakka-sampo-id
                             #{"ely urakanvalvoja" "urakan vastuuhenkilö"})]
    (if-not (empty? ilmoituksen-saajat)
      (doseq [henkilo ilmoituksen-saajat]
        (sahkoposti/laheta-viesti!
          email
          (sahkoposti/vastausosoite email)
          (:sahkoposti henkilo)
          (format "Harja: Kohteen '%s' tiemerkinnän voi aloittaa %s"
                  (or kohde-nimi (tierekisteri/tierekisteriosoite-tekstina kohde-osoite))
                  (fmt/pvm kohde-valmis-tiemerkintaan-pvm))
          (tiemerkinnan-sahkopostiviesti {:paallystysurakka-nimi paallystysurakka-nimi
                                          :kohde-nimi kohde-nimi
                                          :kohde-osoite kohde-osoite
                                          :kohde-valmis-tiemerkintaan-pvm kohde-valmis-tiemerkintaan-pvm
                                          :ilmoittaja ilmoittaja
                                          :tiemerkintaurakka-nimi tiemerkintaurakka-nimi})))
      (log/warn (format "Tiemerkintäurakalle %s ei löydy FIM:stä henkiöä, jolle ilmoittaa kohteen valmiudesta tiemerkintään."
                        tiemerkintaurakka-id)))))

(defn merkitse-kohde-valmiiksi-tiemerkintaan
  "Merkitsee kohteen valmiiksi tiemerkintään annettuna päivämääränä.
   Palauttaa päivitetyt kohteet aikataulunäkymään"
  [db fim email user
   {:keys [urakka-id sopimus-id tiemerkintapvm kohde-id] :as tiedot}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Merkitään urakan " urakka-id " kohde " kohde-id " valmiiksi tiemerkintää päivämäärällä " tiemerkintapvm)
  (let [{:keys [kohde-nimi tr-numero tr-alkuosa tr-alkuetaisyys tr-loppuosa tr-loppuetaisyys
                tiemerkintaurakka-sampo-id paallystysurakka-nimi
                tiemerkintaurakka-id tiemerkintaurakka-nimi]}
        (first (q/hae-kohteen-tiedot-sahkopostilahetykseen
                 db
                 {:id kohde-id}))]
    (jdbc/with-db-transaction [db db]
      (q/merkitse-kohde-valmiiksi-tiemerkintaan<!
        db
        {:valmis_tiemerkintaan tiemerkintapvm
         :aikataulu_tiemerkinta_takaraja (-> tiemerkintapvm
                                             (c/from-date)
                                             tm-domain/tiemerkinta-oltava-valmis
                                             (c/to-date))
         :id kohde-id
         :urakka urakka-id}))

    (try+
      (laheta-sahkoposti-tiemerkitsijoille {:fim fim
                                            :email email
                                            :paallystysurakka-nimi paallystysurakka-nimi
                                            :kohde-nimi kohde-nimi
                                            :ilmoittaja (str (:etunimi user) " " (:sukunimi user)
                                                             (when-let [puhelin (:puhelin user)]
                                                               (str " (" puhelin ")")))
                                            :kohde-osoite {:tr-numero tr-numero
                                                           :tr-alkuosa tr-alkuosa
                                                           :tr-alkuetaisyys tr-alkuetaisyys
                                                           :tr-loppuosa tr-loppuosa
                                                           :tr-loppuetaisyys tr-loppuetaisyys}
                                            :kohde-valmis-tiemerkintaan-pvm tiemerkintapvm
                                            :tiemerkintaurakka-id tiemerkintaurakka-id
                                            :tiemerkintaurakka-sampo-id tiemerkintaurakka-sampo-id
                                            :tiemerkintaurakka-nimi tiemerkintaurakka-nimi})
      (catch Object e
        (log/error (format "Päällystysurakoitsija merkitsi kohteen %s valmiiksi, mutta sähköpostia ei voitu lähettää urakan %s tiemerkitsijälle: %s %s"
                           kohde-id
                           tiemerkintaurakka-id
                           e
                           (.printStackTrace e)))))

    (hae-urakan-aikataulu db user {:urakka-id urakka-id
                                   :sopimus-id sopimus-id})))

(defn tallenna-yllapitokohteiden-aikataulu [db user {:keys [urakka-id sopimus-id kohteet]}]
  (assert (and urakka-id sopimus-id kohteet) "anna urakka-id ja sopimus-id ja kohteet")
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-aikataulu user urakka-id)
  (log/debug "Tallennetaan urakan " urakka-id " ylläpitokohteiden aikataulutiedot: " kohteet)
  ;; Oma päivityskysely kullekin urakalle, sillä päällystysurakoitsija ja tiemerkkari
  ;; eivät saa muokata samoja asioita
  (let [voi-tallentaa-tiemerkinnan-takarajan?
        (oikeudet/on-muu-oikeus? "TM-valmis"
                                 oikeudet/urakat-aikataulu
                                 urakka-id
                                 user)]
    (jdbc/with-db-transaction [db db]
      (case (hae-urakkatyyppi db urakka-id)
        :paallystys
        (doseq [rivi kohteet]
          (q/tallenna-paallystyskohteen-aikataulu!
            db
            {:aikataulu_paallystys_alku (:aikataulu-paallystys-alku rivi)
             :aikataulu_paallystys_loppu (:aikataulu-paallystys-loppu rivi)
             :aikataulu_kohde_valmis (:aikataulu-kohde-valmis rivi)
             :aikataulu_muokkaaja (:id user)
             :suorittava_tiemerkintaurakka (:suorittava-tiemerkintaurakka rivi)
             :id (:id rivi)
             :urakka urakka-id})
          (when voi-tallentaa-tiemerkinnan-takarajan?
            (q/tallenna-yllapitokohteen-valmis-viimeistaan-paallystysurakasta!
              db
              {:aikataulu_tiemerkinta_takaraja (:aikataulu-tiemerkinta-takaraja rivi)
               :id (:id rivi)
               :urakka urakka-id})))
        :tiemerkinta
        (doseq [rivi kohteet]
          (q/tallenna-tiemerkintakohteen-aikataulu!
            db
            {:aikataulu_tiemerkinta_alku (:aikataulu-tiemerkinta-alku rivi)
             :aikataulu_tiemerkinta_loppu (:aikataulu-tiemerkinta-loppu rivi)
             :aikataulu_muokkaaja (:id user)
             :id (:id rivi)
             :urakka urakka-id})
          (when voi-tallentaa-tiemerkinnan-takarajan?
            (q/tallenna-yllapitokohteen-valmis-viimeistaan-tiemerkintaurakasta!
              db
              {:aikataulu_tiemerkinta_takaraja (:aikataulu-tiemerkinta-takaraja rivi)
               :id (:id rivi)
               :urakka urakka-id}))))
      (hae-urakan-aikataulu db user {:urakka-id urakka-id
                                     :sopimus-id sopimus-id}))))

(defn- luo-uusi-yllapitokohde [db user urakka-id sopimus-id
                               {:keys [kohdenumero nimi
                                       tr-numero tr-alkuosa tr-alkuetaisyys
                                       tr-loppuosa tr-loppuetaisyys tr-ajorata tr-kaista
                                       yllapitoluokka yllapitokohdetyyppi yllapitokohdetyotyyppi
                                       sopimuksen-mukaiset-tyot arvonvahennykset bitumi-indeksi
                                       kaasuindeksi poistettu nykyinen-paallyste
                                       keskimaarainen-vuorokausiliikenne
                                       indeksin-kuvaus]}]
  (log/debug "Luodaan uusi ylläpitokohde tyyppiä " yllapitokohdetyotyyppi)
  (when-not poistettu
    (q/luo-yllapitokohde<! db
                           {:urakka urakka-id
                            :sopimus sopimus-id
                            :kohdenumero kohdenumero
                            :nimi nimi
                            :tr_numero tr-numero
                            :tr_alkuosa tr-alkuosa
                            :tr_alkuetaisyys tr-alkuetaisyys
                            :tr_loppuosa tr-loppuosa
                            :tr_loppuetaisyys tr-loppuetaisyys
                            :tr_ajorata tr-ajorata
                            :tr_kaista tr-kaista
                            :keskimaarainen_vuorokausiliikenne keskimaarainen-vuorokausiliikenne
                            :yllapitoluokka yllapitoluokka
                            :nykyinen_paallyste nykyinen-paallyste
                            :sopimuksen_mukaiset_tyot sopimuksen-mukaiset-tyot
                            :arvonvahennykset arvonvahennykset
                            :bitumi_indeksi bitumi-indeksi
                            :kaasuindeksi kaasuindeksi
                            :yllapitokohdetyyppi (when yllapitokohdetyyppi (name yllapitokohdetyyppi))
                            :yllapitokohdetyotyyppi (when yllapitokohdetyotyyppi (name yllapitokohdetyotyyppi))
                            :indeksin_kuvaus indeksin-kuvaus})))

(defn- paivita-yllapitokohde [db user urakka-id
                              {:keys [id kohdenumero nimi
                                      tr-numero tr-alkuosa tr-alkuetaisyys
                                      tr-loppuosa tr-loppuetaisyys tr-ajorata tr-kaista
                                      yllapitoluokka
                                      sopimuksen-mukaiset-tyot
                                      arvonvahennykset bitumi-indeksi kaasuindeksi
                                      nykyinen-paallyste keskimaarainen-vuorokausiliikenne
                                      indeksin-kuvaus poistettu]}]
  (if poistettu
    (do (log/debug "Tarkistetaan onko ylläpitokohteella ilmoituksia")
        (let [paallystysilmoitus (q/onko-olemassa-paallystysilmoitus? db id)
              paikkausilmoitus (q/onko-olemassa-paikkausilmioitus? db id)]
          (log/debug "Vastaus päällystysilmoitus: " paallystysilmoitus)
          (log/debug "Vastaus paikkausilmoitus: " paikkausilmoitus)
          (if (or paallystysilmoitus paikkausilmoitus)
            (log/debug "Ei voi poistaa, ylläpitokohteelle on kirjattu ilmoituksia!")
            (do
              (log/debug "Ilmoituksia ei löytynyt, poistetaan ylläpitokohde")
              (q/poista-yllapitokohde! db {:id id :urakka urakka-id})))))
    (do (log/debug "Päivitetään ylläpitokohde")
        (q/paivita-yllapitokohde! db
                                  {:kohdenumero kohdenumero
                                   :nimi nimi
                                   :tr_numero tr-numero
                                   :tr_alkuosa tr-alkuosa
                                   :tr_alkuetaisyys tr-alkuetaisyys
                                   :tr_loppuosa tr-loppuosa
                                   :tr_loppuetaisyys tr-loppuetaisyys
                                   :tr_ajorata tr-ajorata
                                   :tr_kaista tr-kaista
                                   :keskimaarainen_vuorokausiliikenne keskimaarainen-vuorokausiliikenne
                                   :yllapitoluokka yllapitoluokka
                                   :nykyinen_paallyste nykyinen-paallyste
                                   :sopimuksen_mukaiset_tyot sopimuksen-mukaiset-tyot
                                   :arvonvanhennykset arvonvahennykset
                                   :bitumi_indeksi bitumi-indeksi
                                   :kaasuindeksi kaasuindeksi
                                   :indeksin_kuvaus indeksin-kuvaus
                                   :id id
                                   :urakka urakka-id}))))

(defn tallenna-yllapitokohteet [db user {:keys [urakka-id sopimus-id kohteet]}]
  (tarkista-urakkatyypin-mukainen-kirjoitusoikeus db user urakka-id)
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)
    (log/debug "Tallennetaan ylläpitokohteet: " (pr-str kohteet))
    (doseq [kohde kohteet]
      (log/debug (str "Käsitellään saapunut ylläpitokohde: " kohde))
      (if (and (:id kohde) (not (neg? (:id kohde))))
        (paivita-yllapitokohde c user urakka-id kohde)
        (luo-uusi-yllapitokohde c user urakka-id sopimus-id kohde)))
    (let [paallystyskohteet (hae-urakan-yllapitokohteet c user {:urakka-id urakka-id
                                                                :sopimus-id sopimus-id})]
      (log/debug "Tallennus suoritettu. Tuoreet ylläpitokohteet: " (pr-str paallystyskohteet))
      paallystyskohteet)))

(defn- luo-uusi-yllapitokohdeosa [db user yllapitokohde-id
                                  {:keys [nimi tunnus tr-numero tr-alkuosa tr-alkuetaisyys tr-loppuosa
                                          tr-loppuetaisyys tr-ajorata tr-kaista toimenpide poistettu sijainti]}]
  (log/debug "Luodaan uusi ylläpitokohdeosa, jonka ylläpitokohde-id: " yllapitokohde-id)
  (when-not poistettu
    (q/luo-yllapitokohdeosa<! db
                              {:yllapitokohde yllapitokohde-id
                               :nimi nimi
                               :tunnus tunnus
                               :tr_numero tr-numero
                               :tr_alkuosa tr-alkuosa
                               :tr_alkuetaisyys tr-alkuetaisyys
                               :tr_loppuosa tr-loppuosa
                               :tr_loppuetaisyys tr-loppuetaisyys
                               :tr_ajorata tr-ajorata
                               :tr_kaista tr-kaista
                               :toimenpide toimenpide})))

(defn- paivita-yllapitokohdeosa [db user urakka-id
                                 {:keys [id nimi tunnus tr-numero tr-alkuosa tr-alkuetaisyys
                                         tr-loppuosa tr-loppuetaisyys tr-ajorata
                                         tr-kaista toimenpide sijainti] :as kohdeosa}]

  (log/debug "Päivitetään ylläpitokohdeosa")
  (q/paivita-yllapitokohdeosa<! db
                                {:nimi nimi
                                 :tunnus tunnus
                                 :tr_numero tr-numero
                                 :tr_alkuosa tr-alkuosa
                                 :tr_alkuetaisyys tr-alkuetaisyys
                                 :tr_loppuosa tr-loppuosa
                                 :tr_loppuetaisyys tr-loppuetaisyys
                                 :tr_ajorata tr-ajorata
                                 :tr_kaista tr-kaista
                                 :toimenpide toimenpide
                                 :id id
                                 :urakka urakka-id}))

(defn tallenna-yllapitokohdeosat
  "Tallentaa ylläpitokohdeosat kantaan.
   Tarkistaa, tuleeko kohdeosat päivittää, poistaa vai luoda uutena.
   Palauttaa kohteen päivittyneet kohdeosat."
  [db user {:keys [urakka-id sopimus-id yllapitokohde-id osat]}]
  (tarkista-urakkatyypin-mukainen-kirjoitusoikeus db user urakka-id)
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)

    (log/debug "SAIN OSAT: " osat)
    (let [hae-osat #(hae-urakan-yllapitokohdeosat c user
                                                  {:urakka-id urakka-id
                                                   :sopimus-id sopimus-id
                                                   :yllapitokohde-id yllapitokohde-id})
          vanhat-osa-idt (into #{}
                               (map :id)
                               (hae-osat))
          uudet-osa-idt (into #{}
                              (keep :id)
                              osat)
          poistuneet-osa-idt (set/difference vanhat-osa-idt uudet-osa-idt)]

      (doseq [id poistuneet-osa-idt]
        (q/poista-yllapitokohdeosa! c {:urakka urakka-id
                                       :id id}))

      (log/debug "Tallennetaan ylläpitokohdeosat: " (pr-str osat) " Ylläpitokohde-id: " yllapitokohde-id)
      (doseq [osa osat]
        (if (and (:id osa) (not (neg? (:id osa))))
          (paivita-yllapitokohdeosa c user urakka-id osa)
          (luo-uusi-yllapitokohdeosa c user yllapitokohde-id osa)))
      (yha/paivita-yllapitourakan-geometriat c urakka-id)
      (let [yllapitokohdeosat (hae-osat)]
        (log/debug "Tallennus suoritettu. Tuoreet ylläpitokohdeosat: " (pr-str yllapitokohdeosat))
        (sort-by tr/tiekohteiden-jarjestys yllapitokohdeosat)))))


(defrecord Yllapitokohteet []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)
          fim (:fim this)
          email (:sonja-sahkoposti this)]
      (julkaise-palvelu http :urakan-yllapitokohteet
                        (fn [user tiedot]
                          (hae-urakan-yllapitokohteet db user tiedot)))
      (julkaise-palvelu http :urakan-yllapitokohteet-lomakkeelle
                        (fn [user tiedot]
                          (hae-urakan-yllapitokohteet-lomakkeelle db user tiedot)))
      (julkaise-palvelu http :urakan-yllapitokohdeosat
                        (fn [user tiedot]
                          (hae-urakan-yllapitokohdeosat db user tiedot)))
      (julkaise-palvelu http :tallenna-yllapitokohteet
                        (fn [user tiedot]
                          (tallenna-yllapitokohteet db user tiedot)))
      (julkaise-palvelu http :tallenna-yllapitokohdeosat
                        (fn [user tiedot]
                          (tallenna-yllapitokohdeosat db user tiedot)))
      (julkaise-palvelu http :hae-aikataulut
                        (fn [user tiedot]
                          (hae-urakan-aikataulu db user tiedot)))
      (julkaise-palvelu http :hae-tiemerkinnan-suorittavat-urakat
                        (fn [user tiedot]
                          (hae-tiemerkinnan-suorittavat-urakat db user tiedot)))
      (julkaise-palvelu http :tallenna-yllapitokohteiden-aikataulu
                        (fn [user tiedot]
                          (tallenna-yllapitokohteiden-aikataulu db user tiedot)))
      (julkaise-palvelu http :merkitse-kohde-valmiiksi-tiemerkintaan
                        (fn [user tiedot]
                          (merkitse-kohde-valmiiksi-tiemerkintaan db fim email user tiedot)))
      (julkaise-palvelu http :hae-tiemerkinnan-yksikkohintaiset-tyot
                        (fn [user tiedot]
                          (hae-tiemerkinnan-yksikkohintaiset-tyot db user tiedot)))
      (julkaise-palvelu http :tallenna-tiemerkinnan-yksikkohintaiset-tyot
                        (fn [user tiedot]
                          (tallenna-tiemerkinnan-yksikkohintaiset-tyot db user tiedot)))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-yllapitokohteet
      :urakan-yllapitokohdeosat
      :tallenna-yllapitokohteet
      :tallenna-yllapitokohdeosat
      :hae-aikataulut
      :tallenna-yllapitokohteiden-aikataulu
      :hae-tiemerkinnan-yksikkohintaiset-tyot
      :tallenna-tiemerkinnan-yksikkohintaiset-tyot)
    this))
