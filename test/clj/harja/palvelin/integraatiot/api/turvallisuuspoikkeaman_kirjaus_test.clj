(ns harja.palvelin.integraatiot.api.turvallisuuspoikkeaman-kirjaus-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [harja.testi :refer :all]
            [harja.palvelin.komponentit.liitteet :as liitteet]
            [com.stuartsierra.component :as component]
            [harja.palvelin.integraatiot.api.turvallisuuspoikkeama :as turvallisuuspoikkeama]
            [harja.palvelin.integraatiot.api.tyokalut :as api-tyokalut]
            [cheshire.core :as cheshire]
            [taoensso.timbre :as log]
            [clojure.core.match :refer [match]]
            [harja.palvelin.integraatiot.turi.turi-komponentti :as turi]
            [clj-time.coerce :as c]
            [clj-time.core :as t]
            [harja.kyselyt.konversio :as konv]))

(def kayttaja "yit-rakennus")

(def jarjestelma-fixture
  (laajenna-integraatiojarjestelmafixturea
    kayttaja
    :liitteiden-hallinta (component/using (liitteet/->Liitteet) [:db])
    :turi (component/using
            (turi/->Turi {})
            [:db :integraatioloki :liitteiden-hallinta])
    :api-turvallisuuspoikkeama (component/using (turvallisuuspoikkeama/->Turvallisuuspoikkeama)
                                                [:http-palvelin :db :integraatioloki :liitteiden-hallinta :turi])))

(use-fixtures :once jarjestelma-fixture)

(deftest tallenna-turvallisuuspoikkeama
  (let [urakka (hae-oulun-alueurakan-2005-2012-id)
        tp-kannassa-ennen-pyyntoa (ffirst (q (str "SELECT COUNT(*) FROM turvallisuuspoikkeama;")))
        vastaus (api-tyokalut/post-kutsu ["/api/urakat/" urakka "/turvallisuuspoikkeama"]
                                         kayttaja portti
                                         (-> "test/resurssit/api/turvallisuuspoikkeama.json" slurp))]
    (cheshire/decode (:body vastaus) true)
    (is (= 200 (:status vastaus)))

    ;; Tarkista ensin perustasolla, että turpon kirjaus onnistui ja tiedot löytyvät
    (let [tp-kannassa-pyynnon-jalkeen (ffirst (q (str "SELECT COUNT(*) FROM turvallisuuspoikkeama;")))
          liite-id (ffirst (q (str "SELECT id FROM liite WHERE nimi = 'testitp36934853.jpg';")))
          tp-id (ffirst (q (str "SELECT id FROM turvallisuuspoikkeama WHERE kuvaus ='Aura-auto suistui tieltä väistäessä jalankulkijaa.'")))
          kommentti-id (ffirst (q (str "SELECT id FROM kommentti WHERE kommentti='Testikommentti';")))]

      (is (= (+ tp-kannassa-ennen-pyyntoa 1) tp-kannassa-pyynnon-jalkeen))
      (is (number? liite-id))
      (is (number? tp-id))
      (is (number? kommentti-id)))

    ;; Tiukka tarkastus, datan pitää olla kirjattu täysin oikein
    (let [uusin-tp (as-> (first (q (str "SELECT
                                  id,
                                  urakka,
                                  tapahtunut,
                                  kasitelty,
                                  sijainti,
                                  kuvaus,
                                  sairauspoissaolopaivat,
                                  sairaalavuorokaudet,
                                  tr_numero,
                                  tr_alkuosa,
                                  tr_alkuetaisyys,
                                  tr_loppuosa,
                                  tr_loppuetaisyys,
                                  vahinkoluokittelu,
                                  vakavuusaste,
                                  tyyppi,
                                  tyontekijanammatti,
                                  tyontekijanammatti_muu,
                                  aiheutuneet_seuraukset,
                                  vammat,
                                  vahingoittuneet_ruumiinosat,
                                  sairauspoissaolo_jatkuu,
                                  ilmoittaja_etunimi,
                                  ilmoittaja_sukunimi,
                                  vaylamuoto,
                                  toteuttaja,
                                  tilaaja,
                                  turvallisuuskoordinaattori_etunimi,
                                  turvallisuuskoordinaattori_sukunimi,
                                  laatija_etunimi,
                                  laatija_sukunimi,
                                  tapahtuman_otsikko,
                                  paikan_kuvaus,
                                  vaarallisten_aineiden_kuljetus,
                                  vaarallisten_aineiden_vuoto
                                  FROM turvallisuuspoikkeama
                                  ORDER BY luotu DESC
                                  LIMIT 1;")))
                         turpo
                         ;; Tapahtumapvm ja käsittely -> clj-time
                         (assoc turpo 2 (c/from-sql-date (get turpo 2)))
                         (assoc turpo 3 (c/from-sql-date (get turpo 3)))
                         ;; Vahinkoluokittelu -> set
                         (assoc turpo 13 (into #{} (.getArray (get turpo 13))))
                         ;; Tyyppi -> set
                         (assoc turpo 15 (into #{} (.getArray (get turpo 15))))
                         ;; Vammat -> set
                         (assoc turpo 19 (into #{} (.getArray (get turpo 19))))
                         ;; Vahingoittuneet ruumiinosat -> set
                         (assoc turpo 20 (into #{} (.getArray (get turpo 20)))))
          turpo-id (first uusin-tp)
          korjaava-toimenpide (as-> (first (q (str "SELECT
                                                      kuvaus,
                                                      suoritettu,
                                                      otsikko,
                                                      vastuuhenkilo,
                                                      toteuttaja,
                                                      tila
                                                      FROM korjaavatoimenpide
                                                      WHERE turvallisuuspoikkeama = " turpo-id ";")))
                                    toimenpide
                                    (assoc toimenpide 1 (c/from-sql-date (get toimenpide 1))))]
      (is (match uusin-tp [_
                           urakka
                           (_ :guard #(and (= (t/year %) 2016)
                                           (= (t/month %) 1)
                                           (= (t/day %) 30)))
                           (_ :guard #(and (= (t/year %) 2016)
                                           (= (t/month %) 2)
                                           (= (t/day %) 1)))
                           (_ :guard #(some? %))
                           "Aura-auto suistui tieltä väistäessä jalankulkijaa."
                           2
                           0
                           1234
                           1
                           100
                           73
                           20
                           #{"henkilovahinko"}
                           "vakava"
                           #{"tyotapaturma", "vaaratilanne"}
                           "muu_tyontekija"
                           "Auraaja"
                           "Sairaalareissu"
                           #{"luunmurtumat"}
                           #{"selka", "vartalo"}
                           true
                           "Veera"
                           "Veistelijä"
                           "tie"
                           "Yritys Oy"
                           "Paula Projektipäällikkö"
                           "Mikko"
                           "Meikäläinen"
                           "Urho"
                           "Urakoitsija"
                           "Aura-auto suistui tieltä"
                           "Liukas tie keskellä metsää."
                           true
                           false]
                 true))
      (is (match korjaava-toimenpide
                 ["Kaadetaan risteystä pimentävä pensaikko"
                  (_ :guard #(and (= (t/year %) 2016)
                                  (= (t/month %) 1)
                                  (= (t/day %) 30)))
                  "Kaadetaan pensaikko"
                  nil
                  "Erkki Esimerkki"
                  "avoin"]
                 true)))))