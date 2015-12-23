(ns harja.ui.yleiset
  "Yleisiä UI komponentteja ja apureita"
  (:require [goog.string :as gstr]
            [harja.asiakas.tapahtumat :as t]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.ikonit :as ikonit]
            [reagent.core :refer [atom] :as r])

  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))


(def ie? (let [ua (-> js/window .-navigator .-userAgent)]
           (or (not= -1 (.indexOf ua "MSIE "))
               (not= -1 (.indexOf ua "Trident/"))
               (not= -1 (.indexOf ua "Edge/")))))


(defn karttakuva
  "Palauttaa kuvatiedoston nimen, jos käytössä IE palauttaa .png kuvan, muuten .svg"
  [perusnimi]
  (str perusnimi (if ie? ".png" ".svg")))

(defn elementti-idlla [id]
  (.getElementById js/document (name id)))

(declare kuuntelija)

(defonce korkeus (atom (-> js/window .-innerHeight)))
(defonce leveys (atom (-> js/window .-innerWidth)))

(defonce ikkunan-koko
         (reaction [@leveys @korkeus]))

;;(defonce sisallon-korkeus (atom (-> js/document .-body .-clientHeight)))

(defonce ikkunan-koko-tapahtuman-julkaisu
         (run!
           (let [h @korkeus
                 w @leveys]
             (t/julkaise! {:aihe :ikkunan-koko-muuttunut :leveys w :korkeus h}))))

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
  ([viesti] (ajax-loader viesti nil))
  ([viesti opts]
   [:div {:class (str "ajax-loader " (when (:luokka opts) (:luokka opts)))}
    [:img {:src "images/ajax-loader.gif"}]
    (when viesti
      [:div.viesti viesti])]))

(defn sijainti
  "Laskee DOM-elementin sijainnin, palauttaa [x y w h]."
  [elt]
  (assert elt (str "Ei voida laskea sijaintia elementille null"))
  (let [r (.getBoundingClientRect elt)
        sijainti [(.-left r) (.-top r) (- (.-right r) (.-left r)) (- (.-bottom r) (.-top r))]]
    sijainti))

(defn offset-korkeus [elt]
  (loop [offset (.-offsetTop elt)
         parent (.-offsetParent elt)]
    (if (or (nil? parent)
            (= js/document.body parent))
      offset
      (recur (+ offset (.-offsetTop parent))
             (.-offsetParent parent)))))

(defn sijainti-sailiossa
  "Palauttaa elementin sijainnin suhteessa omaan säiliöön."
  [elt]
  (let [[x1 y1 w1 h1] (sijainti elt)
        [x2 y2 w2 h2] (sijainti (.-parentNode elt))]
    [(- x1 x2) (- y1 y2) w1 h1]))


(defn ajax-loader-pisteet
  "Näyttää latausanimaatiokuvan ja optionaalisen viestin."
  ([] (ajax-loader-pisteet nil))
  ([viesti]
   [:span.ajax-loader-pisteet
    [:img {:class "ajax-loader-pisteet" :src "images/ajax-loader-pisteet.gif"}]
    (when viesti
      [:div.viesti viesti])]))


