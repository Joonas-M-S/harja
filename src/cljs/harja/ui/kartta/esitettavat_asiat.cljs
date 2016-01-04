(ns harja.ui.kartta.esitettavat-asiat
  (:require [harja.pvm :as pvm]
            [clojure.string :as str]
            [harja.loki :refer [log warn]]
            [cljs-time.core :as t]
            [harja.tiedot.urakka.laadunseuranta.laatupoikkeamat :as laatupoikkeamat]
            [harja.tiedot.urakka.laadunseuranta.tarkastukset :as tarkastukset]
            [harja.ui.yleiset :refer [karttakuva]]))


(defn- oletusalue [asia valittu?]
  (merge
    (or (:sijainti asia)
        (:sijainti (first (:reittipisteet asia))))
    {:color  (if (valittu? asia) "blue" "green")
     :radius 300
     :stroke {:color "black" :width 10}}))

(defmulti
  ^{:private true}
  asia-kartalle :tyyppi-kartalla)

(defn sisaltaako-kuittauksen? [ilmoitus kuittaustyyppi]
  (some #(= (:kuittaustyyppi %) kuittaustyyppi) (get-in ilmoitus [:kuittaukset])))

(defn ilmoituksen-tooltip [ilmoitus]
  (if (empty? (:kuittaukset ilmoitus))
    "Ei kuittauksia"

    (case (last (map :kuittaustyyppi (:kuittaukset ilmoitus)))
      :vastaanotto "Vastaanottokuittaus annettu"
      :vastaus "Vastauskuittaus annettu"
      :aloitus "Aloituskuittaus annettu"
      :lopetus "Lopetuskuittaus annettu"
      :muutos "Muutoskuittaus annettu"
      nil)))

(defmethod asia-kartalle :tiedoitus [ilmoitus valittu?]
  (let [tooltip (or (ilmoituksen-tooltip ilmoitus) "Tiedotus")]
    [(assoc ilmoitus
       :type :ilmoitus
       :nimi tooltip
       :selite {:teksti "Tiedotus"
                :img    (karttakuva "kartta-tiedotus-violetti")}
       :alue {:type        :tack-icon
              :scale       (if (valittu? ilmoitus) 1.5 1)
              :img         (karttakuva "kartta-tiedotus-violetti")
              :coordinates (get-in ilmoitus [:sijainti :coordinates])})]))

(defmethod asia-kartalle :kysely [ilmoitus valittu?]
  (let [tooltip (or (ilmoituksen-tooltip ilmoitus) "Kysely")
        aloitettu? (sisaltaako-kuittauksen? ilmoitus :aloitus)
        lopetettu? (sisaltaako-kuittauksen? ilmoitus :lopetus)
        ikoni (cond
                lopetettu? (karttakuva "kartta-kysely-violetti") ;; TODO Lisää harmaat ikonit kun valmistuvat.
                aloitettu? (karttakuva "kartta-kysely-violetti")
                :else (karttakuva "kartta-kysely-kesken-punainen"))]
    [(assoc ilmoitus
       :type :ilmoitus
       :nimi tooltip
       :selite {:teksti (cond
                          aloitettu? "Kysely, aloitettu"
                          lopetettu? "Kysely, lopetettu"
                          :else "Kysely, ei aloituskuittausta.")
                :img    ikoni}
       :alue {:type        :tack-icon
              :scale       (if (valittu? ilmoitus) 1.5 1)
              :img         ikoni
              :coordinates (get-in ilmoitus [:sijainti :coordinates])})]))

(defmethod asia-kartalle :toimenpidepyynto [ilmoitus valittu?]
  (let [tooltip (or (ilmoituksen-tooltip ilmoitus) "Toimenpidepyyntö")
        vastaanotettu? (sisaltaako-kuittauksen? ilmoitus :vastaanotettu)
        lopetettu? (sisaltaako-kuittauksen? ilmoitus :lopetus)
        ikoni (cond
                lopetettu? (karttakuva "kartta-toimenpidepyynto-violetti") ;; TODO
                vastaanotettu? (karttakuva "kartta-toimenpidepyynto-violetti")
                :else (karttakuva "kartta-toimenpidepyynto-kesken-punainen"))]
    [(assoc ilmoitus
       :type :ilmoitus
       :nimi tooltip
       :selite {:teksti (cond
                          vastaanotettu? "Toimenpidepyyntö, kuitattu"
                          lopetettu? "Toimenpidepyyntö, lopetettu"
                          :else "Toimenpidepyyntö, kuittaamaton")
                :img    ikoni}
       :alue {:type        :tack-icon
              :scale       (if (valittu? ilmoitus) 1.5 1)
              :img         ikoni
              :coordinates (get-in ilmoitus [:sijainti :coordinates])})]))

(defn selvita-laadunseurannan-ikoni [ikonityyppi tekija]
  (case tekija
    :urakoitsija (str "kartta-" ikonityyppi (karttakuva "-urakoitsija-violetti"))
    :tilaaja (str "kartta-" ikonityyppi (karttakuva "-tilaaja-violetti"))
    :konsultti (str "kartta-" ikonityyppi (karttakuva "-konsultti-violetti"))
    (str "kartta-" ikonityyppi (karttakuva "-violetti"))))

(defn selvita-tarkastuksen-ikoni [tekija]
  (selvita-laadunseurannan-ikoni "tarkastus" tekija))

(defn selvita-laatupoikkeaman-ikoni [tekija]
  (selvita-laadunseurannan-ikoni "havainto" tekija))

(defmethod asia-kartalle :laatupoikkeama [laatupoikkeama valittu?]
  (when-let [sijainti (:sijainti laatupoikkeama)]
    [(assoc laatupoikkeama
            :type :laatupoikkeama
            :nimi (or (:nimi laatupoikkeama)
                      (str "Laatupoikkeama (" (laatupoikkeamat/kuvaile-tekija (:tekija laatupoikkeama)) ")"))
            :selite {:teksti (str "Laatupoikkeama (" (laatupoikkeamat/kuvaile-tekija (:tekija laatupoikkeama)) ")")
                     :img    (selvita-laatupoikkeaman-ikoni (:tekija laatupoikkeama))}
            :alue {:type        :tack-icon
                   :scale       (if (valittu? laatupoikkeama) 1.5 1)
                   :img         (selvita-laatupoikkeaman-ikoni (:tekija laatupoikkeama))
                   :coordinates (if (= :line (get-in laatupoikkeama [:sijainti :type]))
                                  ;; Lopetuspiste. Kai? Ainakin "viimeinen klikkaus" kun käyttää tr-komponenttia
                                  (first (get-in laatupoikkeama [:sijainti :points]))

                                  (get-in laatupoikkeama [:sijainti :coordinates]))})]))

(defmethod asia-kartalle :tarkastus [tarkastus valittu?]
  [(assoc tarkastus
     :type :tarkastus
     :nimi (or (:nimi tarkastus)
               (str (tarkastukset/+tarkastustyyppi->nimi+ (:tyyppi tarkastus)) " (" (laatupoikkeamat/kuvaile-tekija (:tekija tarkastus)) ")"))
     :selite {:teksti (str "Tarkastus (" (laatupoikkeamat/kuvaile-tekija (:tekija tarkastus)) ")")
              :img    (selvita-tarkastuksen-ikoni (:tekija tarkastus))}
     :alue (let [ikoni (selvita-tarkastuksen-ikoni (:tekija tarkastus))
                 skaala (if (valittu? tarkastus) 1.5 1)
                 sijainti (:sijainti tarkastus)]
             (case (:type sijainti)
               :line {:type   :tack-icon-line
                      :scale  skaala
                      :img    ikoni
                      :points (:points sijainti)}
               :multiline {:type :tack-icon-line
                           :scale skaala
                           :img ikoni
                           :points (mapcat :points (:lines sijainti))}
               :point {:type        :tack-icon
                       :scale       skaala
                       :img         ikoni
                       :coordinates (:coordinates sijainti)})))])

(defmethod asia-kartalle :varustetoteuma [varustetoteuma]
  [(assoc varustetoteuma
     :type :varustetoteuma
     :nimi (or (:selitys-kartalla varustetoteuma) "Varustetoteuma")
     :alue {:type        :tack-icon
            :img         (karttakuva "kartta-hairion-hallinta-sininen")
            :coordinates (get-in (first (:reittipisteet varustetoteuma)) [:sijainti :coordinates])})])

(def toteuma-varit-ja-nuolet
  [["rgb(255,0,0)" "punainen"]
   ["rgb(255,128,0)" "oranssi"]
   ["rgb(255,255,0)" "keltainen"]
   ["rgb(255,0,255)" "magenta"]
   ["rgb(0,255,0)" "vihrea"]
   ["rgb(0,255,128)" "turkoosi"]
   ["rgb(0,255,255)" "syaani"]
   ["rgb(0,128,255)" "sininen"]
   ["rgb(0,0,255)" "tummansininen"]
   ["rgb(128,0,255)" "violetti"]
   ["rgb(128,255,0)" "lime"]   
   ["rgb(255,0,128)" "pinkki"]])

(let [varien-lkm (count toteuma-varit-ja-nuolet)]
  (defn tehtavan-vari-ja-nuoli [tehtavan-nimi]
    (nth toteuma-varit-ja-nuolet (Math/abs (rem (hash tehtavan-nimi) varien-lkm)))))

(defmethod asia-kartalle :toteuma [toteuma valittu?]
  ;; Yhdellä reittipisteellä voidaan tehdä montaa asiaa, ja tämän takia yksi reittipiste voi tulla
  ;; monta kertaa fronttiin.
  (let [reittipisteet (keep
                        (fn [[_ arvo]] (first arvo))
                        (group-by :id (:reittipisteet toteuma)))
        nimi (or (get-in toteuma [:tehtavat 0 :nimi])
                 (get-in toteuma [:reittipisteet 0 :tehtava :toimenpide]))
        [vari nuoli] (tehtavan-vari-ja-nuoli nimi)]
    (when-not (empty? reittipisteet)
      [(assoc toteuma
               :type :toteuma
               :nimi (or (:nimi toteuma)
                         nimi
                         (get-in toteuma [:tpi :nimi])
                         (if (> 1 (count (:tehtavat toteuma)))
                           (str (:toimenpide (first (:tehtavat toteuma))) " & ...")
                           (str (:toimenpide (first (:tehtavat toteuma))))))
               :selite {:teksti nimi
                        :vari vari}
               :alue {:type   :arrow-line
                      :width 5
                      :color vari
                      :arrow-image (karttakuva (str "images/nuoli-" nuoli))
                      :scale  (if (valittu? toteuma) 2 1.5) ;; TODO: Vaihda tämä joksikin paremmaksi kun saadaan oikeat ikonit :)
                      :points (mapv #(get-in % [:sijainti :coordinates])
                                    (sort-by :aika pvm/ennen? reittipisteet))})])))

(defn paattele-turpon-ikoni [turpo]
  (let [kt (:korjaavattoimenpiteet turpo)]
    (if (empty? kt)
      [(karttakuva "kartta-turvallisuuspoikkeama-avoin-oranssi") "Turvallisuuspoikkeama, avoin"]

      (if (some (comp nil? :suoritettu) kt)
        [(karttakuva "kartta-turvallisuuspoikkeama-ei-toteutettu-punainen") "Turvallisuuspoikkeama, ei korjauksia"]

        [(karttakuva "kartta-turvallisuuspoikkeama-toteutettu-vihrea") "Turvallisuuspoikkeama, kaikki korjattu"]))))

(defmethod asia-kartalle :turvallisuuspoikkeama [tp valittu?]
  (let [[ikoni selite] (paattele-turpon-ikoni tp)
        sijainti (:sijainti tp)
        tyyppi (:type sijainti)
        skaala (if (valittu? tp) 1.5 1)]
    (when sijainti
      [(assoc tp
              :type :turvallisuuspoikkeama
              :nimi (or (:nimi tp) "Turvallisuuspoikkeama")
              :selite {:teksti selite
                       :img    ikoni}
              :alue (case tyyppi
                      :line
                      {:type   :tack-icon-line
                       :color  "black"
                       :scale  skaala
                       :img    ikoni
                       :points (get-in tp [:sijainti :points])}

                      :multiline
                      {:type :tack-icon-line
                       :color "black"
                       :scale skaala
                       :img ikoni
                       :points (mapcat :points (:lines sijainti))}

                      :point
                      {:type :tack-icon
                       :scale skaala
                       :img ikoni
                       :coordinates (get-in tp [:sijainti :coordinates])}))])))

;; TODO: Päällystyksissä ja paikkauksissa on kommentoitua koodia, koska näille dedikoituijen näkymien käyttämät
;; kyselyt palauttavat datan sellaisessa muodossa, että sijainti pitää kaivaa erikseen "kohdeosista".
;; Tilannekuvassa tämä sijaintitieto palautetaan suoraan samassa kyselyssä. Tilannekuva on tällä hetkellä
;; ainoa paikka jossa piirretään päällystyksiä/paikkauksia tämän namespacen avulla, joten päätettiin toteuttaa
;; metodit uudelleen. Kun päällystys/paikkaus-näkymät laitetaan käyttämään tätä uutta paradigmaa, voidaan joko
;; toteuttaa näille omat metodit TAI miettiä, tarviiko tosiaan näiden käyttämä data palauttaa sellaisessa muodossa?
(defmethod asia-kartalle :paallystys [pt valittu?]
  [(assoc pt
     :type :paallystys
     :nimi (or (:nimi pt) "Päällystys")
     :alue (:sijainti pt))]

  #_(mapv
      (fn [kohdeosa]
        (assoc kohdeosa
          :type :paallystys
          :nimi (or (:nimi pt) "Päällystyskohde")
          :alue (:sijainti kohdeosa)))
      (:kohdeosat pt)))

