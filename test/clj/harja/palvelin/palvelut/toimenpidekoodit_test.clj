(ns harja.palvelin.palvelut.toimenpidekoodit-test
  (:require [clojure.test :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.toimenpidekoodit :refer :all]
            [harja.palvelin.palvelut.urakan-toimenpiteet :as urakan-toimenpiteet]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]))

(deftest hae-urakan-kaikki-toimenpiteet []
    (let [db (apply tietokanta/luo-tietokanta testitietokanta)
        user "harja"
        urakka-id 1]
    (not (nil? (urakan-toimenpiteet/hae-urakan-toimenpiteet db user urakka-id)))))