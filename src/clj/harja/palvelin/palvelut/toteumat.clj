(ns harja.palvelin.palvelut.toteumat
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.palvelin.oikeudet :as oik]
            [harja.kyselyt.konversio :as konv]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [harja.domain.skeema :refer [Toteuma validoi]]
            [harja.domain.roolit :as roolit]
            [clojure.java.jdbc :as jdbc]

            [harja.kyselyt.toteumat :as q]
            [harja.kyselyt.materiaalit :as materiaalit-q]
            [harja.kyselyt.muutoshintaiset-tyot :as mht-q]

            [harja.palvelin.palvelut.materiaalit :as materiaalipalvelut]))

(def toteuma-xf
  (comp (map #(-> %
                  (konv/array->vec :tehtavat)
                  (konv/array->vec :materiaalit)))))

(def muunna-desimaaliluvut-xf
  (map #(-> %
            (assoc-in [:maara]
                      (or (some-> % :maara double) 0)))))

(defn toteuman-tehtavat->map [toteumat]
  (let [mapattu (map (fn [rivi]
                       (log/debug "Mapataan rivi: " (pr-str rivi))
                       (assoc rivi :tehtavat
                                   (mapv (fn [tehtava]
                                           (log/debug "Mapataan Tehtävä: " (pr-str tehtava))
                                           (let [splitattu (str/split tehtava #"\^")]
                                             {:tehtava-id (Integer/parseInt (first splitattu))
                                              :tpk-id (Integer/parseInt (second splitattu))
                                              :nimi   (get splitattu 2)
                                              :maara  (Integer/parseInt (get splitattu 3))
                                              }))
                                         (:tehtavat rivi))))
                     toteumat)]
    (log/debug "Mappaus valmis: " (pr-str mapattu))
    mapattu))

(defn toteuman-parametrit [toteuma kayttaja]
  [(:urakka-id toteuma) (:sopimus-id toteuma)
   (konv/sql-timestamp (:alkanut toteuma)) (konv/sql-timestamp (:paattynyt toteuma))
   (name (:tyyppi toteuma)) (:id kayttaja)
   (:suorittajan-nimi toteuma) (:suorittajan-ytunnus toteuma) (:lisatieto toteuma)])

(defn toteumatehtavan-parametrit [toteuma kayttaja]
  [(get-in toteuma [:tehtava :toimenpidekoodi]) (get-in toteuma [:tehtava :maara]) (:id kayttaja)
   (get-in toteuma [:tehtava :paivanhinta])])


(defn hae-urakan-toteumat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi]}]
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [rivit (into []
                    toteuma-xf
                    (q/listaa-urakan-toteumat db urakka-id sopimus-id (konv/sql-date alkupvm) (konv/sql-date loppupvm) (name tyyppi)))]
    (toteuman-tehtavat->map rivit)))

(defn hae-urakan-toteuma [db user {:keys [urakka-id toteuma-id]}]
  (log/debug "Haetaan urakan toteuma id:llä: " toteuma-id)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [rivi (first (into []
                          toteuma-xf
                          (q/listaa-urakan-toteuma db urakka-id toteuma-id)))]
    (first (toteuman-tehtavat->map [rivi]))))

(defn hae-urakan-toteumien-tehtavien-summat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi]}]
  (log/debug "Haetaan urakan toteuman tehtävien summat: " urakka-id sopimus-id alkupvm loppupvm tyyppi)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into []
        muunna-desimaaliluvut-xf
        (q/listaa-toteumien-tehtavien-summat db urakka-id sopimus-id (konv/sql-date alkupvm) (konv/sql-date loppupvm) (name tyyppi))))

(defn hae-urakan-toteutuneet-tehtavat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi]}]
  (log/debug "Haetaan urakan toteutuneet tehtävät: " urakka-id sopimus-id alkupvm loppupvm tyyppi)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [toteutuneet-tehtavat (into []
                                   muunna-desimaaliluvut-xf
                                   (q/hae-urakan-toteutuneet-tehtavat db urakka-id sopimus-id (konv/sql-timestamp alkupvm) (konv/sql-timestamp loppupvm) (name tyyppi)))]
    (log/debug "Haetty urakan toteutuneet tehtävät: " toteutuneet-tehtavat)
    toteutuneet-tehtavat))

