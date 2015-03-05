(ns harja.ui.kentat
  "UI input kenttien muodostaminen typpin perusteella, esim. grid ja lomake komponentteihin."
  (:require [reagent.core :refer [atom] :as r]
            [harja.pvm :as pvm]
            [harja.ui.pvm :as pvm-valinta]
            [harja.ui.yleiset :refer [alasvetovalinta linkki ajax-loader nuolivalinta]]
            [harja.ui.protokollat :refer [hae]]
            [harja.loki :refer [log]]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

;; PENDING: dokumentoi rajapinta, mitä eri avaimia kentälle voi antaa


(defmulti tee-kentta (fn [t _] (:tyyppi t)))

(defmethod tee-kentta :haku [{:keys [lahde nayta]} data]
  (let [nyt-valittu @data
        teksti (atom (if nyt-valittu
                       ((or nayta str) nyt-valittu) ""))
        tulokset (atom nil)
        valittu-idx (atom nil)]
    (fn [_ data]
      [:div.dropdown {:class (when-not (nil? @tulokset) "open")}
       
       [:input {:value @teksti
                :on-change #(let [v (-> % .-target .-value)]
                              (reset! data nil)
                              (reset! teksti v)
                              (if (> (count v) 2)
                                (do (reset! tulokset :haetaan)
                                    (go (let [tul (<! (hae lahde v))]
                                          (reset! tulokset tul)
                                          (reset! valittu-idx nil))))
                                (reset! tulokset nil)))
                :on-key-down (nuolivalinta #(let [t @tulokset]
                                              (log "YLÖS " @valittu-idx)
                                              (when (vector? t)
                                                (swap! valittu-idx
                                                       (fn [idx]
                                                         (if (or (= 0 idx) (nil? idx))
                                                           (dec (count t))
                                                           (dec idx))))))
                                           #(let [t @tulokset]
                                              (log "ALAS " @valittu-idx)
                                              (when (vector? t)
                                                (swap! valittu-idx
                                                       (fn [idx]
                                                         (if (and (nil? idx) (not (empty? t)))
                                                           0
                                                           (if (< idx (dec (count t)))
                                                             (inc idx)
                                                             0))))))
                                           #(let [t @tulokset
                                                  idx @valittu-idx]
                                              (when (number? idx)
                                                (let [v (nth t idx)]
                                                  (reset! data v)
                                                  (reset! teksti ((or nayta str) v))
                                                  (reset! tulokset nil)))))  }]
       [:ul.dropdown-menu {:role "menu"}
        (let [nykyiset-tulokset @tulokset
              idx @valittu-idx]
          (if (= :haetaan nykyiset-tulokset)
            [:li {:role "presentation"} (ajax-loader) " haetaan: " @teksti]
            (map-indexed (fn [i t]
                           ^{:key (hash t)}
                           [:li {:class (when (= i idx) "korostettu") :role "presentation"}
                            [linkki ((or nayta str) t) #(do (reset! data t)
                                                            (reset! teksti ((or nayta str) t))
                                                            (reset! tulokset nil))]])
                         nykyiset-tulokset)))]])))

                
                             
(defmethod tee-kentta :string [{:keys [nimi pituus-max pituus-min regex]} data]
  [:input {:on-change #(reset! data (-> % .-target .-value))
           :value @data}])

(defmethod tee-kentta :numero [kentta data]
  (let [teksti (atom (str @data))]
        (fn [kentta data]
          (let [nykyinen-teksti @teksti]
            [:input {:type "text"
                     :value nykyinen-teksti
                     :on-change #(let [v (-> % .-target .-value)]
                                   (when (or (= v "") 
                                           (re-matches #"\d+((\.|,)\d*)?" v))
                                     (reset! teksti v))
                                   (let [numero (js/parseFloat v)]
                                       (reset! data
                                               (when (not (js/isNaN numero))
                                                 numero))))}]))))



(defmethod tee-kentta :email [kentta data]
  [:input {:type "email"
           :value @data
           :on-change #(reset! data (-> % .-target .-value))}])



(defmethod tee-kentta :puhelin [kentta data]
  [:input {:type "tel"
           :value @data
           :max-length (:pituus kentta)
           :on-change #(let [uusi (-> % .-target .-value)]
                         (when (re-matches #"(\s|\d)*" uusi)
                           (reset! data uusi)))}])

 

(defmethod tee-kentta :valinta [{:keys [valinta-nayta valinta-arvo valinnat]} data]
  (let [arvo (or valinta-arvo :id)
        nayta (or valinta-nayta str)
        nykyinen-arvo (arvo @data)]
    [alasvetovalinta {:valinta @data
                      :valitse-fn #(do (log "valinta: " %)
                                       (reset! data %))
                      :format-fn valinta-nayta}
     valinnat]))




(defmethod tee-kentta :kombo [{:keys [valinnat]} data]
  (let [auki (atom false)]
    (fn [{:keys [valinnat]} data]
      (let [nykyinen-arvo (or @data "")]
        [:div.dropdown {:class (when @auki "open")}
         [:input.kombo {:type "text" :value nykyinen-arvo
                        :on-change #(reset! data (-> % .-target .-value))}]
         [:button {:on-click #(do (swap! auki not) nil)}
          [:span.caret ""]]
         [:ul.dropdown-menu {:role "menu"}
          (for [v (filter #(not= -1 (.indexOf (.toLowerCase (str %)) (.toLowerCase nykyinen-arvo))) valinnat)]
            ^{:key (hash v)}
            [:li {:role "presentation"} [linkki v #(do (reset! data v)
                                                       (reset! auki false))]])]]))))




  
(defmethod tee-kentta :pvm [_ data]
  
  (let [;; pidetään kirjoituksen aikainen ei validi pvm tallessa
        teksti (atom (if-let [p @data]
                       (pvm/pvm p)
                       ""))
        ;; picker auki?
        auki (atom false)

        muuta! (fn [t]
                 (let [d (pvm/->pvm t)]
                   (reset! teksti t)
                   (reset! data d)))
        ]
    (r/create-class
     {:component-will-receive-props
      (fn [this [_ _ data]]
        (swap! teksti #(if-let [p @data]
                         (pvm/pvm p)
                         %)))
      
      :reagent-render
      (fn [_ data]
        (let [nykyinen-pvm @data
              nykyinen-teksti @teksti]
          [:span {:on-click #(do (reset! auki true) nil)}
           [:input.pvm {:value nykyinen-teksti
                        :on-change #(muuta! (-> % .-target .-value))}]
           (when @auki
             [:div.aikavalinta
              [pvm-valinta/pvm {:valitse #(do (reset! auki false)
                                              (reset! data %)
                                              (reset! teksti (pvm/pvm %)))
                                :pvm nykyinen-pvm}]])]))})))
 
