(ns harja.palvelin.raportointi.indeksitarkastus-test
  (:require [clojure.test :refer :all]
            [harja.testi :refer :all]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.toimenpidekoodit :refer :all]
            [harja.palvelin.palvelut.urakat :refer :all]
            [harja.palvelin.palvelut.raportit :as raportit]
            [harja.palvelin.komponentit.pdf-vienti :as pdf-vienti]
            [harja.palvelin.raportointi :refer :all :as raportointi]
            [harja.palvelin.raportointi.raportit.laskutusyhteenveto :as laskutusyhteenveto]
            [harja.palvelin.raportointi.testiapurit :as apurit]
            [harja.pvm :as pvm]

            [taoensso.timbre :as log]
            [com.stuartsierra.component :as component]
            [clj-time.coerce :as c]
            [clj-time.core :as t]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :pdf-vienti (component/using
                                      (pdf-vienti/luo-pdf-vienti)
                                      [:http-palvelin])
                        :raportointi (component/using
                                       (raportointi/luo-raportointi)
                                       [:db :pdf-vienti])
                        :raportit (component/using
                                    (raportit/->Raportit)
                                    [:http-palvelin :db :raportointi :pdf-vienti])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :once (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))

(deftest indeksiraportin-summa-sama-kuin-laskutusyhteenvedon-indeksien-summa
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :suorita-raportti
                                +kayttaja-jvh+
                                {:nimi :indeksitarkistus
                                 :konteksti "urakka"
                                 :urakka-id @oulun-alueurakan-2014-2019-id
                                 :parametrit {:alkupvm   (pvm/->pvm "1.8.2015")
                                              :loppupvm (pvm/->pvm "31.8.2015")
                                              :urakkatyyppi "hoito"}})
        taulukko (apurit/taulukko-otsikolla vastaus "Kaikki yhteensä")
        laskutusyhteenveto (laskutusyhteenveto/hae-laskutusyhteenvedon-tiedot
                                          (:db jarjestelma)
                                          +kayttaja-jvh+
                                          {:urakka-id @oulun-alueurakan-2014-2019-id
                                           :alkupvm   (pvm/->pvm "1.8.2015")
                                           :loppupvm (pvm/->pvm "31.8.2015")})
        laskutusyhteenveto-indeksien-nurkkasumma (reduce + (map :kaikki_laskutetaan_ind_korotus laskutusyhteenveto))]

    (is (vector? vastaus))
    (apurit/tarkista-raportti vastaus "Indeksitarkistusraportti Oulun alueurakka 2014-2019 01.08.2015 - 31.08.2015")

    (apurit/tarkista-taulukko-rivit
      taulukko

      (fn [[kuukausi kokhint ykshint erilliskust bonus muutos-ja-lisatyot vahinkojen-korjaukset
            akilliset-hoitotyot sanktiot suolabonus-ja-sakko
            yhteensa & _ ]]
        (and (= kuukausi "elo")
             (=marginaalissa? (apurit/raporttisolun-arvo kokhint) 228.38M)
             (=marginaalissa? (apurit/raporttisolun-arvo ykshint) 50.75M)
             (=marginaalissa? (apurit/raporttisolun-arvo erilliskust) 16.92M)
             (=marginaalissa? (apurit/raporttisolun-arvo bonus) 4.07)
             (=marginaalissa? (apurit/raporttisolun-arvo muutos-ja-lisatyot) 33.83M)
             (=marginaalissa? (apurit/raporttisolun-arvo vahinkojen-korjaukset) 16.92M)
             (=marginaalissa? (apurit/raporttisolun-arvo akilliset-hoitotyot) 16.92M)
             (=marginaalissa? (apurit/raporttisolun-arvo sanktiot) -30.45M)
             (=marginaalissa? (apurit/raporttisolun-arvo suolabonus-ja-sakko) -94.99M)
             (=marginaalissa? (apurit/raporttisolun-arvo yhteensa) 242.34M)

             (=marginaalissa? yhteensa laskutusyhteenveto-indeksien-nurkkasumma)))
      (fn [[yht & _]]
        (= "Yhteensä" yht)))))