(defmethod asia-kartalle :paikkaus [pt valittu?]
  [(assoc pt
     :type :paikkaus
     :nimi (or (:nimi pt) "Paikkaus")
     :alue (:sijainti pt))]
  #_(mapv
      (fn [kohdeosa]
        (assoc kohdeosa
          :type :paikkaus
          :nimi (or (:nimi pt) "Paikkaus")
          :alue (:sijainti kohdeosa)))
      (:kohdeosat pt)))

(defn- paattele-tyokoneen-ikoni
  [tehtavat lahetetty valittu?]
  ;; TODO Miten päätellään järkevästi mikä ikoni työkoneelle näytetään?
  ;; Ensinnäkin, en ole yhtään varma osuuko nämä suoritettavat tehtävät edes oikeanlaisiin ikoneihin
  ;; Mutta tärkempää on, että työkoneella voi olla useampi tehtävä. Miten se hoidetaan?
  ;; Voisi kuvitella että jotkut tehtävät ovat luonnostaan kiinnostavampia,
  ;; Esim jos talvella aurataan paljon mutta suolataan vain vähän (ja yleensä aurataan kun suolataan),
  ;; niin silloin pitäisi näyttää suolauksen ikoni silloin harvoin kun sitä tehdään.
  (let [ikonikartta {"auraus ja sohjonpoisto"          ["talvihoito" "Talvihoito"]
                     "suolaus"                         ["talvihoito" "Talvihoito"]
                     "pistehiekoitus"                  ["talvihoito" "Talvihoito"]
                     "linjahiekoitus"                  ["talvihoito" "Talvihoito"]
                     "lumivallien madaltaminen"        ["talvihoito" "Talvihoito"]
                     "sulamisveden haittojen torjunta" ["talvihoito" "Talvihoito"]
                     "kelintarkastus"                  ["talvihoito" "Talvihoito"]

                     "tiestotarkastus"                 ["liikenneympariston-hoito" "Liikenneympäristön hoito"]
                     "koneellinen niitto"              ["liikenneympariston-hoito" "Liikenneympäristön hoito"]
                     "koneellinen vesakonraivaus"      ["liikenneympariston-hoito" "Liikenneympäristön hoito"]

                     "liikennemerkkien puhdistus"      ["varusteet-ja-laitteet" "Varusteet ja laitteet"]

                     "sorateiden muokkaushoylays"      ["sorateiden-hoito" "Sorateiden hoito"]
                     "sorateiden polynsidonta"         ["sorateiden-hoito" "Sorateiden hoito"]
                     "sorateiden tasaus"               ["sorateiden-hoito" "Sorateiden hoito"]
                     "sorastus"                        ["sorateiden-hoito" "Sorateiden hoito"]

                     "harjaus"                         ["paallysteiden-yllapito" "Päällysteiden ylläpito"]
                     "pinnan tasaus"                   ["paallysteiden-yllapito" "Päällysteiden ylläpito"]
                     "paallysteiden paikkaus"          ["paallysteiden-yllapito" "Päällysteiden ylläpito"]
                     "paallysteiden juotostyot"        ["paallysteiden-yllapito" "Päällysteiden ylläpito"]

                     "siltojen puhdistus"              ["sillat" "Sillat"]

                     "l- ja p-alueiden puhdistus"      ["hairion-hallinta" "Häiriön hallinta"] ;; En tiedä yhtään mikä tämä on
                     "muu"                             ["hairion-hallinta" "Häiriön hallinta"]}
        tila (cond
               valittu? "valittu"
               (and lahetetty (t/before? lahetetty (t/now)) (> 20 (t/in-minutes (t/interval lahetetty (t/now)))))
               "sininen"
               :else "harmaa")
        [ikoni selite] (or (get ikonikartta (first tehtavat)) ["hairion-hallinta" "Häiriön hallinta"])]
    [(karttakuva (str "kartta-" ikoni "-" tila)) (karttakuva (str "kartta-" ikoni "-sininen")) selite]))

