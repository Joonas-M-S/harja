(ns harja.views.kartta
  "Harjan kartta."
  (:require [cljs.core.async :refer [timeout <! >! chan] :as async]
            [clojure.string :as str]
            [clojure.set :as set]
            [goog.events.EventType :as EventType]
            [goog.events :as events]
            [harja.asiakas.kommunikaatio :as k]
            [harja.asiakas.tapahtumat :as t]
            [harja.asiakas.tapahtumat :as tapahtumat]
            [harja.fmt :as fmt]
            [harja.geo :as geo]
            [harja.loki :refer [log tarkkaile!]]
            [harja.tiedot.hallintayksikot :as hal]
            [harja.tiedot.navigaatio :as nav]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.animaatio :as animaatio]
            [harja.ui.komponentti :as komp]
            [harja.ui.openlayers :refer [openlayers] :as openlayers]
            [harja.ui.dom :as dom]
            [harja.views.kartta.tasot :as tasot]
            [harja.views.kartta.infopaneeli :as infopaneeli]
            [reagent.core :refer [atom] :as reagent]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.kartta.varit.alpha :as varit]
            [harja.ui.openlayers.taso :as taso]
            [harja.ui.kartta.apurit :refer [+koko-suomi-extent+]]
            [harja.ui.openlayers.edistymispalkki :as edistymispalkki]
            [harja.tiedot.kartta :as tiedot]
            [harja.ui.kartta.ikonit :as kartta-ikonit])

  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go go-loop]]))


(def kartta-kontentin-vieressa? (atom false))

(def +kartan-napit-padding+ 26)
(def +kartan-korkeus-s+ 0)

(def kartan-korkeus (reaction
                      (let [koko @nav/kartan-koko
                            kork @dom/korkeus
                            murupolku? @nav/murupolku-nakyvissa?]
                        (case koko
                          :S +kartan-korkeus-s+
                          :M (int (* 0.25 kork))
                          :L (int (* 0.60 kork))
                          :XL (int (if murupolku?
                                     (* 0.80 kork)
                                     (- kork (yleiset/navigaation-korkeus) 5)))
                          (int (* 0.60 kork))))))

;; Kanava, jonne kartan uusi sijainti kirjoitetaan
(defonce paivita-kartan-sijainti (chan))

(defn- aseta-kartan-sijainti [x y w h naulattu?]
  (when-let
    [karttasailio (dom/elementti-idlla "kartta-container")]
    (let [tyyli (.-style karttasailio)]
      ;;(log "ASETA-KARTAN-SIJAINTI: " x ", " y ", " w ", " h ", " naulattu?)
      (if naulattu?
        (do
          (set! (.-position tyyli) "fixed")
          (set! (.-left tyyli) (fmt/pikseleina x))
          (set! (.-top tyyli) "0px")
          (set! (.-width tyyli) (fmt/pikseleina w))
          (set! (.-height tyyli) (fmt/pikseleina h))
          (openlayers/set-map-size! w h))
        (do
          (set! (.-position tyyli) "absolute")
          (set! (.-left tyyli) (fmt/pikseleina x))
          (set! (.-top tyyli) (fmt/pikseleina y))
          (set! (.-width tyyli) (fmt/pikseleina w))
          (set! (.-height tyyli) (fmt/pikseleina h))
          (openlayers/set-map-size! w h)))
      ;; jotta vältetään muiden kontrollien hautautuminen float:right Näytä kartta alle, kavenna kartta-container
      (when (= :S @nav/kartan-koko)
        (set! (.-left tyyli) "")
        (set! (.-right tyyli) (fmt/pikseleina 20))
        (set! (.-width tyyli) (fmt/pikseleina 100))))))

;; Kun kartan paikkavaraus poistuu, aseta flägi, joka pakottaa seuraavalla
;; kerralla paikan asetuksen... läheta false kanavaan

(defn- elementti-idlla-odota
  "Pollaa DOMia 10ms välein kunnes annettu elementti löytyy. Palauttaa kanavan, josta
  elementin voi lukea."
  [id]
  (go (loop [elt (.getElementById js/document id)]
        (if elt
          elt
          (do #_(log "odotellaan elementtiä " id)
            (<! (timeout 10))
            (recur (.getElementById js/document id)))))))

