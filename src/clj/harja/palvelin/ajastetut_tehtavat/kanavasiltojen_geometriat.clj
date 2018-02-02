(ns harja.palvelin.ajastetut-tehtavat.kanavasiltojen-geometriat
  (:require [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]
            [harja.palvelin.tyokalut.ajastettu-tehtava :as ajastettu-tehtava]
            [harja.kyselyt.geometriapaivitykset :as q-geometriapaivitykset]
            [harja.pvm :as pvm]
            [clj-time.coerce :as c]
            [harja.palvelin.integraatiot.integraatioloki :as loki]
            [harja.palvelin.integraatiot.integraatiotapahtuma :as integraatiotapahtuma]
            [cheshire.core :as cheshire]
            [clojure.java.jdbc :as jdbc]
            [harja.kyselyt.kanavat.kanavasillat :as q-kanavasillat]
            [harja.geo :as geo]
            [harja.kyselyt.konversio :as konv]
            [clojure.data.json :as json]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import (org.postgis Point)))

;; Kanavasillat täydentävät kanavasulkuja. Molemmat ovat kanavakokonaisuuden kohteen osia.
;; Yhteen kohteeseen voi kuulua esimerkiksi kaksi osaa: silta ja sulku.
;; Kanavakokonaisuuden muodostaa.
(def geometriapaivitystunnus "kanavasillat")

