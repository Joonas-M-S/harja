(ns harja.tiedot.urakka.toteumat.suola
  "Tämän nimiavaruuden avulla voidaan hakea urakan suola- ja lämpötilatietoja."
  (:require [reagent.core :refer [atom] :as r]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.atom :refer-macros [reaction<!]]
            [harja.tiedot.urakka :as tiedot-urakka]
            [harja.tiedot.navigaatio :as nav])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(declare hae-toteumat)

(defonce toteumat
  (reaction<! [hae? @suolatoteumissa?
               ur @nav/valittu-urakka
               sopimus @tiedot-urakka/valittu-sopimusnumero
               hk @tiedot-urakka/valittu-hoitokausi
               kk @tiedot-urakka/valittu-hoitokauden-kuukausi]
              {:nil-kun-haku-kaynnissa? true}
              (when (and hae? ur)
                (go
                  (into []
                        ;; luodaan kaikille id
                        (map-indexed (fn [i rivi]
                                       (assoc rivi :id i)))

                        (<! (hae-toteumat (:id ur) (first sopimus)
                                                (or kk hk))))))))

(defonce lampotilojen-hallinnassa? (atom false))

(defonce valittu-suolatoteuma (atom nil))

(def karttataso-suolatoteumat (atom false))

(defonce suolatoteumat-kartalla
  (reaction
    (let [valittu-turvallisuuspoikkeama-id (:id @valittu-suolatoteuma)]
      (when @karttataso-suolatoteumat
        (kartalla-esitettavaan-muotoon
          @toteumat
          #(= valittu-turvallisuuspoikkeama-id (:id %))
          (comp
            (apply concat (map #(:toteumat %) @toteumat ))
            (map #(assoc % :tyyppi-kartalla :turvallisuuspoikkeama))))))))

(defn hae-toteumat [urakka-id sopimus-id [alkupvm loppupvm]]
  (k/post! :hae-suolatoteumat {:urakka-id urakka-id
                               :sopimus-id sopimus-id
                               :alkupvm alkupvm
                               :loppupvm loppupvm}))

(defn tallenna-toteumat [urakka-id sopimus-id rivit]
  (let [tallennettavat (into [] (->> rivit
                                     (filter (comp not :koskematon))
                                     (map #(assoc % :paattynyt (:alkanut %)))))]
    (k/post! :tallenna-suolatoteumat
             {:urakka-id urakka-id
              :sopimus-id sopimus-id
              :toteumat tallennettavat})))

(defn hae-materiaalit []
  (k/get! :hae-suolamateriaalit))

(defn hae-lampotilat-ilmatieteenlaitokselta [talvikauden-alkuvuosi]
  (k/post! :hae-lampotilat-ilmatieteenlaitokselta {:vuosi talvikauden-alkuvuosi} nil true))

(defn hae-teiden-hoitourakoiden-lampotilat [hoitokausi]
  (k/post! :hae-teiden-hoitourakoiden-lampotilat {:hoitokausi hoitokausi}))

(def hoitokaudet
  (vec
    (let [nyt (pvm/nyt)
          tama-vuosi (pvm/vuosi nyt)
          ;; sydäntalvi on joulu-helmikuu, tarjotaan sydäntalven keskilämpöltilan hakua aikaisintaan
          ;; maaliskuussa. Ei tarpeen huomioida karkauspäivää koska manuaalinen integraatio.
          sydantalvi-ohi? (pvm/jalkeen? nyt (pvm/->pvm (str "28.2." tama-vuosi)))
          vanhin-haettava-vuosi 2005]
      (for [vuosi (range vanhin-haettava-vuosi
                         (if sydantalvi-ohi?
                           tama-vuosi
                           (dec tama-vuosi)))]
        [(pvm/hoitokauden-alkupvm vuosi) (pvm/hoitokauden-loppupvm (inc vuosi))]))))

(defonce valittu-hoitokausi (atom (last hoitokaudet)))

(defn valitse-hoitokausi! [tk]
  (reset! valittu-hoitokausi tk))

(defonce hoitourakoiden-lampotilat
  (reaction<! [lampotilojen-hallinnassa? @lampotilojen-hallinnassa?
               valittu-hoitokausi @valittu-hoitokausi]
              {:nil-kun-haku-kaynnissa? true}
              (when (and lampotilojen-hallinnassa?
                         valittu-hoitokausi)
                (hae-teiden-hoitourakoiden-lampotilat valittu-hoitokausi))))

(defn hae-urakan-suolasakot-ja-lampotilat [urakka-id]
  (k/post! :hae-urakan-suolasakot-ja-lampotilat urakka-id))

(defn aseta-suolasakon-kaytto [urakka-id kaytossa?]
  (k/post! :aseta-suolasakon-kaytto {:urakka-id urakka-id
                                     :kaytossa? kaytossa?}))

(defn tallenna-teiden-hoitourakoiden-lampotilat [hoitokausi lampotilat]
  (let [lampotilat (mapv #(assoc % :alkupvm (first hoitokausi)
                                  :loppupvm (second hoitokausi))
                         (vec (vals lampotilat)))]
    (log "tallenna lämpötilat: " (pr-str lampotilat))
    (k/post! :tallenna-teiden-hoitourakoiden-lampotilat {:hoitokausi hoitokausi
                                                         :lampotilat lampotilat})))