(defn hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi toimenpidekoodi]}]
  (log/debug "Haetaan urakan toteutuneet tehtävät tyypillä ja toimenpidekoodilla: " urakka-id sopimus-id alkupvm loppupvm tyyppi toimenpidekoodi)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into []
        muunna-desimaaliluvut-xf
        (q/hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla db urakka-id sopimus-id (konv/sql-timestamp alkupvm) (konv/sql-timestamp loppupvm) (name tyyppi) toimenpidekoodi)))

(defn hae-urakan-toteuma-paivat [db user {:keys [urakka-id sopimus-id alkupvm loppupvm]}]
  (log/debug "Haetaan urakan toteumapäivän: " urakka-id)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into #{}
        (map :paiva)
        (q/hae-urakan-toteuma-paivat db urakka-id sopimus-id (konv/sql-date alkupvm) (konv/sql-date loppupvm))))


(defn hae-urakan-tehtavat [db user urakka-id]
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into []
        (q/hae-urakan-tehtavat db urakka-id)))

(defn kasittele-toteumatehtava [c user toteuma tehtava]
  (if (and (:tehtava-id tehtava) (pos? (:tehtava-id tehtava)))
    (do
      (if (:poistettu tehtava)
        (do (log/debug "Poistetaan tehtävä: " (pr-str tehtava))
            (q/poista-toteuman-tehtava! c (:tehtava-id tehtava)))
        (do (log/debug "Pävitetään tehtävä: " (pr-str tehtava))
            (q/paivita-toteuman-tehtava! c (:toimenpidekoodi tehtava) (:maara tehtava) (or (:poistettu tehtava) false)
                                         (or (:paivanhinta tehtava) nil)
                                         (:tehtava-id tehtava)))))
    (do
      (when (not (:poistettu tehtava))
        (log/debug "Luodaan uusi tehtävä.")
        (q/luo-tehtava<! c (:toteuma-id toteuma) (:toimenpidekoodi tehtava) (:maara tehtava) (:id user) nil)))))

(defn kasittele-toteuman-tehtavat [c user toteuma]
  (doseq [tehtava (:tehtavat toteuma)]
    (kasittele-toteumatehtava c user toteuma tehtava)))

(defn paivita-toteuma [c user toteuma]
  (do
    (q/paivita-toteuma! c (konv/sql-date (:alkanut toteuma)) (konv/sql-date (:paattynyt toteuma)) (:id user)
                        (:suorittajan-nimi toteuma) (:suorittajan-ytunnus toteuma) (:lisatieto toteuma) (:toteuma-id toteuma) (:urakka-id toteuma))
    (kasittele-toteuman-tehtavat c user toteuma)
    (:toteuma-id toteuma)))

(defn luo-toteuma [c user toteuma]
  (do
    (let [toteuman-parametrit (into [] (concat [c] (toteuman-parametrit toteuma user)))
          uusi (apply q/luo-toteuma<! toteuman-parametrit)
          id (:id uusi)
          toteumatyyppi (name (:tyyppi toteuma))]
      (doseq [{:keys [toimenpidekoodi maara]} (:tehtavat toteuma)]
        (q/luo-tehtava<! c id toimenpidekoodi maara (:id user) nil)
        (q/merkitse-toteuman-maksuera-likaiseksi! c toteumatyyppi toimenpidekoodi))
      id)))

