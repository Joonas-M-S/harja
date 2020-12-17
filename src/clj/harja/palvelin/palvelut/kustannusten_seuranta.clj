(ns harja.palvelin.palvelut.kustannusten-seuranta
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [taoensso.timbre :as log]
            [slingshot.slingshot :refer [throw+ try+]]
            [harja.domain.skeema :refer [Toteuma validoi]]
            [harja.domain.kulut.kustannusten-seuranta :as kustannusten-seuranta]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.roolit :as roolit]
            [harja.fmt :as fmt]
            [harja.pvm :as pvm]
            [harja.tyokalut.big :as big]
            [harja.kyselyt.kustannusten-seuranta :as kustannusten-seuranta-q]
            [harja.palvelin.raportointi.excel :as excel]
            [harja.palvelin.komponentit.excel-vienti :as excel-vienti]))


(defn- hae-urakan-kustannusten-seuranta-paaryhmittain [db user {:keys [urakka-id hoitokauden-alkuvuosi alkupvm loppupvm] :as tiedot}]
  (if (oikeudet/voi-lukea? oikeudet/urakat-toteumat-kokonaishintaisettyot urakka-id user)
    (let [_ (println "hae-urakan-kustannusten-seuranta-paaryhmittain :: tiedot " (pr-str tiedot))
          res (kustannusten-seuranta-q/listaa-kustannukset-paaryhmittain db {:urakka urakka-id
                                                                             :alkupvm alkupvm
                                                                             :loppupvm loppupvm
                                                                             :hoitokauden-alkuvuosi (int hoitokauden-alkuvuosi)})]
      res)
    (throw+ (roolit/->EiOikeutta "Ei oikeutta"))))

(defn- rivita-toimenpiteet [toimenpiteet paaryhma]
  (mapcat (fn [toimenpide]
            (let [toimenpide-tot (:toimenpide-toteutunut-summa toimenpide)
                  toimenpide-bud (:toimenpide-budjetoitu-summa toimenpide)
                  erotus (when (not= 0 toimenpide-bud) (- toimenpide-bud toimenpide-tot))]
              (concat [{:paaryhma paaryhma
                        :toimenpide (:toimenpide toimenpide)
                        :tehtava_nimi nil
                        :toteutunut_summa toimenpide-tot
                        :budjetoitu_summa toimenpide-bud
                        :erotus erotus
                        :prosentti (if (or (= 0M toimenpide-tot) (= 0M toimenpide-bud))
                                     0
                                     (* 100 (* 100 (.divide toimenpide-tot toimenpide-bud 2))))}]
                      (mapcat
                        (fn [rivi]
                          (let [toteutunut-summa (or (:toteutunut_summa rivi) 0)]
                            [{:paaryhma paaryhma
                              :toimenpide (:toimenpide rivi)
                              :tehtava_nimi (:tehtava_nimi rivi)
                              :toteutunut_summa toteutunut-summa
                              :budjetoitu_summa nil
                              :erotus nil
                              :prosentti nil}]))
                        (:tehtavat toimenpide)))))
          toimenpiteet))

(defn- rivita-lisatyot [lisatyot yhteensa]
  (concat [{:paaryhma "Lisätyöt"
            :toimenpide nil
            :tehtava_nimi nil
            :toteutunut_summa yhteensa
            :budjetoitu_summa nil
            :erotus nil
            :prosentti nil}]
          (mapcat
            (fn [l]
              [{:paaryhma "Lisätyöt"
                :toimenpide (:toimenpide l)
                :tehtava_nimi (or (:tehtava_nimi l) (:toimenpidekoodi_nimi l))
                :toteutunut_summa (or (:toteutunut_summa l) 0)
                :budjetoitu_summa nil
                :erotus nil
                :prosentti nil}])
            lisatyot)))

(defn- luo-excel-rivi-toimenpiteelle [rivi ensimmainen?]
  (if ensimmainen?
    {:rivi [(:paaryhma rivi)
            (:toimenpide rivi)
            (:tehtava_nimi rivi)
            (:budjetoitu_summa rivi)
            (:toteutunut_summa rivi)
            (:erotus rivi)
            (:prosentti rivi)]
     :lihavoi? true}
    [nil
     (:toimenpide rivi)
     (:tehtava_nimi rivi)
     (:budjetoitu_summa rivi)
     (:toteutunut_summa rivi)
     (:erotus rivi)
     (:prosentti rivi)]))

(defn- luo-excel-rivi-hoidonjohdolle [kustannusdata]
  (let [bud (get-in kustannusdata [:taulukon-rivit :hoidonjohdonpalkkio-budjetoitu])
        tot (get-in kustannusdata [:taulukon-rivit :hoidonjohdonpalkkio-toteutunut])
        erotus (- bud tot)
        prosentti (if (or (= 0M tot) (= 0M bud))
                    0
                    (* 100 (* 100 (.divide tot bud 2))))]
    [{:rivi ["Hoidonjohdonpalkkio" nil nil bud tot erotus prosentti] :lihavoi? true}]))

(defn- luo-excel-rivi-erillishankinnoille [kustannusdata]
  (let [bud (get-in kustannusdata [:taulukon-rivit :erillishankinnat-budjetoitu])
        tot (get-in kustannusdata [:taulukon-rivit :erillishankinnat-toteutunut])
        erotus (- bud tot)
        prosentti (if (or (= 0M tot) (= 0M bud))
                    0
                    (* 100 (* 100 (.divide tot bud 2))))]
    [{:rivi ["Erillishankinnat" nil nil bud tot erotus prosentti] :lihavoi? true}]))

