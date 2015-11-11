(ns harja.palvelin.ajastetut-tehtavat.geometriapaivitykset
  (:require [taoensso.timbre :as log]
            [chime :refer [chime-ch]]
            [chime :refer [chime-at]]
            [com.stuartsierra.component :as component]
            [clj-time.periodic :refer [periodic-seq]]
            [clj-time.core :as time]
            [clj-time.coerce :as time-coerce]
            [harja.kyselyt.geometriapaivitykset :as geometriapaivitykset]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.alk-komponentti :as alk]
            [harja.pvm :as pvm]
            [harja.palvelin.tyokalut.lukot :as lukko]
            [harja.palvelin.tyokalut.kansio :as kansio]
            [harja.palvelin.tyokalut.arkisto :as arkisto]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.tieverkko :as tieverkon-tuonti]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.sillat :as siltojen-tuonti]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.pohjavesialueet :as pohjavesialueen-tuonti]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.soratien-hoitoluokat :as soratien-hoitoluokkien-tuonti]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.talvihoidon-hoitoluokat :as talvihoidon-tuonti]
            [clojure.java.io :as io]
            [clj-time.coerce :as coerce])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import (java.net URI)
           (java.sql Timestamp)))

(defn aja-alk-paivitys [alk db paivitystunnus kohdepolku kohdetiedoston-polku tiedostourl tiedoston-muutospvm paivitys]
  (log/debug "Geometria-aineisto: " paivitystunnus " on muuttunut ja tarvitaan päivittää")
  (kansio/poista-tiedostot kohdepolku)
  (alk/hae-tiedosto alk (str paivitystunnus "-haku") tiedostourl kohdetiedoston-polku)
  (arkisto/pura-paketti kohdetiedoston-polku)
  (paivitys)
  (geometriapaivitykset/paivita-viimeisin-paivitys<! db tiedoston-muutospvm paivitystunnus)
  (log/debug "Geometriapäivitys: " paivitystunnus " onnistui"))

(defn onko-kohdetiedosto-ok? [kohdepolku kohdetiedoston-nimi]
  (and
    (not (empty kohdepolku))
    (not (empty kohdetiedoston-nimi))
    (.isDirectory (clojure.java.io/file kohdepolku))))

(defn pitaako-paivittaa? [db paivitystunnus tiedoston-muutospvm]
  (let [paivityksen-tiedot (first (geometriapaivitykset/hae-paivitys db paivitystunnus))
        viimeisin-paivitys (:viimeisin_paivitys paivityksen-tiedot)]
    (or (nil? viimeisin-paivitys)
        (pvm/jalkeen?
          (time-coerce/from-sql-time tiedoston-muutospvm)
          (time-coerce/from-sql-time viimeisin-paivitys)))))

(defn tarvitaanko-paikallinen-paivitys? [db paivitystunnus tiedostourl]
  (try
    (let [polku (if (not tiedostourl) nil (.substring (.getSchemeSpecificPart (URI. tiedostourl)) 2))
          tiedosto (if (not polku) nil (io/file polku))
          tiedoston-muutospvm (if (not tiedosto) nil (coerce/to-sql-time (Timestamp. (.lastModified tiedosto))))]
      (if (and
            (not (nil? tiedosto))
            (.exists tiedosto)
            (pitaako-paivittaa? db paivitystunnus tiedoston-muutospvm))
        (do
          (log/debug "Tarvitaan ajaa paikallinen geometriapäivitys:" paivitystunnus)
          true)
        false))
    (catch Exception e
      (log/warn "Tarkistettaessa paikallista ajoa geometriapäivitykselle: " paivitystunnus ", tapahtui poikkeus: " e)
      false)))

(defn kaynnista-alk-paivitys [alk db paivitystunnus tiedostourl kohdepolku kohdetiedoston-nimi paivitys]
  (log/debug "Tarkistetaan onko geometria-aineisto: " paivitystunnus " päivittynyt ALK:ssa.")
  (let [kohdetiedoston-polku (str kohdepolku kohdetiedoston-nimi)]
    ;; todo: tarvii todennäköisesti tehdä tarkempi tarkastus kohdetiedostolle
    (when (and (not-empty tiedostourl) (onko-kohdetiedosto-ok? kohdepolku kohdetiedoston-nimi))
      (try+
        (let [tiedoston-muutospvm (alk/hae-tiedoston-muutospaivamaara alk (str paivitystunnus "-muutospaivamaaran-haku") tiedostourl)
              alk-paivitys (fn [] (aja-alk-paivitys alk db paivitystunnus kohdepolku kohdetiedoston-polku tiedostourl tiedoston-muutospvm paivitys))]
          (if (pitaako-paivittaa? db paivitystunnus tiedoston-muutospvm)
            (lukko/aja-lukon-kanssa db paivitystunnus alk-paivitys)
            (log/debug "Geometria-aineisto: " paivitystunnus ", ei ole päivittynyt viimeisimmän haun jälkeen. Päivitystä ei tehdä.")))
        (catch Exception e
          (log/error "Geometria-aineiston päivityksessä: " paivitystunnus ", tapahtui poikkeus: " e))))))