(deftest raportin-suoritus-koko-maalle-toimii
  (let [vastaus (kutsu-palvelua (:http-palvelin jarjestelma)
                                :suorita-raportti
                                +kayttaja-jvh+
                                {:nimi :indeksitarkistus
                                 :konteksti "koko maa"
                                 :parametrit {:alkupvm (c/to-date (t/local-date 2014 1 1))
                                              :loppupvm (c/to-date (t/local-date 2015 12 31))
                                              :urakkatyyppi "hoito"}})]
    (is (vector? vastaus))
    (apurit/tarkista-raportti vastaus "Indeksitarkistusraportti KOKO MAA 01.01.2014 - 31.12.2015")

    ;; Kaikki yhteensä
    (let [otsikko "Kaikki yhteensä"
          taulukko (apurit/taulukko-otsikolla vastaus otsikko)]
      (apurit/tarkista-taulukko-sarakkeet taulukko
                                          {:otsikko "Kuukausi"}
                                          {:otsikko "Kokonais\u00ADhintaiset työt"}
                                          {:otsikko "Yksikkö\u00ADhintaiset työt"}
                                          {:otsikko "Erillis\u00ADkustannukset"}
                                          {:otsikko "Bonus"}
                                          {:otsikko "Muutos- ja lisä\u00ADtyöt"}
                                          {:otsikko "Vahinkojen korjaukset"}
                                          {:otsikko "Äkillinen hoitotyö"}
                                          {:otsikko "Sanktiot"}
                                          {:otsikko "Suolabonukset ja -sanktiot"}
                                          {:otsikko "Yhteensä (€)"})
      (apurit/tarkista-taulukko-kaikki-rivit taulukko
                                             (fn [[kuukausi kok-hint yks-hint er-kust bonus muutos vahinkojen-korjaukset akilliset-hoitotyot sanktiot
                                                   suolabonus yhteensa :as rivi]]
                                               (and (= (count rivi) 11)
                                                    (string? kuukausi)
                                                    (number? kok-hint)
                                                    (number? yks-hint)
                                                    (number? er-kust)
                                                    (number? bonus)
                                                    (number? muutos)
                                                    (number? vahinkojen-korjaukset)
                                                    (number? akilliset-hoitotyot)
                                                    (number? sanktiot)
                                                    (number? suolabonus)
                                                    (number? yhteensa)))))

    ;; Talvihoito
    (let [otsikko "Talvihoito"
          taulukko (apurit/taulukko-otsikolla vastaus otsikko)]
      (apurit/tarkista-taulukko-sarakkeet taulukko
                                          {:otsikko "Kuukausi"}
                                          {:otsikko "Kokonais\u00ADhintaiset työt"}
                                          {:otsikko "Yksikkö\u00ADhintaiset työt"}
                                          {:otsikko "Erillis\u00ADkustannukset"}
                                          {:otsikko "Bonus"}
                                          {:otsikko "Muutos- ja lisä\u00ADtyöt"}
                                          {:otsikko "Vahinkojen korjaukset"}
                                          {:otsikko "Äkillinen hoitotyö"}
                                          {:otsikko "Sanktiot"}
                                          {:otsikko "Suolabonukset ja -sanktiot"}
                                          {:otsikko "Yhteensä (€)"})
      (apurit/tarkista-taulukko-kaikki-rivit taulukko
                                             (fn [[kuukausi kok-hint yks-hint er-kust bonus muutos vahinkojen-korjaukset akilliset-hoitotyot sanktiot
                                                   suolabonus yhteensa :as rivi]]
                                               (and (= (count rivi) 11)
                                                    (string? kuukausi)
                                                    (number? kok-hint)
                                                    (number? yks-hint)
                                                    (number? er-kust)
                                                    (number? bonus)
                                                    (number? muutos)
                                                    (number? vahinkojen-korjaukset)
                                                    (number? akilliset-hoitotyot)
                                                    (number? sanktiot)
                                                    (number? suolabonus)
                                                    (number? yhteensa)))))

    ;; Liikenneympäristön hoito
    (let [otsikko "Liikenneympäristön hoito"
          taulukko (apurit/taulukko-otsikolla vastaus otsikko)]
      (apurit/tarkista-taulukko-sarakkeet taulukko
                                          {:otsikko "Kuukausi"}
                                          {:otsikko "Kokonais\u00ADhintaiset työt"}
                                          {:otsikko "Yksikkö\u00ADhintaiset työt"}
                                          {:otsikko "Erillis\u00ADkustannukset"}
                                          {:otsikko "Bonus"}
                                          {:otsikko "Muutos- ja lisä\u00ADtyöt"}
                                          {:otsikko "Vahinkojen korjaukset"}
                                          {:otsikko "Äkillinen hoitotyö"}
                                          {:otsikko "Sanktiot"}
                                          {:otsikko "Yhteensä (€)"})
      (apurit/tarkista-taulukko-kaikki-rivit taulukko
                                             (fn [[kuukausi kok-hint yks-hint er-kust bonus muutos vahinkojen-korjaukset akilliset-hoitotyot sanktiot
                                                   yhteensa :as rivi]]
                                               (and (= (count rivi) 10)
                                                    (string? kuukausi)
                                                    (number? kok-hint)
                                                    (number? yks-hint)
                                                    (number? er-kust)
                                                    (number? bonus)
                                                    (number? muutos)
                                                    (number? vahinkojen-korjaukset)
                                                    (number? akilliset-hoitotyot)
                                                    (number? sanktiot)
                                                    (number? yhteensa)))))

    ;; Soratien hoito
    (let [otsikko "Soratien hoito"
          taulukko (apurit/taulukko-otsikolla vastaus otsikko)]
      (apurit/tarkista-taulukko-sarakkeet taulukko
                                          {:otsikko "Kuukausi"}
                                          {:otsikko "Kokonais\u00ADhintaiset työt"}
                                          {:otsikko "Yksikkö\u00ADhintaiset työt"}
                                          {:otsikko "Erillis\u00ADkustannukset"}
                                          {:otsikko "Bonus"}
                                          {:otsikko "Muutos- ja lisä\u00ADtyöt"}
                                          {:otsikko "Vahinkojen korjaukset"}
                                          {:otsikko "Äkillinen hoitotyö"}
                                          {:otsikko "Sanktiot"}
                                          {:otsikko "Yhteensä (€)"})
      (apurit/tarkista-taulukko-kaikki-rivit taulukko
                                             (fn [[kuukausi kok-hint yks-hint er-kust bonus muutos vahinkojen-korjaukset akilliset-hoitotyot sanktiot
                                                   yhteensa :as rivi]]
                                               (and (= (count rivi) 10)
                                                    (string? kuukausi)
                                                    (number? kok-hint)
                                                    (number? yks-hint)
                                                    (number? er-kust)
                                                    (number? bonus)
                                                    (number? muutos)
                                                    (number? vahinkojen-korjaukset)
                                                    (number? akilliset-hoitotyot)
                                                    (number? sanktiot)
                                                    (number? yhteensa)))))))