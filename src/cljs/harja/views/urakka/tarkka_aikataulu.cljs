(ns harja.views.urakka.tarkka-aikataulu
  "Ylläpidon urakoiden kohteen tarkka aikataulu"
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt]]
            [harja.ui.komponentti :as komp]
            [harja.domain.yllapitokohde :as ypk]
            [harja.tiedot.urakka.tarkka-aikataulu :as tiedot]
            [harja.ui.grid :as grid]
            [harja.pvm :as pvm]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.urakka.aikataulu :as aikataulu-tiedot]
            [cljs-time.core :as t])
  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

(defn- kohteen-aikataulutaulukko [{:keys [aikataulurivi vuosi voi-tallentaa? otsikko urakka-id]}]
  (let [tarkka-aikataulu (or (filter #(= (:urakka-id %) urakka-id)
                                     (:tarkka-aikataulu aikataulurivi))
                             [])]
    [grid/grid
     {:otsikko otsikko
      :tyhja "Ei aikataulua"
      :tallenna (if voi-tallentaa?
                  #(tiedot/tallenna-aikataulu
                     {:rivit %
                      :urakka-id urakka-id
                      :yllapitokohde-id (:id aikataulurivi)
                      :onnistui-fn (fn [vastaus]
                                     (reset! aikataulu-tiedot/tarkka-aikataulu-paivitetty (t/now)))
                      :epaonnistui-fn (fn []
                                        (viesti/nayta! "Talennus epäonnistui!" :danger))})
                  :ei-mahdollinen)}
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
       :validoi [[:ei-tyhja "Anna loppu"]
                 [:pvm-kentan-jalkeen :alku "Lopun on oltava alun jälkeen"]]}]
     tarkka-aikataulu]))

(defn tarkka-aikataulu [{:keys [rivi vuosi voi-muokata-paallystys? voi-muokata-tiemerkinta?
                                paallystysurakka-id tiemerkintaurakka-id]}]
  [:div
   [kohteen-aikataulutaulukko
    {:otsikko "Kohteen päällystysurakan yksityiskohtainen aikataulu"
     :aikataulurivi rivi
     :vuosi vuosi
     :voi-tallentaa? voi-muokata-paallystys?
     :urakka-id paallystysurakka-id}]
   [kohteen-aikataulutaulukko
    {:otsikko "Kohteen tiemerkintäurakan yksityiskohtainen aikataulu"
     :aikataulurivi rivi
     :vuosi vuosi
     :voi-tallentaa? voi-muokata-tiemerkinta?
     :urakka-id tiemerkintaurakka-id}]])