(ns harja.views.vesivaylat.urakka.toimenpiteet.yksikkohintaiset
  (:require [reagent.core :refer [atom]]
            [tuck.core :refer [tuck]]
            [harja.ui.otsikkopaneeli :refer [otsikkopaneeli]]
            [harja.domain.vesivaylat.toimenpide :as to]
            [harja.ui.yleiset :refer [ajax-loader]]
            [harja.tiedot.vesivaylat.urakka.toimenpiteet.yksikkohintaiset :as tiedot]
            [harja.ui.komponentti :as komp]
            [harja.loki :refer [log]]
            [harja.ui.debug :refer [debug]]
            [harja.ui.kentat :as kentat]
            [harja.ui.grid :as grid]
            [harja.pvm :as pvm]
            [harja.fmt :as fmt]
            [reagent.core :as r]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.napit :as napit]
            [clojure.string :as str]
            [harja.ui.valinnat :as valinnat]
            [harja.tiedot.navigaatio :as nav]
            [harja.domain.vesivaylat.vayla :as va]
            [harja.domain.vesivaylat.turvalaite :as tu]
            [harja.views.urakka.valinnat :as urakka-valinnat]
            [harja.tiedot.urakka :as u]
            [harja.views.vesivaylat.urakka.toimenpiteet.jaettu :as jaettu])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def otsikoiden-checkbox-sijainti "94.3%")
;; FIXME Tämä ei aina osu täysin samalle Y-akselille gridissä olevien checkboksien kanssa
;; Ilmeisesti mahdoton määrittää arvoa, joka toimisi aina?

(defn- ryhmittele-toimenpiteet-vaylalla [e! toimenpiteet]
  (let [vaylalla-ryhmiteltyna (group-by ::to/vayla toimenpiteet)
        vaylat (keys vaylalla-ryhmiteltyna)]
    (vec (mapcat (fn [vayla]
                   (cons (grid/otsikko
                           (grid/otsikkorivin-tiedot
                             (::va/nimi vayla)
                             (count (to/toimenpiteet-vaylalla toimenpiteet (::va/id vayla))))
                           {:id (::va/id vayla)
                            :otsikkokomponentit
                            [{:sijainti otsikoiden-checkbox-sijainti
                              :sisalto
                              (fn [{:keys [id]}]
                                (let [vayla-id id
                                      vaylan-toimenpiteet (to/toimenpiteet-vaylalla toimenpiteet vayla-id)
                                      kaikki-valittu? (every? true? (map :valittu? vaylan-toimenpiteet))
                                      mitaan-ei-valittu? (every? (comp not true?)
                                                                 (map :valittu? vaylan-toimenpiteet))]
                                  [kentat/tee-kentta
                                   {:tyyppi :checkbox}
                                   (r/wrap (cond kaikki-valittu? true
                                                 mitaan-ei-valittu? false
                                                 :default ::kentat/indeterminate)
                                           (fn [uusi]
                                             (e! (tiedot/->ValitseVayla {:vayla-id vayla-id
                                                                         :valinta uusi}))))]))}]})
                         (get vaylalla-ryhmiteltyna vayla)))
                 vaylat))))

