(ns harja.palvelin.raportointi.raportit.yksikkohintaiset-tyot
  (:require [harja.palvelin.palvelut.toteumat :as toteumat]
            [harja.kyselyt.konversio :as konv]
            [harja.domain.roolit :as roolit]
            [harja.kyselyt.yksikkohintaiset-tyot :refer [hae-yksikkohintaiset-tyot-per-paiva]]
            [harja.kyselyt.toimenpideinstanssit :refer [hae-urakan-toimenpideinstanssi]]
            [harja.fmt :as fmt]
            [harja.pvm :as pvm]
            [taoensso.timbre :as log]
            ))

;; oulu au 2014 - 2019:
;; 1.10.2014-30.9.2015 elokuu 2015 kaikki
;;
;; Päivämäärä	Tehtävä	Yksikkö	Yksikköhinta	Suunniteltu määrä hoitokaudella	Toteutunut määrä	Suunnitellut kustannukset hoitokaudella	Toteutuneet kustannukset
;; 01.08.2015	Vesakonraivaus	ha	100,00 €	240	10	24 000,00 €	1 000,00 €
;; 19.08.2015	Vesakonraivaus	ha	100,00 €	240	10	24 000,00 €	1 000,00 €
;; 20.08.2015	Vesakonraivaus	ha	100,00 €	240	10	24 000,00 €	1 000,00 €
;; Yhteensä					72 000,00 €	3 000,00 €

(defn suorita [db user {:keys [urakka-id aikavali-alkupvm aikavali-loppupvm toimenpide-id] :as parametrit}]
  (let [naytettavat-rivit (hae-yksikkohintaiset-tyot-per-paiva db
                                                               urakka-id aikavali-alkupvm aikavali-loppupvm
                                                               (if toimenpide-id true false) toimenpide-id)
        otsikko (str "Yksikköhintaisten töiden raportti"
                     (some->> toimenpide-id
                              (hae-urakan-toimenpideinstanssi db urakka-id)
                              first :nimi
                              (str ", "))
                     ", " (pvm/pvm aikavali-alkupvm) " - " (pvm/pvm aikavali-loppupvm))]
    [:raportti {:orientaatio :landscape
                :nimi otsikko}
     [:taulukko {:otsikko otsikko
                 :viimeinen-rivi-yhteenveto? true
                 :tyhja   (if (empty? naytettavat-rivit) "Ei raportoitavia tehtäviä.")}
      [{:leveys "10%" :otsikko "Päivämäärä"}
       {:leveys "25%" :otsikko "Tehtävä"}
       {:leveys "5%" :otsikko "Yks."}
       {:leveys "10%" :otsikko "Yksikkö\u00adhinta"}
       {:leveys "10%" :otsikko "Suunniteltu määrä hoitokaudella"}
       {:leveys "10%" :otsikko "Toteutunut määrä"}
       {:leveys "15%" :otsikko "Suunnitellut kustannukset hoitokaudella"}
       {:leveys "15%" :otsikko "Toteutuneet kustannukset"}]

      (conj (mapv (juxt (comp pvm/pvm :pvm)
                        :nimi
                        :yksikko
                        (comp fmt/euro-opt :yksikkohinta)
                        :suunniteltu_maara
                        :toteutunut_maara
                        (comp fmt/euro-opt :suunnitellut_kustannukset)
                        (comp fmt/euro-opt :toteutuneet_kustannukset))
                  naytettavat-rivit)
            [nil "Yhteensä" nil nil nil nil
             (fmt/euro-opt (reduce + (keep :suunnitellut_kustannukset naytettavat-rivit)))
             (fmt/euro-opt (reduce + (keep :toteutuneet_kustannukset naytettavat-rivit)))])]]))

