(ns harja.views.vesivaylat.urakka.laadunseuranta.tarkastukset
  (:require [reagent.core :refer [atom]]
            [tuck.core :refer [tuck]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.vesivaylat.urakka.laadunseuranta.tarkastukset :as tiedot]))

(defn tarkastukset* [e! app]
  (komp/luo
    (komp/sisaan-ulos #(e! (tiedot/->Nakymassa? true))
                      #(e! (tiedot/->Nakymassa? false)))
    (fn [e! app]
      [:div {:style {:padding "10px"}}
       [:img {:src "images/harja_favicon.png"}]
       [:div {:style {:color "orange"}} "Työmaa"]])))

(defn tarkastukset []
  [tuck tiedot/tila tarkastukset*])
