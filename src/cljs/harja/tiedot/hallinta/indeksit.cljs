(ns harja.tiedot.hallinta.indeksit
  "Indeksien tiedot"
  (:require [reagent.core :refer [atom] :as reagent]
            [cljs.core.async :refer [<! >! chan close!]]

            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]]
            [harja.tiedot.navigaatio :as nav])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))


(def indeksit (atom nil))

 (defn hae-indeksit []
   (if (nil? @indeksit)
           (go (reset! indeksit
               (<! (k/get! :indeksit))))))

(defn tallenna-indeksi
  "Tallentaa indeksiarvot, palauttaa kanavan, josta vastauksen voi lukea."
  [nimi uudet-indeksivuodet]
  (go (let [tallennettavat
            (into []
                  (comp (filter #(not (:poistettu %))))
                  uudet-indeksivuodet)
            res (<! (k/post! :tallenna-indeksi
                             {:nimi nimi
                              :indeksit tallennettavat}))]
        (reset! indeksit res)
        true)))

(defonce kaikkien-urakkatyyppien-indeksit
  (let [a (atom nil)]
    (go (reset! a (<! (k/get! :urakkatyypin-indeksit))))
    a))

(defn urakkatyypin-indeksit
  [urakkatyyppi]
  (filter #(= urakkatyyppi (:urakkatyyppi %))
          @kaikkien-urakkatyyppien-indeksit))

(defn hae-paallystysurakan-indeksitiedot
  [urakka-id]
  (log "hae päällyhstyksurakan indeksitoedit " (pr-str urakka-id))
  (when @kaikkien-urakkatyyppien-indeksit
    (k/post! :paallystysurakan-indeksitiedot {:urakka-id urakka-id})))

(defn tallenna-paallystysurakan-indeksit
  [{:keys [urakka-id tiedot]}]
  (log "tallenna päällystysurakan indeksit urakkaan " urakka-id " tiedot" (pr-str tiedot))
  (go (let [res (<! (k/post! :tallenna-paallystysurakan-indeksitiedot
                             {:urakka-id urakka-id
                              :indeksitiedot tiedot}))]
        res)))

(defn raakaaineen-indeksit
  "Palauttaa raakaaineen indeksit"
  [raakaaine paallystysurakan-indeksitiedot]
  (filter #(or (nil? (:raakaaine %))
                           (= raakaaine (:raakaaine %))) paallystysurakan-indeksitiedot))