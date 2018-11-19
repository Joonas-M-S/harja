(ns harja.tiedot.urakka.paallystys
  "Päällystyksen tiedot"
  (:require
    [reagent.core :refer [atom] :as r]
    [tuck.core :refer [process-event] :as tuck]
    [harja.tyokalut.tuck :as tuck-apurit]
    [harja.tiedot.muokkauslukko :as lukko]
    [harja.loki :refer [log tarkkaile!]]
    [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]
    [harja.tiedot.urakka.yllapitokohteet :as yllapitokohteet]
    [harja.tiedot.urakka.paallystys-muut-kustannukset :as muut-kustannukset]
    [cljs.core.async :refer [<!]]
    [harja.atom :refer [paivita!]]
    [harja.asiakas.kommunikaatio :as k]
    [harja.tiedot.navigaatio :as nav]
    [harja.tiedot.urakka :as urakka]
    [harja.domain.tierekisteri :as tr-domain]
    [harja.domain.oikeudet :as oikeudet]
    [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]
    [harja.domain.paallystysilmoitus :as pot]
    [harja.domain.urakka :as urakka-domain]
    [harja.tiedot.urakka.yllapito :as yllapito-tiedot]
    [harja.ui.viesti :as viesti])

  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<! reaction-writable]]))

(def kohdeluettelossa? (atom false))
(def paallystysilmoitukset-tai-kohteet-nakymassa? (atom false))
(def validointivirheet-modal (atom nil))

(defn hae-paallystysilmoitukset [urakka-id sopimus-id vuosi]
  (k/post! :urakan-paallystysilmoitukset {:urakka-id urakka-id
                                          :sopimus-id sopimus-id
                                          :vuosi vuosi}))

(defn hae-paallystysilmoitus-paallystyskohteella [urakka-id yllapitokohde-id]
  (k/post! :urakan-paallystysilmoitus-paallystyskohteella {:urakka-id urakka-id
                                                           :paallystyskohde-id yllapitokohde-id}))

(defn tallenna-paallystysilmoitus! [{:keys [urakka-id sopimus-id vuosi lomakedata]}]
  (k/post! :tallenna-paallystysilmoitus {:urakka-id urakka-id
                                         :sopimus-id sopimus-id
                                         :vuosi vuosi
                                         :paallystysilmoitus lomakedata}))

(def paallystysilmoitukset
  (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
               vuosi @urakka/valittu-urakan-vuosi
               [valittu-sopimus-id _] @urakka/valittu-sopimusnumero
               nakymassa? @paallystysilmoitukset-tai-kohteet-nakymassa?]
              {:nil-kun-haku-kaynnissa? true}
              (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                (hae-paallystysilmoitukset valittu-urakka-id valittu-sopimus-id vuosi))))

(def paallystysilmoitukset-suodatettu
  (reaction (let [tienumero @yllapito-tiedot/tienumero
                  kohdenumero @yllapito-tiedot/kohdenumero]
              (when @paallystysilmoitukset
                (yllapitokohteet/suodata-yllapitokohteet @paallystysilmoitukset {:tienumero tienumero
                                                                                 :kohdenumero kohdenumero})))))

(defonce paallystysilmoitus-lomakedata (atom nil))

(defonce karttataso-paallystyskohteet (atom false))

(def yllapitokohteet
  (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
               vuosi @urakka/valittu-urakan-vuosi
               [valittu-sopimus-id _] @urakka/valittu-sopimusnumero
               nakymassa? @kohdeluettelossa?]
              {:nil-kun-haku-kaynnissa? true}
              (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                (yllapitokohteet/hae-yllapitokohteet valittu-urakka-id valittu-sopimus-id vuosi))))

(def yllapitokohteet-suodatettu
  (reaction (let [tienumero @yllapito-tiedot/tienumero
                  yllapitokohteet @yllapitokohteet
                  kohdenumero @yllapito-tiedot/kohdenumero
                  kohteet (when yllapitokohteet
                            (yllapitokohteet/suodata-yllapitokohteet yllapitokohteet {:tienumero tienumero
                                                                                      :kohdenumero kohdenumero}))]
              kohteet)))

