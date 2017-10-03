(ns harja.palvelin.tyokalut.ajastettu-tehtava
  "Apufunktioita tehtävien ajastamiseen"

  (:require
    [chime :refer [chime-at]]
    [taoensso.timbre :as log]
    [clj-time.core :as t]
    [clj-time.periodic :refer [periodic-seq]])
  (:import (org.joda.time DateTimeZone)))

(def virhekasittely
  {:error-handler #(log/error "Käsittelemätön poikkeus ajastetussa tehtävässä:" %)})

(defn ajasta-paivittain [[tunti minuutti sekuntti] tehtava]
  (when (and tunti minuutti sekuntti)
    (chime-at (periodic-seq
               (.. (t/now)
                   (withZone (DateTimeZone/forID "Europe/Helsinki"))
                   (withTime tunti minuutti sekuntti 0))
               (t/days 1))
              tehtava
              virhekasittely)))

(defn ajasta-minuutin-valein [minuutit tehtava]
  (when minuutit
    (chime-at (periodic-seq
               (.. (t/now)
                   (withZone (DateTimeZone/forID "Europe/Helsinki")))
               (t/minutes minuutit))
              tehtava
              virhekasittely)))

(defn ajasta-sekunnin-valein [sekunnit tehtava]
  (when sekunnit
    (chime-at (periodic-seq
                (.. (t/now)
                    (withZone (DateTimeZone/forID "Europe/Helsinki")))
                (t/seconds sekunnit))
              tehtava
              virhekasittely)))
