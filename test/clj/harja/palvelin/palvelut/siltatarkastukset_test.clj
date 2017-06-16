(ns harja.palvelin.palvelut.siltatarkastukset-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [harja.testi :refer :all]
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
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :each (compose-fixtures tietokanta-fixture jarjestelma-fixture))

(defn- silta-nimella [sillat nimi]
  (first (filter #(= nimi (:siltanimi %)) sillat)))

(defn- poista-testin-tarkastukset []
  (let [id (ffirst (q "SELECT id FROM siltatarkastus WHERE tarkastaja = 'TESTIKAYTTAJA';"))]
    (u "DELETE FROM siltatarkastuskohde WHERE siltatarkastus = " id ";")
    (u "DELETE FROM siltatarkastus WHERE tarkastaja = 'TESTIKAYTTAJA'")))

(deftest joutsensillalle-ei-ole-tarkastuksia
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat +kayttaja-jvh+
                                    {:urakka-id (hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :kaikki})
        joutsensilta (silta-nimella sillat "Joutsensilta")]
    (is joutsensilta "Joutsensilta löytyi")
    (is (nil? (:tarkastusaika joutsensilta)) "Joutsensiltaa ei ole tarkastettu")))

(deftest kempeleen-testisillan-tarkastus
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat +kayttaja-jvh+
                                    {:urakka-id (hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :kaikki})
        kempele (silta-nimella sillat "Kempeleen testisilta")]
    (is kempele "Kempeleen testisilta löytyy")
    (is (= "Late Lujuuslaskija" (:tarkastaja kempele)))))

(deftest puutteellisia-siltoja
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat +kayttaja-jvh+
                                    {:urakka-id (hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :puutteet})]
    (is (silta-nimella sillat "Kempeleen testisilta"))
    (is (silta-nimella sillat "Oulujoen silta"))
    (is (nil? (silta-nimella sillat "Joutsensilta")) "Joutsensilta ei löydy puutelistalta")))

(deftest korjattuja-siltoja
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat +kayttaja-jvh+
                                    {:urakka-id (hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :korjatut})
        kajaanintie (silta-nimella sillat "Kajaanintien silta")]
    (is kajaanintie)
    (is (= 24 (:rikki-ennen kajaanintie)) "Ennen oli kaikki rikki")
    (is (= 0 (:rikki-nyt kajaanintie)) "Nyt on kaikki korjattu")))