(defn odota-mount-tai-timeout
  "Odottaa, että paivita-kartan-sijainti kanavaan tulee :mount tapahtuma tai 150ms timeout.
  Paluttaa kanavan, josta voi :mount tai :timeout arvon."
  []
  (let [t (timeout 150)]
    (go (loop []
          (let [[arvo ch] (async/alts! [t paivita-kartan-sijainti])]
            (if (= ch t)
              :timeout
              (if (= arvo :mount)
                :mount
                (recur))))))))

(defonce kartan-sijaintipaivitys
  (let [transition-end-tuettu? (animaatio/transition-end-tuettu?)]
    (go (loop [naulattu? nil
               x nil
               y nil
               w nil
               h nil
               offset-y nil]
          (let [ensimmainen-kerta? (nil? naulattu?)
                paivita (<! paivita-kartan-sijainti)
                aseta (when-not paivita
                        ;; Kartan paikkavaraus poistuu, asetetaan lähtötila, jolloin
                        ;; seuraava päivitys aina asettaa kartan paikan.

                        ;; Odotetaan joko seuraavaa eventtiä paivita-kartan-sijainti (jos uusi komponentti
                        ;; tuli näkyviin, tai timeout 20ms (jos kartta oikeasti lähti näkyvistä)
                        (case (<! (odota-mount-tai-timeout))
                          :mount true
                          :timeout
                          ;; timeout, kartta oikeasti poistu, asetellaan -h paikkaan
                          (do                        ;; (log "KARTTA LÄHTI OIKEASTI")
                            (aseta-kartan-sijainti x (- @dom/korkeus) w h false)
                            (recur nil nil nil w h nil))))
                paikka-elt (<! (elementti-idlla-odota "kartan-paikka"))
                [uusi-x uusi-y uusi-w uusi-h] (dom/sijainti paikka-elt)
                uusi-offset-y (dom/offset-korkeus paikka-elt)]

            ;; (log "KARTAN PAIKKA: " x "," y " (" w "x" h ") OY: " offset-y " => " uusi-x "," uusi-y " (" uusi-w "x" uusi-h ") OY: " uusi-offset-y)


            (cond
              ;; Eka kerta, asetetaan kartan sijainti
              (or (= :aseta paivita) aseta (nil? naulattu?))
              (let [naulattu? (neg? uusi-y)]
                                        ;(log "EKA KERTA")
                (aseta-kartan-sijainti uusi-x uusi-offset-y uusi-w uusi-h naulattu?)
                (when (or (not= w uusi-w) (not= h uusi-h))
                  (reagent/next-tick #(openlayers/invalidate-size!)))
                (recur naulattu?
                       uusi-x uusi-y uusi-w uusi-h uusi-offset-y))

              ;; Jos kartta ei ollut naulattu yläreunaan ja nyt meni negatiiviseksi
              ;; koko pitää asettaa
              (and (not naulattu?) (neg? uusi-y))
              (do (aseta-kartan-sijainti uusi-x uusi-y uusi-w uusi-h true)
                  (recur true
                         uusi-x uusi-y uusi-w uusi-h uusi-offset-y))

              ;; Jos oli naulattu ja nyt on positiivinen, pitää naulat irroittaa
              (and naulattu? (pos? uusi-y))
              (do (aseta-kartan-sijainti uusi-x uusi-offset-y uusi-w uusi-h false)
                  (recur false
                         uusi-x uusi-y uusi-w uusi-h uusi-offset-y))

              ;; jos w/h muuttuu
              (or (not= w uusi-w)
                  (not= h uusi-h))
              (do (when-not transition-end-tuettu?
                    (go (<! (async/timeout 150))
                        (openlayers/invalidate-size!)))
                  (recur naulattu?
                         uusi-x uusi-y uusi-w uusi-h uusi-offset-y))

              :default
              (recur naulattu?
                     uusi-x uusi-y uusi-w uusi-h uusi-offset-y)))))))

;; halutaan että kartan koon muutos aiheuttaa rerenderin kartan paikalle
(defn- kartan-paikkavaraus
  [kartan-koko & args]
  (let [paivita (fn [paikkavaraus]
                  (go (>! paivita-kartan-sijainti paikkavaraus)))
        scroll-kuuntelija (fn [_]
                            (paivita :scroll))]
    (komp/luo
     (komp/kuuntelija #{:ikkunan-koko-muuttunut
                        :murupolku-naytetty-domissa?}
                      #(paivita :aseta))
      {:component-did-mount    #(do
                                 (events/listen js/window
                                                EventType/SCROLL
                                                scroll-kuuntelija)
                                 (paivita :mount))
       :component-did-update   #(paivita :aseta)
       :component-will-unmount (fn [this]
                                 ;; jos karttaa ei saa näyttää, asemoidaan se näkyvän osan yläpuolelle
                                 (events/unlisten js/window EventType/SCROLL scroll-kuuntelija)
                                 (paivita false))}

      (fn []
        [:div#kartan-paikka {:style {:height        (fmt/pikseleina @kartan-korkeus)
                                     :margin-bottom "5px"
                                     :width         "100%"}}]))))

