(ns harja.tyokalut.vkm
  "TR-osoitehaku. HUOM! TÄMÄN NAMESPACEN TARJOAMAT PALVELUT OVAT VANHENTUNEET!"
  (:require [cljs.core.async :refer [<! >! chan put! close!]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn- vkm-base-url []	
  "https://testportal.tiehallinto.fi/vkm/")

(defn koordinaatti->trosoite-kahdella [[x1 y1] [x2 y2]]
  (k/post! :hae-tr-pisteilla {:x1 x1 :y1 y1 :x2 x2 :y2 y2}))

(let [juokseva-id (atom 0)]	
  (defn- vkm-kutsu-id []	
    (swap! juokseva-id inc)))	
	
(defn- map->parametrit [parametrit]	
  (loop [uri "?"	
         [[k v] & parametrit] (seq parametrit)]	
    (if-not k	
      uri	
      (recur (str uri (name k) "=" v "&")	
             parametrit))))	
	
(defn- vkm-kutsu	
  "Tekee VKM kutsun JSONP muodossa ja palauttaa kanavan tuloksiin. 	
Annetun osoitteen tulee olla suhteellinen VKM:n osoitteeseen. Parametrit	
on avainsanamäppi parametrejä."	
  [uri parametrit]	
  (let [kutsu-id (vkm-kutsu-id)	
        callback (str "vkm_tulos_" kutsu-id)	
        s (doto (.createElement js/document "script")	
            (.setAttribute "type" "text/javascript")	
            (.setAttribute "src" (str (vkm-base-url) uri	
                                      (map->parametrit	
                                       (assoc parametrit	
                                         :callback callback)))))	
        ch (chan)	
        tulos #(do (put! ch (js->clj %)) ;; FIXME: JSON->CLJ muunnos	
                   (close! ch)	
                   (.removeChild (.-head js/document) s)	
                   (aset js/window callback nil))]	
    (aset js/window callback tulos)	
    (.appendChild (.-head js/document) s)	
    ;; PENDING: implement timeout	
    ch))	

(defn koordinaatti->tieosoite	
  "Muuntaa annetun koordinaatin tieosoitteeksi."	
  [[x y]]	
  (vkm-kutsu "tieosoite" {:x x :y y}))	


(defn tieosoite 	
  "Kutsuu tieosoite palvelua tierekisteriosoitteella, osoite sisältää avaimet:	
	
  :numero        tien numero	
  :alkuosa       tien osa	
  :alkuetaisyys  etäisyys tien alusta	
  :loppuosa      loppuosa	
  :loppuetaisyys loppuetäisyys	
	
Palautettavassa datassa:	
  \"alkupiste\":{\"tieosoitteet\":[{\"osa\":4,\"etaisyys\":5000,\"ajorata\":1,\"tie\":50,\"point\":{\"y\":6683955.515503107,\"spatialReference\":{\"wkid\":3067},\"x\":377686.44404436304}},{\"osa\":4,\"etaisyys\":5000,\"ajorata\":2,\"tie\":50,\"point\":{\"y\":6683973.695825488,\"spatialReference\":{\"wkid\":3067},\"x\":377681.47636308137}}]},\"loppupiste\":{\"pituus\":1005,\"tieosoitteet\":[{\"osa\":5,\"etaisyys\":100,\"ajorata\":1,\"tie\":50,\"point\":{\"y\":6683992.441209993,\"spatialReference\":{\"wkid\":3067},\"x\":378655.91600080184}},{\"osa\":5,\"etaisyys\":100,\"ajorata\":2,\"tie\":50,\"point\":{\"y\":6684006.147510614,\"spatialReference\":{\"wkid\":3067},\"x\":378652.39605513535}}]},\"lines\":{\"lines\":[{\"paths\":[[[377686,6683955],[377739,6683941],[377873,6683899],[377925,6683887],[378036,6683867],[378142,6683860],[378254,6683864],[378375,6683882],[378470,6683905],[378568,6683946],[378602,6683961],[378655,6683992]]],\"spatialReference\":{\"wkid\":3067}},{\"paths\":[[[377681,6683973],[377740,6683957],[377799,6683937],[377873,6683915],[377984,6683892],[378039,6683884],[378100,6683877],[378253,6683880],[378346,6683894],[378445,6683916],[378496,6683931],[378565,6683959],[378612,6683983],[378652,6684006]]],\"spatialReference\":{\"wkid\":3067}}]}}"	
  [{:keys [numero alkuosa alkuetaisyys loppuosa loppuetaisyys] :as tierekisteriosoite}]	
  (vkm-kutsu "tieosoite" {:tie numero	
                          :osa alkuosa	
                          :etaisyys alkuetaisyys	
                          :losa loppuosa	
                          :let loppuetaisyys}))

(declare virhe?)

(defn tieosoite->sijainti	
  "Kutsuu VKM:n kautta tierekisteriosoitetta ja yrittää löytää parhaan sijainnin. 	
   Palauttaa kanavan, josta sijainnin voi lukea. Virhetapauksessa kanavaan kirjoitetaan virheen kuvaus. "	
  [tierekisteriosoite]
  (log "Muunetaan tieosoite sijainniksi: " (pr-str tierekisteriosoite))
  (go (let [tulos (<! (tieosoite tierekisteriosoite))]	
        (log "TULOS: " (pr-str tulos))	
        (if (virhe? tulos)	
          ;; Annetaan virhe ulos sellaisenaan	
          tulos	
	
          ;; Onnistui, joten yritetään löytää sijaintia	
          (let [osoitteet (some-> tulos	
                                  (get "alkupiste")	
                                  (get "tieosoitteet"))]	
            ;; PENDING: onko jotain heuristiikkaa miten etsiä paras?	
            ;; Palautetaan nyt vain alkupisteen ensimmäinen x/y piste	
            (log "Osoitteet: " (pr-str osoitteet))	
            (some-> osoitteet	
                    first	
                    (get "point")	
                    ((fn [{x "x" y "y"}]	
                       {:type :point	
                        :coordinates [x y]}))))))))	

(defn tieosoite->piste [trosoite]
  (log "Haetaan piste tieosoitteelle")
  (k/post! :hae-tr-pisteeksi trosoite))

(defn tieosoite->viiva [trosoite]
  (k/post! :hae-tr-viivaksi trosoite))

(defn koordinaatti->trosoite [[x y]]
  (k/post! :hae-tr-pisteella {:x x :y y}))

(defn virhe?
  "Tarkistaa epäonnistuiko VKM kutsu"
  [tulos]
  (contains? tulos :virhe))
       
(def pisteelle-ei-loydy-tieta "Pisteelle ei löydy tietä.")
(def vihje-zoomaa-lahemmas "Yritä zoomata lähemmäs.")
  
