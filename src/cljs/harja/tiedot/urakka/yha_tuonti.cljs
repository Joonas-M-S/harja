(ns harja.tiedot.urakka.yhatuonti
  (:require [reagent.core :refer [atom] :as reagent]
            [harja.ui.yleiset :refer [ajax-loader vihje]]
            [harja.ui.viesti :as viesti]
            [harja.loki :refer [log logt tarkkaile!]]
            [cljs.core.async :refer [<! >! chan timeout]]
            [harja.ui.lomake :refer [lomake]]
            [harja.ui.grid :refer [grid]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tyokalut.vkm :as vkm]
            [harja.tiedot.navigaatio :as nav]
            [cljs-time.core :as t]
            [harja.domain.oikeudet :as oikeudet]
            [harja.tiedot.istunto :as istunto]
            [harja.pvm :as pvm])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]))

(def hakulomake-data (atom nil))
(def hakutulokset-data (atom []))
(def sidonta-kaynnissa? (atom false))

(defn hae-yha-urakat [{:keys [yhatunniste sampotunniste vuosi harja-urakka-id]}]
  (log "[YHA] Hae YHA-urakat...")
  (reset! hakutulokset-data nil)
  (k/post! :hae-urakat-yhasta {:harja-urakka-id harja-urakka-id
                               :yhatunniste yhatunniste
                               :sampotunniste sampotunniste
                               :vuosi vuosi}))


(defn sido-yha-urakka-harja-urakkaan [harja-urakka-id yha-tiedot]
  (k/post! :sido-yha-urakka-harja-urakkaan {:harja-urakka-id harja-urakka-id
                                            :yha-tiedot yha-tiedot}))

(defn- tallenna-uudet-yha-kohteet [harja-urakka-id kohteet]
  (k/post! :tallenna-uudet-yha-kohteet {:urakka-id harja-urakka-id
                                        :kohteet kohteet}))

(defn- hae-yha-kohteet [harja-urakka-id]
  (log "[YHA] Haetaan YHA-kohteet urakalle id:llä" harja-urakka-id)
  (k/post! :hae-yha-kohteet {:urakka-id harja-urakka-id}))

(defn paivita-yha-kohteet
  "Hakee YHA-kohteet, päivittää ne kutsumalla VMK-palvelua ja tallentaa ne Harjan kantaan.
   Suoritus tapahtuu asynkronisesti"
  [harja-urakka-id]
  ; FIXME Lisää virhekäsittely (k/virhe? ja näytä harja.ui.viesti jos jokin kohta menee pieleen)
  (go (let [uudet-yha-kohteet (<! (hae-yha-kohteet harja-urakka-id))
            vkm-kohteet (vkm/muunna-yha-kohteet uudet-yha-kohteet)
            _ (log "[YHA] Tallennetaan kohteet kantaan")
            yhatiedot (<! (tallenna-uudet-yha-kohteet harja-urakka-id vkm-kohteet))]
        (log "[YHA] Kohteet käsitelty, urakan uudet yhatiedot: " (pr-str yhatiedot))
        (swap! nav/valittu-urakka assoc :yhatiedot yhatiedot))))

(defn paivita-kohdeluettelo [urakka oikeus]
  [harja.ui.napit/palvelinkutsu-nappi
   "Päivitä kohdeluettelo"
   #(do
     (log "[YHA] Päivitetään Harja-urakan " (:id urakka) " kohdeluettelo.")
     (paivita-yha-kohteet (:id urakka)))
   {:luokka "nappi-ensisijainen"
    :disabled (not (oikeudet/on-muu-oikeus? "sido" oikeus (:id urakka) @istunto/kayttaja))
    :virheviesti "Kohdeluettelon päivittäminen epäonnistui."
    :kun-onnistuu (fn [_]
                    (log "[YHA] Kohdeluettelo päivitetty")
                    (swap! nav/valittu-urakka assoc-in [:yhatiedot :kohdeluettelo-paivitetty]
                           (cljs-time.core/to-default-time-zone (t/now))))}])

(defn kohdeluettelo-paivitetty [urakka]
  [:div (str "Kohdeluettelo päivitetty: "
             (if-let [kohdeluettelo-paivitetty (get-in urakka [:yhatiedot :kohdeluettelo-paivitetty])]
               (pvm/pvm-aika kohdeluettelo-paivitetty)
               "ei koskaan"))])