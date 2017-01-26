(ns harja.tiedot.tierekisteri.varusteet
  "Tierekisterin varusteisiin ja niiden hakuun liittyvät tiedot"
  (:require [tuck.core :as t]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<!]]
            [harja.loki :refer [log]]
            [harja.domain.tierekisteri.varusteet :as varusteet]
            [harja.tiedot.urakka.toteumat.varusteet :as varuste-tiedot]
            [harja.tiedot.urakka :as urakka]
            [harja.tiedot.navigaatio :as nav]
            [clojure.walk :as walk]
            [harja.ui.viesti :as viesti]
            [cljs.core.async :as async])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn varustetoteuma
  ([tiedot toiminto] (varustetoteuma tiedot toiminto nil nil))
  ([{:keys [tietue]} toiminto kuntoluokitus lisatieto]
   (let [tr-osoite (get-in tietue [:sijainti :tie])]
     {:arvot (walk/keywordize-keys (get-in tietue [:tietolaji :arvot]))
      :puoli (:puoli tr-osoite)
      :ajorata (:ajr tr-osoite)
      :tierekisteriosoite {:numero (:numero tr-osoite)
                           :alkuosa (:aosa tr-osoite)
                           :alkuetaisyys (:aet tr-osoite)
                           :loppuosa (:losa tr-osoite)
                           :loppuetaisyys (:let tr-osoite)}
      :tietolaji (get-in tietue [:tietolaji :tunniste])
      :toiminto toiminto
      :urakka-id @nav/valittu-urakka-id
      :alkupvm (:alkupvm tietue)
      :loppupvm (:loppupvm tietue)
      :kuntoluokitus kuntoluokitus
      :lisatieto lisatieto})))

(defn hakutulokset
  ([app] (hakutulokset app nil nil))
  ([app tietolaji varusteet]
   (-> app
       (assoc-in [:tierekisterin-varusteet :varusteet] varusteet)
       (assoc-in [:tierekisterin-varusteet :tietolaji] tietolaji)
       (assoc-in [:tierekisterin-varusteet :listaus-skeema]
                 (into [varusteet/varusteen-osoite-skeema]
                       (comp (filter varusteet/kiinnostaa-listauksessa?)
                             (map varusteet/varusteominaisuus->skeema))
                       (:ominaisuudet tietolaji)))
       (assoc-in [:tierekisterin-varusteet :hakuehdot :haku-kaynnissa?] false))))

;; Määritellään varustehaun UI tapahtumat

;; Päivittää varustehaun hakuehdot
(defrecord AsetaVarusteidenHakuehdot [hakuehdot])
;; Suorittaa haun
(defrecord HaeVarusteita [])
(defrecord VarusteHakuTulos [tietolaji varusteet])
(defrecord VarusteHakuEpaonnistui [virhe])

;; Toimenpiteet Tierekisteriin
(defrecord PoistaVaruste [varuste])
(defrecord AloitaVarusteenTarkastus [varuste tunniste tietolaji])
(defrecord PeruutaVarusteenTarkastus [])
(defrecord AsetaVarusteTarkastuksenTiedot [tiedot])
(defrecord TallennaVarustetarkastus [varuste tarkastus])
(defrecord AloitaVarusteenMuokkaus [varuste])
(defrecord ToimintoEpaonnistui [toiminto virhe])
(defrecord ToimintoOnnistui [vastaus])

