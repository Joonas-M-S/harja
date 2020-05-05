(ns harja.palvelin.palvelut.laskut
  "Nimiavaruutta käytetään vain urakkatyypissä teiden-hoito (MHU)."
  (:require [com.stuartsierra.component :as component]
            [harja.kyselyt
             [laskut :as q]
             [aliurakoitsijat :as ali-q]
             [kustannusarvioidut-tyot :as kust-q]]
            [harja.kyselyt.konversio :as konv]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.tyokalut.big :as big]
            [harja.palvelin.palvelut.kulut.pdf :as kpdf]
            [harja.palvelin.komponentit.pdf-vienti :as pdf-vienti]
            [harja.palvelin.komponentit.excel-vienti :as excel-vienti]
            [harja.palvelin.raportointi.excel :as excel]
            [harja.pvm :as pvm]
            [harja.kyselyt.konversio :as konversio]))


(defn kasittele-suorittaja
  "Tarkistaa onko aliurakoitsija-id olemassa tai löytyykö aliurakoitsija nimellä. Jos ei löydy, tallentaa aliurakoitsijan.
  Palauttaa olemassa olleen tai juuri tallennetun aliurakoitsijan id:n."
  [db user suorittaja-nimi]
  (let [suorittaja-id (:id (first (ali-q/hae-aliurakoitsija-nimella db
                                                                    {:nimi suorittaja-nimi})))]
    (if (nil? suorittaja-id)
      (do (ali-q/luo-aliurakoitsija<! db {:nimi     suorittaja-nimi
                                          :kayttaja (:id user)})
          (:id (first (ali-q/hae-aliurakoitsija-nimella db
                                                        {:nimi suorittaja-nimi}))))
      suorittaja-id)))

(defn hae-urakan-laskut
  "Palauttaa urakan laskut valitulta ajanjaksolta ilman laskuerittelyä (kohdennustietoja)."
  [db user hakuehdot]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laskutus-laskunkirjoitus user (:urakka-id hakuehdot))
  (q/hae-urakan-laskut db {:urakka   (:urakka-id hakuehdot)
                           :alkupvm  (:alkupvm hakuehdot)
                           :loppupvm (:loppupvm hakuehdot)}))


(defn kasittele-kohdistukset
  [db laskukohdistukset]
  (map
    (fn [[id kohdistukset]]
      (let [lasku (first kohdistukset)
            liitteet (into [] (q/hae-liitteet db {:lasku-id id}))]
        (into {} {:id                    id
                  :tyyppi                (:tyyppi lasku)
                  :kokonaissumma         (:kokonaissumma lasku)
                  :erapaiva              (:erapaiva lasku)
                  :laskun-numero         (:laskun-numero lasku)
                  :koontilaskun-kuukausi (:koontilaskun-kuukausi lasku)
                  :liitteet              liitteet
                  :kohdistukset          (mapv #(dissoc %
                                                        :tyyppi
                                                        :kokonaissumma
                                                        :erapaiva
                                                        :suorittaja-id
                                                        :id
                                                        :liitteet
                                                        :koontilaskun-kuukausi)
                                               kohdistukset)})))
    laskukohdistukset))

(defn hae-kaikki-urakan-laskuerittelyt
  "Palauttaa urakan laskut laskuerittelyineen."
  [db user hakuehdot]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laskutus-laskunkirjoitus user (:urakka-id hakuehdot))
  (let [laskukohdistukset (group-by :id (q/hae-kaikki-urakan-laskuerittelyt db {:urakka (:urakka-id hakuehdot)}))]
    (kasittele-kohdistukset db laskukohdistukset)))

(defn hae-urakan-laskuerittelyt
  "Palauttaa urakan laskut valitulta ajanjaksolta laskuerittelyineen."
  [db user hakuehdot]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laskutus-laskunkirjoitus user (:urakka-id hakuehdot))
  (let [laskukohdistukset (group-by :id (q/hae-urakan-laskuerittelyt db {:urakka   (:urakka-id hakuehdot)
                                                                         :alkupvm  (:alkupvm hakuehdot)
                                                                         :loppupvm (:loppupvm hakuehdot)}))]
    (kasittele-kohdistukset db laskukohdistukset)))

(defn hae-laskuerittely
  "Hakee yksittäisen laskun tiedot laskuerittelyineen."
  [db user {:keys [urakka-id id]}]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (let [lasku (first (q/hae-lasku db {:urakka urakka-id
                                      :id     id}))
        laskun-kohdistukset (into []
                                  (q/hae-laskun-kohdistukset db {:lasku (:id lasku)}))
        liitteet (into [] (q/hae-liitteet db {:lasku-id id}))]
    (if-not (empty? lasku)
      (assoc lasku :kohdistukset laskun-kohdistukset :liitteet liitteet)
      lasku)))

