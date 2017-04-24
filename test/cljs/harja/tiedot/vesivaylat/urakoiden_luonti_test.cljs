(ns harja.tiedot.vesivaylat.urakoiden-luonti-test
  (:require [harja.tiedot.vesivaylat.urakoiden-luonti :as luonti]
            [harja.domain.urakka :as u]
            [harja.domain.sopimus :as s]
            [clojure.test :refer-macros [deftest is testing]]
            [harja.tuck-apurit :refer-macros [vaadi-async-kutsut] :refer [e! e-tila!]]))

(deftest urakan-valinta
  (let [ur {:foobar 1}]
    (is (= ur (:valittu-urakka (e! luonti/->ValitseUrakka ur))))))

(deftest nakymaan-tuleminen
  (is (true? (:nakymassa? (e! luonti/->Nakymassa? true))))
  (is (false? (:nakymassa? (e! luonti/->Nakymassa? false)))))

(deftest uuden-urakan-luonnin-aloitus
  (is (= luonti/uusi-urakka (:valittu-urakka (e! luonti/->UusiUrakka)))))

(deftest tallentamisen-aloitus
  (vaadi-async-kutsut
    #{luonti/->UrakkaTallennettu luonti/->UrakkaEiTallennettu}

    (is (true? (:tallennus-kaynnissa? (e-tila! luonti/->TallennaUrakka {:id 1} {:haetut-urakat []}))))))

(deftest tallentamisen-valmistuminen
  (testing "Uuden urakan tallentaminen"
    (let [vanhat [{::u/id 1} {::u/id 2}]
          uusi {::u/id 3}
          tulos (e-tila! luonti/->UrakkaTallennettu uusi {:haetut-urakat vanhat})]
      (is (false? (:tallennus-kaynnissa? tulos)))
      (is (nil? (:valittu-urakka tulos)))
      (is (= (conj vanhat uusi) (:haetut-urakat tulos)))))

  (testing "Urakan muokkaaminen"
    (let [vanhat [{::u/id 1 :nimi :a} {::u/id 2 :nimi :b}]
          uusi {::u/id 2 :nimi :bb}
          tulos (e-tila! luonti/->UrakkaTallennettu uusi {:haetut-urakat vanhat})]
      (is (false? (:tallennus-kaynnissa? tulos)))
      (is (nil? (:valittu-urakka tulos)))
      (is (= [{::u/id 1 :nimi :a} {::u/id 2 :nimi :bb}] (:haetut-urakat tulos))))))

(deftest tallentamisen-epaonnistuminen
  (let [tulos (e! luonti/->UrakkaEiTallennettu "virhe")]
    (is (false? (:tallennus-kaynnissa? tulos)))
    (is (nil? (:valittu-urakka tulos)))))

(deftest urakan-muokkaaminen-lomakkeessa
  (let [ur {:nimi :foobar}]
    (is (= ur (:valittu-urakka (e! luonti/->UrakkaaMuokattu ur))))))

(deftest hakemisen-aloitus
  (vaadi-async-kutsut
    #{luonti/->UrakatHaettu luonti/->UrakatEiHaettu}
    (is (true? (:urakoiden-haku-kaynnissa? (e! luonti/->HaeUrakat {:id 1}))))))

(deftest hakemisen-valmistuminen
  (let [urakat [{:id 1 :nimi :a} {:id 2 :nimi :b}]
        tulos (e! luonti/->UrakatHaettu urakat)]
    (is (false? (:urakoiden-haku-kaynnissa? tulos)))
    (is (= [{:id 1 :nimi :a} {:id 2 :nimi :b}] (:haetut-urakat tulos)))))

(deftest hakemisen-epaonnistuminen
  (let [tulos (e! luonti/->UrakatEiHaettu "virhe")]
    (is (false? (:urakoiden-haku-kaynnissa? tulos)))))

