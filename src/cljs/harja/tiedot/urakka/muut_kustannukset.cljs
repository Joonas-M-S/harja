(ns harja.tiedot.urakka.muut-kustannukset
  (:require
    [reagent.core :refer [atom] :as r]
    [harja.loki :refer [log tarkkaile!]]
    [harja.tiedot.urakka :as tiedot-urakka]
    [harja.tiedot.urakka.laadunseuranta.sanktiot :as tiedot-sanktiot]
    [harja.tiedot.navigaatio :as nav]

    [cljs.core.async :refer [<!]]
    [harja.asiakas.kommunikaatio :as k])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [harja.atom :refer [reaction<!]]
                   [cljs.core.async.macros :refer [go]]))

(defonce muiden-kustannusten-tiedot (atom nil))
(defonce kohdistamattomien-sanktioiden-tiedot (atom nil))

(defn grid-tiedot* [mk-tiedot]
  (map #(assoc % :muokattava true) mk-tiedot))

(def grid-tiedot
  (reaction (grid-tiedot* @muiden-kustannusten-tiedot)))

#_(defn hae-kohteettomat-sanktiot! [urakkaid alkupvm loppupvm]
  (go (let [ch (tiedot-sanktiot/hae-urakan-sanktiot urakkaid [alkupvm loppupvm])
            vastaus (<! ch)]
        (log "sanktiot-vastaus:" (clj->js vastaus)))))

(defn hae-muiden-kustannusten-tiedot! [urakka-id sopimus-id alkupvm loppupvm]
  (do (log "hae-muiden-kustannusten-tiedot! post")
      (k/post! :hae-yllapito-toteumat {:urakka urakka-id :sopimus sopimus-id :alkupvm alkupvm :loppupvm loppupvm})))

(defn tallenna-toteuma! [toteuman-tiedot]
  (k/post! :tallenna-yllapito-toteuma toteuman-tiedot))

(defn tallenna-lomake [urakka data-atomi grid-data] ;; XXX siirrä tämä tiedot-namespaceen
  ;; kustannukset tallennetaan ilman laskentakohdetta yllapito_toteuma-tauluun,
  ;; -> backin palvelut.yllapito-toteumat/tallenna-yllapito-toteuma
  (let [toteuman-avaimet-gridista #(select-keys % [:id :toteuma :alkupvm :loppupvm :selite :pvm :hinta])
        [sopimus-id sopimus-nimi] @tiedot-urakka/valittu-sopimusnumero
        dump #(do (log "talenna-toteuma saa:" (pr-str %)) %)]
    (go
      (mapv #(-> % toteuman-avaimet-gridista (assoc :urakka (:id urakka) :sopimus sopimus-id) dump tallenna-toteuma!)
            grid-data)
      (println "tallennettiin" grid-data))))