(deftest oulun-urakan-2005-2012-sillat
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat +kayttaja-jvh+
                                    {:urakka-id (hae-oulun-alueurakan-2005-2012-id)
                                     :listaus :kaikki})
        sillat-ilman-tarkastuksia (filter #(and (not= "Joutsensilta" (:siltanimi %))
                                                (not= "Pyhäjoen silta" (:siltanimi %))) sillat)]
    (is (= (count sillat) 6))
    (is (= (count sillat-ilman-tarkastuksia) 3))
    (is (every? #(some? (:tarkastusaika %)) sillat-ilman-tarkastuksia))))

(deftest oulun-urakan-2014-2019-sillat
  ;; Tässä uudemmassa urakassa halutaan nähdä vanhassa urakassa tehty viimeisin tarkastus
  (let [sillat (kutsu-http-palvelua :hae-urakan-sillat +kayttaja-jvh+
                                    {:urakka-id (hae-oulun-alueurakan-2014-2019-id)
                                     :listaus :kaikki})
        sillat-ilman-tarkastuksia (filter #(and (not= "Joutsensilta" (:siltanimi %))
                                                (not= "Pyhäjoen silta" (:siltanimi %))) sillat)]
    (is (= (count sillat) 6))
    (is (= (count sillat-ilman-tarkastuksia) 3))
    (is (every? #(some? (:tarkastusaika %)) sillat-ilman-tarkastuksia))))

(deftest oulun-urakan-2005-2012-tarkastukset
  (let [tarkastukset (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                          {:urakka-id (hae-oulun-alueurakan-2005-2012-id)
                                           :silta-id (hae-oulujoen-sillan-id)})]
    (is (= (count tarkastukset) 2))
    (is (every? #(map? (:kohteet %)) tarkastukset))))

(deftest oulun-urakan-2005-2014-tarkastukset
  ;; Tässä uudemmassa urakassa halutaan nähdä myös sillan aiemmat tarkastukset
  (let [tarkastukset (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                          {:urakka-id (hae-oulun-alueurakan-2014-2019-id)
                                           :silta-id (hae-oulujoen-sillan-id)})]
    (is (= (count tarkastukset) 2))
    (is (every? #(map? (:kohteet %)) tarkastukset))))

(defn uusi-tarkastus []
  {:uudet-liitteet nil, :urakka-id (hae-oulun-alueurakan-2014-2019-id)
   :kohteet {7 ["A" ""], 20 ["B" ""], 1 ["A" ""], 24 ["C" ""], 4 ["A" ""], 15 ["B" ""],
             21 ["B" ""], 13 ["A" ""], 22 ["C" ""], 6 ["B" ""], 17 ["B" ""], 3 ["A" ""],
             12 ["A" ""], 2 ["A" ""], 23 ["C" ""], 19 ["C" ""], 11 ["A" ""], 9 ["B" ""],
             5 ["B" ""], 14 ["A" ""], 16 ["A" ""], 10 ["B" ""], 18 ["B" ""], 8 ["B" ""]},
   :silta-id (hae-oulujoen-sillan-id),
   :liitteet [],
   :tarkastusaika #inst "2017-07-28T11:34:49.000-00:00",
   :poistettu false
   :tarkastaja "TESTIKAYTTAJA"})

(deftest tarkastuksen-tallennus-oulujoen-sillalle
  (let [urakka-id (hae-oulun-alueurakan-2014-2019-id)
        silta-id (hae-oulujoen-sillan-id)
        tarkastukset-ennen-uutta (count (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                             {:urakka-id urakka-id
                                                              :silta-id silta-id}))
        _ (kutsu-http-palvelua :tallenna-siltatarkastus +kayttaja-jvh+
                               (uusi-tarkastus))
        tarkastukset-kutsun-jalkeen (count (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                                {:urakka-id urakka-id
                                                                 :silta-id silta-id}))]
    (is (= (+ tarkastukset-ennen-uutta 1) tarkastukset-kutsun-jalkeen))
    (poista-testin-tarkastukset)))

(deftest tarkastuksen-tallennus-ei-urakan-sillalle-epaonnistuu
  (let [urakka-id (hae-kajaanin-alueurakan-2014-2019-id)
        silta-id (hae-pyhajoen-sillan-id)
        tarkastukset-ennen-uutta (count (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                             {:urakka-id urakka-id
                                                              :silta-id silta-id}))
        _ (is (thrown? SecurityException (kutsu-http-palvelua :tallenna-siltatarkastus +kayttaja-jvh+
                                                              (-> (uusi-tarkastus)
                                                                  (assoc :silta-id silta-id)
                                                                  (assoc :urakka-id urakka-id)))))
        tarkastukset-kutsun-jalkeen (count (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                                {:urakka-id urakka-id
                                                                 :silta-id silta-id}))]
    (is (= tarkastukset-ennen-uutta tarkastukset-kutsun-jalkeen))))

(deftest tarkastuksen-tallennus-ilman-oikeuksia-epaonnistuu
  (is (thrown? Exception (kutsu-http-palvelua :tallenna-siltatarkastus +kayttaja-tero+
                                              (uusi-tarkastus)))))


(deftest tarkista-siltatarkastuksen-poisto
  (let [urakka-id (hae-oulun-alueurakan-2014-2019-id)
        silta-id (hae-oulujoen-sillan-id)
        tarkastukset-ennen-uutta (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                      {:urakka-id urakka-id
                                                       :silta-id silta-id})
        poistettavan-tarkastuksen-id (:id (kutsu-http-palvelua :tallenna-siltatarkastus +kayttaja-jvh+ (uusi-tarkastus)))
        tarkastukset-lisayksen-jalkeen (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                            {:urakka-id urakka-id
                                                             :silta-id silta-id})
        poistovastaus (kutsu-http-palvelua :poista-siltatarkastus +kayttaja-jvh+ {:urakka-id urakka-id
                                                                                  :silta-id silta-id
                                                                                  :siltatarkastus-id poistettavan-tarkastuksen-id})
        tarkastukset-poiston-jalkeen (kutsu-http-palvelua :hae-sillan-tarkastukset +kayttaja-jvh+
                                                          {:urakka-id urakka-id
                                                           :silta-id silta-id})]
    (is (= [{:id 2
             :kohteet {1 ["A"
                          ""]
                       10 ["A"
                           ""]
                       11 ["A"
                           ""]
                       12 ["A"
                           ""]
                       13 ["A"
                           ""]
                       14 ["A"
                           ""]
                       15 ["A"
                           ""]
                       16 ["A"
                           ""]
                       17 ["A"
                           ""]
                       18 ["D"
                           ""]
                       19 ["A"
                           ""]
                       2 ["A"
                          ""]
                       20 ["C"
                           ""]
                       21 ["B"
                           ""]
                       22 ["A"
                           ""]
                       23 ["A"
                           ""]
                       24 ["A"
                           ""]
                       3 ["A"
                          ""]
                       4 ["A"
                          ""]
                       5 ["A"
                          ""]
                       6 ["A"
                          ""]
                       7 ["A"
                          ""]
                       8 ["B"
                          ""]
                       9 ["A"
                          ""]}
             :liitteet []
             :luoja nil
             :luotu nil
             :muokattu nil
             :muokkaaja nil
             :poistettu false
             :silta 1
             :tarkastaja "Sirkka Sillankoestaja"
             :tarkastusaika #inst "2007-02-24T22:00:00.000000000-00:00"
             :urakka 1}
            {:id 1
             :kohteet {1 ["A"
                          "Maatuki ruosteessa."]
                       10 ["A"
                           ""]
                       11 ["A"
                           ""]
                       12 ["A"
                           ""]
                       13 ["A"
                           ""]
                       14 ["A"
                           ""]
                       15 ["A"
                           ""]
                       16 ["A"
                           ""]
                       17 ["A"
                           ""]
                       18 ["A"
                           ""]
                       19 ["A"
                           ""]
                       2 ["B"
                          ""]
                       20 ["A"
                           ""]
                       21 ["A"
                           ""]
                       22 ["A"
                           ""]
                       23 ["A"
                           ""]
                       24 ["A"
                           ""]
                       3 ["C"
                          ""]
                       4 ["D"
                          ""]
                       5 ["A"
                          ""]
                       6 ["A"
                          ""]
                       7 ["A"
                          ""]
                       8 ["A"
                          ""]
                       9 ["A"
                          ""]}
             :liitteet []
             :luoja nil
             :luotu nil
             :muokattu nil
             :muokkaaja nil
             :poistettu false
             :silta 1
             :tarkastaja "Sini Sillantarkastaja"
             :tarkastusaika #inst "2006-04-14T21:00:00.000000000-00:00"
             :urakka 1}]
          poistovastaus))
    (is (= (+ (count tarkastukset-ennen-uutta) 1)
           (count tarkastukset-lisayksen-jalkeen))
        "Lisäyksen jälkeen on 1 uusi tarkastus")
    (is (= (count tarkastukset-ennen-uutta)
           (count tarkastukset-poiston-jalkeen))
        "Poiston jälkeen on sama määrä tarkastuksia kuin aluksi")
    (is (not (some #(= poistettavan-tarkastuksen-id (:id %)) tarkastukset-poiston-jalkeen)) "Poistettua tarkastusta ei löydy listasta")
    (poista-testin-tarkastukset)))