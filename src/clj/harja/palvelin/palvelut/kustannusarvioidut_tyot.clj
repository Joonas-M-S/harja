(ns harja.palvelin.palvelut.kustannusarvioidut-tyot
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelu]]
            [harja.kyselyt.kustannusarvioidut-tyot :as q]
            [harja.kyselyt.toimenpideinstanssit :as tpi-q]
            [harja.domain.oikeudet :as oikeudet]
            [harja.tyokalut.big :as big]
            [clojure.set :as set]
            [harja.domain.roolit :as roolit]))

(defn hae-urakan-kustannusarvioidut-tyot
  "Funktio palauttaa urakan kustannusarvioidut työt. Käytetään teiden hoidon urakoissa (MHU)."
  [db user urakka-id]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-suunnittelu-kustannussuunnittelu user urakka-id)
  (into []
        (comp
          (map #(assoc %
                  :summa (if (:summa %) (double (:summa %)))))
          (map #(if (:osuus-hoitokauden-summasta %)
                  (update % :osuus-hoitokauden-summasta big/->big)
                  %)))
        (q/hae-kustannusarvioidut-tyot db {:urakka urakka-id})))

(defn tallenna-kustannusarvioidut-tyot
  "Funktio tallentaa ja palautaa urakan kustannusarvioidut tyot. Käytetään teiden hoidon urakoissa (MHU)."
  [db user {:keys [urakka-id sopimusnumero tyot]}]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-suunnittelu-kustannussuunnittelu user urakka-id)
  (assert (vector? tyot) "tyot tulee olla vektori")
  (let [nykyiset-arvot (hae-urakan-kustannusarvioidut-tyot db user urakka-id)
        valitut-vuosi-ja-kk (into #{} (map (juxt :vuosi :kuukausi) tyot))
        tyo-avain (fn [rivi]
                    [(:tyyppi rivi) (:toimenpideinstanssi rivi) (:vuosi rivi) (:kuukausi rivi)])
        tyot-kannassa (into #{} (map tyo-avain
                                     (filter #(and
                                                (= (:sopimus %) sopimusnumero)
                                                (valitut-vuosi-ja-kk [(:vuosi %) (:kuukausi %)]))
                                             nykyiset-arvot)))
        urakan-toimenpideinstanssit (into #{}
                                          (map :id)
                                          (tpi-q/urakan-toimenpideinstanssi-idt db urakka-id))
        tallennettavat-toimenpideinstanssit (into #{} (map #(:toimenpideinstanssi %) tyot))]

    ;; Varmistetaan ettei päivitystä voi tehdä toimenpideinstanssille, joka ei kuulu tähän urakkaan.
    (when-not (empty? (set/difference tallennettavat-toimenpideinstanssit
                                      urakan-toimenpideinstanssit))
      (throw (roolit/->EiOikeutta "virheellinen toimenpideinstanssi")))

    (doseq [tyo tyot]
      (as-> tyo t
            (update t :summa big/unwrap)
            (assoc t :sopimus sopimusnumero)
            (assoc t :kayttaja (:id user))
            (if (not (tyot-kannassa (tyo-avain t)))
              (q/lisaa-kustannusarvioitu-tyo<! db t)
              (q/paivita-kustannusarvioitu-tyo! db t)))))

  (hae-urakan-kustannusarvioidut-tyot db user urakka-id))