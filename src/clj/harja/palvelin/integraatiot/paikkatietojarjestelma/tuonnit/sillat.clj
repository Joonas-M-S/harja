(ns harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.sillat
  (:require [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as spec]
            [harja.kyselyt.siltatarkastukset :as s]
            [harja.tyokalut.functor :as functor]
            [harja.palvelin.integraatiot.paikkatietojarjestelma.tuonnit.shapefile :as shapefile]))

(defn float-intiksi [v]
  (if (float? v)
    (int v)
    v))

(defn mapin-floatit-inteiksi [m]
  (into {} (map (fn [[k v]]
                  [k (float-intiksi v)])
                m)))


(def elytunnuksen-laani
  {"Uud" "U" ;; uusimaa
   "Var" "T" ;; varsinais-suomi
   "Kas" "KaS" ;; kaakkois-suomi
   "Pir" "H" ;; pirkanmaa
   "Pos" "SK" ;; pohjois-savo
   "Kes" "KeS" ;; keskis-suomi
   "Epo" "V" ;; etelä-pohjanmaa
   "Pop" "O" ;; pohjois-pohjanmaa
   "Lap" "L" ;; lappi
   })


(defn luo-tai-paivita-silta [db silta-floateilla]
  (let [silta (mapin-floatit-inteiksi silta-floateilla)
        tyyppi (:rakennety silta)
        numero (:siltanro silta)
        nimi (:siltanimi silta)
        geometria (.toString (:the_geom silta))
        tie (:tie silta)
        alkuosa (:aosa silta)
        alkuetaisyys (:aet silta)
        ely-lyhenne (:ely_lyhenn silta)
        ely-numero (:ely silta)
        loppupvm (:loppupvm silta)
        laani-lyhenne (get elytunnuksen-laani ely-lyhenne)
        tunnus (when (not-empty laani-lyhenne)
                 (str laani-lyhenne "-" numero))
        id (when :silta_id silta
                 (int (:silta_id silta)))]

    ;; (log/debug "silta luettu tl261-tietueesta:" tyyppi, numero, nimi, tie, alkuosa, alkuetaisyys, ely-lyhenne, ely-numero tunnus, id)
    (when (and id tunnus (empty? loppupvm))
      ;; huom - tämä on tietokannan "siltaid" -kenttä, ei "id"-kenttä.
      ;; id-kenttää ei kai käytetä mihinkään vaan se on itse keksitty id
      (if-let [vanha-silta (first (s/hae-silta-idlla db id))]
        (do
          (s/paivita-silta-idlla! db tyyppi numero nimi geometria tie alkuosa alkuetaisyys tunnus id))
        (s/luo-silta! db tyyppi numero nimi geometria tie alkuosa alkuetaisyys tunnus id))
      true)))


(spec/def ::the_geom  (fn [g]
                        (re-matches #"POINT[. 0-9()]+" (.toString g))))
(spec/def ::ely_lyhenn #(contains? elytunnuksen-laani %))
(spec/def ::silta_id some?)
(spec/def ::silta-shp-spec (spec/keys  :req-un [::the_geom ::ely_lyhenn ::silta_id]))

(defn vie-silta-entry [db silta]
  (if (spec/valid? ::silta-shp-spec silta)
    (luo-tai-paivita-silta db silta)
    (log/debug "Siltaa ei voida tuoda ilman geometriaa, ely-lyhennettä ja silta-id:tä. Validointi:" (with-out-str (spec/explain ::silta-shp-spec silta)))))


(defn vie-sillat-kantaan [db shapefile]
  (if shapefile
    (let [kpl (atom 0)
          siltatietueet-shapefilesta (shapefile/tuo shapefile)
          ;; siltatietueet-shapefilesta (take 200 siltatietueet-shapefilesta)
          siltaid-kentalliset-siltatietueet  (filter :silta_id siltatietueet-shapefilesta)
          _ (log/debug "montako siltaa joilla silta_id? -> " (count siltaid-kentalliset-siltatietueet))
          _ (log/debug "montako uniikkia silta_id:ta? -> " (count (set (map :silta_id siltaid-kentalliset-siltatietueet))))
          _ (log/debug "montako ei-tyhjää loppupvm:aa? -> " (count (filter #(not-empty (:loppupvm %)) siltaid-kentalliset-siltatietueet)))
          ]
      (log/debug (str "Tuodaan sillat kantaan tiedostosta " shapefile))
      (jdbc/with-db-transaction [db db]
        (doseq [silta siltatietueet-shapefilesta]
          (when (true? (vie-silta-entry db silta))
            (swap! kpl inc))))
      (s/paivita-urakoiden-sillat db)
      (log/debug "Siltojen tuonti kantaan valmis, tallennettiin" @kpl "kpl, luettuja oli" (count siltatietueet-shapefilesta)))
    (log/debug "Siltojen tiedostoa ei löydy konfiguraatiosta. Tuontia ei suoriteta.")))
