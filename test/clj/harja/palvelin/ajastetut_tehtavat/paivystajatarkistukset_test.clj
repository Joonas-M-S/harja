(ns harja.palvelin.ajastetut-tehtavat.paivystajatarkistukset-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [clj-time.periodic :refer [periodic-seq]]
            [harja.palvelin.ajastetut-tehtavat.paivystystarkistukset :as paivystajatarkistukset]
            [harja.testi :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [clj-time.core :as t]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.fim :as fim])
  (:use org.httpkit.fake))

(def +testi-fim-+ "https://localhost:6666/FIMDEV/SimpleREST4FIM/1/Group.svc/getGroupUsersFromEntitity")

(defn jarjestelma-fixture [testit]
  (alter-var-root
    #'jarjestelma
    (fn [_]
      (component/start
        (component/system-map
          :db (tietokanta/luo-tietokanta testitietokanta)
          :http-palvelin (testi-http-palvelin)
          :integraatioloki (component/using (integraatioloki/->Integraatioloki nil) [:db])
          :fim (component/using
                 (fim/->FIM +testi-fim-+)
                 [:db :integraatioloki])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures tietokanta-fixture jarjestelma-fixture))

(def testipaivystykset
  [{:urakka 4,
    :nimi "Oulun alueurakka 2014-2019",
    :paivystykset [{:id 3,
                    :alku (t/local-date 2016 10 1)
                    :loppu (t/local-date 2016 10 1)}
                   {:id 1,
                    :alku (t/local-date 2016 10 2)
                    :loppu (t/local-date 2016 10 2)}
                   {:id 2,
                    :alku (t/local-date 2016 10 3)
                    :loppu (t/local-date 2016 10 3)}]}
   {:urakka 20, :nimi "Kajaanin alueurakka 2014-2019"}
   {:urakka 11, :nimi "Oulun valaistuksen palvelusopimus 2013-2018"}
   {:urakka 12, :nimi "Pirkanmaan tiemerkinnän palvelusopimus 2013-2018"}
   {:urakka 10, :nimi "Oulun tiemerkinnän palvelusopimus 2013-2018"}
   {:urakka 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015"}
   {:urakka 13, :nimi "Lapin tiemerkinnän palvelusopimus 2013-2018"}
   {:urakka 21, :nimi "Vantaan alueurakka 2014-2019"}
   {:urakka 5, :nimi "Muhoksen päällystysurakka"}
   {:urakka 19, :nimi "Tievalaistuksen palvelusopimus 2015-2020"}
   {:urakka 8, :nimi "YHA-päällystysurakka (sidottu)"}
   {:urakka 6, :nimi "YHA-päällystysurakka"}
   {:urakka 16, :nimi "Tiemerkintöjen palvelusopimus KAS ELY 2013 - 2017"}
   {:urakka 22, :nimi "Espoon alueurakka 2014-2019"}
   {:urakka 9, :nimi "Porintien päällystysurakka"}
   {:urakka 7, :nimi "YHA-paikkausurakka"}])

(def ilmoituksien-saajat
  [{:tunniste nil,
    :kayttajatunnus "A000001",
    :etunimi "Erkki",
    :sukunimi "Esimerkki",
    :sahkoposti "erkki.esimerkki@example.com",
    :puhelin "",
    :roolit ["ELY urakanvalvoja"],
    :organisaatio "ELY"}
   {:tunniste nil,
    :kayttajatunnus "A000002",
    :etunimi "Eero",
    :sukunimi "Esimerkki",
    :sahkoposti "eero.esimerkki@example.com",
    :puhelin "0400123456789",
    :roolit ["Urakan vastuuhenkilö"],
    :organisaatio "ELY"}])

(deftest urakoiden-paivystajien-haku-toimii
  (let [testitietokanta (tietokanta/luo-tietokanta testitietokanta)
        urakoiden-paivystykset (paivystajatarkistukset/hae-urakoiden-paivystykset
                                 testitietokanta
                                 (t/local-date 2016 10 1))]

    ;; Oulun alueurakka 2014-2019 löytyy 3 päivystystä
    (is (= (count (:paivystykset (first (filter
                                          #(= (:nimi %) "Oulun alueurakka 2014-2019")
                                          urakoiden-paivystykset))))
           3))

    ;; Kaikki testidatan käynnissä olleet urakat löytyi
    (is (= (count urakoiden-paivystykset) 17))))

(deftest urakat-ilman-paivystysta-toimii
  ;; Testidatassa ei yhdelläkään urakalla päivystystä annettuna aikana, eli palautuu sama data takaisin
  (is (= (paivystajatarkistukset/urakat-ilman-paivystysta
           testipaivystykset
           (t/local-date 2010 10 1))
         testipaivystykset))

  ;; Oulun alueurakka 2014-2019 sisältää päivystyksen annettuna aikana, joten se ei palaudu
  (is (= (paivystajatarkistukset/urakat-ilman-paivystysta
           testipaivystykset
           (t/local-date 2016 10 1))
         (rest testipaivystykset))))

(deftest ilmoituksien-saajien-haku-toimii
  (let [vastaus-xml (slurp (io/resource "xsd/fim/esimerkit/hae-urakan-kayttajat.xml"))]
    (with-fake-http
      [+testi-fim-+ vastaus-xml]
      (let [vastaus (paivystajatarkistukset/hae-ilmoituksen-saajat
                      (:fim jarjestelma)
                      "1242141-OULU2")]
        (is (= vastaus ilmoituksien-saajat))))))