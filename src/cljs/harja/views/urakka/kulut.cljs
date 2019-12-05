(ns harja.views.urakka.kulut
  (:require [tuck.core :as tuck]
            [reagent.core :as r]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.ui.debug :as debug]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.urakka.mhu-laskutus :as tiedot]
            [harja.ui.taulukko.taulukko :as taulukko]
            [harja.ui.taulukko.jana :as jana]
            [harja.ui.taulukko.osa :as osa]
            [harja.ui.taulukko.tyokalut :as tyokalu]
            [harja.ui.taulukko.protokollat :as p]
            [harja.ui.modal :as modal]
            [harja.ui.liitteet :as liitteet]
            [harja.loki :refer [log]]
            [harja.loki :as loki]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.pvm :as pvm-valinta])
  (:require-macros [harja.ui.taulukko.tyokalut :refer [muodosta-taulukko]]))

(defn- osien-paivitys-fn
  [funktiot]
  (fn [osat]
    (mapv
      (fn [osa]
        (let [paivitys (partial (get funktiot (p/osan-id osa)))]
          (paivitys osa)))
      osat)))

(defn- luo-paivitys-fn
  [& avain-arvot]
  (fn [osa]
    (apply
      (partial p/aseta-arvo osa)
      avain-arvot)))

(defn aliurakoitsija-modaali
  [_ _]
  (let [tila (r/atom {})]
    (fn [tallennus-fn sulku-fn]
      (let [{:keys [nimi ytunnus]} @tila]
        [:div.peitto-modal
         [:div.peitto-kontentti
          [:h1 "Lisää aliurakoitsija"
           [:input {:type  :button
                    :value "X"}]]
          [:label "Yrityksen nimi"
           [:input.input-default.komponentin-input
            {:type      :text
             :value     nimi
             :on-change #(swap! tila assoc :nimi (-> % .-target .-value))}]]
          [:label "Y-tunnus"
           [:input.input-default.komponentin-input
            {:type      :text
             :value     ytunnus
             :on-change #(swap! tila assoc :ytunnus (-> % .-target .-value))}]]
          [:div
           [:button
            {:class    #{"tallenna"}
             :on-click #(tallennus-fn @tila)}
            "Tallenna"]
           [:button
            {:class    #{"sulje"}
             :on-click sulku-fn}
            "Sulje"]]]]))))

(defn- validoi
  [pakolliset objekti]
  (some #(not (get % pakolliset)) (keys objekti)))

(defn alasveto-toiminnolla
  [_ _]
  (let [auki? (r/atom false)]
    (komp/luo
      (komp/klikattu-ulkopuolelle #(reset! auki? false))
      (fn [toiminto {:keys [valittu valinnat valinta-fn formaatti-fn]}]
        (loki/log valinnat valittu)
        [:div {:class #{"select-default" (when @auki? "open")}}
         [:button.nappi-alasveto
          [:div.valittu {:on-click #(swap! auki? not)}
           (or (formaatti-fn valittu)
               "Ei valittu")]]
         [:ul {:style {:display (if @auki?
                                  "block"
                                  "none")}}
          (for [v valinnat]
            [:li.harja-alasvetolistaitemi
             {:on-click #(do
                           (swap! auki? not)
                           (valinta-fn v))}
             [:span (formaatti-fn v)]])
          [:li.harja-alasvetolistaitemi [toiminto {:sulje #(swap! auki? not)}]]]]))))

(def kuukaudet-strs {:tammikuu  "Tammikuu"
                     :helmikuu  "Helmikuu"
                     :maaliskuu "Maaliskuu"
                     :huhtikuu  "Huhtikuu"
                     :toukokuu  "Toukokuu"
                     :kesakuu   "Kesäkuu"
                     :heinakuu  "Heinäkuu"
                     :elokuu    "Elokuu"
                     :syyskuu   "Syyskuu"
                     :lokakuu   "Lokakuu"
                     :marraskuu "Marraskuu"
                     :joulukuu  "Joulukuu"})

(defn lisaa-kohdistus [m]
  (conj m {:tehtavaryhma        nil
           :toimenpideinstanssi nil
           :suorittaja-nimi     nil
           :summa               nil
           :rivi                (count m)}))

(defonce kuukaudet [:lokakuu :marraskuu :joulukuu :tammikuu :helmikuu :maaliskuu :huhtikuu :toukokuu :kesakuu :heinakuu :elokuu :syyskuu])

(defn lisatiedot [paivitys-fn {:keys [aliurakoitsija] :as lomake} e! aliurakoitsijat]
  [:div.col-sm-6.col-xs-12
   [:h2 "Lisätiedot"]
   [:div
    [:label "Aliurakoitsija"]
    [alasveto-toiminnolla
     (fn [{sulje :sulje}]
       [:div
        {:on-click #(do
                      (sulje)
                      (paivitys-fn :nayta :aliurakoitsija-modaali))}
        "Lisää aliurakoitsija"])
     {:valittu      (some #(when (= aliurakoitsija (:id %)) %) aliurakoitsijat)
      :valinnat     aliurakoitsijat
      :valinta-fn   #(paivitys-fn :aliurakoitsija (:id %)
                                  :kohdistukset (fn [kohdistukset] (mapv (fn [m] (assoc m :suorittaja-nimi (:nimi %))) kohdistukset)))
      :formaatti-fn #(get % :nimi)}]
    [:label "Aliurakoitsijan y-tunnus"]
    [:div.select-default [:button.nappi-alasveto {:disabled true}
                          [:div.valittu (or (some #(when (= aliurakoitsija (:id %)) (:ytunnus %)) aliurakoitsijat)
                                            "Y-tunnus puuttuu")]]]]
   [:div
    [:label "Kirjoita tähän halutessasi lisätietoa"]
    [:input.input-default.komponentin-input
     {:type      :text
      :on-change #(paivitys-fn :lisatieto (-> % .-target .-value))}]]
   [:div
    [:label "Liite"]
    [liitteet/lisaa-liite (-> @tila/yleiset :urakka :id) {:liite-ladattu #(e! (tiedot/->LiiteLisatty %))}]
    ;:kuvaus, :fileyard-hash, :urakka, :nimi,
    ;:id,:lahde,:tyyppi, :koko 65528
    ]])

(defn laskun-tiedot [paivitys-fn {:keys [koontilaskun-kuukausi erapaiva viite kohdistukset] :as lomake}]
  [:div.col-sm-6.col-xs-12
   [:h2 "Koontilaskun tiedot"]
   [:div
    [:label.alasvedon-otsikko "Koontilaskun kuukausi"]
    [yleiset/livi-pudotusvalikko {:vayla-tyyli? true
                                  :valinta      koontilaskun-kuukausi
                                  :valitse-fn   #(paivitys-fn :koontilaskun-kuukausi %)
                                  :format-fn    #(get kuukaudet-strs %)}
     kuukaudet]]
   [:div
    [:label "Laskun pvm"]
    [pvm-valinta/pvm-valintakalenteri-inputilla {:valitse       #(paivitys-fn :erapaiva %)
                                                 :luokat        #{"input-default" "komponentin-input"}
                                                 :pvm           erapaiva
                                                 :pakota-suunta false
                                                 :valittava?-fn #(true? true)}]]
   [:div
    [:label "Laskun viite"]
    [:input.input-default.komponentin-input
     {:type      :text
      :value     viite
      :on-change #(paivitys-fn :viite (-> % .-target .-value))}]]
   [:div
    [:label "Koontilaskun numero"]
    [:input.input-default.komponentin-input
     {:type      :text
      :on-change #(paivitys-fn :laskun-numero (-> % .-target .-value))}]]
   (when (< (count kohdistukset) 2)
     [:div
      [:label "Määrä"]
      [:input.input-default.komponentin-input
       {:type      :text
        :value     (or (get-in lomake [:kohdistukset 0 :summa])
                       0)
        :on-change #(paivitys-fn [:kohdistukset 0 :summa] (-> % .-target .-value js/parseFloat))}]])])

(defn tehtavaryhma-maara
  [{:keys [tehtavaryhmat kohdistukset-lkm paivitys-fn]} indeksi t]
  (let [{:keys [tehtavaryhma summa]} t]
    (loki/log "TR" tehtavaryhma)
    [:div.lomake-rivi
     [:div.row
      [:div.col-xs-12.col-sm-6
       [:label "Tehtäväryhmä"]
       [yleiset/livi-pudotusvalikko {:vayla-tyyli? true
                                     :valinta      tehtavaryhma
                                     :valitse-fn   #(paivitys-fn [:kohdistukset indeksi :tehtavaryhma] (:id %)
                                                                 [:kohdistukset indeksi :toimenpideinstanssi] (:toimenpideinstanssi %))
                                     :format-fn    #(get % :tehtavaryhma)}
        tehtavaryhmat]]]
     (when (> kohdistukset-lkm 1)
       [:div.row
        [:div.col-xs-12.col-sm-6
         [:label "Määrä"]
         [:input.input-default.komponentin-input
          {:type      :text
           :value     summa
           :on-change #(paivitys-fn [:kohdistukset indeksi :summa] (-> % .-target .-value js/parseFloat))}]]]
       )]))

(defn tehtavien-syotto [paivitys-fn {:keys [kohdistukset] :as lomake} tehtavaryhmat]
  (let [kohdistukset-lkm (count kohdistukset)]
    [:div.col-xs-12.col-sm-6
     [:input#kulut-kohdistuvat-useammalle.vayla-checkbox {:type      :radio
                                                          :on-change #(paivitys-fn :kohdistukset lisaa-kohdistus)}]
     [:label {:for "kulut-kohdistuvat-useammalle"} "Kulut kohdistuvat useammalle eri tehtävälle"]
     (into [:div] (map-indexed (r/partial tehtavaryhma-maara {:tehtavaryhmat tehtavaryhmat :kohdistukset-lkm kohdistukset-lkm :paivitys-fn paivitys-fn}) kohdistukset))
     (when (> kohdistukset-lkm 1)
       [:div.row
        [:div.col-xs-12.col-sm-6 {:on-click #(paivitys-fn :kohdistukset lisaa-kohdistus)} "+ lisää juttuja"]])]))

(defn- kulujen-syottolomake
  [e! _]
  (let [paivitys-fn (fn [& polut-ja-arvot]
                      (e! (tiedot/->PaivitaLomake polut-ja-arvot)))]
    (fn [e! {:keys [syottomoodi lomake aliurakoitsijat tehtavaryhmat]}]
      (let [{:keys [nayta]} lomake
            validointi-fn (partial validoi #{:summa :koontilaskun-kuukausi})]
        [:div
         [debug/debug @tila/yleiset]
         [debug/debug lomake]
         [:div.row
          [:h1 "Uusi kulu"]
          [tehtavien-syotto paivitys-fn lomake tehtavaryhmat]]
         [:div.row
          [laskun-tiedot paivitys-fn lomake]
          [lisatiedot paivitys-fn lomake e! aliurakoitsijat]]
         [:button {:class    #{"nappi" "nappi-ensisijainen"}
                   :on-click #(e! (tiedot/->TallennaKulu))}
          "Tallenna"]
         [:button {:class    #{"nappi" "nappi-toissijainen"}
                   :on-click #(e! (tiedot/->KulujenSyotto (not syottomoodi)))}
          "Peruuta!"]
         (when (= nayta :aliurakoitsija-modaali)
           [aliurakoitsija-modaali
            (fn [arvo]
              (e! (tiedot/->LuoUusiAliurakoitsija arvo))
              (paivitys-fn :nayta nil))
            #(paivitys-fn :nayta nil)])]))))

(defn- luo-kulumodaali
  [e! app]
  [kulujen-syottolomake e! app])

(defn- luo-kulutaulukko
  []
  (loki/log "taulukon luonti")
  (let [paivitysfunktiot {"Pvm"          (luo-paivitys-fn
                                           :id :pvm
                                           :arvo "Pvm")
                          "Maksuerä"     (luo-paivitys-fn
                                           :id :maksuera
                                           :arvo "Maksuerä")
                          "Toimenpide"   (luo-paivitys-fn
                                           :id :toimenpide
                                           :arvo "Toimenpide")
                          "Tehtäväryhmä" (luo-paivitys-fn
                                           :id :tehtavaryhma
                                           :arvo "Tehtäväryhmä")
                          "Määrä"        (luo-paivitys-fn
                                           :id :maara
                                           :arvo "Määrä")}
        otsikot-rivi (fn [rivi]
                       (-> rivi
                           (p/aseta-arvo :id :otsikko-rivi
                                         :class #{"table-default" "table-default-header"})
                           (p/paivita-arvo :lapset
                                           (osien-paivitys-fn paivitysfunktiot))))
        kulut-rivi (fn [rivi]
                     (-> rivi
                         (p/aseta-arvo :id :kulut-rivi
                                       :class #{"table-default-even"})))]
    (muodosta-taulukko :kohdistetut-kulut-taulukko
                       {:otsikot {:janan-tyyppi jana/Rivi
                                  :osat         [osa/Teksti osa/Teksti osa/Teksti osa/Teksti osa/Teksti]}
                        :kulut   {:janan-tyyppi jana/Rivi
                                  :osat         [osa/Teksti osa/Teksti osa/Teksti osa/Teksti osa/Teksti]}}
                       ["Pvm" "Maksuerä" "Toimenpide" "Tehtäväryhmä" "Määrä"]
                       [:otsikot otsikot-rivi
                        :kulut kulut-rivi]
                       {:class                 #{}
                        :taulukon-paivitys-fn! (fn [uusi]
                                                 (loki/log "UUSI" (type uusi) uusi (->
                                                                                     tila/laskutus-kohdistetut-kulut
                                                                                     (swap! assoc-in [:taulukko] uusi)
                                                                                     :taulukko))
                                                 (->
                                                   tila/laskutus-kohdistetut-kulut
                                                   (swap! assoc-in [:taulukko] uusi)
                                                   :taulukko))})))

(defn- kohdistetut*
  [e! app]
  (komp/luo
    (komp/piirretty (fn [this]
                      (e! (tiedot/->HaeAliurakoitsijat))
                      (e! (tiedot/->HaeUrakanLaskut (select-keys (-> @tila/yleiset :urakka) [:id :alkupvm :loppupvm])))
                      (e! (tiedot/->HaeUrakanToimenpiteet (-> @tila/yleiset :urakka :id)))
                      (e! (tiedot/->LuoKulutaulukko (luo-kulutaulukko)))))
    (fn [e! {:keys [taulukko syottomoodi] :as app}]
      [:div
       (if syottomoodi
         [luo-kulumodaali e! app]
         [:div
          [debug/debug app]
          [debug/debug taulukko]
          [:button {:class    #{"nappi" "nappi-toissijainen"}
                    :disabled true} "Tallenna Excel"]
          [:button {:class    #{"nappi" "nappi-toissijainen"}
                    :disabled true} "Tallenna PDF"]
          [:button {:class    #{"nappi" "nappi-ensisijainen"}
                    :on-click #(e! (tiedot/->KulujenSyotto (not syottomoodi)))} "Uusi kulu"]
          (when taulukko
            [p/piirra-taulukko taulukko])])
       ])))

(defn kohdistetut-kulut
  []
  [tuck/tuck tila/laskutus-kohdistetut-kulut kohdistetut*])