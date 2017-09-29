(ns harja.palvelin.palvelut.kayttajatiedot-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.haku :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.palvelin.palvelut.kayttajatiedot :as kayttajatiedot]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :kayttajatiedot (component/using
                                          (kayttajatiedot/->Kayttajatiedot)
                                          [:http-palvelin :db])))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once jarjestelma-fixture)

(deftest yhteydenpito-vastaanottajat-toimii
  (let [tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :yhteydenpito-vastaanottajat +kayttaja-jvh+ nil)]

    (is (= (count tulos) 7))
    (is (= (vec (distinct (mapcat keys tulos))) [:etunimi :sukunimi :sahkoposti]))))

(deftest yhdista-kayttajan-urakat-alueittain
  (let [ely-kaakkoissuomi {:id 7, :nimi "Kaakkois-Suomi", :elynumero 3}]

    (let [urakat-a [{:tyyppi :paallystys,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}]}
                    {:tyyppi :hoito,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 1, :nimi "Joku hoidon urakka", :alue nil}]}]
          urakat-b [{:tyyppi :paallystys,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 2, :nimi "Joku tienpäällystysjuttu", :alue nil}]}]]
      (is (= (kayttajatiedot/yhdista-kayttajan-urakat-alueittain
               urakat-a
               urakat-b))
          [{:tyyppi :paallystys,
            :hallintayksikko ely-kaakkoissuomi,
            :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}
                     {:id 2, :nimi "Joku tienpäällystysjuttu", :alue nil}]}
           {:tyyppi :hoito,
            :hallintayksikko ely-kaakkoissuomi,
            :urakat [{:id 1, :nimi "Joku hoidon urakka", :alue nil}]}]))

    (let [urakat-a [{:tyyppi :paallystys,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}]}
                    {:tyyppi :hoito,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 1, :nimi "Joku hoidon urakka", :alue nil}]}]
          urakat-b [{:tyyppi :hoito,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 24, :nimi "Joku hoitourakkajuttu", :alue nil}]}]]
      (is (= (kayttajatiedot/yhdista-kayttajan-urakat-alueittain
               urakat-a
               urakat-b))
          [{:tyyppi :paallystys,
            :hallintayksikko ely-kaakkoissuomi,
            :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}]}
           {:tyyppi :hoito,
            :hallintayksikko ely-kaakkoissuomi,
            :urakat [{:id 1, :nimi "Joku hoidon urakka", :alue nil}]}
           {:tyyppi :hoito,
            :hallintayksikko ely-kaakkoissuomi,
            :urakat [{:id 24, :nimi "Joku hoitourakkajuttu", :alue nil}]}]))

    (let [urakat-a [{:tyyppi :paallystys,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}]}]
          urakat-b [{:tyyppi :paallystys,
                     :hallintayksikko ely-kaakkoissuomi,
                     :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}]}]]
      (is (= (kayttajatiedot/yhdista-kayttajan-urakat-alueittain
               urakat-a
               urakat-b))
          [{:tyyppi :paallystys,
            :hallintayksikko ely-kaakkoissuomi,
            :urakat [{:id 18, :nimi "Tienpäällystysurakka KAS ELY 1 2015", :alue nil}]}]))

    (let [urakat-a []
          urakat-b []]
      (is (= (kayttajatiedot/yhdista-kayttajan-urakat-alueittain
               urakat-a
               urakat-b))
          []))))