(defn tee-alkuajastus []
  (time/plus- (time/now) (time/seconds 10)))

(defn ajasta-paivitys [this paivitystunnus tuontivali osoite kohdepolku kohdetiedosto paivitys]
  (log/debug " Ajastetaan geometria-aineiston " paivitystunnus " päivitys ajettavaksi " tuontivali "minuutin välein ")
  (chime-at (periodic-seq (time/now) (-> tuontivali time/minutes))
            (fn [_]
              (kaynnista-alk-paivitys (:alk this) (:db this) paivitystunnus osoite kohdepolku kohdetiedosto paivitys))))

(defn tee-tieverkon-alk-paivitystehtava
  [this {:keys [tuontivali
                tieosoiteverkon-alk-osoite
                tieosoiteverkon-alk-tuontikohde
                tieosoiteverkon-shapefile]}]
  (when (and tuontivali
             tieosoiteverkon-alk-osoite
             tieosoiteverkon-alk-tuontikohde
             tieosoiteverkon-shapefile)
    (ajasta-paivitys this
                     "tieverkko"
                     tuontivali
                     tieosoiteverkon-alk-osoite
                     tieosoiteverkon-alk-tuontikohde
                     "tieosoiteverkko.zip"
                     (fn [] (tieverkon-tuonti/vie-tieverkko-kantaan (:db this) tieosoiteverkon-shapefile)))))



(comment
  ;; ylempi yleiseen muotoon ilman makroja =>

  (defn maarittele-alk-paivitystehtava
    [nimi alk-osoite-avain alk-tuontikohde-avain shapefile-avain paivitysfunktio]
    (fn [this {:keys [tuontivali] :as asetukset}]
      (let [alk-osoite (get asetukset alk-osoite-avain)
            alk-tuontikohde (get asetukset alk-tuontikohde-avain)
            shapefile (get asetukset shapefile-avain)]
        (when (and tuontivali
                   alk-osoite
                   alk-tuontikohde
                   shapefile)
          (ajasta-paivitys this
                           nimi
                           tuontivali
                           alk-osoite
                           alk-tuontikohde
                           (str nimi ".zip") ;; FIXME: esim "tieverkko" vs "tieosoiteverkko.zip" ei ihan sama
                           (fn [] (paivitysfunktio (:db this) shapefile)))))))

  (def tieverkon-alk-paivitystehtava
    (maarittele-alk-paivitystehtava "tieosoiteverkko"
                                    :tieosoiteverkon-alk-osoite :tieosoiteverkon-alk-tuontikohde
                                    :tieosoiteverkon-shapefile
                                    tieverkon-tuonti/vie-tieverkko-kantaan))
  
;; vastaava muunnos paikalliselle päivitystehtävälle
)

(defn tee-tieverkon-paikallinen-paivitystehtava
  [{:keys [db]}
   {:keys [tieosoiteverkon-alk-osoite
           tieosoiteverkon-alk-tuontikohde
           tieosoiteverkon-shapefile
           tuontivali]}]
  (when (not (and tieosoiteverkon-alk-osoite tieosoiteverkon-alk-tuontikohde))
    (chime-at
      (periodic-seq (tee-alkuajastus) (-> tuontivali time/minutes))
      (fn [_]
        (try
          (when (tarvitaanko-paikallinen-paivitys? db "tieverkko" tieosoiteverkon-shapefile)
            (log/debug "Ajetaan tieverkon paikallinen päivitys")
            (tieverkon-tuonti/vie-tieverkko-kantaan db tieosoiteverkon-shapefile)
            (geometriapaivitykset/paivita-viimeisin-paivitys<! db (harja.pvm/nyt) "tieverkko"))
          (catch Exception e
            (log/debug "Tieosoiteverkon paikallisessa tuonnissa tapahtui poikkeus: " e)))))))

(defn tee-pohjavesialueiden-alk-paivitystehtava
  [this {:keys [tuontivali
                pohjavesialueen-alk-osoite
                pohjavesialueen-alk-tuontikohde
                pohjavesialueen-shapefile]}]
  (when (and tuontivali
             pohjavesialueen-alk-osoite
             pohjavesialueen-alk-tuontikohde
             pohjavesialueen-shapefile)
    (ajasta-paivitys this
                     "pohjavesialueet"
                     tuontivali
                     pohjavesialueen-alk-osoite
                     pohjavesialueen-alk-tuontikohde
                     "pohjavesialue.zip"
                     (fn [] (pohjavesialueen-tuonti/vie-pohjavesialue-kantaan (:db this) pohjavesialueen-shapefile)))))

