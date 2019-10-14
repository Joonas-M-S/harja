(ns harja.palvelin.palvelut.budjettisuunnittelu-test
  (:require [clojure.test :refer [deftest testing use-fixtures compose-fixtures is]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [harja.palvelin.palvelut.budjettisuunnittelu :as bs]
            [harja.palvelin.palvelut.pois-kytketyt-ominaisuudet :as pois-kytketyt-ominaisuudet]
            [harja.testi :refer :all]
            [harja.data.hoito.kustannussuunnitelma :as data-gen]
            [com.stuartsierra.component :as component]
            [harja.pvm :as pvm]
            [slingshot.slingshot :refer [try+]]))

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
                                            (bs/->Budjettisuunnittelu)
                                            [:http-palvelin :db :pois-kytketyt-ominaisuudet])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :each (compose-fixtures tietokanta-fixture jarjestelma-fixture))

(s/def ::aika-kuukaudella-ivalon-urakalle
  (s/with-gen ::bs/aika
              (fn []
                (gen/fmap (fn [_]
                            (let [aloitus-vuosi (pvm/vuosi (first (pvm/paivamaaran-hoitokausi (pvm/nyt))))
                                  vuosi (rand-nth (range aloitus-vuosi (+ aloitus-vuosi 6)))
                                  kk (cond
                                       (= vuosi aloitus-vuosi) (rand-nth (range 10 13))
                                       (= vuosi (+ aloitus-vuosi 5)) (rand-nth (range 1 10))
                                       :else (rand-nth (range 1 13)))]
                              {:vuosi vuosi
                               :kuukausi kk}))
                          (gen/int)))))

(s/def ::aika-vuodella-ivalon-urakalle
  (s/with-gen ::bs/aika
              (fn []
                (gen/fmap (fn [_]
                            (let [aloitus-vuosi (pvm/vuosi (first (pvm/paivamaaran-hoitokausi (pvm/nyt))))
                                  vuosi (rand-nth (range aloitus-vuosi (+ aloitus-vuosi 6)))
                                  kk (cond
                                       (= vuosi aloitus-vuosi) (rand-nth (range 10 13))
                                       (= vuosi (+ aloitus-vuosi 5)) (rand-nth (range 1 10))
                                       :else (rand-nth (range 1 13)))]
                              {:vuosi vuosi}))
                          (gen/int)))))

