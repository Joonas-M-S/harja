(ns harja.tiedot.kanavat.hallinta.kohteiden-luonti-test
  (:require [harja.tiedot.kanavat.hallinta.kohteiden-luonti :as tiedot]
            [clojure.test :refer-macros [deftest is testing]]
            [harja.testutils.tuck-apurit :refer-macros [vaadi-async-kutsut] :refer [e!]]
            
            [harja.domain.kanavat.kohdekokonaisuus :as kok]
            [harja.domain.kanavat.kohde :as kohde]
            [harja.domain.muokkaustiedot :as m]
            [harja.domain.urakka :as ur]))

(deftest kohderivit
  (is (= [{::kohde/id 1
           ::kohde/tyyppi :sulku
           ::kok/id 1
           ::kok/nimi "Foobar"}
          {::kohde/id 2
           ::kohde/tyyppi :silta
           ::kohde/nimi "komea silta"
           ::kok/id 1
           ::kok/nimi "Foobar"}
          {::kohde/id 3
           ::kohde/tyyppi :sulku
           ::kok/id 2
           ::kok/nimi "Bazbar"}]
         (tiedot/kohderivit
           [{::kok/id 1
             ::kok/nimi "Foobar"
             ::kok/kohteet [{::kohde/id 1
                                ::kohde/tyyppi :sulku}
                               {::kohde/id 2
                                ::kohde/tyyppi :silta
                                ::kohde/nimi "komea silta"}]}
            {::kok/id 2
             ::kok/nimi "Bazbar"
             ::kok/kohteet [{::kohde/id 3
                                ::kohde/tyyppi :sulku}]}]))))

(deftest kanavat
  (is (= [{::kok/id 1
           ::kok/nimi "Foobar"}
          {::kok/id 2
           ::kok/nimi "Bazbar"}]
         (tiedot/kanavat
           [{::kok/id 1
             ::kok/nimi "Foobar"
             :huahuhue "joo"
             ::kok/kohteet [{::kohde/id 1
                                ::kohde/tyyppi :sulku}
                               {::kohde/id 2
                                ::kohde/tyyppi :silta
                                ::kohde/nimi "komea silta"}]}
            {::kok/id 2
             ::kok/nimi "Bazbar"
             :huahuhue "joo"
             ::kok/kohteet [{::kohde/id 3
                                ::kohde/tyyppi :sulku}]}]))))

(deftest voi-tallentaa?
  (is (true?
        (tiedot/kohteet-voi-tallentaa? {:kanava 1
                                        :kohteet [{::kohde/tyyppi 1}
                                                  {::kohde/tyyppi 2}]})))

  (is (false?
        (tiedot/kohteet-voi-tallentaa? {:kanava nil
                                        :kohteet [{::kohde/tyyppi 1}
                                                  {::kohde/tyyppi 2}]})))

  (is (false?
        (tiedot/kohteet-voi-tallentaa? {:kanava 1
                                        :kohteet []})))

  (is (false?
        (tiedot/kohteet-voi-tallentaa? {:kanava 1
                                        :kohteet [{::kohde/tyyppi nil}]})))

  (is (false?
        (tiedot/kohteet-voi-tallentaa? {:kanava 1
                                        :kohteet [{::kohde/tyyppi nil}
                                                  {::kohde/tyyppi true}]}))))

(deftest muokattavat-kohteet
  (is (= {:foo :bar}
         (tiedot/muokattavat-kohteet {:lomakkeen-tiedot {:kohteet {:foo :bar}}}))))

(deftest tallennusparametrit
  (is (= [{::kohde/nimi :foo
           ::kohde/id 1
           ::kohde/kanava-id 1
           ::kohde/tyyppi :sulku
           ::m/poistettu? true}
          {::kohde/nimi :foo
           ::kohde/id 2
           ::kohde/kanava-id 1
           ::kohde/tyyppi :sulku}]
         (tiedot/tallennusparametrit
           {:kanava {::kok/id 1}
            :kohteet [{::kohde/nimi :foo
                       :id 1
                       ::kohde/kanava-id 1
                       ::kohde/tyyppi :sulku
                       :poistettu true}
                      {::kohde/nimi :foo
                       :id 2
                       ::kohde/kanava-id 1
                       ::kohde/tyyppi :sulku}]}))))

(deftest kohteen-urakat
  (is (= "A, B, C"
         (tiedot/kohteen-urakat
           {::kohde/urakat [{::ur/nimi "B"}
                            {::ur/nimi "C"}
                            {::ur/nimi "A"}]}))))

