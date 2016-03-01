(ns harja.views.murupolku
  "Murupolku on sovelluksenlaajuinen navigaatiokomponentti.
  Sen avulla voidaan vaikuttaa sovelluksen tilaan muun muassa
  seuraavia parametrejä käyttäen: väylämuoto, hallintayksikkö,
  urakka, urakan tyyppi, urakoitsija."
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.yleiset :refer [ajax-loader linkki alasveto-ei-loydoksia livi-pudotusvalikko]]

            [harja.loki :refer [log]]
            [harja.tiedot.urakoitsijat :as urakoitsijat]
            [harja.tiedot.hallintayksikot :as hal]
            [harja.tiedot.navigaatio :as nav]
            [harja.asiakas.tapahtumat :as t]
            [harja.ui.komponentti :as komp]
            [harja.ui.dom :as dom]))

(defn koko-maa []
  [:li
   [:a.murupolkuteksti {:href     "#"
                        :style    (when (nil? @nav/valittu-hallintayksikko)
                                    {:text-decoration "none"
                                     :color           "#323232"})
                        :on-click #(do
                                    (.preventDefault %)
                                    (nav/valitse-hallintayksikko nil))}
    "Koko maa"]])

(defn hallintayksikko [valinta-auki]
  (let [valittu @nav/valittu-hallintayksikko]
    [:li.dropdown.livi-alasveto {:class (when (= :hallintayksikko @valinta-auki) "open")}

     (let [vu @nav/valittu-urakka
           va @valinta-auki]
       (if (or (not (nil? vu)) (= va :hallintayksikko))
         [:a.murupolkuteksti {:href     "#"
                              :on-click #(do
                                          (.preventDefault %)
                                          (nav/valitse-hallintayksikko valittu))}
          (str (or (:nimi valittu) "- Hallintayksikkö -") " ")]

         [:span.valittu-hallintayksikko.murupolkuteksti (or (:nimi valittu) "- Hallintayksikkö -") " "]))

     [:button.nappi-murupolkualasveto.dropdown-toggle {:on-click #(swap! valinta-auki
                                                                         (fn [v]
                                                                           (if (= v :hallintayksikko)
                                                                             nil
                                                                             :hallintayksikko)))}
      [:span.livicon-chevron-down]]

     ;; Alasvetovalikko yksikön nopeaa vaihtamista varten
     [:ul.dropdown-menu.livi-alasvetolista {:role "menu"}
      (for [muu-yksikko (filter #(not= % valittu) @hal/hallintayksikot)]
        ^{:key (str "hy-" (:id muu-yksikko))}
        [:li.harja-alasvetolistaitemi
         [linkki (:nimi muu-yksikko)
          #(do (reset! valinta-auki nil)
               (nav/valitse-hallintayksikko muu-yksikko))]])]]))

