(ns harja.views.tilannekuva.tilannekuva
  (:require [reagent.core :refer [atom]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.tilannekuva.tilannekuva :as tiedot]
            [harja.views.kartta :as kartta]
            [harja.loki :refer [log tarkkaile!]]
            [harja.views.tilannekuva.tilannekuva-popupit :as popupit]
            [harja.ui.kentat :as kentat]
            [reagent.core :as r]
            [harja.pvm :as pvm]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.on-off-valinta :as on-off]
            [harja.ui.yleiset :as yleiset])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defn pudotusvalikon-elementti [[avain valittu?]]
  ^{:key (str "pudotusvalikon-asia-" (get tiedot/suodattimien-nimet avain))}
  [:li.tk-pudotusvalikon-listan-elementti
   [:div.tk-checkbox
    [:label
     [:input {:class   "checkbox"
              :checked valittu?}]
     (get tiedot/suodattimien-nimet avain)]]])

(defn pudotusvalikko [otsikko elementit]
  (let [auki? (atom false)]
    [:div
     [:div.tk-pudotusvalikko-nappi {:on-click #(swap! auki? not)}
      [:span.tk-pudotusvalikko-tila (if @auki? (ikonit/chevron-down) (ikonit/chevron-right))]
      [:div.tk-pudotusvalikko-checkbox
       [:label
        [:input {:type    "checkbox"
                 :checked true}]
        otsikko]]]

     [:ul.tk-pudotusvalikon-lista (if @auki? {:class "tk-pudotusvalikko-auki"} {:class "tk-pudotusvalikko-kiinni"})
      (doall (for [elementti elementit]
               [pudotusvalikon-elementti elementti]))]]))

;; TODO (reset! tiedot/valitun-aikasuodattimen-arvo tunnit)
(defn nykytilanteen-aikasuodattimen-elementti [[teksti]]
  ^{:key (str "nykytilanteen_aikasuodatin_" teksti)}
  [:div.tk-nykytilanne-aikavalitsin
   [:div.tk-radio
    [:label
     [:input {:type    "radio"
              :checked false}]
     teksti]]])

(defn nykytilanteen-aikavalinta []
  (let [aikavalinnat-hiccup (map
                              (fn [aika]
                                [nykytilanteen-aikasuodattimen-elementti aika])
                              tiedot/aikasuodatin-tunteina)]
  [:div#tk-nykytilanteen-aikavalinta
   [:div.tk-nykytilanteen-aikavalinta-ryhma-tunnit
    (nth aikavalinnat-hiccup 0)
    (nth aikavalinnat-hiccup 1)
    (nth aikavalinnat-hiccup 2)]
   [:div.tk-nykytilanteen-aikavalinta-ryhma-vuorokaudet
    (nth aikavalinnat-hiccup 3)
    (nth aikavalinnat-hiccup 4)
    (nth aikavalinnat-hiccup 5)]
   [:div.tk-nykytilanteen-aikavalinta-ryhma-viikot
    (nth aikavalinnat-hiccup 6)
    (nth aikavalinnat-hiccup 7)
    (nth aikavalinnat-hiccup 8)]]))

(defn nykytilanteen-suodattimet []
  [:div#tk-nykytila-paavalikko
   [:p "Näytä seuraavat aikavälillä:"]
   [nykytilanteen-aikavalinta]
   [pudotusvalikko "Talvihoitotyöt" (:talvi @tiedot/suodattimet)]
   [pudotusvalikko "Kesähoitotyöt" (:kesa @tiedot/suodattimet)]
   [pudotusvalikko "Laadunseuranta" (:laadunseuranta @tiedot/suodattimet)]])

(defn tilan-vaihtaja []
  (let [on-off-tila (atom false)]
  [:div#tk-tilan-vaihtajat
   [:div.tk-tilan-vaihto-nykytilanne "Nykytilanne"]
   [:div.tk-tilan-vaihto-historia "Historia"]
   [on-off/on-off-valinta on-off-tila {:luokka "on-off-tilannekuva"
                                       :on-change (fn []
                                                    (if @on-off-tila
                                                      (reset! tiedot/valittu-tila :nykytilanne)
                                                      (reset! tiedot/valittu-tila :historiakuva)))}]]))

(defonce suodattimet
         [:span
          [tilan-vaihtaja]
          ;; [historiakuvan-aikavalitsin] TODO: (when historia [aikavalinta])
          [pudotusvalikko "Ilmoitukset" (:ilmoitukset @tiedot/suodattimet)]
          [pudotusvalikko "Ylläpito" (:yllapito @tiedot/suodattimet)]
          [nykytilanteen-suodattimet]]) ;; TODO (if historia ..)

(defonce hallintapaneeli (atom {1 {:auki true :otsikko "Tilannekuva" :sisalto suodattimet}}))

(defn tilannekuva []
  (komp/luo
    (komp/lippu tiedot/nakymassa? tiedot/karttataso-tilannekuva)
    (komp/sisaan-ulos #(reset! kartta/pida-geometriat-nakyvilla? false) #(reset! kartta/pida-geometriat-nakyvilla? true))
    (komp/kuuntelija [:toteuma-klikattu :reittipiste-klikattu :ilmoitus-klikattu
                      :laatupoikkeama-klikattu :tarkastus-klikattu :turvallisuuspoikkeama-klikattu
                      :paallystyskohde-klikattu :paikkaustoteuma-klikattu :tyokone-klikattu
                      :uusi-tyokonedata] (fn [_ tapahtuma] (popupit/nayta-popup tapahtuma))
                     :popup-suljettu #(reset! popupit/klikattu-tyokone nil))
    {:component-will-mount   (fn [_]
                               (kartta/aseta-yleiset-kontrollit
                                 [yleiset/haitari hallintapaneeli {:piiloita-kun-kiinni? true
                                                                   :luokka "haitari-tilannekuva"}]))
     :component-will-unmount (fn [_]
                               (kartta/tyhjenna-yleiset-kontrollit)
                               (kartta/poista-popup!))}
    (fn []
      [:span.tilannekuva
       [kartta/kartan-paikka]])))

;; TODO: Vanhaa koodia vanhasta näkymästä. Veikkaanpa että yleinen pvm-komponentti ei
;; taivu tähän näkymään, vaan kannattaa vaan tehdä uusi.
#_(defn historiakuvan-aikavalitsin []
    [:span#tk-aikavalitsin
     [kentat/tee-kentta {:tyyppi :pvm :absoluuttinen? true}
      (r/wrap (first @tiedot/historiakuvan-aikavali)
              (fn [u]
                (swap! tiedot/historiakuvan-aikavali assoc 0 u)
                (when (apply pvm/jalkeen? @tiedot/historiakuvan-aikavali)
                  (swap! tiedot/historiakuvan-aikavali assoc 1 (second (pvm/kuukauden-aikavali u))))))]

     [kentat/tee-kentta {:tyyppi :pvm :absoluuttinen? true}
      (r/wrap (second @tiedot/historiakuvan-aikavali)
              (fn [u]
                (swap! tiedot/historiakuvan-aikavali assoc 1 u)
                (when (apply pvm/jalkeen? @tiedot/historiakuvan-aikavali)
                  (swap! tiedot/historiakuvan-aikavali assoc 0 (first (pvm/kuukauden-aikavali u))))))]])