(deftest kohteen-kuuluminen-urakkaan
  (is (true?
        (tiedot/kohde-kuuluu-urakkaan?
          {:uudet-urakkaliitokset {}}
          {::kohde/urakat [{:foo :bar}
                           {:id 1}]}
          {:foo :bar})))

  (is (true?
        (tiedot/kohde-kuuluu-urakkaan?
          {:uudet-urakkaliitokset {[666 1] true}}
          {::kohde/id 666
           ::kohde/urakat [{:foo :bar}
                           {:id 1}]}
          {:foo :bar})))

  (is (false?
        (tiedot/kohde-kuuluu-urakkaan?
          {:uudet-urakkaliitokset {[666 1] false}}
          {::kohde/id 666
           ::kohde/urakat [{:foo :bar}
                           {:id 1}]}
          {:foo :bar})))

  (is (false?
        (tiedot/kohde-kuuluu-urakkaan?
          {:uudet-urakkaliitokset {}}
          {::kohde/urakat [{:foo :bar}
                           {:id 1}]}
          {:foo :baz})))

  (is (false?
        (tiedot/kohde-kuuluu-urakkaan?
          {:uudet-urakkaliitokset {}}
          {::kohde/urakat []}
          {:foo :bar}))))

(deftest poista-kohde-kohteista
  (is (= [{:id 1}
          {:id 3}]
         (tiedot/poista-kohde
           [{:id 1}
            {:id 2}
            {:id 3}]
           {:id 2}))))

(deftest nakymaan-tuleminen
  (is (true? (:nakymassa? (e! (tiedot/->Nakymassa? true)))))
  (is (false? (:nakymassa? (e! (tiedot/->Nakymassa? false))))))

(deftest kohteiden-hakeminen
  (vaadi-async-kutsut
    #{tiedot/->KohteetHaettu tiedot/->KohteetEiHaettu}
    (is (true? (:kohteiden-haku-kaynnissa? (e! (tiedot/->HaeKohteet))))))

  (vaadi-async-kutsut
    #{}
    (is (= {:kohteiden-haku-kaynnissa? true}
           (e! (tiedot/->HaeKohteet) {:kohteiden-haku-kaynnissa? true})))))

(deftest kohteet-haettu
  (is (= {:kohteiden-haku-kaynnissa? false
          :kanavat [{::kok/id 1
                     ::kok/nimi "Foobar"}
                    {::kok/id 2
                     ::kok/nimi "Bazbar"}]
          :kohderivit [{::kohde/id 1
                        ::kohde/tyyppi :sulku
                        ::kok/id 1
                        ::kok/nimi "Foobar"}
                       {::kohde/id 2
                        ::kohde/tyyppi :silta
                        ::kohde/nimi "komea silta"
                        ::kok/id 1
                        ::kok/nimi "Foobar"}
                       {::kohde/id 3
                        ::kohde/tyyppi :sulku
                        ::kok/id 2
                        ::kok/nimi "Bazbar"}]}
         (e! (tiedot/->KohteetHaettu [{::kok/id 1
                                       ::kok/nimi "Foobar"
                                       ::kok/kohteet [{::kohde/id 1
                                                          ::kohde/tyyppi :sulku}
                                                         {::kohde/id 2
                                                          ::kohde/tyyppi :silta
                                                          ::kohde/nimi "komea silta"}]}
                                      {::kok/id 2
                                       ::kok/nimi "Bazbar"
                                       ::kok/kohteet [{::kohde/id 3
                                                          ::kohde/tyyppi :sulku}]}])))))

(deftest kohteet-ei-haettu
  (is (= {:kohteiden-haku-kaynnissa? false}
         (e! (tiedot/->KohteetEiHaettu {})))))

(deftest avaa-lomake
  (is (= {:kohdelomake-auki? true}
         (e! (tiedot/->AvaaKohdeLomake)))))

(deftest sulje-lomake
  (is (= {:kohdelomake-auki? false
          :lomakkeen-tiedot nil}
         (e! (tiedot/->SuljeKohdeLomake)))))

(deftest valitse-kanava
  (is (= {:lomakkeen-tiedot {:kanava {::kok/id 1}
                             :kohteet [{::kok/id 1 :id 1}
                                       {::kok/id 1 :id 3}]}
          :kohderivit [{::kok/id 1 :id 1}
                       {::kok/id 2 :id 2}
                       {::kok/id 1 :id 3}
                       {::kok/id 3 :id 4}
                       {::kok/id 2 :id 5}]}
         (e! (tiedot/->ValitseKanava {::kok/id 1})
             {:kohderivit [{::kok/id 1 :id 1}
                           {::kok/id 2 :id 2}
                           {::kok/id 1 :id 3}
                           {::kok/id 3 :id 4}
                           {::kok/id 2 :id 5}]}))))

