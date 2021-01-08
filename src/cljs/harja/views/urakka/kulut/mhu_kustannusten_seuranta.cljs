(ns harja.views.urakka.kulut.mhu-kustannusten-seuranta
  "Urakan 'Toteumat' välilehden Määrien toteumat osio"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<! >! chan]]
            [cljs.core.async :refer [<! timeout]]
            [cljs-time.core :as t]
            [clojure.string :as str]
            [tuck.core :as tuck]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.ui.debug :as debug]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.ui.yleiset :refer [ajax-loader linkki livi-pudotusvalikko +korostuksen-kesto+]]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.komponentti :as komp]
            [harja.transit :as transit]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.tiedot.urakka.kulut.mhu-kustannusten-seuranta :as kustannusten-seuranta-tiedot]
            [harja.domain.kulut.kustannusten-seuranta :as kustannusten-seuranta]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.tyokalut.big :as big])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))

(defn- muotoile-prosentti
  "Olettaa saavansa molemmat parametrit big arvoina."
  [toteuma suunniteltu negatiivinen?]
  (if (or (nil? toteuma)
          (nil? suunniteltu)
          (big/eq (big/->big 0) toteuma)
          (big/eq (big/->big 0) suunniteltu))
    [:span 0]
    [:span (when negatiivinen?
             {:class "pilleri"})
     (big/fmt (big/mul (big/->big 100) (big/div toteuma suunniteltu)) 2)]))

; spekseistä laskettu
(def leveydet {:caret-paaryhma "2%"
               :paaryhma-vari "2%"
               :tehtava "41%"
               :budjetoitu "15%"
               :toteuma "15%"
               :erotus "15%"
               :prosentti "10%"})

(def row-index-atom (r/atom 0))

;; Formatoidaan näytölle arvo ja käännetään se big decimaaliksi
(defn fmt->big
  ([arvo] (fmt->big arvo false))
  ([arvo on-big?]
   (let [arvo (if on-big?
                arvo
                (big/->big arvo))
         fmt-arvo (harja.fmt/desimaaliluku (or (:b arvo) 0) 2 true)]
     fmt-arvo)))

(defn- lisaa-taulukkoon-tehtava-rivi [nimi toteuma]
  [:tr.bottom-border {:key (str (hash nimi) "-" (hash toteuma))}
   [:td.paaryhma-center {:style {:width (:caret-paaryhma leveydet)}}]
   [:td.paaryhma-center {:style {:width (:paaryhma-vari leveydet)}}]
   [:td {:style {:width (:tehtava leveydet)}} nimi]
   [:td.numero {:style {:width (:budjetoitu leveydet)}}]
   [:td.numero {:style {:width (:toteuma leveydet)}} toteuma]
   [:td.numero {:style {:width (:erotus leveydet)}}]
   [:td.numero {:style {:width (:prosentti leveydet)}}]])

(defn- rivita-lisatyot [e! app lisatyot]
  (for [l lisatyot]
    ^{:key (str (hash l))}
    (lisaa-taulukkoon-tehtava-rivi (or (:tehtava_nimi l) (:toimenpidekoodi_nimi l))
                                   (fmt->big (:toteutunut_summa l) false))))

(defn- kokoa-toimenpiteen-alle [toimenpide tehtavat toimenpideryhma yht-toteuma]
  (let [row-index (r/atom 0)]
    (concat
      (when (and (= "hankintakustannukset" (:paaryhma toimenpide))
                 (> (count tehtavat) 0))
        [^{:key (str toimenpideryhma "-" (hash toimenpide) "-" (hash tehtavat))}
         (lisaa-taulukkoon-tehtava-rivi [:span {:style {:padding-left "8px" :font-weight "bold"}} toimenpideryhma]
                                        (fmt->big yht-toteuma false))])
      (when (and
              (= "hankintakustannukset" (:paaryhma toimenpide))
              (> (count tehtavat) 0))
        (mapcat
          (fn [rivi]
            (let [toteutunut-summa (big/->big (or (:toteutunut_summa rivi) 0))
                  _ (reset! row-index (inc @row-index))]
              (concat
                [^{:key (str @row-index "-" toimenpideryhma "-" (hash rivi))}
                 (lisaa-taulukkoon-tehtava-rivi [:span {:style {:padding-left "16px"}} (:tehtava_nimi rivi)]
                                                (fmt->big toteutunut-summa false))])))
          tehtavat)))))

