(ns harja.palvelin.integraatiot.turi.turi-komponentti
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.palvelin.integraatiot.integraatiopisteet.http :as http]
            [harja.kyselyt.turvallisuuspoikkeamat :as q]
            [harja.palvelin.integraatiot.turi.turvallisuuspoikkeamasanoma :as sanoma]))

(defprotocol TurvallisuusPoikkeamanLahetys
  (laheta-turvallisuuspoikkeama [this id]))

(defn tee-lokittaja [this]
  (integraatioloki/lokittaja (:integraatioloki this) (:db this) "turi" "laheta-turvallisuuspoikkeama"))

(defn kasittele-turin-vastaus [db vastaus otsikot id]
  ;; todo: tarkista onnistuiko
  (q/lokita-lahetys<! db true id))

(defn laheta-turvallisuuspoikkeama-turiin [{:keys [db integraatioloki url kayttajatunnus salasana]} id]
  (let [lokittaja (integraatioloki/lokittaja integraatioloki db "turi" "laheta-turvallisuuspoikkeama")
        integraatiopiste (http/luo-integraatiopiste lokittaja {:kayttajatunnus kayttajatunnus :salasana salasana})
        vastauskasittelija (fn [vastaus otsikot] (kasittele-turin-vastaus db vastaus otsikot id))
        turvallisuuspoikkeama (q/hae-turvallisuuspoikkeama db id)
        xml (sanoma/muodosta turvallisuuspoikkeama)]
    (try
      (http/POST integraatiopiste url xml vastauskasittelija)
      (catch Exception e
        (log/error e (format "Turvallisuuspoikkeaman (id: %s) lähetyksessä tapahtui poikkeus." id))
        (q/lokita-lahetys<! db false id)))))

(defrecord Turi [asetukset]
  component/Lifecycle
  (start [this]
    (let [turi (:turi asetukset)
          {url :url kayttajatunnus :kayttajatunnus salasana :salasana} turi]
      (log/debug (format "Käynnistetään TURI-komponentti (URL: %s)" url))
      (assoc this
        :url url
        :kayttajatunnus kayttajatunnus
        :salasana salasana)))

  (stop [this]
    this)

  TurvallisuusPoikkeamanLahetys
  (laheta-turvallisuuspoikkeama [this id]
    (laheta-turvallisuuspoikkeama-turiin this id)))

