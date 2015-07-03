(ns harja.tiedot.urakka.paallystys
  "Tämä nimiavaruus hallinnoi urakan päällystystietoja."
  (:require [reagent.core :refer [atom] :as r]
            [harja.asiakas.kommunikaatio :as k]
            [harja.asiakas.tapahtumat :as t]
            [cljs.core.async :refer [<! >! chan]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(defn hae-paallystyskohteet [urakka-id sopimus-id]
  (k/post! :urakan-paallystyskohteet {:urakka-id urakka-id
                                          :sopimus-id sopimus-id}))

(defn hae-paallystyskohdeosat [urakka-id sopimus-id paallystyskohde-id]
  (k/post! :urakan-paallystyskohdeosat {:urakka-id urakka-id
                                      :sopimus-id sopimus-id
                                      :paallystyskohde-id paallystyskohde-id}))

(defn hae-paallystystoteumat [urakka-id sopimus-id]
  (k/post! :urakan-paallystystoteumat {:urakka-id urakka-id
                                      :sopimus-id sopimus-id}))

(defn hae-paallystysilmoitus-paallystyskohteella [urakka-id sopimus-id paallystyskohde-id]
  (k/post! :urakan-paallystysilmoitus-paallystyskohteella {:urakka-id urakka-id
                                       :sopimus-id sopimus-id
                                       :paallystyskohde-id paallystyskohde-id}))

(defn tallenna-paallystysilmoitus [urakka-id sopimus-id lomakedata]
  (k/post! :tallenna-paallystysilmoitus {:urakka-id urakka-id
                                         :sopimus-id sopimus-id
                                         :lomakedata lomakedata}))

(defn tallenna-paallystysilmoituksen-paatos [urakka-id sopimus-id paallystyskohde-id paatostiedot]
  (k/post! :tallenna-paallystysilmoituksen-paatos {:urakka-id          urakka-id
                                                   :sopimus-id         sopimus-id
                                                   :paallystyskohde-id paallystyskohde-id
                                                   :paatostiedot       paatostiedot}))


(defonce paallystyskohteet (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                        [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                        valittu-urakan-valilehti @u/urakan-valittu-valilehti]
                                       (when (and valittu-urakka-id valittu-sopimus-id (= valittu-urakan-valilehti :kohdeluettelo))
                                         (log "PÄÄ Haetaan päällystyskohteet.")
                                         (hae-paallystyskohteet valittu-urakka-id valittu-sopimus-id))))

(defonce paallystystoteumat (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
                                         [valittu-sopimus-id _] @u/valittu-sopimusnumero
                                         valittu-urakan-valilehti @u/urakan-valittu-valilehti]
                                        (when (and valittu-urakka-id valittu-sopimus-id (= valittu-urakan-valilehti :kohdeluettelo))
                                          (log "PÄÄ Haetaan päällystystoteumat.")
                                          (hae-paallystystoteumat valittu-urakka-id valittu-sopimus-id))))
