(ns harja.tiedot.vesivaylat.urakka.toimenpiteet.kokonaishintaiset-test
  (:require [harja.tiedot.vesivaylat.urakka.toimenpiteet.kokonaishintaiset :as tiedot]
            [clojure.test :refer-macros [deftest is testing]]
            [harja.loki :refer [log]]
            [harja.tuck-apurit :refer-macros [vaadi-async-kutsut] :refer [e!]]
            [harja.pvm :as pvm]
            [harja.domain.vesivaylat.toimenpide :as to]))

(def testitila {:nakymassa? true
                :toimenpiteet [{::to/id 0
                                ::to/tyolaji "Viitat"
                                ::to/vayla {:nimi "Kopio, Iisalmen väylä"}
                                ::to/tyoluokka "Asennus ja huolto"
                                ::to/toimenpide "Huoltotyö"
                                ::to/pvm (pvm/nyt)
                                ::to/vikakorjaus true
                                ::to/turvalaite {:nimi "Siitenluoto (16469)"}}
                               {::to/id 1
                                ::to/tyolaji "Viitat"
                                ::to/vayla {:nimi "Kuopio, Iisalmen väylä"}
                                ::to/tyoluokka "Asennus ja huolto"
                                ::to/toimenpide "Huoltotyö"
                                ::to/pvm (pvm/nyt)
                                ::to/turvalaite {:nimi "Siitenluoto (16469)"}}
                               {::to/id 2
                                ::to/tyolaji "Viitat"
                                ::to/vayla {:nimi "Kopio, Iisalmen väylä"}
                                ::to/tyoluokka "Asennus ja huolto"
                                ::to/toimenpide "Huoltotyö"
                                ::to/pvm (pvm/nyt)
                                ::to/turvalaite {:nimi "Siitenluoto (16469)"}}
                               {::to/id 3
                                ::to/tyolaji "Viitat"
                                ::to/vayla {:nimi "Varkaus, Kuopion väylä"}
                                ::to/tyoluokka "Asennus ja huolto"
                                ::to/toimenpide "Huoltotyö"
                                ::to/pvm (pvm/nyt)
                                ::to/turvalaite {:nimi "Siitenluoto (16469)"}}
                               {::to/id 4
                                ::to/tyolaji "Tykityöt"
                                ::to/vayla {:nimi "Varkaus, Kuopion väylä"}
                                ::to/tyoluokka "Asennus ja huolto"
                                ::to/toimenpide "Huoltotyö"
                                ::to/pvm (pvm/nyt)
                                ::to/turvalaite {:nimi "Siitenluoto (16469)"}}
                               {::to/id 5
                                ::to/tyolaji "Poljut"
                                ::to/vayla {:nimi "Varkaus, Kuopion väylä"}
                                ::to/tyoluokka "Asennus ja huolto"
                                ::to/toimenpide "Huoltotyö"
                                ::to/pvm (pvm/nyt)
                                ::to/turvalaite {:nimi "Siitenluoto (16469)"}}]})

(deftest nakymaan-tuleminen
  (is (true? (:nakymassa? (e! (tiedot/->Nakymassa? true)))))
  (is (false? (:nakymassa? (e! (tiedot/->Nakymassa? false))))))

(deftest rivin-valinta
  (testing "Rivin asettaminen valituksi"
    (let [vanha-tila testitila
          vanha-kohde (to/toimenpide-idlla (:toimenpiteet vanha-tila) 0)
          uusi-tila (e! (tiedot/->ValitseToimenpide {:id 0 :valinta true}) vanha-tila)
          muokattu-kohde (to/toimenpide-idlla (:toimenpiteet uusi-tila) 0)]
      (is (not (:valittu? vanha-kohde)))
      (is (true? (:valittu? muokattu-kohde)))))

  (testing "Rivin asettaminen ei-valituksi"
    (let [vanha-tila testitila
          vanha-kohde (to/toimenpide-idlla (:toimenpiteet vanha-tila) 1)
          uusi-tila (e! (tiedot/->ValitseToimenpide {:id 1 :valinta false}) vanha-tila)
          muokattu-kohde (to/toimenpide-idlla (:toimenpiteet uusi-tila) 1)]
      (is (true? (:valittu? vanha-kohde)))
      (is (false? (:valittu? muokattu-kohde))))))