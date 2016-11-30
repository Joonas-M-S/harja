(ns harja.tiedot.kartta
  (:require [harja.geo :as geo]
            [cljs.core.async :refer [timeout <! >! chan] :as async]
            [reagent.core :refer [atom]]
            [harja.tiedot.navigaatio :as nav]
            [harja.views.kartta.tasot :as tasot]
            [harja.loki :refer [log]]
            [harja.ui.kartta.apurit :refer [+koko-suomi-extent+]]
            [harja.ui.openlayers :as openlayers])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def pida-geometria-nakyvilla-oletusarvo true)
(defonce pida-geometriat-nakyvilla? (atom pida-geometria-nakyvilla-oletusarvo))

(defonce infopaneeli-nakyvissa? (atom true))

(defn keskita-kartta-alueeseen! [alue]
  (reset! nav/kartan-extent alue))

(defn zoomaa-valittuun-hallintayksikkoon-tai-urakkaan
  []
  (let [v-hal @nav/valittu-hallintayksikko
        v-ur @nav/valittu-urakka]
    (if-let [alue (and v-ur (:alue v-ur))]
      (keskita-kartta-alueeseen! (geo/extent alue))
      (if-let [alue (and v-hal (:alue v-hal))]
        (keskita-kartta-alueeseen! (geo/extent alue))
        (keskita-kartta-alueeseen! +koko-suomi-extent+)))))

(defn zoomaa-geometrioihin
  "Zoomaa kartan joko kartalla näkyviin geometrioihin, tai jos kartalla ei ole geometrioita,
  valittuun hallintayksikköön tai urakkaan"
  []
  (when @pida-geometriat-nakyvilla?
    ;; Haetaan kaikkien tasojen extentit ja yhdistetään ne laajentamalla
    ;; extentiä siten, että kaikki mahtuvat.
    ;; Jos extentiä tasoista ei ole, zoomataan urakkaan tai hallintayksikköön.
    (let [extent (reduce geo/yhdista-extent
                         (keep #(-> % meta :extent) (vals @tasot/geometriat-kartalle)))
          extentin-margin-metreina geo/pisteen-extent-laajennus]
      (log "EXTENT TASOISTA: " (pr-str extent))
      (if extent
        (keskita-kartta-alueeseen! (geo/laajenna-extent extent extentin-margin-metreina))
        (zoomaa-valittuun-hallintayksikkoon-tai-urakkaan)))))

(defn kuuntele-valittua! [atomi]
  (add-watch atomi :kartan-valittu-kuuntelija (fn [_ _ _ uusi]
                                                (when-not uusi
                                                  (zoomaa-geometrioihin))))
  #(remove-watch atomi :kartan-valittu-kuuntelija))

(def kartan-ohjelaatikko-sisalto (atom nil))

(defn aseta-ohjelaatikon-sisalto! [uusi-sisalto]
  (reset! kartan-ohjelaatikko-sisalto uusi-sisalto))

(defn tyhjenna-ohjelaatikko! []
  (reset! kartan-ohjelaatikko-sisalto nil))

(def aseta-tooltip! openlayers/aseta-tooltip!)

(def aseta-kursori! openlayers/aseta-kursori!)

(def aseta-klik-kasittelija! openlayers/aseta-klik-kasittelija!)
(def poista-klik-kasittelija! openlayers/poista-klik-kasittelija!)
(def aseta-hover-kasittelija! openlayers/aseta-hover-kasittelija!)
(def poista-hover-kasittelija! openlayers/poista-hover-kasittelija!)

(def kartan-yleiset-kontrollit-sisalto (atom nil))

(defn kaappaa-hiiri
  "Muuttaa kartan toiminnallisuutta siten, että hover ja click eventit annetaan datana annettuun kanavaan.
Palauttaa funktion, jolla kaappaamisen voi lopettaa. Tapahtumat ovat vektori, jossa on kaksi elementtiä:
tyyppi ja sijainti. Kun kaappaaminen lopetetaan, suljetaan myös annettu kanava."
  [kanava]
  (let [kasittelija #(go (>! kanava %))]
    (aseta-klik-kasittelija! kasittelija)
    (aseta-hover-kasittelija! kasittelija)

    #(do (poista-klik-kasittelija!)
         (poista-hover-kasittelija!)
         (async/close! kanava))))

(defn aseta-yleiset-kontrollit! [uusi-sisalto]
  (reset! kartan-yleiset-kontrollit-sisalto uusi-sisalto))

(defn tyhjenna-yleiset-kontrollit! []
  (reset! kartan-yleiset-kontrollit-sisalto nil))