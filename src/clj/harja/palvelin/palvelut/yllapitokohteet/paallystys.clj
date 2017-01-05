(ns harja.palvelin.palvelut.yllapitokohteet.paallystys
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
            [harja.palvelin.palvelut.yllapitokohteet.yllapitokohteet :as yllapitokohteet]
            [harja.domain.yllapitokohteet :as yllapitokohteet-domain]))

(defn tyot-tyyppi-string->avain [json avainpolku]
  (-> json
      (assoc-in avainpolku
                (when-let [tyot (some-> json (get-in avainpolku))]
                  (map #(assoc % :tyyppi (keyword (:tyyppi %))) tyot)))))

(defn tyot-tyyppi-avain->string [json avainpolku]
  (-> json
      (assoc-in avainpolku
                (when-let [tyot (some-> json (get-in avainpolku))]
                  (map #(assoc % :tyyppi (name (:tyyppi %))) tyot)))))

(defn hae-urakan-paallystysilmoitukset [db user {:keys [urakka-id sopimus-id vuosi]}]
  (log/debug "Haetaan urakan päällystysilmoitukset. Urakka-id " urakka-id ", sopimus-id: " sopimus-id)
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kohdeluettelo-paallystysilmoitukset user urakka-id)
  (let [vastaus (into []
                      (comp
                        (map #(assoc % :tila (yllapitokohteet-domain/yllapitokohteen-tarkka-tila %)))
                        (map #(assoc % :tila-kartalla (yllapitokohteet-domain/yllapitokohteen-tila-kartalla %))))
                      (q/hae-urakan-paallystysilmoitukset-kohteineen db urakka-id sopimus-id vuosi))]
    (log/debug "Päällystysilmoitukset saatu: " (count vastaus) "kpl")
    vastaus))

(defn- lisaa-paallystysilmoitukseen-kohdeosien-tiedot [paallystysilmoitus]
  (-> paallystysilmoitus
      (assoc-in [:ilmoitustiedot :osoitteet]
                (->> paallystysilmoitus
                     :kohdeosat
                     (map (fn [kohdeosa]
                            ;; Lisää kohdeosan tietoihin päällystystoimenpiteen tiedot
                            (merge (clojure.set/rename-keys kohdeosa {:id :kohdeosa-id})
                                   (some
                                     (fn [paallystystoimenpide]
                                       (when (= (:id kohdeosa)
                                                (:kohdeosa-id paallystystoimenpide))
                                         paallystystoimenpide))
                                     (get-in paallystysilmoitus
                                             [:ilmoitustiedot :osoitteet])))))
                     (sort-by tierekisteri-domain/tiekohteiden-jarjestys)
                     vec))
      (dissoc :kohdeosat)))

(defn hae-urakan-paallystysilmoitus-paallystyskohteella
  "Hakee päällystysilmoituksen ja kohteen tiedot.

   Päällystysilmoituksen kohdeosien tiedot haetaan yllapitokohdeosa-taulusta ja liitetään mukaan ilmoitukseen.

   Huomaa, että vaikka päällystysilmoitusta ei olisi tehty, tämä kysely palauttaa joka tapauksessa
   kohteen tiedot ja esitäytetyn ilmoituksen, jossa kohdeosat on syötetty valmiiksi."
  [db user {:keys [urakka-id paallystyskohde-id]}]
  (log/debug "Haetaan urakan päällystysilmoitus, jonka päällystyskohde-id " paallystyskohde-id)
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kohdeluettelo-paallystysilmoitukset user urakka-id)
  (let [paallystysilmoitus (into []
                                 (comp (map konv/alaviiva->rakenne)
                                       (map #(konv/jsonb->clojuremap % :ilmoitustiedot))
                                       (map #(konv/string-poluista->keyword
                                              %
                                              [[:taloudellinen-osa :paatos]
                                               [:tekninen-osa :paatos]
                                               [:tila]])))
                                 (q/hae-paallystysilmoitus-kohdetietoineen-paallystyskohteella
                                   db
                                   {:paallystyskohde paallystyskohde-id}))
        paallystysilmoitus (first (konv/sarakkeet-vektoriin
                                    paallystysilmoitus
                                    {:kohdeosa :kohdeosat}
                                    :id))
        _ (when-let [ilmoitustiedot (:ilmoitustiedot paallystysilmoitus)]
            (skeema/validoi paallystysilmoitus-domain/+paallystysilmoitus+
                            ilmoitustiedot))
        paallystysilmoitus (tyot-tyyppi-string->avain paallystysilmoitus [:ilmoitustiedot :tyot])
        ;; Tyhjälle ilmoitukselle esitäytetään kohdeosat. Jos ilmoituksessa on tehty toimenpiteitä
        ;; kohdeosille, niihin liitetään kohdeosan tiedot, jotta voidaan muokata frontissa.
        paallystysilmoitus (lisaa-paallystysilmoitukseen-kohdeosien-tiedot paallystysilmoitus)

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


(defn- poista-ilmoitustiedoista-tieosoitteet
  "Poistaa päällystysilmoituksen ilmoitustiedoista sellaiset tiedot, jotka tallennetaan
   ylläpitokohdeosa-tauluun."
  [ilmoitustiedot]
  (let [paivitetyt-osoitteet (mapv
                               (fn [osoite]
                                 (-> osoite
                                     (dissoc :tr-kaista
                                             :tr-ajorata
                                             :tr-loppuosa
                                             :tunnus
                                             :tr-alkuosa
                                             :tr-loppuetaisyys
                                             :nimi
                                             :tr-alkuetaisyys
                                             :tr-numero
                                             :toimenpide)))
                               (:osoitteet ilmoitustiedot))]
    (assoc ilmoitustiedot :osoitteet paivitetyt-osoitteet)))

(defn- luo-paallystysilmoitus [db user urakka-id sopimus-id
                               {:keys [paallystyskohde-id ilmoitustiedot
                                       takuupvm]
                                :as paallystysilmoitus}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-kohdeluettelo-paallystysilmoitukset user urakka-id)
  (log/debug "Luodaan uusi päällystysilmoitus.")
  (let [muutoshinta (paallystysilmoitus-domain/laske-muutokset-kokonaishintaan (:tyot ilmoitustiedot))
        tila (paallystysilmoitus-domain/paattele-ilmoituksen-tila
               (:valmis-kasiteltavaksi paallystysilmoitus)
               (= (get-in paallystysilmoitus [:tekninen-osa :paatos]) :hyvaksytty)
               (= (get-in paallystysilmoitus [:taloudellinen-osa :paatos]) :hyvaksytty))
        ilmoitustiedot (-> ilmoitustiedot
                           (poista-ilmoitustiedoista-tieosoitteet)
                           (tyot-tyyppi-avain->string [:tyot]))
        _ (skeema/validoi paallystysilmoitus-domain/+paallystysilmoitus+
                          ilmoitustiedot)
        encoodattu-ilmoitustiedot (cheshire/encode ilmoitustiedot)]
    (log/debug "Asetetaan ilmoituksen tilaksi " tila)
    (log/debug "POT muutoshinta: " muutoshinta)
    (:id (q/luo-paallystysilmoitus<!
           db
           {:paallystyskohde paallystyskohde-id
            :tila tila
            :ilmoitustiedot encoodattu-ilmoitustiedot
            :takuupvm (konv/sql-date takuupvm)
            :muutoshinta muutoshinta
            :kayttaja (:id user)}))))

(defn- tarkista-paallystysilmoituksen-lukinta [paallystysilmoitus-kannassa]
  (log/debug "Tarkistetaan onko POT lukittu...")
  (if (= :lukittu (:tila paallystysilmoitus-kannassa))
    (do (log/debug "POT on lukittu, ei voi päivittää!")
        (throw (SecurityException. "Päällystysilmoitus on lukittu, ei voi päivittää!")))
    (log/debug "POT ei ole lukittu, vaan " (pr-str (:tila paallystysilmoitus-kannassa)))))

(defn- paivita-kasittelytiedot [db user urakka-id
                                {:keys [paallystyskohde-id
                                        tekninen-osa taloudellinen-osa]}]
  (if (oikeudet/on-muu-oikeus? "päätös" oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
                               urakka-id user)
    (do
      (log/debug "Päivitetään päällystysilmoituksen käsittelytiedot")
      (q/paivita-paallystysilmoituksen-kasittelytiedot<!
        db
        {:paatos_tekninen_osa (some-> tekninen-osa :paatos name)
         :paatos_taloudellinen_osa (some-> taloudellinen-osa :paatos name)
         :perustelu_tekninen_osa (:perustelu tekninen-osa)
         :perustelu_taloudellinen_osa (:perustelu taloudellinen-osa)
         :kasittelyaika_tekninen_osa (konv/sql-date (:kasittelyaika tekninen-osa))
         :kasittelyaika_taloudellinen_osa (konv/sql-date (:kasittelyaika taloudellinen-osa))
         :muokkaaja (:id user)
         :id paallystyskohde-id
         :urakka urakka-id}))
    (log/debug "Ei oikeutta päivittää päätöstä.")))

(defn- paivita-asiatarkastus [db user urakka-id
                              {:keys [paallystyskohde-id asiatarkastus]}]
  (let [{:keys [tarkastusaika tarkastaja tekninen-osa taloudellinen-osa lisatiedot]} asiatarkastus]
    (if (oikeudet/on-muu-oikeus? "asiatarkastus" oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
                                 urakka-id user)
      (do (log/debug "Päivitetään päällystysilmoituksen asiatarkastus: " asiatarkastus)
          (q/paivita-paallystysilmoituksen-asiatarkastus<!
            db
            {:asiatarkastus_pvm (konv/sql-date tarkastusaika)
             :asiatarkastus_tarkastaja tarkastaja
             :asiatarkastus_tekninen_osa tekninen-osa
             :asiatarkastus_taloudellinen_osa taloudellinen-osa
             :asiatarkastus_lisatiedot lisatiedot
             :muokkaaja (:id user)
             :id paallystyskohde-id
             :urakka urakka-id}))
      (log/debug "Ei oikeutta päivittää asiatarkastusta."))))

(defn- paivita-paallystysilmoituksen-perustiedot
  [db user urakka-id sopimus-id
   {:keys [id paallystyskohde-id ilmoitustiedot
           takuupvm
           tekninen-osa taloudellinen-osa] :as paallystysilmoitus}]
  (if (oikeudet/voi-kirjoittaa?
        oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
        urakka-id
        user)
    (do (log/debug "Päivitetään päällystysilmoituksen perustiedot")
        (let [muutoshinta (paallystysilmoitus-domain/laske-muutokset-kokonaishintaan
                            (:tyot ilmoitustiedot))
              tila (paallystysilmoitus-domain/paattele-ilmoituksen-tila
                     (:valmis-kasiteltavaksi paallystysilmoitus)
                     (= (get-in paallystysilmoitus [:tekninen-osa :paatos]) :hyvaksytty)
                     (= (get-in paallystysilmoitus [:taloudellinen-osa :paatos]) :hyvaksytty))
              ilmoitustiedot (-> ilmoitustiedot
                                 (poista-ilmoitustiedoista-tieosoitteet)
                                 (tyot-tyyppi-avain->string [:tyot]))
              _ (skeema/validoi paallystysilmoitus-domain/+paallystysilmoitus+
                                ilmoitustiedot)
              encoodattu-ilmoitustiedot (cheshire/encode ilmoitustiedot)]
          (log/debug "Encoodattu ilmoitustiedot: " (pr-str encoodattu-ilmoitustiedot))
          (log/debug "Asetetaan ilmoituksen tilaksi " tila)
          (log/debug "POT muutoshinta: " muutoshinta)
          (q/paivita-paallystysilmoitus<!
            db
            {:tila tila
             :ilmoitustiedot encoodattu-ilmoitustiedot
             :takuupvm (konv/sql-date takuupvm)
             :muutoshinta muutoshinta
             :muokkaaja (:id user)
             :id paallystyskohde-id
             :urakka urakka-id}))
        id)
    (log/debug "Ei oikeutta päivittää perustietoja.")))

(defn- paivita-paallystysilmoitus [db user urakka-id sopimus-id
                                   uusi-paallystysilmoitus paallystysilmoitus-kannassa]
  ;; Ilmoituksen kaikki tiedot lähetetään aina tallennettavaksi, vaikka käyttäjällä olisi oikeus
  ;; muokata vain tiettyä osaa ilmoituksesta. Frontissa on estettyä muokkaamasta sellaisia asioita, joita
  ;; käyttäjä ei saa muokata. Täällä ilmoitus päivitetään osa kerrallaan niin, että jokaista
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

(defn- lisaa-paallystysilmoitukseen-kohdeosien-idt [paallystysilmoitus paivitetyt-kohdeosat]
  (assert (not (empty? paivitetyt-kohdeosat)) "Ei voida liittää päällystysilmoitukseen tyhjiä kohdeosia")
  (-> paallystysilmoitus
      (assoc-in [:ilmoitustiedot :osoitteet]
                (into []
                      (keep
                        (fn [osoite]
                          (let [vastaava-kohdeosa
                                (first
                                  (filter #(and
                                            (= (:tr-numero %) (:tr-numero osoite))
                                            (= (:tr-alkuosa %) (:tr-alkuosa osoite))
                                            (= (:tr-alkuetaisyys %) (:tr-alkuetaisyys osoite))
                                            (= (:tr-loppuosa %) (:tr-loppuosa osoite))
                                            (= (:tr-loppuetaisyys %) (:tr-loppuetaisyys osoite)))
                                          paivitetyt-kohdeosat))]
                            ;; Jos osoitteelle ei ole kohdeosaa, se on poistettu
                            (when vastaava-kohdeosa
                              (assoc osoite :kohdeosa-id (:id vastaava-kohdeosa)))))
                        (get-in paallystysilmoitus [:ilmoitustiedot :osoitteet]))))))

(defn tallenna-paallystysilmoitus
  "Tallentaa päällystysilmoituksen tiedot kantaan.

  Päällystysilmoituksen kohdeosien tietoja ei tallenneta itse ilmoitukseen, vaan ne tallennetaan
  yllapitokohdeosa-tauluun."
  [db user {:keys [urakka-id sopimus-id paallystysilmoitus]}]
  (log/debug "Tallennetaan päällystysilmoitus: " paallystysilmoitus
             ". Urakka-id " urakka-id
             ", sopimus-id: " sopimus-id
             ", päällystyskohde-id:" (:paallystyskohde-id paallystysilmoitus))

  (log/debug "Aloitetaan päällystysilmoituksen tallennus")
  (jdbc/with-db-transaction [c db]
    (yha/lukitse-urakan-yha-sidonta db urakka-id)
    (let [paallystyskohde-id (:paallystyskohde-id paallystysilmoitus)
          paivitetyt-kohdeosat (yllapitokohteet/tallenna-yllapitokohdeosat
                                 db user {:urakka-id urakka-id :sopimus-id sopimus-id
                                          :yllapitokohde-id paallystyskohde-id
                                          :osat (map #(assoc % :id (:kohdeosa-id %))
                                                     (->> paallystysilmoitus
                                                          :ilmoitustiedot
                                                          :osoitteet
                                                          (filter (comp not :poistettu))))})
          paallystysilmoitus (lisaa-paallystysilmoitukseen-kohdeosien-idt paallystysilmoitus paivitetyt-kohdeosat)
          paallystysilmoitus-kannassa
          (first (into []
                       (comp (map #(konv/jsonb->clojuremap % :ilmoitustiedot))
                             (map #(tyot-tyyppi-string->avain % [:ilmoitustiedot :tyot]))
                             (map #(konv/string-poluista->keyword %
                                                                  [[:paatos :taloudellinen-osa]
                                                                   [:paatos :tekninen-osa]
                                                                   [:tila]])))
                       (q/hae-paallystysilmoitus-paallystyskohteella
                         db
                         {:paallystyskohde paallystyskohde-id})))]
      (let [paallystysilmoitus-id
            (if paallystysilmoitus-kannassa
              (paivita-paallystysilmoitus db user urakka-id sopimus-id paallystysilmoitus
                                          paallystysilmoitus-kannassa)
              (luo-paallystysilmoitus db user urakka-id sopimus-id paallystysilmoitus))]

        (tallenna-paallystysilmoituksen-kommentti db user paallystysilmoitus paallystysilmoitus-id)

        ;; FIXME: haun voisi irrottaa erilleen
        (let [uudet-ilmoitukset (hae-urakan-paallystysilmoitukset c user {:urakka-id urakka-id
                                                                          :sopimus-id sopimus-id})]
          (log/debug "Tallennus tehty, palautetaan uudet päällystysilmoitukset: "
                     (count uudet-ilmoitukset) " kpl")
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
