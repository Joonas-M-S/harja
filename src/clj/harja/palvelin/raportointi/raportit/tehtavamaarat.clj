(ns harja.palvelin.raportointi.raportit.tehtavamaarat
  "Tehtävämääräraportti"
  (:require [harja.kyselyt
             [urakat :as urakat-q]
             [tehtavamaarat :as tm-q]
             [toteumat :as toteuma-q]]
            [harja.pvm :as pvm])
  (:import (java.math RoundingMode)))

(defn- laske-hoitokaudet
  [alkupvm loppupvm]
  (let [alku-hoitokausi (if (pvm/ennen? alkupvm (pvm/hoitokauden-alkupvm (pvm/vuosi alkupvm)))
                          (dec (pvm/vuosi alkupvm))
                          (pvm/vuosi alkupvm))
        loppu-hoitokausi (if (pvm/ennen? loppupvm (pvm/hoitokauden-alkupvm (pvm/vuosi loppupvm)))
                           (dec (pvm/vuosi loppupvm))
                           (pvm/vuosi loppupvm))]
    (range alku-hoitokausi (inc loppu-hoitokausi))))

(defn- hae-tehtavamaarat
  [db {:keys [urakka-id hallintayksikko-id alkupvm loppupvm]}]
  (let [hoitokaudet (laske-hoitokaudet alkupvm loppupvm)]
    (cond
      hallintayksikko-id (tm-q/hae-hallintayksikon-tehtavamaarat-ja-toteumat-aikavalilla
                           db
                           {:alkupvm         alkupvm
                            :loppupvm        loppupvm
                            :hoitokausi      hoitokaudet
                            :hallintayksikko hallintayksikko-id})
      urakka-id (tm-q/hae-urakan-tehtavamaarat-ja-toteumat-aikavalilla
                  db
                  {:urakka     urakka-id
                   :alkupvm    alkupvm
                   :loppupvm   loppupvm
                   :hoitokausi hoitokaudet})
      :oletus (tm-q/hae-kaikki-tehtavamaarat-ja-toteumat-aikavalilla
                db
                {:alkupvm    alkupvm
                 :loppupvm   loppupvm
                 :hoitokausi hoitokaudet}))))

(defn- laske-toteuma-%
  [rivi]
  (let [[_ _ suunniteltu toteuma] rivi
        toteuma-% (when (-> rivi
                            count
                            (> 1))
                    (cond
                      (zero? toteuma) ""
                      (zero? suunniteltu) "!"
                      :oletus (* (.divide toteuma suunniteltu 4 RoundingMode/HALF_UP) 100)))]
    (keep identity
          (conj (into [] rivi) toteuma-%))))

(defn- null->0
  [kvp]
  (let [[avain arvo] kvp]
    [avain (if (nil? arvo) 0 arvo)]))

(defn- null-arvot-nollaksi-rivilla
  [rivi]
  (into {} (map null->0 rivi)))

(defn- nayta-vain-toteuma-suunnitteluyksikko-!=-yksikko
  [{:keys [yksikko suunniteltu suunnitteluyksikko] :as rivi}]
  (if (not= 1 (count (keys rivi)))
    (let [rivi (select-keys rivi [:nimi :toteuma :yksikko])]
      (if (= yksikko suunnitteluyksikko)
        (assoc rivi :suunniteltu suunniteltu)
        (assoc rivi :suunniteltu 0)))
    rivi))

(defn- ota-tarvittavat-arvot
  [m]
  (vals
    (select-keys m
                 [:nimi :yksikko :suunniteltu :toteuma])))

(defn- muodosta-otsikot
  [m]
  (if (= 1 (count (keys m)))
    {:rivi (conj (vec m) "" "" "" "") :korosta-hennosti? true :lihavoi? true}
    (vec m)))

(defn- muodosta-taulukko
  [db user parametrit]
  (let [xform (map (fn [[_ rivit]]
                     [[(:urakka (first rivit)) (:jarjestys (first rivit))] rivit]))
        suunnitellut-ryhmissa (->> parametrit
                                   (hae-tehtavamaarat db)
                                   (sort-by :jarjestys)
                                   (group-by :toimenpide))
        suunnitellut-ryhmissa-muunnetut-avaimet (into {}
                                                      xform
                                                      suunnitellut-ryhmissa)
        suunnitellut-sortattu (into
                                (sorted-map-by
                                  (fn [a b]
                                    (compare a b)))
                                suunnitellut-ryhmissa-muunnetut-avaimet)
        suunnitellut-flattina (mapcat second suunnitellut-sortattu)
        suunnitellut-valiotsikoineen (keep identity
                                           (loop [rivit suunnitellut-flattina
                                                  toimenpide nil
                                                  kaikki []]
                                             (if (empty? rivit)
                                               kaikki
                                               (let [rivi (first rivit)
                                                     uusi-toimenpide? (not= toimenpide (:toimenpide rivi))
                                                     toimenpide (if uusi-toimenpide?
                                                                  (:toimenpide rivi)
                                                                  toimenpide)]
                                                 (recur (rest rivit)
                                                        toimenpide
                                                        (conj kaikki
                                                              (when uusi-toimenpide?
                                                                {:nimi toimenpide})
                                                              rivi))))))
        muodosta-rivi (comp
                        (map nayta-vain-toteuma-suunnitteluyksikko-!=-yksikko)
                        (map null-arvot-nollaksi-rivilla)
                        (map ota-tarvittavat-arvot)
                        (map laske-toteuma-%)
                        (map muodosta-otsikot))
        rivit (into [] muodosta-rivi suunnitellut-valiotsikoineen)]
    {:rivit   rivit
     :otsikot [{:otsikko "Tehtävä" :leveys 6}
               {:otsikko "Yksikkö" :leveys 1}
               {:otsikko "Suunniteltu määrä" :leveys 2 :fmt :numero}
               {:otsikko "Toteuma" :leveys 2 :fmt :numero}
               {:otsikko "Toteuma-%" :leveys 2}]}))

(defn suorita
  [db user {:keys [alkupvm loppupvm] :as params}]
  (println params)
  (let [{:keys [otsikot rivit debug]} (muodosta-taulukko db user params)]
    [:raportti
     {:nimi "Tehtävämäärät"}
     #_[:teksti (str "helou" (pr-str rivit))]
     #_[:teksti (str "helou" (pr-str otsikot))]
     #_[:teksti (str "helou" (pr-str debug))]
     [:taulukko
      {:otsikko    (str "Tehtävämäärät ajalta " (pvm/pvm alkupvm) "-" (pvm/pvm loppupvm))
       :sheet-nimi (str "Tehtävämäärät " (pvm/pvm alkupvm) "-" (pvm/pvm loppupvm))}
      otsikot
      rivit]]))