(ns harja.palvelin.integraatiot.api.tyokoneenseuranta-test
  (:require [harja.palvelin.integraatiot.api.tyokoneenseuranta :refer :all :as tyokoneenseuranta]
            [com.stuartsierra.component :as component]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.api.tyokalut :as api-tyokalut]
            [clojure.test :refer :all]
            [harja.kyselyt.konversio :as konv]
            [harja.fmt :as fmt]
            [taoensso.timbre :as log]))

(def kayttaja "destia")

(def jarjestelma-fixture (laajenna-integraatiojarjestelmafixturea kayttaja
                                                                  :api-tyokoneenseuranta (component/using
                                                                                           (tyokoneenseuranta/->Tyokoneenseuranta)
                                                                                           [:http-palvelin :db :integraatioloki])))

(use-fixtures :once (compose-fixtures tietokanta-fixture
                                      jarjestelma-fixture))


(def skeeman-tehtavat
  #{"auraus ja sohjonpoisto",
    "aurausviitoitus ja kinostimet",
    "harjaus",
    "kelintarkastus",
    "koneellinen niitto",
    "koneellinen vesakonraivaus",
    "l- ja p-alueiden puhdistus",
    "liikennemerkkien puhdistus",
    "liik. opast. ja ohjausl. hoito seka reunapaalujen kun.pito",
    "linjahiekoitus",
    "lumensiirto",
    "lumivallien madaltaminen",
    "muu",
    "ojitus",
    "paallysteiden juotostyot",
    "paallysteiden paikkaus",
    "paannejaan poisto",
    "palteen poisto",
    "pinnan tasaus",
    "pistehiekoitus",
    "paallystetyn tien sorapientareen taytto",
    "siltojen puhdistus",
    "sorastus",
    "sorapientareen taytto",
    "sorateiden muokkaushoylays",
    "sorateiden polynsidonta",
    "sorateiden tasaus",
    "sulamisveden haittojen torjunta",
    "suolaus",
    "tiestotarkastus"})

(deftest tallenna-tyokoneen-seurantakirjaus-uusi
  (let [kutsu (api-tyokalut/post-kutsu
                ;; kokonaan uusi tyokone, kantaan pitäisi tulla uusi rivi
                ["/api/seuranta/tyokone"] kayttaja portti (-> "test/resurssit/api/tyokoneseuranta_uusi.json"
                                                              slurp
                                                              (.replace "__TEHTAVA__" "suolaus")))]
    (let [[sijainti line] (first (q "SELECT sijainti,ST_AsText(reitti) FROM tyokonehavainto WHERE tyokoneid=666"))
          tehtavat (-> (ffirst (q "SELECT tehtavat FROM tyokonehavainto WHERE tyokoneid=666"))
                       (konv/array->set))]
      (log/debug "TEHTÄVÄT:" (pr-str tehtavat))
      (is (= 200 (:status kutsu)))
      (is (= (str sijainti) "(429015.0,7198161.0)"))
      (is (= line "LINESTRING(429015 7198161)"))
      (is (= tehtavat #{"suolaus"})))))

(deftest tallenna-tyokoneen-seurantakirjaus-olemassaoleva
  (let [kutsu (api-tyokalut/post-kutsu
                ;; tyokone 31337 on jo kannassa, katsotaan muuttuuko raportoidut koordinaatit esimerkin mukaiseksi
                ["/api/seuranta/tyokone"] kayttaja portti (slurp "test/resurssit/api/tyokoneseuranta.json"))]
    (let [[s line]
          (first
           (q "SELECT sijainti,ST_AsText(reitti) FROM tyokonehavainto WHERE tyokoneid=31337"))]

      (is (= 200 (:status kutsu)))
      (is (= (str s) "(429005.0,7198151.0)"))
      (is (= (str line) "LINESTRING(429493 7207739,429005 7198151)")))))

(deftest kaikkien-tehtavien-kirjaus-toimii
  (doseq [tehtava skeeman-tehtavat]
    (let [kutsu (api-tyokalut/post-kutsu
                  ["/api/seuranta/tyokone"] kayttaja portti (-> "test/resurssit/api/tyokoneseuranta_uusi.json"
                                                                slurp
                                                                (.replace "__TEHTAVA__" tehtava)))]
      (let [tehtavat-kannassa (-> (ffirst (q "SELECT tehtavat FROM tyokonehavainto WHERE tyokoneid=666"))
                         (konv/array->set))
            tehtava-kannassa (first tehtavat-kannassa)]
        (is (= 200 (:status kutsu)))
        (is (= tehtava-kannassa tehtava)
            (str "Tehtävä '" tehtava "' raportoitu onnistuneesti"))))))
