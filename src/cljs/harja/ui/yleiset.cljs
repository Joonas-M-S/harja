(ns harja.ui.yleiset
  "Yleisiä UI komponentteja ja apureita"
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.loki :refer [log tarkkaile!]]
            [harja.asiakas.tapahtumat :as t]))

(declare kuuntelija)

(defonce korkeus (atom (-> js/window .-innerHeight)))
(defonce leveys (atom (-> js/window .-innerWidth)))
;;(defonce sisallon-korkeus (atom (-> js/document .-body .-clientHeight)))


(defonce koon-kuuntelija (do (set! (.-onresize js/window)
                                   (fn [_]
                                     (reset! korkeus (-> js/window .-innerHeight))
                                     (reset! leveys (-> js/window .-innerWidth))
                                     ))
                             true))

;;(defonce sisallon-koon-kuuntelija (do
;;                                    (js/setInterval #(reset! sisallon-korkeus (-> js/document .-body .-clientHeight)) 200)
;;                                    true))

(defn navigaation-korkeus []
  (some-> js/document
          (.getElementsByTagName "nav")
          (aget 0)
          .-clientHeight))

(defn murupolun-korkeus []
  (some-> js/document
          (.getElementsByClassName "murupolku")
          (aget 0)
          .-clientHeight))

(defn ajax-loader
  "Näyttää latausanimaatiokuvan ja optionaalisen viestin."
  ([] (ajax-loader nil))
  ([viesti]
     [:div.ajax-loader
      [:img {:src "/images/ajax-loader.gif"}]
      (when viesti
        [:div.viesti viesti])]))


(defn sisalla? 
  "Tarkistaa onko annettu tapahtuma tämän React komponentin sisällä."
  [komponentti tapahtuma]
  (let [dom (reagent/dom-node komponentti)
        elt (.-target tapahtuma)]
    (loop [ylempi (.-parentNode elt)]
      (if (or (nil? ylempi)
              (= ylempi js/document.body))
        false
        (if (= dom ylempi)
          true
          (recur (.-parentNode ylempi)))))))


(defn nuolivalinta
  "Tekee handlerin, joka helpottaa nuolivalinnan tekemistä. Ottaa kolme funktiota: ylös, alas ja enter, 
joita kutsutaan kun niiden näppäimiä paineetaan."
  [ylos alas enter]
  #(let [kc (.-keyCode %)]
     (when (or (= kc 38)
               (= kc 40)
               (= kc 13))
       (.preventDefault %)
       (case kc
         38 ;; nuoli ylös
         (ylos)

         40 ;; nuoli alas
         (alas)

         13 ;; enter
         (enter)))))

  
