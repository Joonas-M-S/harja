(ns harja.palvelin.palvelut.laskutusyhteenveto-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.raportointi.raportit.laskutusyhteenveto :as laskutusyhteenveto]
            [harja.palvelin.palvelut.yksikkohintaiset-tyot :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.kyselyt.konversio :as konv]
            [harja.pvm :as pvm]
            [harja.testi :as testi]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :yksikkohintaiset-tyot (component/using
                                                 (->Yksikkohintaiset-tyot)
                                                 [:http-palvelin :db])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures
                      tietokanta-fixture
                      (compose-fixtures jarjestelma-fixture urakkatieto-fixture)))

(deftest laskutusyhteenvedon-tietojen-haku
  (testing "laskutusyhteenvedon-tietojen-haku"
    (let [haetut-tiedot-oulu (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma)
                               +kayttaja-jvh+
                               {:urakka-id @oulun-alueurakan-2014-2019-id
                                :alkupvm   (pvm/->pvm "1.8.2015")
                                :loppupvm (pvm/->pvm "31.8.2015")})
          haetut-tiedot-kajaani (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                                  (:db jarjestelma)
                                  +kayttaja-jvh+
                                  {:urakka-id @kajaanin-alueurakan-2014-2019-id
                                   :alkupvm   (pvm/->pvm "1.8.2015")
                                   :loppupvm  (pvm/->pvm "31.8.2015")})
          poista-tpi (fn [tiedot]
                                (map #(dissoc %
                                              :tpi) tiedot))
          haetut-tiedot-oulu-ilman-tpita (poista-tpi haetut-tiedot-oulu)
          haetut-tiedot-kajaani-ilman-tpita (poista-tpi haetut-tiedot-kajaani)

          haetut-tiedot-oulu-talvihoito (first (filter #(= (:tuotekoodi %) "23100") haetut-tiedot-oulu))
          haetut-tiedot-oulu-liikenneymparisto (first (filter #(= (:tuotekoodi %) "23110") haetut-tiedot-oulu))
          haetut-tiedot-oulu-soratiet (first (filter #(= (:tuotekoodi %) "23120") haetut-tiedot-oulu))
          _ (log/debug "haetut-tiedot-oulu-talvihoito" haetut-tiedot-oulu-talvihoito)
          _ (log/debug "haetut-tiedot-oulu-liikenneymparisto" haetut-tiedot-oulu-liikenneymparisto)
          _ (log/debug "haetut-tiedot-oulu-soratiet" haetut-tiedot-oulu-soratiet)

          odotetut-talvihoito
          {:bonukset_laskutettu_ind_korotettuna               0.0M
           :bonukset_laskutetaan                              1000.0M
           :yht_laskutettu_ind_korotettuna                    2009.57854406130265900000M
           :suolasakot_laskutetaan                            -29760.00000M
           :kaikki_laskutetaan_ind_korotus                    -5.6239047196267293788000000M
           :sakot_laskutetaan_ind_korotettuna                 0.0M
           :kaikki_paitsi_kht_laskutettu_ind_korotus          14.84674329501915670000M
           :muutostyot_laskutettu                             1000.0M
           :kht_laskutettu                                    35000.0M
           :akilliset_hoitotyot_laskutettu_ind_korotettuna    0.0M
           :kaikki_laskutetaan                                -23265.6239047196267293788000000M
           :kht_laskutetaan_ind_korotettuna                   3560.3448275862068000M
           :akilliset_hoitotyot_laskutettu_ind_korotus        0.0M
           :vahinkojen_korjaukset_laskutettu_ind_korotettuna  0.0M
           :vahinkojen_korjaukset_laskutetaan                 0.0M
           :yht_laskutettu_ind_korotus                        9.57854406130265900000M
           :kaikki_laskutettu                                 39042.24137931034423443500M
           :muutostyot_laskutettu_ind_korotus                 14.36781609195400000M
           :kht_laskutetaan                                   3500.0M
           :vahinkojen_korjaukset_laskutetaan_ind_korotettuna 0.0M
           :kaikki_paitsi_kht_laskutettu                      4042.24137931034423443500M
           :sakot_laskutettu_ind_korotettuna                  -99.52107279693486590000M
           :kht_laskutettu_ind_korotus                        127.39463601532507773500M
           :bonukset_laskutettu                               0.0M
           :sakot_laskutetaan                                 0.0M
           :bonukset_laskutettu_ind_korotus                   0.0M
           :kht_laskutetaan_ind_korotus                       60.3448275862068000M
           :yht_laskutetaan                                   0.0M
           :erilliskustannukset_laskutettu_ind_korotus        -9.57854406130263640000M
           :muutostyot_laskutetaan_ind_korotus                17.24137931034480000M
           :suolasakot_laskutettu_ind_korotettuna             0.0M
           :muutostyot_laskutetaan                            1000.0M
           :suolasakot_laskutetaan_ind_korotus                -104.5210727969348753088000000M
           :erilliskustannukset_laskutetaan                   1000.0M
           :yht_laskutetaan_ind_korotettuna                   0.0M
           :nimi                                              "Talvihoito"
           :yht_laskutettu                                    2000.0M
           :bonukset_laskutetaan_ind_korotettuna              1004.06958187041174593000M
           :muutostyot_laskutettu_ind_korotettuna             1014.36781609195400000M
           :erilliskustannukset_laskutettu_ind_korotettuna    990.42145593869736360000M
           :muutostyot_laskutetaan_ind_korotettuna            1017.24137931034480000M
           :erilliskustannukset_laskutetaan_ind_korotettuna   1017.2413793103448000M
           :sakot_laskutetaan_ind_korotus                     0.0M
           :kaikki_paitsi_kht_laskutetaan                     -26765.6239047196267293788000000M
           :akilliset_hoitotyot_laskutetaan_ind_korotus       0.0M
           :lampotila_puuttuu                                 false
           :perusluku                                         104.4M
           :suolasakot_laskutettu                             0.0M
           :kaikki_paitsi_kht_laskutetaan_ind_korotus         -65.9687323058335293788000000M
           :bonukset_laskutetaan_ind_korotus                  4.06958187041174593000M
           :akilliset_hoitotyot_laskutettu                    0.0M
           :akilliset_hoitotyot_laskutetaan_ind_korotettuna   0.0M
           :suolasakot_laskutetaan_ind_korotettuna            -29864.5210727969348753088000000M
           :kaikki_laskutettu_ind_korotus                     142.24137931034423443500M
           :vahinkojen_korjaukset_laskutettu_ind_korotus      0.0M
           :erilliskustannukset_laskutetaan_ind_korotus       17.2413793103448000M
           :tuotekoodi                                        "23100"
           :akilliset_hoitotyot_laskutetaan                   0.0M
           :sakot_laskutettu_ind_korotus                      0.47892720306513410000M
           :sakot_laskutettu                                  -100.0M
           :erilliskustannukset_laskutettu                    1000.0M
           :vahinkojen_korjaukset_laskutetaan_ind_korotus     0.0M
           :suolasakko_kaytossa                               true
           :kht_laskutettu_ind_korotettuna                    35127.39463601532507773500M
           :suolasakot_laskutettu_ind_korotus                 0.0M
           :tpi                                               4
           :vahinkojen_korjaukset_laskutettu                  0.0M
           :yht_laskutetaan_ind_korotus                       0.0M}

          odotetut-liikenneymparisto
          {:bonukset_laskutettu_ind_korotettuna               0.0M
           :bonukset_laskutetaan                              0.0M
           :yht_laskutettu_ind_korotettuna                    995.21072796934865900000M
           :suolasakot_laskutetaan                            0.0M
           :kaikki_laskutetaan_ind_korotus                    103.44827586206880000M
           :sakot_laskutetaan_ind_korotettuna                 0.0M
           :kaikki_paitsi_kht_laskutettu_ind_korotus          20.11494252873560900000M
           :muutostyot_laskutettu                             3000.0M
           :kht_laskutettu                                    0.0M
           :akilliset_hoitotyot_laskutettu_ind_korotettuna    1014.36781609195400000M
           :kaikki_laskutetaan                                13103.44827586206880000M
           :kht_laskutetaan_ind_korotettuna                   0.0M
           :akilliset_hoitotyot_laskutettu_ind_korotus        14.36781609195400000M
           :vahinkojen_korjaukset_laskutettu_ind_korotettuna  0.0M
           :vahinkojen_korjaukset_laskutetaan                 1000.0M
           :yht_laskutettu_ind_korotus                        -4.78927203065134100000M
           :kaikki_laskutettu                                 3820.11494252873560900000M
           :muutostyot_laskutettu_ind_korotus                 14.36781609195400000M
           :kht_laskutetaan                                   0.0M
           :vahinkojen_korjaukset_laskutetaan_ind_korotettuna 1017.24137931034480000M
           :kaikki_paitsi_kht_laskutettu                      3820.11494252873560900000M
           :sakot_laskutettu_ind_korotettuna                  -1203.8314176245210500M
           :kht_laskutettu_ind_korotus                        0.0M
           :bonukset_laskutettu                               0.0M
           :sakot_laskutetaan                                 0.0M
           :bonukset_laskutettu_ind_korotus                   0.0M
           :kht_laskutetaan_ind_korotus                       0.0M
           :yht_laskutetaan                                   3000.0M
           :erilliskustannukset_laskutettu_ind_korotus        0.0M
           :muutostyot_laskutetaan_ind_korotus                17.24137931034480000M
           :suolasakot_laskutettu_ind_korotettuna             0.0M
           :muutostyot_laskutetaan                            7000.0M
           :suolasakot_laskutetaan_ind_korotus                0.0M
           :erilliskustannukset_laskutetaan                   0.0M
           :yht_laskutetaan_ind_korotettuna                   3051.7241379310344000M
           :nimi                                              "Liikenneympäristön hoito"
           :yht_laskutettu                                    1000.0M
           :bonukset_laskutetaan_ind_korotettuna              0.0M
           :muutostyot_laskutettu_ind_korotettuna             3014.36781609195400000M
           :erilliskustannukset_laskutettu_ind_korotettuna    0.0M
           :muutostyot_laskutetaan_ind_korotettuna            7017.24137931034480000M
           :erilliskustannukset_laskutetaan_ind_korotettuna   0.0M
           :sakot_laskutetaan_ind_korotus                     0.0M
           :kaikki_paitsi_kht_laskutetaan                     13103.44827586206880000M
           :akilliset_hoitotyot_laskutetaan_ind_korotus       17.24137931034480000M
           :lampotila_puuttuu                                 false
           :perusluku                                         104.4M
           :suolasakot_laskutettu                             0.0M
           :kaikki_paitsi_kht_laskutetaan_ind_korotus         103.44827586206880000M
           :bonukset_laskutetaan_ind_korotus                  0.0M
           :akilliset_hoitotyot_laskutettu                    1000.0M
           :akilliset_hoitotyot_laskutetaan_ind_korotettuna   2017.24137931034480000M
           :suolasakot_laskutetaan_ind_korotettuna            0.0M
           :kaikki_laskutettu_ind_korotus                     20.11494252873560900000M
           :vahinkojen_korjaukset_laskutettu_ind_korotus      0.0M
           :erilliskustannukset_laskutetaan_ind_korotus       0.0M
           :tuotekoodi                                        "23110"
           :akilliset_hoitotyot_laskutetaan                   2000.0M
           :sakot_laskutettu_ind_korotus                      -3.8314176245210500M
           :sakot_laskutettu                                  -1200.0M
           :erilliskustannukset_laskutettu                    0.0M
           :vahinkojen_korjaukset_laskutetaan_ind_korotus     17.24137931034480000M
           :suolasakko_kaytossa                               true
           :kht_laskutettu_ind_korotettuna                    0.0M
           :suolasakot_laskutettu_ind_korotus                 0.0M
           :tpi                                               5
           :vahinkojen_korjaukset_laskutettu                  0.0M
           :yht_laskutetaan_ind_korotus                       51.7241379310344000M}

          odotetut-soratiet
          {:bonukset_laskutettu_ind_korotettuna               0.0M
           :bonukset_laskutetaan                              0.0M
           :yht_laskutettu_ind_korotettuna                    0.0M
           :suolasakot_laskutetaan                            0.0M
           :kaikki_laskutetaan_ind_korotus                    141.3793103448273600M
           :sakot_laskutetaan_ind_korotettuna                 -1831.0344827586206400M
           :kaikki_paitsi_kht_laskutettu_ind_korotus          0.0M
           :muutostyot_laskutettu                             0.0M
           :kht_laskutettu                                    100000.0M
           :akilliset_hoitotyot_laskutettu_ind_korotettuna    0.0M
           :kaikki_laskutetaan                                8341.3793103448273600M
           :kht_laskutetaan_ind_korotettuna                   10172.4137931034480000M
           :akilliset_hoitotyot_laskutettu_ind_korotus        0.0M
           :vahinkojen_korjaukset_laskutettu_ind_korotettuna  0.0M
           :vahinkojen_korjaukset_laskutetaan                 0.0M
           :yht_laskutettu_ind_korotus                        0.0M
           :kaikki_laskutettu                                 100363.98467432950022210000M
           :muutostyot_laskutettu_ind_korotus                 0.0M
           :kht_laskutetaan                                   10000.0M
           :vahinkojen_korjaukset_laskutetaan_ind_korotettuna 0.0M
           :kaikki_paitsi_kht_laskutettu                      363.98467432950022210000M
           :sakot_laskutettu_ind_korotettuna                  0.0M
           :kht_laskutettu_ind_korotus                        363.98467432950022210000M
           :bonukset_laskutettu                               0.0M
           :sakot_laskutetaan                                 -1800.0M
           :bonukset_laskutettu_ind_korotus                   0.0M
           :kht_laskutetaan_ind_korotus                       172.4137931034480000M
           :yht_laskutetaan                                   0.0M
           :erilliskustannukset_laskutettu_ind_korotus        0.0M
           :muutostyot_laskutetaan_ind_korotus                0.0M
           :suolasakot_laskutettu_ind_korotettuna             0.0M
           :muutostyot_laskutetaan                            0.0M
           :suolasakot_laskutetaan_ind_korotus                0.0M
           :erilliskustannukset_laskutetaan                   0.0M
           :yht_laskutetaan_ind_korotettuna                   0.0M
           :nimi                                              "Soratien hoito"
           :yht_laskutettu                                    0.0M
           :bonukset_laskutetaan_ind_korotettuna              0.0M
           :muutostyot_laskutettu_ind_korotettuna             0.0M
           :erilliskustannukset_laskutettu_ind_korotettuna    0.0M
           :muutostyot_laskutetaan_ind_korotettuna            0.0M
           :erilliskustannukset_laskutetaan_ind_korotettuna   0.0M
           :sakot_laskutetaan_ind_korotus                     -31.0344827586206400M
           :kaikki_paitsi_kht_laskutetaan                     -1658.6206896551726400M
           :akilliset_hoitotyot_laskutetaan_ind_korotus       0.0M
           :lampotila_puuttuu                                 false
           :perusluku                                         104.4M
           :suolasakot_laskutettu                             0.0M
           :kaikki_paitsi_kht_laskutetaan_ind_korotus         -31.0344827586206400M
           :bonukset_laskutetaan_ind_korotus                  0.0M
           :akilliset_hoitotyot_laskutettu                    0.0M
           :akilliset_hoitotyot_laskutetaan_ind_korotettuna   0.0M
           :suolasakot_laskutetaan_ind_korotettuna            0.0M
           :kaikki_laskutettu_ind_korotus                     363.98467432950022210000M
           :vahinkojen_korjaukset_laskutettu_ind_korotus      0.0M
           :erilliskustannukset_laskutetaan_ind_korotus       0.0M
           :tuotekoodi                                        "23120"
           :akilliset_hoitotyot_laskutetaan                   0.0M
           :sakot_laskutettu_ind_korotus                      0.0M
           :sakot_laskutettu                                  0.0M
           :erilliskustannukset_laskutettu                    0.0M
           :vahinkojen_korjaukset_laskutetaan_ind_korotus     0.0M
           :suolasakko_kaytossa                               true
           :kht_laskutettu_ind_korotettuna                    100363.98467432950022210000M
           :suolasakot_laskutettu_ind_korotus                 0.0M
           :tpi                                               6
           :vahinkojen_korjaukset_laskutettu                  0.0M
           :yht_laskutetaan_ind_korotus                       0.0M}

          ]

      (is (= (count haetut-tiedot-oulu-ilman-tpita)
             (count haetut-tiedot-kajaani-ilman-tpita)))
      (mapv (fn [eka toka]
              (testi/tarkista-map-arvot eka toka))
            haetut-tiedot-oulu-ilman-tpita haetut-tiedot-kajaani-ilman-tpita)
      (testing "Talvihoito"
        (testi/tarkista-map-arvot odotetut-talvihoito haetut-tiedot-oulu-talvihoito))
      (testing "Liikenneympäristön hoito"
        (testi/tarkista-map-arvot odotetut-liikenneymparisto haetut-tiedot-oulu-liikenneymparisto))
      (testing "Soratien hoito"
        (testi/tarkista-map-arvot odotetut-soratiet haetut-tiedot-oulu-soratiet)))))


;; HAR-1959: Laskutusyhteenveto ottaa talvisuolasakon väärään hoitokauteen loka-joulukuussa
(deftest suolasakko-oikean-vuoden-laskutusyhteenvedossa
  (testing "suolasakko-oikean-vuoden-laskutusyhteenvedossa"
    (let [haetut-tiedot-oulu (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma)
                               +kayttaja-jvh+
                               {:urakka-id @oulun-alueurakan-2014-2019-id
                                :alkupvm   (pvm/->pvm "1.10.2014")
                                :loppupvm (pvm/->pvm "31.10.2014")})
          haetut-tiedot-oulu-talvihoito (first (filter #(= (:tuotekoodi %) "23100") haetut-tiedot-oulu))]

      (is (= (:suolasakko_kaytossa haetut-tiedot-oulu-talvihoito) true) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutettu haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutettu_ind_korotettuna haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutettu_ind_korotus haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutetaan haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutetaan_ind_korotettuna haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutetaan_ind_korotus haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa"))))

(deftest suolasakko-oikein-hoitokauden-laskutusyhteenvedossa ;HAR-3477
  (testing "suolasakko-oikein-hoitokauden-laskutusyhteenvedossa"
    (let [haetut-tiedot-oulu (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma)
                               +kayttaja-jvh+
                               {:urakka-id @oulun-alueurakan-2014-2019-id
                                :alkupvm   (pvm/->pvm "1.10.2014")
                                :loppupvm (pvm/->pvm "30.9.2015")})
          haetut-tiedot-oulu-talvihoito (first (filter #(= (:tuotekoodi %) "23100") haetut-tiedot-oulu))]

      (is (= (:suolasakko_kaytossa haetut-tiedot-oulu-talvihoito) true) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutettu haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutettu_ind_korotettuna haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (= (:suolasakot_laskutettu_ind_korotus haetut-tiedot-oulu-talvihoito) 0.0M) "suolasakko laskutusyhteenvedossa")
      (is (=marginaalissa? (:suolasakot_laskutetaan haetut-tiedot-oulu-talvihoito) -29760.0M) "suolasakko laskutusyhteenvedossa")
      (is (=marginaalissa? (:suolasakot_laskutetaan_ind_korotettuna haetut-tiedot-oulu-talvihoito) -29864.5M) "suolasakko laskutusyhteenvedossa")
      (is (=marginaalissa? (:suolasakot_laskutetaan_ind_korotus haetut-tiedot-oulu-talvihoito) -104.5M) "suolasakko laskutusyhteenvedossa"))))


(deftest kuun-viimeisen-paivan-yht-oikein-laskutusyhteenvedossa ;HAR-3965
  (testing "kuun-viimeisen-paivan-yht-oikein-laskutusyhteenvedossa"
    (let [haetut-tiedot-oulu (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma)
                               +kayttaja-jvh+
                               {:urakka-id @oulun-alueurakan-2014-2019-id
                                :alkupvm   (pvm/->pvm "1.11.2016")
                                :loppupvm (pvm/->pvm "30.11.2016")})
          haetut-tiedot-oulu-liikenneympariston-hoito (first (filter #(= (:tuotekoodi %) "23110") haetut-tiedot-oulu))]

      (is (= (:yht_laskutetaan haetut-tiedot-oulu-liikenneympariston-hoito) 7882.5M) ":yht_laskutetaan laskutusyhteenvedossa")
      (is (= (:yht_laskutetaan_ind_korotus haetut-tiedot-oulu-liikenneympariston-hoito) nil) ":yht_laskutetaan laskutusyhteenvedossa")
      (is (= (:yht_laskutetaan_ind_korotettuna haetut-tiedot-oulu-liikenneympariston-hoito) nil) ":yht_laskutetaan laskutusyhteenvedossa"))))

(deftest laskutusyhteenvedon-cache
  (testing "laskutusyhteenvedon-cache"
    (let [urakka-id @oulun-alueurakan-2014-2019-id
          varmista-tyhjyys (u "DELETE FROM laskutusyhteenveto_cache WHERE urakka = " urakka-id ";")
          cache-tyhja (first (q-map "SELECT * FROM laskutusyhteenveto_cache WHERE urakka = " urakka-id ";"))
          eka-ajo (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma) +kayttaja-jvh+
                               {:urakka-id urakka-id :alkupvm   (pvm/->pvm "1.8.2015")
                                :loppupvm (pvm/->pvm "31.8.2015")})
          toka-ajo (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma) +kayttaja-jvh+
                               {:urakka-id urakka-id :alkupvm   (pvm/->pvm "1.8.2015")
                                :loppupvm (pvm/->pvm "31.8.2015")})
          haetut-tiedot-oulu (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                               (:db jarjestelma) +kayttaja-jvh+
                               {:urakka-id urakka-id :alkupvm   (pvm/->pvm "1.8.2015")
                                :loppupvm (pvm/->pvm "31.8.2015")})
          cachesta-haettu-kysely (first (q-map "select y.urakka,
           sum((y.rivi).yht_laskutettu) as yht_laskutettu,
           sum((y.rivi).yht_laskutetaan) as yht_laskutetaan,
           sum((y.rivi).kaikki_laskutettu) as kaikki_laskutettu,
           sum((y.rivi).kaikki_laskutetaan) as kaikki_laskutetaan,
            sum((y.rivi).kaikki_laskutettu_ind_korotus) as kaikki_laskutettu_ind_korotus,
            sum((y.rivi).kaikki_laskutetaan_ind_korotus) as kaikki_laskutetaan_ind_korotus
            FROM (select unnest(x.rivit) as rivi, x.nimi as urakka
           FROM (SELECT c.urakka, c.alkupvm, c.loppupvm, c.tallennettu, c.rivit, u.nimi
                   FROM laskutusyhteenveto_cache c
                        JOIN urakka u ON u.id = c.urakka
                  WHERE c.urakka = " urakka-id " AND c.alkupvm = '2015-8-01') x) y WHERE (y.rivi).tuotekoodi = '23110' GROUP BY y.urakka order by y.urakka;"))
          haetut-tiedot-oulu-liikenneympariston-hoito (first (filter #(= (:tuotekoodi %) "23110") haetut-tiedot-oulu))
          cache-count (:count (first (q-map "SELECT count(*) FROM laskutusyhteenveto_cache c WHERE c.urakka = " urakka-id " AND c.alkupvm = '2015-8-01';")))]
      (is (= cache-count 1) "Monta kyselyä samoin parametrein, silti vain yksi cachessa")
      (is (nil? cache-tyhja) "cache tyhjä ennen kyselyn ajoa")
      (is (some? cachesta-haettu-kysely) "cache ei tyhjä ennen kyselyn ajoa")
      (is (= eka-ajo toka-ajo haetut-tiedot-oulu) "Saman laskutusyhteenvedon eri ajokerrat antaa saman tulokset")
      (is (= 1000.0M
             (:yht_laskutettu haetut-tiedot-oulu-liikenneympariston-hoito)
             (:yht_laskutettu cachesta-haettu-kysely)) ":yht_laskutettu laskutusyhteenvedossa")
      (is (= 3000.0M
             (:yht_laskutetaan haetut-tiedot-oulu-liikenneympariston-hoito)
             (:yht_laskutetaan cachesta-haettu-kysely)) ":yht_laskutetaan laskutusyhteenvedossa")
      (is (= 3820.11494252873560900000M
             (:kaikki_laskutettu haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutettu cachesta-haettu-kysely)) ":kaikki_laskutettu laskutusyhteenvedossa")
      (is (= 13103.44827586206880000M
             (:kaikki_laskutetaan haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutetaan cachesta-haettu-kysely)) ":kaikki_laskutetaan laskutusyhteenvedossa")
      (is (= 20.11494252873560900000M
             (:kaikki_laskutettu_ind_korotus haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutettu_ind_korotus cachesta-haettu-kysely)) ":kaikki_laskutettu_ind_korotus laskutusyhteenvedossa")
      (is (= 103.44827586206880000M
             (:kaikki_laskutetaan_ind_korotus haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutetaan_ind_korotus cachesta-haettu-kysely)) ":kaikki_laskutetaan_ind_korotus laskutusyhteenvedossa"))))

(deftest laskutusyhteenvedon-cache-tyhjenee-jos-yksikkohinta-muuttuu
  (testing "laskutusyhteenvedon-cache-ja-triggerit"
    (let [urakka-id @oulun-alueurakan-2014-2019-id
          varmista-tyhjyys (u "DELETE FROM laskutusyhteenveto_cache WHERE urakka = " urakka-id ";")
          cache-tyhja (first (q-map "SELECT * FROM laskutusyhteenveto_cache WHERE urakka = " urakka-id ";"))
          eka-tietojen-haku (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                              (:db jarjestelma) +kayttaja-jvh+
                              {:urakka-id urakka-id :alkupvm (pvm/->pvm "1.8.2015")
                               :loppupvm (pvm/->pvm "31.8.2015")})
          cachesta-haettu-kysely (first (q-map "select y.urakka,
           sum((y.rivi).yht_laskutettu) as yht_laskutettu,
           sum((y.rivi).yht_laskutetaan) as yht_laskutetaan,
           sum((y.rivi).kaikki_laskutettu) as kaikki_laskutettu,
           sum((y.rivi).kaikki_laskutetaan) as kaikki_laskutetaan,
            sum((y.rivi).kaikki_laskutettu_ind_korotus) as kaikki_laskutettu_ind_korotus,
            sum((y.rivi).kaikki_laskutetaan_ind_korotus) as kaikki_laskutetaan_ind_korotus
            FROM (select unnest(x.rivit) as rivi, x.nimi as urakka
           FROM (SELECT c.urakka, c.alkupvm, c.loppupvm, c.tallennettu, c.rivit, u.nimi
                   FROM laskutusyhteenveto_cache c
                        JOIN urakka u ON u.id = c.urakka
                  WHERE c.urakka = " urakka-id " AND c.alkupvm = '2015-8-01') x) y WHERE (y.rivi).tuotekoodi = '23110' GROUP BY y.urakka order by y.urakka;"))
          haetut-tiedot-oulu-liikenneympariston-hoito (first (filter #(= (:tuotekoodi %) "23110") eka-tietojen-haku))
          cache-count (:count (first (q-map "SELECT count(*) FROM laskutusyhteenveto_cache c WHERE c.urakka = " urakka-id " AND c.alkupvm = '2015-8-01';")))

          ;; muuta yks.hintaa tehtävälle Metsän harvennus ko. ajalle --> cachen pitäisi tyhjentyä
          metsanharvennus-yksikkohinta-hyotykuorma [{:yhteensa-kkt-1-9 122040, :yhteensa 162720, :yhteensa-kkt-10-12 40680,
                                                     :loppupvm (pvm/->pvm "31.12.2014") :yksikko "ha", :koskematon true,
                                                     :tehtava 1432, :urakka 4, :yksikkohinta 678, :maara 600,
                                                     :tehtavan_nimi "Metsän harvennus", :sopimus 2, :maara-kkt-1-9 180,
                                                     :alkupvm (pvm/->pvm "1.10.2014") :tehtavan_id 1432}
                                                    {:yhteensa-kkt-1-9 122040, :yhteensa 162720, :yhteensa-kkt-10-12 40680,
                                                     :loppupvm (pvm/->pvm "30.09.2015") :yksikko "ha", :koskematon true,
                                                     :tehtava 1432, :urakka 4, :yksikkohinta 678, :maara 1800,
                                                     :tehtavan_nimi "Metsän harvennus", :sopimus 2, :maara-kkt-1-9 180,
                                                     :alkupvm (pvm/->pvm "1.1.2015") :tehtavan_id 1432}]]
      (is (= cache-count 1) "Monta kyselyä samoin parametrein, silti vain yksi cachessa")
      (is (nil? cache-tyhja) "cache tyhjä ennen kyselyn ajoa")
      (is (some? cachesta-haettu-kysely) "cache ei tyhjä ennen kyselyn ajoa")

      (is (= 1000.0M
             (:yht_laskutettu haetut-tiedot-oulu-liikenneympariston-hoito)
             (:yht_laskutettu cachesta-haettu-kysely)) ":yht_laskutettu laskutusyhteenvedossa")
      (is (= 3000.0M
             (:yht_laskutetaan haetut-tiedot-oulu-liikenneympariston-hoito)
             (:yht_laskutetaan cachesta-haettu-kysely)) ":yht_laskutetaan laskutusyhteenvedossa")
      (is (= 3820.11494252873560900000M
             (:kaikki_laskutettu haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutettu cachesta-haettu-kysely)) ":kaikki_laskutettu laskutusyhteenvedossa")
      (is (= 13103.44827586206880000M
             (:kaikki_laskutetaan haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutetaan cachesta-haettu-kysely)) ":kaikki_laskutetaan laskutusyhteenvedossa")
      (is (= 20.11494252873560900000M
             (:kaikki_laskutettu_ind_korotus haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutettu_ind_korotus cachesta-haettu-kysely)) ":kaikki_laskutettu_ind_korotus laskutusyhteenvedossa")
      (is (= 103.44827586206880000M
             (:kaikki_laskutetaan_ind_korotus haetut-tiedot-oulu-liikenneympariston-hoito)
             (:kaikki_laskutetaan_ind_korotus cachesta-haettu-kysely)) ":kaikki_laskutetaan_ind_korotus laskutusyhteenvedossa")
      (let [yksikkohintaa-muutettu
            (kutsu-palvelua (:http-palvelin jarjestelma)
                            :tallenna-urakan-yksikkohintaiset-tyot +kayttaja-jvh+
                            {:urakka-id @oulun-alueurakan-2014-2019-id
                             :sopimusnumero @oulun-alueurakan-2014-2019-paasopimuksen-id
                             :tyot metsanharvennus-yksikkohinta-hyotykuorma})
            cache-tyhja-koska-yht-trigger (first (q-map "SELECT * FROM laskutusyhteenveto_cache WHERE urakka = " urakka-id ";"))
            toka-tietojen-haku (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                                 (:db jarjestelma) +kayttaja-jvh+
                                 {:urakka-id urakka-id :alkupvm (pvm/->pvm "1.8.2015")
                                  :loppupvm (pvm/->pvm "31.8.2015")})
            toka-haetut-tiedot-oulu-liikenneympariston-hoito (first (filter #(= (:tuotekoodi %) "23110") toka-tietojen-haku))
            cachesta-haettu-kysely-triggerin-jalkeen (first (q-map "select y.urakka,
           sum((y.rivi).yht_laskutettu) as yht_laskutettu,
           sum((y.rivi).yht_laskutetaan) as yht_laskutetaan,
           sum((y.rivi).kaikki_laskutettu) as kaikki_laskutettu,
           sum((y.rivi).kaikki_laskutetaan) as kaikki_laskutetaan,
            sum((y.rivi).kaikki_laskutettu_ind_korotus) as kaikki_laskutettu_ind_korotus,
            sum((y.rivi).kaikki_laskutetaan_ind_korotus) as kaikki_laskutetaan_ind_korotus
            FROM (select unnest(x.rivit) as rivi, x.nimi as urakka
           FROM (SELECT c.urakka, c.alkupvm, c.loppupvm, c.tallennettu, c.rivit, u.nimi
                   FROM laskutusyhteenveto_cache c
                        JOIN urakka u ON u.id = c.urakka
                  WHERE c.urakka = " urakka-id " AND c.alkupvm = '2015-8-01') x) y WHERE (y.rivi).tuotekoodi = '23110' GROUP BY y.urakka order by y.urakka;"))]
        (is (nil? cache-tyhja-koska-yht-trigger) "cache-tyhja-koska-yht-trigger")
        (is (= 6780.0M
               (:yht_laskutettu toka-haetut-tiedot-oulu-liikenneympariston-hoito)
               (:yht_laskutettu cachesta-haettu-kysely-triggerin-jalkeen)) ":yht_laskutettu laskutusyhteenvedossa")
        (is (= 20340.0M
               (:yht_laskutetaan toka-haetut-tiedot-oulu-liikenneympariston-hoito)
               (:yht_laskutetaan cachesta-haettu-kysely-triggerin-jalkeen)) ":yht_laskutetaan laskutusyhteenvedossa")
        (is (= 9572.43295019157085802000M
               (:kaikki_laskutettu toka-haetut-tiedot-oulu-liikenneympariston-hoito)
               (:kaikki_laskutettu cachesta-haettu-kysely-triggerin-jalkeen)) ":kaikki_laskutettu laskutusyhteenvedossa")
        (is (= 30742.41379310344763200M
               (:kaikki_laskutetaan toka-haetut-tiedot-oulu-liikenneympariston-hoito)
               (:kaikki_laskutetaan cachesta-haettu-kysely-triggerin-jalkeen)) ":kaikki_laskutetaan laskutusyhteenvedossa")
        (is (= -7.56704980842914198000M
               (:kaikki_laskutettu_ind_korotus toka-haetut-tiedot-oulu-liikenneympariston-hoito)
               (:kaikki_laskutettu_ind_korotus cachesta-haettu-kysely-triggerin-jalkeen)) ":kaikki_laskutettu_ind_korotus laskutusyhteenvedossa")
        (is (= 402.41379310344763200M
               (:kaikki_laskutetaan_ind_korotus toka-haetut-tiedot-oulu-liikenneympariston-hoito)
               (:kaikki_laskutetaan_ind_korotus cachesta-haettu-kysely-triggerin-jalkeen)) ":kaikki_laskutetaan_ind_korotus laskutusyhteenvedossa")))))
