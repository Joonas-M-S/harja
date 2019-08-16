(ns harja.tiedot.urakka.suunnittelu.mhu-kustannussuunnitelma
  (:require [clojure.string :as clj-str]
            [cljs.core.async :as async :refer [<! >! chan timeout]]
            [tuck.core :as tuck]
            [harja.pvm :as pvm]
            [harja.tyokalut.tuck :as tuck-apurit]
            [harja.tiedot.urakka.urakka :as tiedot]
            [harja.loki :refer [log]]
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

(defn hoitokausi-nyt []
  (let [hoitovuoden-pvmt (pvm/paivamaaran-hoitokausi (pvm/nyt))
        urakan-aloitusvuosi (pvm/vuosi (-> @tiedot/yleiset :urakka :alkupvm))
        kuluva-urakan-vuosi (inc (- urakan-aloitusvuosi (pvm/vuosi (first hoitovuoden-pvmt))))]
    [kuluva-urakan-vuosi hoitovuoden-pvmt]))

(defrecord HaeKustannussuunnitelma [hankintojen-taulukko])
(defrecord HaeTavoiteJaKattohintaOnnistui [vastaus])
(defrecord HaeTavoiteJaKattohintaEpaonnistui [vastaus])
(defrecord HaeHankintakustannuksetOnnistui [vastaus hankintojen-taulukko])
(defrecord HaeHankintakustannuksetEpaonnistui [vastaus])
(defrecord LaajennaSoluaKlikattu [polku-taulukkoon rivin-id this auki?])
(defrecord MuutaTaulukonOsa [osa polku-taulukkoon arvo])
(defrecord PaivitaTaulukonOsa [osa polku-taulukkoon paivitys-fn])
(defrecord PaivitaKustannussuunnitelmanYhteenvedot [maara-solu polku-taulukkoon])
(defrecord TaytaAlas [maara-solu polku-taulukkoon])

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

(extend-protocol tuck/Event
  PaivitaKustannussuunnitelmanYhteenvedot
  (process-event [{:keys [maara-solu polku-taulukkoon]} {:keys [hankintakustannukset] :as app}]
    (let [taulukko (get-in app polku-taulukkoon)
          arvo (:value (tyokalut/arvo maara-solu :arvo))
          [polku-riviin _] (p/osan-polku-taulukossa taulukko maara-solu)
          hoitokauden-container (get-in taulukko polku-riviin)
          hoitokauden-yhteensa-rivi (first (p/janan-osat hoitokauden-container))
          yhteensa-rivi-yhteensa-solu (nth (p/janan-osat hoitokauden-yhteensa-rivi)
                                           (p/otsikon-index taulukko "Yhteensä"))
          yhteensa-sarakkeen-index (p/otsikon-index taulukko "Yhteensä")
          maara-rivi-yhteensa-solu (some (fn [lapsirivi]
                                           (when (p/osan-polku lapsirivi maara-solu)
                                             (nth (p/janan-osat lapsirivi)
                                                  yhteensa-sarakkeen-index)))
                                         (p/janan-osat (get-in taulukko polku-riviin)))
          entinen-arvo (tyokalut/arvo maara-rivi-yhteensa-solu :arvo)
          arvon-muutos (- arvo entinen-arvo)
          [kuluva-hoitokausi _] (hoitokausi-nyt)
          yhteensa-rivin-arvo (+ (tyokalut/arvo yhteensa-rivi-yhteensa-solu :arvo)
                                 arvon-muutos)
          paivita-solu! (fn [app osa]
                          (p/paivita-solu! (get-in app polku-taulukkoon) osa app))]
      (-> app
          (paivita-solu! (tyokalut/aseta-arvo maara-rivi-yhteensa-solu :arvo arvo))
          (paivita-solu! (tyokalut/aseta-arvo yhteensa-rivi-yhteensa-solu :arvo yhteensa-rivin-arvo))
          (update-in [:hankintakustannukset :yhteenveto (dec kuluva-hoitokausi) :summa] + arvon-muutos))))
  HaeKustannussuunnitelma
  (process-event [{:keys [hankintojen-taulukko]} app]
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
                              :onnistui-parametrit [hankintojen-taulukko]
                              :epaonnistui ->HaeHankintakustannuksetEpaonnistui
                              :paasta-virhe-lapi? true}))))
  HaeTavoiteJaKattohintaOnnistui
  (process-event [{vastaus :vastaus} app]
    (println "HAE TAVOITE JA KATTOHINTA ONNISTUI")
    (cljs.pprint/pprint vastaus)
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
  (process-event [{:keys [vastaus hankintojen-taulukko]} {{valinnat :valinnat} :hankintakustannukset :as app}]
    (println "HAE HANKINTAKUSTANNUKSET ONNISTUI")
    (let [hankintojen-pohjadata (hankinnat-pohjadata)
          toimenpiteiden-kasittely-fn (fn [haetut-toimenpiteet]
                                        (let [valmis-data (mapcat (fn [toimenpide]
                                                                    (tyokalut/generoi-pohjadata identity
                                                                                                hankintojen-pohjadata
                                                                                                (filter #(= (:toimenpide %) (get toimenpiteiden-avaimet toimenpide))
                                                                                                        haetut-toimenpiteet)
                                                                                                {:summa 0
                                                                                                 :toimenpide (get toimenpiteiden-avaimet toimenpide)}))
                                                                  toimenpiteet)]
                                          (group-by :toimenpide
                                                    (map (fn [{:keys [vuosi kuukausi] :as maarat}]
                                                           (assoc maarat :pvm (pvm/luo-pvm vuosi (dec kuukausi) 15)))
                                                         valmis-data))))
          hankinnat (:kiinteahintaiset-tyot vastaus)
          hankinnat-laskutukseen-perustuen (filter #(= (:tyyppi % "laskutettava-tyo"))
                                                   (:kustannusarvioidut-tyot vastaus))
          toimenpiteet (toimenpiteiden-kasittely-fn hankinnat)
          toimenpiteet-laskutukseen-perustuen (toimenpiteiden-kasittely-fn hankinnat-laskutukseen-perustuen)]
      (-> app
          (assoc-in [:hankintakustannukset :yhteenveto] (into []
                                                              (map-indexed (fn [index [vuosi tiedot]]
                                                                             {:hoitokausi (inc index)
                                                                              :summa (apply + (map :summa tiedot))})
                                                                           (group-by :vuosi (concat hankinnat-laskutukseen-perustuen hankinnat)))))
          (assoc-in [:hankintakustannukset :toimenpiteet]
                    (into {}
                          (map (fn [[toimenpide-avain toimenpide-nimi]]
                                 [toimenpide-avain (hankintojen-taulukko (get toimenpiteet toimenpide-nimi) valinnat toimenpide-avain true false)])
                               toimenpiteiden-avaimet)))
          (assoc-in [:hankintakustannukset :toimenpiteet-laskutukseen-perustuen]
                    (into {}
                          (map (fn [[toimenpide-avain toimenpide-nimi]]
                                 [toimenpide-avain (hankintojen-taulukko (get toimenpiteet-laskutukseen-perustuen toimenpide-nimi) valinnat toimenpide-avain true true)])
                               toimenpiteiden-avaimet))))))
  HaeHankintakustannuksetEpaonnistui
  (process-event [{vastaus :vastaus} app]
    ;; TODO
    (println "HAE HANKINTAKUSTANNUKSET EPÄONNISTUI")
    (cljs.pprint/pprint vastaus)
    app)
  LaajennaSoluaKlikattu
  (process-event [{:keys [polku-taulukkoon rivin-id auki?]} app]
    (let [rivin-container (tyokalut/rivin-vanhempi (get-in app (conj polku-taulukkoon :rivit))
                                                   rivin-id)
          #_#__ (tyokalut/ominaisuus-predikaatilla (get-in app (conj polku-taulukkoon :rivit))
                                                   (fn [index rivi]
                                                     rivi)
                                                   (fn [rivi]
                                                     (when (= (type rivi) jana/RiviLapsilla)
                                                       (p/janan-id? (first (p/janan-osat rivi)) rivin-id))))]
      (p/paivita-taulukko! (update (get-in app polku-taulukkoon) :rivit
                                   (fn [rivit]
                                     (mapv (fn [rivi]
                                             (if (p/janan-id? rivi (p/janan-id rivin-container))
                                               (if auki?
                                                 (update rivi :janat (fn [[paa & lapset]]
                                                                       (into []
                                                                             (cons paa
                                                                                   (map #(update % :luokat disj "piillotettu") lapset)))))
                                                 (update rivi :janat (fn [[paa & lapset]]
                                                                       (into []
                                                                             (cons paa
                                                                                   (map #(update % :luokat conj "piillotettu") lapset))))))
                                               rivi))
                                           rivit)))
                           app)))
  MuutaTaulukonOsa
  (process-event [{:keys [osa arvo polku-taulukkoon]} app]
    (p/paivita-solu! (get-in app polku-taulukkoon)
                     (tyokalut/aseta-arvo osa :arvo arvo)
                     app))
  PaivitaTaulukonOsa
  (process-event [{:keys [osa polku-taulukkoon paivitys-fn]} app]
    (p/paivita-solu! (get-in app polku-taulukkoon)
                     (tyokalut/paivita-arvo osa :arvo paivitys-fn)
                     app))
  TaytaAlas
  (process-event [{:keys [maara-solu polku-taulukkoon]} app]
    (let [taulukko (get-in app polku-taulukkoon)
          [polku-riviin _] (p/osan-polku-taulukossa taulukko maara-solu)
          tayta-rivista-eteenpain (first (keep-indexed (fn [index rivi]
                                                         (when (p/osan-polku rivi maara-solu)
                                                           index))
                                                       (tyokalut/arvo (get-in taulukko polku-riviin) :lapset)))
          value (:value (tyokalut/arvo maara-solu :arvo))
          maara-otsikon-index (p/otsikon-index taulukko "Määrä")
          yhteensa-otsikon-index (p/otsikon-index taulukko "Yhteensä")
          ;; Päivitetään ensin kaikkien maararivien 'määrä' ja 'yhteensä' solut
          app (p/paivita-rivi! taulukko
                               (tyokalut/paivita-arvo (get-in taulukko polku-riviin) :lapset
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
                               app)
          taulukko (get-in app polku-taulukkoon)]
      ;; Sitten päivitetään yhteensä rivin yhteensä solu
      (p/paivita-rivi! taulukko
                       (tyokalut/paivita-arvo (get-in taulukko polku-riviin) :lapset
                                              (fn [rivit]
                                                (tyokalut/mapv-range 0 1
                                                                     (fn [yhteensa-rivi]
                                                                       (tyokalut/paivita-arvo yhteensa-rivi
                                                                                              :lapset
                                                                                              (fn [osat]
                                                                                                (tyokalut/mapv-indexed
                                                                                                  (fn [index osa]
                                                                                                    (if (= index yhteensa-otsikon-index)
                                                                                                      (tyokalut/aseta-arvo osa :arvo (apply + (map (fn [rivi]
                                                                                                                                                     (tyokalut/arvo (get (tyokalut/arvo rivi :lapset) yhteensa-otsikon-index)
                                                                                                                                                                    :arvo))
                                                                                                                                                   (rest rivit))))
                                                                                                      osa))
                                                                                                  osat))))
                                                                     rivit)))
                       app))))