(defn kartan-paikka
  [& args]
  (let [koko @nav/kartan-koko]
    (if-not (= :hidden koko)
      (if (= :S koko)
        [:span
         [kartan-paikkavaraus koko args]
         [:div.pystyvali-karttanapille]]
        [kartan-paikkavaraus koko args])
      [:span.ei-karttaa])))

(reset! nav/kartan-extent +koko-suomi-extent+)

(defonce urakka-kuuntelija
         (t/kuuntele! :urakka-valittu
                      #(openlayers/hide-popup!)))

(defonce kartan-koon-paivitys
         (run! (do @dom/ikkunan-koko
                   (openlayers/invalidate-size!))))

(defn kartan-koko-kontrollit
  []
  (let [koko @nav/kartan-koko
        kartan-korkeus @kartan-korkeus
        v-ur @nav/valittu-urakka
        [muuta-kokoa-teksti ikoni] (case koko
                             :M ["Suurenna karttaa" (ikonit/livicon-arrow-down)]
                             :L ["Pienennä karttaa" (ikonit/livicon-arrow-up)]
                             :XL ["Pienennä karttaa" (ikonit/livicon-arrow-up)]
                             ["" nil])]
    ;; TODO: tähän alkaa kertyä näkymäkohtaista logiikkaa, mietittävä vaihtoehtoja.
    [:div.kartan-kontrollit.kartan-koko-kontrollit {:class (when-not @nav/kartan-kontrollit-nakyvissa? "hide")}


     ;; käytetään tässä inline-tyylejä, koska tarvitsemme kartan-korkeus -arvoa asemointiin
     [:div.kartan-koko-napit {:style {:position   "absolute"
                                      :text-align "center"
                                      :top        (fmt/pikseleina (- kartan-korkeus
                                                                     (if (= :S koko)
                                                                       0
                                                                       +kartan-napit-padding+)))
                                      :width      "100%"
                                      :z-index    100}}
      (if (= :S koko)
        [:button.btn-xs.nappi-ensisijainen.nappi-avaa-kartta.pull-right
         {:on-click #(nav/vaihda-kartan-koko! :L)}
         (ikonit/expand) " Näytä kartta"]
        [:span
         (when-not @kartta-kontentin-vieressa?              ;ei pointtia muuttaa korkeutta jos ollaan kontentin vieressä
           [:button.btn-xs.nappi-toissijainen {:on-click #(nav/vaihda-kartan-koko!
                                                           (case koko
                                                             :M :L
                                                             :L :M
                                                             ;; jos tulee tarve, voimme hanskata kokoja kolmella napilla
                                                             ;; suurenna | pienennä | piilota
                                                             :XL :M))}
            ikoni muuta-kokoa-teksti])

         [:button.btn-xs.nappi-ensisijainen {:on-click #(nav/vaihda-kartan-koko! :S)}
          (ikonit/compress) " Piilota kartta"]])]]))

(def keskita-kartta-pisteeseen openlayers/keskita-kartta-pisteeseen!)


(def ikonien-selitykset-nakyvissa-oletusarvo true)
;; Eri näkymät voivat tarpeen mukaan asettaa ikonien selitykset päälle/pois komponenttiin tultaessa.
;; Komponentista poistuttaessa tulisi arvo asettaa takaisin oletukseksi
(def ikonien-selitykset-nakyvissa? (atom true))
(def ikonien-selitykset-auki (atom false))

(defn kartan-ikonien-selitykset []
  (let [selitteet (reduce set/union
                          (keep #(when % (taso/selitteet %))
                                (vals @tasot/geometriat-kartalle)))
        lukumaara-str (fmt/left-pad 2 (count selitteet))
        varilaatikon-koko 20
        teksti (if @ikonien-selitykset-auki
                 (str "Piilota | " lukumaara-str " kpl")
                 (str "Karttaselitteet | " lukumaara-str " kpl"))]
    (if (and (not= :S @nav/kartan-koko)
             (not (empty? selitteet))
             @ikonien-selitykset-nakyvissa?)
      [:div.kartan-selitykset.kartan-ikonien-selitykset
       (if @ikonien-selitykset-auki
         [:div
          [:table
           [:tbody
            (for [{:keys [img vari teksti]} (sort-by :teksti selitteet)]
              (when
                (or (not-empty vari) (not-empty img))
                ^{:key (str (or vari img) "_" teksti)}
                [:tr
                 (cond
                   (string? vari)
                   [:td.kartan-ikonien-selitykset-ikoni-sarake
                    [:div.kartan-ikoni-vari {:style {:background-color vari
                                                     :width            (str varilaatikon-koko "px")
                                                     :height           (str varilaatikon-koko "px")}}]]

                   (coll? vari)
                   (let [vk varilaatikon-koko
                         kaikki-koot [[vk]
                                      [vk (- vk 10)]
                                      [vk (- vk 6) (- vk 12)]
                                      [vk (- vk 4) (- vk 8) (- vk 12)]]
                         koot (nth kaikki-koot (dec (count vari)) (take (count vari) (range vk 0 -2)))
                         solut (partition 2 (interleave koot vari))
                         pohja (first solut)
                         sisakkaiset (butlast (rest solut))
                         viimeinen (last solut)]
                     [:td.kartan-ikonien-selitykset-ikoni-sarake
                      [:div.kartan-ikoni-vari-pohja {:style {:background-color (second pohja)
                                                             :width            (first pohja)
                                                             :height           (first pohja)}}]
                      (doall
                        (for [[koko v] sisakkaiset]
                          ^{:key (str koko "_" v "--" teksti)}
                          [:div.kartan-ikoni-vari-sisakkainen {:style {:background-color v
                                                                       :width            koko
                                                                       :height           koko
                                                                       :margin           (/ (- varilaatikon-koko koko) 2)}}]))

                      [:div.kartan-ikoni-vari-sisakkainen {:style {:background-color (second viimeinen)
                                                                   :width            (first viimeinen)
                                                                   :height           (first viimeinen)
                                                                   :position         "relative"
                                                                   :margin           (/ (- varilaatikon-koko (first viimeinen)) 2)}}]])


                   :else [:td.kartan-ikonien-selitykset-ikoni-sarake
                          [:img.kartan-ikonien-selitykset-ikoni {:src img}]])
                 [:td.kartan-ikonien-selitykset-selitys-sarake [:span.kartan-ikonin-selitys teksti]]]))]]
          [:div.kartan-ikonien-selitykset-sulje.klikattava
           {:on-click (fn [event]
                        (reset! ikonien-selitykset-auki false)
                        (.stopPropagation event)
                        (.preventDefault event))} teksti]]
         [:span.kartan-ikonien-selitykset-avaa.klikattava {:on-click (fn [event]
                                                                       (reset! ikonien-selitykset-auki true)
                                                                       (.stopPropagation event)
                                                                       (.preventDefault event))}
          teksti])])))

(defn kartan-yleiset-kontrollit
  "Kartan yleiset kontrollit -komponentti, johon voidaan antaa mitä tahansa sisältöä, jota tietyssä näkymässä tarvitaan"
  []
  (let [sisalto @tiedot/kartan-yleiset-kontrollit-sisalto
        luokka-str (or (:class (meta sisalto)) "kartan-yleiset-kontrollit")]
    (when (and sisalto (not= :S @nav/kartan-koko))
      [:div {:class (str "kartan-kontrollit " luokka-str)} sisalto])))

(def paivitetaan-karttaa-tila (atom false))
(defonce kuvatason-lataus (atom nil))
(defonce geometriatason-lataus (atom nil))

;; Määrittelee asiat, jotka ovat nykyisessä pisteessä.
;; Avaimet:
;; :koordinaatti  klikatun pisteen koordinatti (tai nil, jos ei valintaa)
;; :asiat         sekvenssi asioita, joita pisteestä löytyy
;; :haetaan?      true kun haku vielä kesken
(defonce asiat-pisteessa (atom {:koordinaatti nil
                                :haetaan? true
                                :asiat nil}))

(defn paivitetaan-karttaa
  []
  (when @paivitetaan-karttaa-tila
    [:div {:style {:position "absolute" :top "50%" :left "50%"}}
     [:div {:style {:position "relative" :left "-50px" :top "-30px"}}
      [:div.paivitetaan-karttaa (yleiset/ajax-loader "Päivitetään karttaa")]]]))

(defonce kuuntele-kuvatason-paivitys
         (t/kuuntele! :edistymispalkki/kuvataso
                      #(reset! kuvatason-lataus %)))

(defonce kuuntele-geometriatason-paivitys
         (t/kuuntele! :edistymispalkki/geometriataso
                      #(reset! geometriatason-lataus %)))

(defn aseta-paivitetaan-karttaa-tila! [uusi-tila]
  (reset! paivitetaan-karttaa-tila uusi-tila))

(defn kartan-ohjelaatikko
  "Kartan ohjelaatikko -komponentti, johon voidaan antaa mitä tahansa sisältöä, jota tietyssä näkymässä tarvitaan"
  []
  (let [sisalto @tiedot/kartan-ohjelaatikko-sisalto]
    (when (and sisalto (not= :S @nav/kartan-koko))
      [:div.kartan-kontrollit.kartan-ohjelaatikko sisalto])))





(defn nayta-popup!
  "Näyttää popup sisällön kartalla tietyssä sijainnissa. Sijainti on vektori [lat lng],
joka kertoo karttakoordinaatit. Sisältö annetaan sisalto-hiccup muodossa ja se renderöidään
HTML merkkijonoksi reagent render-to-string funktiolla (eikä siis ole täysiverinen komponentti)"
  [sijainti sisalto-hiccup]
  (openlayers/show-popup! sijainti sisalto-hiccup))

(defn poista-popup! []
  (openlayers/hide-popup!))

(defn poista-popup-ilman-eventtia!
  "Poistaa pop-upin ilmoittamatta siitä kuuntelijoille. Kätevä esim. silloin kun pop-up poistetaan
   ja luodaan uudelleen uuteen sijaintiin."
  []
  (openlayers/hide-popup-without-event!))

(defonce poista-popup-kun-tasot-muuttuvat
         (tapahtumat/kuuntele! :karttatasot-muuttuneet
                               (fn [_]
                                 (poista-popup!))))




;; harja.views.kartta=> (viivan-piirto-aloita)
;; klikkaile kartalta pisteitä...
;; harja.views.kartta=> (viivan-piirto-lopeta)
;;
;; js consoleen logittuu koko ajan rakentuva linestring, jonka voi sijainniksi laittaa
(defonce viivan-piirto (cljs.core/atom nil))
(defn ^:export viivan-piirto-aloita []
  (let [eventit (chan)]
    (reset! viivan-piirto
            (tiedot/kaappaa-hiiri eventit))
    (go-loop [e (<! eventit)
              pisteet []]
             (log "LINESTRING("
                  (str/join ", " (map (fn [[x y]] (str x " " y)) pisteet))
                  ")")
             (when e
               (recur (<! eventit)
                      (if (= :click (:tyyppi e))
                        (conj pisteet (:sijainti e))
                        pisteet))))))

(defn ^:export viivan-piirto-lopeta []
  (@viivan-piirto)
  (reset! viivan-piirto nil))



(defn- paivita-extent [_ newextent]
  (reset! nav/kartalla-nakyva-alue {:xmin (aget newextent 0)
                                    :ymin (aget newextent 1)
                                    :xmax (aget newextent 2)
                                    :ymax (aget newextent 3)}))


(defn suomen-sisalla? [alue]
  (openlayers/extent-sisaltaa-extent? +koko-suomi-extent+ (geo/extent alue)))


(defn- kun-geometriaa-klikattu
  "Event handler geometrioiden yksi- ja tuplaklikkauksille"
  [item event asiat-pisteessa]
  (let [item (assoc item :klikkaus-koordinaatit (js->clj (.-coordinate event)))]
    (condp = (:type item)
      :hy (when-not (= (:id item) (:id @nav/valittu-hallintayksikko))
            (nav/valitse-hallintayksikko item))
      :ur (when-not (= (:id item) (:id @nav/valittu-urakka))
            (t/julkaise! (assoc item :aihe :urakka-klikattu)))
      (swap! asiat-pisteessa update :asiat conj item))))

(defn- hae-asiat-pisteessa [tasot event atomi]
  (let [koordinaatti (js->clj (.-coordinate event))]
    (swap! atomi assoc
           :koordinaatti koordinaatti
           :haetaan? true
           :asiat [])
    (tasot/nayta-geometria! :klikattu-karttapiste
                            {:alue {:type :icon
                                    :coordinates koordinaatti
                                    :img (kartta-ikonit/sijainti-ikoni "syaani")}})

    (go
      (let [in-ch (async/merge
                    (map #(taso/hae-asiat-pisteessa % koordinaatti)
                         (remove nil? (vals tasot))))]
        (loop [asia (<! in-ch)]
          (when asia
            (swap! atomi update :asiat conj asia)
            (recur (<! in-ch))))
        (swap! atomi assoc :haetaan? false)))))

(defn- geometria-maarat [geometriat]
  (reduce-kv (fn [m k v]
               (if (nil? v)
                 m
                 (assoc m k (count v))))
             {}
             geometriat))

(defn- tapahtuman-geometria-on-hallintayksikko-tai-urakka? [geom]
  (or (= :ur (:type geom))
      (= :hy (:type geom))))

(defn- tapahtuman-geometria-on-valittu-hallintayksikko-tai-urakka?
  [geom]
  (or (and
        (= (:type geom) :ur)
        (= (:id geom) (:id @nav/valittu-urakka)))
      (and
        (= (:type geom) :hy)
        (= (:id geom) (:id @nav/valittu-hallintayksikko)))))


(defn hae-asiat? [item]
  ;; Haetaan koordinaatin asiat ja aukaistaan infopaneeli, jos
  ;; Klikattu asia ei ole hallintayksikkö tai urakka
  ;; Ollaan tilannekuvassa tai ilmoituksissa
  ;;  - Ilmoituksissa ja tilannekuvassa kartan käyttäytyminen ei sinällään
  ;;    riipu siitä, mikä hy/urakka on valittuna. Näkymiä voi käyttää aina kuitenkin
  ;; Klikattu asia on valittu urakka
  ;;  - Jos asia on ei-valittu urakka, ollaan Urakat-näkymän etusivulla (tai ilmoituksissa/tilannekuassa).
  ;;  - Muussa tapauksessa ollaan "muualla Harjassa", jolloin tietenkin halutaan tehdä haku
  (cond
    (not (tapahtuman-geometria-on-hallintayksikko-tai-urakka? item))
    true

    (#{:tilannekuva :ilmoitukset} @nav/valittu-sivu)
    true

    (= (:id item) (:id @nav/valittu-urakka))
    true))

(defn kaynnista-asioiden-haku-pisteesta! [tasot event asiat-pisteessa]
  (hae-asiat-pisteessa tasot event asiat-pisteessa)
  (reset! tiedot/nayta-infopaneeli? true))

(defn kartta-openlayers []
  (komp/luo

    {:component-did-mount
     #(tiedot/zoomaa-geometrioihin)}
    (komp/sisaan
      (fn [_]
        (tiedot/zoomaa-geometrioihin)

        (add-watch nav/kartan-koko :muuttuvan-kartan-koon-kuuntelija
                   (fn [_ _ _ _]
                     (when @tiedot/pida-geometriat-nakyvilla?
                       (log "Kartan koko muuttui, zoomataan!")
                       (tiedot/zoomaa-geometrioihin))))

        ;; Hallintayksiköt ja valittu urakka ovat nykyään :organisaatio
        ;; tasossa, joten ne eivät tarvitse erillistä kuuntelijaa.
        (add-watch tasot/geometriat-kartalle :muuttuvien-geometrioiden-kuuntelija
                   (fn [_ _ vanha uusi]
                     (when (not= (dissoc vanha :nakyman-geometriat)
                                 (dissoc uusi :nakyman-geometriat))
                       ;; Kun karttatasoissa muuttuu jotain muuta kuin :nakyman-geometriat
                       ;; (klikattu piste), piilotetaan infopaneeli ja poistetaan
                       ;; klikattu piste näkymän geometrioista.
                       (reset! tiedot/nayta-infopaneeli? false)
                       (tasot/poista-geometria! :klikattu-karttapiste))




                     ;; Jos vanhoissa ja uusissa geometrioissa ei ole samat määrät asioita,
                     ;; niin voidaan olettaa että nyt geometriat ovat muuttuneet.
                     ;; Tällainen workaround piti tehdä, koska asian valitseminen muuttaa
                     ;; geometriat atomia, mutta silloin ei haluta triggeröidä zoomaamista.
                     ;; Myös jos :organisaatio karttatason tiedot ovat muuttuneet, tehdään zoomaus (urakka/hallintayksikkö muutos)

                     (when @tiedot/pida-geometriat-nakyvilla?
                       (when (or (not= (geometria-maarat vanha) (geometria-maarat uusi))
                                 (not= (:organisaatio vanha) (:organisaatio uusi)))
                         (tiedot/zoomaa-geometrioihin)))))))
    (fn []
      (let [koko (if-not (empty? @nav/tarvitsen-isoa-karttaa)
                   :L
                   @nav/kartan-koko)]

        [openlayers
         {:id                 "kartta"
          :width              "100%"
          ;; set width/height as CSS units, must set height as pixels!
          :height             (fmt/pikseleina @kartan-korkeus)
          :style              (when (= koko :S)
                                ;; display none estää kartan korkeuden
                                ;; animoinnin suljettaessa
                                {:display "none"})
          :class              (when (or
                                      (= :hidden koko)
                                      (= :S koko))
                                "piilossa")

          ;; :extent-key muuttuessa zoomataan aina uudelleen, vaikka itse alue ei olisi muuttunut

          :extent-key         (str (if (or (= :hidden koko) (= :S koko)) "piilossa" "auki") "_" (name @nav/valittu-sivu))
          :extent             @nav/kartan-extent

          :selection          nav/valittu-hallintayksikko
          :on-zoom            paivita-extent
          :on-drag            (fn [item event]
                                (paivita-extent item event)
                                (t/julkaise! {:aihe :karttaa-vedetty}))
          :on-postrender      (fn [_]
                                ;; Geometriatason pakottaminen valmiiksi postrenderissä
                                ;; tuntuu toimivan hyvin, mutta kuvatason pakottaminen ei.
                                ;; Postrender triggeröityy monta kertaa, kun kuvatasoja piirretään.
                                (edistymispalkki/geometriataso-pakota-valmistuminen!))
          :on-mount           (fn [initialextent]
                                (paivita-extent nil initialextent))
          :on-click           (fn [event]
                                #_(t/julkaise! {:aihe :tyhja-click :klikkaus-koordinaatit at})
                                #_(poista-popup!)
                                (kaynnista-asioiden-haku-pisteesta! @tasot/geometriat-kartalle
                                                                    event
                                                                    asiat-pisteessa)
                                (.stopPropagation event)
                                (.preventDefault event))
          :on-select          (fn [item event]
                                (when (hae-asiat? item)
                                  (kaynnista-asioiden-haku-pisteesta! @tasot/geometriat-kartalle
                                                                      event
                                                                      asiat-pisteessa))
                                (kun-geometriaa-klikattu item event asiat-pisteessa)
                                (.stopPropagation event)
                                (.preventDefault event))

          :on-dblclick        nil

          :on-dblclick-select (fn [item event]
                                ;; jos tuplaklikattiin valittua hallintayksikköä tai urakkaa (eli "tyhjää"),
                                ;; niin silloin ei pysäytetä eventtiä, eli zoomataan sisään
                                (when-not (tapahtuman-geometria-on-valittu-hallintayksikko-tai-urakka? item)
                                  (.stopPropagation event)
                                  (.preventDefault event)

                                  ;; Jos tuplaklikattu asia oli jotain muuta kuin HY/urakka, niin keskitetään
                                  ;; kartta siihen.
                                  (when-not (tapahtuman-geometria-on-hallintayksikko-tai-urakka? item)
                                    (kaynnista-asioiden-haku-pisteesta! @tasot/geometriat-kartalle
                                                                        event
                                                                        asiat-pisteessa)
                                    (kun-geometriaa-klikattu item event asiat-pisteessa)
                                    (tiedot/keskita-kartta-alueeseen! (harja.geo/extent (:alue item))))))

          :tooltip-fn         (fn [geom]
                                ; Palauttaa funktion joka palauttaa tooltipin sisällön, tai nil jos hoverattu asia
                                ; on valittu hallintayksikkö tai urakka.
                                (if (or (tapahtuman-geometria-on-valittu-hallintayksikko-tai-urakka? geom)
                                        (and (empty? (:nimi geom)) (empty? (:siltanimi geom))))
                                  nil
                                  (fn []
                                    (and geom
                                         [:div {:class (name (:type geom))} (or (:nimi geom) (:siltanimi geom))]))))

          :geometries         @tasot/geometriat-kartalle
          :layers             [{:type  :mml
                                :url   (str (k/wmts-polku) "maasto/wmts")
                                :layer "taustakartta"}]}]))))

(defn kartan-edistyminen [kuvataso geometriataso]
  (let [ladattu (+ (:ladattu kuvataso) (:ladattu geometriataso))
        ladataan (+ (:ladataan kuvataso) (:ladattu geometriataso))]
    (when (and @nav/kartta-nakyvissa? (pos? ladataan))
      [:div.kartta-progress {:style {:width (str (* 100.0 (/ ladattu ladataan)) "%")}}])))

(defn kartta []
  [:div.karttacontainer
   [paivitetaan-karttaa]
   [kartan-koko-kontrollit]
   [kartan-yleiset-kontrollit]
   [kartan-ohjelaatikko]
   (when @tiedot/infopaneeli-nakyvissa?
     [infopaneeli/infopaneeli @asiat-pisteessa #(reset! tiedot/nayta-infopaneeli? false)
      tiedot/infopaneelin-linkkifunktiot])
   [kartan-ikonien-selitykset]
   [kartta-openlayers]
   [kartan-edistyminen @kuvatason-lataus @geometriatason-lataus]])


;; Käytä tätä jos haluat luoda rinnakkain sisällön ja kartan näkymääsi
;; tämä on täällä eikä ui.yleiset koska olisi tullut syklinen riippuvuus
(defn sisalto-ja-kartta-2-palstana
  "Luo BS-rivin ja sarakkeet, joissa toisella puolella parameterinä annettava sisältö, toisella kartta."
  [sisalto]
  [:div.row
   [:div {:class (if (= @nav/kartan-koko :S)
                   "col-sm-12"
                   "col-sm-6")}
    sisalto]
   [:div {:class (if (= @nav/kartan-koko :S)
                   ""
                   "col-sm-6")}
    [kartan-paikka]]])