(defmethod asia-kartalle :tyokone [tyokone valittu?]
  (let [[img selite-img selite-teksti] (paattele-tyokoneen-ikoni
                                         (:tehtavat tyokone)
                                         (or (:lahetysaika tyokone) (:vastaanotettu tyokone))
                                         (valittu? tyokone))]
    [(assoc tyokone
       :type :tyokone
       :nimi (or (:nimi tyokone) (str/capitalize (name (:tyokonetyyppi tyokone))))
       :selite {:teksti selite-teksti
                :img    [(karttakuva "kartta-suuntanuoli-sininen") selite-img]}
       :alue (if-let [reitti (:reitti tyokone)]
               {:type      :sticker-icon-line
                :points    reitti
                :direction (+ (- Math/PI) (* (/ Math/PI 180) (:suunta tyokone)))
                :img       img}
               {:type        :sticker-icon
                :coordinates (:sijainti tyokone)
                :direction   (+ (- Math/PI) (* (/ Math/PI 180) (:suunta tyokone)))
                :img         img}))]))

(defmethod asia-kartalle :default [asia _]
  (warn "Kartalla esitettävillä asioilla pitää olla :tyyppi-kartalla avain!, sain: " (pr-str asia))
  nil)

(defn- valittu? [valittu tunniste asia]
  (and
    (not (nil? valittu))
    (= (get-in asia tunniste) (get-in valittu tunniste))))

;; Palauttaa joukon vektoreita joten kutsu (mapcat kartalla-xf @jutut)
;; Tämä sen takia, että aiemmin toteumille piirrettiin "itse toteuma" viivana, ja jokaiselle reittipisteelle
;; oma merkki. Tästä luovuttiin, mutta pidetään vielä kiinni siitä että täältä palautetaan joukko vektoreita,
;; jos vastaisuudessa tulee samankaltaisia tilanteita.
(defn kartalla-xf
  ([asia] (kartalla-xf asia nil nil))
  ([asia valittu] (kartalla-xf asia valittu [:id]))
  ([asia valittu tunniste] (asia-kartalle asia (partial valittu? valittu tunniste))))

(defn kartalla-esitettavaan-muotoon
  ([asiat] (kartalla-esitettavaan-muotoon asiat nil nil))
  ([asiat valittu] (kartalla-esitettavaan-muotoon asiat valittu [:id]))
  ([asiat valittu tunniste]
   (kartalla-esitettavaan-muotoon asiat valittu tunniste nil))
  ([asiat valittu tunniste asia-xf]
   (into []
         (comp (or asia-xf identity)
               (mapcat #(kartalla-xf % valittu tunniste)))
         asiat)))
