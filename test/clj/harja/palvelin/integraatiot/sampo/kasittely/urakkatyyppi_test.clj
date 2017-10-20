(ns harja.palvelin.integraatiot.sampo.kasittely.urakkatyyppi-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.sampo.kasittely.urakkatyyppi :as urakkatyyppi]))

(deftest tarkista-liikennemuodo-paattely
  (is (= "r" (urakkatyyppi/paattele-liikennemuoto "R")) "Rautatie liikennemuoto päätellään oikein.")
  (is (= "v" (urakkatyyppi/paattele-liikennemuoto "V")) "Vesiväylä liikennemuoto päätellään oikein.")
  (is (= "t" (urakkatyyppi/paattele-liikennemuoto "T")) "Tie liikennemuoto päätellään oikein.")
  (is (= "t" (urakkatyyppi/paattele-liikennemuoto nil)) "Tyhjä arvo päätellään oletuksena tie liikennemuodoksi.")
  (is (= "t" (urakkatyyppi/paattele-liikennemuoto "123")) "Ei-validi arvo päätellään oletuksena tie liikennemuodoksi."))

(deftest tarkista-urakkatyypin-paattely
  (is (= "paallystys" (urakkatyyppi/paattele-urakkatyyppi "TYP")) "Päällystys urakkatyyppi päätellään oikein.")
  (is (= "valaistus" (urakkatyyppi/paattele-urakkatyyppi "TYV")) "Valaistus urakkatyyppi päätellään oikein.")
  (is (= "paallystys" (urakkatyyppi/paattele-urakkatyyppi "TY")) "Ilman alityyppiä tuotu ylläpidon urakka merkitään päällystykseksi.")
  (is (= "siltakorjaus" (urakkatyyppi/paattele-urakkatyyppi "TYS")) "Siltakorjaus urakkatyyppi päätellään oikein.")
  (is (= "tiemerkinta" (urakkatyyppi/paattele-urakkatyyppi "TYT")) "Tiemerkintä urakkatyyppi päätellään oikein.")
  (is (= "paallystys" (urakkatyyppi/paattele-urakkatyyppi "THP")) "Päällystys urakkatyyppi päätellään oikein.")
  (is (= "valaistus" (urakkatyyppi/paattele-urakkatyyppi "THV")) "Valaistus urakkatyyppi päätellään oikein.")
  (is (= "siltakorjaus" (urakkatyyppi/paattele-urakkatyyppi "THS")) "Siltakorjaus urakkatyyppi päätellään oikein.")
  (is (= "tiemerkinta" (urakkatyyppi/paattele-urakkatyyppi "THT")) "Tiemerkintä urakkatyyppi päätellään oikein.")
  (is (= "tekniset-laitteet" (urakkatyyppi/paattele-urakkatyyppi "THL")) "Tekniset laittet urakkatyyppi päätellään oikein.")
  (is (= "tekniset-laitteet" (urakkatyyppi/paattele-urakkatyyppi "TYL")) "Tekniset laittet urakkatyyppi päätellään oikein.")
  (is (= "vesivayla-kanavien-hoito" (urakkatyyppi/paattele-urakkatyyppi "VHK")) "Hoito urakkatyyppi päätellään oikein.")
  (is (= "vesivayla-kanavien-yllapito" (urakkatyyppi/paattele-urakkatyyppi "VYK")) "Hoito urakkatyyppi päätellään oikein.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi "TH")) "Hoito urakkatyyppi päätellään oikein.")
  (is (= "paallystys" (urakkatyyppi/paattele-urakkatyyppi "typ")) "Päättely toimii pienillä kirjaimilla.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi "TH123")) "Hoito urakkatyyppi päätellään oikein.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi "")) "Tyhjä arvo päätellään oletuksena hoito urakkatyypiksi.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi nil)) "Tyhjä arvo päätellään oletuksena hoito urakkatyypiksi.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi "123")) "Ei-validi arvo päätellään oletuksena hoito urakkatyypiksi.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi "")) "Liian lyhyt arvo päätellään hoito urakkatyypiksi.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi "1")) "Liian lyhyt arvo päätellään hoito urakkatyypiksi.")
  (is (= "hoito" (urakkatyyppi/paattele-urakkatyyppi nil)) "Nil arvo päätellään hoito urakkatyypiksi."))

(deftest tarkista-sampon-urakkatyypin-rakentaminen
  (is (= "TH" (urakkatyyppi/rakenna-sampon-tyyppi "hoito")))
  (is (= "TYP" (urakkatyyppi/rakenna-sampon-tyyppi "paallystys")))
  (is (= "TYT" (urakkatyyppi/rakenna-sampon-tyyppi "tiemerkinta")))
  (is (= "TYV" (urakkatyyppi/rakenna-sampon-tyyppi "valaistus")))
  (is (= "TYS" (urakkatyyppi/rakenna-sampon-tyyppi "siltakorjaus")))
  (is (= "TYL" (urakkatyyppi/rakenna-sampon-tyyppi "tekniset-laitteet")))
  (is (= "VYK" (urakkatyyppi/rakenna-sampon-tyyppi "vesivayla-kanavien-korjaus")))
  (is (= "VHK" (urakkatyyppi/rakenna-sampon-tyyppi "vesivayla-kanavien-hoito")))
  (is (thrown-with-msg? RuntimeException #"Tuntematon urakkatyyppi: tuntematon"
                        (urakkatyyppi/rakenna-sampon-tyyppi "tuntematon"))))
