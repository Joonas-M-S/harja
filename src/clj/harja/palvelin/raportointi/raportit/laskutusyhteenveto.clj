(ns harja.palvelin.raportointi.raportit.laskutusyhteenveto
  "Laskutusyhteenveto"
  (:require [harja.kyselyt.laskutusyhteenveto :as laskutus-q]
            [harja.kyselyt.hallintayksikot :as hallintayksikko-q]
            [harja.kyselyt.urakat :as urakat-q]

            [taoensso.timbre :as log]
            [harja.palvelin.raportointi.raportit.yleinen :as yleinen :refer [rivi]]
            [harja.palvelin.palvelut.indeksit :as indeksipalvelu]
            [harja.tyokalut.functor :refer [fmap]]
            [harja.kyselyt.konversio :as konv]
            [harja.pvm :as pvm]
            [clj-time.local :as l]
            [harja.fmt :as fmt]
            [harja.pvm :as pvm]
            [clojure.string :as str]
            [harja.domain.toimenpidekoodit :as toimenpidekoodit]))


(defn hae-laskutusyhteenvedon-tiedot
  [db user {:keys [urakka-id alkupvm loppupvm] :as tiedot}]
  (let [[hk-alkupvm hk-loppupvm] (if (or (pvm/kyseessa-kk-vali? alkupvm loppupvm)
                                         (pvm/kyseessa-hoitokausi-vali? alkupvm loppupvm))
                                   ;; jos kyseessä vapaa aikaväli, lasketaan vain yksi sarake joten
                                   ;; hk-pvm:illä ei ole merkitystä, kunhan eivät konfliktoi alkupvm ja loppupvm kanssa
                                   (pvm/paivamaaran-hoitokausi alkupvm)
                                   [alkupvm loppupvm])]
    (log/debug "hae-urakan-laskutusyhteenvedon-tiedot" tiedot)

    (let [tulos (vec
                  (sort-by (juxt (comp toimenpidekoodit/tuotteen-jarjestys :tuotekoodi) :nimi)
                           (into []
                                 (laskutus-q/hae-laskutusyhteenvedon-tiedot db
                                                                            (konv/sql-date hk-alkupvm)
                                                                            (konv/sql-date hk-loppupvm)
                                                                            (konv/sql-date alkupvm)
                                                                            (konv/sql-date loppupvm)
                                                                            urakka-id))))]
      #_(log/debug (pr-str tulos))
      tulos)))

(defn laske-asiakastyytyvaisyysbonus
  [db {:keys [urakka-id maksupvm indeksinimi summa] :as tiedot}]
  (assert (and maksupvm summa) "Annettava maksupvm ja summa jotta voidaan laskea asiakastyytyväisyysbonuksen arvo.")
  (first
    (into []
          (laskutus-q/laske-asiakastyytyvaisyysbonus db
                                                     urakka-id
                                                     (konv/sql-date maksupvm)
                                                     indeksinimi
                                                     summa))))

(defn- kuukausi [date]
  (.format (java.text.SimpleDateFormat. "MMMM") date))


(defn- korotettuna-jos-indeksi-saatavilla
  [rivi avain]
  (let [avain-ind-korotus (keyword (str avain "_ind_korotus"))
        ind-korotus (avain-ind-korotus rivi)
        ilman-korotusta  ((keyword avain) rivi)]
    (if-not ind-korotus  ;indeksipuutteen aiheuttama nil
      {:tulos            ilman-korotusta
       :indeksi-puuttui? true}
      {:tulos (+ ilman-korotusta ind-korotus)
       :indeksi-puuttui? false})))


