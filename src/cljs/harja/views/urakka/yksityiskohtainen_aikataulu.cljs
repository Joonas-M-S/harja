(ns harja.views.urakka.yksityiskohtainen-aikataulu
  "Ylläpidon urakoiden yksityiskohtainen aikataulu"
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt]]
            [harja.ui.komponentti :as komp]
            [harja.domain.yllapitokohde :as ypk]
            [harja.tiedot.urakka.yksityiskohtainen-aikataulu :as tiedot]
            [harja.ui.grid :as grid]
            [harja.pvm :as pvm])
  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

(defn yksityiskohtainen-aikataulu [rivi vuosi]
  (let [yksityiskohtainen-aikataulu (atom (or (:yksityiskohtainen-aikataulu rivi) []))]
    (fn [rivi]
      [:div
       [grid/grid
        {:otsikko "Kohteen yksityiskohtainen aikataulu"
         :tyhja "Ei aikataulua"
         :tallenna tiedot/tallenna-aikataulu}
        [{:otsikko "Toimenpide"
          :leveys 10
          :nimi :toimenpide
          :tyyppi :valinta
          :validoi [[:ei-tyhja "Anna toimenpiode"]]
          :valinnat ypk/tarkan-aikataulun-toimenpiteet
          :valinta-nayta #(if % (ypk/tarkan-aikataulun-toimenpiide-fmt %) "- valitse -")
          :fmt ypk/tarkan-aikataulun-toimenpiide-fmt
          :pituus-max 128}
         {:otsikko "Kuvaus"
          :leveys 10
          :nimi :kuvaus
          :tyyppi :string
          :pituus-max 1024}
         {:otsikko "Alku"
          :leveys 5
          :nimi :alku
          :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
          :tyyppi :pvm
          :validoi [[:ei-tyhja "Anna alku"]]}
         {:otsikko "Loppu"
          :leveys 5
          :nimi :loppu
          :tyyppi :pvm
          :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
          :validoi [[:ei-tyhja "Anna loppu"]]}]
        @yksityiskohtainen-aikataulu]])))