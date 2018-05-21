(ns harja.palvelin.raportointi.raportit.kanavien-muutos-ja-lisatyot
  (:require [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.toimenpideinstanssit :refer [hae-urakan-toimenpideinstanssi]]
            [harja.fmt :as fmt]
            [harja.pvm :as pvm]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.palvelin.raportointi.raportit.yleinen :as yleinen
             :refer [raportin-otsikko vuosi-ja-kk vuosi-ja-kk-fmt kuukaudet
                     pylvaat-kuukausittain ei-osumia-aikavalilla-teksti rivi]]

            [harja.domain.raportointi :refer [info-solu]]
            [taoensso.timbre :as log]
            [jeesql.core :refer [defqueries]]
            [clojure.string :as str]
            [harja.kyselyt.konversio :as konv]))

(defqueries "harja/palvelin/raportointi/raportit/kanavien_muutos_ja_lisatyot.sql")

(def hallintayksikko {:lyhenne "KAN"
                      :nimi "Kanavat ja avattavat sillat"})

(defn- urakat
  "Palauttaa urakka id:t. Hakee id:t ryhmiteltyjen työrivien avaimesta."
  [tyot-ryhmiteltyna]
  (distinct (map #(get % :urakka) (keys tyot-ryhmiteltyna))))

(defn- urakan-nimi
  [db urakka-id]
  (hae-urakan-nimi db [:urakkaid urakka-id]))

(defn- urakan-tyot
  "Palauttaa urakkaan liittyvät työrivit."
  [urakka-id tyot-ryhmiteltyna]
  (filter #(= urakka-id (:urakka (key %))) tyot-ryhmiteltyna))

(defn- hinnoittelu-ryhmat
  "Palauttaa hinnoitteluryhmien tunnukset. Hakee ne ryhmiteltyjen työrivien avaimesta."
  [tyot-ryhmiteltyna]
  (distinct (map #(get % :hinnoittelu_ryhma) (keys tyot-ryhmiteltyna))))

(defn hinnoitteluryhman-nimi
  [hinnoitteluryhma]
  (log/debug (println-str "Hinnoitteluryhmä " hinnoitteluryhma))
  (case (keyword hinnoitteluryhma)
    :sopimushintainen-tyo-tai-materiaali "Sopimushintainen työ tai materiaali"
    :omakustanteinen-tyo-tai-materiaali "Omakustanteinen työ tai materiaali"
    :varaosat-ja-materiaalit "Varaosat ja materiaalit"
    :muu-tyo "Muut työt (ei indeksilaskentaa)"
    :muut-kulut "Muut"
    "Hinnoitteluryhmän nimi puuttuu."))

(defn- hinnoitteluryhman-tyot
  "Palauttaa hintaryhmään (= hinnoitteluryhmään) liittyvät työrivit.
  Rajaa urakka ennen funktion kutsumista, jos haluat vain yksittäisen urakan hinnoitteluryhmäerittelyn."
  [hinnoitteluryhma tyot-ryhmiteltyna]
  (filter #(= hinnoitteluryhma (:hinnoittelu_ryhma (key %))) tyot-ryhmiteltyna))

(defn- hinnoitteryhman-tyot-urakassa
  "Palauttaa urakkaan ja hintaryhmään liittyvät työrivit."
  [urakka-id hinnoitteluryhma tyot-ryhmiteltyna]
  (filter #(and
             (= hinnoitteluryhma (:hinnoittelu_ryhma (key %)))
             (= urakka-id (:urakka (key %)))) tyot-ryhmiteltyna))

(defn- summien-summa
  "Palauttaa valmiiksi suodatettujen rivien yhteissumman rivien :summa-tiedoista."
  [suodatetut-tyot]
  (reduce + (merge-with + (map #(get % :summa) (first (vals suodatetut-tyot))))))

(defn- muodosta-rivi-hinnoitteluryhmalle
  [hinnoitteluryhman-tyot-urakassa]
  (hash-map
    :hinnoittelu (hinnoitteluryhman-nimi
                   (:hinnoittelu_ryhma (first hinnoitteluryhman-tyot-urakassa)))
    :summa (summien-summa hinnoitteluryhman-tyot-urakassa)
    :indeksi nil))

(defn- muodosta-erittely-urakan-hinnoitteluryhmille
  "Koko maa, urakkakohtainen erittely. Data taulukkoa varten."
  [urakka-id tyot-ryhmiteltyna]
  (let [urakan-tyot (urakan-tyot urakka-id tyot-ryhmiteltyna)]
    (map (fn [ryhma] (muodosta-rivi-hinnoitteluryhmalle
                       (hinnoitteluryhman-tyot ryhma urakan-tyot)))
         (hinnoittelu-ryhmat urakan-tyot))))

(defn kokeilu
  [tyot-ryhmiteltyna]
  (map #(println (keyword %)) hinnoitteluryhman-nimi))


(defn- sarakkeet [tyyppi]
  (case tyyppi
    :muutos-ja-lisatyot
    [{:leveys 5 :otsikko "Pvm"}
     {:leveys 8 :otsikko "Tehtava"}
     {:leveys 8 :otsikko "Kohde"}
     {:leveys 8 :otsikko "Kohteen osa"}
     {:leveys 8 :otsikko "Huoltokohde"}
     {:leveys 15 :otsikko "Lisätieto"}
     {:leveys 8 :otsikko "Hinnoittelu"}
     {:leveys 5 :otsikko "Summa" :fmt :raha}
     {:leveys 5 :otsikko "Indeksi" :fmt :raha}]
    :muutos-ja-lisatyot-koko-maa
    [{:leveys 5 :otsikko "Hinnoittelu"}
     {:leveys 5 :otsikko "Summa" :fmt :raha}
     {:leveys 5 :otsikko "Indeksi" :fmt :raha}]
    :yhteenveto))

(def ei-indeksilaskentaa-solu (info-solu "Ei indeksilaskentaa"))
(def indeksi-puuttuu-info-solu (info-solu "Indeksi puuttuu"))
(def ei-raha-summaa-info-solu (info-solu "Ei rahasummaa"))



(defn- summa [arvot]
  (reduce + (keep identity arvot)))

(defn- rivien-summa [rivit yhteenlaskettava-tieto]
  (summa (map yhteenlaskettava-tieto rivit)))


(defn- muodosta-summarivi [konteksti yhteenlaskettavat-tyot]

  (let [summien-summa (reduce + 0 (keep :summa yhteenlaskettavat-tyot))
        indeksien-summa (reduce + 0 (keep :indeksi yhteenlaskettavat-tyot))]

    (case (keyword konteksti)
      :muutos-ja-lisatyot-koko-maa
      ["Yhteensä" summien-summa indeksien-summa]
      :muutos-ja-lisatyot
      ["" "" "" "" "" "" "Yhteensä" summien-summa indeksien-summa])
    ))


(defn- summa-rivit-yhteensa [konteksti muutos-ja-lisatyot]

  (case konteksti
    :muutos-ja-lisatyot-koko-maa
    (let [yhteensa-rivi ["Yhteensä"
                         ""
                         (reduce + 0 (keep :summa muutos-ja-lisatyot))
                         (reduce + 0 (keep :indeksi muutos-ja-lisatyot))]]
      yhteensa-rivi)
    :muutos-ja-lisatyot
    (let [yhteensa-rivi ["Yhteensä"
                         "" "" "" "" "" ""
                         (reduce + 0 (keep :summa muutos-ja-lisatyot))
                         (reduce + 0 (keep :indeksi muutos-ja-lisatyot))]]
      yhteensa-rivi)
    ))

(defn- kaikki-yhteensa [muutos-ja-lisatyot]
  (log/debug (println-str "Laske summa " muutos-ja-lisatyot))
  ["" "" "" "" "" "" "" (reduce + 0 (keep :summa muutos-ja-lisatyot))])


(defn muodosta-rivikokonaisuus
  [konteksti otsikko tyot]

  (log/debug (pr-str "TYÖTTYÖTTYÖT" tyot))
  (log/debug (pr-str "KONTEKSTIKONTEKSTI" konteksti))
  (case (keyword konteksti)

    :muutos-ja-lisatyot-koko-maa
    ;;  (conj
    [:taulukko {:otsikko otsikko
                :sheet-nimi otsikko
                :viimeinen-rivi-yhteenveto? false}
     (sarakkeet :muutos-ja-lisatyot-koko-maa)
     (mapv (fn [urakan-tyot-ryhmiteltyna] (conj
                                            (rivi
                                              (hinnoitteluryhman-nimi (:hinnoittelu_ryhma
                                                                        urakan-tyot-ryhmiteltyna))
                                              (or (:summa urakan-tyot-ryhmiteltyna) ei-raha-summaa-info-solu)
                                              (or (:indeksi urakan-tyot-ryhmiteltyna) indeksi-puuttuu-info-solu))
                                            (muodosta-summarivi konteksti urakan-tyot-ryhmiteltyna)))
           (muodosta-erittely-urakan-hinnoitteluryhmille (:urakka (key (first tyot))) tyot))
     (muodosta-summarivi :muutos-ja-lisatyot-koko-maa tyot)]
    ;;)

    :muutos-ja-lisatyot

    (conj
      [:taulukko {:otsikko otsikko
                  :sheet-nimi otsikko
                  :viimeinen-rivi-yhteenveto? false}
       (sarakkeet :muutos-ja-lisatyot)
       (mapv (fn [urakan-tyot-ryhmiteltyna] (conj (rivi
                                                    (or (pvm/pvm (:pvm urakan-tyot-ryhmiteltyna)) " ")
                                                    (:tehtava urakan-tyot-ryhmiteltyna)
                                                    (:kohde urakan-tyot-ryhmiteltyna)
                                                    (or (:kohteenosa urakan-tyot-ryhmiteltyna) " ")
                                                    (:huoltokohde urakan-tyot-ryhmiteltyna)
                                                    (str (or (:otsikko urakan-tyot-ryhmiteltyna) " ") " " (or (:lisatieto urakan-tyot-ryhmiteltyna) " "))
                                                    (hinnoitteluryhman-nimi (:hinnoittelu_ryhma urakan-tyot-ryhmiteltyna))
                                                    (or (:summa urakan-tyot-ryhmiteltyna) ei-raha-summaa-info-solu)
                                                    (or (:indeksi urakan-tyot-ryhmiteltyna) indeksi-puuttuu-info-solu)) ;; TODO
                                                  )) (sort-by :tehtava tyot))
       (muodosta-summarivi :muutos-ja-lisatyot-koko-maa tyot)
       ])))





(defn- taulukko-urakka [otsikko tyot]
  (conj
    (muodosta-rivikokonaisuus :muutos-ja-lisatyot otsikko tyot)
    (muodosta-summarivi :muutos-ja-lisatyot tyot)))

(defn- taulukko-koko-maa [otsikko tyot-ryhmiteltyna]
  (conj
    (first (map (fn [urakka-id]
                  (muodosta-rivikokonaisuus :muutos-ja-lisatyot-koko-maa
                                            otsikko
                                            (urakan-tyot urakka-id tyot-ryhmiteltyna)))
                (urakat tyot-ryhmiteltyna)))))

(defn rajaus
  [urakka-id kohde-id tehtava-id]
  (if (and urakka-id tehtava-id kohde-id)
    :urakka-kohde-ja-tehtava
    (if (and urakka-id tehtava-id)
      :urakka-ja-tehtava
      (if (and urakka-id kohde-id)
        :urakka-ja-kohde
        (if urakka-id
          :urakka
          :ei-rajausta)))))

(defn hae-kanavien-muutos-ja-lisatyot-raportille
  [db {:keys [alkupvm loppupvm urakka-id kohde-id tehtava-id] :as parametrit} rajaus]
  (let [hakuparametrit {:urakkaid urakka-id
                        :alkupvm (konv/sql-date alkupvm)
                        :loppupvm (konv/sql-date loppupvm)
                        :kohdeid kohde-id
                        :tehtavaid tehtava-id}]

    (log/debug (println-str "Haetaan rajauksella " rajaus))
    (log/debug (println-str "Hakuparametrit " hakuparametrit))
    (case rajaus
      :urakka-kohde-ja-tehtava
      (hae-kanavien-kohde-ja-tehtavakohtaiset-muutos-ja-lisatyot-raportille db hakuparametrit)
      :urakka-ja-tehtava
      (hae-kanavien-tehtavakohtaiset-muutos-ja-lisatyot db hakuparametrit)
      :urakka-ja-kohde
      (hae-kanavien-kohdekohtaiset-muutos-ja-lisatyot db hakuparametrit)
      :urakka
      (hae-kanavien-urakkakohtaiset-muutos-ja-lisatyot db hakuparametrit)
      :ei-rajausta
      (hae-kanavien-muutos-ja-lisatyot db hakuparametrit)
      )))

(defn suorita
  [db user {:keys [alkupvm loppupvm urakka-id kohde-id tehtava-id] :as parametrit}]
  (let [konteksti (cond urakka-id :urakka
                        :default :koko-maa)
        rajaus (rajaus urakka-id kohde-id tehtava-id)
        raportin-kontekstin-nimi (case konteksti
                                   :urakka (:nimi (first (urakat-q/hae-urakka db urakka-id)))
                                   :koko-maa "KOKO MAA")
        raportin-nimi "Muutos- ja lisätöiden raportti"
        raportin-otsikko (raportin-otsikko raportin-kontekstin-nimi raportin-nimi alkupvm loppupvm)

        ;; TODO: tarkempien raporttien rajaavat tiedot myös otsikkoon: kohde, tehtava
        kohde-nimi (if kohde-id
                     (:nimi (first (hae-kanavakohteen-nimi db {:kohdeid kohde-id})))
                     "Kaikki kohteet")
        tehtava-nimi (if tehtava-id
                       (:nimi (first (hae-kanavatoimenpiteen-nimi db {:tehtavaid tehtava-id}))
                         "Kaikki toimenpiteet"))
        muutos-ja-lisatyot (hae-kanavien-muutos-ja-lisatyot-raportille db parametrit rajaus)
        raportin-alaotsikko (str/join ", " (remove str/blank? [raportin-kontekstin-nimi kohde-nimi tehtava-nimi]))]



    [:raportti {:orientaatio :landscape
                :nimi raportin-otsikko}
     (conj
       (when (not-empty muutos-ja-lisatyot)
         (if (= (keyword konteksti) :urakka)
           (taulukko-urakka raportin-alaotsikko muutos-ja-lisatyot)
           (taulukko-koko-maa raportin-alaotsikko
                              (group-by #(select-keys % [:urakka :hinnoittelu_ryhma]) muutos-ja-lisatyot))))
       [:taulukko {:otsikko "Kaikki yhteensä"
                   :tyhja (when (empty? muutos-ja-lisatyot) "Ei raportoitavaa.")
                   :sheet-nimi "Yhteensä"
                   :viimeinen-rivi-yhteenveto? true}
        (kaikki-yhteensa muutos-ja-lisatyot)])]))


