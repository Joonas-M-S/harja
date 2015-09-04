(ns harja.views.raportit
  "Harjan raporttien pääsivu."
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.komponentti :as komp]
            [harja.ui.valinnat :as valinnat]
            [harja.ui.lomake :as lomake]
            [harja.views.urakat :as urakat]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.pvm :as pvm]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.yleiset :refer [livi-pudotusvalikko]]
            [harja.fmt :as fmt]
            [harja.tiedot.raportit :as raportit]
            [harja.ui.grid :as grid]
            [cljs.core.async :refer [<! >! chan]]
            [harja.views.kartta :as kartta]
            [cljs-time.core :as t])
  (:require-macros [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(defonce valittu-raporttityyppi (atom nil))

(def +raporttityypit+
  ; HUOM: Hardcoodattu testidata vectori mappeja
  [{:nimi      :laskutusyhteenveto
    :otsikko   "Yks.hint. töiden toteumat -raportti"
    :konteksti #{:urakka}
    :parametrit
               [{:otsikko  "Hoitokausi"
                 :nimi     :hoitokausi
                 :tyyppi   :valinta
                 :validoi  [[:ei-tyhja "Anna arvo"]]
                 :valinnat :valitun-urakan-hoitokaudet}
                {:otsikko  "Kuukausi"
                 :nimi     :kuukausi
                 :tyyppi   :valinta
                 :validoi  [[:ei-tyhja "Anna arvo"]]
                 :valinnat :valitun-aikavalin-kuukaudet}]
    :suorita   (fn []
                 (let [urakka @nav/valittu-urakka
                       alkupvm (t/minus (t/now) (t/years 30)) ; FIXME Käytä valittua kuukautta
                       loppupvm (t/plus alkupvm (t/years 30))
                                        sisalto (go (let [vastaus (<! (raportit/hae-yksikkohintaisten-toiden-kuukausiraportti urakka
                                                                                                                              alkupvm
                                                                                                                              loppupvm))]
                                                      (log "[RAPORTTI] Data raportille: " (pr-str vastaus))
                                                      vastaus))]
                   [:span
                    [grid/grid
                     {:otsikko      "Yksikköhintaisten töiden kuukausiraportti"
                      :tyhja        (if (empty? sisalto) "Raporttia ei voitu luoda.")
                      :voi-muokata? false}
                     [{:otsikko "Päivämäärä" :nimi :pvm :muokattava? (constantly false) :tyyppi :pvm :leveys "20%"}
                      {:otsikko "Tehtävä" :nimi :nimi :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}
                      {:otsikko "Yksikkö" :nimi :yksikko :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}
                      {:otsikko "Yksikköhinta" :nimi :yksikkohinta :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}
                      {:otsikko "Suunniteltu määrä" :nimi :suunniteltu-maara :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}
                      {:otsikko "Toteutunut määrä" :nimi :toteutunut-maara :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}
                      {:otsikko "Suunnitellut kustannukset" :nimi :suunnitellut-kustannukset :fmt fmt/euro-opt :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}
                      {:otsikko "Toteutuneet kustannukset" :nimi :toteutuneet-kustannukset :fmt fmt/euro-opt :muokattava? (constantly false) :tyyppi :numero :leveys "20%"}]
                     sisalto]]))}])

(defn tee-lomakekentta [kentta lomakkeen-tiedot]
  (if (= :valinta (:tyyppi kentta))
    (case (:valinnat kentta)
      :valitun-urakan-hoitokaudet
      (assoc kentta :valinnat @u/valitun-urakan-hoitokaudet
                    :valinta-nayta #(if % (fmt/pvm-vali-opt %) "- Valitse hoitokausi - "))
      :valitun-aikavalin-kuukaudet
      (assoc kentta :valinnat (if-let [hk (:hoitokausi lomakkeen-tiedot)] ; FIXME Valintojen pitäisi päivittyä jos hoitokausi vaihtuu.
                                (pvm/hoitokauden-kuukausivalit hk) ; FIXME Näytä kuukaudet tekstinä "Tammikuu, Helmikuu jne. Ehkä myös Koko hoitokausi?"
                                [])
                    :valinta-nayta #(if % (fmt/pvm-opt (first %)) "- Valitse kuukausi - ")))
    kentta))

(def lomake-tiedot (atom nil))
(def lomake-virheet (atom nil))

(defn raporttinakyma []
  (let [nakyma (:suorita @valittu-raporttityyppi)]
    (nakyma)))

(def raportti-valmis-naytettavaksi?
  (reaction (let [valittu-raporttityyppi @valittu-raporttityyppi
                  lomake-virheet @lomake-virheet]
              (when (and valittu-raporttityyppi
                         (not (nil? lomake-virheet))
                         (empty? lomake-virheet))
                true))))

(defn raporttivalinnat
  []
  (komp/luo
    (fn []
      [:div.raportit
       [:div.label-ja-alasveto
        [:span.alasvedon-otsikko "Valitse raportti"]
        [livi-pudotusvalikko {:valinta    @valittu-raporttityyppi
                              ;;\u2014 on väliviivan unikoodi
                              :format-fn  #(if % (:otsikko %) "Valitse")
                              :valitse-fn #(reset! valittu-raporttityyppi %)
                              :class      "valitse-raportti-alasveto"}
         +raporttityypit+]]

       (when @valittu-raporttityyppi
         [lomake/lomake
          {:luokka   :horizontal
           :virheet  lomake-virheet
           :muokkaa! (fn [uusi]
                       (reset! lomake-tiedot uusi))}
          (let [lomake-tiedot @lomake-tiedot
                kentat (into []
                             (concat
                               [{:otsikko "Kohde" :nimi :kohteen-nimi :hae #(:nimi @nav/valittu-urakka) :muokattava? (constantly false)}]
                               (map
                                 (fn [kentta]
                                   (tee-lomakekentta kentta lomake-tiedot))
                                 (:parametrit @valittu-raporttityyppi))))]
            kentat)

          @lomake-tiedot])])))

(defn raporttivalinnat-ja-raportti []
  [:span
   [raporttivalinnat]
   (when @raportti-valmis-naytettavaksi?
     [raporttinakyma])])

(defn raportit []
  (komp/luo
    (fn []
      (or
        (urakat/valitse-hallintayksikko-ja-urakka) ; FIXME Voi olla tarve luoda raportti hallintayksikön alueesta tai koko Suomesta.
        (raporttivalinnat-ja-raportti)))))