(defn tallenna-toteuma-ja-yksikkohintaiset-tehtavat ; FIXME Tallenna luoja, muokattu & muokkaaja
  "Tallentaa toteuman. Palauttaa sen ja tehtävien summat."
  [db user toteuma]
  (oik/vaadi-rooli-urakassa user roolit/toteumien-kirjaus (:urakka toteuma))
  (log/debug "Toteuman tallennus aloitettu. Payload: " (pr-str toteuma))
  (jdbc/with-db-transaction [c db]
                            (let [id
                                  (if (:toteuma-id toteuma)
                                    (paivita-toteuma c user toteuma)
                                    (luo-toteuma c user toteuma))
                                  paivitetyt-summat
                                  (hae-urakan-toteumien-tehtavien-summat c user
                                                                         {:urakka-id (:urakka-id toteuma)
                                                                          :sopimus-id (:sopimus-id toteuma)
                                                                          :alkupvm (konv/sql-timestamp (:hoitokausi-aloituspvm toteuma))
                                                                          :loppupvm (konv/sql-timestamp (:hoitokausi-lopetuspvm toteuma))
                                                                          :tyyppi (:tyyppi toteuma)})]
                              (log/debug "Päivitetyt summat: " paivitetyt-summat)
                              {:toteuma (assoc toteuma :toteuma-id id)
                               :tehtavien-summat paivitetyt-summat})))

(defn paivita-yk-hint-toiden-tehtavat  ; FIXME Tallenna muokattu & muokkaajja
  "Päivittää yksikköhintaisen töiden toteutuneet tehtävät. Palauttaa päivitetyt tehtävät sekä tehtävien summat"
  [db user {:keys [urakka-id sopimus-id alkupvm loppupvm tyyppi tehtavat]}]
  (oik/vaadi-rooli-urakassa user #{roolit/urakanvalvoja roolit/urakoitsijan-urakan-vastuuhenkilo} urakka-id)
  (log/debug (str "Yksikköhintaisten töiden päivitys aloitettu. Payload: " (pr-str (into [] tehtavat))))

  (let [tehtavatidt (into #{} (map #(:tehtava_id %) tehtavat))]
    (jdbc/with-db-transaction [c db]
                              (doall
                                (for [tehtava tehtavat]
                                  (do
                                    (log/debug (str "Päivitetään saapunut tehtävä. id: " (:tehtava_id tehtava)))
                                    (q/paivita-toteuman-tehtava! c (:toimenpidekoodi tehtava) (:maara tehtava) (:poistettu tehtava) (or (:paivanhinta tehtava) nil) (:tehtava_id tehtava)))))

                              (log/debug "Merkitään tehtavien: " tehtavatidt " maksuerät likaisiksi")
                              (q/merkitse-toteumatehtavien-maksuerat-likaisiksi! c tehtavatidt)))

  (let [paivitetyt-tehtavat (hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla db user
                                                                                {:urakka-id urakka-id
                                                                                 :sopimus-id sopimus-id
                                                                                 :alkupvm alkupvm
                                                                                 :loppupvm loppupvm
                                                                                 :tyyppi tyyppi
                                                                                 :toimenpidekoodi (:toimenpidekoodi (first tehtavat))})
        paivitetyt-summat (hae-urakan-toteumien-tehtavien-summat db user
                                                                 {:urakka-id urakka-id
                                                                  :sopimus-id sopimus-id
                                                                  :alkupvm alkupvm
                                                                  :loppupvm loppupvm
                                                                  :tyyppi tyyppi})]
    (log/debug "Palautetaan päivittynyt data: " (pr-str paivitetyt-tehtavat))
    {:tehtavat paivitetyt-tehtavat :tehtavien-summat paivitetyt-summat}))

(def erilliskustannus-tyyppi-xf
  (map #(assoc % :tyyppi (keyword (:tyyppi %)))))

(def erilliskustannus-rahasumma-xf
  (map #(if (:rahasumma %)
         (assoc % :rahasumma (double (:rahasumma %)))
         (identity %))))

(def erilliskustannus-xf
  (comp
    erilliskustannus-tyyppi-xf
    erilliskustannus-rahasumma-xf))

(defn hae-urakan-erilliskustannukset [db user {:keys [urakka-id alkupvm loppupvm]}]
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into []
        erilliskustannus-xf
        (q/listaa-urakan-hoitokauden-erilliskustannukset db urakka-id (konv/sql-date alkupvm) (konv/sql-date loppupvm))))

(defn tallenna-erilliskustannus [db user ek]
  (oik/vaadi-rooli-urakassa user
                            roolit/toteumien-kirjaus
                            (:urakka-id ek))
  (jdbc/with-db-transaction [c db]
                            (let [parametrit [c (:tyyppi ek) (:sopimus ek) (:toimenpideinstanssi ek)
                                              (konv/sql-date (:pvm ek)) (:rahasumma ek) (:indeksin_nimi ek) (:lisatieto ek) (:id user)]]
                              (if (not (:id ek))
                                (apply q/luo-erilliskustannus<! parametrit)

                                (apply q/paivita-erilliskustannus! (concat parametrit [(or (:poistettu ek) false) (:id ek)]))))
                            (q/merkitse-toimenpideinstanssin-kustannussuunnitelma-likaiseksi! c (:toimenpideinstanssi ek))
                            (hae-urakan-erilliskustannukset c user {:urakka-id (:urakka-id ek)
                                                                    :alkupvm   (:alkupvm ek)
                                                                    :loppupvm  (:loppupvm ek)})))


(def muut-tyot-rahasumma-xf
  (map #(if (:tehtava_paivanhinta %)
         (assoc % :tehtava_paivanhinta (double (:tehtava_paivanhinta %)))
         (identity %))))

(def muut-tyot-tyyppi-xf
  (map #(if (:tyyppi %)
         (assoc % :tyyppi (keyword (:tyyppi %)))
         (identity %))))

(def muut-tyot-maara-xf
  (map #(if (:tehtava_maara %)
         (assoc % :tehtava_maara (double (:tehtava_maara %)))
         (identity %))))


