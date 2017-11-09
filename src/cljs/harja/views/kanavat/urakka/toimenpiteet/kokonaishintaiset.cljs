(ns harja.views.kanavat.urakka.toimenpiteet.kokonaishintaiset
  (:require [reagent.core :refer [atom] :as r]
            [tuck.core :refer [tuck]]

            [harja.tiedot.kanavat.urakka.toimenpiteet.kokonaishintaiset :as tiedot]
            [harja.loki :refer [tarkkaile! log]]
            [harja.id :refer [id-olemassa?]]

            [harja.ui.komponentti :as komp]
            [harja.ui.grid :as grid]
            [harja.ui.napit :as napit]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.ui.yleiset :refer [ajax-loader ajax-loader-pieni tietoja]]
            [harja.ui.debug :refer [debug]]

            [harja.domain.kanavat.kanavan-toimenpide :as kanavan-toimenpide]
            [harja.domain.kanavat.kanavan-kohde :as kanavan-kohde]
            [harja.domain.kanavat.kanavan-huoltokohde :as kanavan-huoltokohde]
            [harja.domain.toimenpidekoodi :as toimenpidekoodi]
            [harja.domain.kayttaja :as kayttaja]

            [harja.pvm :as pvm]
            [harja.views.kanavat.urakka.toimenpiteet :as toimenpiteet-view]
            [harja.views.urakka.valinnat :as urakka-valinnat]
            [harja.ui.valinnat :as valinnat]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u])
  (:require-macros
    [cljs.core.async.macros :refer [go]]
    [harja.makrot :refer [defc fnc]]))

(defn valinnat [urakka]
  [valinnat/urakkavalinnat {:urakka urakka}
   ^{:key "valinnat"}
   [urakka-valinnat/urakan-sopimus-ja-hoitokausi-ja-aikavali-ja-toimenpide urakka]
   ^{:key "toiminnot"}
   [valinnat/urakkatoiminnot {:urakka urakka :sticky? true}
    ^{:key "uusi-nappi"}
    [napit/uusi
     "Uusi toimenpide"
     (fn [_]
       ;;todo
       )]]])

(defn kokonaishintaiset-toimenpiteet-taulukko [e! app]
  [grid/grid
   {:otsikko "Kokonaishintaiset toimenpiteet"
    :voi-lisata? false
    :voi-muokata? false
    :voi-poistaa? false
    :voi-kumota? false
    :piilota-toiminnot? true
    :tyhja "Ei kokonaishitaisia toimenpiteita"
    :jarjesta ::kanavan-toimenpide/pvm
    :tunniste ::kanavan-toimenpide/id}
   (toimenpiteet-view/toimenpidesarakkeet
     e! app
     {:kaikki-valittu?-fn #(= (count (:toimenpiteet app))
                              (count (:valitut-toimenpide-idt app)))
      :otsikko-valittu-fn (fn [uusi-arvo]
                            (e! (tiedot/->ValitseToimenpiteet
                                  {:kaikki-valittu? uusi-arvo})))
      :rivi-valittu?-fn (fn [rivi]
                          (boolean ((:valitut-toimenpide-idt app)
                                     (::kanavan-toimenpide/id rivi))))
      :rivi-valittu-fn (fn [rivi uusi-arvo]
                         (e! (tiedot/->ValitseToimenpide
                               {:id (::kanavan-toimenpide/id rivi)
                                :valittu? uusi-arvo})))})
   (:toimenpiteet app)])

(defn kokonaishintaiset-nakyma [e! app]
  [:div
   [valinnat (get-in app [:valinnat :urakka])]
   [kokonaishintaiset-toimenpiteet-taulukko e! app]])

(defn kokonaishintaiset* [e! app]
  (komp/luo
    (komp/watcher tiedot/valinnat (fn [_ _ uusi]
                                    (e! (tiedot/->PaivitaValinnat uusi))))
    (komp/sisaan-ulos #(do
                         (e! (tiedot/->Nakymassa? true))
                         (e! (tiedot/->PaivitaValinnat
                               {:urakka @nav/valittu-urakka
                                :sopimus-id (first @u/valittu-sopimusnumero)
                                :aikavali @u/valittu-aikavali
                                :toimenpide @u/valittu-toimenpideinstanssi})))
                      #(do
                         (e! (tiedot/->Nakymassa? false))))
    (fn [e! {:keys [toimenpiteet haku-kaynnissa?] :as app}]
      @tiedot/valinnat ;; Reaktio on pakko lukea komponentissa, muuten se ei päivity!
      [:span
       [kokonaishintaiset-nakyma e! app]])))

(defc kokonaishintaiset []
      [tuck tiedot/tila kokonaishintaiset*])