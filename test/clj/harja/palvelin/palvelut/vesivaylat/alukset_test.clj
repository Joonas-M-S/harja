(ns harja.palvelin.palvelut.vesivaylat.alukset-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [harja
             [pvm :as pvm]
             [testi :refer :all]]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.yllapito-toteumat :refer :all]
            [harja.tyokalut.functor :refer [fmap]]
            [taoensso.timbre :as log]
            [clojure.string :as str]
            [harja.palvelin.palvelut.vesivaylat.toimenpiteet.apurit :as apurit]
            [harja.palvelin.palvelut.vesivaylat.alukset :as vv-alukset]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as c]

            [harja.domain.vesivaylat.alus :as alus]
            [harja.domain.urakka :as urakka]
            [harja.domain.organisaatio :as organisaatio]
            [harja.domain.muokkaustiedot :as m]))

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db (tietokanta/luo-tietokanta testitietokanta)
                        :http-palvelin (testi-http-palvelin)
                        :pois-kytketyt-ominaisuudet testi-pois-kytketyt-ominaisuudet
                        :vv-alukset (component/using
                                      (vv-alukset/->Alukset)
                                      [:db :http-palvelin])))))
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :each (compose-fixtures
                      jarjestelma-fixture
                      urakkatieto-fixture))

(deftest hae-kaikki-alukset
  (let [kaikkien-alusten-lkm-kannassa (ffirst (q "SELECT COUNT(*) FROM vv_alus"))
        args {}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-kaikki-alukset +kayttaja-jvh+
                              args)]

    (is (s/valid? ::alus/hae-kaikki-alukset-kysely args))
    (is (s/valid? ::alus/hae-kaikki-alukset-vastaus tulos))

    (is (some #(= (::alus/nimi %) "Rohmu") tulos))
    (is (= (count tulos) kaikkien-alusten-lkm-kannassa))))

(deftest hae-urakan-alukset
  (let [urakka-id (hae-helsingin-vesivaylaurakan-id)
        urakan-alusten-lkm-kannassa (ffirst (q "SELECT COUNT(*) FROM vv_alus_urakka WHERE urakka = " urakka-id ";"))
        args {::urakka/id urakka-id}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-urakan-alukset +kayttaja-jvh+
                              args)]

    (is (s/valid? ::alus/hae-urakan-alukset-kysely args))
    (is (s/valid? ::alus/hae-urakan-alukset-vastaus tulos))

    (is (some #(= (::alus/nimi %) "Rohmu") tulos))
    (is (= (count tulos) urakan-alusten-lkm-kannassa))))

(deftest hae-urakoitsijan-alukset
  (let [urakoitsija-id (hae-helsingin-vesivaylaurakan-urakoitsija)
        urakoitsijan-urakat (hae-urakoitsijan-urakka-idt urakoitsija-id)
        urakoitsijan-alusten-lkm-kannassa (ffirst (q "SELECT COUNT(*) FROM vv_alus_urakka
                                                      WHERE urakka IN (" (str/join "," urakoitsijan-urakat) ");"))
        args {::organisaatio/id urakoitsija-id}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-urakoitsijan-alukset +kayttaja-jvh+
                              args)]

    (is (s/valid? ::alus/hae-urakoitsijan-alukset-kysely args))
    (is (s/valid? ::alus/hae-urakoitsijan-alukset-vastaus tulos))

    (is (some #(= (::alus/nimi %) "Rohmu") tulos))
    (is (= (count tulos) urakoitsijan-alusten-lkm-kannassa))))

(deftest hae-alusten-reitit
  (let [args {:alukset nil :alku nil :loppu nil}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-alusten-reitit +kayttaja-jvh+)]

    (is (s/valid? ::alus/:hae-alusten-reitit-kysely args))
    (is (s/valid? ::alus/:hae-alusten-reitit-vastaus tulos))

    (is (every?
          (fn [t]
            (and (every? not-empty (vals t))
                 (= #{::alus/sijainti ::alus/alus-mmsi} (into #{} (keys t)))))
          tulos)))

  (let [args {:alukset #{230111580} :alku nil :loppu nil}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-alusten-reitit +kayttaja-jvh+)]

    (is (s/valid? ::alus/:hae-alusten-reitit-kysely args))
    (is (s/valid? ::alus/:hae-alusten-reitit-vastaus tulos))

    (is (= 1 (count tulos)))
    (is (every?
          (fn [t]
            (and (every? not-empty (vals t))
                 (= #{::alus/sijainti ::alus/alus-mmsi} (into #{} (keys t)))))
          tulos))))

(deftest hae-alusten-reitit-pisteineen
  (let [args {:alukset nil :alku nil :loppu nil}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-alusten-reitit-pisteineen +kayttaja-jvh+)]

    (is (s/valid? ::alus/:hae-alusten-reitit-pisteineen-kysely args))
    (is (s/valid? ::alus/:hae-alusten-reitit-pisteineen-vastaus tulos))

    (is (every?
          (fn [t]
            (and (every? not-empty (vals t))
                 (= #{::alus/sijainti ::alus/alus-mmsi ::alus/pisteet} (into #{} (keys t)))))
          tulos))
    (is (every?
          (fn [p]
            (and (every? not-empty (vals p))
                 (= #{::alus/aika ::alus/sijainti} (into #{} (keys p)))))
          (map ::alus/pisteet tulos))))

  (let [args {:alukset #{230111580} :alku nil :loppu nil}
        tulos (kutsu-palvelua (:http-palvelin jarjestelma)
                              :hae-alusten-reitit-pisteineen +kayttaja-jvh+)]

    (is (s/valid? ::alus/:hae-alusten-reitit-pisteineen-kysely args))
    (is (s/valid? ::alus/:hae-alusten-reitit-pisteineen-vastaus tulos))

    (is (= 1 (count tulos)))
    (is (every?
          (fn [t]
            (and (every? not-empty (vals t))
                 (= #{::alus/sijainti ::alus/alus-mmsi ::alus/pisteet} (into #{} (keys t)))))
          tulos))
    (is (every?
          (fn [p]
            (and (every? not-empty (vals p))
                 (= #{::alus/aika ::alus/sijainti} (into #{} (keys p)))))
          (map ::alus/pisteet tulos)))))
