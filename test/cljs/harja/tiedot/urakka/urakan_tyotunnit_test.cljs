(ns harja.tiedot.urakka.urakan-tyotunnit-test
  (:require [tuck.core :refer [tuck]]
            [cljs.test :as t :refer-macros [deftest is]]
            [harja.testutils :refer [fake-palvelut-fixture fake-palvelukutsu jvh-fixture]]
            [harja.testutils.shared-testutils :as u]
            [harja.tiedot.urakka.urakan-tyotunnit :as urakan-tyotunnit])
  (:require-macros [harja.testutils.macros :refer [komponenttitesti]]
                   [cljs.core.async.macros :refer [go]]))

(t/use-fixtures :each u/komponentti-fixture fake-palvelut-fixture jvh-fixture)

(deftest urakan-tyotunnit-naytettavasta-tallennettavaksi
  (let [naytettavat [{:vuosi 2017
                      :ensimmainen-vuosikolmannes 111
                      :toinen-vuosikolmannes 222
                      :kolmas-vuosikolmannes 333}
                     {:vuosi 2016
                      :ensimmainen-vuosikolmannes 444
                      :toinen-vuosikolmannes 555
                      :kolmas-vuosikolmannes 666}]
        odotetut [{:harja.domain.urakan-tyotunnit/urakka-id 666
                   :harja.domain.urakan-tyotunnit/vuosi 2017
                   :harja.domain.urakan-tyotunnit/vuosikolmannes 1
                   :harja.domain.urakan-tyotunnit/tyotunnit 111}
                  {:harja.domain.urakan-tyotunnit/urakka-id 666
                   :harja.domain.urakan-tyotunnit/vuosi 2017
                   :harja.domain.urakan-tyotunnit/vuosikolmannes 2
                   :harja.domain.urakan-tyotunnit/tyotunnit 222}
                  {:harja.domain.urakan-tyotunnit/urakka-id 666
                   :harja.domain.urakan-tyotunnit/vuosi 2017
                   :harja.domain.urakan-tyotunnit/vuosikolmannes 3
                   :harja.domain.urakan-tyotunnit/tyotunnit 222}
                  {:harja.domain.urakan-tyotunnit/urakka-id 666
                   :harja.domain.urakan-tyotunnit/vuosi 2016
                   :harja.domain.urakan-tyotunnit/vuosikolmannes 1
                   :harja.domain.urakan-tyotunnit/tyotunnit 444}
                  {:harja.domain.urakan-tyotunnit/urakka-id 666
                   :harja.domain.urakan-tyotunnit/vuosi 2016
                   :harja.domain.urakan-tyotunnit/vuosikolmannes 2
                   :harja.domain.urakan-tyotunnit/tyotunnit 555}
                  {:harja.domain.urakan-tyotunnit/urakka-id 666
                   :harja.domain.urakan-tyotunnit/vuosi 2016
                   :harja.domain.urakan-tyotunnit/vuosikolmannes 3
                   :harja.domain.urakan-tyotunnit/tyotunnit 555}]
        tallennettavat (urakan-tyotunnit/tyotunnit-tallennettavana 666 naytettavat)]
    (is (= odotetut tallennettavat))))

(deftest urakan-tyotunnit-palvelusta-naytettavaksi
  (let [vuodet [{:vuosi 2017}
                {:vuosi 2016}
                {:vuosi 2015}]
        palvelusta [{:harja.domain.urakan-tyotunnit/tyotunnit 666
                     :harja.domain.urakan-tyotunnit/id 27
                     :harja.domain.urakan-tyotunnit/vuosi 2017
                     :harja.domain.urakan-tyotunnit/lahetys-onnistunut false
                     :harja.domain.urakan-tyotunnit/vuosikolmannes 2
                     :harja.domain.urakan-tyotunnit/urakka-id 4}]
        naytettavat (urakan-tyotunnit/tyotunnit-naytettavana vuodet palvelusta)
        odotetut [{:kolmas-vuosikolmannes nil
                   :vuosi 2017
                   :ensimmainen-vuosikolmannes nil
                   :toinen-vuosikolmannes 666}
                  {:kolmas-vuosikolmannes nil
                   :vuosi 2016
                   :ensimmainen-vuosikolmannes nil
                   :toinen-vuosikolmannes nil}
                  {:kolmas-vuosikolmannes nil
                   :vuosi 2015
                   :ensimmainen-vuosikolmannes nil
                   :toinen-vuosikolmannes nil}]]
    (is (= odotetut naytettavat))))



