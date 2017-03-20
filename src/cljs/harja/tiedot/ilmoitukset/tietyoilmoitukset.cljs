(ns harja.tiedot.ilmoitukset.tietyoilmoitukset
  (:require [reagent.core :refer [atom]]
            [harja.pvm :as pvm]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as tiedot-urakka]
            [harja.tiedot.urakat :as tiedot-urakat]
            [harja.loki :refer [log tarkkaile!]]
            [cljs.core.async :as async]
            [harja.atom :refer [paivita-periodisesti] :refer-macros [reaction<!]]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon]]
            [tuck.core :as tuck]
            [harja.domain.tietyoilmoitukset :as t]
            [cljs.pprint :refer [pprint]])
  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

(def luonti-aikavalit [{:nimi "Ei rajausta" :ei-rajausta? true}
                       {:nimi "1 päivän ajalta" :tunteja 24}
                       {:nimi "1 viikon ajalta" :tunteja 168}
                       {:nimi "4 viikon ajalta" :tunteja 672}
                       {:nimi "Vapaa aikaväli" :vapaa-aikavali true}])

(def kaynnissa-aikavalit [{:nimi "Ei rajausta" :ei-rajausta? true}
                          {:nimi "1 päivän sisällä" :tunteja 24}
                          {:nimi "1 viikon sisällä" :tunteja 168}
                          {:nimi "4 viikon sisällä" :tunteja 672}
                          {:nimi "Vapaa aikaväli" :vapaa-aikavali true}])

(defonce ulkoisetvalinnat
         (reaction {:voi-hakea? true
                    :hallintayksikko (:id @nav/valittu-hallintayksikko)
                    :urakka @nav/valittu-urakka
                    :valitun-urakan-hoitokaudet @tiedot-urakka/valitun-urakan-hoitokaudet
                    :urakoitsija (:id @nav/valittu-urakoitsija)
                    :urakkatyyppi (:arvo @nav/urakkatyyppi)
                    :hoitokausi @tiedot-urakka/valittu-hoitokausi}))


(defonce tietyoilmoitukset (atom {:ilmoitusnakymassa? false
                                  :valittu-ilmoitus nil
                                  :haku-kaynnissa? false
                                  :tietyoilmoitukset nil
                                  :valinnat {:luotu-vakioaikavali (second luonti-aikavalit)
                                             :luotu-alkuaika (pvm/tuntia-sitten 24)
                                             :luotu-loppuaika (pvm/nyt)
                                             :kaynnissa-vakioaikavali (first kaynnissa-aikavalit)
                                             :kaynnissa-alkuaika (pvm/tunnin-paasta 24)
                                             :kaynnissa-loppuaika (pvm/tunnin-paasta 24)}}))

(defonce karttataso-tietyoilmoitukset (atom false))

