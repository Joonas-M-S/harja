(ns harja.tiedot.urakka.suunnittelu.mhu-kustannussuunnitelma
  (:require [clojure.string :as clj-str]
            [cljs.core.async :as async :refer [<! >! chan timeout]]
            [tuck.core :as tuck]
            [harja.pvm :as pvm]
            [harja.tyokalut.tuck :as tuck-apurit]
            [harja.tiedot.urakka.urakka :as tiedot]
            [harja.loki :refer [log]]
            [harja.ui.viesti :as viesti]
            [harja.ui.taulukko.protokollat :as p]
            [harja.ui.taulukko.tyokalut :as tyokalut]
            [harja.ui.taulukko.osa :as osa]
            [harja.ui.taulukko.jana :as jana]))


(defonce viive-kanava (chan))

(def toimenpiteet #{:talvihoito
                    :liikenneympariston-hoito
                    :sorateiden-hoito
                    :paallystepaikkaukset
                    :mhu-yllapito
                    :mhu-korvausinvestointi})

(def toimenpiteet-rahavarauksilla #{:talvihoito
                                    :liikenneympariston-hoito
                                    :sorateiden-hoito})

(def toimenpiteiden-avaimet
  {:paallystepaikkaukset "Päällysteiden paikkaus (hoidon ylläpito)"
   :mhu-yllapito "MHU Ylläpito"
   :talvihoito "Talvihoito laaja TPI"
   :liikenneympariston-hoito "Liikenneympäristön hoito laaja TPI"
   :sorateiden-hoito "Soratien hoito laaja TPI"
   :mhu-korvausinvestointi "MHU Korvausinvestointi"})

(def talvikausi [10 11 12 1 2 3 4])
(def kesakausi (into [] (range 5 10)))
(def hoitokausi (concat talvikausi kesakausi))

(def kaudet {:kesa kesakausi
             :talvi talvikausi
             :kaikki hoitokausi})

(defn yhteensa-yhteenveto [paivitetty-hoitokausi app]
  (+
    ;; Hankintakustannukset
    (apply +
           (map (fn [taulukko]
                  (let [yhteensa-sarake-index (p/otsikon-index taulukko "Yhteensä")]
                    (transduce
                      (comp (filter (fn [rivi]
                                      (= :laajenna-lapsilla (p/rivin-skeema taulukko rivi))))
                            (filter (fn [laajenna-lapsilla-rivi]
                                      (= (:hoitokausi laajenna-lapsilla-rivi) paivitetty-hoitokausi)))
                            (mapcat (fn [laajenna-lapsilla-rivi]
                                      (rest (tyokalut/arvo laajenna-lapsilla-rivi :lapset))))
                            (map (fn [kk-rivi]
                                   (get (tyokalut/arvo kk-rivi :lapset) yhteensa-sarake-index)))
                            (map (fn [kk-solu]
                                   (let [arvo (tyokalut/arvo kk-solu :arvo)]
                                     (if (number? arvo)
                                       arvo 0)))))
                      + 0 (tyokalut/arvo taulukko :lapset))))
                (concat (vals (get-in app [:hankintakustannukset :toimenpiteet]))
                        (vals (get-in app [:hankintakustannukset :toimenpiteet-laskutukseen-perustuen])))))
    ;; Rahavaraukset
    (apply + (map (fn [taulukko]
                    (let [yhteensa-sarake-index (p/otsikon-index taulukko "Yhteensä")]
                      (transduce
                        (comp (filter (fn [rivi]
                                        (= :syottorivi (p/rivin-skeema taulukko rivi))))
                              (map (fn [syottorivi]
                                        (get (tyokalut/arvo syottorivi :lapset) yhteensa-sarake-index)))
                              (map (fn [yhteensa-solu]
                                     (let [arvo (tyokalut/arvo yhteensa-solu :arvo)]
                                       (if (number? arvo)
                                         arvo 0)))))
                        + 0 (tyokalut/arvo taulukko :lapset))))
                  (vals (get-in app [:hankintakustannukset :rahavaraukset]))))))

(defrecord Hoitokausi [])
(defrecord HaeKustannussuunnitelma [hankintojen-taulukko rahavarausten-taulukko
                                    johto-ja-hallintokorvaus-laskulla-taulukko johto-ja-hallintokorvaus-yhteenveto-taulukko toimistokulut-taulukko
                                    johtopalkkio-taulukko])
(defrecord HaeTavoiteJaKattohintaOnnistui [vastaus])
(defrecord HaeTavoiteJaKattohintaEpaonnistui [vastaus])
(defrecord HaeHankintakustannuksetOnnistui [vastaus hankintojen-taulukko rahavarausten-taulukko
                                            johto-ja-hallintokorvaus-laskulla-taulukko johto-ja-hallintokorvaus-yhteenveto-taulukko toimistokulut-taulukko
                                            johtopalkkio-taulukko])
(defrecord HaeHankintakustannuksetEpaonnistui [vastaus])
(defrecord LaajennaSoluaKlikattu [polku-taulukkoon rivin-id this auki?])
(defrecord MuutaTaulukonOsa [osa polku-taulukkoon arvo])
(defrecord MuutaTaulukonOsanSisarta [osa sisaren-tunniste polku-taulukkoon arvo])
(defrecord PaivitaTaulukonOsa [osa polku-taulukkoon paivitys-fn])
(defrecord PaivitaKustannussuunnitelmanYhteenvedot [])
(defrecord PaivitaToimenpideTaulukko [maara-solu polku-taulukkoon])
(defrecord TaytaAlas [maara-solu polku-taulukkoon])
(defrecord ToggleHankintakustannuksetOtsikko [kylla?])
(defrecord PaivitaJHRivit [paivitetty-osa])