(defn- luo-excel-rivi-yhteensa [kustannusdata]
  (let [bud (get-in kustannusdata [:yhteensa :yht-budjetoitu-summa])
        tot (get-in kustannusdata [:yhteensa :yht-toteutunut-summa])
        erotus (when (not= 0 bud) (- bud tot))
        prosentti (if (or (= 0M tot) (= 0M bud))
                    0
                    (* 100 (* 100 (.divide tot bud 2))))]
    [{:rivi ["Yhteensä" nil nil bud tot erotus prosentti] :lihavoi? true}]))

(defn- luo-excel-rivi-lisatyot [rivi ensimmainen?]
  (if ensimmainen?
    {:rivi ["Lisätyöt" (:toimenpide rivi) (:tehtava_nimi rivi) nil (:toteutunut_summa rivi) nil nil] :lihavoi? true}
    [nil (:toimenpide rivi) (:tehtava_nimi rivi) nil (:toteutunut_summa rivi) nil nil]))

(defn- kustannukset-excel
  [db workbook user {:keys [urakka-id urakka-nimi hoitokauden-alkuvuosi alkupvm loppupvm] :as tiedot}]
  (oikeudet/voi-lukea? oikeudet/urakat-toteumat-kokonaishintaisettyot urakka-id user)
  (let [_ (println "kustannukset-excel :: tiedot " (pr-str tiedot))
        kustannukset-tehtavittain (kustannusten-seuranta-q/listaa-kustannukset-paaryhmittain
                                    db {:urakka urakka-id
                                        :alkupvm alkupvm
                                        :loppupvm loppupvm
                                        :hoitokauden-alkuvuosi (int hoitokauden-alkuvuosi)})
        kustannusdata (kustannusten-seuranta/jarjesta-tehtavat kustannukset-tehtavittain)
        hankintakustannusten-toimenpiteet (rivita-toimenpiteet
                                            (get-in kustannusdata [:taulukon-rivit :hankintakustannukset])
                                            "Hankintakustannukset")
        hallintakorvausten-toimenpiteet (rivita-toimenpiteet
                                          (get-in kustannusdata [:taulukon-rivit :johto-ja-hallintakorvaus])
                                          "Johto- ja Hallintokorvaukset")
        lisatyot (rivita-lisatyot (get-in kustannusdata [:taulukon-rivit :lisatyot]) (get-in kustannusdata [:taulukon-rivit :lisatyot-summa]))
        sarakkeet [{:otsikko "Ryhmä"} {:otsikko "Toimenpide"} {:otsikko "Tehtavä"}
                   {:otsikko "Budjetti €" :fmt :raha} {:otsikko "Toteuma €" :fmt :raha}
                   {:otsikko "Erotus €" :fmt :raha} {:otsikko "%" :fmt :prosentti}]
        optiot {:nimi urakka-nimi
                :tyhja (if (empty? kustannukset-tehtavittain) "Ei kustannuksia valitulla aikavälillä.")}
        ;; Raporttiin laitetaan otsikot aina pääryhmän yläpuolelle ja tästä syystä tämä :taulukko lisätään raporttiin monta kertaa
        taulukot [[:taulukko optiot sarakkeet
                   (concat
                     (mapv #(luo-excel-rivi-toimenpiteelle % (if (= % (first hankintakustannusten-toimenpiteet))
                                                               true
                                                               false)) hankintakustannusten-toimenpiteet)
                     (mapv #(luo-excel-rivi-toimenpiteelle % (if (= % (first hallintakorvausten-toimenpiteet))
                                                               true
                                                               false)) hallintakorvausten-toimenpiteet)
                     (luo-excel-rivi-hoidonjohdolle kustannusdata)
                     (luo-excel-rivi-erillishankinnoille kustannusdata)
                     (luo-excel-rivi-yhteensa kustannusdata)
                     (mapv (fn [rivi]
                             (luo-excel-rivi-lisatyot rivi (if (= rivi (first lisatyot))
                                                             true
                                                             false)))
                           lisatyot))]]
        taulukko (concat
                   [:raportti {:nimi (str urakka-nimi "_" alkupvm "-" loppupvm)
                               :orientaatio :landscape}]
                   (if (empty? taulukot)
                     [[:taulukko optiot nil [["Ei kustannuksia valitulla aikavälillä"]]]]
                     taulukot))]
    (excel/muodosta-excel (vec taulukko)
                          workbook)))

(defrecord KustannustenSeuranta []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)
          db-replica (:db-replica this)
          excel (:excel-vienti this)]
      (assert (some? db-replica))

      (julkaise-palvelu
        http
        :urakan-kustannusten-seuranta-paaryhmittain
        (fn [user tiedot]
          (hae-urakan-kustannusten-seuranta-paaryhmittain db-replica user tiedot))
        )
      (when excel
        (excel-vienti/rekisteroi-excel-kasittelija! excel :kustannukset (partial #'kustannukset-excel db)))


      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-kustannusten-seuranta-paaryhmittain)
    (when (:excel-vienti this)
      (excel-vienti/poista-excel-kasittelija! (:excel-vienti this) :kustannukset))
    this))