(defn taulukko-elementti
  [otsikko taulukon-tiedot kyseessa-kk-vali?
   laskutettu-teksti laskutetaan-teksti
   yhteenveto yhteenveto-teksti summa-fmt]
  [:taulukko {:oikealle-tasattavat-kentat #{1 2 3}
              :viimeinen-rivi-yhteenveto? true}
   (rivi
     {:otsikko otsikko :leveys 36}
     (when kyseessa-kk-vali? {:otsikko laskutettu-teksti :leveys 29 :tyyppi :varillinen-teksti})
     (when kyseessa-kk-vali? {:otsikko laskutetaan-teksti :leveys 24 :tyyppi :varillinen-teksti})
     {:otsikko yhteenveto-teksti :leveys 29 :tyyppi :varillinen-teksti})

   (into []
         (concat
           (map (fn [[nimi {laskutettu :tulos laskutettu-ind-puuttui? :indeksi-puuttui?}
                      {laskutetaan :tulos laskutetaan-ind-puuttui? :indeksi-puuttui?}]]
                  (rivi
                    nimi
                    (when kyseessa-kk-vali? [:varillinen-teksti {:arvo (summa-fmt laskutettu)
                                                                 :tyyli (when laskutettu-ind-puuttui? :virhe)}])
                    (when kyseessa-kk-vali? [:varillinen-teksti {:arvo (summa-fmt laskutetaan)
                                                                 :tyyli (when laskutetaan-ind-puuttui? :virhe)}])
                    (if (and laskutettu laskutetaan)
                      [:varillinen-teksti {:arvo (summa-fmt (+ laskutettu laskutetaan))
                                           :tyyli (when (or laskutettu-ind-puuttui? laskutetaan-ind-puuttui?)
                                                    :virhe)}]
                      [:varillinen-teksti {:arvo (summa-fmt nil)
                                           :tyyli :virhe}]))) taulukon-tiedot)
           [yhteenveto]))])

(defn- summaa-korotetut
  [rivit]
  (reduce
    (fn [{summa :tulos
          ind-puuttui? :indeksi-puuttui?}
         {tulos :tulos indeksi-puuttui? :indeksi-puuttui?}]
      {:tulos (+ summa (or tulos 0.0)) :indeksi-puuttui? (or ind-puuttui? indeksi-puuttui?)})
    {:tulos 0 :indeksi-puuttui? false}
    rivit))

(defn- indeksi-puuttuu-jos-nil
  [tulos]
  {:tulos tulos
   :indeksi-puuttui? (nil? tulos)})

