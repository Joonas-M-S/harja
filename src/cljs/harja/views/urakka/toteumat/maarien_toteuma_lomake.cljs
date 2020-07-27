(ns harja.views.urakka.toteumat.maarien-toteuma-lomake
  (:require [tuck.core :as tuck]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.tiedot.urakka.toteumat.maarien-toteumat :as tiedot]
            [harja.ui.lomake :as ui-lomake]
            [harja.domain.toteuma :as t]
            [harja.ui.debug :as debug]
            [harja.loki :as loki]
            [harja.ui.napit :as napit]
            [reagent.core :as r]
            [harja.ui.kentat :as kentat]))


(defn- laheta! [e! data]
  (loki/log "data " data)
  (e! (tiedot/->LahetaLomake data)))

(defn- tyhjenna! [e! data]
  (loki/log "tyhjään")
  (e! (tiedot/->TyhjennaLomake data)))

(defn- maaramitattavat-toteumat
  [{:keys [e! tehtavat]} {{toteumat ::t/toteumat :as lomake} :data :as kaikki}]
  (let [paivita! (fn [polku indeksi arvo]
                   (e! (tiedot/->PaivitaLomake (assoc-in lomake [::t/toteumat indeksi polku] arvo))))]
    [:div
     (doall
       (map-indexed
         (fn [indeksi {tehtava      ::t/tehtava
                       maara        ::t/maara
                       lisatieto    ::t/lisatieto
                       sijainti     ::t/sijainti
                       ei-sijaintia ::t/ei-sijaintia
                       toteuma-id   ::t/toteuma-id
                       poistettu    ::t/poistettu
                       :as          _toteuma}]
           [(if (= 1 (count toteumat))
              :<>
              :div.row.lomakerivi.lomakepalstat)
            [(if (= 1 (count toteumat))
               :<>
               :div.lomakepalsta)
             (when (and toteuma-id
                        (not= (count toteumat) 1))
               [:div.lomakepalsta
                [:div.row.lomakerivi
                 [:label "Poista toteuma"]
                 [kentat/tee-kentta {::ui-lomake/col-luokka ""
                                     :teksti                "Pooista toteuma"
                                     :tyyppi                :checkbox}
                  (r/wrap poistettu
                          (r/partial paivita! ::t/poistettu indeksi))]]])
             [:div.row.lomakerivi
              [:label "Tehtävä"]
              [kentat/tee-kentta
               {:pakollinen?           true
                ::ui-lomake/col-luokka ""
                :vayla-tyyli?          true
                :tyyppi                :valinta
                :valinnat              tehtavat
                :valinta-nayta         :tehtava}
               (r/wrap tehtava
                       (r/partial paivita! ::t/tehtava indeksi))]]
             [:div.row.lomakerivi
              [:label "Toteutunut määrä"]
              [kentat/tee-kentta
               {::ui-lomake/col-luokka ""
                :vayla-tyyli?          true
                :pakollinen?           true
                :tyyppi                :numero}
               (r/wrap maara
                       (r/partial paivita! ::t/maara indeksi))]]
             [:div.row.lomakerivi
              [:label "Lisätieto"]
              [kentat/tee-kentta
               {::ui-lomake/col-luokka ""
                :vayla-tyyli?          true
                :pakollinen?           true
                :tyyppi                :string}
               (r/wrap lisatieto
                       (r/partial paivita! ::t/lisatieto indeksi))]]]
            (when (not= (count toteumat) 1)
              [:div.lomakepalsta
               [:div.row.lomakerivi
                [:label "Sijainti"]
                [kentat/tee-kentta
                 {::ui-lomake/col-luokka ""
                  :teksti                "Kyseiseen tehtävään ei ole sijaintia"
                  :pakollinen?           (not ei-sijaintia)
                  :disabled?             ei-sijaintia
                  :tyyppi                :tierekisteriosoite
                  :sijainti              (r/wrap sijainti (constantly true))}
                 (r/wrap sijainti
                         (r/partial paivita! ::t/sijainti indeksi))]]
               [:div.row.lomakerivi
                [kentat/tee-kentta
                 {::ui-lomake/col-luokka ""
                  :teksti                "Kyseiseen tehtävään ei ole sijaintia"
                  :tyyppi                :checkbox}
                 (r/wrap ei-sijaintia
                         (r/partial paivita! ::t/ei-sijaintia indeksi))]]])])
         toteumat))
     (when (> (count toteumat) 1)
       [napit/tallenna
        "Lisää tehtävä"
        #(e! (tiedot/->LisaaToteuma lomake))
        {:ikoni         [harja.ui.ikonit/plus-sign]
         :vayla-tyyli?  true
         :teksti-nappi? true}])]))

