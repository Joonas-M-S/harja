(ns harja.views.tierekisteri.varustehaku
  "Tierekisterin varustehaun käyttöliittymä"
  (:require [harja.tiedot.tierekisteri.varusteet :as v]
            [harja.domain.tierekisteri.varusteet :as varusteet]
            [harja.ui.lomake :as lomake]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.napit :as napit]
            [harja.loki :refer [log]]
            [harja.ui.debug :refer [debug]]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :as yleiset]))

(defn varustehaku-ehdot [e! {haku? :haku-kaynnissa? :as hakuehdot}]
  [lomake/lomake
   {:otsikko "Hae varusteita Tierekisteristä"
    :muokkaa! #(e! (v/->AsetaVarusteidenHakuehdot %))
    :footer-fn (fn [rivi]
                 [:div
                  [napit/yleinen "Hae Tierekisteristä"
                   #(e! (v/->HaeVarusteita))
                   {:disabled (:haku-kaynnissa? hakuehdot)
                    :ikoni (ikonit/livicon-search)}]
                  (when haku?
                    [yleiset/ajax-loader "Varusteita haetaan tierekisteristä"])])
    :tunniste (comp :tunniste :varuste)
    :ei-borderia? false}

   [{:nimi :tietolaji
     :otsikko "Varusteen tyyppi"
     :tyyppi :valinta
     :pakollinen? true
     :valinnat (vec varusteet/tietolaji->selitys)
     :valinta-nayta second
     :valinta-arvo first}
    {:nimi :tierekisteriosoite
     :otsikko "Tierekisteriosoite"
     :tyyppi :tierekisteriosoite}
    {:nimi :tunniste
     :otsikko "Varusteen tunniste"
     :tyyppi :string}]
   hakuehdot])

(defn sarakkeet [e! tietolajin-listaus-skeema]
  (let [sarakkeet (mapv #(assoc % :leveys 4) tietolajin-listaus-skeema)
        toiminnot {:nimi :toiminnot
                   :otsikko "Toiminnot"
                   :tyyppi :komponentti
                   :leveys 9
                   :komponentti (fn [_]
                                  [:div
                                   [napit/tarkasta "Tarkasta" #()]
                                   [napit/muokkaa "Muokkaa" #()]
                                   [napit/poista "Poista" #()]])}]
    (conj sarakkeet toiminnot)))

(defn varustehaku-varusteet [e! tietolajin-listaus-skeema varusteet]
  [grid/grid
   {:otsikko "Tierekisteristä löytyneet varusteet"

    :tunniste (fn [varuste]
                ;; Valitettavasti varusteiden tunnisteet eivät ole uniikkeja, vaan
                ;; sama varuste voi olla pätkitty useiksi TR osoitteiksi, joten yhdistetään
                ;; niiden avaimeksi tunniste ja osoite.
                (str (get-in varuste [:varuste :tunniste])
                     "_" (pr-str (get-in varuste [:varuste :tietue :sijainti :tie]))))}
   (sarakkeet e! tietolajin-listaus-skeema)
   varusteet])

(defn varustehaku
  "Komponentti, joka näyttää lomakkeen varusteiden hakemiseksi tierekisteristä
  sekä haun tulokset."
  [e! {:keys [hakuehdot listaus-skeema tietolaji varusteet] :as app}]
  [:div.varustehaku
   [varustehaku-ehdot e! (:hakuehdot app)]
   (when (and listaus-skeema varusteet)
     [varustehaku-varusteet e! listaus-skeema varusteet])])
