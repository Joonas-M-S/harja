(ns harja-laadunseuranta.tiedot.paikannus
  (:require [reagent.core :as reagent :refer [atom]]
            [harja-laadunseuranta.tiedot.asetukset.asetukset :as asetukset]
            [harja-laadunseuranta.tiedot.kalman :as kalman]
            [harja-laadunseuranta.utils :as utils]
            [harja-laadunseuranta.math :as math]
            [harja-laadunseuranta.utils :refer [timestamp ipad?]]
            [harja-laadunseuranta.tiedot.projektiot :as projektiot]))

(def +oletusraportointivali+ 2000)
(def paikan-raportointivali-ms (atom +oletusraportointivali+))
(def +raportointivalin-kertakasvatus+ 2000)
(def paikan-raportointivali-ms-max 8000)

(defn- geolocation-api []
  (.-geolocation js/navigator))

(defn- geolokaatio-tuettu? []
  (not (nil? (geolocation-api))))

(defn- etaisyys
  "Laskee kahden koordinaatin (ETRS-TM35FIN) välisen etäisyyden metreinä"
  [a b]
  (let [xdist (- (:lon a) (:lon b))
        ydist (- (:lat a) (:lat b))]
    (Math/sqrt (+ (* xdist xdist) (* ydist ydist)))))

(defn- konvertoi-latlon
  "Muuntaa geolocation-apin antaman objektin cljs-otukseksi ja tekee
  WGS84 -> ETRS-TM35FIN -muunnoksen"
  [position]
  (let [coords (.-coords position)
        wgs84-lat (.-latitude coords)
        wgs84-lon (.-longitude coords)
        latlon (projektiot/wgs84->etrsfin [wgs84-lon wgs84-lat])]
    {:lat (aget latlon 1)
     :lon (aget latlon 0)
     :heading (or (.-heading coords) 0)
     :accuracy (.-accuracy coords)
     :speed (or (.-speed coords) 0)}))

(defn tee-paikannusoptiot []
  #js {:enableHighAccuracy true
       :maximumAge 1000
       :timeout @paikan-raportointivali-ms})

(defn paivita-sijainti [{:keys [nykyinen]} sijainti ts]
  (let [uusi-sijainti (assoc sijainti :timestamp ts)
        uusi-nykyinen (if (ipad?)
                        uusi-sijainti ;; iPadissa on ilmeisesti riittävä suodatus itsessään
                        (kalman/kalman nykyinen uusi-sijainti
                                       (math/ms->sec (- ts (or (:timestamp nykyinen) ts)))))]
    {:edellinen nykyinen
     :nykyinen (assoc uusi-nykyinen
                 :speed (:speed uusi-sijainti)
                 :heading (:heading uusi-sijainti)
                 :accuracy (:accuracy uusi-sijainti)
                 :timestamp ts)}))

(defn- yrita-kasvattaa-paikannuksen-raportointivalia []
  (let [kasvatettu-raportointivali (+ @paikan-raportointivali-ms +raportointivalin-kertakasvatus+)]
    (when (<= kasvatettu-raportointivali paikan-raportointivali-ms-max)
      (reset! paikan-raportointivali-ms kasvatettu-raportointivali))))

(defn- palauta-oletusraportointivali []
  (reset! paikan-raportointivali-ms +oletusraportointivali+))

(defn kaynnista-paikannus
  ([sijainti-atom] (kaynnista-paikannus sijainti-atom nil nil))
  ([sijainti-atomi ensimmainen-sijainti-saatu-atom ensimmainen-sijainti-virhekoodi-atom]
   (when (geolokaatio-tuettu?)
     (.log js/console "Paikannus käynnistetään")
     (js/setInterval
       (fn []
         (let [sijainti-saatu (fn [sijainti]
                                (palauta-oletusraportointivali)
                                (when (and ensimmainen-sijainti-saatu-atom
                                           (nil? @ensimmainen-sijainti-saatu-atom))
                                  (reset! ensimmainen-sijainti-saatu-atom true))
                                (swap! sijainti-atomi (fn [entinen]
                                                        (paivita-sijainti entinen (konvertoi-latlon sijainti) (timestamp)))))
               sijainti-epaonnistui (fn [virhe]
                                      (yrita-kasvattaa-paikannuksen-raportointivalia)
                                      (.log js/console "Paikannus epäonnistui, uusi väli: " @paikan-raportointivali-ms)
                                      (when (and ensimmainen-sijainti-saatu-atom ensimmainen-sijainti-virhekoodi-atom
                                                 (nil? @ensimmainen-sijainti-saatu-atom)
                                                 (>= @paikan-raportointivali-ms paikan-raportointivali-ms-max))
                                        (reset! ensimmainen-sijainti-virhekoodi-atom (.-code virhe))
                                        (reset! ensimmainen-sijainti-saatu-atom false))
                                      (swap! sijainti-atomi identity))]
           (.getCurrentPosition (geolocation-api)
                                sijainti-saatu
                                sijainti-epaonnistui
                                (tee-paikannusoptiot))))
       @paikan-raportointivali-ms))))

(defn aseta-testisijainti
  "HUOM: testikäyttöön. Asettaa nykyisen sijainnin koordinaatit. Oikean geolocation pollerin tulisi
  olla pois päältä kun tätä käytetään."
  [sijainti-atomi [x y] tarkkuus]
  (swap! sijainti-atomi
         (fn [{:keys [nykyinen]}]
           {:edellinen nykyinen
            :nykyinen {:lat y
                       :lon x
                       :heading 0
                       :accuracy tarkkuus
                       :speed 40
                       :timestamp (timestamp)}})))

(defn lopeta-paikannus [id]
  (when id
    (js/clearInterval id)
    (.log js/console "Paikannut lopetettu!")))
