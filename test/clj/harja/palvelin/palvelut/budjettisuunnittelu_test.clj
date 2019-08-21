(ns harja.palvelin.palvelut.budjettisuunnittelu-test
  (:require [clojure.test :refer :all]
            [harja.palvelin.palvelut.budjettisuunnittelu :refer :all]
            [harja.palvelin.palvelut.pois-kytketyt-ominaisuudet :as pois-kytketyt-ominaisuudet]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (luo-testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :pois-kytketyt-ominaisuudet (component/using
                                                      (pois-kytketyt-ominaisuudet/->PoisKytketytOminaisuudet #{})
                                                      [:http-palvelin])
                        :budjetoidut-tyot (component/using
                                            (->Budjettisuunnittelu)
                                            [:http-palvelin :db :pois-kytketyt-ominaisuudet])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))


(def budjetoidut-tyot {:kiinteahintaiset-tyot
                                              [{:kuukausi            1,
                                                :vuosi               2021,
                                                :summa               333,
                                                :maksupvm            "2021-01-15",
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104"),
                                                :sopimus             (hae-oulun-maanteiden-hoitourakan-2019-2024-sopimus-id)
                                                }
                                               {:vuosi               2021,
                                                :kuukausi            2,
                                                :summa               333,
                                                :maksupvm            "2021-02-15",
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104"),
                                                :sopimus             (hae-oulun-maanteiden-hoitourakan-2019-2024-sopimus-id),
                                                }
                                               {:vuosi               2021,
                                                :kuukausi            3,
                                                :summa               666,
                                                :maksupvm            "2021-03-15",
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104"),
                                                :sopimus             (hae-oulun-maanteiden-hoitourakan-2019-2024-sopimus-id),
                                                }]
                       :kustannusarvioidut-tyot
                                              [{:kuukausi            5
                                                :vuosi               2021,
                                                :summa               66,
                                                :maksupvm            nil,
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23116"),
                                                :tehtavaryhma        nil,
                                                :tehtava             (hae-toimenpidekoodin-id "Äkillinen hoitotyö" "23116"),
                                                :tyyppi              "akillinen-hoitotyo"
                                                }
                                               {:kuukausi            5
                                                :vuosi               2021,
                                                :summa               600,
                                                :maksupvm            nil,
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23124"),
                                                :tehtavaryhma        nil,
                                                :tehtava             (hae-toimenpidekoodin-id "Äkillinen hoitotyö" "23124"),
                                                :tyyppi              "akillinen-hoitotyo"
                                                }
                                               {:kuukausi            5
                                                :vuosi               2021,
                                                :summa               66,
                                                :maksupvm            nil,
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23124"),
                                                :tehtavaryhma        nil,
                                                :tehtava             (hae-toimenpidekoodin-id "Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen" "23124"),
                                                :tyyppi              "vahinkojen-korjaukset"
                                                }
                                               {:kuukausi            5
                                                :vuosi               2021,
                                                :summa               500,
                                                :maksupvm            nil,
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23151"),
                                                :tehtavaryhma        (hae-tehtavaryhman-id "Muut liik.ymp.hoitosasiat"),
                                                :tehtava             nil,
                                                :tyyppi              "laskutettava-tyo"
                                                }
                                               {:summa               166,
                                                :vuosi               2021,:kuukausi            5
                                                :maksupvm            nil,
                                                :toimenpideinstanssi (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23151"),
                                                :tehtavaryhma        nil,
                                                :tehtava             (hae-toimenpidekoodin-id "Hoito- ja korjaustöiden pientarvikevarasto" "23151")
                                                :tyyppi              "laskutettava-tyo"
                                                }]
                       :yksikkohintaiset-tyot [{:kuukausi           5,
                                                :vuosi              2021,
                                                :alkupvm            #inst "2021-05-01T00:00:00.000-00:00",
                                                :loppupvm           #inst "2021-05-31T00:00:00.000-00:00",
                                                :maara              12,
                                                :yksikko            "h",
                                                :yksikkohinta       55,
                                                :arvioitu_kustannus 660,
                                                :tehtava            (hae-toimenpidekoodin-id "Hoitourakan työnjohto" "23151"),
                                                :tehtavan_nimi      "Hoitourakan työnjohto",
                                                :id                 13579,
                                                :sopimus            (hae-oulun-maanteiden-hoitourakan-2019-2024-sopimus-id)
                                                }
                                               {:kuukausi           5,
                                                :vuosi              2021,
                                                :alkupvm            #inst "2021-05-01T00:00:00.000-00:00",
                                                :loppupvm           #inst "2021-05-31T00:00:00.000-00:00",
                                                :maara              3,
                                                :yksikko            "-",
                                                :yksikkohinta       2,
                                                :arvioitu_kustannus 6,
                                                :tehtava            (hae-toimenpidekoodin-id "Hoito- ja korjaustöiden pientarvikevarasto" "23151"),
                                                :tehtavan_nimi      "Hoito- ja korjaustöiden pientarvikevarasto",
                                                :id                 13580,
                                                :sopimus            (hae-oulun-maanteiden-hoitourakan-2019-2024-sopimus-id)
                                                }]})


;; Testeissä hyödynnetään etukäteen tallennettua testidataa.

;; Haetaan budjetoidut työt. Lopputuloksen pitäisi olla:
;; Kiinteähintaiset, kustannusarvioidut ja yksikköhintaiset työt on palautuvat oikein.
;; Puutteellisilla oikeuksilla saadaan virhe, urakan vastuuhenkilö saa tiedot.
(deftest hae-budjetoidut-tyot-testi
  (let [palvelun-palauttamat-budjetoidut-tyot (kutsu-palvelua (:http-palvelin jarjestelma)
                                                              :budjetoidut-tyot +kayttaja-jvh+ {:urakka-id @oulun-maanteiden-hoitourakan-2019-2024-id})
        kiinteahintaiset-tyot (:kiinteahintaiset-tyot palvelun-palauttamat-budjetoidut-tyot)
        kustannusarvioidut-tyot (:kustannusarvioidut-tyot palvelun-palauttamat-budjetoidut-tyot)
        yksikkohintaiset-tyot (:yksikkohintaiset-tyot palvelun-palauttamat-budjetoidut-tyot)
        akilliset-hoitotyot (filter #(= "akillinen-hoitotyo" (:tyyppi %)) kustannusarvioidut-tyot)
        vahinkojen-korjaukset (filter #(= "vahinkojen-korjaukset" (:tyyppi %)) kustannusarvioidut-tyot)
        hallinnolliset-tyot (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23151") (:toimenpideinstanssi %)) kustannusarvioidut-tyot)
        talvihoidon-tyot (clojure.set/union (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104") (:toimenpideinstanssi %)) kiinteahintaiset-tyot)
                                        (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104") (:toimenpideinstanssi %)) kustannusarvioidut-tyot))
        urakan-vastuuhenkilo (oulun-2019-urakan-urakoitsijan-urakkavastaava)
        vaara-urakan-vastuuhenkilo (ei-ole-oulun-urakan-urakoitsijan-urakkavastaava)
        budjetoidut-tyot-kutsuja-urakanvalvoja (kutsu-palvelua (:http-palvelin jarjestelma) :budjetoidut-tyot
                                                               urakan-vastuuhenkilo {:urakka-id @oulun-maanteiden-hoitourakan-2019-2024-id})]

    (is (thrown? RuntimeException (kutsu-palvelua (:http-palvelin jarjestelma) :budjetoidut-tyot
                                                  vaara-urakan-vastuuhenkilo {:urakka-id @oulun-maanteiden-hoitourakan-2019-2024-id})) "Ilman urakkakohtaisia oikeuksia, Urakan vastuuhenkilö-roolilla ei saa tietoja budjetoiduista töistä.")

    (is (= palvelun-palauttamat-budjetoidut-tyot budjetoidut-tyot-kutsuja-urakanvalvoja) "Järjestelmävastaava ja urakan vastuuhenkilö saavat samat tiedot budjetoiduista töistä.")

    (is (= (reduce + (map :summa kiinteahintaiset-tyot)) 16666M) (str "Kiinteähintaiset työt yhteensä (e)." (reduce + (map :summa kiinteahintaiset-tyot))))
    (is (= (reduce + (map :summa kustannusarvioidut-tyot)) 1866M) (str "Kustannusarvioidut työt yhteensä (e). " (reduce + (map :summa kustannusarvioidut-tyot))))
    (is (= (reduce + (map :arvioitu_kustannus yksikkohintaiset-tyot)) 10485M) (str "Yksikköhintaiset työt yhteensä (e). " (reduce + (map :arvioitu_kustannus yksikkohintaiset-tyot))))
    (is (= (reduce + (map :summa akilliset-hoitotyot)) 650M) (str "Äkilliset hoitotyöt yhteensä (e). " (reduce + (map :summa akilliset-hoitotyot))))
    (is (= (reduce + (map :summa vahinkojen-korjaukset)) 0) (str "Vahinkojenkorjaukset yhteensä (e). " (reduce + (map :summa vahinkojen-korjaukset))))
    (is (= (reduce + (map :summa hallinnolliset-tyot)) 666M) (str "Hallinnolliset toimenpiteet yhteensä (e). " (reduce + (map :summa hallinnolliset-tyot))))
    (is (= (reduce + (map :summa talvihoidon-tyot)) 16816M) (str "Talvihoito yhteensä (e). " (reduce + (map :summa talvihoidon-tyot))))))



;; Tallennetaan budjetoidut työt. Lopputuloksen pitäisi olla:
;; Kiinteähintaiset, kustannusarvioidut ja yksikköhintaiset työt on tallennettu omiin tauluihinsa oikein.
;; Kiinteähintainen osuus kasvattaa kokonaishintaista maksuerää ja kustannussuunnitelma on merkitty likaiseksi.
(deftest tallenna-budjetoidut-tyot-testi
(let [urakan-budjetoidut-tyot (kutsu-palvelua (:http-palvelin jarjestelma)
                                                            :tallenna-budjetoidut-tyot +kayttaja-jvh+ {:urakka-id @oulun-maanteiden-hoitourakan-2019-2024-id
                                                                                                       :sopimusnumero @oulun-maanteiden-hoitourakan-2019-2024-sopimus-id
                                                                                                       :tyot budjetoidut-tyot})
      kiinteahintaiset-tyot (:kiinteahintaiset-tyot urakan-budjetoidut-tyot)
      kustannusarvioidut-tyot (:kustannusarvioidut-tyot urakan-budjetoidut-tyot)
      yksikkohintaiset-tyot (:yksikkohintaiset-tyot urakan-budjetoidut-tyot)
      akilliset-hoitotyot (filter #(= "akillinen-hoitotyo" (:tyyppi %)) kustannusarvioidut-tyot)
      vahinkojen-korjaukset (filter #(= "vahinkojen-korjaukset" (:tyyppi %)) kustannusarvioidut-tyot)
      hallinnolliset-tyot (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23151") (:toimenpideinstanssi %)) kustannusarvioidut-tyot)
      liikenneympariston-hoidon-tyot (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23116") (:toimenpideinstanssi %)) kustannusarvioidut-tyot)
      sorateiden-hoidon-tyot (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23124") (:toimenpideinstanssi %)) kustannusarvioidut-tyot)
      talvihoidon-tyot (clojure.set/union (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104") (:toimenpideinstanssi %)) kiinteahintaiset-tyot)
                                          (filter #(= (hae-oulun-maanteiden-hoitourakan-toimenpideinstanssi "23104") (:toimenpideinstanssi %)) kustannusarvioidut-tyot))
      urakan-vastuuhenkilo (oulun-2019-urakan-urakoitsijan-urakkavastaava)]

  (is (thrown? RuntimeException (kutsu-palvelua (:http-palvelin jarjestelma)
                                                :tallenna-budjetoidut-tyot urakan-vastuuhenkilo {:urakka-id @oulun-maanteiden-hoitourakan-2019-2024-id
                                                                                           :sopimusnumero @oulun-maanteiden-hoitourakan-2019-2024-sopimus-id
                                                                                           :tyot budjetoidut-tyot})) "Urakan vastuuhenkilöllä ei ole kirjoitusoikeuksia suunnittelutietoihin.")

  (is (= (reduce + (map :summa kiinteahintaiset-tyot)) 17998M) (str "Kiinteähintaiset työt yhteensä (e)." (reduce + (map :summa kiinteahintaiset-tyot))))
  (is (= (reduce + (map :summa kustannusarvioidut-tyot)) 3264M) (str "Kustannusarvioidut työt yhteensä (e). " (reduce + (map :summa kustannusarvioidut-tyot))))
  (is (= (reduce + (map :arvioitu_kustannus yksikkohintaiset-tyot)) 11151M) (str "Yksikköhintaiset työt yhteensä (e). " (reduce + (map :arvioitu_kustannus yksikkohintaiset-tyot))))
  (is (= (reduce + (map :summa akilliset-hoitotyot)) 1316M) (str "Äkilliset hoitotyöt yhteensä (e). " (reduce + (map :summa akilliset-hoitotyot))))
  (is (= (reduce + (map :summa vahinkojen-korjaukset)) 66M) (str "Vahinkojenkorjaukset yhteensä (e). " (reduce + (map :summa vahinkojen-korjaukset))))
  (is (= (reduce + (map :summa hallinnolliset-tyot)) 1332M) (str "Hallinnolliset kustannusarvioidut yhteensä (e). " (reduce + (map :summa hallinnolliset-tyot))))
  (is (= (reduce + (map :summa liikenneympariston-hoidon-tyot)) 1116M) (str "Liikenteenympäristön hoito yhteensä (e). " (reduce + (map :summa liikenneympariston-hoidon-tyot))))
  (is (= (reduce + (map :summa sorateiden-hoidon-tyot)) 666M) (str "Sorateiden hoito yhteensä (e). " (reduce + (map :summa hallinnolliset-tyot))))
  (is (= (reduce + (map :summa talvihoidon-tyot)) 18148M) (str "Talvihoito yhteensä (e). " (reduce + (map :summa talvihoidon-tyot)))))

  ;; Onko oikea määrä kustannussuunnitelmia merkattu likaiseksi (= lähtettäväksi Sampoon)
  (is (= 4 (ffirst (q "select count(maksuera) from kustannussuunnitelma where likainen is true and maksuera in (select numero from maksuera where toimenpideinstanssi in (select id from toimenpideinstanssi where urakka = " @oulun-maanteiden-hoitourakan-2019-2024-id "));")))))