(defonce tietyoilmoitukset-kartalla
         (reaction
           (let [{:keys [tietyoilmoitukset valittu-ilmoitus]} @tietyoilmoitukset]
             (when @karttataso-tietyoilmoitukset
               (kartalla-esitettavaan-muotoon
                 (map #(assoc % :tyyppi-kartalla :tietyoilmoitus) tietyoilmoitukset)
                 #(= (::t/id %) (::t/id valittu-ilmoitus)))))))

(defn- nil-hylkiva-concat [akku arvo]
  (if (or (nil? arvo) (nil? akku))
    nil
    (concat akku arvo)))

(defonce karttataso-ilmoitukset (atom false))

(defrecord AsetaValinnat [valinnat])
(defrecord YhdistaValinnat [ulkoisetvalinnat])
(defrecord HaeIlmoitukset [])
(defrecord IlmoituksetHaettu [tulokset])
(defrecord ValitseIlmoitus [ilmoitus])
(defrecord PoistaIlmoitusValinta [])
(defrecord IlmoitustaMuokattu [ilmoitus])
(defrecord HaeKayttajanUrakat [hallintayksikot])
(defrecord KayttajanUrakatHaettu [urakat])
(defrecord PaivitaSijainti [sijainti])
(defrecord PaivitaNopeusrajoituksetGrid [nopeusrajoitukset])

(defn- hae-ilmoitukset [{valinnat :valinnat haku :ilmoitushaku-id :as app}]
  (when haku
    (.clearTimeout js/window haku))
  (assoc app :ilmoitushaku-id (.setTimeout js/window (tuck/send-async! ->HaeIlmoitukset) 1000)))

(extend-protocol tuck/Event
  AsetaValinnat
  (process-event [{valinnat :valinnat} app]
    (hae-ilmoitukset (assoc app :valinnat valinnat)))

  YhdistaValinnat
  (process-event [{ulkoisetvalinnat :ulkoisetvalinnat :as e} app]
    (let [uudet-valinnat (merge ulkoisetvalinnat (:valinnat app))
          app (assoc app :valinnat uudet-valinnat)]
      (hae-ilmoitukset app)))

  HaeIlmoitukset
  (process-event [_ {valinnat :valinnat :as app}]
    (let [tulos! (tuck/send-async! ->IlmoituksetHaettu)]
      (go
        (tulos!
          (let [parametrit (select-keys valinnat [:luotu-alkuaika
                                                  :luotu-loppuaika
                                                  :luotu-vakioaikavali
                                                  :kaynnissa-alkuaika
                                                  :kaynnissa-loppuaika
                                                  :kaynnissa-vakioaikavali
                                                  :sijainti
                                                  :urakka
                                                  :vain-kayttajan-luomat])]
            {:tietyoilmoitukset (async/<! (k/post! :hae-tietyoilmoitukset parametrit))}))))
    (assoc app :tietyoilmoitukset nil))

  IlmoituksetHaettu
  (process-event [vastaus {valittu :valittu-ilmoitus :as app}]
    (let [ilmoitukset (:tietyoilmoitukset (:tulokset vastaus))]
      (assoc app :tietyoilmoitukset ilmoitukset)))

  ValitseIlmoitus
  (process-event [{ilmoitus :ilmoitus} app]
    (assoc app :valittu-ilmoitus ilmoitus))

  PoistaIlmoitusValinta
  (process-event [_ app]
    (assoc app :valittu-ilmoitus nil))

  IlmoitustaMuokattu
  (process-event [ilmoitus app]
    #_(log "IlmoitustaMuokattu: saatiin" (keys ilmoitus) "ja" (keys app))
    (assoc app :valittu-ilmoitus (:ilmoitus ilmoitus)))

  HaeKayttajanUrakat
  (process-event [{hallintayksikot :hallintayksikot} app]
    (let [tulos! (tuck/send-async! ->KayttajanUrakatHaettu)]
      (when hallintayksikot
        (go (tulos! (async/<!
                      (async/reduce nil-hylkiva-concat []
                                    (async/merge
                                      (mapv tiedot-urakat/hae-hallintayksikon-urakat hallintayksikot))))))))
    (assoc app :kayttajan-urakat nil))

  KayttajanUrakatHaettu
  (process-event [{urakat :urakat} app]
    (let [urakka (when @nav/valittu-urakka ((comp str :id) @nav/valittu-urakka))]
      (assoc app :kayttajan-urakat urakat
                 :valinnat (assoc (:valinnat app) :urakka urakka))))

  PaivitaSijainti
  (process-event [{sijainti :sijainti} app]
    (assoc-in app [:valinnat :sijainti] sijainti))

  PaivitaNopeusrajoituksetGrid
  (process-event [{nopeusrajoitukset :nopeusrajoitukset} app]
    (log "PaivitaNopeusrajoituksetGrid:" (pr-str nopeusrajoitukset))
    (assoc-in app [:valittu-ilmoitus :nopeusrajoitukset] nopeusrajoitukset)))


(def tyotyyppi-vaihtoehdot-tienrakennus
  [["Alikulkukäytävän rak." "Alikulkukäytävän rakennus"]
   ["Kevyenliik. väylän rak." "Kevyenliikenteenväylän rakennus"]
   ["Tienrakennus" "Tienrakennus"]])

(def tyotyyppi-vaihtoehdot-huolto
  [["Tienvarsilaitteiden huolto" "Tienvarsilaitteiden huolto"]
   ["Vesakonraivaus/niittotyö" "Vesakonraivaus / niittotyö"]
   ["Rakenteen parannus" "Rakenteen parannus"]
   ["Tutkimus/mittaus" "Tutkimus / mittaus"]])

(def tyotyyppi-vaihtoehdot-asennus
  [
   ["Jyrsintä-/stabilointityö" "Jyrsintä- / stabilointityö"]
   ["Kaapelityö" "Kaapelityö"]
   ["Kaidetyö" "Kaidetyö"]
   ["Päällystystyö" "Päällystystyö"]
   ["Räjäytystyö" "Räjäytystyö"]
   ["Siltatyö" "Siltatyö"]
   ["Tasoristeystyö" "Tasoristeystyö"]
   ["Tiemerkintätyö" "Tiemerkintätyö"]
   ["Viimeistely" "Viimeistely"]

   ["Valaistustyö" "Valaistustyö"]])

(def tyotyyppi-vaihtoehdot-muut [["Liittymä- ja kaistajärj." "Liittymä- ja kaistajärjestely"]
                                 ["Silmukka-anturin asent." "Silmukka-anturin asentaminen"]
                                 ["Muu, mikä?" "Muu, mikä?"]])

(def tyotyyppi-vaihtoehdot-map (into {} (concat
                                          tyotyyppi-vaihtoehdot-tienrakennus
                                          tyotyyppi-vaihtoehdot-huolto
                                          tyotyyppi-vaihtoehdot-asennus
                                          tyotyyppi-vaihtoehdot-muut)))

(def kaistajarjestelyt-vaihtoehdot-map {"ajokaistaSuljettu" "Yksi ajokaista suljettu"
                                        "ajorataSuljettu" "Yksi ajorata suljettu"
                                        "tieSuljettu" "Tie suljettu"
                                        "muu" "Muu, mikä"})

(def vaikutussuunta-vaihtoehdot-map {"molemmat" "Haittaa molemmissa ajosuunnissa"
                                     "tienumeronKasvusuuntaan" "Tienumeron kasvusuuntaan"
                                     "vastenTienumeronKasvusuuntaa" "Vasten tienumeron kasvusuuntaa"})

(defn henkilo->nimi [henkilo]
  (str (::t/etunimi henkilo) " " (::t/sukunimi henkilo)))
