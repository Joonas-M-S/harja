(ns harja.palvelin.komponentit.http-palvelin-test
  (:require [harja.testi :refer :all]
            [clojure.test :refer :all]
            [org.httpkit.client :as http]
            [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :as palvelin]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.index :as index]
            [harja.kyselyt.anti-csrf :as anti-csrf-q]
            [harja.palvelin.komponentit.todennus :as todennus]
            [harja.palvelin.komponentit.http-palvelin :as http-palvelin]
            [clj-time.core :as t]))

(def kayttaja +kayttaja-jvh+)

(def csrf-token-secret "foobar")

(def random-avain "baz")

(def csrf-token (index/muodosta-csrf-token random-avain
                                           csrf-token-secret))
(defn jarjestelma-fixture [testit]
  (let [nyt (t/now)]
    (pudota-ja-luo-testitietokanta-templatesta)
    (alter-var-root #'portti (fn [_#] (arvo-vapaa-portti)))
    (alter-var-root #'jarjestelma
                    (fn [_]
                      (component/start
                        (component/system-map
                          :todennus (component/using
                                      (todennus/http-todennus {})
                                      [:db])
                          :db (tietokanta/luo-tietokanta testitietokanta)
                          :http-palvelin (component/using
                                           (http-palvelin/luo-http-palvelin {:portti portti
                                                                             :anti-csrf-token csrf-token-secret} true)
                                           [:todennus :db])))))
    (anti-csrf-q/poista-ja-luo-csrf-sessio (:db jarjestelma) (:kayttajanimi kayttaja) csrf-token nyt)
    (testit)
    (alter-var-root #'jarjestelma component/stop)))

(use-fixtures :each jarjestelma-fixture)

(defn ok-palvelu-get [user] {})

(defn bad-request-palvelu-get [user]
  (throw (IllegalArgumentException. "bad request")))

(defn internal-server-error-palvelu-get [user]
  (throw (RuntimeException. "internal server error")))

(defn get-kutsu [palvelu]
  @(http/get (str "http://localhost:" portti "/_/" (name palvelu))
             {:headers {"OAM_REMOTE_USER" (:kayttajanimi kayttaja)
                        "OAM_GROUPS" (interpose "," (:roolit kayttaja))
                        "Content-Type" "application/json"
                        "x-csrf-token" random-avain}}))

(deftest get-palvelu-palauta-ok
  (println "petar palvelin " (:http-palvelin jarjestelma))
  (palvelin/julkaise-palvelu (:http-palvelin jarjestelma) :ok-palvelu ok-palvelu-get)
  (let [vastaus (get-kutsu :ok-palvelu)]
    (println "petar vastaus ")
    (clojure.pprint/pprint vastaus)
    (is (= 200 (:status vastaus)))))