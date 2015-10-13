(ns harja.palvelin.integraatiot.api.siltatarkastukset
  "Siltatarkstuksien API-kutsut"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer [POST GET DELETE PUT]]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-reitti poista-palvelut]]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [tee-sisainen-kasittelyvirhevastaus tee-viallinen-kutsu-virhevastaus tee-vastaus]]
            [harja.palvelin.integraatiot.api.tyokalut.json-skeemat :as json-skeemat]
            [harja.palvelin.integraatiot.api.tyokalut.kutsukasittely :refer [kasittele-kutsu]]
            [harja.kyselyt.siltatarkastukset :as silta-q]
            [clojure.java.jdbc :as jdbc]
            [harja.palvelin.integraatiot.api.tyokalut.validointi :as validointi])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn luo-siltatarkastuas [ulkoinen-id tarkastus kayttaja db]
  (log/debug "Luodaan uusi siltarkastus")
  ; TODO
  )

(defn paivita-siltatarkastus [ulkoinen-id tarkastus kayttaja db]
  (log/debug "Päivitetään vanha siltarkastus")
  ; TODO
  )

(defn luo-tai-paivita-siltatarkastus [ulkoinen-id tarkastus kayttaja db]
  (let [siltatarkastus-kannassa (first (silta-q/hae-siltatarkastus-ulkoisella-idlla-ja-luojalla db ulkoinen-id (:id kayttaja)))]
    (if siltatarkastus-kannassa
      (paivita-siltatarkastus ulkoinen-id tarkastus kayttaja db)
      (luo-siltatarkastuas ulkoinen-id tarkastus kayttaja db))))

(defn lisaa-siltatarkastuskohteet [ulkoinen-id tarkastus kayttaja db]
  ;; TODO Poista vanha
  ;; TODO Lisää uudet
  )

(defn lisaa-siltatarkastus [{id :id} tarkastus kayttaja db]
  (let [urakka-id (Long/parseLong id)
        ulkoinen-id (-> tarkastus :tunniste :id)]
    (validointi/tarkista-urakka-ja-kayttaja db urakka-id kayttaja)
    (log/info "Kirjataan siltatarkastus käyttäjältä: " kayttaja)
    (jdbc/with-db-transaction [db db]
                              (luo-tai-paivita-siltatarkastus ulkoinen-id tarkastus kayttaja db)
                              (lisaa-siltatarkastuskohteet ulkoinen-id tarkastus kayttaja db))))

(defrecord Siltatarkastukset []
  component/Lifecycle
  (start [{http :http-palvelin db :db integraatioloki :integraatioloki :as this}]
    (julkaise-reitti
      http :lisaa-siltatarkastus
      (POST "/api/urakat/:id/tarkastus/siltatarkastus" request
        (kasittele-kutsu db integraatioloki :hae-tietolaji request json-skeemat/+siltatarkastuksen-kirjaus+ nil
                         (fn [parametrit data kayttaja db]
                           (lisaa-siltatarkastus parametrit data kayttaja db)))))
    this)

  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http
                     :lisaa-siltatarkastus)
    this))