(def yhan-paallystyskohteet
  (reaction-writable
    (let [kohteet @yllapitokohteet-suodatettu
          yhan-paallystyskohteet (when kohteet
                                   (yllapitokohteet/suodata-yllapitokohteet
                                     kohteet
                                     {:yha-kohde? true :yllapitokohdetyotyyppi :paallystys}))]
      (tr-domain/jarjesta-kohteiden-kohdeosat yhan-paallystyskohteet))))

(def harjan-paikkauskohteet
  (reaction-writable
    (let [kohteet @yllapitokohteet-suodatettu
          harjan-paikkauskohteet (when kohteet
                                   (yllapitokohteet/suodata-yllapitokohteet
                                     kohteet
                                     {:yha-kohde? false :yllapitokohdetyotyyppi :paikkaus}))]
      (tr-domain/jarjesta-kohteiden-kohdeosat harjan-paikkauskohteet))))

(def kaikki-kohteet
  (reaction (concat @yhan-paallystyskohteet @harjan-paikkauskohteet (when muut-kustannukset/kohteet
                                                                      @muut-kustannukset/kohteet))))

(defonce paallystyskohteet-kartalla
  (reaction (let [taso @karttataso-paallystyskohteet
                  paallystyskohteet @yhan-paallystyskohteet
                  lomakedata @paallystysilmoitus-lomakedata]
              (when (and taso paallystyskohteet)
                (yllapitokohteet/yllapitokohteet-kartalle
                  paallystyskohteet
                  lomakedata)))))

(defonce kohteet-yha-lahetyksessa (atom nil))

;; Yhteiset UI-asiat

(def paallyste-grid-skeema
  {:otsikko "Päällyste"
   :nimi :paallystetyyppi
   :tyyppi :valinta
   :valinta-arvo :koodi
   :valinta-nayta (fn [rivi]
                    (if (:koodi rivi)
                      (str (:lyhenne rivi) " - " (:nimi rivi))
                      (:nimi rivi)))
   :valinnat paallystys-ja-paikkaus/+paallystetyypit-ja-nil+})

(def raekoko-grid-skeema
  {:otsikko "Rae\u00ADkoko" :nimi :raekoko :tyyppi :numero :desimaalien-maara 0
   :tasaa :oikea
   :validoi [[:rajattu-numero 0 99]]})

(def tyomenetelma-grid-skeema
  {:otsikko "Pääll. työ\u00ADmenetelmä"
   :nimi :tyomenetelma
   :tyyppi :valinta
   :valinta-arvo :koodi
   :valinta-nayta (fn [rivi]
                    (if (:koodi rivi)
                      (str (:lyhenne rivi) " - " (:nimi rivi))
                      (:nimi rivi)))
   :valinnat pot/+tyomenetelmat-ja-nil+})

(defn tallenna-paallystysilmoitusten-takuupvmt [urakka-id paallystysilmoitukset]
  (let [ilmoitukset-joilla-jo-pot (keep #(when (:harja.domain.paallystysilmoitus/id %) %)
                                        paallystysilmoitukset)]
    (k/post! :tallenna-paallystysilmoitusten-takuupvmt
             {::urakka-domain/id urakka-id
              ::pot/tallennettavat-paallystysilmoitusten-takuupvmt ilmoitukset-joilla-jo-pot})))