(defn- laskuerittelyn-maksueratyyppi
  "Selvittää laskuerittelyn maksueratyypin, jotta laskun summa lasketaan myöhemmin oikeaan Sampoon lähetettvään maksuerään.
  Yleensä tyyppi on kokonaishintainen. Jos tehtävä on Äkillinen hoitotyö, maksuerätyyppi on akillinen-hoitotyö.
  Jos tehtävä on "
  [db tehtavaryhma-id tehtava-id]
  ;;TODO: tarkista ehdot, korjaa
  (cond (or (.contains (or (:nimi (first (q/hae-tehtavan-nimi db {:id tehtava-id}))) "") "Äkilliset hoitotytöt")
            (.contains (or (:nimi (first (q/hae-tehtavaryhman-nimi db {:id tehtavaryhma-id}))) "") "ÄKILLISET HOITOTYÖT"))
        "akilliset-hoitotyot"
        (or (.contains (or (:nimi (first (q/hae-tehtavan-nimi db {:id tehtava-id}))) "") "vahinkojen korja")
            (.contains (or (:nimi (first (q/hae-tehtavaryhman-nimi db {:id tehtavaryhma-id}))) "") "VAHINKOJEN KORJAAMINEN"))
        "muu"                                               ;; vahinkojen korjaukset
        :default
        "kokonaishintainen"))

(defn luo-tai-paivita-laskun-kohdistus
  "Luo uuden laskuerittelyrivin (kohdistuksen) kantaan tai päivittää olemassa olevan rivin. Rivi tunnistetaan laskun viitteen ja rivinumeron perusteella."
  [db user urakka-id lasku-id laskurivi]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (let [yhteiset {:id                  (:kohdistus-id laskurivi)
                  :summa               (:summa laskurivi)
                  :toimenpideinstanssi (:toimenpideinstanssi laskurivi)
                  :tehtavaryhma        (:tehtavaryhma laskurivi)
                  :maksueratyyppi      (laskuerittelyn-maksueratyyppi db (:tehtavaryhma laskurivi) (:tehtava laskurivi))
                  :alkupvm             (:suoritus-alku laskurivi)
                  :loppupvm            (:suoritus-loppu laskurivi)
                  :kayttaja            (:id user)}]
    (if (nil? (:kohdistus-id laskurivi))
      (q/luo-laskun-kohdistus<! db (assoc yhteiset :lasku lasku-id
                                                   :rivi (:rivi laskurivi)))
      (q/paivita-laskun-kohdistus<! db yhteiset)))
  (kust-q/merkitse-maksuerat-likaisiksi! db {:toimenpideinstanssi
                                             (:toimenpideinstanssi laskurivi)}))


