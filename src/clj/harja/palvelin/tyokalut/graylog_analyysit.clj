(ns harja.palvelin.tyokalut.graylog-analyysit
  (:require [taoensso.timbre :as log]
            [harja.pvm :as pvm]
            [harja.palvelin.tyokalut.graylog-muunnokset :as muunnokset]))

(defn yrita-korjata
  [rikkinainen]
  (map #(let [palvelu (get-in % [:yhteyskatkokset 0 :palvelu])
              katkokset (get-in % [:yhteyskatkokset 0 :katkokset])]
          {:yhteyskatkokset [{:palvelu palvelu :katkokset katkokset}]})
       rikkinainen))

(defn pvm-valinen-aika
  [pvm-ennen pvm-jalkeen]
  (let [ensimmainen-ms (when pvm-ennen
                         (.getTime pvm-ennen))
        viimeinen-ms (when pvm-jalkeen
                      (.getTime pvm-jalkeen))]
   (when (and ensimmainen-ms viimeinen-ms)
     (- viimeinen-ms ensimmainen-ms))))

(defn viimeinen-pingaus
  [yhteyskatkokset-map]
  (let [ping-yhteyskatkokset (some (fn [palvelun-katkokset]
                                     (when (= "ping" (:palvelu palvelun-katkokset))
                                       palvelun-katkokset))
                                   (:yhteyskatkokset yhteyskatkokset-map))
        ; Tämä tehdään siltä varalta, että pingiä ei kerettyä tehdä. Siinä tapauksessa otetaan vain joku palvelukutsu
        ping-yhteyskatkokset (if ping-yhteyskatkokset
                                ping-yhteyskatkokset
                                (first (:yhteyskatkokset yhteyskatkokset-map)))
        viimeinen-katkos (:viimeinen-katkos ping-yhteyskatkokset)
        ensimmainen-katkos (:ensimmainen-katkos ping-yhteyskatkokset)]
    (if (and viimeinen-katkos ensimmainen-katkos)
      (if (> (.getTime viimeinen-katkos)
             (.getTime ensimmainen-katkos))
         viimeinen-katkos
         ensimmainen-katkos)
      nil)))

(defn ok-yhteyskatkos-data
  [yhteyskatkokset]
  (filter :yhteyskatkokset yhteyskatkokset))

(defn rikkinainen-yhteyskatkos-data
  [yhteyskatkokset]
  (keep #(when (:rikkinainen %) (:rikkinainen %))  yhteyskatkokset))

(defn ota-mapin-n-suurinta-arvoa
  [mappi n]
  (into {}
        (take-last n (sort-by (fn [avain-arvo-pari]
                                (second avain-arvo-pari))
                              mappi))))

(defn rikkinaisten-yhteyskatkosten-analyysi
  [rikkinainen-data]
  (let [lukumaara (count (filter #(when (or (:kayttaja %) ; jotta sama rikkinainen lasketaan vain kerran
                                            (= % "foo"))
                                      true)
                                 rikkinainen-data))
        eheytetyt-yhteyskatkokset (yrita-korjata (filter #(when-not (= % "foo")
                                                            true)
                                                         rikkinainen-data))
        eheytetyt-yhteyskatkokset-lkm (count eheytetyt-yhteyskatkokset)]
    {:rikkinaiset-lkm lukumaara
     :eheytetyt-lkm eheytetyt-yhteyskatkokset-lkm
     :eheytetyt-yhteyskatkokset eheytetyt-yhteyskatkokset}))

(defn yhteyskatkokset-ryhmittain-analyysi
  [yhteyskatkos-data]
  (let [yhteyskatkokset-ryhmittain (map muunnokset/yhteyskatkokset-ryhmittain yhteyskatkos-data)
        katkokset-ynnatty (apply muunnokset/yhdista-avaimet-kun + :katkokset [:palvelu] (mapcat :yhteyskatkokset yhteyskatkokset-ryhmittain))]
    {:eniten-katkoksia (take-last 5 (sort-by #(:katkokset %) katkokset-ynnatty))}))

(defn yhteyskatkokset-analyysi
  [yhteyskatkos-data]
  (let [katkokset-ynnatty (apply muunnokset/yhdista-avaimet-kun + :katkokset [:palvelu] (mapcat :yhteyskatkokset yhteyskatkos-data))]
    {:eniten-katkoksia (take-last 5 (sort-by #(:katkokset %) katkokset-ynnatty))}))

(defn selain-sammutettu-katkoksen-aikana
  [yhteyskatkos-data]
  (reduce #(let [lokitus-tapahtui (pvm/dateksi (:pvm %2))
                 viimeinen-pingaus-tapahtui (viimeinen-pingaus %2)
                 lokituksen-ja-pingauksen-vali (pvm-valinen-aika viimeinen-pingaus-tapahtui lokitus-tapahtui)
                 kutsutut-palvelut (keep (fn [palvelun-katkokset]
                                           (if (= "ping" (:palvelu palvelun-katkokset))
                                             nil
                                             (:palvelu palvelun-katkokset)))
                                         (:yhteyskatkokset %2))]
              (if (and lokituksen-ja-pingauksen-vali (> lokituksen-ja-pingauksen-vali 10000))
                (merge-with + %1 (zipmap kutsutut-palvelut
                                         (repeat (count kutsutut-palvelut) 1)))
                %1))
          {} yhteyskatkos-data))

(defn analyysit-yhteyskatkoksista
  [yhteyskatkokset {analysointimetodi :analysointimetodi haettavat-analyysit :haettavat-analyysit}]
  (let [ok-yhteyskatkos-data (ok-yhteyskatkos-data yhteyskatkokset)
        rikkinainen-yhteyskatkos-data (rikkinainen-yhteyskatkos-data yhteyskatkokset)
        rikkinaiset-lokitukset (when (contains? haettavat-analyysit :rikkinaiset-lokitukset)
                                  (rikkinaisten-yhteyskatkosten-analyysi rikkinainen-yhteyskatkos-data))
        yhteyskatkos-data (concat ok-yhteyskatkos-data (:eheytetyt-yhteyskatkokset rikkinaiset-lokitukset))
        yhteyskatkokset-ryhmittain-analyysi (when (contains? haettavat-analyysit :eniten-katkosryhmia)
                                              (yhteyskatkokset-ryhmittain-analyysi yhteyskatkos-data))
        yhteyskatkokset-analyysi (when (contains? haettavat-analyysit :eniten-katkoksia)
                                   (yhteyskatkokset-analyysi yhteyskatkos-data))
        selain-sammutettu-katkoksen-aikana (when (contains? haettavat-analyysit :selain-sammutettu-katkoksen-aikana)
                                             (selain-sammutettu-katkoksen-aikana yhteyskatkos-data))]
    {:yhteyskatkokset-analyysi yhteyskatkokset-analyysi
     :rikkinaiset-lokitukset rikkinaiset-lokitukset
     :yhteyskatkokset-ryhmittain-analyysi yhteyskatkokset-ryhmittain-analyysi
     :selain-sammutettu-katkoksen-aikana selain-sammutettu-katkoksen-aikana}))
