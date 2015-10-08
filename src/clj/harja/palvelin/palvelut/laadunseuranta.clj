(ns harja.palvelin.palvelut.laadunseuranta
  "Laadunseuranta: Tarkastukset, Havainnot ja Sanktiot"

  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelut poista-palvelut]]

            [harja.kyselyt.havainnot :as havainnot]
            [harja.kyselyt.kommentit :as kommentit]
            [harja.kyselyt.liitteet :as liitteet]
            [harja.kyselyt.sanktiot :as sanktiot]
            [harja.kyselyt.tarkastukset :as tarkastukset]
            [harja.kyselyt.kayttajat :as kayttajat-q]
            [harja.kyselyt.urakat :as urakat-q]

            [harja.kyselyt.konversio :as konv]
            [harja.domain.roolit :as roolit]
            [harja.geo :as geo]

            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]))

(def havainto-xf (comp
                   (geo/muunna-pg-tulokset :sijainti)
                   (map konv/alaviiva->rakenne)
                   (map #(assoc % :selvitys-pyydetty (:selvityspyydetty %)))
                   (map #(dissoc % :selvityspyydetty))
                   (map #(assoc % :tekija (keyword (:tekija %))))
                   (map #(update-in % [:paatos :paatos]
                                    (fn [p]
                                      (when p (keyword p)))))
                   (map #(update-in % [:paatos :kasittelytapa]
                                    (fn [k]
                                      (when k (keyword k)))))
                   (map #(if (nil? (:kasittelyaika (:paatos %)))
                          (dissoc % :paatos)
                          %))))