(deftest sopimuksen-paivittaminen
  (let [testaa (fn [tila annettu haluttu]
                 (= (-> (e-tila! luonti/->PaivitaSopimuksetGrid annettu {:valittu-urakka {::u/sopimukset tila}})
                        (get-in [:valittu-urakka ::u/sopimukset]))
                    haluttu))]
    (testing "Rivin lisääminen gridiin"
      (is (testaa [{::s/id 1 ::s/paasopimus-id nil}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id -2 ::s/paasopimus-id nil}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id -2 ::s/paasopimus-id nil}])))

    (testing "Rivin asettaminen sopimukseksi gridiin"
      (is (testaa [{::s/id 1 ::s/paasopimus-id nil} {::s/id -2 ::s/paasopimus-id nil}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id nil}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id nil}])))

    ;; Pääsopimus asetetaan muualla..

    (testing "Sopimuksen lisääminen gridiin, kun pääsopimus on jo asetettu"
      (is (testaa [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id 1}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id 1} {::s/id -3 ::s/paasopimus-id nil}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id 1} {::s/id -3 ::s/paasopimus-id 1}])))

    (testing "Rivin poistaminen gridistä"
      (is (testaa [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id 1} {::s/id -3 ::s/paasopimus-id 1}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id 1 :poistettu true} {::s/id -3 ::s/paasopimus-id nil :poistettu true}]
                  [{::s/id 1 ::s/paasopimus-id nil} {::s/id 2 ::s/paasopimus-id 1 :poistettu true} {::s/id -3 ::s/paasopimus-id 1 :poistettu true}])))))


(deftest lomakevaihtoehtojen-hakemisen-aloitus
  (vaadi-async-kutsut
    #{luonti/->LomakevaihtoehdotHaettu luonti/->LomakevaihtoehdotEiHaettu}
    (is (= {:foo :bar} (e-tila! luonti/->HaeLomakevaihtoehdot {:id 1} {:foo :bar})))))

(deftest lomakevaihtoehtojen-hakemisen-valmistuminen
  (let [hy [{:id 1}]
        ur [{:id 2}]
        h [{:id 3}]
        s [{:id 4}]
        payload {:hallintayksikot hy
                 :urakoitsijat ur
                 :hankkeet h
                 :sopimukset s}
        app (e! luonti/->LomakevaihtoehdotHaettu payload)]
    (is (= hy (:haetut-hallintayksikot app)))
    (is (= ur (:haetut-urakoitsijat app)))
    (is (= h (:haetut-hankkeet app)))
    (is (= s (:haetut-sopimukset app)))))

(deftest lomakevaihtoehtojen-hakemisen-epaonnistuminen
  (is (= {:foo :bar} (e-tila! luonti/->LomakevaihtoehdotEiHaettu "virhe" {:foo :bar}))))