(defn avaa-paallystysilmoituksen-lukitus!
  [{:keys [urakka-id kohde-id tila]}]
  (k/post! :aseta-paallystysilmoituksen-tila {::urakka-domain/id urakka-id
                                              ::pot/paallystyskohde-id kohde-id
                                              ::pot/tila tila}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pikkuhiljaa tätä muutetaan tuckin yhden atomin maalimaan
(def tila (atom nil))

(defrecord MuutaTila [polku arvo])
(defrecord SuodataYllapitokohteet [])
(defrecord HaePaallystysilmoitukset [])
(defrecord HaePaallystysilmoituksetOnnnistui [vastaus])
(defrecord HaePaallystysilmoituksetEpaonnisuti [vastaus])
(defrecord HaePaallystysilmoitusPaallystyskohteellaOnnnistui [vastaus])
(defrecord HaePaallystysilmoitusPaallystyskohteellaEpaonnisuti [vastaus])
(defrecord AvaaPaallystysilmoitus [paallystyskohde-id])
(defrecord YHAVientiOnnistui [paallystysilmoitukset])
(defrecord YHAVientiEpaonnistui [paallystysilmoitukset])

(extend-protocol tuck/Event
  MuutaTila
  (process-event [{:keys [polku arvo]} tila]
    (assoc-in tila polku arvo))
  SuodataYllapitokohteet
  (process-event [_ {paallystysilmoitukset :paallystysilmoitukset
                     {:keys [tienumero kohdenumero]} :yllapito-tila :as tila}]
    (when paallystysilmoitukset
      (yllapitokohteet/suodata-yllapitokohteet paallystysilmoitukset {:tienumero tienumero
                                                                      :kohdenumero kohdenumero}))
    tila)
  HaePaallystysilmoitukset
  (process-event [_ {{urakka-id :id} :urakka
                     {:keys [valittu-sopimusnumero valittu-urakan-vuosi]} :urakka-tila
                     :as app}]
    (let [parametrit {:urakka-id urakka-id
                      :sopimus-id (first valittu-sopimusnumero)
                      :vuosi valittu-urakan-vuosi}]
      (println "HAETAAN PARAMETRILLA " parametrit)
      (-> app
          (tuck-apurit/post! :urakan-paallystysilmoitukset
                             parametrit
                             {:onnistui ->HaePaallystysilmoituksetOnnnistui
                              :epaonnistui ->HaePaallystysilmoituksetEpaonnisuti})
          (assoc :kiintioiden-haku-kaynnissa? true))))
  HaePaallystysilmoituksetOnnnistui
  (process-event [{vastaus :vastaus} app]
    (println "VASTAUS ONNISTUI: " vastaus)
    (assoc app :paallystysilmoitukset vastaus))
  HaePaallystysilmoituksetEpaonnisuti
  (process-event [{vastaus :vastaus} app]
    (println "VASTAUS EPÄONNISTUI: " vastaus)
    app)
  HaePaallystysilmoitusPaallystyskohteellaOnnnistui
  (process-event [{vastaus :vastaus} {urakka :urakka :as app}]
    (println "VASTAUS ONNISTUI: " vastaus)
    (assoc app :paallystysilmoitus-lomakedata
           (-> vastaus
               ;; Leivotaan jokaiselle kannan JSON-rakenteesta nostetulle alustatoimelle id järjestämistä varten
               (update-in [:ilmoitustiedot :alustatoimet]
                          (fn [alustatoimet]
                            (vec (map #(assoc %1 :id %2)
                                      alustatoimet (iterate inc 1)))))
               (assoc
                 :kirjoitusoikeus?
                 (oikeudet/voi-kirjoittaa? oikeudet/urakat-kohdeluettelo-paallystysilmoitukset
                                           (:id urakka))))))
  HaePaallystysilmoitusPaallystyskohteellaEpaonnisuti
  (process-event [{vastaus :vastaus} app]
    (println "VASTAUS EPÄONNISTUI: " vastaus)
    (viesti/nayta! "Päällystysilmoituksen haku epäonnistui." :warning viesti/viestin-nayttoaika-lyhyt)
    app)
  AvaaPaallystysilmoitus
  (process-event [{paallystyskohde-id :paallystyskohde-id} {urakka :urakka :as app}]
    (let [parametrit {:urakka-id (:id urakka)
                      :paallystyskohde-id paallystyskohde-id}]
      (tuck-apurit/post! app
                         :urakan-paallystysilmoitus-paallystyskohteella
                         parametrit
                         {:onnistui ->HaePaallystysilmoitusPaallystyskohteellaOnnnistui
                          :epaonnistui ->HaePaallystysilmoitusPaallystyskohteellaEpaonnisuti})))
  YHAVientiOnnistui
  (process-event [{paallystysilmoitukset :paallystysilmoitukset} app]
    (viesti/nayta! "Kohteet lähetetty onnistuneesti." :success)
    (assoc app :paallystysilmoitukset paallystysilmoitukset))
  YHAVientiEpaonnistui
  (process-event [{paallystysilmoitukset :paallystysilmoitukset} app]
    (viesti/nayta! "Lähetys epäonnistui osalle kohteista. Tarkista kohteiden tiedot." :warning)
    (assoc app :paallystysilmoitukset paallystysilmoitukset)))