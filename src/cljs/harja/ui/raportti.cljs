(ns harja.ui.raportti
  "Harjan raporttielementtien HTML näyttäminen."
  (:require [harja.ui.grid :as grid]
            [harja.ui.dom :as dom]
            [harja.ui.yleiset :as yleiset]
            [harja.visualisointi :as vis]
            [harja.loki :refer [log]]
            [harja.asiakas.kommunikaatio :as k]))

(defmulti muodosta-html
  "Muodostaa Reagent komponentin annetulle raporttielementille."
  (fn [elementti]
    (assert (and (vector? elementti)
                 (> (count elementti) 1)
                 (keyword? (first elementti)))
            (str "Raporttielementin on oltava vektori, jonka 1. elementti on tyyppi ja muut sen sisältöä. Raporttielementti oli: " (pr-str elementti)))
    (first elementti)))


(defmethod muodosta-html :taulukko [[_ {:keys [otsikko viimeinen-rivi-yhteenveto?
                                               korosta-rivit korostustyyli oikealle-tasattavat-kentat]}
                                     sarakkeet data]]
  (log "GRID DATALLA: " (pr-str sarakkeet) " => " (pr-str data))
  (let [oikealle-tasattavat-kentat (or oikealle-tasattavat-kentat #{})]
    [grid/grid {:otsikko            (or otsikko "")
                :tunniste           (fn [rivi] (str "raportti_rivi_"
                                                    (or (::rivin-indeksi rivi)
                                                        (hash rivi))))
                :piilota-toiminnot? true}
     (into []
           (map-indexed (fn [i sarake]
                          (merge
                            {:hae #(get % i)
                             :leveys (:leveys sarake)
                             :otsikko (:otsikko sarake)
                             :pakota-rivitys? (:pakota-rivitys? sarake)
                             :otsikkorivi-luokka (:otsikkorivi-luokka sarake)
                             :nimi (str "sarake" i)
                             :tyyppi (if (= (:tyyppi sarake) :liite)
                                       :komponentti
                                       :string)
                             :tasaa (when (oikealle-tasattavat-kentat i) :oikea)}
                            (when (= (:tyyppi sarake) :liite)
                              {:komponentti (fn [rivi]
                                              (let [liitteet (second (get rivi i))]
                                                [:span
                                                 (map-indexed
                                                   (fn [index liite]
                                                     [:span
                                                      [:a {:href (k/liite-url (:id liite))
                                                           :target "_blank"}
                                                       (inc index)]
                                                      [:span " "]])
                                                   liitteet)]))})))
                        sarakkeet))
     (if (empty? data)
       [(grid/otsikko "Ei tietoja")]
       (let [viimeinen-rivi (last data)]
         (into []
               (map-indexed (fn [index rivi]
                              (if-let [otsikko (:otsikko rivi)]
                                (grid/otsikko otsikko)
                                (let [mappina (assoc
                                                (zipmap (range (count sarakkeet))
                                                       rivi)
                                                ::rivin-indeksi index)]
                                  (cond-> mappina
                                          (and viimeinen-rivi-yhteenveto?
                                               (= viimeinen-rivi rivi))
                                          (assoc :yhteenveto true)
                                          (when korosta-rivit (korosta-rivit index))
                                          (assoc :korosta true))))))
               data)))]))


(defmethod muodosta-html :otsikko [[_ teksti]]
  [:h3 teksti])

(defmethod muodosta-html :otsikko-kuin-pylvaissa [[_ teksti]]
  [:h3 teksti])

(defmethod muodosta-html :teksti [[_ teksti {:keys [vari]}]]
  [:p {:style {:color (when vari vari)}} teksti])

(defmethod muodosta-html :varoitusteksti [[_ teksti]]
  (muodosta-html [:teksti teksti {:vari "#dd0000"}]))

(defmethod muodosta-html :pylvaat [[_ {:keys [otsikko vari fmt piilota-arvo? legend]} pylvaat]]
  (let [w (int (* 0.85 @dom/leveys))
        h (int (/ w 2.9))]
    [:div.pylvaat
     [:h3 otsikko]
     [vis/bars {:width         w
                :height        h
                :format-amount (or fmt str)
                :hide-value?   piilota-arvo?
                :legend legend
                }
      pylvaat]]))

(defmethod muodosta-html :yhteenveto [[_ otsikot-ja-arvot]]
  (apply yleiset/taulukkotietonakyma {}
         (mapcat identity otsikot-ja-arvot)))

  
(defmethod muodosta-html :raportti [[_ raportin-tunnistetiedot & sisalto]]
  (log "muodosta html raportin-tunnistetiedot " (pr-str raportin-tunnistetiedot))
  [:div.raportti {:class (:tunniste raportin-tunnistetiedot)}
   (when (:nimi raportin-tunnistetiedot)
     [:h3 (:nimi raportin-tunnistetiedot)])
   (keep-indexed (fn [i elementti]
                   (when elementti
                     ^{:key i}
                     [muodosta-html elementti]))
                 (mapcat (fn [sisalto]
                           (if (list? sisalto)
                             sisalto
                             [sisalto]))
                         sisalto))])