(deftest kohteiden-lisays
  (is (= {:lomakkeen-tiedot {:kohteet [{:foo :bar}]
                             :kanava 1}}
         (e! (tiedot/->LisaaKohteita [{:foo :bar}])
             {:lomakkeen-tiedot {:kohteet [{:baz :bar}]
                                 :kanava 1}}))))

(deftest kohteiden-tallennus
  (vaadi-async-kutsut
    #{tiedot/->KohteetTallennettu tiedot/->KohteetEiTallennettu}
    (is (true? (:kohteiden-tallennus-kaynnissa? (e! (tiedot/->TallennaKohteet))))))

  (vaadi-async-kutsut
    #{}
    (is (= {:kohteiden-tallennus-kaynnissa? true}
           (e! (tiedot/->TallennaKohteet) {:kohteiden-tallennus-kaynnissa? true})))))

(deftest kohteet-tallennettu
  (is (= {:kohderivit [{::kohde/id 1
                        ::kohde/tyyppi :sulku
                        ::kok/id 1
                        ::kok/nimi "Foobar"
                        :rivin-teksti "FIXME"}
                       {::kohde/id 2
                        ::kohde/tyyppi :silta
                        ::kohde/nimi "komea silta"
                        ::kok/id 1
                        ::kok/nimi "Foobar"
                        :rivin-teksti "FIXME"}
                       {::kohde/id 3
                        ::kohde/tyyppi :sulku
                        ::kok/id 2
                        ::kok/nimi "Bazbar"
                        :rivin-teksti "FIXME"}]
          :kanavat [{::kok/id 1
                     ::kok/nimi "Foobar"}
                    {::kok/id 2
                     ::kok/nimi "Bazbar"}]
          :kohdelomake-auki? false
          :lomakkeen-tiedot nil
          :kohteiden-tallennus-kaynnissa? false}
         (e! (tiedot/->KohteetTallennettu [{::kok/id 1
                                            ::kok/nimi "Foobar"
                                            ::kok/kohteet [{::kohde/id 1
                                                               ::kohde/tyyppi :sulku}
                                                              {::kohde/id 2
                                                               ::kohde/tyyppi :silta
                                                               ::kohde/nimi "komea silta"}]}
                                           {::kok/id 2
                                            ::kok/nimi "Bazbar"
                                            ::kok/kohteet [{::kohde/id 3
                                                               ::kohde/tyyppi :sulku}]}])))))

(deftest kohteet-ei-tallennettu
  (is (= {:kohteiden-tallennus-kaynnissa? false}
         (e! (tiedot/->KohteetEiTallennettu {})))))

(deftest aloita-haku
  (vaadi-async-kutsut
    #{tiedot/->UrakatHaettu tiedot/->UrakatEiHaettu}

    (is (= {:urakoiden-haku-kaynnissa? true}
           (e! (tiedot/->AloitaUrakoidenHaku))))))

(deftest urakat-haettu
  (is (= {:urakoiden-haku-kaynnissa? false
          :urakat [{::ur/nimi "Foo" ::ur/id 1}
                   {::ur/nimi "Bar" ::ur/id 2}]}
         (e! (tiedot/->UrakatHaettu
               [{:id 1 :nimi "Foo"}
                {:id 2 :nimi "Bar"}])))))

(deftest urakat-ei-haettu
  (is (= {:urakoiden-haku-kaynnissa? false}
         (e! (tiedot/->UrakatEiHaettu :virhe)))))

(deftest urakan-valinta
  (is (= {:valittu-urakka {:foo :bar}}
         (e! (tiedot/->ValitseUrakka {:foo :bar})))))

(deftest kohteiden-liittaminen-urakkaan
  (vaadi-async-kutsut
    #{tiedot/->LiitoksetPaivitetty tiedot/->LiitoksetEiPaivitetty}
    (is (= {:uudet-urakkaliitokset {[1 2] true}}
           (e! (tiedot/->PaivitaKohteidenUrakkaliitokset)
               {:uudet-urakkaliitokset {[1 2] true}
                :liittaminen-kaynnissa? true})))))

(deftest kohde-liitetty
  (is (= {:liittaminen-kaynnissa? true
          :uudet-urakkaliitokset {[1 2] true}}
         (e! (tiedot/->LiitoksetPaivitetty [])
             {:liittaminen-kaynnissa? false
              :kohderivit []
              :uudet-urakkaliitokset {}}))))

(deftest kohde-ei-liitetty
  (is (= {:liittaminen-kaynnissa? true}
         (e! (tiedot/->LiitoksetEiPaivitetty)
             {:liittaminen-kaynnissa? false}))))