(ns harja.palvelin.palvelut.paallystys
  "Päällystyksen palvelut"
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.konversio :as konv]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [harja.domain.skeema :refer [Toteuma validoi]]
            [clojure.java.jdbc :as jdbc]
            [harja.kyselyt.kommentit :as kommentit]
            [harja.domain.paallystysilmoitus :as paallystysilmoitus-domain]
            [harja.kyselyt.paallystys :as q]
            [cheshire.core :as cheshire]
            [harja.palvelin.palvelut.yha :as yha]
            [harja.domain.skeema :as skeema]
            [harja.domain.tierekisteri :as tierekisteri-domain]
            [harja.domain.oikeudet :as oikeudet]
            [harja.palvelin.palvelut.yllapitokohteet :as yllapitokohteet]))

(defn tyot-tyyppi-string->avain [json avainpolku]
  (-> json
      (assoc-in avainpolku
                (when-let [tyot (some-> json (get-in avainpolku))]
                  (map #(assoc % :tyyppi (keyword (:tyyppi %))) tyot)))))

(defn hae-urakan-paallystysilmoitukset [db user {:keys [urakka-id sopimus-id]}]
  (log/debug "Haetaan urakan päällystysilmoitukset. Urakka-id " urakka-id ", sopimus-id: " sopimus-id)
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kohdeluettelo-paallystysilmoitukset user urakka-id)
  (let [vastaus (q/hae-urakan-paallystysilmoitukset-kohteineen db urakka-id sopimus-id)]
    (log/debug "Päällystysilmoitukset saatu: " (count vastaus) "kpl")
    vastaus))

(defn hae-urakan-paallystysilmoitus-paallystyskohteella
  "Hakee päällystysilmoituksen ja kohteen tiedot.

   Päällystysilmoituksen kohdeosien tiedot haetaan yllapitokohteet-taulusta ja liitetään mukaan ilmoitukseen.
   Jos kohdeosalle löytyy myös toimenpidetiedot päällystysilmoituksesta, myös ne liitetään mukaan.

   Huomaa, että vaikka päällystysilmoitusta ei olisi tehty, tämä kysely palauttaa joka tapauksessa
   kohteen tiedot ja esitäytetyn ilmoituksen, jossa kohdeosat on syötetty valmiiksi."
  [db user {:keys [urakka-id paallystyskohde-id]}]
  (log/debug "Haetaan urakan päällystysilmoitus, jonka päällystyskohde-id " paallystyskohde-id)
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kohdeluettelo-paallystysilmoitukset user urakka-id)
  (let [paallystysilmoitus (into []
                                 (comp (map konv/alaviiva->rakenne)
                                       (map #(konv/jsonb->clojuremap % :ilmoitustiedot))
                                       (map #(tyot-tyyppi-string->avain % [:ilmoitustiedot :tyot]))
                                       (map #(konv/string-poluista->keyword % [[:paatos-taloudellinen-osa]
                                                                               [:paatos-tekninen-osa]
                                                                               [:tila]])))
                                 (q/hae-urakan-paallystysilmoitus-paallystyskohteella
                                   db
                                   {:paallystyskohde paallystyskohde-id}))
        paallystysilmoitus (first (konv/sarakkeet-vektoriin
                                    paallystysilmoitus
                                    {:kohdeosa :kohdeosat}
                                    :id))
        ;; Tyhjälle ilmoitukselle esitäytetään kohdeosat. Jos ilmoituksessa on tehty toimenpiteitä
        ;; kohdeosille, niihin liitetään kohdeosan tiedot, jotta voidaan muokata frontissa.
        paallystysilmoitus (-> paallystysilmoitus
                               (assoc-in
                                 [:ilmoitustiedot :osoitteet]
                                 (mapv
                                   (fn [kohdeosa]
                                     ;; Lisää kohdeosan tietoihin päällystystoimenpiteen tiedot
                                     (merge (clojure.set/rename-keys kohdeosa {:id :kohdeosa-id})
                                            (some
                                              (fn [paallystystoimenpide]
                                                (when (= (:id kohdeosa) (:kohdeosa-id paallystystoimenpide))
                                                  paallystystoimenpide))
                                              (get-in paallystysilmoitus [:ilmoitustiedot :osoitteet]))))
                                   (sort-by tierekisteri-domain/tiekohteiden-jarjestys (:kohdeosat paallystysilmoitus))))
                               (dissoc :kohdeosat))
        kokonaishinta (reduce + (keep paallystysilmoitus [:sopimuksen-mukaiset-tyot
                                                          :arvonvahennykset
                                                          :bitumi-indeksi
                                                          :kaasuindeksi]))]
    (log/debug "Päällystysilmoitus kasattu: " (pr-str paallystysilmoitus))
    (log/debug "Haetaan kommentit...")
    (let [kommentit (into []
                          (comp (map konv/alaviiva->rakenne)
                                (map (fn [{:keys [liite] :as kommentti}]
                                       (if (:id
                                             liite)
                                         kommentti
                                         (dissoc kommentti :liite)))))
                          (q/hae-paallystysilmoituksen-kommentit db {:id (:id paallystysilmoitus)}))]
      (log/debug "Kommentit saatu: " kommentit)
      (assoc paallystysilmoitus
        :kokonaishinta kokonaishinta
        :paallystyskohde-id paallystyskohde-id
        :kommentit kommentit))))

(defn- kasittele-paallystysilmoituksen-tierekisterikohteet
  "Ottaa päällystysilmoituksen ilmoitustiedot.
   Päivittää päällystyskohteen kohdeosat niin, että niiden tiedot ovat samat kuin päällystysilmoituslomakkeessa.
   Palauttaa ilmoitustiedot, jossa päällystystoimenpiteiltä on riisuttu tieosoitteet."
  [db user urakka-id sopimus-id yllapitokohde-id ilmoitustiedot]
  (log/debug "Käsitellään ilmoituksen kohdeosat")
  (let [uudet-osoitteet (into []
                              (keep
                                (fn [osoite]
                                  (log/debug "Käsitellään POT-lomakkeen TR-osoite: " (pr-str osoite))
                                  (let [kohdeosa-kannassa
                                        (yllapitokohteet/tallenna-yllapitokohdeosa
                                          db
                                          user
                                          {:urakka-id urakka-id
                                           :sopimus-id sopimus-id
                                           :yllapitokohde-id yllapitokohde-id
                                           :osa {:id (:kohdeosa-id osoite)
                                                 :nimi (:nimi osoite)
                                                 :tunnus (:tunnus osoite)
                                                 :tr-numero (:tie osoite)
                                                 :tr-alkuosa (:aosa osoite)
                                                 :tr-alkuetaisyys (:aet osoite)
                                                 :tr-loppuosa (:losa osoite)
                                                 :tr-loppuetaisyys (:let osoite)
                                                 :tr-ajorata (:ajorata osoite)
                                                 :tr-kaista (:kaista osoite)
                                                 :poistettu (:poistettu osoite)
                                                 :sijainti (:sijainti osoite)}})
                                        _ (log/debug "Kohdeosan tiedot päivitetty omaan tauluun. Uusi kohdeosa kannassa: " (pr-str kohdeosa-kannassa))]
                                    (cond-> osoite
                                            true
                                            (dissoc :nimi :tunnus :tie :aosa :aet :losa :let :pituus :poistettu :ajorata :kaista)
                                            (some? kohdeosa-kannassa)
                                            (assoc :kohdeosa-id (:id kohdeosa-kannassa)))))
                                (:osoitteet ilmoitustiedot)))
        uudet-ilmoitustiedot (assoc ilmoitustiedot :osoitteet uudet-osoitteet)]
    (log/debug "Uudet ilmoitustiedot: " (pr-str uudet-ilmoitustiedot))
    uudet-ilmoitustiedot))

(defn- paivita-paallystysilmoituksen-perustiedot
  [db user urakka-id sopimus-id
   {:keys [id paallystyskohde-id ilmoitustiedot aloituspvm valmispvm-kohde
           valmispvm-paallystys takuupvm
           paatos-tekninen-osa paatos-taloudellinen-osa] :as paallystysilmoitus}]
  (if (oikeudet/voi-kirjoittaa?
        oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
        urakka-id
        user)
    (do (log/debug "Päivitetään päällystysilmoituksen perustiedot")
        (let [ilmoitustiedot (kasittele-paallystysilmoituksen-tierekisterikohteet db
                                                                                  user
                                                                                  urakka-id
                                                                                  sopimus-id
                                                                                  paallystyskohde-id
                                                                                  ilmoitustiedot)
              muutoshinta (paallystysilmoitus-domain/laske-muutokset-kokonaishintaan (:tyot ilmoitustiedot))
              tila (if (and (= paatos-tekninen-osa :hyvaksytty)
                            (= paatos-taloudellinen-osa :hyvaksytty))
                     "lukittu"
                     (if (and valmispvm-kohde valmispvm-paallystys) "valmis" "aloitettu"))
              encoodattu-ilmoitustiedot (cheshire/encode ilmoitustiedot)]
          (log/debug "Encoodattu ilmoitustiedot: " (pr-str encoodattu-ilmoitustiedot))
          (log/debug "Asetetaan ilmoituksen tilaksi " tila)
          (log/debug "POT muutoshinta: " muutoshinta)
          (q/paivita-paallystysilmoitus<!
            db
            {:tila tila
             :ilmoitustiedot encoodattu-ilmoitustiedot
             :aloituspvm (konv/sql-date aloituspvm)
             :valmispvm_kohde (konv/sql-date valmispvm-kohde)
             :valmispvm_paallystys (konv/sql-date valmispvm-paallystys)
             :takuupvm (konv/sql-date takuupvm)
             :muutoshinta muutoshinta
             :muokkaaja (:id user)
             :id paallystyskohde-id
             :urakka urakka-id}))
        id)
    (log/debug "Ei oikeutta päivittää perustietoja.")))

(defn- luo-paallystysilmoitus [db user urakka-id sopimus-id
                               {:keys [paallystyskohde-id ilmoitustiedot aloituspvm
                                       valmispvm-kohde valmispvm-paallystys
                                       takuupvm] :as paallystysilmoitus}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-kohdeluettelo-paallystysilmoitukset user urakka-id)
  (log/debug "Luodaan uusi päällystysilmoitus.")
  (let [ilmoitustiedot (kasittele-paallystysilmoituksen-tierekisterikohteet db
                                                                            user
                                                                            urakka-id
                                                                            sopimus-id
                                                                            paallystyskohde-id
                                                                            ilmoitustiedot)
        muutoshinta (paallystysilmoitus-domain/laske-muutokset-kokonaishintaan (:tyot ilmoitustiedot))
        tila (if (and valmispvm-kohde valmispvm-paallystys) "valmis" "aloitettu")
        encoodattu-ilmoitustiedot (cheshire/encode ilmoitustiedot)]
    (log/debug "Asetetaan ilmoituksen tilaksi " tila)
    (log/debug "POT muutoshinta: " muutoshinta)
    (:id (q/luo-paallystysilmoitus<!
           db
           {:paallystyskohde paallystyskohde-id
            :tila tila
            :ilmoitustiedot encoodattu-ilmoitustiedot
            :aloituspvm (konv/sql-date aloituspvm)
            :valmispvm_kohde (konv/sql-date valmispvm-kohde)
            :valmispvm_paallystys (konv/sql-date valmispvm-paallystys)
            :takuupvm (konv/sql-date takuupvm)
            :muutoshinta muutoshinta
            :kayttaja (:id user)}))))

(defn- tarkista-paallystysilmoituksen-lukinta [paallystysilmoitus-kannassa]
  (log/debug "Tarkistetaan onko POT lukittu...")
  (if (= :lukittu (:tila paallystysilmoitus-kannassa))
    (do (log/debug "POT on lukittu, ei voi päivittää!")
        (throw (RuntimeException. "Päällystysilmoitus on lukittu, ei voi päivittää!")))
    (log/debug "POT ei ole lukittu, vaan " (pr-str (:tila paallystysilmoitus-kannassa)))))

(defn- paivita-kasittelytiedot [db user urakka-id
                                {:keys [paallystyskohde-id
                                        paatos-tekninen-osa paatos-taloudellinen-osa perustelu-tekninen-osa
                                        perustelu-taloudellinen-osa kasittelyaika-tekninen-osa
                                        kasittelyaika-taloudellinen-osa] :as uusi-paallystysilmoitus}]
  (if (oikeudet/on-muu-oikeus? "päätös" oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
                               urakka-id user)
    (do
      (log/debug "Päivitetään päällystysilmoituksen käsittelytiedot")
      (q/paivita-paallystysilmoituksen-kasittelytiedot<!
        db
        {:paatos_tekninen_osa (if paatos-tekninen-osa (name paatos-tekninen-osa))
         :paatos_taloudellinen_osa (if paatos-taloudellinen-osa (name paatos-taloudellinen-osa))
         :perustelu_tekninen_osa perustelu-tekninen-osa
         :perustelu_taloudellinen_osa perustelu-taloudellinen-osa
         :kasittelyaika_tekninen_osa (konv/sql-date kasittelyaika-tekninen-osa)
         :kasittelyaika_taloudellinen_osa (konv/sql-date kasittelyaika-taloudellinen-osa)
         :muokkaaja (:id user)
         :id paallystyskohde-id
         :urakka urakka-id}))
    (log/debug "Ei oikeutta päivittää päätöstä.")))

(defn- paivita-asiatarkastus [db user urakka-id
                              {:keys [paallystyskohde-id
                                      asiatarkastus-tarkastusaika asiatarkastus-tarkastaja
                                      asiatarkastus-tekninen-osa asiatarkastus-taloudellinen-osa
                                      asiatarkastus-lisatiedot] :as uusi-paallystysilmoitus}]
  (if (oikeudet/on-muu-oikeus? "asiatarkastus" oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
                               urakka-id user)
    (do (log/debug "Päivitetään päällystysilmoituksen asiatarkastus")
        (q/paivita-paallystysilmoituksen-asiatarkastus<!
          db
          {:asiatarkastus_pvm (konv/sql-date asiatarkastus-tarkastusaika)
           :asiatarkastus_tarkastaja asiatarkastus-tarkastaja
           :asiatarkastus_tekninen_osa asiatarkastus-tekninen-osa
           :asiatarkastus_taloudellinen_osa asiatarkastus-taloudellinen-osa
           :asiatarkastus_lisatiedot asiatarkastus-lisatiedot
           :muokkaaja (:id user)
           :id paallystyskohde-id
           :urakka urakka-id}))
    (log/debug "Ei oikeutta päivittää asiatarkastusta.")))

(defn- paivita-paallystysilmoitus [db user urakka-id sopimus-id
                                   uusi-paallystysilmoitus paallystysilmoitus-kannassa]
  ;; Ilmoituksen kaikki tiedot lähetetään aina tallennettavaksi, vaikka käyttäjällä olisi oikeus
  ;; muokata vain tiettyä osaa ilmoituksesta. Näin ollen ilmoitus päivitetään osa kerrallaan niin, että jokaista
  ;; osaa vasten tarkistetaan tallennusoikeus.
  (log/debug "Päivitetään olemassa oleva päällystysilmoitus")
  (tarkista-paallystysilmoituksen-lukinta paallystysilmoitus-kannassa)
  (paivita-kasittelytiedot db user urakka-id uusi-paallystysilmoitus)
  (paivita-asiatarkastus db user urakka-id uusi-paallystysilmoitus)
  (paivita-paallystysilmoituksen-perustiedot db user urakka-id sopimus-id uusi-paallystysilmoitus)
  (log/debug "Päällystysilmoitus päivitetty!")
  (:id paallystysilmoitus-kannassa))

(defn tallenna-paallystysilmoituksen-kommentti [db user uusi-paallystysilmoitus paallystysilmoitus-id]
  (when-let [uusi-kommentti (:uusi-kommentti uusi-paallystysilmoitus)]
    (log/info "Tallennetaan uusi kommentti: " uusi-kommentti)
    (let [kommentti (kommentit/luo-kommentti<! db
                                               nil
                                               (:kommentti uusi-kommentti)
                                               nil
                                               (:id user))]
      (q/liita-kommentti<! db {:paallystysilmoitus paallystysilmoitus-id
                               :kommentti (:id kommentti)}))))

(defn tallenna-paallystysilmoitus
  "Tallentaa päällystysilmoituksen tiedot kantaan.

  Päällystysilmoituksen kohdeosien tietoja ei tallenneta itse ilmoitukseen, vaan ne päivitetään
  yllapitokohdeosa-tauluun. Tästä syystä kohdeosien tiedot poistetaan ilmoituksesta ennen tallennusta."
  [db user {:keys [urakka-id sopimus-id paallystysilmoitus]}]
  (log/debug "Tallennetaan päällystysilmoitus: " paallystysilmoitus
             ". Urakka-id " urakka-id
             ", sopimus-id: " sopimus-id
             ", päällystyskohde-id:" (:paallystyskohde-id paallystysilmoitus))

  (log/debug "Aloitetaan päällystysilmoituksen tallennus")
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)
    (skeema/validoi paallystysilmoitus-domain/+paallystysilmoitus+ (:ilmoitustiedot paallystysilmoitus))

    (let [paallystyskohde-id (:paallystyskohde-id paallystysilmoitus)
          paallystysilmoitus-kannassa (first (into []
                                                   (comp (map #(konv/jsonb->clojuremap % :ilmoitustiedot))
                                                         (map #(tyot-tyyppi-string->avain % [:ilmoitustiedot :tyot]))
                                                         (map #(konv/string-poluista->keyword % [[:paatos-taloudellinen-osa]
                                                                                                 [:paatos-tekninen-osa]
                                                                                                 [:tila]])))
                                                   (q/hae-paallystysilmoitus-paallystyskohteella
                                                     db
                                                     {:paallystyskohde paallystyskohde-id})))]
      (let [paallystysilmoitus-id
            (if paallystysilmoitus-kannassa
              (paivita-paallystysilmoitus db user urakka-id sopimus-id paallystysilmoitus paallystysilmoitus-kannassa)
              (luo-paallystysilmoitus db user urakka-id sopimus-id paallystysilmoitus))]

        (tallenna-paallystysilmoituksen-kommentti db user paallystysilmoitus paallystysilmoitus-id)
        (let [uudet-ilmoitukset (hae-urakan-paallystysilmoitukset c user {:urakka-id urakka-id
                                                                          :sopimus-id sopimus-id})]
          (log/debug "Tallennus tehty, palautetaan uudet päällystysilmoitukset: " (count uudet-ilmoitukset) " kpl")
          uudet-ilmoitukset)))))

(defrecord Paallystys []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)]
      (julkaise-palvelu http :urakan-paallystysilmoitukset
                        (fn [user tiedot]
                          (hae-urakan-paallystysilmoitukset db user tiedot)))
      (julkaise-palvelu http :urakan-paallystysilmoitus-paallystyskohteella
                        (fn [user tiedot]
                          (hae-urakan-paallystysilmoitus-paallystyskohteella db user tiedot)))
      (julkaise-palvelu http :tallenna-paallystysilmoitus
                        (fn [user tiedot]
                          (tallenna-paallystysilmoitus db user tiedot)))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-paallystysilmoitukset
      :urakan-paallystysilmoitus-paallystyskohteella
      :tallenna-paallystysilmoitus
      :tallenna-paallystyskohteet)
    this))
