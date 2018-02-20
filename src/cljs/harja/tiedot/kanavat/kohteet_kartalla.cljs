(ns harja.tiedot.kanavat.kohteet-kartalla
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]
            [harja.tiedot.kanavat.urakka.kanavaurakka :as kanavaurakka]
            [harja.tiedot.kanavat.urakka.toimenpiteet.kokonaishintaiset :as kokonaishintaiset]
            [harja.domain.kanavat.kohde :as kohde]
            [harja.domain.kanavat.kanavan-toimenpide :as kanavan-toimenpide]
            [clojure.set :as set])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(defonce karttataso-kohteet (atom false))

(defn- kohde-valittu? [kohde]
  (let [kokonaishintaiset-nakymassa? (:nakymassa? @kokonaishintaiset/tila)]
    (when kokonaishintaiset-nakymassa?
      (= (::kohde/id kohde) (-> @kokonaishintaiset/tila :avattu-toimenpide ::kanavan-toimenpide/kohde ::kohde/id)))))

(defonce kohteet-kartalla
  (reaction
    (when @karttataso-kohteet
      (kartalla-esitettavaan-muotoon
        (map #(-> %
                  (set/rename-keys {::kohde/sijainti :sijainti})
                  (assoc :tyyppi-kartalla :kohde)
                  (dissoc ::kohde/kohteenosat ::kohde/kohdekokonaisuus ::kohde/urakat))
             @kanavaurakka/kanavakohteet)
        kohde-valittu?
        ;#(osa-kuuluu-valittuun-kohteeseen? % @tila)
        )
      )))