(defn tee-pohjavesialueiden-paikallinen-paivitystehtava
  [{:keys [db]}
   {:keys [pohjavesialueen-alk-osoite
           pohjavesialueen-alk-tuontikohde
           pohjavesialueen-shapefile
           tuontivali]}]
  (when (not (and pohjavesialueen-alk-osoite pohjavesialueen-alk-tuontikohde))
    (chime-at
      (periodic-seq (tee-alkuajastus) (-> tuontivali time/minutes))
      (fn [_]
        (try
          (when (tarvitaanko-paikallinen-paivitys? db "pohjavesialueet" pohjavesialueen-shapefile)
            (log/debug "Ajetaan pohjavesialueiden paikallinen päivitys")
            (pohjavesialueen-tuonti/vie-pohjavesialue-kantaan db pohjavesialueen-shapefile)
            (geometriapaivitykset/paivita-viimeisin-paivitys<! db (harja.pvm/nyt) "pohjavesialueet"))
          (catch Exception e
            (log/debug "Pohjavesialueiden paikallisessa tuonnissa tapahtui poikkeus: " e)))))))

(defn tee-siltojen-alk-paivitystehtava
  [this {:keys [tuontivali
                siltojen-alk-osoite
                siltojen-alk-tuontikohde
                siltojen-shapefile]}]
  (when (and tuontivali
             siltojen-alk-osoite
             siltojen-alk-tuontikohde
             siltojen-shapefile)
    (ajasta-paivitys this
                     "sillat"
                     tuontivali
                     siltojen-alk-osoite
                     siltojen-alk-tuontikohde
                     "sillat.zip"
                     (fn [] (siltojen-tuonti/vie-sillat-kantaan (:db this) siltojen-shapefile)))))

(defn tee-siltojen-paikallinen-paivitystehtava
  [{:keys [db]}
   {:keys [siltojen-alk-osoite
           siltojen-alk-tuontikohde
           siltojen-shapefile
           tuontivali]}]
  (when (not (and siltojen-alk-osoite siltojen-alk-tuontikohde))
    (chime-at
      (periodic-seq (tee-alkuajastus) (-> tuontivali time/minutes))
      (fn [_]
        (try
          (when (tarvitaanko-paikallinen-paivitys? db "sillat" siltojen-shapefile)
            (log/debug "Ajetaan siltojen paikallinen päivitys")
            (siltojen-tuonti/vie-sillat-kantaan db siltojen-shapefile)
            (geometriapaivitykset/paivita-viimeisin-paivitys<! db (harja.pvm/nyt) "sillat"))
          (catch Exception e
            (log/debug "Siltojen paikallisessa tuonnissa tapahtui poikkeus: " e)))))))

(defn tee-talvihoidon-hoitoluokkien-alk-paivitystehtava
  [this {:keys [tuontivali
                talvihoidon-hoitoluokkien-alk-osoite
                talvihoidon-hoitoluokkien-alk-tuontikohde
                talvihoidon-hoitoluokkien-shapefile]}]
  (when (and tuontivali
             talvihoidon-hoitoluokkien-alk-osoite
             talvihoidon-hoitoluokkien-alk-tuontikohde
             talvihoidon-hoitoluokkien-shapefile)
    (ajasta-paivitys this
                     "talvihoitoluokat"
                     tuontivali
                     talvihoidon-hoitoluokkien-alk-osoite
                     talvihoidon-hoitoluokkien-alk-tuontikohde
                     "talvihoidon-hoitoluokat.tgz"
                     (fn [] (talvihoidon-tuonti/vie-hoitoluokat-kantaan (:db this) talvihoidon-hoitoluokkien-shapefile)))))

(defn tee-talvihoidon-hoitoluokkien-paikallinen-paivitystehtava
  [{:keys [db]}
   {:keys [tuontivali
           talvihoidon-hoitoluokkien-alk-osoite
           talvihoidon-hoitoluokkien-alk-tuontikohde
           talvihoidon-hoitoluokkien-shapefile]}]
  (when (not (and talvihoidon-hoitoluokkien-alk-osoite talvihoidon-hoitoluokkien-alk-tuontikohde))
    (chime-at (periodic-seq (tee-alkuajastus) (-> tuontivali time/minutes))
              (fn [_]
                (try
                  (when (tarvitaanko-paikallinen-paivitys? db "talvihoitoluokat" talvihoidon-hoitoluokkien-shapefile)
                    (log/debug "Ajetaan talvihoidon hoitoluokkien paikallinen päivitys")
                    (talvihoidon-tuonti/vie-hoitoluokat-kantaan db talvihoidon-hoitoluokkien-shapefile)
                    (geometriapaivitykset/paivita-viimeisin-paivitys<! db (harja.pvm/nyt) "talvihoitoluokat"))
                  (catch Exception e
                    (log/debug "Talvihoidon paikallisessa tuonnissa tapahtui poikkeus: " e)))))))

