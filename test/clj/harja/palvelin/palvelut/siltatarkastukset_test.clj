(ns harja.palvelin.palvelut.siltatarkastukset-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [com.stuartsierra.component :as component]
            [harja.testi :refer [jarjestelma luo-testitietokanta testi-http-palvelin kutsu-http-palvelua] :as testi]

            [harja.palvelin.palvelut.siltatarkastukset :as siltatarkastukset]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (luo-testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :siltatarkastukset (component/using
                                             (siltatarkastukset/->Siltatarkastukset)
                                             [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'testi/jarjestelma component/stop))

(use-fixtures :once jarjestelma-fixture)

(defn- silta-nimella [sillat nimi]
  (first (filter #(= nimi (:siltanimi %)) sillat)))

(deftest joutsensillalle-ei-ole-tarkastuksia
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :kaikki})
        joutsensilta (silta-nimella sillat "Joutsensilta")]
    (is joutsensilta "Joutsensilta löytyi")
    (is (nil? (:tarkastusaika joutsensilta)) "Joutsensiltaa ei ole tarkastettu")))

(deftest kempeleen-testisillan-tarkastus
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :kaikki})
        kempele (silta-nimella sillat "Kempeleen testisilta")]
    (is kempele "Kempeleen testisilta löytyy")
    (is (= "Late Lujuuslaskija" (:tarkastaja kempele)))))

(deftest puutteellisia-siltoja
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :puutteet})]
    (is (silta-nimella sillat "Kempeleen testisilta"))
    (is (silta-nimella sillat "Oulujoen silta"))
    (is (nil? (silta-nimella sillat "Joutsensilta")) "Joutsensilta ei löydy puutelistalta")))

(deftest korjattuja-siltoja
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :korjatut})
        kajaanintie (silta-nimella sillat "Kajaanintien silta")]
    (is kajaanintie)
    (is (= 24 (:rikki-ennen kajaanintie)) "Ennen oli kaikki rikki")
    (is (= 0 (:rikki-nyt kajaanintie)) "Nyt on kaikki korjattu")))

(deftest oulun-urakan-2005-2012-sillat
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :kaikki})
        sillat-paitsi-joutsensilta (filter #(not= "Joutsensilta" (:siltanimi %)) sillat)]
    (is (= (count sillat) 5))
    (is (= (count sillat-paitsi-joutsensilta) 4))
    (is (every? #(some? (:tarkastusaika %)) sillat-paitsi-joutsensilta))))


(deftest oulun-urakan-2014-2019-sillat
  ;; Tässä uudemmassa urakassa halutaan nähdä vanhassa urakassa tehty viimeisin tarkastus
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2014-2019-id)
                                     :listaus :kaikki})
        sillat-paitsi-joutsensilta (filter #(not= "Joutsensilta" (:siltanimi %)) sillat)]
    (is (= (count sillat) 5))
    (is (= (count sillat-paitsi-joutsensilta) 4))
    (is (every? #(some? (:tarkastusaika %)) sillat-paitsi-joutsensilta))))

(deftest oulun-urakan-2005-2012-tarkastukset
  (let [tarkastukset (kutsu-http-palvelua :hae-sillan-tarkastukset testi/+kayttaja-jvh+
                                    {:urakka-id (testi/hae-oulun-alueurakan-2005-2012-id)
                                     :silta-id (testi/hae-oulujoen-sillan-id)})]
    (is (= (count tarkastukset) 2))
    (is (every? #(map? (:kohteet %)) tarkastukset))))

(deftest oulun-urakan-2005-2014-tarkastukset
  ;; Tässä uudemmassa urakassa halutaan nähdä myös sillan aiemmat tarkastukset
  (let [tarkastukset (kutsu-http-palvelua :hae-sillan-tarkastukset testi/+kayttaja-jvh+
                                          {:urakka-id (testi/hae-oulun-alueurakan-2014-2019-id)
                                           :silta-id (testi/hae-oulujoen-sillan-id)})]
    (is (= (count tarkastukset) 2))
    (is (every? #(map? (:kohteet %)) tarkastukset))))