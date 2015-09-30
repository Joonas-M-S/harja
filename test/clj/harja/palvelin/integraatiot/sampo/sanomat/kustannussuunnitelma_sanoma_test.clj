(ns harja.palvelin.integraatiot.sampo.sanomat.kustannussuunnitelma-sanoma-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.palvelin.integraatiot.sampo.sanomat.kustannussuunnitelma-sanoma :as kustannussuunnitelma-sanoma]
            [hiccup.core :refer [html]]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [harja.testi :refer :all]
            [harja.tyokalut.xml :as xml])
  (:import (java.text SimpleDateFormat)))

(def +xsd-polku+ "xsd/sampo/outbound/")

(defn parsi-paivamaara [teksti]
  (.parse (SimpleDateFormat. "dd.MM.yyyy") teksti))

(def +maksuera+ {:numero               123456789
                 :maksuera             {:nimi
                                                "Testimaksuera"
                                        :tyyppi "kokonaishintainen"}
                 :toimenpideinstanssi  {:alkupvm         (parsi-paivamaara "12.12.2015")
                                        :loppupvm        (parsi-paivamaara "1.1.2017")
                                        :vastuuhenkilo   "A009717"
                                        :talousosasto    "talousosasto"
                                        :tuotepolku      "polku/tuote"
                                        :toimenpidekoodi {
                                                          :koodi "20112"
                                                          }}
                 :urakka               {:sampoid "PR00020606"}
                 :sopimus              {:sampoid "00LZM-0033600"}
                 :kustannussuunnitelma {:summa 93999M}
                 :tuotenumero          111})

(deftest tarkista-kustannussuunnitelman-validius
  (let [kustannussuunnitelma (html (kustannussuunnitelma-sanoma/muodosta +maksuera+))
        xsd "nikuxog_costPlan.xsd"]
    (is (xml/validoi +xsd-polku+ xsd kustannussuunnitelma) "Muodostettu XML-tiedosto on XSD-skeeman mukainen")))

(deftest tarkista-lkp-tilinnumeron-paattely
  (is (= "43021" (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero "20112" nil)) "Oikea LKP-tilinnumero valittu toimenpidekoodin perusteella")
  (is (= "43021" (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil 112)) "Oikea LKP-tilinnumero valittu tuotenumeroon perusteella")
  (is (= "43021" (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil 536)) "Oikea LKP-tilinnumero valittu tuotenumeroon perusteella")
  (is (= "12981" (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil 30)) "Oikea LKP-tilinnumero valittu tuotenumeroon perusteella")
  (is (= "12981" (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil 242)) "Oikea LKP-tilinnumero valittu toimenpidekoodin perusteella")
  (is (= "12981" (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil 318)) "Oikea LKP-tilinnumero valittu toimenpidekoodin perusteella")
  (is (thrown? RuntimeException (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil nil)) "Jos LKP-tuotenumeroa ei voida päätellä, täytyy aiheutua poikkeus")
  (is (thrown? RuntimeException (kustannussuunnitelma-sanoma/valitse-lkp-tilinumero nil 1)) "Jos LKP-tuotenumeroa ei voida päätellä, täytyy aiheutua poikkeus"))

(deftest tarkista-kulun-jakaminen-vuosille
  (let [segmentit (kustannussuunnitelma-sanoma/luo-summat
                    (parsi-paivamaara "12.12.2015")
                    (parsi-paivamaara "1.11.2017") +maksuera+)]
    (is (= 4 (count segmentit)) "Segmentit on jaoteltu 3 vuodelle")

    (let [segmentti (second (second segmentit))
          odotettu-summa (/ (double (get-in +maksuera+ [:kustannussuunnitelma :summa])) 3)]
      (is (= odotettu-summa (:value segmentti)))
      (is (= "2015-12-31T02:00:00.0" (:start segmentti)))
      (is (= "2015-01-01T02:00:00.0" (:finish segmentti))))))