(defn- toimenpiteet-tyolajilla [toimenpiteet tyolajit]
  (filterv #(= (::to/tyolaji %) tyolajit) toimenpiteet))

(defn- suodata-ja-ryhmittele-toimenpiteet-gridiin [e! toimenpiteet tyolaji]
  (-> toimenpiteet
      (toimenpiteet-tyolajilla tyolaji)
      (->> (ryhmittele-toimenpiteet-vaylalla e!))))

(defn- toimenpide-infolaatikossa [toimenpide]
  [:div
   [yleiset/tietoja {:otsikot-omalla-rivilla? true
                     :kavenna? true
                     :jata-kaventamatta #{"Työlaji" "Työluokka" "Toimenpide"}
                     :otsikot-samalla-rivilla #{"Työlaji" "Työluokka" "Toimenpide"}
                     :tyhja-rivi-otsikon-jalkeen #{"Vesialue ja väylä" "Toimenpide"}}
    ;; TODO Osa tiedoista puuttuu
    "Urakoitsija" "-"
    "Sopimusnumero" "-"
    "Vesialue ja väylä" (get-in toimenpide [::to/vayla ::va/nimi])
    "Työlaji" (to/reimari-tyolaji-fmt (::to/tyolaji toimenpide))
    "Työluokka" (::to/tyoluokka toimenpide)
    "Toimenpide" (::to/toimenpide toimenpide)
    "Päivämäärä ja aika" (pvm/pvm-opt (::to/pvm toimenpide))
    "Turvalaite" (get-in toimenpide [::to/turvalaite ::tu/nimi])
    "Urakoitsijan vastuuhenkilö" "-"
    "Henkilölukumaara" "-"]
   [:footer.livi-grid-infolaatikko-footer
    [:h5 "Käytetyt komponentit"]
    [:table
     [:thead
      [:tr
       [:th {:style {:width "50%"}} "Kompo\u00ADnent\u00ADti"]
       [:th {:style {:width "25%"}} "Määrä"]
       [:th {:style {:width "25%"}} "Jäljellä"]]]
     [:tbody
      [:tr
       ;; TODO Komponenttitiedot puuttuu
       [:td "-"]
       [:td "-"]
       [:td "-"]]]]]])

(defn- paneelin-sisalto [e! toimenpiteet]
  [grid/grid
   {:tunniste ::to/id
    :infolaatikon-tila-muuttui (fn [nakyvissa?]
                                 (e! (tiedot/->AsetaInfolaatikonTila nakyvissa?)))
    :rivin-infolaatikko (fn [rivi data]
                          [toimenpide-infolaatikossa rivi])
    :salli-valiotsikoiden-piilotus? true
    :ei-footer-muokkauspaneelia? true
    :valiotsikoiden-alkutila :kaikki-kiinni}
   [{:otsikko "Työluokka" :nimi ::to/tyoluokka :fmt to/reimari-tyoluokka-fmt :leveys 10}
    {:otsikko "Toimenpide" :nimi ::to/toimenpide :fmt to/reimari-toimenpidetyyppi-fmt :leveys 10}
    {:otsikko "Päivämäärä" :nimi ::to/pvm :fmt pvm/pvm-opt :leveys 10}
    {:otsikko "Turvalaite" :nimi ::to/turvalaite :leveys 10 :hae #(get-in % [::to/turvalaite ::tu/nimi])}
    {:otsikko "Valitse" :nimi :valinta :tyyppi :komponentti :tasaa :keskita
     :komponentti (fn [rivi]
                    ;; TODO Olisi kiva jos otettaisiin click koko solun alueelta
                    ;; Siltatarkastuksissa käytetty radio-elementti expandoi labelin
                    ;; koko soluun. Voisi ehkä käyttää myös checkbox-elementille
                    ;; Täytyy kuitenkin varmistaa, ettei mikään mene rikki.
                    ;; Ja entäs otsikkorivit?
                    [kentat/tee-kentta
                     {:tyyppi :checkbox}
                     (r/wrap (:valittu? rivi)
                             (fn [uusi]
                               (e! (tiedot/->ValitseToimenpide {:id (::to/id rivi)
                                                                :valinta uusi}))))])
     :leveys 5}]
   toimenpiteet])

(defn- luo-otsikkorivit [e! toimenpiteet]
  (let [tyolajit (keys (group-by ::to/tyolaji toimenpiteet))]
    (vec (mapcat
           (fn [tyolaji]
             [tyolaji
              (grid/otsikkorivin-tiedot (to/reimari-tyolaji-fmt tyolaji)
                                        (count (toimenpiteet-tyolajilla
                                                 toimenpiteet
                                                 tyolaji)))
              [paneelin-sisalto
               e!
               (suodata-ja-ryhmittele-toimenpiteet-gridiin
                 e!
                 toimenpiteet
                 tyolaji)]])
           tyolajit))))

(defn- suodattimet-ja-toiminnot [e! app urakka]
  [valinnat/urakkavalinnat {}
   ^{:key "valintaryhmat"}
   [valinnat/valintaryhmat-3
    [urakka-valinnat/urakan-sopimus-ja-hoitokausi-ja-aikavali urakka]

    [:div
     [valinnat/vaylatyyppi
      (r/wrap (get-in app [:valinnat :vaylatyyppi])
              (fn [uusi]
                (e! (tiedot/->PaivitaValinnat {:vaylatyyppi uusi}))))
      (sort-by va/tyyppien-jarjestys (into [nil] va/tyypit))
      #(if % (va/tyyppi-fmt %) "Kaikki")]

     [kentat/tee-otsikollinen-kentta {:otsikko "Väylä"
                                      :kentta-params {:tyyppi :haku
                                                      :nayta ::va/nimi
                                                      :lahde tiedot/vaylahaku}
                                      :arvo-atom (r/wrap (get-in app [:valinnat :vayla])
                                                         (fn [uusi]
                                                           (e! (tiedot/->PaivitaValinnat {:vayla (::va/id uusi)}))))}]]

    [:div
     [valinnat/tyolaji
      (r/wrap (get-in app [:valinnat :tyolaji])
              (fn [uusi]
                (e! (tiedot/->PaivitaValinnat {:tyolaji uusi}))))
      (to/jarjesta-reimari-tyolajit (into [nil] (distinct (vals to/reimari-tyolajit))))
      #(if % (to/reimari-tyolaji-fmt %) "Kaikki")]

     [valinnat/tyoluokka
      (r/wrap (get-in app [:valinnat :tyoluokka])
              (fn [uusi]
                (e! (tiedot/->PaivitaValinnat {:tyoluokka uusi}))))
      (to/jarjesta-reimari-tyoluokat (into [nil] (distinct (vals to/reimari-tyoluokat))))
      #(if % (to/reimari-tyoluokka-fmt %) "Kaikki")]

     [valinnat/toimenpide
      (r/wrap (get-in app [:valinnat :toimenpide])
              (fn [uusi]
                (e! (tiedot/->PaivitaValinnat {:toimenpide uusi}))))
      (to/jarjesta-reimari-toimenpidetyypit (into [nil] (distinct (vals to/reimari-toimenpidetyypit))))
      #(if % (to/reimari-toimenpidetyyppi-fmt %) "Kaikki")]]]

   ^{:key "urakkatoiminnot"}
   [valinnat/urakkatoiminnot {:sticky? true}
    ^{:key "siirto"}
    [napit/yleinen-ensisijainen "Siirrä valitut yksikköhintaisiin"
     #(log "Painoit nappia")
     {:disabled (not (some :valittu? (:toimenpiteet app)))}]]])

(defn- toimenpiteet-listaus [e! {:keys [toimenpiteet infolaatikko-nakyvissa? haku-kaynnissa?] :as app}]
  (cond (or haku-kaynnissa? (nil? toimenpiteet)) [ajax-loader "Toimenpiteitä haetaan..."]
        (empty? toimenpiteet) [:div "Ei toimenpiteitä"]

        :default
        (into [otsikkopaneeli
               {:otsikkoluokat (when infolaatikko-nakyvissa? ["livi-grid-infolaatikolla"])
                :paneelikomponentit
                [{:sijainti otsikoiden-checkbox-sijainti
                  :sisalto
                  (fn [{:keys [tunniste]}]
                    (let [tyolajin-toimenpiteet (toimenpiteet-tyolajilla toimenpiteet tunniste)
                          kaikki-valittu? (every? true? (map :valittu? tyolajin-toimenpiteet))
                          mitaan-ei-valittu? (every? (comp not true?)
                                                     (map :valittu? tyolajin-toimenpiteet))]
                      [kentat/tee-kentta
                       {:tyyppi :checkbox}
                       (r/wrap (cond kaikki-valittu? true
                                     mitaan-ei-valittu? false
                                     :default ::kentat/indeterminate)
                               (fn [uusi]
                                 (e! (tiedot/->ValitseTyolaji {:tyolaji tunniste
                                                               :valinta uusi}))))]))}]}]
              (luo-otsikkorivit e! toimenpiteet))))

(defn- yksikkohintaiset-toimenpiteet-nakyma [e! app valinnat]
  (komp/luo
    (komp/watcher tiedot/valinnat (fn [_ _ uusi]
                                    (e! (tiedot/->PaivitaValinnat uusi))
                                    (e! (tiedot/->HaeToimenpiteet {}))))
    (komp/sisaan-ulos #(do (e! (tiedot/->Nakymassa? true))
                           (e! (tiedot/->PaivitaValinnat {:urakka-id (get-in valinnat [:urakka :id])
                                                          :sopimus-id (first (:sopimus valinnat))
                                                          :aikavali (:aikavali valinnat)}))
                           (e! (tiedot/->HaeToimenpiteet {})))
                      #(e! (tiedot/->Nakymassa? false)))
    (fn [e! {:keys [toimenpiteet infolaatikko-nakyvissa?] :as app}]
      @tiedot/valinnat ;; Reaktio on pakko lukea komponentissa, muuten se ei päivity.

      [:div
       [debug app]
       [suodattimet-ja-toiminnot e! app (:urakka valinnat)]
       [toimenpiteet-listaus e! app]])))

(defn- yksikkohintaiset-toimenpiteet* [e! app]
  [yksikkohintaiset-toimenpiteet-nakyma e! app {:urakka @nav/valittu-urakka
                                                 :sopimus @u/valittu-sopimusnumero
                                                 :aikavali @u/valittu-aikavali}])

(defn yksikkohintaiset-toimenpiteet []
  [tuck tiedot/tila yksikkohintaiset-toimenpiteet*])