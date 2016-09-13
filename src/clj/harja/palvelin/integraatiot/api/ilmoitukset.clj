(ns harja.palvelin.integraatiot.api.ilmoitukset
  "Ilmoitusten haku ja ilmoitustoimenpiteiden kirjaus"
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [with-channel on-close send!]]
            [compojure.core :refer [PUT GET]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer
             [kasittele-kutsu lokita-kutsu lokita-vastaus tee-vastaus aja-virhekasittelyn-kanssa hae-kayttaja tee-kirjausvastauksen-body]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.palvelin.integraatiot.api.tyokalut.ilmoitusnotifikaatiot :as notifikaatiot]
            [harja.palvelin.integraatiot.api.tyokalut.json :refer [aika-string->java-sql-date]]
            [harja.kyselyt.ilmoitukset :as ilmoitukset]
            [harja.kyselyt.konversio :as konversio]
            [harja.palvelin.integraatiot.api.sanomat.ilmoitus-sanomat :as sanomat]
            [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet]
            [harja.palvelin.integraatiot.api.tyokalut.parametrit :as parametrit]
            [harja.palvelin.integraatiot.tloik.tloik-komponentti :as tloik]
            [harja.tyokalut.json-validointi :as json])
  (:use [slingshot.slingshot :only [throw+]]))

(defn hae-ilmoituksen-id [db ilmoitusid]
  (if-let [id (:id (first (ilmoitukset/hae-id-ilmoitus-idlla db ilmoitusid)))]
    id
    (virheet/heita-viallinen-apikutsu-poikkeus
      {:koodi :tuntematon-ilmoitus
       :viesti (format "Ilmoitus id:llä %s. ei löydy ilmoitusta." ilmoitusid)})))

(defn tee-onnistunut-ilmoitustoimenpidevastaus []
  (tee-kirjausvastauksen-body {:ilmoitukset "Ilmoitustoimenpide kirjattu onnistuneesti"}))

(defn luo-ilmoitustoimenpide [db id ilmoitusid ilmoitustoimenpide ilmoittaja kasittelija kuittaustyyppi vapaateksti]
  (:id (ilmoitukset/luo-ilmoitustoimenpide<!
         db
         {:ilmoitus id
          :ilmoitusid ilmoitusid
          :kuitattu (aika-string->java-sql-date (:aika ilmoitustoimenpide))
          :vakiofraasi nil
          :vapaateksti vapaateksti
          :kuittaustyyppi kuittaustyyppi
          :kuittaaja_henkilo_etunimi (get-in ilmoittaja [:henkilo :etunimi])
          :kuittaaja_henkilo_sukunimi (get-in ilmoittaja [:henkilo :sukunimi])
          :kuittaaja_henkilo_matkapuhelin (get-in ilmoittaja [:henkilo :matkapuhelin])
          :kuittaaja_henkilo_tyopuhelin (get-in ilmoittaja [:henkilo :tyopuhelin])
          :kuittaaja_henkilo_sahkoposti (get-in ilmoittaja [:henkilo :sahkoposti])
          :kuittaaja_organisaatio_nimi (get-in ilmoittaja [:organisaatio :nimi])
          :kuittaaja_organisaatio_ytunnus (get-in ilmoittaja [:organisaatio :ytunnus])
          :kasittelija_henkilo_etunimi (get-in kasittelija [:henkilo :etunimi])
          :kasittelija_henkilo_sukunimi (get-in kasittelija [:henkilo :sukunimi])
          :kasittelija_henkilo_matkapuhelin (get-in kasittelija [:henkilo :matkapuhelin])
          :kasittelija_henkilo_tyopuhelin (get-in kasittelija [:henkilo :tyopuhelin])
          :kasittelija_henkilo_sahkoposti (get-in kasittelija [:henkilo :sahkoposti])
          :kasittelija_organisaatio_nimi (get-in kasittelija [:organisaatio :nimi])
          :kasittelija_organisaatio_ytunnus (get-in kasittelija [:organisaatio :ytunnus])})))

(defn kirjaa-ilmoitustoimenpide [db tloik parametrit
                                 {{:keys [tyyppi vapaateksti ilmoittaja kasittelija] :as ilmoitustoimenpide}
                                  :ilmoitustoimenpide}]
  (let [ilmoitusid (Integer/parseInt (:id parametrit))
        id (hae-ilmoituksen-id db ilmoitusid)
        _ (log/debug (format "Kirjataan toimenpide ilmoitukselle, jonka id on: %s ja ilmoitusid on: %s" id ilmoitusid))]

    (when (and (= tyyppi :aloitus) (not (ilmoitukset/ilmoitukselle-olemassa-vastaanottokuittaus? db ilmoitusid)))
      (let [aloitus-kuittaus-id (luo-ilmoitustoimenpide db id ilmoitusid ilmoitustoimenpide ilmoittaja kasittelija
                                                        :vastaanotto "Vastaanotettu")]
        (tloik/laheta-ilmoitustoimenpide tloik aloitus-kuittaus-id)))

    (let [kuittaus-id (luo-ilmoitustoimenpide db id ilmoitusid ilmoitustoimenpide ilmoittaja kasittelija tyyppi
                                              vapaateksti)]
      (tloik/laheta-ilmoitustoimenpide tloik kuittaus-id))

    (tee-onnistunut-ilmoitustoimenpidevastaus)))