(defn- summataulukko
  [otsikko avaimet
   laskutettu-teksti laskutetaan-teksti
   yhteenveto-teksti kyseessa-kk-vali?
   tiedot summa-fmt lisaa-kht-ind-korotus?]
  (let [taulukon-rivit (for [rivi tiedot
                             :let [nimi (:nimi rivi)
                                   laskutetut-arvot (map #(korotettuna-jos-indeksi-saatavilla rivi (str % "_laskutettu"))
                                                         avaimet)

                                   laskutettu (summaa-korotetut (if lisaa-kht-ind-korotus?
                                                                  (conj laskutetut-arvot (indeksi-puuttuu-jos-nil (:kht_laskutettu_ind_korotus rivi)))
                                                                  laskutetut-arvot))
                                   laskutetaan-arvot (map #(korotettuna-jos-indeksi-saatavilla rivi (str % "_laskutetaan"))
                                                          avaimet)
                                   laskutetaan (summaa-korotetut (if lisaa-kht-ind-korotus?
                                                                   (conj laskutetaan-arvot (indeksi-puuttuu-jos-nil (:kht_laskutetaan_ind_korotus rivi)))
                                                                   laskutetaan-arvot))
                                   summa (summaa-korotetut [laskutettu laskutetaan])]]
                         [nimi laskutettu laskutetaan summa])
        laskutettu-yht (summaa-korotetut (map second taulukon-rivit))
        laskutetaan-yht (summaa-korotetut (map #(nth % 2) taulukon-rivit))
        yhteensa (summaa-korotetut [laskutettu-yht laskutetaan-yht])
        yhteenveto (rivi "Toimenpiteet yhteensä"
                        (when kyseessa-kk-vali? [:varillinen-teksti {:arvo (summa-fmt (:tulos laskutettu-yht))
                                                                     :tyyli (when (:indeksi-puuttui? laskutettu-yht) :virhe)}])
                        (when kyseessa-kk-vali? [:varillinen-teksti {:arvo (summa-fmt (:tulos laskutetaan-yht))
                                                                     :tyyli (when (:indeksi-puuttui? laskutetaan-yht) :virhe)}])

                         [:varillinen-teksti {:arvo (summa-fmt (:tulos yhteensa))
                                              :tyyli (when (:indeksi-puuttui? yhteensa) :virhe)}])]
    (when-not (empty? taulukon-rivit)
      (taulukko-elementti otsikko taulukon-rivit kyseessa-kk-vali?
                          laskutettu-teksti laskutetaan-teksti
                          yhteenveto yhteenveto-teksti summa-fmt))))

(defn- taulukko
  ([otsikko otsikko-jos-tyhja
    laskutettu-teksti laskutettu-kentta
    laskutetaan-teksti laskutetaan-kentta
    yhteenveto-teksti kyseessa-kk-vali?
    tiedot summa-fmt]
  (let [laskutettu-kentat (map laskutettu-kentta tiedot)
        laskutetaan-kentat (map laskutetaan-kentta tiedot)
        kaikkien-toimenpiteiden-summa (fn [kentat]
                                        (reduce + (keep identity kentat)))
        laskutettu-yht (kaikkien-toimenpiteiden-summa laskutettu-kentat)
        laskutetaan-yht (kaikkien-toimenpiteiden-summa laskutetaan-kentat)
        laskutettu-summasta-puuttuu-indeksi? (some nil? laskutettu-kentat)
        laskutetaan-summasta-puuttuu-indeksi? (some nil? laskutetaan-kentat)
        yhteenveto (rivi "Toimenpiteet yhteensä"
                         (when kyseessa-kk-vali? [:varillinen-teksti {:arvo (summa-fmt laskutettu-yht)
                                                                      :tyyli (when laskutettu-summasta-puuttuu-indeksi? :virhe)}])
                         (when kyseessa-kk-vali? [:varillinen-teksti {:arvo (summa-fmt laskutetaan-yht)
                                                                      :tyyli (when laskutetaan-summasta-puuttuu-indeksi? :virhe)}])
                         (if (and laskutettu-yht laskutetaan-yht)
                           [:varillinen-teksti {:arvo (summa-fmt (+ laskutettu-yht laskutetaan-yht))
                                                :tyyli (when (or laskutettu-summasta-puuttuu-indeksi?
                                                                laskutetaan-summasta-puuttuu-indeksi?) :virhe)}]
                           nil))
        taulukon-tiedot (filter (fn [[_ laskutettu laskutetaan]]
                                  (not (and (= 0.0M (:tulos laskutettu))
                                            (= 0.0M (:tulos laskutetaan)))))
                                (map (juxt :nimi
                                           (fn [rivi]
                                             {:tulos (laskutettu-kentta rivi)
                                              :indeksi-puuttui? laskutettu-summasta-puuttuu-indeksi?})
                                           (fn [rivi]
                                             {:tulos (laskutetaan-kentta rivi)
                                              :indeksi-puuttui? laskutetaan-summasta-puuttuu-indeksi?}))
                                     tiedot))]
    (when-not (empty? taulukon-tiedot)
      (taulukko-elementti otsikko taulukon-tiedot kyseessa-kk-vali?
                          laskutettu-teksti laskutetaan-teksti
                          yhteenveto yhteenveto-teksti summa-fmt)))))

(defn- aseta-sheet-nimi [[ensimmainen & muut]]
  (when ensimmainen
    (concat [(assoc-in ensimmainen [1 :sheet-nimi] "Laskutusyhteenveto")]
            muut)))

(defn- kustannuslajin-kaikki-kentat [kentan-kantanimi]
  [(keyword (str kentan-kantanimi "_laskutettu"))
   (keyword (str kentan-kantanimi "_laskutettu_ind_korotus"))
   (keyword (str kentan-kantanimi "_laskutettu_ind_korotettuna"))
   (keyword (str kentan-kantanimi "_laskutetaan"))
   (keyword (str kentan-kantanimi "_laskutetaan_ind_korotus"))
   (keyword (str kentan-kantanimi "_laskutetaan_ind_korotettuna"))])

(defn- laskettavat-kentat [rivi]
  (let [kustannusten-kentat (into []
                                  (apply concat [(kustannuslajin-kaikki-kentat "kht")
                                                 (kustannuslajin-kaikki-kentat "yht")
                                                 (kustannuslajin-kaikki-kentat "sakot")
                                                 (kustannuslajin-kaikki-kentat "muutostyot")
                                                 (kustannuslajin-kaikki-kentat "akilliset_hoitotyot")
                                                 (kustannuslajin-kaikki-kentat "vahinkojen_korjaukset")
                                                 (kustannuslajin-kaikki-kentat "bonukset")
                                                 (kustannuslajin-kaikki-kentat "erilliskustannukset")
                                                 (kustannuslajin-kaikki-kentat "kaikki_paitsi_kht")
                                                 (kustannuslajin-kaikki-kentat "kaikki")]))]
    (if (and (some? (:suolasakot_laskutettu rivi))
             (some?(:suolasakot_laskutetaan rivi)))
      (into []
            (concat kustannusten-kentat
                    (kustannuslajin-kaikki-kentat "suolasakot")))
      kustannusten-kentat)))

(defn suorita [db user {:keys [alkupvm loppupvm urakka-id hallintayksikko-id] :as parametrit}]
  (log/debug "LASKUTUSYHTEENVETO PARAMETRIT: " (pr-str parametrit))
  (let [konteksti (cond urakka-id :urakka
                        hallintayksikko-id :hallintayksikko
                        :default :urakka)
        ;; Konteksti ja urakkatiedot
        {alueen-nimi :nimi indeksi :indeksi} (first
                                               (if (= konteksti :hallintayksikko)
                                                 (hallintayksikko-q/hae-organisaatio db hallintayksikko-id)
                                                 (urakat-q/hae-urakka db urakka-id)))
        urakat (urakat-q/hae-urakkatiedot-laskutusyhteenvetoon
                 db {:alkupvm alkupvm :loppupvm loppupvm
                     :hallintayksikkoid hallintayksikko-id :urakkaid urakka-id})
        urakoiden-parametrit (mapv #(assoc parametrit :urakka-id (:id %)
                                                      :urakka-nimi (:nimi %)
                                                      :indeksi (:indeksi %)) urakat)

        ;; Aikavälit ja otsikkotekstit
        kyseessa-kk-vali? (pvm/kyseessa-kk-vali? alkupvm loppupvm)
        kyseessa-hoitokausi-vali? (pvm/kyseessa-hoitokausi-vali? alkupvm loppupvm)
        kyseessa-vuosi-vali? (pvm/kyseessa-vuosi-vali? alkupvm loppupvm)
        laskutettu-teksti (str "Laskutettu hoito\u00ADkaudella ennen " (pvm/kuukausi-ja-vuosi alkupvm) " \u20AC")
        laskutetaan-teksti (str "Laskutetaan " (pvm/kuukausi-ja-vuosi alkupvm) " \u20AC")
        yhteenveto-teksti (str (if (or kyseessa-kk-vali? kyseessa-hoitokausi-vali?)
                                 (str "Hoitokaudella " (pvm/vuosi (first (pvm/paivamaaran-hoitokausi alkupvm))) "-"
                                      (pvm/vuosi (second (pvm/paivamaaran-hoitokausi alkupvm))) " yhteensä" " \u20AC")
                                 (if kyseessa-vuosi-vali?
                                   (str "Vuonna " (pvm/vuosi (l/to-local-date-time alkupvm)) " yhteensä" " \u20AC")
                                   (str (pvm/pvm alkupvm) " - " (pvm/pvm loppupvm) " yhteensä"))))

        ;; Datan nostaminen tietokannasta urakoittain, hyödyntää cachea
        laskutusyhteenvedot (mapv (fn [urakan-parametrit]
                                    (mapv #(assoc % :urakka-id (:urakka-id urakan-parametrit)
                                                    :urakka-nimi (:urakka-nimi urakan-parametrit)
                                                    :indeksi (:indeksi urakan-parametrit))
                                          (hae-laskutusyhteenvedon-tiedot db user urakan-parametrit)))
                                  urakoiden-parametrit)
        ;; Talteen lähtötiedot, josta tutkitaan mahdolliset laskentaa sotkevat puutteet. Mahdollisia puutteita:
        ;; lämpötilatietoja puuttuu (suolasakon laskenta), indeksitietoja puuttuu (perusluku tai raportin aikaisia pistelukuja)
        urakoiden-lahtotiedot (into
                                (sorted-map)
                                (mapv (fn [urakan-laskutusyhteenveto]
                                        (let [talvihoidon-rivi (first (filter #(= "Talvihoito" (:nimi %)) urakan-laskutusyhteenveto))]
                                          {(:urakka-id talvihoidon-rivi)
                                           (select-keys talvihoidon-rivi
                                                        [:urakka-id :urakka-nimi
                                                         :indeksi :perusluku
                                                         :suolasakko_kaytossa :lampotila_puuttuu
                                                         :suolasakot_laskutetaan :suolasakot_laskutettu])}))
                                      laskutusyhteenvedot))
        perusluku (when (= 1 (count urakat))
                    (:perusluku (val (first urakoiden-lahtotiedot))))
        ainakin-yhdessa-urakassa-suolasakko-kaytossa? (some #(:suolasakko_kaytossa (val %)) urakoiden-lahtotiedot)
        ainakin-yhdessa-urakassa-indeksit-kaytossa? (some #(:indeksi (val %)) urakoiden-lahtotiedot)
        urakat-joissa-indeksilaskennan-perusluku-puuttuu (into #{}
                                                               (keep (fn [urakan-lahtotiedot]
                                                                       (let [tiedot (second urakan-lahtotiedot)]
                                                                         (when (and (:indeksi tiedot)
                                                                                    (nil? (:perusluku tiedot)))
                                                                           (:urakka-nimi (second urakan-lahtotiedot)))))
                                                                     urakoiden-lahtotiedot))
        urakat-joissa-suolasakon-laskenta-epaonnistui-ja-lampotila-puuttuu (into #{}
                                                                                 (keep (fn [urakan-lahtotiedot]
                                                                                         (let [tiedot (second urakan-lahtotiedot)]
                                                                                           (when (and (:suolasakko_kaytossa tiedot)
                                                                                                      (:lampotila_puuttuu tiedot)
                                                                                                      (or (nil? (:suolasakot_laskutettu tiedot))
                                                                                                          (nil? (:suolasakot_laskutetaan tiedot))))
                                                                                             (:urakka-nimi (second urakan-lahtotiedot)))))
                                                                                       urakoiden-lahtotiedot))

        suolasakko-kentat (kustannuslajin-kaikki-kentat "suolasakot")
        urakat-joissa-raportin-aikavalilla-indeksiarvon-puute (into #{}
                                                                    (apply concat
                                                                           (keep
                                                                             not-empty
                                                                             (mapv (fn [laskutusyhteenveto]
                                                                                     (set (keep
                                                                                            #(some (fn [rivin-map-entry]
                                                                                                     (when (nil? (val rivin-map-entry))
                                                                                                       (get % :urakka-nimi)))
                                                                                                   (apply dissoc % suolasakko-kentat))
                                                                                            laskutusyhteenveto)))
                                                                                   laskutusyhteenvedot))))
        ;; Datan tuotteittain ryhmittely
        tiedot-tuotteittain (fmap #(group-by :nimi %) laskutusyhteenvedot)
        kaikki-tuotteittain (apply merge-with concat tiedot-tuotteittain)
        ;; Urakoiden datan yhdistäminen yhteenlaskulla
        kaikki-tuotteittain-summattuna (fmap #(apply merge-with (fnil + 0)
                                                     (map (fn [rivi]
                                                            (select-keys rivi (laskettavat-kentat rivi)))
                                                          %))
                                             kaikki-tuotteittain)
        tiedot (into []
                     (map #(merge {:nimi (key %)} (val %)) kaikki-tuotteittain-summattuna))
        kkn-indeksiarvo (when kyseessa-kk-vali?
                          (indeksipalvelu/hae-urakan-kuukauden-indeksiarvo db urakka-id (pvm/vuosi alkupvm) (pvm/kuukausi alkupvm)))


        ;; Varoitustekstit raportille
        varoitus-indeksilaskennan-perusluku-puuttuu (when-not (empty? urakat-joissa-indeksilaskennan-perusluku-puuttuu)
                                                      (if (= 1 (count urakat-joissa-indeksilaskennan-perusluku-puuttuu))
                                                        "Urakan indeksilaskennan perusluku puuttuu."
                                                        (str "Seuraavissa urakoissa indeksilaskennan perusluku puuttuu: "
                                                             (str/join ", "
                                                               (for [u urakat-joissa-indeksilaskennan-perusluku-puuttuu]
                                                                 u)))))

        varoitus-lampotila-puuttuu (when-not (empty? urakat-joissa-suolasakon-laskenta-epaonnistui-ja-lampotila-puuttuu)
                                     (str "Seuraavissa urakoissa talvisuolasakko on käytössä mutta lämpötilatieto puuttuu: "
                                          (str/join ", "
                                                    (for [u urakat-joissa-suolasakon-laskenta-epaonnistui-ja-lampotila-puuttuu]
                                                      u))))

        varoitus-indeksitietojen-puuttumisesta
        (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
          ;; perusluvun puute on fataalein - silloin kaikki muu indeksilaskenta feilaa, eli näytetään vain se tieto.
          ;; Muuten edetään aikaperustaiseen tarkasteluun
          (if varoitus-indeksilaskennan-perusluku-puuttuu
            varoitus-indeksilaskennan-perusluku-puuttuu
            (when-not (empty? urakat-joissa-raportin-aikavalilla-indeksiarvon-puute)
              (str "Seuraavissa urakoissa indeksilaskentaa ei voitu suorittaa koska tarpeellisia indeksiarvoja puuttuu: "
                   (str/join ", "
                             (for [u urakat-joissa-raportin-aikavalilla-indeksiarvon-puute]
                               u))))))
        vain-jvh-voi-muokata-tietoja-viesti "Vain järjestelmän vastuuhenkilö voi syöttää indeksiarvoja ja lämpötiloja Harjaan."

        kentat-kaikki-paitsi-kht #{"yht" "sakot" "suolasakot" "muutostyot" "akilliset_hoitotyot"
                                   "vahinkojen_korjaukset" "bonukset" "erilliskustannukset"}
        kentat-kaikki (conj kentat-kaikki-paitsi-kht "kht")
        taulukot
        (aseta-sheet-nimi
          (concat
            (keep (fn [[otsikko tyhja laskutettu laskutetaan tiedot summa-fmt :as taulukko-rivi]]
                    (when taulukko-rivi
                      (taulukko otsikko tyhja
                                laskutettu-teksti laskutettu
                                laskutetaan-teksti laskutetaan
                                yhteenveto-teksti kyseessa-kk-vali?
                                tiedot (or summa-fmt
                                           (if ainakin-yhdessa-urakassa-indeksit-kaytossa?
                                             fmt/luku-indeksikorotus
                                             fmt/euro-opt)))))
                  [[" Kokonaishintaiset työt " " Ei kokonaishintaisia töitä "
                    :kht_laskutettu :kht_laskutetaan tiedot]
                   [" Yksikköhintaiset työt " " Ei yksikköhintaisia töitä "
                    :yht_laskutettu :yht_laskutetaan tiedot]
                   [" Sanktiot " " Ei sanktioita "
                    :sakot_laskutettu :sakot_laskutetaan tiedot]
                   (when ainakin-yhdessa-urakassa-suolasakko-kaytossa?
                     [" Talvisuolasakko/\u00ADbonus (autom. laskettu) " " Ei talvisuolasakkoa "
                      :suolasakot_laskutettu :suolasakot_laskutetaan tiedot fmt/euro-ei-voitu-laskea])
                   [" Muutos- ja lisätyöt " " Ei muutos- ja lisätöitä "
                    :muutostyot_laskutettu :muutostyot_laskutetaan tiedot]
                   [" Äkilliset hoitotyöt " " Ei äkillisiä hoitotöitä "
                    :akilliset_hoitotyot_laskutettu :akilliset_hoitotyot_laskutetaan tiedot]
                   [" Vahinkojen korjaukset " " Ei vahinkojen korjauksia "
                    :vahinkojen_korjaukset_laskutettu :vahinkojen_korjaukset_laskutetaan tiedot]
                   [" Bonukset " " Ei bonuksia "
                    :bonukset_laskutettu :bonukset_laskutetaan tiedot]
                   [" Erilliskustannukset (muut kuin bonukset) " " Ei erilliskustannuksia "
                    :erilliskustannukset_laskutettu :erilliskustannukset_laskutetaan tiedot]
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Kokonaishintaisten töiden indeksitarkistukset " " Ei indeksitarkistuksia "
                      :kht_laskutettu_ind_korotus :kht_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Yksikköhintaisten töiden indeksitarkistukset " " Ei indeksitarkistuksia "
                      :yht_laskutettu_ind_korotus :yht_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Sanktioiden indeksitarkistukset " " Ei indeksitarkistuksia "
                      :sakot_laskutettu_ind_korotus :sakot_laskutetaan_ind_korotus tiedot])
                   (when (and ainakin-yhdessa-urakassa-indeksit-kaytossa? ainakin-yhdessa-urakassa-suolasakko-kaytossa?)
                     [" Talvisuolasakon/\u00ADbonuksen indeksitarkistus (autom. laskettu) " " Ei indeksitarkistuksia "
                      :suolasakot_laskutettu_ind_korotus :suolasakot_laskutetaan_ind_korotus tiedot fmt/euro-ei-voitu-laskea])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Muutos- ja lisätöiden indeksitarkistukset " " Ei indeksitarkistuksia "
                      :muutostyot_laskutettu_ind_korotus :muutostyot_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Äkillisten hoitotöiden indeksitarkistukset " " Ei indeksitarkistuksia "
                      :akilliset_hoitotyot_laskutettu_ind_korotus :akilliset_hoitotyot_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Vahinkojen korjausten indeksitarkistukset " " Ei indeksitarkistuksia "
                      :vahinkojen_korjaukset_laskutettu_ind_korotus :vahinkojen_korjaukset_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Bonusten indeksitarkistukset " " Ei indeksitarkistuksia "
                      :bonukset_laskutettu_ind_korotus :bonukset_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Erilliskustannusten indeksitarkistukset (muut kuin bonukset) " " Ei indeksitarkistuksia "
                      :erilliskustannukset_laskutettu_ind_korotus :erilliskustannukset_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Muiden kuin kok.hint. töiden indeksitarkistukset yhteensä " " Ei indeksitarkistuksia "
                      :kaikki_paitsi_kht_laskutettu_ind_korotus :kaikki_paitsi_kht_laskutetaan_ind_korotus tiedot])
                   (when ainakin-yhdessa-urakassa-indeksit-kaytossa?
                     [" Kaikki indeksitarkistukset yhteensä " " Ei indeksitarkistuksia "
                      :kaikki_laskutettu_ind_korotus :kaikki_laskutetaan_ind_korotus tiedot])])

            [(summataulukko " Kaikki paitsi kok.hint. työt yhteensä " kentat-kaikki-paitsi-kht
                            laskutettu-teksti laskutetaan-teksti
                            yhteenveto-teksti kyseessa-kk-vali?
                            tiedot
                            (if ainakin-yhdessa-urakassa-indeksit-kaytossa?
                              fmt/luku-indeksikorotus
                              fmt/euro-opt) true)

             (summataulukko " Kaikki yhteensä " kentat-kaikki
                            laskutettu-teksti laskutetaan-teksti
                            yhteenveto-teksti kyseessa-kk-vali?
                            tiedot
                            (if ainakin-yhdessa-urakassa-indeksit-kaytossa?
                              fmt/luku-indeksikorotus
                              fmt/euro-opt) false)]))]

    (vec (keep identity
               [:raportti {:nimi "Laskutusyhteenveto"}
                [:otsikko (str (or (str alueen-nimi ", ") "") (pvm/pvm alkupvm) "-" (pvm/pvm loppupvm))]
                (when perusluku
                  (yleinen/urakan-indlask-perusluku {:perusluku perusluku}))
                (when (and ainakin-yhdessa-urakassa-indeksit-kaytossa? urakka-id)
                  (yleinen/kkn-indeksiarvo {:kyseessa-kk-vali? kyseessa-kk-vali?
                                            :alkupvm alkupvm :kkn-indeksiarvo kkn-indeksiarvo}))

                [:varoitusteksti varoitus-indeksitietojen-puuttumisesta]
                [:varoitusteksti varoitus-lampotila-puuttuu]
                (when (or varoitus-indeksitietojen-puuttumisesta
                          varoitus-lampotila-puuttuu)
                  [:varoitusteksti vain-jvh-voi-muokata-tietoja-viesti])

                (if (empty? taulukot)
                  [:teksti " Ei laskutettavaa"]
                  taulukot)

                (when (and hallintayksikko-id (< 0 (count urakat)))
                  [:otsikko "Raportti sisältää seuraavien urakoiden tiedot: "])
                (when (and hallintayksikko-id (< 0 (count urakat)))
                  (for [u (sort-by :nimi urakat)]
                    [:teksti (str (:nimi u))]))]))))