(defn urakka [valinta-auki]
  (when @nav/valittu-hallintayksikko
    (let [valittu @nav/valittu-urakka]
      [:li.dropdown.livi-alasveto {:class (when (= :urakka @valinta-auki) "open")}
       [:span.valittu-urakka.murupolkuteksti (or (:nimi valittu) "- Urakka -") " "]

       [:button.nappi-murupolkualasveto.dropdown-toggle {:on-click #(swap! valinta-auki
                                                                           (fn [v]
                                                                             (if (= v :urakka)
                                                                               nil
                                                                               :urakka)))}
        [:span.livicon-chevron-down]]

       ;; Alasvetovalikko urakan nopeaa vaihtamista varten
       [:ul.dropdown-menu.livi-alasvetolista {:role "menu"}

        (let [muut-urakat (filter #(not= % valittu) @nav/suodatettu-urakkalista)]

          (if (empty? muut-urakat)
            [alasveto-ei-loydoksia "Tästä hallintayksiköstä ei löydy muita urakoita valituilla hakukriteereillä."]

            (for [muu-urakka muut-urakat]
              ^{:key (str "ur-" (:id muu-urakka))}
              [:li.harja-alasvetolistaitemi [linkki (:nimi muu-urakka) #(nav/valitse-urakka muu-urakka)]])))]])))

(defn urakoitsija []
  [:div.murupolku-urakoitsija
   [:div.livi-valikkonimio.murupolku-urakoitsija-otsikko "Urakoitsija"]
   [livi-pudotusvalikko {:valinta    @nav/valittu-urakoitsija
                         :format-fn  #(if % (:nimi %) "Kaikki")
                         :valitse-fn nav/valitse-urakoitsija!
                         :class      (str "alasveto-urakoitsija" (when (boolean @nav/valittu-urakka) " disabled"))
                         :disabled   (boolean @nav/valittu-urakka)}
    (vec (conj (into [] (case (:arvo @nav/valittu-urakkatyyppi)
                          :hoito @urakoitsijat/urakoitsijat-hoito
                          :paallystys @urakoitsijat/urakoitsijat-paallystys
                          :tiemerkinta @urakoitsijat/urakoitsijat-tiemerkinta
                          :valaistus @urakoitsijat/urakoitsijat-valaistus

                          @urakoitsijat/urakoitsijat-hoito)) ;;defaulttina hoito
               nil))]])

(defn urakkatyyppi-murupolussa []
  [:li
   [livi-pudotusvalikko {:valinta    @nav/valittu-urakkatyyppi
                         :format-fn  #(if % (:nimi %) "Kaikki")
                         :valitse-fn nav/vaihda-urakkatyyppi!
                         :class      (str "alasveto-urakkatyyppi" (when (boolean @nav/valittu-urakka) " disabled"))
                         :disabled   (boolean @nav/valittu-urakka)}
    nav/+urakkatyypit+]])

(defn urakkatyyppi []
  [:div.murupolku-urakkatyyppi
   [:div.livi-valikkonimio.murupolku-urakkatyyppi-otsikko "Urakkatyyppi"]
   [livi-pudotusvalikko {:valinta    @nav/valittu-urakkatyyppi
                         :format-fn  #(if % (:nimi %) "Kaikki")
                         :valitse-fn nav/vaihda-urakkatyyppi!
                         :class      (str "alasveto-urakkatyyppi" (when (boolean @nav/valittu-urakka) " disabled"))
                         :disabled   (boolean @nav/valittu-urakka)}
    nav/+urakkatyypit+]])

(def murupolku-muoto (atom :perus))
(defn aseta-murupolku-muotoon [muoto]
  (reset! murupolku-muoto muoto))
(defn aseta-murupolku-perusmuotoon []
  (reset! murupolku-muoto :perus))

(defn murupolku
  "Itse murupolkukomponentti joka sisältää html:n"
  []
  (let [valinta-auki (atom nil)
        sivu (nav/sivu)]
    (komp/luo
     (komp/kuuntelija
      [:hallintayksikko-valittu :hallintayksikkovalinta-poistettu
       :urakka-valittu :urakkavalinta-poistettu]
      #(reset! valinta-auki false)
      ;; FIXME Tässä voisi käyttää (komp/klikattu-ulkopuolelle #(reset! valinta-auki false))
      ;; Mutta aiheuttaa mystisen virheen kun raporteista poistutaan
      :body-klikkaus
      (fn [this {klikkaus :tapahtuma}]
        (when-not (dom/sisalla? this klikkaus)
          (reset! valinta-auki false))))
     {:component-did-mount (fn [_]
                              (t/julkaise! {:aihe :murupolku-muuttunut}))}
     (fn []
       (let [ur @nav/valittu-urakka
             ei-urakkaa? (nil? ur)]
         [:span {:class (when (empty? @nav/tarvitsen-isoa-karttaa)
                          (cond
                            (= sivu :hallinta) "hide"
                            (= sivu :about) "hide"
                            :default ""))}
          (case @murupolku-muoto
            :tilannekuva [:ol.murupolku
                          [:div.col-sm-8.murupolku-vasen
                           [koko-maa]
                           [hallintayksikko valinta-auki]
                           (when ei-urakkaa?
                             [urakkatyyppi-murupolussa])
                           [urakka valinta-auki]]
                          (when ei-urakkaa?
                            [:div.col-sm-4.murupolku-oikea
                             [urakoitsija]])]
            ;; Perusversio
            (if (or ei-urakkaa? (= sivu :raportit))
              [:ol.murupolku
               [:div.col-sm-6.murupolku-vasen
                [koko-maa]
                [hallintayksikko valinta-auki]
                [urakka valinta-auki]]
               [:div.col-sm-6.murupolku-oikea
                [urakoitsija]
                [urakkatyyppi]]]
              [:ol.murupolku
               [:div.col-sm-12.murupolku-vasen
                [koko-maa]
                [hallintayksikko valinta-auki]
                [urakka valinta-auki]]]))])))))