(defn ilmoituslahettaja [integraatioloki tapahtumat kanava tapahtuma-id sulje-lahetyksen-jalkeen?]
  (fn [ilmoitukset]
    (send! kanava
           (aja-virhekasittelyn-kanssa
             "laheta-ilmoitus"
             (fn []
               (let [data {:ilmoitukset (mapv (fn [ilmoitus] (sanomat/rakenna-ilmoitus (konversio/alaviiva->rakenne ilmoitus))) ilmoitukset)}
                     vastaus (tee-vastaus json-skeemat/ilmoitusten-haku data)]
                 (lokita-vastaus integraatioloki :hae-ilmoitukset vastaus tapahtuma-id)
                 (doseq [ilmoitus ilmoitukset]
                   (notifikaatiot/ilmoita-lahetetysta-ilmoituksesta tapahtumat (:ilmoitusid ilmoitus) :api))
                 vastaus)))
           sulje-lahetyksen-jalkeen?)))

(defn pura-ilmoitusten-kuuntelun-kutsuparametrit [request]
  (let [{urakka-id :id
         muuttunut-jalkeen "muuttunutJalkeen"
         odota-uusia "odotaUusia"
         sulje-vastauksen-jalkeen "suljeVastauksenJalkeen"} (:params request)
        odota-uusia? (if odota-uusia
                       (Boolean/valueOf odota-uusia)
                       false)]
    {:urakka-id (when urakka-id (Integer/parseInt urakka-id))
     :muuttunut-jalkeen (when muuttunut-jalkeen (parametrit/pvm-aika muuttunut-jalkeen))
     :odota-uusia? odota-uusia?
     :sulje-vastauksen-jalkeen? (if sulje-vastauksen-jalkeen
                                  (and odota-uusia? (not (Boolean/valueOf sulje-vastauksen-jalkeen)))
                                  true)}))

(defn kaynnista-ilmoitusten-kuuntelu [db integraatioloki tapahtumat request]
  (aja-virhekasittelyn-kanssa
    "hae-ilmoitukset"
    (fn []
      (let [{urakka-id :urakka-id
             muuttunut-jalkeen :muuttunut-jalkeen
             odota-uusia? :odota-uusia?
             sulje-vastauksen-jalkeen? :sulje-vastauksen-jalkeen?} (pura-ilmoitusten-kuuntelun-kutsuparametrit request)
            tapahtuma-id (lokita-kutsu integraatioloki :hae-ilmoitukset request nil)
            kayttaja (hae-kayttaja db (get (:headers request) "oam_remote_user"))]
        (log/debug (format "Käynnistetään ilmoitusten kuuntelu urakalle id: %s. Muutosaika: %s." urakka-id muuttunut-jalkeen))
        (validointi/tarkista-urakka-ja-kayttaja db urakka-id kayttaja)
        (with-channel request kanava
                      (on-close kanava
                                (fn [_]
                                  (log/debug (format "Suljetaan urakan id: %s ilmoitusten kuuntelu." urakka-id))
                                  (notifikaatiot/lopeta-ilmoitusten-kuuntelu tapahtumat urakka-id)))

                      (let [laheta-ilmoitukset (ilmoituslahettaja integraatioloki tapahtumat kanava tapahtuma-id sulje-vastauksen-jalkeen?)
                            odottavat-ilmoitukset (and muuttunut-jalkeen (ilmoitukset/hae-muuttuneet-ilmoitukset db urakka-id muuttunut-jalkeen))]

                        ;; Jos löytyi vanhoja ilmoituksia tai ei pidä jäädä odottamaan uusia ilmoituksia, palautetaan response välittömästi
                        (when (or (not odota-uusia?) (not (empty? odottavat-ilmoitukset)))
                          (laheta-ilmoitukset odottavat-ilmoitukset))

                        ;; Muussa tapauksessa jäädään kuuntelemaan uusia ilmoituksia
                        (notifikaatiot/kuuntele-urakan-ilmoituksia
                          tapahtumat
                          urakka-id
                          (fn [ilmoitus-id]
                            (laheta-ilmoitukset (ilmoitukset/hae-ilmoitukset-ilmoitusidlla db [(Integer/parseInt ilmoitus-id)]))))))))))

(defrecord Ilmoitukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db integraatioloki :integraatioloki klusterin-tapahtumat :klusterin-tapahtumat tloik :tloik :as this}]
    (julkaise-reitti
      http :hae-ilmoitukset
      (GET "/api/urakat/:id/ilmoitukset" request
        (kaynnista-ilmoitusten-kuuntelu db integraatioloki klusterin-tapahtumat request)))

    (julkaise-reitti
      http :kirjaa-ilmoitustoimenpide
      (PUT "/api/ilmoitukset/:id/" request
        (kasittele-kutsu db integraatioloki :kirjaa-ilmoitustoimenpide request json-skeemat/ilmoitustoimenpiteen-kirjaaminen json-skeemat/kirjausvastaus
                         (fn [parametrit data _ db] (kirjaa-ilmoitustoimenpide db tloik parametrit data)))))
    this)
  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :hae-ilmoitukset)
    (poista-palvelut http :kirjaa-ilmoitustoimenpide)
    this))
