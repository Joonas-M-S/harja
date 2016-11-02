(ns harja.palvelin.palvelut.api-jarjestelmatunnukset-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.haku :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.palvelin.palvelut.api-jarjestelmatunnukset :as api-jarjestelmatunnukset])
  (:import (harja.domain.roolit EiOikeutta))
  (:use [slingshot.slingshot :only [try+ throw+]]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :api-jarjestelmatunnukset (component/using
                                                    (api-jarjestelmatunnukset/->APIJarjestelmatunnukset)
                                                    [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once jarjestelma-fixture)

(deftest jarjestelmatunnuksien-haku-toimii
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-jarjestelmatunnukset +kayttaja-jvh+ nil)]
    (is (vector? vastaus))
    (is (= (count vastaus) 5))))

(deftest jarjestelmatunnuksien-haku-ei-toimi-ilman-oikeuksia
  (try+
    (let [_ (kutsu-palvelua (:http-palvelin jarjestelma)
                            :hae-jarjestelmatunnukset +kayttaja-tero+ nil)])
    (is false "Nyt on joku paha oikeusongelma")
    (catch EiOikeutta e
      (is e))))

(deftest jarjestelmatunnuksien-lisaoikeuksian-haku-toimii
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-jarjestelmatunnuksen-lisaoikeudet +kayttaja-jvh+ nil)]
    (is (vector? vastaus))
    (is (= (count vastaus) 0))))

(deftest jarjestelmatunnuksien-lisaoikeuksian-haku-ei-toimi-ilman-oikeuksia
  (try+
    (let [_ (kutsu-palvelua (:http-palvelin jarjestelma)
                            :hae-jarjestelmatunnuksen-lisaoikeudet +kayttaja-tero+ nil)])
    (is false "Nyt on joku paha oikeusongelma")
    (catch EiOikeutta e
      (is e))))

(deftest urakoiden-haku-toimii
  (let [odotettu-maara (ffirst (q "SELECT COUNT(*) FROM urakka;"))
        vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-urakat-lisaoikeusvalintaan +kayttaja-jvh+ nil)]
    (is (vector? vastaus))
    (is (= (count vastaus) odotettu-maara))))

(deftest urakoiden-haku-ei-toimi-ilman-oikeuksia
  (try+
    (let [_ (kutsu-palvelua (:http-palvelin jarjestelma)
                            :hae-urakat-lisaoikeusvalintaan +kayttaja-tero+ nil)])
    (is false "Nyt on joku paha oikeusongelma")
    (catch EiOikeutta e
      (is e))))

(deftest jarjestelmatunnusten-tallennus-toimii
  (let [testitunnukset [{:id -1, :kayttajanimi "juha88",
                         :kuvaus "noni",
                         :organisaatio {:nimi "Liikennevirasto", :id 1}}
                        {:id (ffirst (q "SELECT id FROM kayttaja WHERE kayttajanimi = 'yit-rakennus'")),
                         :kayttajanimi "yit-rakennus",
                         :kuvaus "testissä muutettu",
                         :organisaatio {:nimi "Liikennevirasto", :id 1}}]
        _ (kutsu-palvelua (:http-palvelin jarjestelma)
                          :tallenna-jarjestelmatunnukset +kayttaja-jvh+ testitunnukset)]

    (is (integer? (ffirst (q "SELECT id FROM kayttaja WHERE kayttajanimi = 'juha88'"))))
    (is (= (ffirst (q "SELECT kuvaus FROM kayttaja WHERE kayttajanimi = 'yit-rakennus'"))
           "testissä muutettu"))
    (u "DELETE FROM kayttaja WHERE kayttajanimi = 'juha88';")))