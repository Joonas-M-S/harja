(ns harja.palvelin.palvelut.tilannekuva-test
  (:require [clojure.test :refer :all]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.tietokanta :as tietokanta]
            [harja.palvelin.palvelut.tilannekuva :refer :all]
            [harja.testi :refer :all]
            [com.stuartsierra.component :as component]
            [harja.kyselyt.konversio :as konv]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [harja.domain.tilannekuva :as tk]
            [harja.palvelin.palvelut.karttakuvat :as karttakuvat]))


(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
    (fn [_]
      (component/start
        (component/system-map
          :db (tietokanta/luo-tietokanta testitietokanta)
          :http-palvelin (testi-http-palvelin)
          :karttakuvat (component/using
                        (karttakuvat/luo-karttakuvat)
                        [:http-palvelin :db])
          :toteumat (component/using
                     (->Tilannekuva)
                      [:http-palvelin :db :karttakuvat])))))

  (testit)
  (alter-var-root #'jarjestelma component/stop))


(use-fixtures :once (compose-fixtures tietokanta-fixture jarjestelma-fixture))

(def nykytilanne false)
(def alku (c/to-date (t/local-date 2000 1 1)))
(def loppu (c/to-date (t/local-date 2030 1 1)))
(def urakoitsija nil)
(def urakkatyyppi :hoito)

(def parametrit-laaja-historia
  {:urakoitsija     urakoitsija
   :urakkatyyppi    urakkatyyppi
   :nykytilanne?    nykytilanne
   :alue            {:xmin -550093.049087613, :ymin 6372322.595126259,
                     :xmax 1527526.529326106, :ymax 7870243.751025201} ; Koko Suomi
   :alku            alku
   :loppu           loppu
   :yllapito        {tk/paallystys true
                     tk/paikkaus   true}
   :ilmoitukset     {:tyypit {tk/tpp true
                              tk/urk true
                              tk/tur true}
                     :tilat  #{:kuittaamaton :vastaanotto :aloitus :lopetus :muutos :vastaus}}
   :turvallisuus    {tk/turvallisuuspoikkeamat true}
   :laatupoikkeamat {tk/laatupoikkeama-tilaaja     true
                     tk/laatupoikkeama-urakoitsija true
                     tk/laatupoikkeama-konsultti   true}
   :tarkastukset    {tk/tarkastus-tiesto     true
                     tk/tarkastus-talvihoito true
                     tk/tarkastus-soratie    true
                     tk/tarkastus-laatu      true}
   :talvi           {tk/auraus-ja-sohjonpoisto          true
                     tk/suolaus                         true
                     tk/pistehiekoitus                  true
                     tk/linjahiekoitus                  true
                     tk/lumivallien-madaltaminen        true
                     tk/sulamisveden-haittojen-torjunta true
                     tk/kelintarkastus                  true
                     tk/aurausviitoitus-ja-kinostimet   true
                     tk/lumensiirto                     true
                     tk/paannejaan-poisto               true
                     tk/muu                             true}
   :kesa            {tk/tiestotarkastus            true
                     tk/koneellinen-niitto         true
                     tk/koneellinen-vesakonraivaus true
                     tk/liikennemerkkien-puhdistus true
                     tk/sorateiden-muokkaushoylays true
                     tk/sorateiden-polynsidonta    true
                     tk/sorateiden-tasaus          true
                     tk/sorastus                   true
                     tk/harjaus                    true
                     tk/pinnan-tasaus              true
                     tk/paallysteiden-paikkaus     true
                     tk/paallysteiden-juotostyot   true
                     tk/siltojen-puhdistus         true
                     tk/l-ja-p-alueiden-puhdistus  true
                     tk/muu                        true}})

(defn aseta-filtterit-falseksi [parametrit ryhma]
  (assoc parametrit ryhma (reduce
                            (fn [eka toka]
                              (assoc eka toka false))
                            (ryhma parametrit)
                            (keys (ryhma parametrit)))))

(defn hae-tk
  ([parametrit] (hae-tk +kayttaja-jvh+ parametrit))
  ([kayttaja parametrit]
   (let [urakat (kutsu-palvelua (:http-palvelin jarjestelma)
                                :hae-urakat-tilannekuvaan kayttaja
                                {:nykytilanne? (:nykytilanne? parametrit)
                                 :alku         (:alku parametrit)
                                 :loppu        (:loppu parametrit)
                                 :urakoitsija  (:urakoitsija parametrit)
                                 :urakkatyyppi (:urakkatyyppi parametrit)})
         urakat (into #{} (mapcat
                           (fn [aluekokonaisuus]
                             (map :id (:urakat aluekokonaisuus)))
                           urakat))]
     (kutsu-palvelua (:http-palvelin jarjestelma)
                     :hae-tilannekuvaan kayttaja
                     (tk/valitut-suodattimet (assoc parametrit
                                                    :urakat urakat))))))

(deftest hae-asioita-tilannekuvaan
  (let [vastaus (hae-tk parametrit-laaja-historia)]
    (is (>= (count (:toteumat vastaus)) 1))
    ;; Testaa, että toteuma selitteissä on enemmän kuin 1 toimenpidekoodi
    (is (> (count (distinct (map :toimenpidekoodi (:toteumat vastaus)))) 1))
    (is (>= (count (:turvallisuuspoikkeamat vastaus)) 1))
    (is (not (contains? vastaus :tarkastus)))
    (is (>= (count (:laatupoikkeamat vastaus)) 1))
    (is (>= (count (:paikkaus vastaus)) 1))
    (is (>= (count (:paallystys vastaus)) 1))
    (is (>= (count (:ilmoitukset vastaus)) 1))))

(deftest ala-hae-laatupoikkeamia
  (let [parametrit (aseta-filtterit-falseksi parametrit-laaja-historia :laatupoikkeamat)
        vastaus (hae-tk parametrit)]
    (is (= (count (:laatupoikkeamat vastaus)) 0))))

(deftest ala-hae-toteumia
  (let [parametrit (-> parametrit-laaja-historia
                       (aseta-filtterit-falseksi :kesa)
                       (aseta-filtterit-falseksi :talvi))
        vastaus (hae-tk parametrit)]
    (is (= (count (:toteumat vastaus)) 0))))

;; Urakkatyyppi ei vaikuta enää hakutuloksiin
(deftest urakkatyyppi-filter-toimii
  (let [parametrit (assoc parametrit-laaja-historia :urakkatyyppi :paallystys)
        vastaus (hae-tk parametrit)]
    (is (= (count (:toteumat vastaus)) 3))))

(deftest ala-hae-tarkastuksia
  (let [parametrit (aseta-filtterit-falseksi parametrit-laaja-historia :tarkastukset)
        vastaus (hae-tk parametrit)]
    (is (= (count (:tarkastukset vastaus)) 0))))

(deftest ala-hae-turvallisuuspoikkeamia
  (let [parametrit (aseta-filtterit-falseksi parametrit-laaja-historia :turvallisuus)
        vastaus (hae-tk parametrit)]
    (is (= (count (:turvallisuus vastaus)) 0))))

(deftest ala-hae-ilmoituksia
  (let [parametrit (assoc parametrit-laaja-historia :ilmoitukset {:tyypit {:toimenpidepyynto false
                                                                           :kysely           false
                                                                           :tiedoitus        false}
                                                                  :tilat  #{:avoimet :suljetut}})
        vastaus (hae-tk parametrit)]
    (is (= (count (:ilmoitukset vastaus)) 0))))

(deftest ala-hae-tyokoneita-historianakymaan
  (let [vastaus (hae-tk parametrit-laaja-historia)]
    (is (= (count (:tyokoneet vastaus)) 0))))

(deftest loyda-vahemman-asioita-tiukalla-aikavalilla
  (let [vastaus-pitka-aikavali (hae-tk parametrit-laaja-historia)
        parametrit (-> parametrit-laaja-historia
                       (assoc :alku (c/to-date (t/local-date 2005 1 1)))
                       (assoc :loppu (c/to-date (t/local-date 2010 1 1))))
        vastaus-lyhyt-aikavali (hae-tk parametrit)]
    (is (< (count (:toteumat vastaus-lyhyt-aikavali))
           (count (:toteumat vastaus-pitka-aikavali))))))

(deftest hae-tyokoneet-nykytilaan
  (let [parametrit (assoc parametrit-laaja-historia :nykytilanne? true)
        vastaus (hae-tk parametrit)]
    ;; Työkonetehtäviä löytyi
    (is (not (empty? (:tehtavat (:tyokoneet vastaus)))))))

(deftest ala-hae-toteumia-liian-lahelle-zoomatussa-historianakymassa
  (let [parametrit (assoc parametrit-laaja-historia :alue {:xmin 0,
                                                           :ymin 0,
                                                           :xmax 1,
                                                           :ymax 1})
        vastaus (hae-tk parametrit)]
    (is (= (count (:toteumat vastaus)) 0))))

(deftest ala-hae-tyokoneita-liian-lahelle-zoomatussa-nykytilannenakymassa
  (let [parametrit (-> parametrit-laaja-historia
                       (assoc :alue {:xmin 0,
                                     :ymin 0,
                                     :xmax 1,
                                     :ymax 1})
                       (assoc :nykytilanne? true))
        vastaus (hae-tk parametrit)]
    (is (= (count (vals (:tyokoneet vastaus))) 0))))

(defn- insert-tyokone [urakka organisaatio]
  (let [x 523892
        y 7229981
        sql (str "INSERT INTO tyokonehavainto "
                 "(tyokoneid, jarjestelma, organisaatio, viestitunniste,lahetysaika,tyokonetyyppi,"
                 "sijainti,urakkaid,tehtavat) "
                 "VALUES (666, 'yksikkötesti', " organisaatio ",666,NOW(),'yksikkötesti',"
                 "ST_MakePoint(" x ", " y ")::POINT, "
                 (if urakka urakka "NULL") ", '{harjaus}')")]
    (u sql)))


(deftest vain-tilaaja-ja-urakoitsija-itse-nakee-urakattomat-tyokoneet []
  (let [parametrit (assoc parametrit-laaja-historia :nykytilanne? true)
        urakoitsija (hae-oulun-alueurakan-2005-2012-urakoitsija)
        hae #(get-in (hae-tk % parametrit) [:tyokoneet 666])]
    ;; Insert menee ok
    (is (= 1 (insert-tyokone nil urakoitsija)) "Urakattoman työkonehavainnon voi insertoida")

    ;; jvh näkee työkoneen
    (is (hae +kayttaja-jvh+) "jvh näkee työkoneen")

    ;; ely käyttäjä näkee
    (is (hae +kayttaja-tero+) "ELYläinen näkee työkoneen")

    ;; saman urakoitsijaorganisaation käyttäjä näkee työkoneen
    (is (hae +kayttaja-yit_uuvh+) "Saman urakoitsijan käyttäjä näkee työkoneen")


    ;; eri urakoitsijaorganisaation käyttä ei näe työkonetta
    (is (nil? (hae +kayttaja-ulle+)) "Eri urakoitsijan käyttäjä ei näe työkonetta")))