(extend-protocol t/Event
  AsetaVarusteidenHakuehdot
  (process-event [{ehdot :hakuehdot} app]
    (assoc-in app [:tierekisterin-varusteet :hakuehdot] ehdot))

  HaeVarusteita
  (process-event [_ {{hakuehdot :hakuehdot} :tierekisterin-varusteet :as app}]
    (let [tulos! (t/send-async! map->VarusteHakuTulos)
          virhe! (t/send-async! ->VarusteHakuEpaonnistui)]
      (go
        (let [vastaus (<! (k/post! :hae-varusteita hakuehdot))]
          (log "[TR] Varustehaun vastaus: " (pr-str vastaus))
          (if (or (k/virhe? vastaus)
                  (not (:onnistunut vastaus)))
            (virhe! vastaus)
            (tulos! vastaus))))
      (-> app
          (assoc-in [:tierekisterin-varusteet :varusteet] nil)
          (assoc-in [:tierekisterin-varusteet :hakuehdot :haku-kaynnissa?] true))))

  VarusteHakuTulos
  (process-event [{tietolaji :tietolaji varusteet :varusteet} app]
    (hakutulokset app tietolaji varusteet))

  VarusteHakuEpaonnistui
  (process-event [{virhe :virhe} app]
    (log "[TR] Virhe haettaessa varusteita: " (pr-str virhe))
    (viesti/nayta! "Virhe haettaessa varusteita Tierekisteristä" :warning)
    (assoc-in app [:tierekisterin-varusteet :hakuehdot :haku-kaynnissa?] false))

  ToimintoEpaonnistui
  (process-event [{{:keys [viesti vastaus]} :toiminto virhe :virhe :as tiedot} app]
    (log "[TR] Virhe suoritettaessa toimintoa. Virhe:" (pr-str virhe) ". Vastaus: " (pr-str vastaus) ".")
    (viesti/nayta! viesti :warning)

    ;; todo: mieti miten tehdä haku tierekisteriin uudestaan
    (-> app
        (hakutulokset)
        (assoc :uudet-varustetoteumat vastaus)))

  ToimintoOnnistui
  (process-event [{{:keys [vastaus]} :toiminto :as tiedot} app]

    ;; todo: mieti miten tehdä haku tierekisteriin uudestaan
    (-> app
        (hakutulokset)
        (assoc :uudet-varustetoteumat vastaus)))

  PoistaVaruste
  (process-event [{varuste :varuste} app]
    (let [tulos! (t/send-async! map->ToimintoOnnistui)
          virhe! (t/send-async! ->ToimintoEpaonnistui)]
      (go
        (let [varustetoteuma (varustetoteuma varuste :poistettu)
              hakuehdot {:urakka-id (:id @nav/valittu-urakka)
                         :sopimus-id (first @urakka/valittu-sopimusnumero)
                         :aikavali @urakka/valittu-aikavali}
              vastaus (<! (varuste-tiedot/tallenna-varustetoteuma hakuehdot varustetoteuma))]
          (if (or (k/virhe? vastaus) (not (:onnistunut vastaus)))
            (virhe! {:vastaus vastaus :viesti "Varusteen poistossa tapahtui virhe."})
            (tulos! {:vastaus vastaus :viesti "Varuste poistettu onnistuneesti."})))))
    app)

  AloitaVarusteenTarkastus
  (process-event [{:keys [varuste tunniste tietolaji]} app]
    (assoc-in app [:tierekisterin-varusteet :tarkastus] {:varuste varuste :tunniste tunniste :tietolaji tietolaji}))

  PeruutaVarusteenTarkastus
  (process-event [_ app]
    (assoc-in app [:tierekisterin-varusteet :tarkastus] nil))

  TallennaVarustetarkastus
  (process-event [{varuste :varuste {lisatieto :lisatietoja kuntoluokitus :kuntoluokitus} :tarkastus :as data} app]
    (let [tulos! (t/send-async! map->ToimintoOnnistui)
          virhe! (t/send-async! ->ToimintoEpaonnistui)]
      (go
        (let [varuste (assoc-in varuste [:tietue :tietolaji :arvot "kuntoluokitus"] kuntoluokitus)
              varustetoteuma (varustetoteuma varuste :tarkastus kuntoluokitus lisatieto)
              hakuehdot {:urakka-id (:id @nav/valittu-urakka)
                         :sopimus-id (first @urakka/valittu-sopimusnumero)
                         :aikavali @urakka/valittu-aikavali}
              vastaus (<! (varuste-tiedot/tallenna-varustetoteuma hakuehdot varustetoteuma))]
          (if (or (k/virhe? vastaus) (not (:onnistunut vastaus)))
            (virhe! {:vastaus vastaus :viesti "Varusteen tarkastuksen kirjauksessa tapahtui virhe."})
            (tulos! {:vastaus vastaus :viesti "Varustetarkastus kirjattu onnistuneesti."})))))
    (assoc-in app [:tierekisterin-varusteet :tarkastus] nil))

  AsetaVarusteTarkastuksenTiedot
  (process-event [{uudet-tiedot :tiedot} {tarkastus :tarkastus :as app}]
    (assoc-in app [:tierekisterin-varusteet :tarkastus] (assoc tarkastus :tiedot uudet-tiedot)))

  AloitaVarusteenMuokkaus
  (process-event [{varuste :varuste} app]
    (assoc app :muokattava-varuste varuste)))
