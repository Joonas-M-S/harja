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
            [harja.ui.yleiset :as yleiset]
            [harja.tiedot.navigaatio :as nav]
            [harja.domain.oikeudet :as oikeudet]
            [clojure.string :as str]
            [reagent.core :as r]))

(defn oikeus-varusteiden-muokkaamiseen? []
  (oikeudet/voi-kirjoittaa? oikeudet/urakat-toteumat-varusteet (:id @nav/valittu-urakka)))

(defn varustehaku-ehdot [e! {haku? :haku-kaynnissa?
                             tr-osoite :tierekisteriosoite
                             varusteentunniste :tunniste
                             :as hakuehdot}]
  (let [tr-ok? (fn [{:keys [numero alkuosa alkuetaisyys loppuosa loppuetaisyys]}]
                 (and numero alkuosa alkuetaisyys loppuosa loppuetaisyys))]
    [lomake/lomake
     {:otsikko "Hae varusteita Tierekisteristä"
      :muokkaa! #(e! (v/->AsetaVarusteidenHakuehdot %))
      :footer-fn (fn [rivi]
                   [:div
                    [napit/yleinen "Hae Tierekisteristä"
                     #(e! (v/->HaeVarusteita))
                     {:disabled (or (:haku-kaynnissa? hakuehdot)
                                    (and (not (tr-ok? tr-osoite))
                                         (str/blank? varusteentunniste)))
                      :ikoni (ikonit/livicon-search)}]
                    [yleiset/vihje "Hakua tehdessä käytetään joko tyyppiä ja tunnistetta, tai tyyppiä ja tr-osoitetta. Jos kaikki kolme on syötetty, käytetään haussa tyyppiä ja tunnistetta."]
                    (when haku?
                      [yleiset/ajax-loader "Varusteita haetaan tierekisteristä"])])
      :tunniste (comp :tunniste :varuste)
      :ei-borderia? false}
     [{:nimi :tietolaji
       :otsikko "Varusteen tyyppi"
       :tyyppi :valinta
       :pakollinen? true
       :valinnat (vec varusteet/tietolaji->selitys)
       :valinta-nayta #(if (nil? %) "- valitse -" (second %))
       :valinta-arvo first}
      (lomake/ryhma
        ""
        {:nimi        :tierekisteriosoite
        :otsikko     "Tierekisteriosoite"
        :tyyppi      :tierekisteriosoite
        :sijainti    (atom nil)                             ;; sijainti ei kiinnosta, mutta johtuen komponentin toiminnasta, atom täytyy antaa
         ;; FIXME: Jostain syystä tr-osoitteen pakollinen-merkki ei poistu, kun tunnisteen syöttää.
         ;:pakollinen? (str/blank? varusteentunniste)
         }
        {:nimi        :tunniste
         :otsikko     "Varusteen tunniste"
         :tyyppi      :string
         ;:pakollinen? (not (tr-ok? tr-osoite))
         })]
     hakuehdot]))

(defn poista-varuste [e! tietolaji tunniste varuste]
  (yleiset/varmista-kayttajalta
    {:otsikko "Varusteen poistaminen Tierekisteristä"
     :viesti [:div "Haluatko varmasti poistaa tietolajin: "
              [:b (str (varusteet/tietolaji->selitys tietolaji) " (" tietolaji ")")] " varusteen, jonka tunniste on: "
              [:b tunniste] "."]
     :peruuta [:div (ikonit/livicon-ban) " Peruuta"]
     :hyvaksy [:div (ikonit/livicon-trash) " Poista"]
     :toiminto-fn (fn [] (e! (v/->PoistaVaruste varuste)))}))

(defn sarakkeet [e! tietolajin-listaus-skeema]
  (if oikeus-varusteiden-muokkaamiseen?
    (let [toiminnot {:nimi :toiminnot
                     :otsikko "Toiminnot"
                     :tyyppi :komponentti
                     :leveys 3.5
                     :komponentti (fn [{varuste :varuste}]
                                    (let [tunniste (:tunniste varuste)
                                          tietolaji (get-in varuste [:tietue :tietolaji :tunniste])]
                                      [:div
                                       [napit/tarkasta "Tarkasta" #()]
                                       [napit/muokkaa "Muokkaa" #()]
                                       [napit/poista "Poista" #(poista-varuste e! tietolaji tunniste varuste)]]))}]
      (conj tietolajin-listaus-skeema toiminnot))
    tietolajin-listaus-skeema))

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