(def muut-tyot-xf
  (comp
    muut-tyot-rahasumma-xf
    muut-tyot-maara-xf
    (map konv/alaviiva->rakenne)
    muut-tyot-tyyppi-xf))

(defn hae-urakan-muut-tyot [db user {:keys [urakka-id sopimus-id alkupvm loppupvm]}]
  (log/debug "Haetaan urakan muut työt: " urakka-id " ajalta " alkupvm "-" loppupvm)
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into []
        muut-tyot-xf
        (q/listaa-urakan-hoitokauden-toteumat-muut-tyot db urakka-id sopimus-id (konv/sql-date alkupvm) (konv/sql-date loppupvm))))

(defn tallenna-muiden-toiden-toteuma
  [db user toteuma]
  (oik/vaadi-rooli-urakassa user
                            #{roolit/urakanvalvoja roolit/urakoitsijan-urakan-vastuuhenkilo}
                            (:urakka-id toteuma))
  (log/debug "tallenna-muiden-toiden-toteuma: " (pr-str toteuma))
  ;; FIXME: tästä alaspäin en vielä kokonaan toimi!
  ;; käytä olemassa olevia toteuma_tehtavan päivityskyselyitä jos pystyt. Refaktoroi
  ;; kyselyjä tarpeen mukaan niin että mm. ottavat :tyyppi parametrin toteumaan (yksikkohintainen, muutostyo, jne)
  (jdbc/with-db-transaction [c db]
                            (let [toteuman-parametrit (into [] (concat [c] (toteuman-parametrit toteuma user)))
                                  toteumatehtava (assoc (:tehtava toteuma)
                                                   :tehtava-id (get-in toteuma [:tehtava :id]))]
                              (log/debug "Toteuman parametrit: " toteuman-parametrit)
                              (if (get-in toteuma [:tehtava :id])
                                (do
                                  ; :alkanut, paattynyt = :paattynyt, muokattu = NOW(), muokkaaja = :kayttaja,
                                  ; suorittajan_nimi = :suorittajan_nimi, suorittajan_ytunnus = :ytunnus, lisatieto = :lisatieto
                                  ; WHERE id = :id AND urakka = :urakka;

                                  (log/debug "Päiväitä toteuma")
                                  (q/paivita-toteuma! c (konv/sql-date (:alkanut toteuma)) (konv/sql-date (:paattynyt toteuma)) (:id user)
                                                      (:suorittajan-nimi toteuma) (:suorittajan-ytunnus toteuma) (:lisatieto toteuma) (:toteuma-id toteuma) (:urakka-id toteuma))
                                  (log/debug "Käsitellään toteumatehtävä: " (pr-str toteumatehtava))
                                  ;;(:toimenpidekoodi tehtava) (:maara tehtava)
                                  ;; (or (:poistettu tehtava) false) (:tehtava-id tehtava)
                                  (kasittele-toteumatehtava c user toteuma toteumatehtava))
                                (do
                                  (log/debug "Luodaan uusi toteuma")
                                  (let [uusi (apply q/luo-toteuma<! toteuman-parametrit)
                                        id (:id uusi)
                                        toteumatyyppi (name (:tyyppi toteuma))
                                        ;fixme: maksuerätyypit selvitettävä asiakkaalta muutos-, lisä- ja äkilliset työt -toteumille
                                        maksueratyyppi (if (or (= toteumatyyppi "muutostyo")
                                                               (= (toteumatyyppi "akillinen-hoitotyo"))
                                                               (= (toteumatyyppi "lisatyo")))
                                                         "lisatyo"
                                                         "muu")
                                        toteumatehtavan-parametrit
                                        (into [] (concat [c id] (toteumatehtavan-parametrit toteuma user)))
                                        {:keys [toimenpidekoodi]} (:tehtava toteuma)]
                                    (log/debug (str "Luodaan uudelle toteumalle id " id " tehtävä" toteumatehtavan-parametrit))
                                    (apply q/luo-tehtava<! toteumatehtavan-parametrit)
                                    (log/debug "Merkitään maksuera likaiseksi maksuerätyypin: " maksueratyyppi " toteumalle jonka toimenpidekoodi on: " toimenpidekoodi)
                                    (q/merkitse-toteuman-maksuera-likaiseksi! c maksueratyyppi toimenpidekoodi)
                                    true)))
                              ;; lisätään tarvittaessa hinta muutoshintainen_tyo tauluun
                              (when (:uusi-muutoshintainen-tyo toteuma)
                                (let [parametrit [c (:yksikko toteuma) (:yksikkohinta toteuma) (:id user)
                                                  (:urakka-id toteuma)(:sopimus-id toteuma) (get-in toteuma [:tehtava :toimenpidekoodi])
                                                  (konv/sql-date (:urakan-alkupvm toteuma))
                                                  (konv/sql-date (:urakan-loppupvm toteuma))]]
                                  (apply mht-q/lisaa-muutoshintainen-tyo<! parametrit)))
                              (hae-urakan-muut-tyot c user
                                                    {:urakka-id (:urakka-id toteuma)
                                                     :sopimus-id (:sopimus-id toteuma)
                                                     :alkupvm (konv/sql-timestamp (:hoitokausi-aloituspvm toteuma))
                                                     :loppupvm (konv/sql-timestamp (:hoitokausi-lopetuspvm toteuma))}))))