(defn hankinnat-pohjadata []
  (let [urakan-aloitus-pvm (-> @tiedot/tila :yleiset :urakka :alkupvm)]
    (into []
          (drop 9
                (drop-last 3
                           (mapcat (fn [vuosi]
                                     (map #(identity
                                             {:vuosi vuosi
                                              :kuukausi %})
                                          (range 1 13)))
                                   (range (pvm/vuosi urakan-aloitus-pvm) (+ (pvm/vuosi urakan-aloitus-pvm) 6))))))))

(defn rahavarauket-pohjadata []
  ;; TODO "Muut rahavaraukset" pitää korjata kannassa olevaan nimeen
  [{:tyyppi "vahinkojen-korjaukset"}
   {:tyyppi "akillinen-hoitotyo"}
   {:tyyppi "muut-rahavaraukset"}])

(defn jh-laskulla-pohjadata []
  [{:toimenkuva "Sopimusvastaava" :kk-v 12}
   {:toimenkuva "Vastuunalainen työnjohtaja" :kk-v 12}
   {:toimenkuva "Päätoiminen apulainen (talvikausi)" :kk-v 7}
   {:toimenkuva "Päätoiminen apulainen (kesäkausi)" :kk-v 5}
   {:toimenkuva "Apulainen/työnjohtaja (talvikausi)" :kk-v 7}
   {:toimenkuva "Apulainen/työnjohtaja (kesäkausi)" :kk-v 5}
   {:toimenkuva "Viherhoidosta vastaava henkilö" :kk-v 5}
   {:toimenkuva "Hankintavastaava  (ennen urakkaa)" :kk-v 4.5}
   {:toimenkuva "Hankintavastaava (1. sopimisvuosi)" :kk-v 12}
   {:toimenkuva "Hankintavastaava (2.-5. sopimusvuosi)" :kk-v 12}
   {:toimenkuva "Harjoittelija" :kk-v 4}])