(def virhekasittely
  {:error-handler #(log/error "Käsittelemätön poikkeus ajastetussa tehtävässä:" %)})

(def avattavat-siltatyypit {
                            :teraksinen-kaantosilta "Teräksinen kääntösilta"
                            :teraksinen-kaantosilta-teraskansi "Teräksinen kääntösilta, teräskantinen"
                            :teraksinen-kantosilta-puukansi "Teräksinen kääntösilta, puukantinen"
                            :teraksinen-lappasilta-teraskansi "Teräksinen läppäsilta, teräskantinen"
                            :teraksinen-lappasilta-terasbetonikansi "Teräksinen läppäsilta, teräsbetonikantinen"
                            :teraksinen-nostosilta "Teräksinen nostosilta"
                            :teraksinen-nostosilta-teraskansi "Teräksinen nostosilta, teräskantinen"
                            :terasbetoninen-ponttonisilta "Teräsbetoninen ponttonisilta"

                            ;; testataan
                            :teraksinen-langer-palkkisilta-teraskansi "Teräksinen langerpalkkisilta, teräskantinen"
                            :teraksinen-langer-palkkisilta-terasbetonikansi "Teräksinen langerpalkkisilta, teräsbetonikantinen"
                            :teraksinen-jatkuva-palkkisilta-terasbetonikansi "Teräksinen jatkuva palkkisilta, teräsbetonikantinen"
                            :teraksinen-palkkisilta-terasbetonikansi-liittorakenne "Teräksinen palkkisilta, teräsbetonikantinen, liittorakenteinen"
                            :teraksinen-palkkisilta-terasbetonikansi "Teräksinen palkkisilta, teräsbetonikantinen"
                            :teraksinen-jatkuva-kotelopalkkisilta "Teräksinen jatkuva kotelopalkkisilta"
                            :teraksinen-ristikkosilta-teraskansi "Teräksinen ristikkosilta,teräskantinen"

                            ;;Jotkut avattavat sillat ovat tämän tyyppisiä, mutta näiden perusteella suodattaessa jäljelle jää paljon turhia siltojas
                            ;;
                            ;;:teraksinen-jatkuva-palkkisilta-terasbetonikansi "Teräksinen jatkuva palkkisilta, teräsbetonikantinen"
                            ;;:teraksinen-jatkuva-kotelopalkkisilta "Teräksinen jatkuva kotelopalkkisilta"
                            ;;:teraksinen-liittorakenteinen-ulokepalkkisilta-terasbetonikansi "Teräksinen ulokepalkkisilta,teräsbetonikantinen,liittorakenteinen"
                            ;;:teraksinen-ristikkosilta-teraskansi "Teräksinen ristikkosilta,teräskantinen"
                            ;;:teraksinen-levypalkkisilta-ajorata-ylhaalla "Teräksinen levypalkkisilta, ajorata ylhäällä"
                            ;;:teraksinen-ristikkosilta-ajorata-ylhaalla "Teräksinen ristikkosilta, ajorata ylhäällä"
                            })

;; Kovakoodaus. Osa silloista on tyypiltään sellaisia, ettei niitä tunnista avattaviksi. TREX:istä ei saa tarpeisiimme
(def avattavat-sillat {
                       :Pohjanlahti 1151
                       :Kellosalmi 2724
                       :Itikka 234
                       :Uimasalmi 1148
                       :Kaltimokoski 1219
                       :Kyrönsalmi 2619
                       :Uimasalmen-rata 2621
                       })

(def kanavasiltatunnukset {:kanavasilta "KaS"})

(def poistetut-siltatilat {:poistettu "poistettu"
                           :purettu "purettu"
                           :liikennointi-lopetettu "liikennointi-lopetettu"})

(defn onko-silta-poistettu? [elinkaaritila]
  (if (= (some #(= elinkaaritila %) (vals poistetut-siltatilat)) nil) false true))

(defn paivitys-tarvitaan? [db paivitysvali-paivissa]
  (let [viimeisin-paivitys (c/from-sql-time
                             (:viimeisin_paivitys
                               (first (q-geometriapaivitykset/hae-paivitys db geometriapaivitystunnus))))]
    (or (nil? viimeisin-paivitys)
        (>= (pvm/paivia-valissa viimeisin-paivitys (pvm/nyt-suomessa)) paivitysvali-paivissa))))


(defn muunna-mapiksi [kanavasilta-osoite]
  (map (fn [[k v]]
         [(keyword k) v])
       (into [] kanavasilta-osoite)))

(defn muunna-tallennettavaan-muotoon [tr-osoite]
  (str "'(" (:tie tr-osoite) "," (:osa tr-osoite) "," (:etaisyys tr-osoite) ",,," (:ajorata tr-osoite) ",,,,) ::TR_OSOITE_LAAJENNETTU'"))

(defn muuta-tr-osoitteiksi [kanavasilta-osoite]
  {:tie (kanavasilta-osoite :tie)
   :aosa (kanavasilta-osoite :alku)
   :aet (kanavasilta-osoite :etaisyys)
   :losa
   :let
   :ajorata (kanavasilta-osoite :ajorata)
   :kaista
   :puoli
   :karttapvm
   :geometria}
  )

;; TODO: MUODOSTA TR-OSOITE LAAJENNOS TYYPPINEN tyypitetty array ja tallenna kan_silta-tauluun.
;; Avattavat sillat haetaan TREX:sta. TREX:in (= taitorakennerekisteri) rajapinnan kuvaus on liitetty tikettiin HAR-6948.

(defn tallenna-kanavasilta [db kanavasilta]
  (let [siltanro (kanavasilta :siltanro)
        nimi (kanavasilta :siltanimi)
        tunnus (kanavasilta :tunnus_prefix)
        kayttotarkoitus (when (kanavasilta :d_kayttotar_koodi) (konv/seq->array (kanavasilta :d_kayttotar_koodi)))
        tila (kanavasilta :elinkaaritila)
        pituus (kanavasilta :siltapit)
        rakennetiedot (when (kanavasilta :rakennety) (konv/seq->array (kanavasilta :rakennety)))
        tieosoitteet nil                                    ;(when (kanavasilta :tieosoitteet) (konv/seq->array (map #((muunna-tallennettavaan-muotoon (muunna-mapiksi %))) (kanavasilta :tieosoitteet)) ))
        sijainti_lev (kanavasilta :sijainti_n)
        sijainti_pit (kanavasilta :sijainti_e)
        avattu (when (kanavasilta :avattuliikenteellepvm) (konv/unix-date->java-date (kanavasilta :avattuliikenteellepvm)))
        trex_muutettu (when (kanavasilta :muutospvm) (konv/unix-date->java-date (kanavasilta :muutospvm)))
        trex_oid (kanavasilta :trex_oid)
        trex_sivu (kanavasilta :sivu)
        poistettu (onko-silta-poistettu? (kanavasilta :elinkaaritila))
        sql-parametrit {:siltanro siltanro
                        :nimi nimi
                        :tunnus tunnus
                        :kayttotarkoitus kayttotarkoitus
                        :tila tila
                        :pituus pituus
                        :rakennetiedot rakennetiedot
                        :tieosoitteet tieosoitteet
                        :sijainti_lev sijainti_lev
                        :sijainti_pit sijainti_pit
                        :avattu avattu
                        :trex_muutettu trex_muutettu
                        :trex_oid trex_oid
                        :trex_sivu trex_sivu
                        :luoja "Integraatio"
                        :muokkaaja "Integraatio"
                        :poistettu poistettu}]
    (q-kanavasillat/luo-kanavasilta<! db sql-parametrit)))



(defn kasittele-kanavasillat [db kanavasillat]
  (jdbc/with-db-transaction [db db]
                            (doseq [kanavasilta kanavasillat]
                              (tallenna-kanavasilta db kanavasilta))
                            (q-geometriapaivitykset/paivita-viimeisin-paivitys db geometriapaivitystunnus (harja.pvm/nyt))))


(defn suodata-avattavat-sillat [vastaus]
  (filter #(not-empty (set/intersection
                        (set (vals avattavat-siltatyypit))
                        (set (% :rakennety))))
          (vastaus :tulokset)))

(defn suodata-sillat [vastaus]
  (filter #(not-empty (set/intersection
                        (set (vals avattavat-sillat))
                        (set (% :siltanro))))
          (vastaus :tulokset)))

(defn muodosta-sivutettu-url [url sivunro]
  (clojure.string/replace url #"%1" (str sivunro)))


(defn paivita-kanavasillat [integraatioloki db url]
  "Hakee kanavasillat Taitorakennerekisteristä. Kutsu tehdään 25 kertaa. Yli 24 000 siltaa haetaan sivu kerrallaan.
   Yhdellä sivulla palautuu 1000 siltaa. Jos yksi kutsu epäonnistuu, koko integraatioajo epäonnistuu, eikä mitään päivitetä. "
  (log/debug "Päivitetään kanavasiltojen geometriat")
  (integraatiotapahtuma/suorita-integraatio
    db
    integraatioloki
    "trex"
    "kanavasillat-haku"
    (fn [konteksti]
      (dotimes [num 25] ;; kutsutaan rajapintaa 25 kertaa, jotta kaikki sillta tule
        (let [http-asetukset {:metodi :GET :url (muodosta-sivutettu-url url (+ num 1))} ;; indeksi alkaa nollasta, sivunumerot ykkösestä
              {vastaus :body}(integraatiotapahtuma/laheta konteksti :http http-asetukset)]
          (if vastaus
            (let [data (cheshire/decode vastaus keyword)]
              (kasittele-kanavasillat db (conj (suodata-avattavat-sillat data) (suodata-sillat data))))
            (log/debug (str "Kanavasiltoja ei palautunut. Sivunumero: " (+ num 1))))))))
  (log/debug "Kanavasiltojen päivitys tehty"))


(defn- kanavasiltojen-geometriahakutehtava [integraatioloki db url paivittainen-tarkistusaika paivitysvali-paivissa]
  (log/debug (format "Ajastetaan kanavasiltojen geometrioiden haku tehtäväksi %s päivän väl ein osoitteesta: %s."
                     paivitysvali-paivissa
                     url))
  (when (and paivittainen-tarkistusaika paivitysvali-paivissa url)
    (ajastettu-tehtava/ajasta-paivittain
      paivittainen-tarkistusaika
      (fn [_]
        (when (paivitys-tarvitaan? db paivitysvali-paivissa)
          (paivita-kanavasillat integraatioloki db url))))))

(defrecord KanavasiltojenGeometriahaku [url paivittainen-tarkistusaika paivitysvali-paivissa]
  component/Lifecycle
  (start [{:keys [integraatioloki db] :as this}]
    (log/debug "kanavasiltojen geometriahaku-komponentti käynnistyy")
    (assoc this :kanavasiltojen-geometriahaku
                (kanavasiltojen-geometriahakutehtava
                  integraatioloki
                  db
                  url
                  paivittainen-tarkistusaika
                  paivitysvali-paivissa)))
  (stop [this]
    (when-let [lopeta-fn (:kanavasiltojen-geometriahaku this)]
      (lopeta-fn))
    this))