(defn luo-tai-paivita-laskuerittely
  "Tallentaa uuden laskun ja siihen liittyvät kohdistustiedot (laskuerittelyn).
  Päivittää laskun tai kohdistuksen tiedot, jos rivi on jo kannassa.
  Palauttaa tallennetut tiedot."
  [db user urakka-id {:keys [erapaiva kokonaissumma urakka tyyppi laskun-numero
                             lisatieto koontilaskun-kuukausi id kohdistukset liitteet] :as _laskuerittely}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (let [yhteiset-tiedot {:erapaiva              (konv/sql-date erapaiva)
                         :kokonaissumma         kokonaissumma
                         :urakka                urakka
                         :tyyppi                tyyppi
                         :numero                laskun-numero
                         :lisatieto             lisatieto
                         :kayttaja              (:id user)
                         :koontilaskun-kuukausi koontilaskun-kuukausi}
        lasku (if (nil? id)
                (q/luo-lasku<! db yhteiset-tiedot)
                (q/paivita-lasku<! db (assoc yhteiset-tiedot
                                        :id id)))]
    (when-not (or (nil? liitteet)
                  (empty? liitteet))
      (doseq [liite liitteet]
        (q/linkita-lasku-ja-liite<! db {:lasku-id (:id lasku)
                                        :liite-id (:liite-id liite)
                                        :kayttaja (:id user)})))
    (doseq [kohdistusrivi kohdistukset]
      (as-> kohdistusrivi r
            (update r :summa big/unwrap)
            (assoc r :lasku (:id lasku))
            (if (true? (:poistettu r))
              (q/poista-laskun-kohdistus! db {:id              id
                                              :urakka          urakka-id
                                              :kohdistuksen-id (:kohdistus-id r)
                                              :kayttaja        (:id user)})
              (luo-tai-paivita-laskun-kohdistus db
                                                user
                                                urakka
                                                (:id lasku)
                                                r))))
    (hae-laskuerittely db user {:id (:id lasku)})))

(defn poista-lasku
  "Merkitsee laskun sekä kaikki siihen liittyvät kohdistukset poistetuksi."
  [db user {:keys [urakka-id id]}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (let [liitteet (into [] (q/hae-liitteet db {:lasku-id id}))
        poistettu-lasku (hae-laskuerittely db user {:id id})]
    (when (not (empty? liitteet))
      (doseq [{liite-id :liite-id} liitteet]
        (q/poista-laskun-ja-liitteen-linkitys! db {:lasku-id id :liite-id liite-id :kayttaja (:id user)})))
    (q/poista-lasku! db {:urakka   urakka-id
                         :id       id
                         :kayttaja (:id user)})
    (q/poista-laskun-kohdistukset! db {:urakka   urakka-id
                                       :id       id
                                       :kayttaja (:id user)})
    poistettu-lasku))

(defn poista-laskun-kohdistus
  "Poistaa yksittäisen rivin laskuerittelystä (kohdistuksista). Palauttaa päivittyneen kantatilanteen."
  [db user {:keys [urakka-id id kohdistuksen-id]}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (q/poista-laskun-kohdistus! db {:id              id
                                  :urakka          urakka-id
                                  :kohdistuksen-id kohdistuksen-id
                                  :kayttaja        (:id user)})
  (hae-laskuerittely db user {:id id}))

(defn tallenna-lasku
  "Funktio tallentaa laskun ja laskuerittelyn (laskun kohdistuksen). Käytetään teiden hoidon urakoissa (MHU)."
  [db user {:keys [urakka-id laskuerittely]}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (luo-tai-paivita-laskuerittely db user urakka-id laskuerittely))

(defn- poista-laskun-liite
  "Merkkaa laskun liitteen poistetuksi"
  [db user {:keys [urakka-id lasku-id liite-id]}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (q/poista-laskun-ja-liitteen-linkitys! db {:lasku-id lasku-id :liite-id liite-id :kayttaja (:id user)})
  (hae-laskuerittely db user {:id lasku-id}))

(defn- kulu-pdf
  [db user {:keys [urakka-id alkupvm loppupvm]}]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (println "Kulud" urakka-id)
  (let [kulut (sort-by :erapaiva
                       (q/hae-laskuerittelyt-tietoineen-vientiin db {:urakka   urakka-id
                                                                     :alkupvm  (or alkupvm
                                                                                   (konversio/sql-timestamp (pvm/->pvm "01.01.1990")))
                                                                     :loppupvm (or
                                                                                 loppupvm
                                                                                 (konversio/sql-timestamp (pvm/nyt)))}))]
    (println kulut)
    (kpdf/kulu-pdf kulut)))

(defn- kulu-excel
  [db workbook user {:keys [urakka-id alkupvm loppupvm]}]
  (println "EXCEL" workbook)
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laskutus-laskunkirjoitus user urakka-id)
  (println "Kuludexcel" urakka-id)
  (let [kulut (sort-by :erapaiva
                       (q/hae-laskuerittelyt-tietoineen-vientiin db {:urakka   urakka-id
                                                                     :alkupvm  (or alkupvm
                                                                                   (konversio/sql-timestamp (pvm/->pvm "01.01.1990")))
                                                                     :loppupvm (or
                                                                                 loppupvm
                                                                                 (konversio/sql-timestamp (pvm/nyt)))}))
        luo-sarakkeet (fn [& otsikot]
                        (mapv #(hash-map :otsikko %) otsikot))
        luo-data (fn [rivi]
                   [(-> rivi
                        :erapaiva
                        pvm/pvm
                        str)
                    (:toimenpide rivi)
                    (or (:tehtavaryhma rivi)
                        "Lisätyö")
                    [:arvo-ja-yksikko {:arvo (:summa rivi) :yksikko "€" :fmt? false}]])
        optiot {:sheet-nimi "Kulu Excel"
                :otsikko    "Hurlumhei"}
        sarakkeet (luo-sarakkeet "Eräpäivä" "Toimenpide" "Tehtäväryhmä" "Summa")
        taulukko [:taulukko optiot sarakkeet (mapv luo-data kulut)]]
    (excel/muodosta-excel [:raportti {:nimi "Blah" :orientaatio :landscape} taulukko] workbook)))

;[:raportti
; {:nimi Sanktioiden yhteenveto, :orientaatio :landscape}
; [:taulukko
;  {:otsikko Pohjois-Pohjanmaa ja Kainuu, Sanktioiden yhteenveto ajalta 01.10.2019 - 30.09.2020,
;   :oikealle-tasattavat-kentat #{1 3 2},
;   :sheet-nimi Sanktioiden yhteenveto}
;  [{:otsikko , :leveys 12}
;   {:otsikko Aktiivinen Kajaani Testi, :leveys 15, :fmt :raha}
;   {:otsikko Aktiivinen Oulu Testi, :leveys 15, :fmt :raha}
;   {:otsikko Yh­teen­sä, :leveys 15, :fmt :raha}]
;  [{:otsikko Talvihoito}
;   [Muistutukset
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]]
;   [Sakko A 0 0 0]
;   [- Päätiet 0 0 0]
;   [- Muut tiet 0 0 0]
;   [Sakko B 0 0 0]
;   [- Päätiet 0 0 0]
;   [- Muut tiet 0 0 0]
;   [Talvihoito, sakot yht. 0 0 0]
;   [Talvihoito, indeksit yht. 0 0 0]
;   {:otsikko Muut tuotteet}
;   [Muistutukset
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]]
;   [Sakko A 0 0 0]
;   [- Liikenneymp. hoito 0 0 0]
;   [- Sorateiden hoito 0 0 0]
;   [Sakko B 0 0 0]
;   [- Liikenneymp. hoito 0 0 0]
;   [- Sorateiden hoito 0 0 0]
;   [Muut tuotteet, sakot yht. 0 0 0]
;   [Muut tuotteet, indeksit yht. 0 0 0]
;   {:otsikko Ryhmä C}
;   [Ryhmä C, sakot yht. 0 0 0]
;   [Ryhmä C, indeksit yht. 0 0 0]
;   {:otsikko Yhteensä}
;   [Muistutukset yht.
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]
;    [:arvo-ja-yksikko {:arvo 0, :yksikko  kpl, :fmt? false}]]
;   [Indeksit yht. 0 0 0]
;   [Kaikki sakot yht. 0 0 0]
;   [Kaikki yht. 0 0 0]]
;  ]]

(defn- luo-pdf
  [pdf user hakuehdot]
  (println "Luod")
  (let [{:keys [tiedosto-bytet tiedostonimi]} (pdf-vienti/luo-pdf pdf :kulut user hakuehdot)]
    tiedosto-bytet))

(defrecord Laskut []
  component/Lifecycle
  (start [this]
    (let [db (:db this)
          http (:http-palvelin this)
          pdf (:pdf-vienti this)
          excel (:excel-vienti this)]
      (julkaise-palvelu http :laskut
                        (fn [user hakuehdot]
                          (hae-urakan-laskut db user hakuehdot)))
      (julkaise-palvelu http :laskuerittelyt
                        (fn [user hakuehdot]
                          (hae-urakan-laskuerittelyt db user hakuehdot)))
      (julkaise-palvelu http :kaikki-laskuerittelyt
                        (fn [user hakuehdot]
                          (hae-kaikki-urakan-laskuerittelyt db user hakuehdot)))
      (julkaise-palvelu http :lasku
                        (fn [user hakuehdot]
                          (hae-laskuerittely db user hakuehdot)))
      (julkaise-palvelu http :tallenna-lasku
                        (fn [user laskuerittely]
                          (tallenna-lasku db user laskuerittely)))
      (julkaise-palvelu http :poista-lasku
                        (fn [user hakuehdot]
                          (poista-lasku db user hakuehdot)))
      (julkaise-palvelu http :poista-laskurivi
                        (fn [user hakuehdot]
                          (poista-laskun-kohdistus db user hakuehdot)))
      (julkaise-palvelu http :poista-laskun-liite
                        (fn [user hakuehdot]
                          (poista-laskun-liite db user hakuehdot)))
      (julkaise-palvelu http :luo-pdf-kuluista
                        (fn [user hakuehdot]
                          (luo-pdf pdf user hakuehdot)))
      (when pdf
        (pdf-vienti/rekisteroi-pdf-kasittelija! pdf :kulut (partial #'kulu-pdf db)))
      (when excel
        (excel-vienti/rekisteroi-excel-kasittelija! excel :kulut (partial #'kulu-excel db)))
      this))

  (stop [this]
    (poista-palvelut (:http-palvelin this) :laskut
                     :lasku
                     :laskuerittelyt
                     :kaikki-laskuerittelyt
                     :tallenna-lasku
                     :poista-lasku
                     :poista-laskurivi
                     :poista-laskun-liite
                     :luo-pdf-kuluista)
    (when (:pdf-vienti this)
      (pdf-vienti/poista-pdf-kasittelija! (:pdf-vienti this) :kulut))
    (when (:excel-vienti this)
      (excel-vienti/poista-excel-kasittelija! (:excel-vienti this) :kulut))
    this))