(defn tarkista-datan-validius! [hankinnat hankinnat-laskutukseen-perustuen]
  (let [[nil-pvm-hankinnat hankinnat] (reduce (fn [[nil-pvmt pvmt] {:keys [vuosi kuukausi] :as hankinta}]
                                                (if (and vuosi kuukausi)
                                                  [nil-pvmt (conj pvmt (assoc hankinta :pvm (pvm/luo-pvm vuosi (dec kuukausi) 15)))]
                                                  [(conj nil-pvmt hankinta) pvmt]))
                                              [[] []] (concat hankinnat hankinnat-laskutukseen-perustuen))
        hankintojen-vuodet (sort (map pvm/vuosi (flatten (keys (group-by #(pvm/paivamaaran-hoitokausi (:pvm %)) hankinnat)))))
        [urakan-aloitus-vuosi urakan-lopetus-vuosi] [(pvm/vuosi (-> @tiedot/yleiset :urakka :alkupvm))
                                                     (pvm/vuosi (-> @tiedot/yleiset :urakka :loppupvm))]
        hankintoja-urakan-hoitokausien-ulkopuolella? (not= [urakan-aloitus-vuosi urakan-lopetus-vuosi]
                                                        [(first hankintojen-vuodet) (last hankintojen-vuodet)])
        nil-pvm-hankintoja? (> (count nil-pvm-hankinnat) 0)
        hoitokausien-ulkopuolella-teksti (str "Urakalle on merkattu vuodet " urakan-aloitus-vuosi " - " urakan-lopetus-vuosi
                                              ", mutta urakalle on merkattu hankintoja vuosille " (first hankintojen-vuodet) " - " (last hankintojen-vuodet) ".")
        nil-hankintoja-teksti (str "Urakalle on merkattu " (count nil-pvm-hankinnat) " hankintaa ilman päivämäärää.")]
    (when (or hankintoja-urakan-hoitokausien-ulkopuolella? nil-pvm-hankintoja?)
      (viesti/nayta! (cond-> ""
                             hankintoja-urakan-hoitokausien-ulkopuolella? (str hoitokausien-ulkopuolella-teksti "\n")
                             nil-pvm-hankintoja? (str nil-hankintoja-teksti))
                     :warning viesti/viestin-nayttoaika-pitka))))

(defn paivita-rahavaraus-summat-automaattisesti [taulukko polku-taulukkoon app]
  (let [yhteensa-otsikon-index (p/otsikon-index taulukko "Yhteensä")
        yhteensa-rivin-index (-> taulukko (tyokalut/arvo :lapset) count dec)]
    (tyokalut/paivita-asiat-taulukossa taulukko
                                       [yhteensa-rivin-index yhteensa-otsikon-index]
                                       (fn [taulukko taulukon-asioiden-polut]
                                         (let [[rivit rivi osat osa] taulukon-asioiden-polut
                                               rivit (get-in taulukko rivit)
                                               osa (get-in taulukko osa)
                                               polut-yhteenlasku-osiin (keep (fn [rivi]
                                                                               (when (= :syottorivi (p/rivin-skeema taulukko rivi))
                                                                                 (into [] (apply concat polku-taulukkoon
                                                                                                 (p/osan-polku-taulukossa taulukko (nth (tyokalut/arvo rivi :lapset) yhteensa-otsikon-index))))))
                                                                             (butlast (rest rivit)))]
                                           (-> osa
                                               (p/lisaa-renderointi-derefable! tiedot/suunnittelu-kustannussuunnitelma polut-yhteenlasku-osiin app)
                                               (p/lisaa-muodosta-arvo (fn [this {yhteenlasku-osat :uusi}]
                                                                        (apply + (map (fn [osa]
                                                                                        (let [arvo (tyokalut/arvo osa :arvo)]
                                                                                          (if (number? arvo)
                                                                                            arvo 0)))
                                                                                      (vals yhteenlasku-osat)))))))))))

(defn paivita-maara-kk-taulukon-summat-automaattisesti [taulukko polku-taulukkoon app]
  (let [yhteensa-otsikon-index (p/otsikon-index taulukko "Yhteensä")
        yhteensa-rivin-index (-> taulukko (tyokalut/arvo :lapset) count dec)]
    (tyokalut/paivita-asiat-taulukossa taulukko
                                       [yhteensa-rivin-index yhteensa-otsikon-index]
                                       (fn [taulukko taulukon-asioiden-polut]
                                         (let [[rivit rivi osat osa] taulukon-asioiden-polut
                                               rivit (get-in taulukko rivit)
                                               osa (get-in taulukko osa)
                                               polku-muokkausrivin-yhteensa-osaan (into [] (apply concat polku-taulukkoon
                                                                                                  (p/osan-polku-taulukossa taulukko (nth (tyokalut/arvo (second rivit) :lapset) yhteensa-otsikon-index))))]
                                           (-> osa
                                               (p/lisaa-renderointi-derefable! tiedot/suunnittelu-kustannussuunnitelma [polku-muokkausrivin-yhteensa-osaan] app)
                                               (p/lisaa-muodosta-arvo (fn [this {maara-kk-osat :uusi}]
                                                                        (let [yhteensa-osan-arvo (-> maara-kk-osat vals first (tyokalut/arvo :arvo))]
                                                                          (if (number? yhteensa-osan-arvo)
                                                                            (* 5 yhteensa-osan-arvo)
                                                                            0))))))))))

(defn paivita-hankinta-summat-automaattisesti [taulukko polku-taulukkoon app]
  (let [yhteensa-otsikon-index (p/otsikon-index taulukko "Yhteensä")
        summa-rivin-index (-> taulukko (tyokalut/arvo :lapset) count dec)]
    (-> taulukko
        (tyokalut/paivita-asiat-taulukossa [:laajenna-lapsilla 0 yhteensa-otsikon-index]
                                           (fn [taulukko taulukon-asioiden-polut]
                                            (let [[rivit laajenna-lapsilla-rivi laajenna-lapsilla-rivit paarivi osat osa] taulukon-asioiden-polut
                                                  laajenna-lapsilla-rivit (get-in taulukko laajenna-lapsilla-rivit)
                                                  osa (get-in taulukko osa)

                                                  polut-summa-osiin (map (fn [laajenna-rivi]
                                                                           (into []
                                                                                 (apply concat polku-taulukkoon
                                                                                        (p/osan-polku-taulukossa taulukko
                                                                                                                 (nth (tyokalut/arvo laajenna-rivi :lapset)
                                                                                                                      yhteensa-otsikon-index)))))
                                                                         (rest laajenna-lapsilla-rivit))]
                                              (-> osa
                                                  (p/lisaa-renderointi-derefable! tiedot/suunnittelu-kustannussuunnitelma polut-summa-osiin app)
                                                  (p/lisaa-muodosta-arvo (fn [this {summa-osat :uusi}]
                                                                           (apply + (map (fn [osa]
                                                                                           (let [arvo (tyokalut/arvo osa :arvo)]
                                                                                             (if (number? arvo)
                                                                                               arvo 0)))
                                                                                         (vals summa-osat)))))))))
        (tyokalut/paivita-asiat-taulukossa [summa-rivin-index yhteensa-otsikon-index]
                                           (fn [taulukko taulukon-asioiden-polut]
                                            (let [[rivit rivi osat osa] taulukon-asioiden-polut
                                                  rivit (get-in taulukko rivit)
                                                  osa (get-in taulukko osa)
                                                  polut-yhteenlasku-osiin (apply concat
                                                                                 (keep (fn [rivi]
                                                                                         (when (= :laajenna-lapsilla (p/rivin-skeema taulukko rivi))
                                                                                           (map (fn [index]
                                                                                                  (into [] (apply concat polku-taulukkoon
                                                                                                                  (p/osan-polku-taulukossa taulukko (tyokalut/get-in-riviryhma rivi [index yhteensa-otsikon-index])))))
                                                                                                (range 1 (count (tyokalut/arvo rivi :lapset))))))
                                                                                       rivit))]
                                              (-> osa
                                                  (p/lisaa-renderointi-derefable! tiedot/suunnittelu-kustannussuunnitelma polut-yhteenlasku-osiin app)
                                                  (p/lisaa-muodosta-arvo (fn [this {yhteenlasku-osat :uusi}]
                                                                           (apply + (map (fn [osa]
                                                                                           (let [arvo (tyokalut/arvo osa :arvo)]
                                                                                             (if (number? arvo)
                                                                                               arvo 0)))
                                                                                         (vals yhteenlasku-osat))))))))))))

(extend-protocol tuck/Event
  Hoitokausi
  (process-event [_ app]
    (let [hoitovuoden-pvmt (pvm/paivamaaran-hoitokausi (pvm/nyt))
          urakan-aloitusvuosi (pvm/vuosi (-> @tiedot/yleiset :urakka :alkupvm))
          kuluva-urakan-vuosi (inc (- urakan-aloitusvuosi (pvm/vuosi (first hoitovuoden-pvmt))))]
      (assoc app :kuluva-hoitokausi {:vuosi kuluva-urakan-vuosi
                                     :pvmt hoitovuoden-pvmt})))
  PaivitaToimenpideTaulukko
  (process-event [{:keys [maara-solu polku-taulukkoon]} app]
    (let [kopioidaan-tuleville-vuosille? (get-in app [:hankintakustannukset :valinnat :kopioidaan-tuleville-vuosille?])
          taulukko (get-in app polku-taulukkoon)
          arvo (:value (tyokalut/arvo maara-solu :arvo))
          [polku-container-riviin polku-riviin polku-soluun] (p/osan-polku-taulukossa taulukko maara-solu)
          muokattu-hoitokausi (:hoitokausi (get-in taulukko polku-container-riviin))
          tulevien-vuosien-rivien-indexit (when kopioidaan-tuleville-vuosille?
                                            (keep-indexed (fn [index rivi]
                                                            (when (> (:hoitokausi rivi) muokattu-hoitokausi)
                                                              index))
                                                          (tyokalut/arvo taulukko :lapset)))
          yhteensa-otsikon-index (p/otsikon-index taulukko "Yhteensä")
          maara-otsikon-index (p/otsikon-index taulukko "Määrä")
          uusi-taulukko (tyokalut/paivita-asiat-taulukossa taulukko [:laajenna-lapsilla (last polku-riviin)]
                                                           (fn [taulukko taulukon-asioiden-polut]
                                                             (let [[rivit hoitokauden-container laajenna-lapsilla-rivit rivi] taulukon-asioiden-polut
                                                                   hoitokauden-container (get-in taulukko hoitokauden-container)
                                                                   rivi (get-in taulukko rivi)
                                                                   kasiteltavan-rivin-hoitokausi (:hoitokausi hoitokauden-container)
                                                                   arvo-paivitetaan? (or (= kasiteltavan-rivin-hoitokausi muokattu-hoitokausi)
                                                                                         (and kopioidaan-tuleville-vuosille?
                                                                                              (some #(= kasiteltavan-rivin-hoitokausi %)
                                                                                                    tulevien-vuosien-rivien-indexit)))]
                                                               (if arvo-paivitetaan?
                                                                 (tyokalut/paivita-arvo rivi :lapset
                                                                                        (fn [osat]
                                                                                          (tyokalut/mapv-indexed (fn [index osa]
                                                                                                                   (cond
                                                                                                                     (= index yhteensa-otsikon-index) (tyokalut/aseta-arvo osa :arvo arvo)
                                                                                                                     (= index maara-otsikon-index) (tyokalut/paivita-arvo osa :arvo assoc :value arvo)
                                                                                                                     :else osa))
                                                                                                                 osat)))
                                                                 rivi))))]
      (p/paivita-taulukko! uusi-taulukko app)))

  PaivitaKustannussuunnitelmanYhteenvedot
  (process-event [_ app]
    (update-in app [:hankintakustannukset :yhteenveto]
               (fn [yhteenvedot]
                 (reduce (fn [yhteenvedot hoitokausi]
                           (assoc-in yhteenvedot [(dec hoitokausi) :summa] (yhteensa-yhteenveto hoitokausi app)))
                         yhteenvedot (range 1 6)))))
  HaeKustannussuunnitelma
  (process-event [{:keys [hankintojen-taulukko rahavarausten-taulukko johto-ja-hallintokorvaus-laskulla-taulukko
                          johto-ja-hallintokorvaus-yhteenveto-taulukko toimistokulut-taulukko johtopalkkio-taulukko]} app]
    (let [urakka-id (-> @tiedot/tila :yleiset :urakka :id)]
      (-> app
          (tuck-apurit/post! :budjettitavoite
                             {:urakka-id urakka-id}
                             {:onnistui ->HaeTavoiteJaKattohintaOnnistui
                              :epaonnistui ->HaeTavoiteJaKattohintaEpaonnistui
                              :paasta-virhe-lapi? true})
          (tuck-apurit/post! :budjetoidut-tyot
                             {:urakka-id urakka-id}
                             {:onnistui ->HaeHankintakustannuksetOnnistui
                              :onnistui-parametrit [hankintojen-taulukko rahavarausten-taulukko
                                                    johto-ja-hallintokorvaus-laskulla-taulukko johto-ja-hallintokorvaus-yhteenveto-taulukko toimistokulut-taulukko
                                                    johtopalkkio-taulukko]
                              :epaonnistui ->HaeHankintakustannuksetEpaonnistui
                              :paasta-virhe-lapi? true}))))
  HaeTavoiteJaKattohintaOnnistui
  (process-event [{vastaus :vastaus} app]
    (println "HAE TAVOITE JA KATTOHINTA ONNISTUI")
    (assoc app :tavoitehinnat (mapv (fn [{:keys [tavoitehinta hoitokausi]}]
                                      {:summa tavoitehinta
                                       :hoitokausi hoitokausi})
                                    vastaus)
               :kattohinnat (mapv (fn [{:keys [kattohinta hoitokausi]}]
                                    {:summa kattohinta
                                     :hoitokausi hoitokausi})
                                  vastaus)))
  HaeTavoiteJaKattohintaEpaonnistui
  (process-event [{vastaus :vastaus} app]
    ;;TODO
    (println "HAE TAVOITE JA KATTOHINTA EPÄONNISTUI")
    app)
  HaeHankintakustannuksetOnnistui
  (process-event [{:keys [vastaus hankintojen-taulukko rahavarausten-taulukko johto-ja-hallintokorvaus-laskulla-taulukko
                          johto-ja-hallintokorvaus-yhteenveto-taulukko toimistokulut-taulukko johtopalkkio-taulukko]}
                  {{valinnat :valinnat} :hankintakustannukset :as app}]
    (println "HAE HANKINTAKUSTANNUKSET ONNISTUI")
    (let [hankintojen-pohjadata (hankinnat-pohjadata)
          hankintojen-taydennys-fn (fn [hankinnat]
                                     (sequence
                                       (comp
                                         (mapcat (fn [toimenpide]
                                                   (tyokalut/generoi-pohjadata identity
                                                                               hankintojen-pohjadata
                                                                               (filter #(= (:toimenpide %) (get toimenpiteiden-avaimet toimenpide))
                                                                                       hankinnat)
                                                                               {:summa ""
                                                                                :toimenpide (get toimenpiteiden-avaimet toimenpide)})))
                                         (map (fn [{:keys [vuosi kuukausi] :as data}]
                                                (assoc data :pvm (pvm/luo-pvm vuosi (dec kuukausi) 15)))))
                                       toimenpiteet))
          hankinnat (:kiinteahintaiset-tyot vastaus)
          hankinnat-laskutukseen-perustuen (filter #(= (:tyyppi % "laskutettava-tyo"))
                                                   (:kustannusarvioidut-tyot vastaus))
          hankinnat-hoitokausille (hankintojen-taydennys-fn hankinnat)
          hankinnat-laskutukseen-perustuen-hoitokausille (hankintojen-taydennys-fn hankinnat-laskutukseen-perustuen)
          laskutukseen-perustuvat-toimenpiteet (reduce (fn [toimenpide-avaimet toimenpide]
                                                         (conj toimenpide-avaimet
                                                               (some #(when (= (second %) toimenpide)
                                                                        (first %))
                                                                     toimenpiteiden-avaimet)))
                                                       #{} (distinct
                                                             (map :toimenpide hankinnat-laskutukseen-perustuen-hoitokausille)))
          hankinnat-toimenpiteittain (group-by :toimenpide hankinnat-hoitokausille)
          hankinnat-laskutukseen-perustuen-toimenpiteittain (group-by :toimenpide hankinnat-laskutukseen-perustuen-hoitokausille)
          hankinnat-hoitokausittain (group-by #(pvm/paivamaaran-hoitokausi (:pvm %))
                                              (concat hankinnat-laskutukseen-perustuen-hoitokausille hankinnat-hoitokausille))

          rahavarausten-pohjadata (rahavarauket-pohjadata)
          rahavarausten-taydennys-fn (fn [rahavaraukset]
                                       (mapcat (fn [toimenpide]
                                              (tyokalut/generoi-pohjadata identity
                                                                          rahavarausten-pohjadata
                                                                          (filter #(= (:toimenpide %) (get toimenpiteiden-avaimet toimenpide))
                                                                                  rahavaraukset)
                                                                          {:summa ""
                                                                           :toimenpide (get toimenpiteiden-avaimet toimenpide)}))
                                            toimenpiteet-rahavarauksilla))
          ;; Kantaan ollaan tallennettu kk-tasolla, koska integroituvat järjestelmät näin haluaa. Kumminkin frontilla
          ;; näytetään vain yksi rivi, joka on sama jokaiselle kk ja vuodelle
          ;; TODO Muut tilaajan rahavaraukset tuohon settiin
          rahavaraukset (distinct (keep #(when (#{"vahinkojen-korjaukset" "akillinen-hoitotyo"} (:tyyppi %))
                                           (select-keys % #{:tyyppi :summa :toimenpide}))
                                        (:kustannusarvioidut-tyot vastaus)))
          rahavaraukset-hoitokausile (rahavarausten-taydennys-fn rahavaraukset)
          rahavarauket-toimenpiteittain (group-by :toimenpide rahavaraukset-hoitokausile)

          jh-laskulla [] ;; TODO tämä kannasta
          jh-laskut-pohjadata (jh-laskulla-pohjadata)
          jh-laskut (tyokalut/generoi-pohjadata identity
                                                jh-laskut-pohjadata
                                                jh-laskulla
                                                {:tunnit-kk ""
                                                 :tuntipalkka ""
                                                 :yhteensa-kk ""})

          jh-yhteenvedot [] ;; TODO tämä kannasta
          jh-yhteenvedot-pohjadata (jh-laskulla-pohjadata)  ;; sama pohjadata kuin laskulla
          jh-yhteenveto (tyokalut/generoi-pohjadata identity
                                                    jh-yhteenvedot-pohjadata
                                                    jh-yhteenvedot
                                                    {:hoitokausi-1 ""
                                                     :hoitokausi-2 ""
                                                     :hoitokausi-3 ""
                                                     :hoitokausi-4 ""
                                                     :hoitokausi-5 ""})

          jh-toimistokulut [] ;; TODO tämä kannasta
          jh-toimistokulut-pohjadata [{:nimi "Toimistokulut"}]
          jh-toimistokulut (tyokalut/generoi-pohjadata identity
                                                       jh-toimistokulut-pohjadata
                                                       jh-toimistokulut
                                                       {:maara ""
                                                        :yhteensa ""})

          johtopalkkio [] ;; TODO tämä kannasta
          johtopalkkio-pohjadata [{:nimi "Hoidonjohtopalkkio"}]
          johtopalkkio (tyokalut/generoi-pohjadata identity
                                                   johtopalkkio-pohjadata
                                                   johtopalkkio
                                                   {:maara ""
                                                    :yhteensa ""})

          valinnat (assoc valinnat :laskutukseen-perustuen laskutukseen-perustuvat-toimenpiteet)

          app (-> app
                  (assoc-in [:hankintakustannukset :valinnat :laskutukseen-perustuen] laskutukseen-perustuvat-toimenpiteet)
                  (assoc-in [:hankintakustannukset :yhteenveto] (into []
                                                                      (map-indexed (fn [index [_ tiedot]]
                                                                                     {:hoitokausi (inc index)
                                                                                      :summa (+
                                                                                               ;; Hankintakustannukset
                                                                                               (apply + (map #(if (number? (:summa %))
                                                                                                                (:summa %) 0)
                                                                                                             tiedot))
                                                                                               ;; Rahavaraukset
                                                                                               (* 12 (apply + (map :summa rahavaraukset))))})
                                                                                   hankinnat-hoitokausittain)))
                  (assoc-in [:hankintakustannukset :toimenpiteet]
                            (into {}
                                  (map (fn [[toimenpide-avain toimenpide-nimi]]
                                         [toimenpide-avain (hankintojen-taulukko (get hankinnat-toimenpiteittain toimenpide-nimi) valinnat toimenpide-avain true false)])
                                       toimenpiteiden-avaimet)))
                  (assoc-in [:hankintakustannukset :toimenpiteet-laskutukseen-perustuen]
                            (into {}
                                  (map (fn [[toimenpide-avain toimenpide-nimi]]
                                         [toimenpide-avain (hankintojen-taulukko (get hankinnat-laskutukseen-perustuen-toimenpiteittain toimenpide-nimi) valinnat toimenpide-avain true true)])
                                       toimenpiteiden-avaimet)))
                  (assoc-in [:hankintakustannukset :rahavaraukset]
                            (into {}
                                  (keep (fn [[toimenpide-avain toimenpide-nimi]]
                                          (when (toimenpiteet-rahavarauksilla toimenpide-avain)
                                            [toimenpide-avain (rahavarausten-taulukko (get rahavarauket-toimenpiteittain toimenpide-nimi) valinnat toimenpide-avain true)]))
                                       toimenpiteiden-avaimet)))
                  (assoc-in [:hallinnolliset-toimenpiteet :johto-ja-hallintokorvaus-laskulla] (johto-ja-hallintokorvaus-laskulla-taulukko jh-laskut true))
                  (assoc-in [:hallinnolliset-toimenpiteet :johto-ja-hallintokorvaus-yhteenveto] (johto-ja-hallintokorvaus-yhteenveto-taulukko jh-yhteenveto true))
                  (assoc-in [:hallinnolliset-toimenpiteet :toimistokulut] (toimistokulut-taulukko (first jh-toimistokulut) true))
                  (assoc-in [:hallinnolliset-toimenpiteet :johtopalkkio] (johtopalkkio-taulukko (first johtopalkkio) true)))]
      (tarkista-datan-validius! hankinnat hankinnat-laskutukseen-perustuen)
      (-> app
          (update-in [:hankintakustannukset :toimenpiteet]
                     (fn [toimenpiteet]
                       (into {}
                             (map (fn [[toimenpide-avain toimenpide]]
                                    [toimenpide-avain (paivita-hankinta-summat-automaattisesti toimenpide
                                                                                               [:hankintakustannukset :toimenpiteet toimenpide-avain]
                                                                                               app)])
                                  toimenpiteet))))
          (update-in [:hankintakustannukset :toimenpiteet-laskutukseen-perustuen]
                     (fn [toimenpiteet]
                       (into {}
                             (map (fn [[toimenpide-avain toimenpide]]
                                    [toimenpide-avain (paivita-hankinta-summat-automaattisesti toimenpide
                                                                                               [:hankintakustannukset :toimenpiteet-laskutukseen-perustuen toimenpide-avain]
                                                                                               app)])
                                  toimenpiteet))))
          (update-in [:hankintakustannukset :rahavaraukset]
                     (fn [rahavaraukset-toimenpiteittain]
                       (into {}
                             (map (fn [[toimenpide rahavaraus]]
                                    [toimenpide (paivita-rahavaraus-summat-automaattisesti rahavaraus
                                                                                           [:hankintakustannukset :rahavaraukset toimenpide]
                                                                                           app)])
                                  rahavaraukset-toimenpiteittain))))
          (update-in [:hallinnolliset-toimenpiteet :toimistokulut]
                     (fn [toimistokulut]
                       (paivita-maara-kk-taulukon-summat-automaattisesti toimistokulut
                                                                         [:hallinnolliset-toimenpiteet :toimistokulut]
                                                                         app))))))
  HaeHankintakustannuksetEpaonnistui
  (process-event [{vastaus :vastaus} app]
    ;; TODO
    (println "HAE HANKINTAKUSTANNUKSET EPÄONNISTUI")
    app)
  LaajennaSoluaKlikattu
  (process-event [{:keys [polku-taulukkoon rivin-id auki?]} app]
    (let [rivin-container (tyokalut/rivin-vanhempi (get-in app (conj polku-taulukkoon :rivit))
                                                   rivin-id)
          toggle-fn (if auki? disj conj)]
      (p/paivita-taulukko! (update (get-in app polku-taulukkoon) :rivit
                                   (fn [rivit]
                                     (mapv (fn [rivi]
                                             (if (p/janan-id? rivi (p/janan-id rivin-container))
                                               (update rivi :janat (fn [[paa & lapset]]
                                                                     (into []
                                                                           (cons paa
                                                                                 (map #(update % :luokat toggle-fn "piillotettu") lapset)))))
                                               rivi))
                                           rivit)))
                           app)))
  MuutaTaulukonOsa
  (process-event [{:keys [osa arvo polku-taulukkoon]} app]
    (p/paivita-solu! (get-in app polku-taulukkoon)
                     (tyokalut/aseta-arvo osa :arvo arvo)
                     app))
  MuutaTaulukonOsanSisarta
  (process-event [{:keys [osa sisaren-tunniste polku-taulukkoon arvo]} app]
    (let [taulukko (get-in app polku-taulukkoon)
          sisar-osa (tyokalut/osan-sisar taulukko osa sisaren-tunniste)]
      (p/paivita-solu! taulukko
                       (tyokalut/aseta-arvo sisar-osa :arvo arvo)
                       app)))
  PaivitaTaulukonOsa
  (process-event [{:keys [osa polku-taulukkoon paivitys-fn]} app]
    (p/paivita-solu! (get-in app polku-taulukkoon)
                     (tyokalut/paivita-arvo osa :arvo paivitys-fn)
                     app))
  TaytaAlas
  (process-event [{:keys [maara-solu polku-taulukkoon]} app]
    (let [kopioidaan-tuleville-vuosille? (get-in app [:hankintakustannukset :valinnat :kopioidaan-tuleville-vuosille?])
          taulukko (get-in app polku-taulukkoon)
          [polku-container-riviin polku-riviin polku-soluun] (p/osan-polku-taulukossa taulukko maara-solu)
          muokattu-hoitokausi (:hoitokausi (get-in taulukko polku-container-riviin))
          tulevien-vuosien-rivien-indexit (when kopioidaan-tuleville-vuosille?
                                            (keep-indexed (fn [index rivi]
                                                            (when (> (:hoitokausi rivi) muokattu-hoitokausi)
                                                              index))
                                                          (tyokalut/arvo taulukko :lapset)))
          paivitettavien-yhteenvetojen-hoitokaudet (if kopioidaan-tuleville-vuosille?
                                                     (keep (fn [rivi]
                                                             (when (>= (:hoitokausi rivi) muokattu-hoitokausi)
                                                               (:hoitokausi rivi)))
                                                           (tyokalut/arvo taulukko :lapset))
                                                     [muokattu-hoitokausi])
          tayta-rivista-eteenpain (first (keep-indexed (fn [index rivi]
                                                         (when (p/osan-polku rivi maara-solu)
                                                           index))
                                                       (tyokalut/arvo (get-in taulukko polku-container-riviin) :lapset)))
          value (:value (tyokalut/arvo maara-solu :arvo))
          maara-otsikon-index (p/otsikon-index taulukko "Määrä")
          yhteensa-otsikon-index (p/otsikon-index taulukko "Yhteensä")
          uusi-taulukko (tyokalut/paivita-asiat-taulukossa taulukko [:laajenna-lapsilla]
                                                           (fn [taulukko taulukon-asioiden-polut]
                                                             (let [[rivit hoitokauden-container] taulukon-asioiden-polut
                                                                   hoitokauden-container (get-in taulukko hoitokauden-container)
                                                                   kasiteltavan-rivin-hoitokausi (:hoitokausi hoitokauden-container)
                                                                   arvo-paivitetaan? (or (= kasiteltavan-rivin-hoitokausi muokattu-hoitokausi)
                                                                                         (and kopioidaan-tuleville-vuosille?
                                                                                              (some #(= kasiteltavan-rivin-hoitokausi %)
                                                                                                    tulevien-vuosien-rivien-indexit)))]
                                                               (if arvo-paivitetaan?
                                                                 (tyokalut/paivita-arvo hoitokauden-container :lapset
                                                                                        (fn [rivit]
                                                                                          (tyokalut/mapv-range tayta-rivista-eteenpain
                                                                                                               (fn [maara-rivi]
                                                                                                                 (tyokalut/paivita-arvo maara-rivi
                                                                                                                                        :lapset
                                                                                                                                        (fn [osat]
                                                                                                                                          (tyokalut/mapv-indexed
                                                                                                                                            (fn [index osa]
                                                                                                                                              (cond
                                                                                                                                                (= index maara-otsikon-index) (tyokalut/paivita-arvo osa :arvo assoc :value value)
                                                                                                                                                (= index yhteensa-otsikon-index) (tyokalut/aseta-arvo osa :arvo value)
                                                                                                                                                :else osa))
                                                                                                                                            osat))))
                                                                                                               rivit)))
                                                                 hoitokauden-container))))
          app (p/paivita-taulukko! uusi-taulukko app)]
      (update-in app [:hankintakustannukset :yhteenveto]
                 (fn [yhteenvedot]
                   (reduce (fn [yhteenvedot hoitokausi]
                             (assoc-in yhteenvedot [(dec hoitokausi) :summa] (yhteensa-yhteenveto hoitokausi app)))
                           yhteenvedot paivitettavien-yhteenvetojen-hoitokaudet)))))
  ToggleHankintakustannuksetOtsikko
  (process-event [{:keys [kylla?]} app]
    (let [toimenpide-avain (get-in app [:hankintakustannukset :valinnat :toimenpide])
          polku [:hankintakustannukset :toimenpiteet toimenpide-avain]
          taulukko (get-in app polku)]
      (p/paivita-taulukko! (tyokalut/paivita-asiat-taulukossa taulukko [0 "Nimi"]
                                                              (fn [taulukko taulukon-asioiden-polut]
                                                                (let [[rivit rivi osat osa] taulukon-asioiden-polut
                                                                      osa (get-in taulukko osa)]
                                                                  (tyokalut/aseta-arvo osa :arvo
                                                                                       (if kylla?
                                                                                         "Kiinteät" " ")))))
                           app)))
  PaivitaJHRivit
  (process-event [{:keys [paivitetty-osa]} app]
    ;; Nämä arvothan voisi päivittää automaattisesti Taulukon TilanSeuranta protokollan avulla, mutta se aiheuttaisi
    ;; saman agregoinnin useaan kertaan. Lasketaan tässä kerralla kaikki tarvittava.
    (let [laskulla-taulukon-polku [:hallinnolliset-toimenpiteet :johto-ja-hallintokorvaus-laskulla]
          yhteenveto-taulukon-polku [:hallinnolliset-toimenpiteet :johto-ja-hallintokorvaus-yhteenveto]
          laskulla-taulukko (get-in app laskulla-taulukon-polku)
          yhteenveto-taulukko (get-in app yhteenveto-taulukon-polku)
          tunnit-sarakkeen-index (p/otsikon-index laskulla-taulukko "Tunnit/kk")
          tuntipalkka-sarakkeen-index (p/otsikon-index laskulla-taulukko "Tuntipalkka")
          kk-v-sarakkeen-index (p/otsikon-index laskulla-taulukko "kk/v")
          [rivin-polku _] (p/osan-polku-taulukossa laskulla-taulukko paivitetty-osa)
          laskulla-taulukko (tyokalut/paivita-asiat-taulukossa laskulla-taulukko [(last rivin-polku) "Yhteensä/kk"]
                                                               (fn [taulukko polut]
                                                                 (let [[rivit rivi osat osa] polut
                                                                       osat (get-in taulukko osat)
                                                                       yhteensaosa (get-in taulukko osa)
                                                                       tunnit (tyokalut/arvo (nth osat tunnit-sarakkeen-index) :arvo)
                                                                       tunnit (if (number? tunnit) tunnit 0)
                                                                       tuntipalkka (tyokalut/arvo (nth osat tuntipalkka-sarakkeen-index) :arvo)
                                                                       tuntipalkka (if (number? tuntipalkka) tuntipalkka 0)]
                                                                   (tyokalut/aseta-arvo yhteensaosa :arvo (* tunnit tuntipalkka)))))
          kuluva-hoitovuosi (get-in app [:kuluva-hoitokausi :vuosi])
          yhteenveto-taulukko (tyokalut/paivita-asiat-taulukossa yhteenveto-taulukko [(last rivin-polku) (str kuluva-hoitovuosi ".vuosi/€")]
                                                                 (fn [taulukko polut]
                                                                   (let [[rivit rivi osat osa] polut
                                                                         vuosi-yhteensa-osa (get-in taulukko osa)
                                                                         laskulla-taulukon-rivin-osat (tyokalut/arvo (get-in laskulla-taulukko rivin-polku) :lapset)
                                                                         tunnit (tyokalut/arvo (nth laskulla-taulukon-rivin-osat tunnit-sarakkeen-index) :arvo)
                                                                         tunnit (if (number? tunnit) tunnit 0)
                                                                         tuntipalkka (tyokalut/arvo (nth laskulla-taulukon-rivin-osat tuntipalkka-sarakkeen-index) :arvo)
                                                                         tuntipalkka (if (number? tuntipalkka) tuntipalkka 0)
                                                                         kk-v (tyokalut/arvo (nth laskulla-taulukon-rivin-osat kk-v-sarakkeen-index) :arvo)]
                                                                     (tyokalut/aseta-arvo vuosi-yhteensa-osa :arvo (* tunnit tuntipalkka kk-v)))))]
      (->> app
           (p/paivita-taulukko! laskulla-taulukko)
           (p/paivita-taulukko! yhteenveto-taulukko)))))