(defn tallenna-toteuma-ja-toteumamateriaalit
  "Tallentaa toteuman ja toteuma-materiaalin, ja palauttaa lopuksi kaikki urakassa käytetyt materiaalit (yksi rivi per materiaali).
  Tiedon mukana tulee yhteenlaskettu summa materiaalin käytöstä.
  * Jos tähän funktioon tehdään muutoksia, pitäisi muutokset tehdä myös
  materiaalit/tallenna-toteumamateriaaleja! funktioon (todnäk)"
  [db user t toteumamateriaalit hoitokausi sopimus]
  (oik/vaadi-rooli-urakassa user #{roolit/urakanvalvoja roolit/urakoitsijan-urakan-vastuuhenkilo} ;fixme roolit??
                            (:urakka t))
  (log/debug "Tallenna toteuma: " (pr-str t) " ja toteumamateriaalit " (pr-str toteumamateriaalit))
  (jdbc/with-db-transaction [c db]
                            ;; Jos toteumalla on positiivinen id, toteuma on olemassa
                            (let [toteuma (if (and (:id t) (pos? (:id t)))
                                            ;; Jos poistettu=true, halutaan toteuma poistaa.
                                            ;; Molemmissa tapauksissa parametrina saatu toteuma tulee palauttaa
                                            (if (:poistettu t)
                                              (do
                                                (log/debug "Poistetaan toteuma " (:id t))
                                                (q/poista-toteuma! c (:id user) (:id t))
                                                t)
                                              (do
                                                (log/debug "Pävitetään toteumaa " (:id t))
                                                (q/paivita-toteuma! c (konv/sql-date (:alkanut t)) (konv/sql-date (:paattynyt t)) (:id user)
                                                                    (:suorittajan-nimi t) (:suorittajan-ytunnus t) (:lisatieto t) (:id t) (:urakka t))
                                                t))
                                            ;; Jos id:tä ei ole tai se on negatiivinen, halutaan luoda uusi toteuma
                                            ;; Tässä tapauksessa palautetaan kyselyn luoma toteuma
                                            (do
                                              (log/debug "Luodaan uusi toteuma")
                                              (q/luo-toteuma<!
                                                c (:urakka t) (:sopimus t) (konv/sql-date (:alkanut t))
                                                (konv/sql-date (:paattynyt t)) (:tyyppi t) (:id user)
                                                (:suorittajan-nimi t)
                                                (:suorittajan-ytunnus t)
                                                (:lisatieto t))))]
                              (log/debug "Toteuman tallentamisen tulos:" (pr-str toteuma))

                              (doall
                                (for [tm toteumamateriaalit]
                                  ;; Positiivinen id = luodaan tai poistetaan toteuma-materiaali
                                  (if (and (:id tm) (pos? (:id tm)))
                                    (if (:poistettu tm)
                                      (do
                                        (log/debug "Poistetaan materiaalitoteuma " (:id tm))
                                        (materiaalit-q/poista-toteuma-materiaali! c (:id user) (:id tm)))
                                      (do
                                        (log/debug "Päivitä materiaalitoteuma "
                                                  (:id tm) " (" (:materiaalikoodi tm) ", " (:maara tm) ", " (:poistettu tm) "), toteumassa " (:id toteuma))
                                        (materiaalit-q/paivita-toteuma-materiaali!
                                          c (:materiaalikoodi tm) (:maara tm) (:id user) (:id toteuma) (:id tm))))
                                    (do
                                      (log/debug "Luo uusi materiaalitoteuma (" (:materiaalikoodi tm) ", " (:maara tm) ") toteumalle " (:id toteuma))
                                      (materiaalit-q/luo-toteuma-materiaali<! c (:id toteuma) (:materiaalikoodi tm) (:maara tm) (:id user))))))
                              ;; Jos saatiin parametrina hoitokausi, voidaan palauttaa urakassa käytetyt materiaalit
                              ;; Tämä ei ole ehkä paras mahdollinen tapa hoitaa tätä, mutta toteuma/materiaalit näkymässä
                              ;; tarvitaan tätä tietoa. -Teemu K
                              (when hoitokausi
                                (materiaalipalvelut/hae-urakassa-kaytetyt-materiaalit c user (:urakka toteuma) (first hoitokausi) (second hoitokausi) sopimus)))))

