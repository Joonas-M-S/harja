(ns harja.palvelin.komponentit.todennus-test
  (:require [harja.palvelin.komponentit.todennus :as todennus]
            [harja.domain.oikeudet :as oikeudet]
            [clojure.test :as t :refer [deftest is use-fixtures]]))

(def testiroolit {"root" {:nimi "root"
                          :kuvaus "Pääkäyttäjä"}
                  "valvoja" {:nimi "valvoja"
                             :kuvaus "Urakan valvoja"
                             :linkki "urakka"}
                  "katsoja" {:nimi "katsoja"
                             :kuvaus "Katsoja"}
                  "urakoitsija" {:nimi "urakoitsija"
                                 :kuvaus "Urakoitsijan käyttäjä"
                                 :linkki "urakoitsija"}
                  "paivystaja" {:nimi "paivystaja"
                                :kuvaus "Urakan päivystäjä"
                                :linkki "urakka"}
                  "Kayttaja" {:nimi "Kayttaja"
                              :kuvaus "Urakoitsijan käyttäjä"
                              :linkki "urakoitsija"}})

(def urakat {"u123" 666})
(def urakoitsijat {"Y123456-7" 42})
(def urakat-monta {"PR00013343" 13343 "PR00014303" 14303})

(def oikeudet (partial todennus/kayttajan-roolit urakat urakoitsijat testiroolit))

(deftest lue-oikeudet-oam-groupsista

  (is (= {:roolit #{"root"} :urakkaroolit {} :organisaatioroolit {}}
         (oikeudet "root")))

  (is (= {:roolit #{} :urakkaroolit {666 #{"valvoja"}} :organisaatioroolit {}}
         (oikeudet "u123_valvoja")))

  (is (= {:roolit #{} :urakkaroolit {666 #{"paivystaja"}}
          :organisaatioroolit {42 #{"urakoitsija"}}}
         (oikeudet "Y123456-7_urakoitsija,u123_paivystaja"))))

(deftest liito-rooli-ei-sekoitu-harja-rooliin
  (is (= {:roolit #{} :urakkaroolit {666 #{"paivystaja"}}
          :organisaatioroolit {42 #{"urakoitsija"}}}
         (oikeudet "Y123456-7_urakoitsija,u123_paivystaja,Extranet_Liito_Kayttaja,Aina_öisin_valvoja"))))

(deftest tilaajan-kayttaja
  (is (= {:roolit             #{"Tilaajan_Kayttaja"}
          :organisaatioroolit {}
          :urakkaroolit       {}}
         (todennus/kayttajan-roolit urakat urakoitsijat oikeudet/roolit "Tilaajan_Kayttaja"))))

(deftest ely-peruskayttaja
  (is (= {:roolit             #{"ELY_Peruskayttaja"}
          :organisaatioroolit {}
          :urakkaroolit       {}}
         (todennus/kayttajan-roolit urakat urakoitsijat oikeudet/roolit "ELY_Peruskayttaja"))))

(deftest ely-urakanvalvoja
  (let [oam-groups "55746,39626,39627,27231,28875,30138,49544,29957,30006,44687,56406,PR00014281_ELY_Urakanvalvoja,PR00013343_ELY_Urakanvalvoja,28311,55550,PR00014258_ELY_Urakanvalvoja,26871,PR00014303_ELY_Urakanvalvoja,51204,49468,49469,28852,30050,30116,28851,PR00014273_ELY_Urakanvalvoja,32065,44594,31866,51805,51804,PR00014248_ELY_Urakanvalvoja,Extranet_Liito_Kayttaja,29778,44556,PR00013356_ELY_Urakanvalvoja,Extranet_Aura_Kayttaja,47074,47075,thuv,51544,51685,51684,55530,54206,56626,PR00014296_ELY_Urakanvalvoja,53865,29726,53864,r,56426,PR00014289_ELY_Urakanvalvoja,PR00014265_ELY_Urakanvalvoja"
        vastaus (todennus/kayttajan-roolit urakat-monta urakoitsijat oikeudet/roolit oam-groups)
        odotetut-roolit {:roolit             #{nil}
                         :organisaatioroolit {}
                         :urakkaroolit       {14303 #{"ELY_Urakanvalvoja"}
                                              13343 #{"ELY_Urakanvalvoja"}}}]
    (is (= vastaus odotetut-roolit))))