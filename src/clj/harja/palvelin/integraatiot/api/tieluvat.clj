(ns harja.palvelin.integraatiot.api.tieluvat
  "Tielupien hallinta API:n kautta"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET DELETE]]
            [taoensso.timbre :as log]
            [clojure.string :refer [join]]
            [slingshot.slingshot :refer [try+ throw+]]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu tee-kirjausvastauksen-body]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.liitteet :refer [tallenna-liitteet-tarkastukselle]]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi]
            [harja.palvelin.integraatiot.api.sanomat.tielupa-sanoma :as tielupa-sanoma]
            [harja.domain.tielupa :as tielupa]
            [harja.kyselyt.tielupa :as tielupa-q]
            [harja.kyselyt.kayttajat :as kayttajat-q]
            [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet])
  (:use [slingshot.slingshot :only [throw+]]))

(defn hae-sijainnit [db tielupa]
  tielupa)

(defn hae-urakka [db tielupa]
  tielupa)

(defn hae-ely [db tielupa ely]
  (let [ely-numero (case ely
                     "Uusimaa" 1
                     "Keski-Suomi" 9
                     "Lappi" 14
                     "Etelä-Pohjanmaa" 10
                     "Pohjois-Pohjanmaa ja Kainuu" 12
                     "Kaakkois-Suomi" 3
                     "Varsinais-Suomi" 2
                     "Pohjois-Savo" 8
                     "Pirkanmaa" 4
                     (throw+ {:type virheet/+viallinen-kutsu+
                              :virheet [{:koodi virheet/+tuntematon-ely+
                                         :viesti (str "Tuntematon ELY " ely)}]}))
        ely-id (:id (first (kayttajat-q/hae-ely-numerolla db ely-numero)))]
    (assoc tielupa ::tielupa/ely ely-id)))

(defn kirjaa-tielupa [liitteiden-hallinta db parametrit data kayttaja]
  (validointi/tarkista-onko-liikenneviraston-jarjestelma db kayttaja)

  (->> (tielupa-sanoma/api->domain (:tielupa data))
       (hae-sijainnit db)
       (hae-urakka db)
       (hae-ely db (get-in data [:tielupa :perustiedot :ely]))
       (tielupa-q/tallenna-tielupa db))
  (tee-kirjausvastauksen-body {:ilmoitukset " Tielupa kirjattu onnistuneesti "}))

(defrecord Tieluvat []
  component/Lifecycle
  (start [{http :http-palvelin db :db liitteiden-hallinta :liitteiden-hallinta integraatioloki :integraatioloki :as this}]
    (julkaise-reitti
      http :kirjaa-tielupa
      (POST " /api/tieluvat " request
        (kasittele-kutsu db
                         integraatioloki
                         :kirjaa-tielupa
                         request
                         json-skeemat/tieluvan-kirjaus-request
                         json-skeemat/kirjausvastaus
                         (fn [parametrit data kayttaja db]
                           (kirjaa-tielupa liitteiden-hallinta db parametrit data kayttaja)))))
    this)

  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :kirjaa-tielupa)
    this))
