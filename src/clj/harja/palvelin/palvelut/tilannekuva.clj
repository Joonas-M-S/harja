(ns harja.palvelin.palvelut.tilannekuva
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin
             :refer [julkaise-palvelu poista-palvelut]]

            [harja.domain.ilmoitukset :as ilmoitukset-domain]
            [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.hallintayksikot :as hal-q]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.tilannekuva :as q]
            [harja.palvelin.palvelut.urakat :as urakat]

            [harja.domain.laadunseuranta :as laadunseuranta]
            [harja.geo :as geo]
            [harja.pvm :as pvm]
            [harja.domain.tilannekuva :as tk]
            [harja.ui.kartta.esitettavat-asiat
             :as esitettavat-asiat
             :refer [kartalla-esitettavaan-muotoon-xf]]
            [harja.palvelin.palvelut.karttakuvat :as karttakuvat]
            [clojure.set :refer [union]]
            [harja.transit :as transit]
            [harja.kyselyt.turvallisuuspoikkeamat :as turvallisuuspoikkeamat-q]
            [harja.domain.oikeudet :as oikeudet]
            [clojure.core.async :as async]
            [clojure.java.jdbc :as jdbc]
            [harja.domain.roolit :as roolit]))

(defn tulosta-virhe! [asiat e]
  (log/error (str "*** ERROR *** Yritettiin hakea tilannekuvaan " asiat
                  ", mutta virhe tapahtui: " (.getMessage e))))

(defn tulosta-tulos! [asiaa tulos]
  (if (vector? tulos)
    (log/debug (str "  - " (count tulos) " " asiaa))
    (log/debug (str "  - " (count (keys tulos)) " " asiaa)))
  tulos)

(defn haettavat [s]
  (into #{}
        (map (comp :nimi tk/suodattimet-idlla))
        s))

