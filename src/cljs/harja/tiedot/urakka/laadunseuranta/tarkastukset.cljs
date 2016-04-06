(ns harja.tiedot.urakka.laadunseuranta.tarkastukset
  (:require [reagent.core :refer [atom]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.geo :as geo]
            [harja.tiedot.urakka :as tiedot-urakka]
            [harja.tiedot.urakka.laadunseuranta :as laadunseuranta]
            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm]
            [harja.loki :refer [log tarkkaile!]]
            [harja.domain.laadunseuranta.tarkastukset :as tarkastukset])
  (:require-macros [harja.atom :refer [reaction<!]]
                   [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(def +tarkastustyyppi->nimi+ tarkastukset/+tarkastustyyppi->nimi+)

(defonce tienumero (atom nil))                              ;; tienumero, tai kaikki
(defonce tarkastustyyppi (atom nil))                        ;; nil = kaikki, :tiesto, :talvihoito, :soratie

(defn hae-tarkastus
  "Hakee tarkastuksen kaikki tiedot urakan id:n ja tarkastuksen id:n perusteella. Tähän liittyy laatupoikkeamat sekä niiden reklamaatiot."
  [urakka-id tarkastus-id]
  (k/post! :hae-tarkastus {:urakka-id    urakka-id
                           :tarkastus-id tarkastus-id}))

(defn tallenna-tarkastus
  "Tallentaa tarkastuksen urakalle."
  [urakka-id tarkastus]
  (k/post! :tallenna-tarkastus {:urakka-id urakka-id
                                :tarkastus tarkastus}))

(defn hae-urakan-tarkastukset
  "Hakee annetun urakan tarkastukset urakka id:n ja ajan perusteella."
  [urakka-id alkupvm loppupvm tienumero tyyppi]
  (k/post! :hae-urakan-tarkastukset {:urakka-id urakka-id
                                     :alkupvm   alkupvm
                                     :loppupvm  loppupvm
                                     :tienumero tienumero
                                     :tyyppi    tyyppi}))

(defn naytettava-aikavali [urakka-kaynnissa? kuukausi aikavali]
  (if urakka-kaynnissa?
    aikavali
    (or kuukausi aikavali)))

(defonce urakan-tarkastukset
  (reaction<! [urakka-id (:id @nav/valittu-urakka)
               urakka-kaynnissa? @tiedot-urakka/valittu-urakka-kaynnissa?
               kuukausi @tiedot-urakka/valittu-hoitokauden-kuukausi
               aikavali @tiedot-urakka/valittu-aikavali
               laadunseurannassa? @laadunseuranta/laadunseurannassa?
               valilehti (nav/valittu-valilehti :laadunseuranta)
               tienumero @tienumero
               tyyppi @tarkastustyyppi]
              {:odota 500
               :nil-kun-haku-kaynnissa? true}
              (let [[alku loppu] (naytettava-aikavali urakka-kaynnissa? kuukausi aikavali)]
                (when (and laadunseurannassa? (= :tarkastukset valilehti)
                           urakka-id alku loppu)
                  (go (into [] (<! (hae-urakan-tarkastukset urakka-id alku loppu tienumero tyyppi))))))))

(defonce valittu-tarkastus (atom nil))

(defn paivita-tarkastus-listaan!
  "Päivittää annetun tarkastuksen urakan-tarkastukset listaan, jos se on valitun aikavälin sisällä."
  [{:keys [aika id] :as tarkastus}]
  (let [[alkupvm loppupvm] (naytettava-aikavali @tiedot-urakka/valittu-urakka-kaynnissa?
                                                @tiedot-urakka/valittu-hoitokauden-kuukausi
                                                @tiedot-urakka/valittu-aikavali)
        sijainti-listassa (first (keep-indexed (fn [i {tarkastus-id :id}]
                                                 (when (= id tarkastus-id) i))
                                               @urakan-tarkastukset))]
    (if (pvm/valissa? aika alkupvm loppupvm)
      ;; Tarkastus on valitulla välillä: päivitetään
      (if sijainti-listassa
        (swap! urakan-tarkastukset assoc sijainti-listassa tarkastus)
        (swap! urakan-tarkastukset conj tarkastus))

      ;; Ei pvm välillä, poistetaan listasta jos se aiemmin oli välillä
      (when sijainti-listassa
        (swap! urakan-tarkastukset (fn [tarkastukset]
                                     (into []
                                           (remove #(= (:id %) id))
                                           tarkastukset)))))))
