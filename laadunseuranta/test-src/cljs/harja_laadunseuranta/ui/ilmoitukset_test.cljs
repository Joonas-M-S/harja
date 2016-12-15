(ns ^:figwheel-load harja-laadunseuranta.ui.ilmoitukset-test
  (:require [cljs.test :as t :refer-macros [deftest is testing async]]
            [reagent.core :as reagent :refer [atom]]
            [harja-laadunseuranta.testutils :refer [sel sel1]]
            [harja-laadunseuranta.ui.ilmoitukset :as i]
            [harja-laadunseuranta.tiedot.ilmoitukset :as i-tiedot])
  (:require-macros [harja-laadunseuranta.test-macros :refer [with-component prepare-component-tests]]
                   [harja-laadunseuranta.macros :refer [after-delay]]))

(prepare-component-tests)

(deftest ilmoituskomponentti-test
  (testing "Ilmoituksen nayttaminen"
    (let [ilmoitukset (atom [])]
      (with-component [i/ilmoitusjonokomponentti ilmoitukset]

        (is (= 0 (.-length (sel [:div.ilmoitus]))))

        (i-tiedot/lisaa-ilmoitus-jonoon "Testi-ilmoitus" ilmoitukset)

        (reagent/flush)

        (is (= 1 (.-length (sel [:div.ilmoitus]))))

        (async ilmoitus-poistunut
          (after-delay (+ i-tiedot/+ilmoituksen-nakymisaika-ms+ 500)
                       (is (= 0 (.-length (sel [:div.ilmoitus]))))
                       (ilmoitus-poistunut)))))))

