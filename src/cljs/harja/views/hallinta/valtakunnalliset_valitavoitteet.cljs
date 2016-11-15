(ns harja.views.hallinta.valtakunnalliset-valitavoitteet
  "Valtakunnallisten välitavoitteiden näkymä"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<! >! timeout chan]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.hallinta.valtakunnalliset-valitavoitteet :as tiedot]
            [harja.pvm :as pvm]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.grid :refer [grid]]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :as y]
            [harja.domain.oikeudet :as oikeudet]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.navigaatio :as nav]
            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.valinnat :as valinnat])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

;; Valtakunnallisille välitavoitteille on haluttu eri urakkatyypeissä käyttää hieman eri nimitystä
(def kertaluontoiset-otsikko {:tiemerkinta "Kertaluontoiset välitavoitepohjat"
                              :oletus "Valtakunnalliset kertaluontoiset välitavoitteet"})
(def toistuvat-otsikko {:tiemerkinta "Vuosittain toistuvat välitavoitepohjat"
                        :oletus "Valtakunnalliset vuosittain toistuvat välitavoitteet"})

(defn kertaluontoiset-valitavoitteet-grid
  [valitavoitteet-atom kertaluontoiset-valitavoitteet-atom valittu-urakkatyyppi-atom]
  [grid/grid
   {:otsikko (case (:arvo @valittu-urakkatyyppi-atom)
               :tiemerkinta (:tiemerkinta kertaluontoiset-otsikko)
               (:oletus kertaluontoiset-otsikko))
    :tyhja (if (nil? @kertaluontoiset-valitavoitteet-atom)
             [y/ajax-loader "Välitavoitteita haetaan..."]
             "Ei kertaluontoisia välitavoitteita")
    :tallenna (when (oikeudet/voi-kirjoittaa? oikeudet/hallinta-valitavoitteet)
                #(go (let [vastaus (<! (tiedot/tallenna-valitavoitteet
                                         (->> %
                                              (map (fn [valitavoite]
                                                     (-> valitavoite
                                                         (assoc :tyyppi :kertaluontoinen)
                                                         (assoc :urakkatyyppi
                                                                (:arvo @tiedot/valittu-urakkatyyppi))))))))]
                       (if (k/virhe? vastaus)
                         (viesti/nayta! "Välitavoitteiden tallentaminen epännistui"
                                        :warning viesti/viestin-nayttoaika-keskipitka)
                         (reset! valitavoitteet-atom vastaus)))))}
   [{:otsikko "Nimi" :leveys 60 :nimi :nimi :tyyppi :string :pituus-max 128
     :validoi [[:ei-tyhja "Anna välitavoitteen nimi"]]}
    {:otsikko "Taka\u00ADraja" :leveys 20 :nimi :takaraja :fmt #(if %
                                                                 (pvm/pvm-opt %)
                                                                 "Ei takarajaa")
     :tyyppi :pvm}]
   (sort-by :takaraja (filter #(= (:urakkatyyppi %)
                                  (:arvo @tiedot/valittu-urakkatyyppi))
                              @kertaluontoiset-valitavoitteet-atom))])

(defn toistuvat-valitavoitteet-grid
  [valitavoitteet-atom toistuvat-valitavoitteet-atom valittu-urakkatyyppi-atom]
  [grid/grid
   {:otsikko (case (:arvo @valittu-urakkatyyppi-atom)
               :tiemerkinta (:tiemerkinta toistuvat-otsikko)
               (:oletus toistuvat-otsikko))
    :tyhja (if (nil? @toistuvat-valitavoitteet-atom)
             [y/ajax-loader "Välitavoitteita haetaan..."]
             "Ei toistuvia välitavoitteita")
    :tallenna (when (oikeudet/voi-kirjoittaa? oikeudet/hallinta-valitavoitteet)
                #(go (let [vastaus (<! (tiedot/tallenna-valitavoitteet
                                         (->> %
                                              (map (fn [valitavoite]
                                                     (-> valitavoite
                                                         (assoc :tyyppi :toistuva)
                                                         (assoc :urakkatyyppi
                                                                (:arvo @tiedot/valittu-urakkatyyppi))))))))]
                       (if (k/virhe? vastaus)
                         (viesti/nayta! "Välitavoitteiden tallentaminen epännistui"
                                        :warning viesti/viestin-nayttoaika-keskipitka)
                         (reset! valitavoitteet-atom vastaus)))))}
   [{:otsikko "Nimi" :leveys 60 :nimi :nimi :tyyppi :string :pituus-max 128
     :validoi [[:ei-tyhja "Anna välitavoitteen nimi"]]}
    {:otsikko "Taka\u00ADrajan toisto\u00ADpäi\u00ADvä" :leveys 10 :nimi :takaraja-toistopaiva
     :tyyppi :numero :desimaalien-maara 0 :validoi [[:rajattu-numero nil 1 31 "Anna päivä välillä 1 - 31"]]}
    {:otsikko "Taka\u00ADrajan toisto\u00ADkuu\u00ADkausi" :leveys 10 :nimi :takaraja-toistokuukausi
     :tyyppi :numero :desimaalien-maara 0 :validoi [[:rajattu-numero nil 1 12 "Anna kuukausi välillä 1 - 12"]]}]
   (sort-by (juxt :takaraja-toistokuukausi :takaraja-toistopaiva)
            (filter #(= (:urakkatyyppi %)
                        (:arvo @tiedot/valittu-urakkatyyppi))
                    @toistuvat-valitavoitteet-atom))])

(defn- suodattimet []
  [valinnat/urakkatyyppi
   tiedot/valittu-urakkatyyppi
   nav/+urakkatyypit+
   #(reset! tiedot/valittu-urakkatyyppi %)])

(defn valitavoitteet []
  (komp/luo
    (komp/lippu tiedot/nakymassa?)
    (fn []
      (let [nayta-valtakunnalliset? (some? (tiedot/valtakunnalliset-valitavoitteet-kaytossa
                                             (:arvo @tiedot/valittu-urakkatyyppi)))]

        [:div
         [suodattimet]
         (if nayta-valtakunnalliset?
           [:div [kertaluontoiset-valitavoitteet-grid
                  tiedot/valitavoitteet
                  tiedot/kertaluontoiset-valitavoitteet
                  tiedot/valittu-urakkatyyppi]
            [:br]
            [toistuvat-valitavoitteet-grid
             tiedot/valitavoitteet
             tiedot/toistuvat-valitavoitteet
             tiedot/valittu-urakkatyyppi]
            [yleiset/vihje-elementti
             [:span
              "Uudet kertaluontoiset välitavoitteet liitetään valituntyyppisiin ei-päättyneisiin urakoihin, jos välitavoitteen takaraja on urakan voimassaoloaikana."
              [:br] "Uudet toistuvat välitavoitteet liitetään valituntyyppisiin ei-päättyneisiin urakoihin kertaalleen per jäljellä oleva urakkavuosi."
              [:br] "Välitavoitteen päivittäminen päivittää tiedot urakoihin, ellei tavoitetta ole muokattu urakassa."
              [:br] "Poistettu välitavoite jää näkyviin päättyneisiin urakoihin tai jos se on ehditty tehdä valmiiksi."]]]
           [:div "Valtakunnalliset välitavoitteet eivät ole käytössä valitussa urakkatyypissä."])]))))
