(ns harja.tiedot.urakka.suunnittelu.mhu-tehtavat
  (:require [tuck.core :refer [process-event] :as tuck]
            [clojure.string :as clj-str]
            [harja.ui.taulukko.protokollat :as p]
            [harja.ui.taulukko.osa :as osa]
            [harja.ui.taulukko.tyokalut :as tyokalut]))

(defrecord MuutaTila [polku arvo])
(defrecord PaivitaTila [polku f])

(defrecord PaivitaMaara [janan-id solun-id arvo])
(defrecord LaajennaSoluaKlikattu [laajenna-osa auki?])
(defrecord JarjestaTehtavienMukaan [])
(defrecord JarjestaMaaranMukaan [])

(defn ylempi-taso?
  [ylempi-taso alempi-taso]
  (case ylempi-taso
    "ylataso" (not= "ylataso" alempi-taso)
    "valitaso" (= "alataso" alempi-taso)
    "alataso" false))

(defn klikatun-rivin-lapsenlapsi?
  [janan-id klikatun-rivin-id taulukon-rivit]
  (let [klikatun-rivin-lapset (get (group-by #(-> % meta :vanhempi)
                                             taulukon-rivit)
                                   klikatun-rivin-id)
        on-lapsen-lapsi? (some #(= janan-id (p/janan-id %))
                               klikatun-rivin-lapset)
        recursion-vastaus (cond
                            (nil? klikatun-rivin-lapset) false
                            on-lapsen-lapsi? true
                            :else (map #(klikatun-rivin-lapsenlapsi? janan-id (p/janan-id %) taulukon-rivit)
                                       klikatun-rivin-lapset))]
    (if (boolean? recursion-vastaus)
      recursion-vastaus
      (some true? recursion-vastaus))))

(defn jarjesta-tehtavan-otsikon-mukaisesti [tehtavat otsikko]
  (let [tehtavan-nimi (fn [tehtava]
                        (let [solu (some #(when (= (-> % meta :sarake) "Tehtävä")
                                            %)
                                         (p/janan-osat tehtava))]
                          (:teksti solu)))
        tehtavan-maara (fn [tehtava]
                         (let [solu (some #(when (= (-> % meta :sarake) "Määrä")
                                             %)
                                          (p/janan-osat tehtava))
                               maara (cond
                                       (= (type solu) osa/Teksti) (get solu :teksti)
                                       (= (type solu) osa/Syote) (get-in solu [:parametrit :value]))]
                           (if (= "" maara) 0 (js/parseFloat maara))))
        tehtavan-id (fn [tehtava]
                      (p/janan-id tehtava))
        vanhemman-tehtava (fn [tehtava]
                            (some #(when (= (:vanhempi (meta tehtava)) (p/janan-id %))
                                     %)
                                  tehtavat))
        tehtavan-tunniste (juxt (case otsikko
                                  :tehtava tehtavan-nimi
                                  :maara tehtavan-maara)
                                tehtavan-id)]
    (into []
          (sort-by (fn [tehtava]
                     (if (= (p/janan-id tehtava) :tehtavataulukon-otsikko)
                       []
                       (case (-> tehtava meta :tehtavaryhmatyyppi)
                         "ylataso" [(tehtavan-tunniste tehtava) nil nil]
                         "valitaso" [(tehtavan-tunniste (vanhemman-tehtava tehtava))
                                     (tehtavan-tunniste tehtava)
                                     nil]
                         "alitaso" [(-> tehtava vanhemman-tehtava vanhemman-tehtava tehtavan-tunniste)
                                    (tehtavan-tunniste (vanhemman-tehtava tehtava))
                                    (tehtavan-tunniste tehtava)])))
                   tehtavat))))

(extend-protocol tuck/Event
  MuutaTila
  (process-event [{:keys [polku arvo]} app]
    (assoc-in app polku arvo))

  PaivitaTila
  (process-event [{:keys [polku f]} app]
    (update-in app polku f))

  PaivitaMaara
  (process-event [{:keys [janan-id solun-id arvo]} app]
    (let [janan-index (tyokalut/janan-index (:tehtavat-taulukko app) janan-id)
          solun-index (p/osan-index (get-in app [:tehtavat-taulukko janan-index]) solun-id)]
      (update-in app [:tehtavat-taulukko janan-index :solut solun-index :parametrit]
                       (fn [parametrit]
                         (assoc parametrit :value arvo)))))

  LaajennaSoluaKlikattu
  (process-event [{:keys [laajenna-osa auki?]} app]
    (update app :tehtavat-taulukko
               (fn [taulukon-rivit]
                 (let [klikatun-rivin-id (first (keep (fn [rivi]
                                                        (when (p/osan-index rivi laajenna-osa)
                                                          (p/janan-id rivi)))
                                                      taulukon-rivit))]
                   (map (fn [{:keys [janan-id] :as rivi}]
                          (let [{:keys [vanhempi]} (meta rivi)
                                klikatun-rivin-lapsi? (= klikatun-rivin-id vanhempi)
                                klikatun-rivin-lapsenlapsi? (klikatun-rivin-lapsenlapsi? janan-id klikatun-rivin-id taulukon-rivit)]
                            ;; Jos joku rivi on kiinnitetty, halutaan sulkea myös kaikki lapset ja lasten lapset.
                            ;; Kumminkin lapsirivien Laajenna osan sisäinen tila jää väärään tilaan, ellei sitä säädä ulko käsin.
                            ;; Tässä otetaan ja muutetaan se oikeaksi.
                            (when (and (not auki?) klikatun-rivin-lapsenlapsi?)
                              (when-let [rivin-laajenna-osa (some #(when (instance? osa/Laajenna %)
                                                                     %)
                                                                  (:solut rivi))]
                                (reset! (p/osan-tila rivin-laajenna-osa) false)))
                            (cond
                              ;; Jos riviä klikataan, piilotetaan lapset
                              (and auki? klikatun-rivin-lapsi?) (vary-meta (update rivi :luokat disj "piillotettu")
                                                                           assoc :piillotettu? false)
                              ;; Jos rivillä on lapsen lapsia, piillotetaan myös ne
                              (and (not auki?) klikatun-rivin-lapsenlapsi?) (vary-meta (update rivi :luokat conj "piillotettu")
                                                                                       assoc :piillotettu? true)
                              :else rivi)))
                        taulukon-rivit)))))
  JarjestaTehtavienMukaan
  (process-event [_ app]
    (update app :tehtavat-taulukko
            (fn [tehtavat]
              (jarjesta-tehtavan-otsikon-mukaisesti tehtavat :tehtava))))
  JarjestaMaaranMukaan
  (process-event [_ app]
    (update app :tehtavat-taulukko
            (fn [tehtavat]
              (jarjesta-tehtavan-otsikon-mukaisesti tehtavat :maara)))))