(defn- listaa-pelkat-tehtavat [toimenpide tehtavat]
  (when tehtavat
    (mapcat
      (fn [rivi]
        (let [toteutunut-summa (big/->big (or (:toteutunut_summa rivi) 0))]
          (concat
            [^{:key (str toimenpide "-" (hash rivi))}
             (lisaa-taulukkoon-tehtava-rivi [:span {:style {:padding-left "16px"}} (:tehtava_nimi rivi)]
                                            (fmt->big toteutunut-summa false))])))
      tehtavat)))

(defn- rivita-toimenpiteet-paaryhmalle [e! app toimenpiteet]
  (map
    (fn [toimenpide]
      (let [hankinta-tehtavat (filter #(= "hankinta" (:toimenpideryhma %)) (:tehtavat toimenpide))
            hankinta-toteuma (reduce (fn [summa rivi]
                                       (+ (or summa 0) (or (:toteutunut_summa rivi) 0)))
                                     0
                                     hankinta-tehtavat)
            vahinkojenkorvaus-tehtavat (filter #(= "vahinkojen-korjaukset" (:toimenpideryhma %)) (:tehtavat toimenpide))
            vahinko-toteuma (reduce (fn [summa rivi]
                                      (+ (or summa 0) (or (:toteutunut_summa rivi) 0)))
                                    0
                                    vahinkojenkorvaus-tehtavat)
            akilliset-tehtavat (filter #(= "akillinen-hoitotyo" (:toimenpideryhma %)) (:tehtavat toimenpide))
            akilliset-toteumat (reduce (fn [summa rivi]
                                         (+ (or summa 0) (or (:toteutunut_summa rivi) 0)))
                                       0
                                       akilliset-tehtavat)
            ;muut-tehtavat (filter #(= "muut-rahavaraukset" (:toimenpideryhma %)) (:tehtavat toimenpide))
            tilaajan-rahavaraus-tehtavat (filter #(= "tilaajan-rahavaraus" (:toimenpideryhma %)) (:tehtavat toimenpide))
            raha-toteumat (reduce (fn [summa rivi]
                                    (+ (or summa 0) (or (:toteutunut_summa rivi) 0)))
                                  0
                                  tilaajan-rahavaraus-tehtavat)
            toimistokulu-tehtavat (filter #(= "toimistokulut" (:toimenpideryhma %)) (:tehtavat toimenpide))
            negatiivinen? (big/gt (big/->big (or (:toimenpide-toteutunut-summa toimenpide) 0))
                                  (big/->big (or (:toimenpide-budjetoitu-summa toimenpide) 0)))

            muodostetut-tehtavat (if-not (= (get-in app [:valittu-rivi :toimenpide]) toimenpide)
                                   nil
                                   (concat
                                     (listaa-pelkat-tehtavat toimenpide toimistokulu-tehtavat)
                                     (kokoa-toimenpiteen-alle toimenpide hankinta-tehtavat "Hankinnat" hankinta-toteuma)
                                     (kokoa-toimenpiteen-alle toimenpide vahinkojenkorvaus-tehtavat "Vahinkojen korjaukset" vahinko-toteuma)
                                     (kokoa-toimenpiteen-alle toimenpide akilliset-tehtavat "Äkilliset hoitotyöt" akilliset-toteumat)
                                     (kokoa-toimenpiteen-alle toimenpide tilaajan-rahavaraus-tehtavat "Tilaajan rahavaraukset" raha-toteumat)))]
        (doall (concat [^{:key (str "otsikko-" (hash toimenpide) "-" (hash toimenpiteet))}
                        [:tr.bottom-border
                         (merge
                           (when (> (count (:tehtavat toimenpide)) 0)
                             {:class "selectable"
                              :on-click #(e! (kustannusten-seuranta-tiedot/->AvaaRivi :toimenpide toimenpide))}))
                         [:td.paaryhma-center {:style {:width (:caret-paaryhma leveydet)}}]
                         [:td.paaryhma-center {:style {:width (:paaryhma-vari leveydet)}}
                          (when (> (count (:tehtavat toimenpide)) 0)
                            (if (= (get-in app [:valittu-rivi :toimenpide]) toimenpide)
                              [:img {:alt "Expander" :src "images/expander-down.svg"}]
                              [:img {:alt "Expander" :src "images/expander.svg"}]))]
                         [:td {:style {:width (:tehtava leveydet)}} (:toimenpide toimenpide)]
                         [:td.numero {:style {:width (:budjetoitu leveydet)}} (fmt->big (:toimenpide-budjetoitu-summa toimenpide))]
                         [:td.numero {:style {:width (:toteuma leveydet)}} (fmt->big (:toimenpide-toteutunut-summa toimenpide))]
                         [:td {:class (if negatiivinen? "negatiivinen-numero" "numero")
                               :style {:width (:erotus leveydet)}} (str (when negatiivinen? "+ ") (fmt->big (- (:toimenpide-toteutunut-summa toimenpide)
                                                                                                               (:toimenpide-budjetoitu-summa toimenpide))))]
                         [:td {:class (if negatiivinen? "negatiivinen-numero" "numero")
                               :style {:width (:prosentti leveydet)}} (muotoile-prosentti
                                                                        (big/->big (or (:toimenpide-toteutunut-summa toimenpide) 0))
                                                                        (big/->big (or (:toimenpide-budjetoitu-summa toimenpide) 0))
                                                                        negatiivinen?)]]]
                       muodostetut-tehtavat))))
    toimenpiteet))

(defn- paaryhma-taulukkoon [e! app paaryhma paaryhma-avain toimenpiteet negatiivinen? budjetoitu toteutunut erotus prosentti]
  (let [row-index (r/atom 0)]
    (doall (concat
             [^{:key (str paaryhma "-" (hash toimenpiteet))}
              [:tr.bottom-border.selectable {:on-click #(e! (kustannusten-seuranta-tiedot/->AvaaRivi :paaryhma paaryhma-avain))
                                             :key paaryhma}
               [:td.paaryhma-center {:style {:width (:caret-paaryhma leveydet)}}
                (if
                  (= paaryhma-avain (get-in app [:valittu-rivi :paaryhma]))
                  [:img {:alt "Expander" :src "images/expander-down.svg"}]
                  (when (> (count toimenpiteet) 0)
                    [:img {:alt "Expander" :src "images/expander.svg"}]))]
               [:td.paaryhma-center {:style {:width (:paaryhma-vari leveydet)}}]
               [:td {:style {:width (:tehtava leveydet)
                             :font-weight "700"}} paaryhma]
               [:td.numero {:style {:width (:budjetoitu leveydet)}} budjetoitu]
               [:td.numero {:style {:width (:toteuma leveydet)}} toteutunut]
               [:td {:class (if negatiivinen? "negatiivinen-numero" "numero")
                     :style {:width (:erotus leveydet)}} (str (when negatiivinen? "+ ") erotus)]
               [:td {:class (if negatiivinen? "negatiivinen-numero" "numero")
                     :style {:width (:prosentti leveydet)}} prosentti]]]

             (when (= paaryhma-avain (get-in app [:valittu-rivi :paaryhma]))
               (mapcat (fn [rivi]
                         (let [_ (reset! row-index (inc @row-index))]
                           [^{:key (str @row-index "-" (hash rivi))}
                            rivi]))
                       toimenpiteet))))))

(defn- kustannukset-taulukko [e! app rivit-paaryhmittain]
  (let [hankintakustannusten-toimenpiteet (rivita-toimenpiteet-paaryhmalle e! app (:hankintakustannukset rivit-paaryhmittain))
        hankintakustannukset-negatiivinen? (big/gt (big/->big (or (:hankintakustannukset-toteutunut rivit-paaryhmittain) 0))
                                                   (big/->big (or (:hankintakustannukset-budjetoitu rivit-paaryhmittain) 0)))
        hallintakorvaus-negatiivinen? (big/gt (big/->big (or (:johto-ja-hallintakorvaus-toteutunut rivit-paaryhmittain) 0))
                                              (big/->big (or (:johto-ja-hallintakorvaus-budjetoitu rivit-paaryhmittain) 0)))
        hoidonjohdonpalkkio-negatiivinen? (big/gt (big/->big (or (:hoidonjohdonpalkkio-toteutunut rivit-paaryhmittain) 0))
                                                  (big/->big (or (:hoidonjohdonpalkkio-budjetoitu rivit-paaryhmittain) 0)))
        erillishankinnat-negatiivinen? (big/gt (big/->big (or (:erillishankinnat-toteutunut rivit-paaryhmittain) 0))
                                               (big/->big (or (:erillishankinnat-budjetoitu rivit-paaryhmittain) 0)))
        yht-negatiivinen? (big/gt (big/->big (or (get-in app [:kustannukset-yhteensa :yht-toteutunut-summa]) 0))
                                  (big/->big (or (get-in app [:kustannukset-yhteensa :yht-budjetoitu-summa]) 0)))
        jjhk-toimenpiteet (rivita-toimenpiteet-paaryhmalle e! app (:johto-ja-hallintakorvaus rivit-paaryhmittain))
        lisatyot (rivita-lisatyot e! app (:lisatyot rivit-paaryhmittain))
        valittu-hoitokauden-alkuvuosi (:hoitokauden-alkuvuosi app)
        valittu-hoitovuosi-nro (kustannusten-seuranta-tiedot/hoitokauden-jarjestysnumero valittu-hoitokauden-alkuvuosi)
        hoitovuosi-nro-menossa (kustannusten-seuranta-tiedot/kuluva-hoitokausi-nro (pvm/nyt))
        hoitovuotta-jaljella (if (= valittu-hoitovuosi-nro hoitovuosi-nro-menossa)
                               (pvm/montako-paivaa-valissa
                                 (pvm/nyt)
                                 (pvm/->pvm (str "30.09." (inc valittu-hoitokauden-alkuvuosi))))
                               nil)]
    [:div.col-xs-12 {:style {:padding-top "24px"}}
     [:div
      [:h4 "Hoitovuosi: " valittu-hoitovuosi-nro " (1.10." valittu-hoitokauden-alkuvuosi " - 09.30." (inc valittu-hoitokauden-alkuvuosi) ")"]
      (when hoitovuotta-jaljella
        [:span "Hoitovuotta on jäljellä " hoitovuotta-jaljella " päivää."])]
     [:div.table-default {:style {:padding-top "24px"}}
      [:table.table-default-header-valkoinen
       [:thead
        [:tr.bottom-border {:style {:text-transform "uppercase"}}
         [:th.paaryhma-center {:style {:width (:caret-paaryhma leveydet)}}]
         [:th.paaryhma-center {:style {:width (:paaryhma-vari leveydet)}}]
         [:th {:style {:width (:tehtava leveydet)}} "Toimenpide"]
         [:th {:style {:width (:budjetoitu leveydet) :text-align "right"}} "Budjetti €"]
         [:th {:style {:width (:toteuma leveydet) :text-align "right"}} "Toteuma €"]
         [:th {:style {:width (:erotus leveydet) :text-align "right"}} "Erotus €"]
         [:th {:style {:width (:prosentti leveydet) :text-align "right"}} "%"]]]
       [:tbody
        (paaryhma-taulukkoon e! app "Hankintakustannukset" :hankintakustannukset
                             hankintakustannusten-toimenpiteet hankintakustannukset-negatiivinen?
                             (fmt->big (:hankintakustannukset-budjetoitu rivit-paaryhmittain))
                             (fmt->big (:hankintakustannukset-toteutunut rivit-paaryhmittain))
                             (fmt->big (- (:hankintakustannukset-toteutunut rivit-paaryhmittain)
                                          (:hankintakustannukset-budjetoitu rivit-paaryhmittain)))
                             (muotoile-prosentti
                               (big/->big (or (:hankintakustannukset-toteutunut rivit-paaryhmittain) 0))
                               (big/->big (or (:hankintakustannukset-budjetoitu rivit-paaryhmittain) 0))
                               hankintakustannukset-negatiivinen?))
        (paaryhma-taulukkoon e! app "Johto- ja hallintokorvaukset" :johto-ja-hallintakorvaus
                             jjhk-toimenpiteet hallintakorvaus-negatiivinen?
                             (fmt->big (:johto-ja-hallintakorvaus-budjetoitu rivit-paaryhmittain))
                             (fmt->big (:johto-ja-hallintakorvaus-toteutunut rivit-paaryhmittain))
                             (fmt->big (- (:johto-ja-hallintakorvaus-toteutunut rivit-paaryhmittain)
                                          (:johto-ja-hallintakorvaus-budjetoitu rivit-paaryhmittain)))
                             (muotoile-prosentti
                               (big/->big (or (:johto-ja-hallintakorvaus-toteutunut rivit-paaryhmittain) 0))
                               (big/->big (or (:johto-ja-hallintakorvaus-budjetoitu rivit-paaryhmittain) 0))
                               hallintakorvaus-negatiivinen?))
        (paaryhma-taulukkoon e! app "Hoidonjohdonpalkkio" :hoidonjohdonpalkkio
                             nil hoidonjohdonpalkkio-negatiivinen?
                             (fmt->big (:hoidonjohdonpalkkio-budjetoitu rivit-paaryhmittain))
                             (fmt->big (:hoidonjohdonpalkkio-toteutunut rivit-paaryhmittain))
                             (fmt->big (- (:hoidonjohdonpalkkio-toteutunut rivit-paaryhmittain)
                                          (:hoidonjohdonpalkkio-budjetoitu rivit-paaryhmittain)))
                             (muotoile-prosentti
                               (big/->big (or (:hoidonjohdonpalkkio-toteutunut rivit-paaryhmittain) 0))
                               (big/->big (or (:hoidonjohdonpalkkio-budjetoitu rivit-paaryhmittain) 0))
                               hoidonjohdonpalkkio-negatiivinen?))
        (paaryhma-taulukkoon e! app "Erillishankinnat" :erillishankinnat
                             nil erillishankinnat-negatiivinen?
                             (fmt->big (:erillishankinnat-budjetoitu rivit-paaryhmittain))
                             (fmt->big (:erillishankinnat-toteutunut rivit-paaryhmittain))
                             (fmt->big (- (:erillishankinnat-toteutunut rivit-paaryhmittain)
                                          (:erillishankinnat-budjetoitu rivit-paaryhmittain)))
                             (muotoile-prosentti
                               (big/->big (or (:erillishankinnat-toteutunut rivit-paaryhmittain) 0))
                               (big/->big (or (:erillishankinnat-budjetoitu rivit-paaryhmittain) 0))
                               erillishankinnat-negatiivinen?))
        ; Näytä yhteensä rivi
        [:tr.bottom-border
         [:td.paaryhma-center {:style {:width (:caret-paaryhma leveydet)}}]
         [:td.paaryhma-center {:style {:width (:paaryhma-vari leveydet)}}]
         [:td {:style {:width (:tehtava leveydet)
                       :font-weight "700"}}
          (get-in app [:kustannukset-yhteensa :toimenpide])]

         [:td.numero {:style {:width (:budjetoitu leveydet)}} (fmt->big (get-in app [:kustannukset-yhteensa :yht-budjetoitu-summa]))]
         [:td.numero {:style {:width (:toteuma leveydet)}} (fmt->big (get-in app [:kustannukset-yhteensa :yht-toteutunut-summa]))]
         [:td {:class (if yht-negatiivinen? "negatiivinen-numero" "numero")
               :style {:width (:erotus leveydet)}} (str (when yht-negatiivinen? "+ ") (fmt->big (- (get-in app [:kustannukset-yhteensa :yht-toteutunut-summa])
                                                                                                   (get-in app [:kustannukset-yhteensa :yht-budjetoitu-summa]))))]
         [:td {:class (if yht-negatiivinen? "negatiivinen-numero" "numero")
               :style {:width (:prosentti leveydet)}} (muotoile-prosentti
                                                        (big/->big (or (get-in app [:kustannukset-yhteensa :yht-toteutunut-summa]) 0))
                                                        (big/->big (or (get-in app [:kustannukset-yhteensa :yht-budjetoitu-summa]) 0))
                                                        yht-negatiivinen?)]]]]
      ;; Lisätyöt
      [:table.table-default-header-valkoinen {:style {:margin-top "32px"}}
       [:tbody
        [:tr.bottom-border.selectable {:key "Lisätyöt"
                                       :on-click #(e! (kustannusten-seuranta-tiedot/->AvaaRivi :paaryhma :lisatyot))}
         [:td.paaryhma-center {:style {:width (:caret-paaryhma leveydet)}}
          (if (= :lisatyot (get-in app [:valittu-rivi :paaryhma]))
            [:img {:alt "Expander" :src "images/expander-down.svg"}]
            (when (> (count lisatyot) 0)
              [:img {:alt "Expander" :src "images/expander.svg"}]))]
         [:td.paaryhma-center {:style {:width (:paaryhma-vari leveydet)}}]
         [:td {:style {:width (:tehtava leveydet) :font-weight "700"}} "Lisätyöt"]
         [:td.numero {:style {:width (:budjetoitu leveydet)}}]
         [:td.numero {:style {:width (:toteuma leveydet)}} (fmt->big (:lisatyot-summa rivit-paaryhmittain))]
         [:td {:style {:width (:erotus leveydet)}}]
         [:td {:style {:width (:prosentti leveydet)}}]]
        (when (= :lisatyot (get-in app [:valittu-rivi :paaryhma]))
          (doall
            (for [l lisatyot]
              ^{:key (hash l)}
              l)))]]]]))

(defn yhteenveto-laatikko [e! app data]
  (let [valittu-hoitokauden-alkuvuosi (:hoitokauden-alkuvuosi app)
        valittu-hoitovuosi-nro (kustannusten-seuranta-tiedot/hoitokauden-jarjestysnumero valittu-hoitokauden-alkuvuosi)
        tavoitehinta (big/->big (or (kustannusten-seuranta-tiedot/hoitokauden-tavoitehinta valittu-hoitovuosi-nro app) 0))
        kattohinta (big/->big (or (kustannusten-seuranta-tiedot/hoitokauden-kattohinta valittu-hoitovuosi-nro app) 0))
        toteuma (big/->big (or (get-in app [:kustannukset-yhteensa :yht-toteutunut-summa]) 0))]
    [:div.col-xs-12
     [:div.yhteenveto
      [:div.header [:span "Yhteenveto"]]
      [:div.row [:span "Tavoitehinta: "] [:span.pull-right (fmt->big tavoitehinta true)]]
      (when (big/gt toteuma tavoitehinta)
        [:div.row [:span "Tavoitehinnan ylitys: "]
         [:span.negatiivinen-numero.pull-right
          (str "+ " (fmt->big (big/minus toteuma tavoitehinta)))]])
      [:div.row [:span "Kattohinta: "] [:span.pull-right (fmt->big kattohinta true)]]
      (when (big/gt toteuma kattohinta)
        [:div.row [:span "Kattohinnan ylitys: "]
         [:span.negatiivinen-numero.pull-right
          (str "+ " (fmt->big (big/minus toteuma kattohinta)))]])
      [:div.row [:span "Toteuma: "] [:span.pull-right (fmt->big toteuma true)]]

      [:div.row [:span "Lisätyöt: "] [:span.pull-right (fmt->big (:lisatyot-summa data) false)]]]]))

(defn kustannukset
  "Kustannukset listattuna taulukkoon"
  [e! app]
  (let [{:keys [alkupvm]} (-> @tila/tila :yleiset :urakka)  ;; Ota urakan alkamis päivä
        vuosi (pvm/vuosi alkupvm)
        hoitokaudet (into [] (range vuosi (+ 5 vuosi)))
        taulukon-rivit (:kustannukset app)
        valittu-hoitokausi (if (nil? (get-in app [:hoitokauden-alkuvuosi]))
                             2019
                             (get-in app [:hoitokauden-alkuvuosi]))
        valittu-kuukausi (:valittu-kuukausi app)
        hoitokauden-kuukaudet (pvm/aikavalin-kuukausivalit
                                [(pvm/->pvm (str "01.10." valittu-hoitokausi))
                                 (pvm/->pvm (str "30.09." (inc valittu-hoitokausi)))])
        haun-alkupvm (if valittu-kuukausi
                       (first valittu-kuukausi)
                       (str valittu-hoitokausi "-10-01"))
        haun-loppupvm (if valittu-kuukausi
                        (second valittu-kuukausi)
                        (str (inc valittu-hoitokausi) "-09-30"))]
    [:div.kustannusten-seuranta
     [debug/debug app]
     [:div
      [:div.col-xs-12.header {:style {:padding-top "1rem"}}
       [:h1 "Kustannusten seuranta"]
       [:p.urakka (:nimi @nav/valittu-urakka)]
       [:p "Tavoite- ja kattohinnat sekä budjetit on suunniteltu Suunnittelu-puolella.
     Toteutumissa näkyy ne kustannukset, jotka ovat Laskutus-osiossa syötetty järjestelmään."]]
      [:div.row {:style {:padding-top "24px"}}
       [:div.col-xs-6.col-md-3 {:style {:height "61px"}}
        [:span.alasvedon-otsikko "Hoitokausi"]
        [yleiset/livi-pudotusvalikko {:valinta valittu-hoitokausi
                                      :vayla-tyyli? true
                                      :valitse-fn #(e! (kustannusten-seuranta-tiedot/->ValitseHoitokausi (:id @nav/valittu-urakka) %))
                                      :format-fn #(str "1.10." % "-30.9." (inc %))
                                      :klikattu-ulkopuolelle-params {:tarkista-komponentti? true}}
         hoitokaudet]]
       [:div.col-xs-6.col-md-3 {:style {:height "61px"}}
        [:span.alasvedon-otsikko "Kuukausi"]
        [yleiset/livi-pudotusvalikko {:valinta valittu-kuukausi
                                      :vayla-tyyli? true
                                      :valitse-fn #(e! (kustannusten-seuranta-tiedot/->ValitseKuukausi (:id @nav/valittu-urakka) % valittu-hoitokausi))
                                      :format-fn #(if %
                                                    (let [[alkupvm _] %
                                                          kk-teksti (pvm/kuukauden-nimi (pvm/kuukausi alkupvm))]
                                                      (str (str/capitalize kk-teksti) " " (pvm/vuosi alkupvm)))
                                                    "Koko hoitokausi")
                                      :klikattu-ulkopuolelle-params {:tarkista-komponentti? true}}
         hoitokauden-kuukaudet]]
       [:div.col-xs-6.col-md-3 {:style {:height "61px" :padding-top "21px"}}
        ^{:key "raporttixls"}
        [:form {:style {:margin-left "auto"}
                :target "_blank" :method "POST"
                :action (k/excel-url :kustannukset)}
         [:input {:type "hidden" :name "parametrit"
                  :value (transit/clj->transit {:urakka-id (:id @nav/valittu-urakka)
                                                :urakka-nimi (:nimi @nav/valittu-urakka)
                                                :hoitokauden-alkuvuosi valittu-hoitokausi
                                                :alkupvm haun-alkupvm
                                                :loppupvm haun-loppupvm})}]
         [:button {:type "submit"
                   :class #{"button-secondary-default" "suuri"}} "Tallenna Excel"]]]]]

     [kustannukset-taulukko e! app taulukon-rivit]
     [yhteenveto-laatikko e! app taulukon-rivit]]))

(defn kustannusten-seuranta* [e! app]
  (komp/luo
    (komp/lippu tila/kustannusten-seuranta-nakymassa?)
    (komp/piirretty (fn [this]
                      (do
                        (e! (kustannusten-seuranta-tiedot/->HaeBudjettitavoite))
                        (e! (kustannusten-seuranta-tiedot/->HaeKustannukset (:hoitokauden-alkuvuosi app)
                                                                            nil nil)))))
    (fn [e! app]
      [:div {:id "vayla"}
       [:div
        [kustannukset e! app]]])))

(defn kustannusten-seuranta []
  (tuck/tuck tila/kustannusten-seuranta kustannusten-seuranta*))