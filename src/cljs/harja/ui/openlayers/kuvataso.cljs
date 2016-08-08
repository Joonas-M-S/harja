(ns harja.ui.openlayers.kuvataso
  "Taso, joka hakee kuvan Harja palvelimelta"
  (:require [kuvataso.Lahde]
            [ol.layer.Tile]
            [ol.source.TileImage]
            [ol.extent :as ol-extent]
            [harja.loki :refer [log]]
            [harja.asiakas.kommunikaatio :refer [karttakuva-url]]
            [harja.ui.openlayers.taso :refer [Taso]]
            [cljs-time.core :as t]
            [harja.asiakas.tapahtumat :as tapahtumat]))

(defn hae-url [source parametrit coord pixel-ratio projection]
  (let [tile-grid (.getTileGridForProjection source projection)
        extent (.getTileCoordExtent tile-grid coord
                               (ol-extent/createEmpty))
        [x1 y1 x2 y2] extent]
    (apply karttakuva-url
           (concat  ["x1" x1 "y1" y1 "x2" x2 "y2" y2
                     "r" (.getResolution tile-grid (aget coord 0))
                     "pr" pixel-ratio]
                    parametrit))))

(def kuvatason-lataus (atom {:ladataan 0 :ladattu 0}))
(defonce julkaise-lataustapahtuma
  (add-watch kuvatason-lataus ::paivitys
             (fn [_ _ _ tila]
               (tapahtumat/julkaise! (assoc tila :aihe :karttakuva)))))

(defn nollaa-jos-valmis [{:keys [ladataan ladattu] :as lataus}]
  (if (= ladataan ladattu)
    {:ladataan 0 :ladattu 0}
    lataus))

(defn aloita-lataus []
  (swap! kuvatason-lataus (comp nollaa-jos-valmis
                                #(update % :ladataan inc))))

(defn lataus-valmis []
  (js/setTimeout
   (fn []
     (swap! kuvatason-lataus (comp nollaa-jos-valmis
                                   #(update % :ladattu inc))))
   100))

(defrecord Kuvataso [projection extent z-index selitteet parametrit]
  Taso
  (aseta-z-index [this z-index]
    (assoc this :z-index z-index))
  (extent [this]
    extent)
  (opacity [this] 1)
  (selitteet [this]
    selitteet)
  (paivita [this ol3 ol-layer aiempi-paivitystieto]
    (let [sama? (= parametrit aiempi-paivitystieto)
          luo? (nil? ol-layer)
          source (if (and sama? (not luo?))
                   (.getSource ol-layer)
                   (doto (ol.source.TileImage. #js {:projection projection})
                     (.on "tileloadstart" aloita-lataus)
                     (.on "tileloadend" lataus-valmis)
                     (.on "tileloaderror" lataus-valmis)))

          ol-layer (or ol-layer
                       (ol.layer.Tile.
                        #js {:source source
                             :wrapX true}))]

      (.setTileUrlFunction source (partial hae-url source parametrit))
      (when luo?
        (.addLayer ol3 ol-layer))

      (when z-index
        (.setZIndex ol-layer z-index))

      (when (and (not luo?) (not sama?))
        ;; Jos ei luoda ja parametrit eivät ole samat
        ;; asetetaan uusi source ol layeriiin
        (.setSource ol-layer source))
      [ol-layer ::kuvataso])))


(defn luo-kuvataso [projection extent selitteet parametrit]
  (->Kuvataso projection extent 99 selitteet parametrit))