(defn indeksi [kokoelma itemi]
  (first (keep-indexed #(when (= %2 itemi) %1) kokoelma)))

(defn sisalla?
  "Tarkistaa onko annettu tapahtuma tämän React komponentin sisällä."
  [komponentti tapahtuma]
  (let [dom (r/dom-node komponentti)
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
        38                                                  ;; nuoli ylös
        (ylos)

        40                                                  ;; nuoli alas
        (alas)

        13                                                  ;; enter
        (enter)))))

(defn virheen-ohje
  "Virheen ohje. Tyyppi on :virhe (oletus jos ei annettu) tai :varoitus."
  ([virheet] (virheen-ohje virheet :virhe))
  ([virheet tyyppi]
   [:div {:class (if (= tyyppi :varoitus) "varoitukset" "virheet")}
    [:div {:class (if (= tyyppi :varoitus) "varoitus" "virhe")}
     (for [v virheet]
       ^{:key (hash v)}
       [:span
        (ikonit/warning-sign)
        [:span (str " " v)]])]]))


(defn linkki [otsikko toiminto]
  [:a {:href "#" :on-click #(do (.preventDefault %) (toiminto))} otsikko])

(defn raksiboksi
  ([teksti checked toiminto info-teksti nayta-infoteksti?]
   (raksiboksi teksti checked toiminto info-teksti nayta-infoteksti? nil))
  ([teksti checked toiminto info-teksti nayta-infoteksti? komponentti]
   (let [toiminto-fn (fn [e] (do (.preventDefault e) (toiminto) nil))]
     [:span
      [:div.raksiboksi.input-group
       [:div.input-group-addon
        [:input {:type      "checkbox"
                 :checked   (if checked "checked" "")
                 :on-change #(toiminto-fn %)}]
        [:span.raksiboksi-teksti {:on-click #(toiminto-fn %)} teksti]]
       (when komponentti
         komponentti)]
      (when nayta-infoteksti?
        info-teksti)])))

(defn alasveto-ei-loydoksia [teksti]
  [:div.alasveto-ei-loydoksia teksti])

(defn virheviesti-sailio
  "Luo virheviestin 'sivun sisään'. Jos toinen parametri on jotain muuta kuin nil tai false,
  säiliön display asetetaan inline-blockiksi."
  ([viesti] (virheviesti-sailio viesti nil false))
  ([viesti rasti-funktio] (virheviesti-sailio viesti rasti-funktio false))
  ([viesti rasti-funktio inline-block?]
   (let [sulkemisnappi [:button.inlinenappi.nappi-kielteinen {:on-click #(rasti-funktio)}
                        [ikonit/remove] " Sulje"]]
     (if inline-block?
       [:div.virheviesti-sailio {:style {:display :inline-block}} viesti
        (when rasti-funktio sulkemisnappi)]
       [:div.virheviesti-sailio viesti
        (when rasti-funktio sulkemisnappi)]))))

(defn livi-pudotusvalikko [_ vaihtoehdot]
  (kuuntelija
    {:auki (atom false)}

    (fn [{:keys [valinta format-fn valitse-fn class disabled on-focus title]} vaihtoehdot]
      (let [auki (:auki (r/state (r/current-component)))
            term (atom "")
            format-fn (or format-fn str)]
        [:div.dropdown.livi-alasveto {:class (str class " " (when @auki "open"))}
         [:button.nappi-alasveto
          {:class (when disabled "disabled")
           :type        "button"
           :disabled    (if disabled "disabled" "")
           :title       title
           :on-click    #(do
                          (swap! auki not)
                          nil)
           :on-focus    on-focus
           :on-key-down #(let [kc (.-keyCode %)]
                          ;; keycode 9 on TAB, ei tehdä silloin mitään, jotta kenttien
                          ;; välillä liikkumista ei estetä
                          (when-not (= kc 9)
                            (.preventDefault %)
                            (.stopPropagation %)
                            (if (or (= kc 38)
                                    (= kc 40)
                                    (= kc 13))
                              (do
                                (when-not (empty? vaihtoehdot)
                                  (let [nykyinen-valittu-idx (loop [i 0]
                                                               (if (= i (count vaihtoehdot))
                                                                 nil
                                                                 (if (= (nth vaihtoehdot i) valinta)
                                                                   i
                                                                   (recur (inc i)))))]
                                    (case kc
                                      38                    ;; nuoli ylös
                                      (if (or (nil? nykyinen-valittu-idx)
                                              (= 0 nykyinen-valittu-idx))
                                        (valitse-fn (nth vaihtoehdot (dec (count vaihtoehdot))))
                                        (valitse-fn (nth vaihtoehdot (dec nykyinen-valittu-idx))))

                                      40                    ;; nuoli alas
                                      (if (or (nil? nykyinen-valittu-idx)
                                              (= (dec (count vaihtoehdot)) nykyinen-valittu-idx))
                                        (valitse-fn (nth vaihtoehdot 0))
                                        (valitse-fn (nth vaihtoehdot (inc nykyinen-valittu-idx))))

                                      13                    ;; enter
                                      (reset! auki false)))))

                              (do
                                (reset! term (char kc))
                                (when-let [itemi (first (filter (fn [vaihtoehto]
                                                                  (= (.indexOf (.toLowerCase (format-fn vaihtoehto))
                                                                               (.toLowerCase @term)) 0))
                                                                vaihtoehdot))]
                                  (valitse-fn itemi)
                                  (reset! auki false)))) nil))}

          [:div.valittu (format-fn valinta)]
          [:span.livicon-chevron-down {:class (when disabled "disabled")}]]
         [:ul.dropdown-menu.livi-alasvetolista
          (doall
            (for [vaihtoehto vaihtoehdot]
              ^{:key (hash vaihtoehto)}
              [:li.harja-alasvetolistaitemi
               (linkki (format-fn vaihtoehto) #(do (valitse-fn vaihtoehto)
                                                   (reset! auki false)
                                                   nil))]))
          ]]))

    :body-klikkaus
    (fn [this {klikkaus :tapahtuma}]
      (when-not (sisalla? this klikkaus)
        (reset! (:auki (r/state this)) false)))
    ))

(defn pudotusvalikko [otsikko optiot valinnat]
  [:div.label-ja-alasveto
   [:span.alasvedon-otsikko otsikko]
   [livi-pudotusvalikko optiot valinnat]])


(defn radiovalinta [otsikko valinta valitse-fn disabled & vaihtoehdot]
  (let [vaihda-valinta (fn [e] (valitse-fn (keyword (.-value (.-target e)))))]
    [:div.btn-group.pull-right.murupolku-radiovalinta
     [:div otsikko " "]
     (for [[otsikko arvo] (partition 2 vaihtoehdot)]
       ^{:key (hash otsikko)}
       [:label.btn.btn-primary {:disabled (if disabled "disabled" "")}
        [:input {:type     "radio" :value (name arvo) :on-change vaihda-valinta
                 :disabled (if disabled "disabled" "")
                 :checked  (if (= arvo valinta) true false)} " " otsikko]])]))
(defn kuuntelija
  "Lisää komponentille käsittelijät tietyille tapahtuma-aiheille.
Toteuttaa component-did-mount ja component-will-unmount elinkaarimetodit annetulle komponentille.
aiheet-ja-kasittelijat on vuorotellen aihe (yksi avainsana tai joukko avainsanoja) ja käsittelyfunktio,
jolle annetaan kaksi parametria: komponentti ja tapahtuma. Alkutila on komponentin inital-state."
  [alkutila render-fn & aiheet-ja-kasittelijat]
  (let [kuuntelijat (partition 2 aiheet-ja-kasittelijat)]
    (r/create-class
      {:get-initial-state      (fn [this] alkutila)
       :reagent-render         render-fn
       :component-did-mount    (fn [this _]
                                 (loop [kahvat []
                                        [[aihe kasittelija] & kuuntelijat] kuuntelijat]
                                   (if-not aihe
                                     (r/set-state this {::kuuntelijat kahvat})
                                     (recur (concat kahvat
                                                    (doall (map #(t/kuuntele! % (fn [tapahtuma] (kasittelija this tapahtuma)))
                                                                (if (keyword? aihe)
                                                                  [aihe]
                                                                  (seq aihe)))))
                                            kuuntelijat))))
       :component-will-unmount (fn [this _]
                                 (let [kuuntelijat (-> this r/state ::kuuntelijat)]
                                   (doseq [k kuuntelijat]
                                     (k))))})))

(defn kaksi-palstaa-otsikkoja-ja-arvoja
  "Tekee geneeriset kaksi palstaa. Optiot on tyhjä mäppi vielä, ehkä jotain classia sinne."
  [optiot & otsikot-ja-arvot]
  [:div.tietoja.container
   (for [[otsikko arvo] (partition 2 otsikot-ja-arvot)
         :when arvo]
     ^{:key otsikko}
     [:div.tietorivi.row
      [:div.col-md-4.tietokentta otsikko]
      [:div.col-md-8.tietoarvo arvo]])])

(defn tietoja
  "Tekee geneerisen tietonäkymän. 
Optiot on mäppi, joka tukee seuraavia optioita:
  :otsikot-omalla-rivilla?  jos true, otsikot ovat blockeja (oletus false)"
  [optiot & otsikot-ja-arvot]
  (let [attrs (if (:otsikot-omalla-rivilla? optiot)
                {:style {:display "block"}}
                {})]
    [:div.tietoja
     (for [[otsikko arvo] (partition 2 otsikot-ja-arvot)
           :when arvo]
       ^{:key otsikko}
       [:div.tietorivi
        [:span.tietokentta attrs otsikko]
        [:span.tietoarvo arvo]])]))

(defn taulukkotietonakyma
  "Tekee geneerisen taulukko-tietonäkymän. Optiot on tyhjä mäppi vielä, ehkä jotain classia sinne."
  [optiot & otsikot-ja-arvot]
  [:div.taulukko-tietonakyma
   [:table
    (for [[otsikko arvo] (partition 2 otsikot-ja-arvot)
          :when arvo]
      ^{:key otsikko}
      [:tr
       [:td.taulukko-tietonakyma-tietokentta [:span otsikko]]
       [:td.taulukko-tietonakyma-tietoarvo [:span arvo]]])]])

(defn kuvaus-ja-avainarvopareja
  [kuvaus & avaimet-ja-arvot]
  [:div
   [:div.kuvaus kuvaus]
   (vec (concat [tietoja {}] avaimet-ja-arvot))])


;; Yleinen tietopaneeleissa käytettävä tietueen koko.
;; Suurella näytöllä, 4 elementtiä vierekkäin, pienimmällä vain 1 per rivi.
(def tietopaneelin-elementtikoko {:lg 3 :md 4 :sm 6 :xs 12})

(defn rivi
  "Tekee bootstrap .row divin, jossa jokaisella komponentilla on sama koko.
Jos annettu koko on numero, tulee luokaksi col-lg-<koko>, jos koko on mäppi {:lg <iso koko> :md <medium koko> ...}
lisätään eri kokoluokka jokaiselle mäpissä mainitulle koolle."
  [{:keys [koko luokka]} & komponentit]
  (let [cls (if-not (map? koko)
              koko
              (apply str (map (fn [[koko-luokka koko]]
                                (str "col-" (name koko-luokka) "-" koko " "))
                              (seq koko))))]
    [:div.row
     (map-indexed
       (fn [i komponentti]
         ^{:key i}
         [:div {:class (str cls luokka)}
          komponentti])
       (keep identity komponentit))]))

(defn otsikolla
  "Käärii annetun komponentin <span> elementiin, ja lisää <h4> otsikon ennen sitä."
  [otsikko komp]
  [:span [:h4 otsikko]
   komp])

;; Lasipaneelin tyyli, huom: parentin on oltava position: relative 
(def lasipaneeli-tyyli {:display          "block"
                        :position         "absolute"
                        :top              0
                        :bottom           0
                        :opacity          0.5
                        :background-color "black"
                        :height           "expression(parentElement.scrollHeight+'px')"
                        :width            "100%"})

(defn lasipaneeli [& sisalto]
  [:div {:style lasipaneeli-tyyli}
   sisalto])

(defn keskita
  "Div-elementti, joka on absoluuttisesti positioitu top 50% left 50%"
  [& sisalto]
  [:div {:style {:position "absolute" :top "50%" :left "50%"}}
   sisalto])

(def +korostuksen-kesto+ 4000)

(defn taulukko2
  "Otsikoille ja arvoille annettavien luokkien tulee noudattaa Bootstrap-sarakkeille annettavien luokkien kaavaa."
  [otsikko-class arvo-class otsikko-max-pituus arvo-max-pituus & otsikot-ja-arvot]
  [:span
   (keep-indexed (fn [i [otsikko arvo]]
                   (and otsikko arvo
                        ^{:key i}
                        [:div.row
                         [:div {:class otsikko-class :style {:max-width otsikko-max-pituus}} otsikko]
                         [:div {:class arvo-class :style {:max-width arvo-max-pituus}} arvo]]))
                 (partition 2 otsikot-ja-arvot))])

(defn- luo-haitarin-rivi [piiloita? rivi]
  ^{:key (:otsikko @rivi)}
  [:div.haitari-rivi
   [:div.haitari-heading.klikattava
    {:on-click #(do
                 (swap! rivi assoc :auki (not (:auki @rivi)))
                 (.preventDefault %))}
    [:span.haitarin-tila (if (:auki @rivi) (ikonit/chevron-down) (ikonit/chevron-right))]
    [:div.haitari-title (when piiloita? {:class "haitari-piilossa"}) (or (:otsikko @rivi) "")]]
   [:div.haitari-sisalto (if (:auki @rivi) {:class "haitari-auki"} {:class "haitari-kiinni"}) (:sisalto @rivi)]])

(defn- pakota-haitarin-rivi-auki
  ([rivit] (pakota-haitarin-rivi-auki rivit (first (keys @rivit))))
  ([rivit avattavan-avain]
   (swap! rivit assoc-in [avattavan-avain :auki] true)))

(defn laske-haitarin-paikka [asetus]
  (cond
    (true? asetus) "185px"
    (number? asetus) (str asetus "px")
    (string? asetus) asetus))

(defn haitari
  ([rivit] (haitari rivit {}))
  ([rivit {:keys [vain-yksi-auki? otsikko aina-joku-auki? piiloita-kun-kiinni? leijuva? luokka]}]
   (let [piilota? (and piiloita-kun-kiinni? (not (some (fn [[_ r]] (:auki r)) @rivit)))]
     (when aina-joku-auki?
       (when-not (some (fn [[_ r]] (:auki r)) @rivit)
         (pakota-haitarin-rivi-auki rivit)))
     [:div {:class (str "harja-haitari " (when luokka luokka))}
      (when leijuva? {:class "leijuva" :style {:top (laske-haitarin-paikka leijuva?)}})
      (when otsikko [:div.haitari-otsikko (if (string? otsikko)
                                            otsikko
                                            [otsikko])])
      [:div.haitari
       (for [[avain rivi] @rivit]
         (luo-haitarin-rivi
           piilota?
           (r/wrap
             rivi
             (fn [uusi]
               (swap! rivit assoc avain uusi)
               ;; Jos vain yksi voi olla auki ja tämä rivi aukaistiin, sulje muut.
               (when (and (:auki uusi) vain-yksi-auki?)
                 (reset! rivit (into {} (map
                                          (fn [[a r]]
                                            (if-not (= avain a)
                                              [a (assoc r :auki false)]
                                              [a r]))
                                          @rivit))))

               ;; Jos rivi suljettiin, ja jonkun pitää olla auki, ja yksikään ei ole auki,
               ;; niin älä sulje riviä.
               (when (and (not (:auki uusi)) aina-joku-auki?)
                 (when-not (some (fn [[_ r]] (:auki r)) @rivit)
                   (swap! rivit assoc-in [avain :auki] true)))))))]])))

(defn pudotuspaneeli
  ([sisalto] (pudotuspaneeli sisalto {}))
  ([sisalto opts]
   (let [piilota? (or (:piilota-kun-kiinni? opts) false)
         rivi (atom (assoc opts :sisalto sisalto
                                :auki (or (:auki opts) false)))]
     (fn [sisalto opts]
       [:div.harja-haitari [:div.haitari (luo-haitarin-rivi piilota? rivi)]]))))

(def +valitse-kuukausi+
  "- Valitse kuukausi -")

(def +valitse-indeksi+
  "- Valitse indeksi -")

(def +ei-sidota-indeksiin+
  "Ei sidota indeksiin")

(defn tierekisteriosoite
  ([numero alkuosa alkuetaisyys] (tierekisteriosoite numero alkuosa alkuetaisyys nil nil))
  ([numero alkuosa alkuetaisyys loppuosa loppuetaisyys]
   [:span.tierekisteriosoite
    [:span.tie "Tie " numero] " / "
    [:span.alkuosa alkuosa] " / "
    [:span.alkuetaisyys alkuetaisyys]
    (when (and loppuosa loppuetaisyys)
      [:span
       " / " [:span.loppuosa loppuosa]
       " / " [:span.loppuetaisyys loppuetaisyys]])]))

(defn vihje
  ([teksti] (vihje teksti nil))
  ([teksti luokka]
   [:div {:class
          (str "lomake-vihje " (or luokka ""))}
    [:div.vihjeen-sisalto
     (harja.ui.ikonit/info-sign)
     (str " " teksti)]]))

(def +tehtavien-hinta-vaihtoehtoinen+ "Urakan tehtävillä voi olla joko yksikköhinta tai muutoshinta")

(defn pitka-teksti
  "Näyttää pitkän tekstin, josta näytetään oletuksena vain ensimmäinen rivi. Käyttäjä voi näyttää/piilottaa 
jatkon."
  ([teksti] (pitka-teksti teksti true))
  ([teksti piilotettu?]
   (let [piilossa? (atom piilotettu?)]
     (fn [teksti _]
       (if-not teksti
         [:span]
         [:span.pitka-teksti
          (if @piilossa?
            [:span.piilossa
             (.substring teksti 0 80)
             (when (> (count teksti) 80)
               [:a.nayta-tai-piilota {:href "#" :on-click #(do (.preventDefault %)
                                                               (swap! piilossa? not))}
                "Lisää..."])]
            [:span.naytetaan
             teksti
             [:a.nayta-tai-piilota {:href "#" :on-click #(do (.preventDefault %)
                                                             (swap! piilossa? not))}
              "Piilota"]])])))))