(deftest sahke-lahetyksen-aloitus
  (vaadi-async-kutsut
    #{luonti/->SahkeeseenLahetetty luonti/->SahkeeseenEiLahetetty}
    (is (= {:kaynnissa-olevat-sahkelahetykset #{1}}
           (e-tila! luonti/->LahetaUrakkaSahkeeseen {:id 1} {:kaynnissa-olevat-sahkelahetykset #{}})))))

(deftest sahke-lahetyksen-valmistuminen
  (is (= {:kaynnissa-olevat-sahkelahetykset #{}}
         (e-tila! luonti/->SahkeeseenLahetetty {} {:id 1} {:kaynnissa-olevat-sahkelahetykset #{1}}))))

(deftest sahke-lahetyksen-epaonnistuminen
  (is (= {:kaynnissa-olevat-sahkelahetykset #{}}
         (e-tila! luonti/->SahkeeseenEiLahetetty "virhe" {:id 1} {:kaynnissa-olevat-sahkelahetykset #{1}}))))

(deftest paasopimuksen-kasittely
  (testing "Löydetään aina vain yksi pääsopimus"
    (is (false? (sequential? (luonti/paasopimus [{:id 1 :paasopimus nil}
                                                 {:id 2 :paasopimus nil}
                                                 {:id 3 :paasopimus 1}
                                                 {:id 4 :paasopimus 2}])))))

  (testing "Pääsopimus löytyy sopimusten joukosta"
    (is (= {:id 1 :paasopimus nil} (luonti/paasopimus [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1}])))
    (is (= {:id 1 :paasopimus nil} (luonti/paasopimus [{:id 2 :paasopimus 1} {:id 1 :paasopimus nil}]))))

  (testing "Jos pääsopimusta ei ole, sitä ei myöskään palauteta"
    (is (= nil (luonti/paasopimus [{:id 1 :paasopimus 2} {:id 3 :paasopimus 2}])))
    (is (= nil (luonti/paasopimus [{:id 1 :paasopimus nil} {:id 3 :paasopimus nil}])))
    (is (= nil (luonti/paasopimus [])))
    (is (= nil (luonti/paasopimus [{:id 1 :paasopimus nil}])))
    (is (= nil (luonti/paasopimus [{:id nil :paasopimus nil}]))))

  (testing "Pääsopimusta päätellessä ei välitetä poistetuista sopimuksista tai uusista riveistä"
    (is (= nil (luonti/paasopimus [{:id 1 :paasopimus nil} {:id 3 :paasopimus 1 :poistettu true}])))
    (is (= nil (luonti/paasopimus [{:id 1 :paasopimus nil} {:id- 3 :paasopimus 1}]))))

  (testing "Sopimus tunnistetaan pääsopimukseksi"
    (is (true? (luonti/paasopimus? [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1}] {:id 1 :paasopimus nil})))
    (is (true? (luonti/paasopimus? [{:id 2 :paasopimus 1} {:id 1 :paasopimus nil}] {:id 1 :paasopimus nil}))))

  (testing "Tunnistetaan, että sopimus ei ole pääsopimus"
    (is (false? (luonti/paasopimus? [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1}] {:id 2 :paasopimus 1})))
    (is (false? (luonti/paasopimus? [{:id 2 :paasopimus 1} {:id 1 :paasopimus nil}] {:id 2 :paasopimus 1}))))

  (testing "Jos pääsopimusta ei ole, sopimusta ei tunnisteta pääsopimukseksi"
    (is (false? (luonti/paasopimus? [{:id 1 :paasopimus nil} {:id 2 :paasopimus nil} {:id 3 :paasopimus nil}] {:id 2 :paasopimus nil})))
    (is (false? (luonti/paasopimus? [{:id 2 :paasopimus nil} {:id 1 :paasopimus nil}] {:id 1 :paasopimus nil})))
    (is (false? (luonti/paasopimus? [{:id 2 :paasopimus nil} {:id 1 :paasopimus nil}] {:id nil :paasopimus nil}))))

  (testing "Uuden pääsopimuksen asettaminen"
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus nil} {:id 2 :paasopimus nil}]
                                              {:id 1 :paasopimus nil})))
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus nil} {:id 2 :paasopimus nil} {:id 3 :paasopimus nil}]
                                              {:id 1 :paasopimus nil})))
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id -3 :paasopimus 1}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus nil} {:id 2 :paasopimus nil} {:id -3 :paasopimus nil}]
                                              {:id 1 :paasopimus nil})))
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1 :poistettu true}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus nil} {:id 2 :paasopimus nil} {:id 3 :paasopimus nil :poistettu true}]
                                              {:id 1 :paasopimus nil}))))

  (testing "Pääsopimuksen muuttaminen"
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus 2} {:id 2 :paasopimus nil}]
                                              {:id 1 :paasopimus nil})))
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus 2} {:id 2 :paasopimus nil} {:id 3 :paasopimus 2}]
                                              {:id 1 :paasopimus nil})))
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id -3 :paasopimus 1}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus 2} {:id 2 :paasopimus nil} {:id -3 :paasopimus 2}]
                                              {:id 1 :paasopimus nil})))
    (is (= [{:id 1 :paasopimus nil} {:id 2 :paasopimus 1} {:id 3 :paasopimus 1 :poistettu true}]
           (luonti/sopimukset-paasopimuksella [{:id 1 :paasopimus 2} {:id 2 :paasopimus nil} {:id 3 :paasopimus 2 :poistettu true}]
                                              {:id 1 :paasopimus nil})))))

(deftest urakan-sopimusvaihtoehdot
  (let [kaikki-sopimukset [{:id 1 :urakka {:id 1}} {:id 2 :urakka {:id 1}} {:id 3 :urakka nil} {:id 4 :urakka nil}]
        urakan-sopimukset [{:id 1 :urakka {:id 1}} {:id 2 :urakka {:id 1}} {:id 3 :urakka nil}]]
    (is (= [{:id 4 :urakka nil}] (luonti/vapaat-sopimukset kaikki-sopimukset urakan-sopimukset)))
    (is (true? (empty? (luonti/vapaat-sopimukset [{:id 1 :urakka {:id 1}} {:id 2 :urakka {:id 1}} {:id 3 :urakka {:id 1}} {:id 4 :urakka {:id 1}}] urakan-sopimukset))))
    (is (true? (empty? (luonti/vapaat-sopimukset [{:id 1 :urakka nil}] [{:id 1 :urakka nil}])))))

  (is (true? (luonti/vapaa-sopimus? {:urakka nil})))
  (is (false? (luonti/vapaa-sopimus? {:urakka {:id 1}}))))
