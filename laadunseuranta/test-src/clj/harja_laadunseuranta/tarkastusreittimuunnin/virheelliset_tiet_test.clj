(ns harja-laadunseuranta.tarkastusreittimuunnin.virheelliset-tiet-test
  (:require [clojure.test :refer :all]
            [harja-laadunseuranta.tarkastusreittimuunnin.ramppianalyysi :as ramppianalyysi]
            [harja-laadunseuranta.tarkastusreittimuunnin.testityokalut :as tyokalut]
            [harja.testi :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja-laadunseuranta.kyselyt :as q]
            [com.stuartsierra.component :as component]
            [harja.domain.tierekisteri :as tr-domain]
            [harja-laadunseuranta.core :as harja-laadunseuranta]
            [harja-laadunseuranta.tarkastusreittimuunnin.virheelliset-tiet :as virheelliset-tiet]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :mobiili-laadunseuranta
                        (component/using
                          (harja-laadunseuranta/->Laadunseuranta)
                          [:db :http-palvelin])))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :once (compose-fixtures tietokanta-fixture jarjestelma-fixture))

;; HOX! Tässä tehdään testejä kannassa löytyville ajoille, joilla on tietty id.
;; Ajojen tekstuaalisen selityksen löydät: testidata/tarkastusajot.sql
;; Tai käytä #tr näkymää piirtämään pisteet kartalle.

(deftest ramppianalyysi-korjaa-virheelliset-rampit-oikeassa-ajossa-1
  (let [tarkastusajo-id 999
        merkinnat (q/hae-reitin-merkinnat-tieosoitteilla (:db jarjestelma)
                                                         {:tarkastusajo tarkastusajo-id
                                                          :laheiset_tiet_threshold 100})]

    (is (> (count merkinnat) 1) "Ainakin yksi merkintä testidatassa")
    (is (= (count (distinct (map #(get-in % [:tr-osoite :tie]) merkinnat))) 2)
        "Osa testidatan merkinnöistä on eri tiellä (yksi osuu sillalle ja muut moottoritielle)")

    (let [korjatut-merkinnat (virheelliset-tiet/korjaa-virheelliset-tiet merkinnat)]
      ;; Korjauksen jälkeen kaikki pisteet projisoitu moottoritielle"
      (is (= (count (distinct (map #(get-in % [:tr-osoite :tie]) korjatut-merkinnat))) 1)
          "Korjauksen jälkeen kaikki pisteet projisoitu moottoritielle")
      (is (every? #(= 4 %) (map #(get-in % [:tr-osoite :tie]) korjatut-merkinnat))))))