(defn- maarien-toteuman-syottolomake*
  [e! {lomake :lomake toimenpiteet :toimenpiteet tehtavat :tehtavat :as app}]
  (let [{tyyppi   ::t/tyyppi
         toteumat ::t/toteumat} lomake
        {ei-sijaintia ::t/ei-sijaintia
         toteuma-id   ::t/toteuma-id
         sijainti     ::t/sijainti} (-> toteumat first)
        laheta-lomake! (r/partial laheta! e!)
        tyhjenna-lomake! (r/partial tyhjenna! e!)
        maaramitattava [{:otsikko               "Työ valmis"
                         :nimi                  ::t/pvm
                         ::ui-lomake/col-luokka ""
                         :pakollinen?           true
                         :tyyppi                :pvm}
                        {:nimi                  ::t/toteumat
                         ::ui-lomake/col-luokka ""
                         :tyyppi                :komponentti
                         :komponentti           (r/partial maaramitattavat-toteumat {:e!           e!
                                                                                     :toimenpiteet toimenpiteet
                                                                                     :tehtavat     tehtavat})}]
        lisatyo [{:otsikko               "Pvm"
                  :nimi                  ::t/pvm
                  ::ui-lomake/col-luokka ""
                  :pakollinen?           true
                  :tyyppi                :pvm}
                 {:otsikko               "Tehtävä"
                  :nimi                  ::t/tehtava
                  :pakollinen?           true
                  ::ui-lomake/col-luokka ""
                  :tyyppi                :valinta
                  :valinta-nayta         :tehtava
                  :valinnat              tehtavat}
                 {:otsikko               "Kuvaus"
                  ::ui-lomake/col-luokka ""
                  :nimi                  ::t/lisatieto
                  :pakollinen?           false
                  :tyyppi                :string}]
        akilliset-ja-korjaukset [{:otsikko               "Pvm"
                                  :nimi                  ::t/pvm
                                  ::ui-lomake/col-luokka ""
                                  :pakollinen?           true
                                  :tyyppi                :pvm}
                                 {:otsikko               "Tehtävä"
                                  :nimi                  ::t/tehtava
                                  :pakollinen?           true
                                  ::ui-lomake/col-luokka ""
                                  :tyyppi                :valinta
                                  :valinnat              tehtavat
                                  :valinta-nayta         :tehtava}
                                 {:otsikko               "Kuvaus"
                                  ::ui-lomake/col-luokka ""
                                  :nimi                  ::t/lisatieto
                                  :pakollinen?           false
                                  :tyyppi                :string}]]
    [:div#vayla
     [debug/debug app]
     [debug/debug lomake]
     [ui-lomake/lomake
      {:muokkaa!     (fn [data]
                       (loki/log "dataa " data)
                       (e! (tiedot/->PaivitaLomake data)))
       :voi-muokata? true
       :palstoja     2
       :header-fn    (fn [data]
                       [:div.flex-row
                        [napit/takaisin "Takaisin" #(tyhjenna-lomake! nil) {:vayla-tyyli? true :teksti-nappi? true}]])
       :footer-fn    (fn [data]
                       [:div.flex-row.alkuun
                        [napit/tallenna
                         "Tallenna"
                         #(laheta-lomake! data)
                         {:vayla-tyyli? true
                          :luokka       "suuri"}]
                        [napit/peruuta
                         "Peruuta"
                         #(tyhjenna-lomake! data)
                         {:vayla-tyyli? true
                          :luokka       "suuri"}]])
       :vayla-tyyli? true}
      [(when (and toteuma-id
                  (= (count toteumat) 1))
         {:tyyppi  :checkbox
          :nimi    [::t/toteumat 0 ::t/poistettu]
          :otsikko "Poista toteuma"})
       (ui-lomake/palstat
         {}
         {:otsikko "Mihin toimenpiteeseen työ liittyy?"}
         [{:otsikko               "Toimenpide"
           :nimi                  ::t/toimenpide
           ::ui-lomake/col-luokka ""
           :pakollinen?           true
           :valinnat              toimenpiteet
           :valinta-nayta         :otsikko
           :tyyppi                :valinta}])
       {:tyyppi           :radio-group
        :nimi             ::t/tyyppi
        :oletusarvo       :maaramitattava
        :otsikko          ""
        :vaihtoehdot      [:maaramitattava :akillinen-hoitotyo :lisatyo]
        :pakollinen?      true
        :nayta-rivina?    true
        :palstoja         2
        :vaihtoehto-nayta {:maaramitattava     "Määrämitattava tehtävä"
                           :akillinen-hoitotyo "Äkillinen hoitotyö, vahingon korjaus, rahavaraus"
                           :lisatyo            "Lisätyö"}}
       {:tyyppi    :checkbox
        :nimi      ::t/useampi-toteuma
        :disabled? (not= tyyppi :maaramitattava)
        :teksti    "Haluan syöttää useamman toteuman tälle toimenpiteelle"}
       (ui-lomake/palstat
         {}
         {:otsikko "Tehtävän tiedot"}
         (case tyyppi
           :maaramitattava maaramitattava
           :lisatyo lisatyo
           (:vahinkojen-korjaukset :tilaajan-varaukset :akillinen-hoitotyo) akilliset-ja-korjaukset
           [])
         (when (= (count toteumat) 1)
           {:otsikko "Sijainti *"})
         (when (= (count toteumat) 1)
           [{:nimi                  [::t/toteumat 0 ::t/sijainti]
             ::ui-lomake/col-luokka ""
             :teksti                "Kyseiseen tehtävään ei ole sijaintia"
             :pakollinen?           (not ei-sijaintia)
             :disabled?             ei-sijaintia
             :tyyppi                :tierekisteriosoite
             :sijainti              (r/wrap sijainti
                                            (constantly true)) ; lomake päivittyy eri funkkarilla, niin never mind this, mutta annetaan sijainti silti
             }
            {:nimi                  [::t/toteumat 0 ::t/ei-sijaintia]
             ::ui-lomake/col-luokka ""
             :teksti                "Kyseiseen tehtävään ei ole sijaintia"
             :tyyppi                :checkbox}]))]
      lomake]]))

(defn akilliset-hoitotyot
  []
  [tuck/tuck tila/toteumat-maarat maarien-toteuman-syottolomake*])