(defn tee-soratien-hoitoluokkien-alk-paivitystehtava
  [this {:keys [tuontivali
                soratien-hoitoluokkien-alk-osoite
                soratien-hoitoluokkien-alk-tuontikohde
                soratien-hoitoluokkien-shapefile]}]
  (when (and tuontivali
             soratien-hoitoluokkien-alk-osoite
             soratien-hoitoluokkien-alk-tuontikohde
             soratien-hoitoluokkien-shapefile)
    (ajasta-paivitys this
                     "soratieluokat"
                     tuontivali
                     soratien-hoitoluokkien-alk-osoite
                     soratien-hoitoluokkien-alk-tuontikohde
                     "soratien-hoitoluokat.tgz"
                     (fn [] (soratien-hoitoluokkien-tuonti/vie-hoitoluokat-kantaan (:db this) soratien-hoitoluokkien-shapefile)))))

(defn tee-soratien-hoitoluokkien-paikallinen-paivitystehtava
  [{:keys [db]}
   {:keys [tuontivali
           soratien-hoitoluokkien-alk-osoite
           soratien-hoitoluokkien-alk-tuontikohde
           soratien-hoitoluokkien-shapefile]}]
  (when (not (and soratien-hoitoluokkien-alk-osoite soratien-hoitoluokkien-alk-tuontikohde))
    (chime-at (periodic-seq (tee-alkuajastus) (-> tuontivali time/minutes))
              (fn [_]
                (try
                  (when (tarvitaanko-paikallinen-paivitys? db "soratieluokat" soratien-hoitoluokkien-shapefile)
                    (log/debug "Ajetaan sorateiden hoitoluokkien paikallinen päivitys")
                    (soratien-hoitoluokkien-tuonti/vie-hoitoluokat-kantaan db soratien-hoitoluokkien-shapefile)
                    (geometriapaivitykset/paivita-viimeisin-paivitys<! db (harja.pvm/nyt) "soratieluokat"))
                  (catch Exception e
                    (log/debug "Sorateiden paikallisessa tuonnissa tapahtui poikkeus: " e)))))))

(defrecord Geometriapaivitykset [asetukset]
  component/Lifecycle
  (start [this] ; FIXME Kaikissa näissä on miltei identtinen pohja, voisi ehkä yleistää yhdeksi funktioksi tai multimethodiksi
    (assoc this :tieverkon-hakutehtava (tee-tieverkon-alk-paivitystehtava this asetukset))
    (assoc this :tieverkon-paivitystehtava (tee-tieverkon-paikallinen-paivitystehtava this asetukset))
    (assoc this :pohjavesialueiden-hakutehtava (tee-pohjavesialueiden-alk-paivitystehtava this asetukset))
    (assoc this :pohjavesialueiden-paivitystehtava (tee-pohjavesialueiden-paikallinen-paivitystehtava this asetukset))
    (assoc this :talvihoidon-hoitoluokkien-hakutehtava (tee-talvihoidon-hoitoluokkien-alk-paivitystehtava this asetukset))
    (assoc this :talvihoidon-hoitoluokkien-paivitystehtava (tee-talvihoidon-hoitoluokkien-paikallinen-paivitystehtava this asetukset))
    (assoc this :soratien-hoitoluokkien-hakutehtava (tee-soratien-hoitoluokkien-alk-paivitystehtava this asetukset))
    (assoc this :soratien-hoitoluokkien-paivitystehtava (tee-soratien-hoitoluokkien-paikallinen-paivitystehtava this asetukset))
    (assoc this :siltojen-hakutehtava (tee-siltojen-alk-paivitystehtava this asetukset))
    (assoc this :siltojen-paivitystehtava (tee-siltojen-paikallinen-paivitystehtava this asetukset)))
  (stop [this]
    (apply (:tieverkon-hakutehtava this) [])
    (apply (:soratien-hoitoluokkien-hakutehtava this) [])
    (apply (:pohjavesialueiden-hakutehtava this) [])
    (apply (:pohjavesialueiden-paivitystehtava this) [])
    (apply (:tieverkon-paivitystehtava this) [])
    (apply (:soratien-hoitoluokkien-paivitystehtava this) [])
    this))