(def tarkastus-xf
  (comp
    (geo/muunna-pg-tulokset :sijainti)
    (map konv/alaviiva->rakenne)
    (map #(konv/string->keyword % :tyyppi [:havainto :tekija]))
    (map #(-> %1
              (assoc-in [:havainto :selvitys-pyydetty] (get-in %1 [:havainto :selvitys-pyydetty]))
              (update-in [:havainto] dissoc :selvityspyydetty)
              (update-in [:havainto] (fn [h]
                                       (if (nil? (:selvitys-pyydetty h))
                                         (dissoc h :selvitys-pyydetty)
                                         h)))))

    (map #(dissoc % :sopimus))                              ;; tarvitaanko sopimusta?
    (map (fn [tarkastus]
           (condp = (:tyyppi tarkastus)
             :talvihoito (dissoc tarkastus :soratiemittaus)
             :soratie (dissoc tarkastus :talvihoitomittaus)
             :tiesto (dissoc tarkastus :soratiemittaus :talvihoitomittaus)
             :laatu (dissoc tarkastus :soratiemittaus :talvihoitomittaus)
             :pistokoe (dissoc tarkastus :soratiemittaus :talvihoitomittaus))))))

(defn hae-urakan-havainnot [db user {:keys [listaus urakka-id alku loppu]}]
  (when urakka-id (roolit/vaadi-lukuoikeus-urakkaan user urakka-id))
  (jdbc/with-db-transaction [db db]
    (let [listaus (or listaus :tilannekuva)
          urakka-idt (if-not (nil? urakka-id)
                       (if (vector? urakka-id) urakka-id [urakka-id])

                       (if (get (:roolit user) "jarjestelmavastuuhenkilo")
                         (mapv :id (urakat-q/hae-kaikki-urakat-aikavalilla db (konv/sql-date alku) (konv/sql-date loppu)))
                         (mapv :urakka_id (kayttajat-q/hae-kayttajan-urakka-roolit db (:id user)))))
          _ (log/debug "Haetaan havaintoja urakoista " (pr-str urakka-idt))
          tulos (apply (comp vec flatten merge)
                       (for [urakka-id urakka-idt]
                         (into []
                               havainto-xf

                               (if (= :omat listaus)
                                 (apply havainnot/hae-omat-havainnot
                                        (conj [db urakka-id (konv/sql-timestamp alku) (konv/sql-timestamp loppu)] (:id user)))
                                 (apply (case listaus
                                          :kaikki havainnot/hae-kaikki-havainnot
                                          :selvitys havainnot/hae-selvitysta-odottavat-havainnot
                                          :kasitellyt havainnot/hae-kasitellyt-havainnot
                                          :tilannekuva havainnot/hae-havainnot-tilannekuvaan)
                                        [db urakka-id (konv/sql-timestamp alku) (konv/sql-timestamp loppu)])))))]
      (log/debug "Löydettiin havainnot: " (pr-str (mapv :id tulos)))
      tulos)))


(defn hae-havainnon-tiedot
  "Hakee yhden havainnon kaiken tiedon muokkausnäkymää varten: havainnon perustiedot, kommentit ja liitteet, päätös ja sanktiot.
   Ottaa urakka-id:n ja havainto-id:n. Urakka id:tä käytetään oikeustarkistukseen, havainnon tulee olla annetun urakan
   toimenpiteeseen kytketty."
  [db user urakka-id havainto-id]
  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [havainto (first (into []
                              havainto-xf
                              (havainnot/hae-havainnon-tiedot db urakka-id havainto-id)))]
    (when havainto
      (assoc havainto
        :kommentit (into []
                         (comp (map konv/alaviiva->rakenne)
                               (map #(assoc % :tekija (name (:tekija %))))
                               (map (fn [{:keys [liite] :as kommentti}]
                                      (if (:id liite)
                                        kommentti
                                        (dissoc kommentti :liite)))))
                         (havainnot/hae-havainnon-kommentit db havainto-id))
        :sanktiot (into []
                        (comp (map #(konv/array->set % :tyyppi_sanktiolaji keyword))
                              (map konv/alaviiva->rakenne)
                              (map #(konv/string->keyword % :laji))
                              (map #(assoc %
                                     :sakko? (not (nil? (:summa %)))
                                     :summa (some-> % :summa double))))
                        (sanktiot/hae-havainnon-sanktiot db havainto-id))
        :liitteet (into [] (havainnot/hae-havainnon-liitteet db havainto-id))))))

(defn hae-urakan-sanktiot
  "Hakee urakan sanktiot perintäpvm:n mukaan"
  [db user {:keys [urakka-id alku loppu]}]

  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  (log/debug "Hae sanktiot (" urakka-id alku loppu ")")
  (into []
        (comp (geo/muunna-pg-tulokset :havainto_sijainti)
              (map #(konv/string->keyword % :havainto_paatos_kasittelytapa))
              (map konv/alaviiva->rakenne)
              (map #(konv/decimal->double % :summa))
              (map #(assoc % :laji (keyword (:laji %)))))
        (sanktiot/hae-urakan-sanktiot db urakka-id (konv/sql-timestamp alku) (konv/sql-timestamp loppu))))

(defn tallenna-havainnon-sanktio
  [db user {:keys [id perintapvm laji tyyppi summa indeksi suorasanktio] :as sanktio} havainto urakka]
  (log/debug "TALLENNA sanktio: " sanktio ", urakka: " urakka ", tyyppi: " tyyppi ", havaintoon " havainto)
  (if (or (nil? id) (neg? id))
    (let [uusi-sanktio (sanktiot/luo-sanktio<!
                         db (konv/sql-timestamp perintapvm)
                         (name laji) (:id tyyppi)
                         urakka
                         summa indeksi havainto (or suorasanktio false))]
      (sanktiot/merkitse-maksuera-likaiseksi! db (:id uusi-sanktio))
      (:id uusi-sanktio))

    (do
      (sanktiot/paivita-sanktio!
        db (konv/sql-timestamp perintapvm)
        (name laji) (:id tyyppi)
        urakka
        summa indeksi havainto (or suorasanktio false)
        id)
      (sanktiot/merkitse-maksuera-likaiseksi! db id)
      id)))

(defn tallenna-havainto [db user {:keys [urakka] :as havainto}]
  (log/info "Tuli havainto: " havainto)
  (roolit/vaadi-rooli-urakassa user roolit/havaintojen-kirjaus urakka)
  (jdbc/with-db-transaction [c db]

    (let [osapuoli (roolit/osapuoli user urakka)
          havainto (assoc havainto
                     ;; Jos osapuoli ei ole urakoitsija, voidaan asettaa selvitys-pyydetty päälle
                     :selvitys-pyydetty (and (not= :urakoitsija osapuoli)
                                             (:selvitys-pyydetty havainto))

                     ;; Jos urakoitsija kommentoi, asetetaan selvitys annettu
                     :selvitys-annettu (and (:uusi-kommentti havainto)
                                            (= :urakoitsija osapuoli)))
          id (havainnot/luo-tai-paivita-havainto c user havainto)]
      ;; Luodaan uudet kommentit
      (when-let [uusi-kommentti (:uusi-kommentti havainto)]
        (log/info "UUSI KOMMENTTI: " uusi-kommentti)
        (let [liite (some->> uusi-kommentti
                             :liite
                             :id
                             (liitteet/hae-urakan-liite-id c urakka)
                             first
                             :id)
              kommentti (kommentit/luo-kommentti<! c
                                                   (name (:tekija havainto))
                                                   (:kommentti uusi-kommentti)
                                                   liite
                                                   (:id user))]
          ;; Liitä kommentti havaintoon
          (havainnot/liita-kommentti<! c id (:id kommentti))))

      ;; Liitä liite havaintoon
      (when-let [uusi-liite (:uusi-liite havainto)]
        (log/info "UUSI LIITE: " uusi-liite)
        (havainnot/liita-liite<! c id (:id uusi-liite)))


      (when (:paatos (:paatos havainto))
        ;; Urakanvalvoja voi kirjata päätöksen
        (roolit/vaadi-rooli-urakassa user roolit/urakanvalvoja urakka)
        (log/info "Kirjataan päätös havainnolle: " id ", päätös: " (:paatos havainto))
        (let [{:keys [kasittelyaika paatos perustelu kasittelytapa muukasittelytapa]} (:paatos havainto)]
          (havainnot/kirjaa-havainnon-paatos! c
                                              (konv/sql-timestamp kasittelyaika)
                                              (name paatos) perustelu
                                              (name kasittelytapa) muukasittelytapa
                                              (:id user)
                                              id))
        (when (= :sanktio (:paatos (:paatos havainto)))
          (doseq [sanktio (:sanktiot havainto)]
            (tallenna-havainnon-sanktio c user sanktio id urakka))))

      (hae-havainnon-tiedot c user urakka id))))

(defn hae-sanktiotyypit
  "Palauttaa kaikki sanktiotyypit, hyvin harvoin muuttuvaa dataa."
  [db user]
  (into []
        ;; Muunnetaan sanktiolajit arraysta, keyword setiksi
        (map #(konv/array->set % :laji keyword))
        (sanktiot/hae-sanktiotyypit db)))


(defn hae-urakan-tarkastukset
  "Palauttaa urakan tarkastukset annetulle aikavälille."
  [db user {:keys [urakka-id alkupvm loppupvm tienumero tyyppi]}]
  (when urakka-id (roolit/vaadi-lukuoikeus-urakkaan user urakka-id))

  (jdbc/with-db-transaction [db db]
    (let [urakka-idt (if-not (nil? urakka-id)
                       (if (vector? urakka-id) urakka-id [urakka-id])

                       (if (get (:roolit user) "jarjestelmavastuuhenkilo")
                         (mapv :id (urakat-q/hae-kaikki-urakat-aikavalilla db (konv/sql-date alkupvm) (konv/sql-date loppupvm)))
                         (mapv :urakka_id (kayttajat-q/hae-kayttajan-urakka-roolit db (:id user)))))
          _ (log/debug "Haetaan tarkastuksia urakoista " (pr-str urakka-idt))
          tulos (apply (comp vec flatten merge)
                       (for [urakka-id urakka-idt]
                         (into []
                               tarkastus-xf
                               (tarkastukset/hae-urakan-tarkastukset db urakka-id
                                                                     (konv/sql-timestamp alkupvm)
                                                                     (konv/sql-timestamp loppupvm)
                                                                     (if tienumero true false) tienumero
                                                                     (if tyyppi true false) (and tyyppi (name tyyppi))))))]
      (log/debug "Löydettiin tarkastukset: " (pr-str (mapv :id tulos)))
      tulos)))

(defn hae-tarkastus [db user urakka-id tarkastus-id]
  (roolit/vaadi-lukuoikeus-urakkaan user urakka-id)
  (let [tarkastus (first (into [] tarkastus-xf (tarkastukset/hae-tarkastus db urakka-id tarkastus-id)))]
    (assoc tarkastus
      :havainto (hae-havainnon-tiedot db user urakka-id (:id (:havainto tarkastus))))))

(defn tallenna-tarkastus [db user urakka-id tarkastus]
  (roolit/vaadi-rooli-urakassa user roolit/havaintojen-kirjaus urakka-id)
  (try
    (jdbc/with-db-transaction [c db]
      (let [havainto (merge (:havainto tarkastus)
                            {:aika   (:aika tarkastus)
                             :urakka urakka-id})

            uusi? (nil? (:id tarkastus))
            havainto-id (:id (tallenna-havainto db user havainto)) ;; (havainnot/luo-tai-paivita-havainto c user havainto)
            id (tarkastukset/luo-tai-paivita-tarkastus c user urakka-id tarkastus
                                                       havainto-id)]

        (condp = (:tyyppi tarkastus)
          :talvihoito (tarkastukset/luo-tai-paivita-talvihoitomittaus c id uusi? (:talvihoitomittaus tarkastus))
          :soratie (tarkastukset/luo-tai-paivita-soratiemittaus c id uusi? (:soratiemittaus tarkastus))
          nil)


        (log/info "SAATIINPA urakalle " urakka-id " tarkastus: " tarkastus)
        (hae-tarkastus c user urakka-id id)))
    (catch Exception e
      (log/info e "Tarkastuksen tallennuksessa poikkeus!"))))

(defn tallenna-suorasanktio [db user sanktio havainto urakka]
  ;; Roolien tarkastukset on kopioitu havainnon kirjaamisesta,
  ;; riittäisi varmaan vain roolit/urakanvalvoja?
  (log/info "Tallenna suorasanktio " (:id sanktio) " havaintoon " (:id havainto) ", urakassa " urakka)
  (roolit/vaadi-rooli-urakassa user roolit/havaintojen-kirjaus urakka)
  (roolit/vaadi-rooli-urakassa user roolit/urakanvalvoja urakka)

  (jdbc/with-db-transaction [c db]
    (let [;; FIXME: Suorasanktiolle pyydetty/annettu flagit?
          #_osapuoli #_(roolit/osapuoli user urakka)
          #_havainto #_(assoc havainto
                         :selvitys-pyydetty (and (not= :urakoitsija osapuoli)
                                                 (:selvitys-pyydetty havainto))
                         :selvitys-annettu (and (:uusi-kommentti havainto)
                                                (= :urakoitsija osapuoli)))
          id (havainnot/luo-tai-paivita-havainto c user (assoc havainto :tekija "tilaaja"))]

      (let [{:keys [kasittelyaika paatos perustelu kasittelytapa muukasittelytapa]} (:paatos havainto)]
        (havainnot/kirjaa-havainnon-paatos! c
                                            (konv/sql-timestamp kasittelyaika)
                                            (name paatos) perustelu
                                            (name kasittelytapa) muukasittelytapa
                                            (:id user)
                                            id))

      ;; Frontilla oletetaan että palvelu palauttaa tallennetun sanktion id:n
      ;; Jos tämä muuttuu, pitää frontillekin tehdä muutokset.
      (tallenna-havainnon-sanktio c user sanktio id urakka))))

(defrecord Laadunseuranta []
  component/Lifecycle
  (start [{:keys [http-palvelin db] :as this}]

    (julkaise-palvelut
      http-palvelin

      :hae-urakan-havainnot
      (fn [user tiedot]
        (hae-urakan-havainnot db user tiedot))

      :tallenna-havainto
      (fn [user havainto]
        (tallenna-havainto db user havainto))

      :tallenna-suorasanktio
      (fn [user tiedot]
        (tallenna-suorasanktio db user (:sanktio tiedot) (:havainto tiedot) (get-in tiedot [:havainto :urakka])))

      :hae-havainnon-tiedot
      (fn [user {:keys [urakka-id havainto-id]}]
        (hae-havainnon-tiedot db user urakka-id havainto-id))

      :hae-urakan-sanktiot
      (fn [user tiedot]
        (hae-urakan-sanktiot db user tiedot))

      :hae-sanktiotyypit
      (fn [user]
        (hae-sanktiotyypit db user))

      :hae-urakan-tarkastukset
      (fn [user tiedot]
        (hae-urakan-tarkastukset db user tiedot))

      :tallenna-tarkastus
      (fn [user {:keys [urakka-id tarkastus]}]
        (tallenna-tarkastus db user urakka-id tarkastus))


      :hae-tarkastus
      (fn [user {:keys [urakka-id tarkastus-id]}]
        (hae-tarkastus db user urakka-id tarkastus-id)))
    this)

  (stop [{:keys [http-palvelin] :as this}]
    (poista-palvelut http-palvelin
                     :hae-urakan-havainnot
                     :tallenna-havainto
                     :hae-havainnon-tiedot
                     :hae-urakan-sanktiot
                     :hae-sanktiotyypit
                     :hae-urakan-tarkastukset
                     :tallenna-tarkastus
                     :tallenna-suorasanktio
                     :hae-tarkastus)
    this))
            