(deftest budjetoidut-tyot-haku
  (let [{urakka-id :id urakan-alkupvm :alkupvm} (first (q-map "SELECT id, alkupvm FROM urakka WHERE nimi='Pellon MHU testiurakka (3. hoitovuosi)';"))
        budjetoidut-tyot (bs/hae-urakan-budjetoidut-tyot (:db jarjestelma) +kayttaja-jvh+ {:urakka-id urakka-id})
        testaa-ajat (fn [tehtavat toimenpide]
                      (let [tehtavat-vuosittain (group-by :vuosi tehtavat)]
                        (is (= (ffirst (sort-by key tehtavat-vuosittain))
                               (pvm/vuosi urakan-alkupvm))
                            (str "Urakan ensimmäinen vuosi pitäisi olla " (pvm/vuosi urakan-alkupvm)
                                 ", mutta se oli " (ffirst (sort-by key tehtavat-vuosittain))))
                        (is (= 6 (count tehtavat-vuosittain)) (str "Tehtäviä pitäisi olla merkattuna kuudelle kalenterivuodelle. Toimenpiteelle "
                                                                   toimenpide " löytyi " (count tehtavat-vuosittain) " vuodelle"))
                        (is (= #{10 11 12}
                               (into #{}
                                     (map :kuukausi
                                          (val (first (sort-by key tehtavat-vuosittain))))))
                            "Ensimmäisellä kalenterivuodella pitäisi olla arvoja vain kolmelle viimeiselle kuukaudelle")
                        (doseq [[_ tehtavat-vuodelle] (butlast (rest (sort-by key tehtavat-vuosittain)))]
                          (is (= (into #{} (range 1 13))
                                 (into #{} (map :kuukausi tehtavat-vuodelle))))
                          "Muille kalenterivuosille pitäisi arvo olla joka kuulle")
                        (is (= (into #{} (range 1 10))
                               (into #{}
                                     (map :kuukausi
                                          (val (last (sort-by key tehtavat-vuosittain))))))
                            "Viimeisellä kalenterivuodella pitäisi olla arvoja vain tammikuusta syysykuuhun")))]
    (testing "Kiinteähintaiset työt on oikein"
      (let [kiinteahintaiset-tyot-toimenpiteittain (group-by :toimenpide (:kiinteahintaiset-tyot budjetoidut-tyot))]
        (doseq [[toimenpide tehtavat] kiinteahintaiset-tyot-toimenpiteittain]
          (is (nil? (some (fn [{:keys [tehtava tehtavaryhma]}]
                            (when (or tehtava tehtavaryhma)
                              true))
                          tehtavat))
              "Kiinteähintainen tehtävä tulisi olla tallennettuna vain toimenpidetasolle")
          (is (nil? (some (fn [{:keys [summa]}]
                            (when-not (number? summa)
                              true))
                          tehtavat))
              "Kaikille summille pitäisi olla jokin arvo")
          (testaa-ajat tehtavat toimenpide))))
    (testing "Kustannusarvioidut työt on oikein"
      (let [kustannusarvioidut-tyot-toimenpiteittain (group-by :toimenpide-avain (:kustannusarvioidut-tyot budjetoidut-tyot))]
        (doseq [[toimenpide-avain tehtavat] kustannusarvioidut-tyot-toimenpiteittain
                :let [ryhmiteltyna (group-by (juxt :tyyppi :tehtava-nimi) tehtavat)]]
          (case toimenpide-avain
            :paallystepaikkaukset (is (= ryhmiteltyna {}))
            :mhu-yllapito (do
                            (is (= (keys ryhmiteltyna) [["muut-rahavaraukset" nil]]))
                            (testaa-ajat tehtavat toimenpide-avain))
            :talvihoito (do
                          (is (= (into #{} (keys ryhmiteltyna))
                                 #{["vahinkojen-korjaukset" nil]
                                   ["akillinen-hoitotyo" nil]}))
                          (doseq [[_ tehtavat] ryhmiteltyna]
                            (testaa-ajat tehtavat toimenpide-avain)))
            :liikenneympariston-hoito (do
                                        (is (= (into #{} (keys ryhmiteltyna))
                                               #{["laskutettava-tyo" nil] ;; Tässä testidatassa on siis painettu päälle "Haluan suunnitella myös määrämitattavia töitä toimenpiteelle"
                                                 ["vahinkojen-korjaukset" nil]
                                                 ["akillinen-hoitotyo" nil]}))
                                        (doseq [[_ tehtavat] ryhmiteltyna]
                                          (testaa-ajat tehtavat toimenpide-avain)))
            :sorateiden-hoito (do
                                (is (= (into #{} (keys ryhmiteltyna))
                                       #{["vahinkojen-korjaukset" nil]
                                         ["akillinen-hoitotyo" nil]}))
                                (doseq [[_ tehtavat] ryhmiteltyna]
                                  (testaa-ajat tehtavat toimenpide-avain)))
            :mhu-korvausinvestointi (is (= ryhmiteltyna {}))
            :mhu-johto (do
                         (is (= (into #{} (keys ryhmiteltyna))
                                #{["laskutettava-tyo" nil]
                                  ["laskutettava-tyo" "Toimistotarvike- ja ICT-kulut, tiedotus, opastus, kokousten järjestäminen jne."]
                                  ["laskutettava-tyo" "Hoitourakan työnjohto"]}))
                         (doseq [[_ tehtavat] ryhmiteltyna]
                           (testaa-ajat tehtavat toimenpide-avain)))))))
    (testing "Johto ja hallintokorvaukset ovat oikein"
      (let [johto-ja-hallintokorvaukset (group-by :toimenkuva (:johto-ja-hallintokorvaukset budjetoidut-tyot))]
        (doseq [[toimenkuva tiedot] johto-ja-hallintokorvaukset]
          (case toimenkuva
            "hankintavastaava" (is (= (into #{}
                                            (mapcat (fn [hoitokausi]
                                                      [[hoitokausi :molemmat]])
                                                    (range 0 6)))
                                      (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))
            "sopimusvastaava" (is (= (into #{}
                                           (mapcat (fn [hoitokausi]
                                                     [[hoitokausi :molemmat]])
                                                   (range 1 6)))
                                     (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))
            "vastuunalainen työnjohtaja" (is (= (into #{}
                                                      (mapcat (fn [hoitokausi]
                                                                [[hoitokausi :molemmat]])
                                                              (range 1 6)))
                                                (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))
            "päätoiminen apulainen" (is (= (into #{}
                                                 (mapcat (fn [hoitokausi]
                                                           [[hoitokausi :talvi]
                                                            [hoitokausi :kesa]])
                                                         (range 1 6)))
                                           (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))
            "apulainen/työnjohtaja" (is (= (into #{}
                                                 (mapcat (fn [hoitokausi]
                                                           [[hoitokausi :talvi]
                                                            [hoitokausi :kesa]])
                                                         (range 1 6)))
                                           (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))
            "viherhoidosta vastaava henkilö" (is (= (into #{}
                                                          (mapcat (fn [hoitokausi]
                                                                    [[hoitokausi :molemmat]])
                                                                  (range 1 6)))
                                                    (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))
            "harjoittelija" (is (= (into #{}
                                         (mapcat (fn [hoitokausi]
                                                   [[hoitokausi :molemmat]])
                                                 (range 1 6)))
                                   (into #{} (keys (group-by (juxt :hoitokausi :maksukausi) tiedot)))))))))))

(deftest tallenna-kiinteahintaiset-tyot
  (let [urakka-id (hae-ivalon-maanteiden-hoitourakan-id)
        tallennettavat-tyot [{:urakka-id urakka-id
                              :toimenpide-avain :paallystepaikkaukset
                              ;; Ajoille tämmöinen hirvitys, että saadaan generoitua random dataa, mutta siten,
                              ;; että lopulta kaikkien aikojen vuosi-kuukausi yhdistelmä on uniikki
                              :ajat (mapv first
                                          (vals (group-by (juxt :vuosi :kuukausi)
                                                          (gen/sample (s/gen ::aika-kuukaudella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :toimenpide-avain :mhu-yllapito
                              :ajat (mapv first
                                          (vals (group-by (juxt :vuosi :kuukausi)
                                                          (gen/sample (s/gen ::aika-kuukaudella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :toimenpide-avain :talvihoito
                              :ajat (mapv first
                                          (vals (group-by (juxt :vuosi :kuukausi)
                                                          (gen/sample (s/gen ::aika-kuukaudella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :toimenpide-avain :liikenneympariston-hoito
                              :ajat (mapv first
                                          (vals (group-by (juxt :vuosi :kuukausi)
                                                          (gen/sample (s/gen ::aika-kuukaudella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :toimenpide-avain :sorateiden-hoito
                              :ajat (mapv first
                                          (vals (group-by (juxt :vuosi :kuukausi)
                                                          (gen/sample (s/gen ::aika-kuukaudella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :toimenpide-avain :mhu-korvausinvestointi
                              :ajat (mapv first
                                          (vals (group-by (juxt :vuosi :kuukausi)
                                                          (gen/sample (s/gen ::aika-kuukaudella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}]]
    (testing "Tallennus onnistuu"
      (doseq [tyo tallennettavat-tyot]
        (let [tyo (update tyo :summa get :uusi)
              vastaus (bs/tallenna-kiinteahintaiset-tyot (:db jarjestelma) +kayttaja-jvh+ tyo)]
          (is (:onnistui? vastaus) (str "Tallentaminen toimenpiteelle " (:toimenpide-avain tyo) " epäonnistui.")))))
    (testing "Data kannassa on oikein"
      (doseq [{:keys [toimenpide-avain urakka-id ajat] {summa :uusi} :summa} tallennettavat-tyot]
        (let [toimenpidekoodi (case toimenpide-avain
                                 :paallystepaikkaukset "20107"
                                 :mhu-yllapito "20191"
                                 :talvihoito "23104"
                                 :liikenneympariston-hoito "23116"
                                 :sorateiden-hoito "23124"
                                 :mhu-korvausinvestointi "14301"
                                 :mhu-johto "23151")
              toimenpide-id (ffirst (q (str "SELECT id FROM toimenpidekoodi WHERE taso = 3 AND koodi = '" toimenpidekoodi "';")))
              toimenpideinstanssi (ffirst (q (str "SELECT id FROM toimenpideinstanssi WHERE urakka = " urakka-id " AND toimenpide = " toimenpide-id ";")))
              data-kannassa (q-map (str "SELECT vuosi, kuukausi, summa, luotu, toimenpideinstanssi FROM kiinteahintainen_tyo WHERE toimenpideinstanssi=" toimenpideinstanssi ";"))]
          (is (every? :luotu data-kannassa) "Luomisaika ei tallennettu")
          (is (every? #(= (float (:summa %))
                          (float summa))
                      data-kannassa)
              (str "Summa ei tallentunut oikein toimenpiteelle " toimenpide-avain))
          (is (= (sort-by (juxt :vuosi :kuukausi) (map #(select-keys % #{:vuosi :kuukausi}) data-kannassa))
                 (sort-by (juxt :vuosi :kuukausi) ajat))
              (str "Ajat eivät tallentuneet kantaan oikein toimenpiteelle " toimenpide-avain)))))
    (testing "Päivitys onnistuu"
      (doseq [tyo tallennettavat-tyot]
        (let [tyo (-> tyo
                      (update :summa get :paivitys)
                      (update :ajat (fn [ajat]
                                      (drop (int (/ (count ajat) 2)) ajat))))
              vastaus (bs/tallenna-kiinteahintaiset-tyot (:db jarjestelma) +kayttaja-jvh+ tyo)]
          (is (:onnistui? vastaus) (str "Päivittäminen toimenpiteelle " (:toimenpide-avain tyo) " epäonnistui.")))))
    (testing "Päivitetty data kannassa on oikein"
      (doseq [{:keys [toimenpide-avain urakka-id ajat summa]} tallennettavat-tyot]
        (let [toimenpidekoodi (case toimenpide-avain
                                :paallystepaikkaukset "20107"
                                :mhu-yllapito "20191"
                                :talvihoito "23104"
                                :liikenneympariston-hoito "23116"
                                :sorateiden-hoito "23124"
                                :mhu-korvausinvestointi "14301"
                                :mhu-johto "23151")
              toimenpide-id (ffirst (q (str "SELECT id FROM toimenpidekoodi WHERE taso = 3 AND koodi = '" toimenpidekoodi "';")))
              toimenpideinstanssi (ffirst (q (str "SELECT id FROM toimenpideinstanssi WHERE urakka = " urakka-id " AND toimenpide = " toimenpide-id ";")))
              data-kannassa (q-map (str "SELECT vuosi, kuukausi, summa, muokattu, toimenpideinstanssi FROM kiinteahintainen_tyo WHERE toimenpideinstanssi=" toimenpideinstanssi ";"))
              vanha-summa (:uusi summa)
              paivitetty-summa (:paivitys summa)
              pudotettava-maara (int (/ (count ajat) 2))
              vanhat-ajat (take pudotettava-maara ajat)
              paivitetyt-ajat (drop pudotettava-maara ajat)
              paivitetty-data-kannassa (filter (fn [{:keys [vuosi kuukausi]}]
                                                 (some #(and (= vuosi (:vuosi %))
                                                             (= kuukausi (:kuukausi %)))
                                                       paivitetyt-ajat))
                                               data-kannassa)]
          (is (every? :muokattu paivitetty-data-kannassa) "Luomisaika ei tallennettu")
          (is (= (+ (count vanhat-ajat) (count paivitetyt-ajat)) (count data-kannassa)))
          (is (every? #(= (float (:summa %))
                          (float vanha-summa))
                      (filter (fn [{:keys [vuosi kuukausi]}]
                                (some #(and (= vuosi (:vuosi %))
                                            (= kuukausi (:kuukausi %)))
                                      vanhat-ajat))
                              data-kannassa))
              (str "Vanhat summat ei pysy kannassa oikein päivityksen jälkeen toimenpiteelle " toimenpide-avain))
          (is (every? #(= (float (:summa %))
                          (float paivitetty-summa))
                      paivitetty-data-kannassa)
              (str "Päivitetyt summat ei tallennu kantaan oikein toimenpiteelle " toimenpide-avain)))))))

(deftest tallenna-kustannusarvioitu-tyo
  (let [{urakka-id :id urakan-alkupvm :alkupvm urakan-loppupvm :loppupvm} (first (q-map (str "SELECT id, alkupvm, loppupvm
                                                                                              FROM   urakka
                                                                                              WHERE  nimi = 'Ivalon MHU testiurakka (uusi)'")))
        urakan-alkuvuosi (pvm/vuosi urakan-alkupvm)
        urakan-loppuvuosi (pvm/vuosi urakan-loppupvm)
        muodosta-ajat (fn [vuosi urakan-alkuvuosi urakan-loppuvuosi]
                       (let [kuukaudet (cond
                                         (= vuosi urakan-alkuvuosi) (range 10 13)
                                         (= vuosi urakan-loppuvuosi) (range 1 10)
                                         :else (range 1 13))]
                         (mapv (fn [kuukausi]
                                 {:vuosi vuosi
                                  :kuukausi kuukausi})
                               kuukaudet)))
        tallennettavat-tyot [{:urakka-id urakka-id
                              :tallennettavat-asiat #{:toimenpiteen-maaramitattavat-tyot}
                              :toimenpide-avain :paallystepaikkaukset
                              ;; Ajoille tämmöinen hirvitys, että saadaan generoitua random dataa, mutta siten,
                              ;; että lopulta kaikkien aikojen vuosi on uniikki
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :tallennettavat-asiat #{:rahavaraus-lupaukseen-1
                                                      :toimenpiteen-maaramitattavat-tyot}
                              :toimenpide-avain :mhu-yllapito
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :tallennettavat-asiat #{:kolmansien-osapuolten-aiheuttamat-vahingot
                                                      :akilliset-hoitotyot
                                                      :toimenpiteen-maaramitattavat-tyot}
                              :toimenpide-avain :talvihoito
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :tallennettavat-asiat #{:kolmansien-osapuolten-aiheuttamat-vahingot
                                                      :akilliset-hoitotyot
                                                      :toimenpiteen-maaramitattavat-tyot}
                              :toimenpide-avain :liikenneympariston-hoito
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :tallennettavat-asiat #{:kolmansien-osapuolten-aiheuttamat-vahingot
                                                      :akilliset-hoitotyot
                                                      :toimenpiteen-maaramitattavat-tyot}
                              :toimenpide-avain :sorateiden-hoito
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :tallennettavat-asiat #{:toimenpiteen-maaramitattavat-tyot}
                              :toimenpide-avain :mhu-korvausinvestointi
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}
                             {:urakka-id urakka-id
                              :tallennettavat-asiat #{:hoidonjohtopalkkio
                                                      :toimistokulut
                                                      :erillishankinnat}
                              :toimenpide-avain :mhu-johto
                              :ajat (mapv first
                                          (vals (group-by :vuosi
                                                          (gen/sample (s/gen ::aika-vuodella-ivalon-urakalle)))))
                              :summa {:uusi (gen/generate (s/gen ::bs/summa))
                                      :paivitys (gen/generate (s/gen ::bs/summa))}}]]
    (testing "Tallennus onnistuu"
      (doseq [tyo tallennettavat-tyot
              :let [tallennettavat-asiat (:tallennettavat-asiat tyo)]
              tallennettava-asia tallennettavat-asiat
              :let [tyo (-> tyo
                            (dissoc :tallennettavat-asiat)
                            (assoc :tallennettava-asia tallennettava-asia)
                            (update :summa get :uusi))]]
        (let [vastaus (bs/tallenna-kustannusarvioitu-tyo (:db jarjestelma) +kayttaja-jvh+ tyo)]
          (is (:onnistui? vastaus) (str "Tallentaminen toimenpiteelle " (:toimenpide-avain tyo) " epäonnistui.")))))
    (testing "Data kannassa on oikein"
      (doseq [{:keys [toimenpide-avain urakka-id ajat tallennettavat-asiat] {summa :uusi} :summa} tallennettavat-tyot]
        (let [toimenpidekoodi (case toimenpide-avain
                                :paallystepaikkaukset "20107"
                                :mhu-yllapito "20191"
                                :talvihoito "23104"
                                :liikenneympariston-hoito "23116"
                                :sorateiden-hoito "23124"
                                :mhu-korvausinvestointi "14301"
                                :mhu-johto "23151")
              toimenpide-id (ffirst (q (str "SELECT id FROM toimenpidekoodi WHERE taso = 3 AND koodi = '" toimenpidekoodi "';")))
              toimenpideinstanssi (ffirst (q (str "SELECT id FROM toimenpideinstanssi WHERE urakka = " urakka-id " AND toimenpide = " toimenpide-id ";")))
              data-kannassa (q-map (str "SELECT kt.vuosi, kt.kuukausi, kt.summa, kt.luotu, kt.tyyppi, tk.nimi AS tehtava, tr.nimi AS tehtavaryhma
                                         FROM kustannusarvioitu_tyo kt
                                           LEFT JOIN toimenpidekoodi tk ON tk.id = kt.tehtava
                                           LEFT JOIN tehtavaryhma tr ON tr.id = kt.tehtavaryhma
                                         WHERE kt.toimenpideinstanssi=" toimenpideinstanssi ";"))]
          (is (every? :luotu data-kannassa) "Luomisaika ei tallennettu")
          (is (every? #(= (float (:summa %))
                          (float summa))
                      data-kannassa)
              (str "Summa ei tallentunut oikein toimenpiteelle " toimenpide-avain))
          (doseq [tallennettava-asia tallennettavat-asiat]
            (let [tallennetun-asian-data? (fn [{:keys [tyyppi tehtava tehtavaryhma]}]
                                            (case tallennettava-asia
                                              :hoidonjohtopalkkio (and (= tyyppi "laskutettava-tyo")
                                                                       (= tehtava "Hoitourakan työnjohto")
                                                                       (nil? tehtavaryhma))
                                              :toimistokulut (and (= tyyppi "laskutettava-tyo")
                                                                  (= tehtava "Toimistotarvike- ja ICT-kulut, tiedotus, opastus, kokousten järjestäminen jne.")
                                                                  (nil? tehtavaryhma))
                                              :erillishankinnat (and (= tyyppi "laskutettava-tyo")
                                                                     (nil? tehtava)
                                                                     (= tehtavaryhma "ERILLISHANKINNAT"))
                                              :rahavaraus-lupaukseen-1 (and (= tyyppi "muut-rahavaraukset")
                                                                            (nil? tehtava)
                                                                            (= tehtavaryhma "TILAAJAN RAHAVARAUS"))
                                              :kolmansien-osapuolten-aiheuttamat-vahingot (and (= tyyppi "vahinkojen-korjaukset")
                                                                                               (= tehtava "Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen")
                                                                                               (nil? tehtavaryhma))
                                              :akilliset-hoitotyot (and (= tyyppi "akillinen-hoitotyo")
                                                                        (= tehtava "Äkillinen hoitotyö")
                                                                        (nil? tehtavaryhma))
                                              :toimenpiteen-maaramitattavat-tyot (and (= tyyppi "laskutettava-tyo")
                                                                                      (nil? tehtava)
                                                                                      (nil? tehtavaryhma))))
                  tallennetun-asian-data-kannassa (filter tallennetun-asian-data? data-kannassa)]
              (is (= (sort-by (juxt :vuosi :kuukausi) (map #(select-keys % #{:vuosi :kuukausi}) tallennetun-asian-data-kannassa))
                     (sort-by (juxt :vuosi :kuukausi) (mapcat (fn [{:keys [vuosi]}]
                                                                (muodosta-ajat vuosi urakan-alkuvuosi urakan-loppuvuosi))
                                                              ajat)))
                  (str "Ajat eivät tallentuneet kantaan oikein toimenpiteelle: " toimenpide-avain " ja tallennettavalle asialle: " tallennettava-asia)))))))
    (testing "Päivitys onnistuu"
      (doseq [tyo tallennettavat-tyot
              :let [tallennettavat-asiat (:tallennettavat-asiat tyo)]
              tallennettava-asia tallennettavat-asiat
              :let [tyo (-> tyo
                            (dissoc :tallennettavat-asiat)
                            (assoc :tallennettava-asia tallennettava-asia)
                            (update :summa get :paivitys)
                            (update :ajat (fn [ajat]
                                            (drop (int (/ (count ajat) 2)) ajat))))]]
        (let [vastaus (bs/tallenna-kustannusarvioitu-tyo (:db jarjestelma) +kayttaja-jvh+ tyo)]
          (is (:onnistui? vastaus) (str "Päivittäminen toimenpiteelle " (:toimenpide-avain tyo) " epäonnistui.")))))
    (testing "Päivitetty data kannassa on oikein"
      (doseq [{:keys [toimenpide-avain urakka-id ajat tallennettavat-asiat summa]} tallennettavat-tyot]
        (let [toimenpidekoodi (case toimenpide-avain
                                :paallystepaikkaukset "20107"
                                :mhu-yllapito "20191"
                                :talvihoito "23104"
                                :liikenneympariston-hoito "23116"
                                :sorateiden-hoito "23124"
                                :mhu-korvausinvestointi "14301"
                                :mhu-johto "23151")
              toimenpide-id (ffirst (q (str "SELECT id FROM toimenpidekoodi WHERE taso = 3 AND koodi = '" toimenpidekoodi "';")))
              toimenpideinstanssi (ffirst (q (str "SELECT id FROM toimenpideinstanssi WHERE urakka = " urakka-id " AND toimenpide = " toimenpide-id ";")))
              data-kannassa (q-map (str "SELECT kt.vuosi, kt.kuukausi, kt.summa, kt.muokattu, kt.tyyppi, tk.nimi AS tehtava, tr.nimi AS tehtavaryhma
                                         FROM kustannusarvioitu_tyo kt
                                           LEFT JOIN toimenpidekoodi tk ON tk.id = kt.tehtava
                                           LEFT JOIN tehtavaryhma tr ON tr.id = kt.tehtavaryhma
                                         WHERE kt.toimenpideinstanssi=" toimenpideinstanssi ";"))]
          (doseq [tallennettava-asia tallennettavat-asiat]
            (let [tallennetun-asian-data? (fn [{:keys [tyyppi tehtava tehtavaryhma]}]
                                            (case tallennettava-asia
                                              :hoidonjohtopalkkio (and (= tyyppi "laskutettava-tyo")
                                                                       (= tehtava "Hoitourakan työnjohto")
                                                                       (nil? tehtavaryhma))
                                              :toimistokulut (and (= tyyppi "laskutettava-tyo")
                                                                  (= tehtava "Toimistotarvike- ja ICT-kulut, tiedotus, opastus, kokousten järjestäminen jne.")
                                                                  (nil? tehtavaryhma))
                                              :erillishankinnat (and (= tyyppi "laskutettava-tyo")
                                                                     (nil? tehtava)
                                                                     (= tehtavaryhma "ERILLISHANKINNAT"))
                                              :rahavaraus-lupaukseen-1 (and (= tyyppi "muut-rahavaraukset")
                                                                            (nil? tehtava)
                                                                            (= tehtavaryhma "TILAAJAN RAHAVARAUS"))
                                              :kolmansien-osapuolten-aiheuttamat-vahingot (and (= tyyppi "vahinkojen-korjaukset")
                                                                                               (= tehtava "Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen")
                                                                                               (nil? tehtavaryhma))
                                              :akilliset-hoitotyot (and (= tyyppi "akillinen-hoitotyo")
                                                                        (= tehtava "Äkillinen hoitotyö")
                                                                        (nil? tehtavaryhma))
                                              :toimenpiteen-maaramitattavat-tyot (and (= tyyppi "laskutettava-tyo")
                                                                                      (nil? tehtava)
                                                                                      (nil? tehtavaryhma))))
                  tallennetun-asian-data-kannassa (filter tallennetun-asian-data? data-kannassa)
                  vanha-summa (:uusi summa)
                  paivitetty-summa (:paivitys summa)
                  pudotettava-maara (int (/ (count ajat) 2))
                  vanhat-ajat (take pudotettava-maara ajat)
                  paivitetyt-ajat (drop pudotettava-maara ajat)
                  vanha-data-kannassa (filter (fn [{vuosi :vuosi}]
                                                (some #(= vuosi (:vuosi %))
                                                      vanhat-ajat))
                                              tallennetun-asian-data-kannassa)
                  uusi-data-kannassa (filter (fn [{vuosi :vuosi}]
                                               (some #(= vuosi (:vuosi %))
                                                     paivitetyt-ajat))
                                             tallennetun-asian-data-kannassa)]
              (is (every? :muokattu uusi-data-kannassa) "Muokkausaika ei tallennettu")
              (is (every? #(= (float (:summa %))
                              (float vanha-summa))
                          vanha-data-kannassa)
                  (str "Vanha summa ei ole oikein päivityksen jälkeen toimenpiteelle " toimenpide-avain))
              (is (every? #(= (float (:summa %))
                              (float paivitetty-summa))
                          uusi-data-kannassa)
                  (str "Päivitetty summa ei ole oikein päivityksen jälkeen toimenpiteelle " toimenpide-avain))
              (is (= (sort-by (juxt :vuosi :kuukausi) (map #(select-keys % #{:vuosi :kuukausi}) vanha-data-kannassa))
                     (sort-by (juxt :vuosi :kuukausi) (mapcat (fn [{:keys [vuosi]}]
                                                                (muodosta-ajat vuosi urakan-alkuvuosi urakan-loppuvuosi))
                                                              vanhat-ajat)))
                  (str "Ajat eivät oikein päivityksen jälkeen vanhalle datalle toimenpiteelle: " toimenpide-avain " ja tallennettavalle asialle: " tallennettava-asia))
              (is (= (sort-by (juxt :vuosi :kuukausi) (map #(select-keys % #{:vuosi :kuukausi}) uusi-data-kannassa))
                     (sort-by (juxt :vuosi :kuukausi) (mapcat (fn [{:keys [vuosi]}]
                                                                (muodosta-ajat vuosi urakan-alkuvuosi urakan-loppuvuosi))
                                                              paivitetyt-ajat)))
                  (str "Ajat eivät oikein päivityksen jälkeen päivitetylle datalle toimenpiteelle: " toimenpide-avain " ja tallennettavalle asialle: " tallennettava-asia)))))))))

(deftest tallenna-johto-ja-hallintokorvaukset
  (let [urakka-id (hae-ivalon-maanteiden-hoitourakan-id)
        paivitysvuosi 3
        tallennettava-data (data-gen/tallenna-johto-ja-hallintokorvaus-data urakka-id)
        paivitettava-data (data-gen/tallenna-johto-ja-hallintokorvaus-data urakka-id {:hoitokaudet (into #{} (range paivitysvuosi 6))})]
    (testing "Tallennus onnistuu"
      (doseq [{:keys [toimenkuva maksukausi] :as parametrit} tallennettava-data]
        (let [vastaus (bs/tallenna-johto-ja-hallintokorvaukset (:db jarjestelma) +kayttaja-jvh+ parametrit)]
          (is (:onnistui? vastaus) (str "Tallennus ei onnistunut toimenkuvalle: " toimenkuva " ja maksukaudelle: " maksukausi)))))
    (testing "Data kannassa on oikein"
      (let [tallennettu-data (q-map (str "SELECT j_h.tunnit, j_h.tuntipalkka, j_h.\"kk-v\", j_h.maksukausi, j_h.hoitokausi,
                                                 tk.toimenkuva, j_h.luotu
                                          FROM johto_ja_hallintokorvaus j_h
                                            JOIN johto_ja_hallintokorvaus_toimenkuva tk ON tk.id = j_h.\"toimenkuva-id\"
                                          WHERE \"urakka-id\"=" urakka-id))
            td-ryhmitelty (group-by :toimenkuva tallennettu-data)]
        (is (every? :luotu tallennettu-data))
        (doseq [{:keys [toimenkuva maksukausi jhkt]} tallennettava-data]
          (is (= (sort-by :hoitokausi (map (fn [data]
                                             (-> data
                                                 (update :tunnit float)
                                                 (update :tuntipalkka float)
                                                 (update :kk-v float)))
                                           jhkt))
                 (sort-by :hoitokausi (keep (fn [data]
                                              (when (= (keyword (:maksukausi data)) maksukausi)
                                                (-> data
                                                    (select-keys #{:hoitokausi :tunnit :tuntipalkka :kk-v})
                                                    (update :tunnit float)
                                                    (update :tuntipalkka float)
                                                    (update :kk-v float))))
                                            (get td-ryhmitelty toimenkuva))))
              (str "Data ei kannassa oikein toimenkuvalle: " toimenkuva " ja maksukaudelle: " maksukausi)))))
    (testing "Päivitys onnistuu"
      (doseq [{:keys [toimenkuva maksukausi] :as parametrit} paivitettava-data]
        (let [vastaus (bs/tallenna-johto-ja-hallintokorvaukset (:db jarjestelma) +kayttaja-jvh+ parametrit)]
          (is (:onnistui? vastaus) (str "Päivittäminen ei onnistunut toimenkuvalle: " toimenkuva " ja maksukaudelle: " maksukausi)))))
    (testing "Päivitetty data kannassa on oikein"
      (let [tallennettu-data (q-map (str "SELECT j_h.tunnit, j_h.tuntipalkka, j_h.\"kk-v\", j_h.maksukausi, j_h.hoitokausi,
                                                 tk.toimenkuva, j_h.muokattu
                                          FROM johto_ja_hallintokorvaus j_h
                                            JOIN johto_ja_hallintokorvaus_toimenkuva tk ON tk.id = j_h.\"toimenkuva-id\"
                                          WHERE \"urakka-id\"=" urakka-id))
            td-ryhmitelty (group-by :toimenkuva tallennettu-data)]
        (doseq [{:keys [toimenkuva maksukausi jhkt]} paivitettava-data]
          (let [vanhat-tallennettava-jhkt (keep (fn [data]
                                                  (when (< (:hoitokausi data) paivitysvuosi)
                                                    (-> data
                                                        (update :tunnit float)
                                                        (update :tuntipalkka float)
                                                        (update :kk-v float))))
                                                (some (fn [{vanha-toimenkuva :toimenkuva
                                                            vanha-maksukausi :maksukausi
                                                            vanha-jhkt :jhkt}]
                                                        (when (and (= vanha-toimenkuva toimenkuva)
                                                                   (= vanha-maksukausi maksukausi))
                                                          vanha-jhkt))
                                                      tallennettava-data))
                paivitetyt-tallennettava-jhkt (keep (fn [data]
                                                      (-> data
                                                          (update :tunnit float)
                                                          (update :tuntipalkka float)
                                                          (update :kk-v float)))
                                                    jhkt)
                vanhat-kannassa-jhkt (keep (fn [data]
                                             (when (and (< (:hoitokausi data) paivitysvuosi)
                                                        (= (keyword (:maksukausi data)) maksukausi))
                                               (-> data
                                                   (select-keys #{:hoitokausi :tunnit :tuntipalkka :kk-v})
                                                   (update :tunnit float)
                                                   (update :tuntipalkka float)
                                                   (update :kk-v float))))
                                           (get td-ryhmitelty toimenkuva))
                paivitetyt-kannassa-jhkt (keep (fn [data]
                                                 (when (and (>= (:hoitokausi data) paivitysvuosi)
                                                            (= (keyword (:maksukausi data)) maksukausi))
                                                   (-> data
                                                       (select-keys #{:hoitokausi :tunnit :tuntipalkka :kk-v :muokattu})
                                                       (update :tunnit float)
                                                       (update :tuntipalkka float)
                                                       (update :kk-v float))))
                                               (get td-ryhmitelty toimenkuva))]
            (is (every? :muokattu paivitetyt-kannassa-jhkt))
            (is (= (sort-by :hoitokausi vanhat-tallennettava-jhkt)
                   (sort-by :hoitokausi vanhat-kannassa-jhkt))
                (str "Vanha data ei kannassa oikein toimenkuvalle: " toimenkuva " ja maksukaudelle: " maksukausi))
            (is (= (sort-by :hoitokausi paivitetyt-tallennettava-jhkt)
                   (sort-by :hoitokausi (map #(dissoc % :muokattu) paivitetyt-kannassa-jhkt)))
                (str "Päivitetty data ei kannassa oikein toimenkuvalle: " toimenkuva " ja maksukaudelle: " maksukausi))))))))

(deftest budjettitavoite-haku
  (let [parametrit {:urakka-id (hae-rovaniemen-maanteiden-hoitourakan-id)}
        budjettitavoite (bs/hae-urakan-tavoite (:db jarjestelma) +kayttaja-jvh+ parametrit)
        kerroin 1.1]
    (is (every? :luotu budjettitavoite) "Luotuaika ei löytynyt")
    (doseq [kausitavoite budjettitavoite
            :let [{:keys [kattohinta tavoitehinta hoitokausi]} kausitavoite]]
      (case hoitokausi
        1 (do (is (= tavoitehinta 250000M))
              (is (= (float kattohinta)
                     (float (* kerroin 250000M)))))
        2 (do (is (= tavoitehinta 300000M))
              (is (= (float kattohinta)
                     (float (* kerroin 300000M)))))
        3 (do (is (= tavoitehinta 350000M))
              (is (= (float kattohinta)
                     (float (* kerroin 350000M)))))
        4 (do (is (= tavoitehinta 250000M))
              (is (= (float kattohinta)
                     (float (* kerroin 250000M)))))
        5 (do (is (= tavoitehinta 250000M))
              (is (= (float kattohinta)
                     (float (* kerroin 250000M)))))))))

(deftest budjettitavoite-tallennus
  (let [urakka-id (hae-ivalon-maanteiden-hoitourakan-id)
        uusi-tavoitehinta (gen/generate (s/gen ::bs/tavoitehinta))
        paivitetty-tavoitehinta (gen/generate (s/gen ::bs/tavoitehinta))
        kerroin 1.1
        paivitys-hoitokaudesta-eteenpain 3
        tallennettavat-tavoitteet (mapv (fn [hoitokausi]
                                          {:hoitokausi hoitokausi
                                           :tavoitehinta {:uusi uusi-tavoitehinta
                                                          :paivitys paivitetty-tavoitehinta}
                                           :kattohinta {:uusi (* kerroin uusi-tavoitehinta)
                                                        :paivitys (* kerroin paivitetty-tavoitehinta)}})
                                        (range 1 5))]
    (testing "Tallennus onnistuu"
      (let [vastaus (bs/tallenna-urakan-tavoite (:db jarjestelma) +kayttaja-jvh+ {:urakka-id urakka-id
                                                                                  :tavoitteet (mapv (fn [tavoite]
                                                                                                      (-> tavoite
                                                                                                          (update :tavoitehinta get :uusi)
                                                                                                          (update :kattohinta get :uusi)))
                                                                                                    tallennettavat-tavoitteet)})]
        (is (:onnistui? vastaus) "Budjettitavoitteen tallentaminen ei onnistunut")))
    (testing "Data kannassa on oikein"
      (let [data-kannassa (q-map (str "SELECT *
                                       FROM urakka_tavoite
                                       WHERE urakka = " urakka-id ";"))]
        (is (every? :luotu data-kannassa) "Luotu aikaa ei kannassa budjettitavoitteelle")
        (is (every? #(= (float (:tavoitehinta %)) (float uusi-tavoitehinta)) data-kannassa) "Tavoitehinta ei tallentunut kantaan oikein")
        (is (every? #(= (float (:kattohinta %)) (float (* kerroin uusi-tavoitehinta))) data-kannassa) "Kattohinta ei tallentunut kantaan oikein")))
    (testing "Päivitys onnistuu"
      (let [vastaus (bs/tallenna-urakan-tavoite (:db jarjestelma) +kayttaja-jvh+ {:urakka-id urakka-id
                                                                                  :tavoitteet (transduce
                                                                                                (comp (filter (fn [tavoite]
                                                                                                                (>= (:hoitokausi tavoite) paivitys-hoitokaudesta-eteenpain)))
                                                                                                      (map (fn [tavoite]
                                                                                                             (-> tavoite
                                                                                                                 (update :tavoitehinta get :paivitys)
                                                                                                                 (update :kattohinta get :paivitys)))))
                                                                                                conj [] tallennettavat-tavoitteet)})]
        (is (:onnistui? vastaus) "Budjettitavoitteen päivittäminen ei onnistunut")))
    (testing "Päivitetty data kannassa on oikein"
      (let [data-kannassa (q-map (str "SELECT *
                                       FROM urakka_tavoite
                                       WHERE urakka = " urakka-id ";"))
            vanhadata-kannassa (filter (fn [tavoite]
                                         (< (:hoitokausi tavoite) paivitys-hoitokaudesta-eteenpain))
                                       data-kannassa)
            uusidata-kannassa (filter (fn [tavoite]
                                        (>= (:hoitokausi tavoite) paivitys-hoitokaudesta-eteenpain))
                                      data-kannassa)]
        (is (every? :muokattu uusidata-kannassa) "Muokattu aika ei kannassa budjettitavoitteelle")
        (is (every? #(= (float (:tavoitehinta %)) (float uusi-tavoitehinta)) vanhadata-kannassa) "Tavoitehinta ei oikein päivityksen jälkeen")
        (is (every? #(= (float (:kattohinta %)) (float (* kerroin uusi-tavoitehinta))) vanhadata-kannassa) "Kattohinta ei oikein päivityksen jälkeen")
        (is (every? #(= (float (:tavoitehinta %)) (float paivitetty-tavoitehinta)) uusidata-kannassa) "Päivitetty tavoitehinta ei oikein päivityksen jälkeen")
        (is (every? #(= (float (:kattohinta %)) (float (* kerroin paivitetty-tavoitehinta))) uusidata-kannassa) "Päivitetty kattohinta ei oikein päivityksen jälkeen")))))

(deftest budjettisuunnittelun-oikeustarkastukset
  (let [urakka-id (hae-rovaniemen-maanteiden-hoitourakan-id)]
    (testing "budjetoidut-tyot kutsun oikeustarkistus"
      (is (= (try+ (bs/hae-urakan-budjetoidut-tyot (:db jarjestelma) +kayttaja-seppo+ {:urakka-id urakka-id})
                   (catch harja.domain.roolit.EiOikeutta eo#
                     :ei-oikeutta-virhe))
             :ei-oikeutta-virhe)))
    (testing "budjettitavoite kutsun oikeustarkistus"
      (is (= (try+ (bs/hae-urakan-tavoite (:db jarjestelma) +kayttaja-seppo+ {:urakka-id urakka-id})
                   (catch harja.domain.roolit.EiOikeutta eo#
                     :ei-oikeutta-virhe))
             :ei-oikeutta-virhe)))
    (testing "tallenna-budjettitavoite kutsun oikeustarkistus"
      (is (= (try+ (bs/tallenna-urakan-tavoite (:db jarjestelma) +kayttaja-seppo+ {:urakka-id urakka-id})
                   (catch harja.domain.roolit.EiOikeutta eo#
                     :ei-oikeutta-virhe))
             :ei-oikeutta-virhe)))
    (testing "tallenna-kiinteahintaiset-tyot kutsun oikeustarkistus"
      (is (= (try+ (bs/tallenna-kiinteahintaiset-tyot (:db jarjestelma) +kayttaja-seppo+ {:urakka-id urakka-id
                                                                                          :toimenpide-avain :foo
                                                                                          :summa 1})
                   (catch harja.domain.roolit.EiOikeutta eo#
                     :ei-oikeutta-virhe))
             :ei-oikeutta-virhe)))
    (testing "tallenna-johto-ja-hallintokorvaukset kutsun oikeustarkistus"
      (is (= (try+ (bs/tallenna-johto-ja-hallintokorvaukset (:db jarjestelma) +kayttaja-seppo+ {:urakka-id urakka-id
                                                                                                :toimenkuva "foo"
                                                                                                :maksukausi :bar})
                   (catch harja.domain.roolit.EiOikeutta eo#
                     :ei-oikeutta-virhe))
             :ei-oikeutta-virhe)))
    (testing "tallenna-kustannusarvioitu-tyo kutsun oikeustarkistus"
      (is (= (try+ (bs/tallenna-kustannusarvioitu-tyo (:db jarjestelma) +kayttaja-seppo+ {:urakka-id urakka-id})
                   (catch harja.domain.roolit.EiOikeutta eo#
                     :ei-oikeutta-virhe))
             :ei-oikeutta-virhe)))))