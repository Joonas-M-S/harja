(ns harja.palvelin.palvelut.tienakyma
  "Tienäkymän backend palvelu.

  Tienäkymässä haetaan TR osoitevälillä tietyllä aikavälillä tapahtuneita
  asioita. Palvelu on vain tilaajan käyttäjille, eikä hauissa ole mitään
  urakkarajauksia näkyvyyteen.

  Tienäkymän kaikki löydökset renderöidään frontilla, koska tietomäärä on rajattu
  yhteen tiehen ja aikaväliin. Tällä mallilla ei tarvitse tehdä erikseen enää
  karttakuvan klikkauksesta hakua vaan kaikki tienäkymän tieto on jo frontilla.

  Kaikki hakufunktiot ottavat samat parametrit: tietokantayhteyden
  ja parametrimäpin, jossa on seuraavat tiedot:
  - hakualueen extent: :x1, :y1, :x2 ja :y2
  - tierekisteriosoitteen geometria: :sijainti
  - tierekisteriosoite: :numero, :alkuosa, :alkuetaisyys, :loppuosa ja :loppuetaisyys
  - haettava aikaväli: :alku ja :loppu

  Hakufunktiot ovat tienakyma-haut mäpissä määritelty.
  "
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin
             :refer [julkaise-palvelut poista-palvelut]]
            [harja.geo :as geo]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.roolit :as roolit]
            [slingshot.slingshot :refer [throw+]]
            [harja.tyokalut.functor :refer [fmap]]
            [taoensso.timbre :as log]
            [harja.kyselyt.tienakyma :as q]
            [harja.kyselyt.konversio :as konv]
            [harja.palvelin.palvelut.tilannekuva :as tilannekuva]))

(defonce debug-hakuparametrit (atom nil))

(defn- hakuparametrit
  "Tekee käyttäjän antamista hakuehdoista jeesql hakuparametrit"
  [{:keys [sijainti alku loppu tierekisteriosoite] :as valinnat}]
  (let [extent (geo/extent sijainti)]
    (merge
     ;; TR-osoitteen geometrinen alue envelope
     (zipmap  [:x1 :y1 :x2 :y2] extent)

     ;; Tierekisteriosoitteen geometria
     {:sijainti (geo/geometry (geo/clj->pg sijainti))}

     ;; Tierekisteriosoite
     tierekisteriosoite

     ;; Aikarajaus, jonka sisällä tarkastellaan
     {:alku alku
      :loppu loppu})))

(defn- hae-toteumat [db parametrit]
  (konv/sarakkeet-vektoriin
   (into []
         (comp (map konv/alaviiva->rakenne)
               (geo/muunna-pg-tulokset :reitti)
               (map #(assoc % :tyyppi-kartalla :toteuma)))
         (q/hae-toteumat db parametrit))
   {:tehtava :tehtavat
    :reittipiste :reittipisteet}
   :id (constantly true)))


(defn- hae-tarkastukset [db parametrit]
  (into []
        (comp (map #(assoc % :tyyppi-kartalla :tarkastus))
              (map #(konv/string->keyword % :tyyppi)))
        (q/hae-tarkastukset db parametrit)))

(defn- hae-turvallisuuspoikkeamat [db parametrit]
  (into []
        (comp (geo/muunna-pg-tulokset :sijainti)
              (map #(assoc % :tyyppi-kartalla :turvallisuuspoikkeama))
              (map #(konv/array->keyword-set % :tyyppi)))
        (q/hae-turvallisuuspoikkeamat db parametrit)))

(defn- hae-laatupoikkeamat [db parametrit]
  (into []
        (comp (map konv/alaviiva->rakenne)
              (geo/muunna-pg-tulokset :sijainti)
              (map #(assoc % :tyyppi-kartalla :laatupoikkeama)))
        (q/hae-laatupoikkeamat db parametrit)))

(defn- hae-ilmoitukset [db parametrit]
  (konv/sarakkeet-vektoriin
   (into []
         (comp tilannekuva/ilmoitus-xf
               (map #(assoc % :tyyppi-kartalla (:ilmoitustyyppi %))))
         (q/hae-ilmoitukset db parametrit))
   {:kuittaus :kuittaukset}))

(def ^{:private true
       :doc "Määrittelee kaikki kyselyt mitä tienäkymään voi hakea"}
  tienakyma-haut
  {:toteumat #'hae-toteumat
   :ilmoitukset #'hae-ilmoitukset
   :tarkastukset #'hae-tarkastukset
   :turvallisuuspoikkeamat #'hae-turvallisuuspoikkeamat
   :laatupoikkeamat #'hae-laatupoikkeamat})


(defn- hae-tienakymaan [db user valinnat]
  (when-not (roolit/tilaajan-kayttaja? user)
    (throw+ (roolit/->EiOikeutta "vain tilaajan käyttäjille")))
  (let [parametrit (hakuparametrit valinnat)]
    (reset! debug-hakuparametrit parametrit)
    (fmap (fn [haku-fn]
            (haku-fn db parametrit))
          tienakyma-haut)))

(defrecord Tienakyma []
  component/Lifecycle
  (start [{db :db http :http-palvelin :as this}]
    (julkaise-palvelut
     http
     :hae-tienakymaan (fn [user valinnat]
                        (hae-tienakymaan db user valinnat)))
    this)

  (stop [{http :http-palvelin :as this}]
    (poista-palvelut http :hae-tienakymaan)
    this))