(defn- hae-ilmoitukset
  [db user {:keys [toleranssi] {:keys [tyypit]} :ilmoitukset :as tiedot} urakat]
  (when-not (empty? urakat)
    (let [haettavat (haettavat tyypit)]
     (when-not (empty? haettavat)
       (mapv
         #(assoc % :uusinkuittaus
                   (when-not (empty? (:kuittaukset %))
                     (:kuitattu (last (sort-by :kuitattu (:kuittaukset %))))))
         (let [ilmoitukset (konv/sarakkeet-vektoriin
                 (into []
                       (comp
                         (geo/muunna-pg-tulokset :sijainti)
                         (map konv/alaviiva->rakenne)
                         (map #(konv/string->keyword % :tila))
                         (map #(assoc % :urakkatyyppi (keyword (:urakkatyyppi %))))
                         (map #(konv/array->vec % :selitteet))
                         (map #(assoc % :selitteet (mapv keyword (:selitteet %))))
                         (map #(assoc-in
                                %
                                [:kuittaus :kuittaustyyppi]
                                (keyword (get-in % [:kuittaus :kuittaustyyppi]))))
                         (map #(assoc % :ilmoitustyyppi (keyword (:ilmoitustyyppi %))))
                         (map #(assoc-in % [:ilmoittaja :tyyppi]
                                         (keyword (get-in % [:ilmoittaja :tyyppi])))))
                       (q/hae-ilmoitukset db
                                          toleranssi
                                          (konv/sql-date (:alku tiedot))
                                          (konv/sql-date (:loppu tiedot))
                                          urakat
                                          (mapv name haettavat)))
                 {:kuittaus :kuittaukset})]
           ilmoitukset))))))

(defn- hae-paallystystyot
  [db user {:keys [toleranssi alku loppu yllapito nykytilanne?]} urakat]
  ;; Muut haut toimivat siten, että urakat parametrissa on vain urakoita, joihin
  ;; käyttäjällä on oikeudet. Jos lista on tyhjä, urakoita ei ole joko valittuna,
  ;; tai yritettiin hakea urakoilla, joihin käyttäjällä ei ole oikeuksia.
  ;; Ylläpidon hommat halutaan hakea, vaikkei valittuna olisikaan yhtään urakkaa.
  ;; Lista voi siis tulla tänne tyhjänä. Jos näin on, täytyy tarkastaa, onko käyttäjällä
  ;; yleistä tilannekuvaoikeutta.
  (when (or (not (empty? urakat)) (oikeudet/voi-lukea? (if nykytilanne?
                                                         oikeudet/tilannekuva-nykytilanne
                                                         oikeudet/tilannekuva-historia)
                                                       nil user))
    (when (tk/valittu? yllapito tk/paallystys)
      (into []
            (comp
              (geo/muunna-pg-tulokset :sijainti)
              (map konv/alaviiva->rakenne)
              (map #(konv/string-polusta->keyword % [:paallystysilmoitus :tila])))
            (if nykytilanne?
              (q/hae-paallystykset-nykytilanteeseen db toleranssi)
              (q/hae-paallystykset-historiakuvaan db
                                                  toleranssi
                                                  (konv/sql-date loppu)
                                                  (konv/sql-date alku)))))))

(defn- hae-paikkaustyot
  [db user {:keys [toleranssi alku loppu yllapito nykytilanne?]} urakat]
  ;; Muut haut toimivat siten, että urakat parametrissa on vain urakoita, joihin
  ;; käyttäjällä on oikeudet. Jos lista on tyhjä, urakoita ei ole joko valittuna,
  ;; tai yritettiin hakea urakoilla, joihin käyttäjällä ei ole oikeuksia.
  ;; Ylläpidon hommat halutaan hakea, vaikkei valittuna olisikaan yhtään urakkaa.
  ;; Lista voi siis tulla tänne tyhjänä. Jos näin on, täytyy tarkastaa, onko käyttäjällä
  ;; yleistä tilannekuvaoikeutta.
  (when (or (not (empty? urakat)) (oikeudet/voi-lukea? (if nykytilanne?
                                                         oikeudet/tilannekuva-nykytilanne
                                                         oikeudet/tilannekuva-historia)
                                                       nil user))
    (when (tk/valittu? yllapito tk/paikkaus)
     (into []
           (comp
             (geo/muunna-pg-tulokset :sijainti)
             (map konv/alaviiva->rakenne)
             (map #(konv/string-polusta->keyword % [:paikkausilmoitus :tila])))
           (if nykytilanne?
             (q/hae-paikkaukset-nykytilanteeseen db toleranssi)
             (q/hae-paikkaukset-historiakuvaan db
                                               toleranssi
                                               (konv/sql-date loppu)
                                               (konv/sql-date alku)))))))

(defn- hae-laatupoikkeamat
  [db user {:keys [toleranssi alku loppu laatupoikkeamat nykytilanne?]} urakat]
  (when-not (empty? urakat)
    (let [haettavat (haettavat laatupoikkeamat)]
     (when-not (empty? haettavat)
       (into []
             (comp
               (map konv/alaviiva->rakenne)
               (map #(update-in % [:paatos :paatos]
                                (fn [p]
                                  (when p (keyword p)))))
               (remove (fn [lp]
                         (if nykytilanne?
                           (#{:hylatty :ei_sanktiota} (get-in lp [:paatos :paatos]))
                           false)))
               (map #(assoc % :selvitys-pyydetty (:selvityspyydetty %)))
               (map #(dissoc % :selvityspyydetty))
               (map #(assoc % :tekija (keyword (:tekija %))))
               (map #(update-in % [:paatos :kasittelytapa]
                                (fn [k]
                                  (when k (keyword k)))))
               (map #(if (nil? (:kasittelyaika (:paatos %)))
                      (dissoc % :paatos)
                      %))
               (geo/muunna-pg-tulokset :sijainti))
             (q/hae-laatupoikkeamat db toleranssi urakat
                                    (konv/sql-date alku)
                                    (konv/sql-date loppu)
                                    (map name haettavat)))))))

(defn- hae-turvallisuuspoikkeamat
  [db user {:keys [toleranssi alku loppu turvallisuus]} urakat]
  (when-not (empty? urakat)
    (when (tk/valittu? turvallisuus tk/turvallisuuspoikkeamat)
     (let [tulos (konv/sarakkeet-vektoriin
                   (into []
                         turvallisuuspoikkeamat-q/turvallisuuspoikkeama-xf
                         (q/hae-turvallisuuspoikkeamat db toleranssi urakat (konv/sql-date alku)
                                                       (konv/sql-date loppu)))
                   {:korjaavatoimenpide :korjaavattoimenpiteet})]
       tulos))))

(defn- hae-tyokoneet
  [db user
   {:keys [alue alku loppu talvi kesa urakka-id hallintayksikko nykytilanne?
           yllapito toleranssi] :as optiot}
   urakat]
  (when-not (empty? urakat)
    (when nykytilanne?
      (let [yllapito (filter tk/yllapidon-reaaliaikaseurattava? yllapito)
            haettavat-toimenpiteet (haettavat (union talvi kesa yllapito))
            urakoitsija? (= :urakoitsija (roolit/osapuoli user))]
        (when (not (empty? haettavat-toimenpiteet))
          (let [tpi-haku-str (konv/seq->array haettavat-toimenpiteet)
                parametrit (merge alue
                                  {:urakat urakat
                                   :toleranssi toleranssi
                                   :nayta-kaikki (not urakoitsija?)
                                   :organisaatio (get-in user [:organisaatio :id])
                                   :toimenpiteet tpi-haku-str})]
            (into {}
                  (comp
                   (map #(update-in % [:sijainti] (comp geo/piste-koordinaatit)))
                   (geo/muunna-pg-tulokset :reitti)
                   (map #(update-in % [:edellinensijainti]
                                    (fn [pos] (when pos
                                                (geo/piste-koordinaatit pos)))))
                   (map #(assoc % :tyyppi :tyokone))
                   (map #(konv/array->set % :tehtavat))
                   (map (juxt :tyokoneid identity)))
                  (q/hae-tyokoneet db parametrit))))))))

(defn- toteumien-toimenpidekoodit [db {:keys [talvi kesa]}]
  (let [koodit (some->> (union talvi kesa)
                        haettavat
                        (q/hae-toimenpidekoodit db)
                        (map :id))]
    (if (empty? koodit)
      nil
      koodit)))

(defn- hae-toteumien-reitit
  [db ch user {:keys [toleranssi alue alku loppu] :as tiedot} urakat]
  (when-not (empty? urakat)
    (when-let [toimenpidekoodit (toteumien-toimenpidekoodit db tiedot)]
      (q/hae-toteumat db ch
                      {:toleranssi toleranssi
                       :alku (konv/sql-date alku)
                       :loppu (konv/sql-date loppu)
                       :toimenpidekoodit toimenpidekoodit
                       :urakat urakat
                       :xmin (:xmin alue)
                       :ymin (:ymin alue)
                       :xmax (:xmax alue)
                       :ymax (:ymax alue)}))))

(defn- hae-tarkastusten-reitit
  [db ch user {:keys [toleranssi alue alku loppu tarkastukset] :as tiedot} urakat]
  (when-not (empty? urakat)
    (q/hae-tarkastukset db ch
                        {:toleranssi toleranssi
                         :alku (konv/sql-date alku)
                         :loppu (konv/sql-date loppu)
                         :urakat urakat
                         :xmin (:xmin alue)
                         :ymin (:ymin alue)
                         :xmax (:xmax alue)
                         :ymax (:ymax alue)
                         :tyypit (map name (haettavat tarkastukset))
                         :kayttaja_on_urakoitsija (roolit/urakoitsija? user)})))

(defn- hae-suljetut-tieosuudet
  [db user {:keys [yllapito alue nykytilanne?]} urakat]
  (when (or (not-empty urakat) (oikeudet/voi-lukea? (if nykytilanne?
                                                      oikeudet/tilannekuva-nykytilanne
                                                      oikeudet/tilannekuva-historia)
                                                    nil user))
    (when (tk/valittu? yllapito tk/suljetut-tiet)
      (vec (map (comp #(konv/array->vec % :kaistat)
                      #(konv/array->vec % :ajoradat))
                (q/hae-suljetut-tieosuudet db {:x1 (:xmin alue)
                                               :y1 (:ymin alue)
                                               :x2 (:xmax alue)
                                               :y2 (:ymax alue)}))))))

(defn- hae-toteumien-selitteet
  [db user {:keys [alue alku loppu] :as tiedot} urakat]
  (when-not (empty? urakat)
    (when-let [toimenpidekoodit (toteumien-toimenpidekoodit db tiedot)]
      (q/hae-toteumien-selitteet db
                                 (konv/sql-date alku) (konv/sql-date loppu)
                                 toimenpidekoodit urakat
                                 (:xmin alue) (:ymin alue)
                                 (:xmax alue) (:ymax alue)))))

(def tilannekuvan-osiot
  #{:toteumat :tyokoneet :turvallisuuspoikkeamat
    :laatupoikkeamat :paikkaus :paallystys :ilmoitukset :suljetut-tieosuudet})

(defmulti hae-osio (fn [db user tiedot urakat osio] osio))
(defmethod hae-osio :toteumat [db user tiedot urakat _]
  (tulosta-tulos! "toteuman selitettä"
                  (hae-toteumien-selitteet db user tiedot urakat)))

(defmethod hae-osio :tyokoneet [db user tiedot urakat _]
  (tulosta-tulos! "tyokonetta"
                  (hae-tyokoneet db user tiedot urakat)))

(defmethod hae-osio :turvallisuuspoikkeamat [db user tiedot urakat _]
  (tulosta-tulos! "turvallisuuspoikkeamaa"
                  (hae-turvallisuuspoikkeamat db user tiedot urakat)))

(defmethod hae-osio :laatupoikkeamat [db user tiedot urakat _]
  (tulosta-tulos! "laatupoikkeamaa"
                  (hae-laatupoikkeamat db user tiedot urakat)))

(defmethod hae-osio :paikkaus [db user tiedot urakat _]
  (tulosta-tulos! "paikkausta"
                  (hae-paikkaustyot db user tiedot urakat)))

(defmethod hae-osio :paallystys [db user tiedot urakat _]
  (tulosta-tulos! "paallystysta"
                  (hae-paallystystyot db user tiedot urakat)))

(defmethod hae-osio :ilmoitukset [db user tiedot urakat _]
  (tulosta-tulos! "ilmoitusta"
                  (hae-ilmoitukset db user tiedot urakat)))

(defmethod hae-osio :suljetut-tieosuudet [db user tiedot urakat _]
  (tulosta-tulos! "suljettua tieosuutta"
                  (hae-suljetut-tieosuudet db user tiedot urakat)))

(defn yrita-hakea-osio [db user tiedot urakat osio]
  (try
    (hae-osio db user tiedot urakat osio)
    (catch Exception e
      (tulosta-virhe! (name osio) e)
      nil)))

(defn hae-urakat [db user tiedot]
  (urakat/kayttajan-urakat-aikavalilta-alueineen
   db user (if (:nykytilanne? tiedot)
             oikeudet/tilannekuva-nykytilanne
             oikeudet/tilannekuva-historia)
   nil (:urakoitsija tiedot) nil
   nil (:alku tiedot) (:loppu tiedot)))

(defn hae-tilannekuvaan
  ([db user tiedot]
   (hae-tilannekuvaan db user tiedot tilannekuvan-osiot))
  ([db user tiedot osiot]
   (let [urakat (filter #(oikeudet/voi-lukea? (if (:nykytilanne? tiedot)
                                                oikeudet/tilannekuva-nykytilanne
                                                oikeudet/tilannekuva-historia) % user)
                        (:urakat tiedot))]
     (log/debug "Haetaan tilannekuvaan asioita urakoista " (pr-str urakat))
     (let [tiedot (assoc tiedot :toleranssi (geo/karkeistustoleranssi (:alue tiedot)))]
       (into {}
             (map (juxt identity (partial yrita-hakea-osio db user tiedot urakat)))
             osiot)))))

(defn- aikavalinta
  "Jos annettu suhteellinen aikavalinta tunteina, pura se :alku ja :loppu avaimiksi."
  [{aikavalinta :aikavalinta :as hakuparametrit}]
  (if-not aikavalinta
    hakuparametrit
    (let [loppu (java.util.Date.)
          alku (java.util.Date. (- (System/currentTimeMillis)
                                   (* 1000 60 60 aikavalinta)))]
      (assoc hakuparametrit
             :alku alku
             :loppu loppu))))

(defn- karttakuvan-suodattimet
  "Tekee karttakuvan URL parametreistä suodattimet"
  [{:keys [extent parametrit]}]
  (let [[x1 y1 x2 y2] extent
        hakuparametrit (some-> parametrit (get "tk") transit/lue-transit-string aikavalinta)]
    (as-> hakuparametrit p
          (merge p
                 {:alue {:xmin x1 :ymin y1
                         :xmax x2 :ymax y2}})
          (assoc p :toleranssi (geo/karkeistustoleranssi (:alue p))))))

(defn- luettavat-urakat [user tiedot]
  (filter #(oikeudet/voi-lukea? (if (:nykytilanne? tiedot)
                                  oikeudet/tilannekuva-nykytilanne
                                  oikeudet/tilannekuva-historia) % user)
          (:urakat tiedot)))

(defn- hae-karttakuvan-tiedot [db user parametrit haku-fn xf ]
  (let [tiedot (karttakuvan-suodattimet parametrit)
        kartalle-xf (kartalla-esitettavaan-muotoon-xf)
        ch (async/chan 32
                       (comp
                         (map konv/alaviiva->rakenne)
                         xf
                         kartalle-xf))
        urakat (luettavat-urakat user tiedot)]
    (async/thread
      (jdbc/with-db-transaction [db db
                                 :read-only? true]
        (try (haku-fn db ch user tiedot urakat)
             (catch Throwable t
               (println t "Virhe haettaessa tilannekuvan karttatietoja")
               (throw t)))))
    ch))

(defn- hae-toteumat-kartalle [db user parametrit]
  (hae-karttakuvan-tiedot db user parametrit hae-toteumien-reitit
                          (map #(assoc %
                                       :tyyppi :toteuma
                                       :tyyppi-kartalla :toteuma
                                       :tehtavat [(:tehtava %)]))))


(defn- hae-toteumat-asiat-kartalle [db user {tk "tk" :as params}]
  (konv/sarakkeet-vektoriin
   (into []
         (comp
          (map konv/alaviiva->rakenne)
          (map #(assoc % :tyyppi-kartalla :toteuma)))
         (q/hae-toteumien-asiat db
                                (as-> tk p
                                  (java.net.URLDecoder/decode p)
                                  (transit/lue-transit-string p)
                                  (assoc p :urakat (luettavat-urakat user p))
                                  (assoc p :toimenpidekoodit (toteumien-toimenpidekoodit db p))
                                  (aikavalinta p)
                                  (merge p (select-keys params [:x :y])))))
   {:tehtava :tehtavat}))

(defn- hae-tarkastukset-kartalle [db user parametrit]
  (hae-karttakuvan-tiedot db user parametrit hae-tarkastusten-reitit
                          (comp (map laadunseuranta/tarkastus-tiedolla-onko-ok)
                                (map #(konv/string->keyword % :tyyppi :tekija))
                                (map #(assoc %
                                             :tyyppi-kartalla :tarkastus
                                             :sijainti (:reitti %))))))

(defn- hae-tarkastuksien-asiat-kartalle [db user {x :x y :y parametrit "tk"}]
  (into []
        (comp (map #(assoc % :tyyppi-kartalla :tarkastus))
              (map #(konv/string->keyword % :tyyppi)))
        (q/hae-tarkastusten-asiat db
                                  (as-> parametrit p
                                    (java.net.URLDecoder/decode parametrit)
                                    (transit/lue-transit-string p)

                                    (aikavalinta p)
                                    (assoc p
                                           :urakat (luettavat-urakat user p)
                                           :tyypit (map name (haettavat (:tarkastukset p)))
                                           :kayttaja_on_urakoitsija (roolit/urakoitsija? user)
                                           :x x :y y :toleranssi 150)))))


(defrecord Tilannekuva []
  component/Lifecycle
  (start [{karttakuvat :karttakuvat
           db :db
           http :http-palvelin
           :as this}]
    (julkaise-palvelu http :hae-tilannekuvaan
                      (fn [user tiedot]
                        (hae-tilannekuvaan db user tiedot)))
    (julkaise-palvelu http :hae-urakat-tilannekuvaan
                      (fn [user tiedot]
                        (hae-urakat db user tiedot)))
    (karttakuvat/rekisteroi-karttakuvan-lahde!
      karttakuvat :tilannekuva-toteumat
      (partial hae-toteumat-kartalle db)
      (partial #'hae-toteumat-asiat-kartalle db))
    (karttakuvat/rekisteroi-karttakuvan-lahde!
     karttakuvat :tilannekuva-tarkastukset
     (partial hae-tarkastukset-kartalle db)
     (partial #'hae-tarkastuksien-asiat-kartalle db))
    this)

  (stop [{karttakuvat :karttakuvat :as this}]
    (poista-palvelut (:http-palvelin this)
                     :hae-tilannekuvaan
                     :hae-urakat-tilannekuvaan)
    (karttakuvat/poista-karttakuvan-lahde! karttakuvat :tilannekuva-toteumat)
    (karttakuvat/poista-karttakuvan-lahde! karttakuvat :tilannekuva-tarkastukset)
    this))
