(ns harja.tiedot.kanavat.urakka.toimenpiteet.kokonaishintaiset
  (:require [reagent.core :refer [atom]]
            [tuck.core :as tuck]
            [cljs.core.async :as async]
            [harja.pvm :as pvm]
            [harja.id :refer [id-olemassa?]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(def tila (atom {:nakymassa? false}))

(defonce valinnat
         (reaction
           (log "--->> reaktiossa")
           (when (:nakymassa? @tila)
             {:urakka-id (:id @nav/valittu-urakka)
              :sopimus-id (first @u/valittu-sopimusnumero)
              :aikavali @u/valittu-aikavali
              :toimenpide @u/valittu-toimenpideinstanssi
              :urakkavuosi @u/valittu-urakan-vuosi})))

(defn hae-toimenpiteet [valinnat]
  (log "--->>> " valinnat))

(defrecord Nakymassa? [nakymassa?])
(defrecord PaivitaValinnat [valinnat])

(extend-protocol tuck/Event
  Nakymassa?
  (process-event [{nakymassa? :nakymassa?} app]
    (assoc app :nakymassa? nakymassa?))

  PaivitaValinnat
  (process-event [{:keys [valinnat] :as e} tila]
    (log "--->>> valinnat" valinnat)
    (hae-toimenpiteet valinnat)
    (update-in tila [:valinnat] merge valinnat)))