(defn poista-toteuma!
  [db user t]
  (oik/vaadi-rooli-urakassa user #{roolit/urakanvalvoja roolit/urakoitsijan-urakan-vastuuhenkilo} ;fixme roolit??
                            (:urakka t))
  (jdbc/with-db-transaction [c db]
                            (let [mat-ja-teht (q/hae-toteuman-toteuma-materiaalit-ja-tehtavat c (:id t))
                                  tehtavaidt (filterv #(not (nil? %)) (map :tehtava_id mat-ja-teht))]

                              (log/debug "Merkitään tehtavien: " tehtavaidt " maksuerät likaisiksi")
                              (q/merkitse-toteumatehtavien-maksuerat-likaisiksi! c tehtavaidt)

                              (materiaalit-q/poista-toteuma-materiaali!
                                c (:id user) (filterv #(not (nil? %)) (map :materiaali_id mat-ja-teht)))
                              (q/poista-tehtava! c (:id user) tehtavaidt)
                              (q/poista-toteuma! c (:id user) (:id t))
                              true)))

(defn poista-tehtava!
  "Poistaa toteuma-tehtävän id:llä. Vaatii lisäksi urakan id:n oikeuksien tarkastamiseen.
  {:urakka X, :id [A, B, ..]}"
  [db user tiedot]
  (oik/vaadi-rooli-urakassa user #{roolit/urakanvalvoja roolit/urakoitsijan-urakan-vastuuhenkilo} ;fixme roolit??
                            (:urakka tiedot))
  (let [tehtavaid (:id tiedot)]
    (log/debug "Merkitään tehtava: " tehtavaid " maksuerä likaiseksi")
    (q/merkitse-toteumatehtavien-maksuerat-likaisiksi! db tehtavaid)

    (q/poista-tehtava! db (:id user) (:id tiedot))))

(defrecord Toteumat []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          db (:db this)]
      (julkaise-palvelu http :urakan-toteumat
                        (fn [user tiedot]
                          (hae-urakan-toteumat db user tiedot)))
      (julkaise-palvelu http :urakan-toteuma
                        (fn [user tiedot]
                          (hae-urakan-toteuma db user tiedot)))
      (julkaise-palvelu http :urakan-toteumien-tehtavien-summat
                        (fn [user tiedot]
                          (hae-urakan-toteumien-tehtavien-summat db user tiedot)))
      (julkaise-palvelu http :poista-toteuma!
                        (fn [user toteuma]
                          (poista-toteuma! db user toteuma)))
      (julkaise-palvelu http :poista-tehtava!
                        (fn [user tiedot]
                          (poista-tehtava! db user tiedot)))
      (julkaise-palvelu http :urakan-toteutuneet-tehtavat
                        (fn [user tiedot]
                          (hae-urakan-toteutuneet-tehtavat db user tiedot)))
      (julkaise-palvelu http :urakan-toteutuneet-tehtavat-toimenpidekoodilla
                        (fn [user tiedot]
                          (hae-urakan-toteutuneet-tehtavat-toimenpidekoodilla db user tiedot)))
      (julkaise-palvelu http :urakan-toteuma-paivat
                        (fn [user tiedot]
                          (hae-urakan-toteuma-paivat db user tiedot)))
      (julkaise-palvelu http :hae-urakan-tehtavat
                        (fn [user urakka-id]
                          (hae-urakan-tehtavat db user urakka-id)))
      (julkaise-palvelu http :tallenna-urakan-toteuma-ja-yksikkohintaiset-tehtavat
                        (fn [user toteuma]
                          (tallenna-toteuma-ja-yksikkohintaiset-tehtavat db user toteuma)))
      (julkaise-palvelu http :paivita-yk-hint-toteumien-tehtavat
                        (fn [user tiedot]
                          (paivita-yk-hint-toiden-tehtavat db user tiedot)))
      (julkaise-palvelu http :urakan-erilliskustannukset
                        (fn [user tiedot]
                          (hae-urakan-erilliskustannukset db user tiedot)))
      (julkaise-palvelu http :tallenna-erilliskustannus
                        (fn [user toteuma]
                          (tallenna-erilliskustannus db user toteuma)))
      (julkaise-palvelu http :urakan-muut-tyot
                        (fn [user tiedot]
                          (hae-urakan-muut-tyot db user tiedot)))
      (julkaise-palvelu http  :tallenna-muiden-toiden-toteuma
                        (fn [user toteuma]
                          (tallenna-muiden-toiden-toteuma db user toteuma)))
      (julkaise-palvelu http :tallenna-toteuma-ja-toteumamateriaalit
                        (fn [user tiedot]
                          (tallenna-toteuma-ja-toteumamateriaalit db user (:toteuma tiedot)
                                                                  (:toteumamateriaalit tiedot)
                                                                  (:hoitokausi tiedot)
                                                                  (:sopimus tiedot))))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :urakan-toteumat
      :urakan-toteuma-paivat
      :hae-urakan-tehtavat
      :tallenna-urakan-toteuma
      :urakan-erilliskustannukset
      :urakan-muut-tyot
      :tallenna-muiden-toiden-toteuma
      :paivita-yk-hint-toteumien-tehtavat
      :tallenna-erilliskustannus
      :tallenna-toteuma-ja-toteumamateriaalit
      :poista-toteuma!
      :poista-tehtava!)
    this))