;;
(defn linkki [otsikko toiminto]
  [:a {:href "#" :on-click #(do (.preventDefault %) (toiminto))} otsikko])

(defn teksti-ja-nappi [teksti napin-teksti toiminto]
  [:div.teksti-ja-nappi
   [:div.form-control.input-group
   [:label.teksti-ja-nappi-label teksti]
   [:button.btn.btn-sm.btn-primary.pull-right
    {:on-click  #(do
                  (.stopPropagation %)
                  (.preventDefault %)
                  (toiminto)
                  nil)
     :type "button"}
    napin-teksti]]])

(defn raksiboksi [teksti checked toiminto info-teksti nayta-infoteksti?]
  [:span
   [:div.raksiboksi.input-group
    [:span.input-group-addon
     [:input {:type "checkbox"
              :checked (if checked "checked" "")
              :on-change #(do (.preventDefault %) (toiminto))}]]
    [:label.form-control {:on-click #(toiminto)}  teksti]]
   (when nayta-infoteksti?
     info-teksti)])

(defn alasveto-ei-loydoksia [teksti]
  [:div.alasveto-ei-loydoksia teksti])

(defn alasvetovalinta [_ vaihtoehdot]
  (kuuntelija
   {:auki (atom false)}

   (fn [{:keys [valinta format-fn valitse-fn class disabled]} vaihtoehdot]
     (let [auki (:auki (reagent/state (reagent/current-component)))]
       [:div.dropdown.harja-alasveto {:class (str class " " (when @auki "open"))}
        [:button.btn.btn-default
         {:type "button"
          :disabled (if disabled "disabled" "")
          :on-click #(do
                       (swap! auki not)
                       nil)
          :on-key-down #(let [kc (.-keyCode %)]
                          (when (or (= kc 38)
                                    (= kc 40)
                                    (= kc 13))
                            (.preventDefault %)
                            (when-not (empty? vaihtoehdot)
                              (let [nykyinen-valittu-idx (loop [i 0]
                                                           (if (= i (count vaihtoehdot))
                                                             nil
                                                             (if (= (nth vaihtoehdot i) valinta)
                                                               i
                                                               (recur (inc i)))))]
                                (case kc
                                  38 ;; nuoli ylös
                                  (if (or (nil? nykyinen-valittu-idx)
                                          (= 0 nykyinen-valittu-idx))
                                    (valitse-fn (nth vaihtoehdot (dec (count vaihtoehdot))))
                                    (valitse-fn (nth vaihtoehdot (dec nykyinen-valittu-idx))))

                                  40 ;; nuoli alas
                                  (if (or (nil? nykyinen-valittu-idx)
                                          (= (dec (count vaihtoehdot)) nykyinen-valittu-idx))
                                    (valitse-fn (nth vaihtoehdot 0))
                                    (valitse-fn (nth vaihtoehdot (inc nykyinen-valittu-idx))))
                               
                                  13 ;; enter
                                  (reset! auki false))))))}
         [:div.valittu (format-fn valinta)]
         " " [:span.caret]]
        [:ul.dropdown-menu
         (for [vaihtoehto vaihtoehdot]
           ^{:key (hash vaihtoehto)}
           [:li (linkki (format-fn vaihtoehto) #(do (valitse-fn vaihtoehto)
                                                    (reset! auki false)
                                                    nil))])
         ]]))

   :body-klikkaus
   (fn [this {klikkaus :tapahtuma}]
     (when-not (sisalla? this klikkaus)
       (reset! (:auki (reagent/state this)) false)))
   ))

(defn radiovalinta [otsikko valinta valitse-fn disabled & vaihtoehdot]
  (let [vaihda-valinta (fn [e] (valitse-fn (keyword (.-value (.-target e)))))]
    [:div.btn-group.pull-right.murupolku-radiovalinta
     [:div otsikko " "]
     (for [[otsikko arvo] (partition 2 vaihtoehdot)]
       ^{:key (hash otsikko)}
       [:label.btn.btn-primary {:disabled (if disabled "disabled" "")}
        [:input {:type "radio" :value (name arvo) :on-change vaihda-valinta
                 :disabled (if disabled "disabled" "")
                 :checked (if (= arvo valinta) true false)} " " otsikko]])]))
(defn kuuntelija
  "Lisää komponentille käsittelijät tietyille tapahtuma-aiheille.
Toteuttaa component-did-mount ja component-will-unmount elinkaarimetodit annetulle komponentille.
aiheet-ja-kasittelijat on vuorotellen aihe (yksi avainsana tai joukko avainsanoja) ja käsittelyfunktio,
jolle annetaan kaksi parametria: komponentti ja tapahtuma. Alkutila on komponentin inital-state."
  [alkutila render-fn & aiheet-ja-kasittelijat]
  (let [kuuntelijat (partition 2 aiheet-ja-kasittelijat)]
    (reagent/create-class
     {:get-initial-state (fn [this] alkutila)
      :reagent-render render-fn
      :component-did-mount (fn [this _]
                             (loop [kahvat []
                                    [[aihe kasittelija] & kuuntelijat] kuuntelijat]
                               (if-not aihe
                                 (reagent/set-state this {::kuuntelijat kahvat})
                                 (recur (concat kahvat
                                                (doall (map #(t/kuuntele! % (fn [tapahtuma] (kasittelija this tapahtuma)))
                                                            (if (keyword? aihe)
                                                              [aihe]
                                                              (seq aihe)))))
                                        kuuntelijat))))
      :component-will-unmount (fn [this _]                                
                                (let [kuuntelijat (-> this reagent/state ::kuuntelijat)]
                                  (doseq [k kuuntelijat]
                                    (k))))})))
                             
    
(defn tietoja
  "Tekee geneerisen tietonäkymän. Optiot on tyhjä mäppi vielä, ehkä jotain classia sinne."
  [optiot & otsikot-ja-arvot]
  [:div.tietoja
   (for [[otsikko arvo] (partition 2 otsikot-ja-arvot)]
     ^{:key otsikko}
     [:div.tietorivi
      [:span.tietokentta otsikko]
      [:span.tietoarvo arvo]])])

;; Yleinen tietopaneeleissa käytettävä tietueen koko.
;; Suurella näytöllä, 4 elementtiä vierekkäin, pienimmällä vain 1 per rivi.
(def tietopaneelin-elementtikoko {:lg 3 :md 4 :sm 6 :xs 12})

(defn rivi
  "Tekee bootstrap .row divin, jossa jokaisella komponentilla on sama koko.
Jos annettu koko on numero, tulee luokaksi col-lg-<koko>, jos koko on mäppi {:lg <iso koko> :md <medium koko> ...}
lisätään eri kokoluokka jokaiselle mäpissä mainitulle koolle."
  [koko & komponentit]
  (let [cls (if-not (map? koko)
              koko
              (apply str (map (fn [[koko-luokka koko]]
                                (str "col-" (name koko-luokka) "-" koko " "))
                              (seq koko))))]
    [:div.row
     (map-indexed
      (fn [i komponentti]
        ^{:key i}
        [:div {:class cls}
         komponentti])
      (keep identity komponentit))]))

(defn otsikolla
  "Käärii annetun komponentin <span> elementiin, ja lisää <h4> otsikon ennen sitä."
  [otsikko komp]
  [:span [:h4 otsikko]
   komp])

;; Lasipaneelin tyyli, huom: parentin on oltava position: relative 
(def lasipaneeli-tyyli {:display "block"
                        :position "absolute"
                        :top 0
                        :bottom 0
                        :opacity 0.5
                        :background-color "black"
                        :height "expression(parentElement.scrollHeight+'px')"
                        :width "100%"})

(defn lasipaneeli [& sisalto]
  [:div {:style lasipaneeli-tyyli}
   sisalto])

