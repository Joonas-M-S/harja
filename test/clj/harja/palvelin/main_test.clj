(ns harja.palvelin.main-test
  "Testaa, että main käynnistää kaikki halutut komponentit. Tämän testin pointti on suojata
  ettei komponenttien lisäämisessä tule virheitä ja joitain tarvittuja komponentteja poistu.
  Kun lisäät komponentin, lisää se myös testin keysettiin."
  (:require [harja.palvelin.main :as sut]
            [harja.palvelin.asetukset :as asetukset]
            [harja.palvelin.tyokalut.jarjestelma :as jarjestelma]
            [harja.palvelin.tyokalut.komponentti-protokollat :as kp]
            [harja.testi :as testi]
            [com.stuartsierra.component :as component]
            [com.stuartsierra.dependency :as dep]
            [clojure.test :refer [is deftest testing use-fixtures]]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (java.io File)
           (clojure.lang ExceptionInfo)))

(def ^:dynamic *testiasetukset* nil)
(def jarjestelma (atom nil))

(defn- muokkaa-asetuksia [asetukset]
  (let [asetukset-datana (-> (edn/read-string asetukset)
                             (assoc-in [:http-palvelin :portti] (testi/arvo-vapaa-portti))
                             (assoc-in [:http-palvelin :salli-oletuskayttaja?] false)
                             (assoc-in [:http-palvelin :dev-resources-path] "dev-resources")
                             (assoc :tietokanta testi/testitietokanta)
                             (assoc :tietokanta-replica testi/testitietokanta)
                             (assoc :sonja {:url "tcp://localhost:61617"
                                            :kayttaja ""
                                            :salasana ""
                                            :tyyppi :activemq})
                             (assoc :sampo {})
                             (assoc :tloik {})
                             (assoc-in [:turi :turvallisuuspoikkeamat-url] "")
                             (assoc-in [:turi :urakan-tyotunnit-url] ""))]
    (str asetukset-datana)))

(defn- poista-sisimmat-sulut-reader-makrosta [s]
  (str/replace s #"\#=\(.*(\([^\(\)]*\))" (fn [args]
                                            (case (count args)
                                              0 ""
                                              1 (first args)
                                              2 (apply str (let [lopputulos (first args)
                                                                 pudotettavien-maara (count (second args))]
                                                             (drop-last pudotettavien-maara lopputulos)))))))

(defn- poista-reader-makrot [teksti korvaava-teksti]
  (loop [teksti teksti
         sisaltaa-readermakroja? (re-find #"\#=" teksti)]
    (if sisaltaa-readermakroja?
      (recur (-> teksti
                 poista-sisimmat-sulut-reader-makrosta
                 (str/replace #"\#=\([^\(\)]*\)" korvaava-teksti))
             (re-find #"\#=" teksti))
      teksti)))

(defn- testiasetukset [testit]
  (let [file (File/createTempFile "asetukset" ".edn")
        asetukset (-> "asetukset.edn"
                      slurp
                      (poista-reader-makrot "\"foo\"")
                      muokkaa-asetuksia)]
    (testi/pudota-ja-luo-testitietokanta-templatesta)
    (testi/pystyta-harja-tarkkailija)
    (spit file asetukset)
    (binding [*testiasetukset* file]
      (testit))
    (when @jarjestelma
      (component/stop @jarjestelma))
    (testi/lopeta-harja-tarkkailija)
    (.delete file)))

(use-fixtures :once testiasetukset)

(def halutut-komponentit
  #{:metriikka
    :db :db-replica
    :todennus :http-palvelin
    :pdf-vienti :excel-vienti
    :virustarkistus :liitteiden-hallinta :kehitysmoodi
    :integraatioloki :sonja :sonja-sahkoposti :solita-sahkoposti :fim :sampo :tloik :tierekisteri :labyrintti
    :turi :yha-integraatio :velho-integraatio :raportointi :paivystystarkistukset :reittitarkistukset
    :kayttajatiedot :urakoitsijat :hallintayksikot :ping :pois-kytketyt-ominaisuudet :haku
    :indeksit :urakat :urakan-toimenpiteet :yksikkohintaiset-tyot :kokonaishintaiset-tyot :budjettisuunnittelu :tehtavamaarat
    :muut-tyot :laskut :aliurakoitsijat :toteumat :yllapitototeumat :paallystys :maaramuutokset
    :yllapitokohteet :muokkauslukko :yhteyshenkilot :toimenpidekoodit :pohjavesialueet
    :materiaalit :selainvirhe :valitavoitteet :siltatarkastukset :lampotilat :maksuerat
    :liitteet :laadunseuranta :tarkastukset :ilmoitukset :tietyoilmoitukset
    :turvallisuuspoikkeamat :integraatioloki-palvelu :raportit :yha :velho :tr-haku
    :geometriapaivitykset :api-yhteysvarmistus :sonja-jms-yhteysvarmistus :tilannekuva
    :tienakyma :karttakuvat :debug :sahke :api-jarjestelmatunnukset :geometria-aineistot
    :organisaatiot :api-urakat :api-laatupoikkeamat :api-paivystajatiedot :api-pistetoteuma
    :api-reittitoteuma :api-varustetoteuma :api-siltatarkastukset :api-tarkastukset
    :api-tyokoneenseuranta :api-tyokoneenseuranta-puhdistus :api-turvallisuuspoikkeama
    :api-suolasakkojen-lahetys :api-varusteet :api-ilmoitukset :api-yllapitokohteet :api-ping
    :api-yhteystiedot :api-tiemerkintatoteuma :laskutusyhteenvetojen-muodostus :status
    :vaylien-geometriahaku
    :kanavasiltojen-geometriahaku
    :mobiili-laadunseuranta
    :api-urakan-tyotunnit
    :sopimukset
    :urakan-tyotuntimuistutukset
    :hankkeet
    :urakan-tyotunnit
    :vv-toimenpiteet
    :vv-vaylat
    :vv-kiintiot
    :vv-hinnoittelut
    :vv-materiaalit
    :reimari
    :vkm
    :vv-turvalaitteet
    :hairioilmoitukset
    :ais-data
    :vv-alukset
    :kan-kohteet
    :kan-liikennetapahtumat
    :komponenttien-tila
    :kan-hairio
    :kan-toimenpiteet
    :api-tieluvat
    :api-paikkaukset
    :koordinaatit
    :tiedostopesula
    :tieluvat
    :paikkaukset
    :jarjestelman-tila
    :yha-paikkauskomponentti
    :pot2})

(def ei-statusta
  #{:metriikka
    :todennus
    :pdf-vienti :excel-vienti
    :virustarkistus :liitteiden-hallinta :kehitysmoodi
    :integraatioloki :sonja :sonja-sahkoposti :solita-sahkoposti :fim :sampo :tloik :tierekisteri :labyrintti
    :turi :yha-integraatio :velho-integraatio :raportointi :paivystystarkistukset :reittitarkistukset
    :kayttajatiedot :urakoitsijat :hallintayksikot :ping :pois-kytketyt-ominaisuudet :haku
    :indeksit :urakat :urakan-toimenpiteet :yksikkohintaiset-tyot :kokonaishintaiset-tyot :budjettisuunnittelu :tehtavamaarat
    :muut-tyot :laskut :aliurakoitsijat :toteumat :yllapitototeumat :paallystys :maaramuutokset
    :yllapitokohteet :muokkauslukko :yhteyshenkilot :toimenpidekoodit :pohjavesialueet
    :materiaalit :selainvirhe :valitavoitteet :siltatarkastukset :lampotilat :maksuerat
    :liitteet :laadunseuranta :tarkastukset :ilmoitukset :tietyoilmoitukset
    :turvallisuuspoikkeamat :integraatioloki-palvelu :raportit :yha :velho :tr-haku
    :geometriapaivitykset :api-yhteysvarmistus :sonja-jms-yhteysvarmistus :tilannekuva
    :tienakyma :karttakuvat :debug :sahke :api-jarjestelmatunnukset :geometria-aineistot
    :organisaatiot :api-urakat :api-laatupoikkeamat :api-paivystajatiedot :api-pistetoteuma
    :api-reittitoteuma :api-varustetoteuma :api-siltatarkastukset :api-tarkastukset
    :api-tyokoneenseuranta :api-tyokoneenseuranta-puhdistus :api-turvallisuuspoikkeama
    :api-suolasakkojen-lahetys :api-varusteet :api-ilmoitukset :api-yllapitokohteet :api-ping
    :api-yhteystiedot :api-tiemerkintatoteuma :laskutusyhteenvetojen-muodostus :status
    :vaylien-geometriahaku
    :kanavasiltojen-geometriahaku
    :mobiili-laadunseuranta
    :api-urakan-tyotunnit
    :sopimukset
    :urakan-tyotuntimuistutukset
    :hankkeet
    :urakan-tyotunnit
    :vv-toimenpiteet
    :vv-vaylat
    :vv-kiintiot
    :vv-hinnoittelut
    :vv-materiaalit
    :reimari
    :vkm
    :vv-turvalaitteet
    :hairioilmoitukset
    :ais-data
    :vv-alukset
    :kan-kohteet
    :kan-liikennetapahtumat
    :komponenttien-tila
    :kan-hairio
    :kan-toimenpiteet
    :api-tieluvat
    :api-paikkaukset
    :koordinaatit
    :tiedostopesula
    :tieluvat
    :paikkaukset
    :jarjestelman-tila
    :yha-paikkauskomponentti})

(deftest main-komponentit-loytyy
  (reset! jarjestelma (component/start (sut/luo-jarjestelma (asetukset/lue-asetukset *testiasetukset*))))
  (let [komponentit (set (keys @jarjestelma))]
    (testing "Kaikki halutut komponentit löytyy!"
      (doseq [k halutut-komponentit]
        (is (komponentit k) (str "Haluttu komponentti avaimella " k " puuttuu!"))))
    (testing "Ei löydy ylimääräisiä komponentteja"
      (doseq [k komponentit]
        (is (halutut-komponentit k) (str "Ylimääräinen komponentti avaimella " k ", lisää testiin uudet komponentit!"))))
    (testing "Kaikkien komponenttien uudelleen käynnistys toimii"
      (try (let [sammutettu-jarjestelma (component/update-system-reverse @jarjestelma komponentit (fn [k]
                                                                                                    (component/stop k)))]
             (reset! jarjestelma sammutettu-jarjestelma))
           (catch ExceptionInfo e
             (is false (str "Komponenttien pysäyttäminen epäonnistui!\n"
                            "Viesti: " (ex-message e) "\n"
                            "Data: " (ex-data e) "\n"
                            "Cause: " (ex-cause e))))
           (catch Throwable t
             (is false (str "Komponentin pysäyttäminen epäonnistui!\n"
                            "Viesti: " (.getMessage t)))))
      (try (let [kaynnistetty-jarjestelma (component/update-system @jarjestelma komponentit (fn [k]
                                                                                              (component/start k)))]
             (reset! jarjestelma kaynnistetty-jarjestelma))
           (catch ExceptionInfo e
             (is false (str "Komponenttien käynnistäminen epäonnistui!\n"
                            "Viesti: " (ex-message e) "\n"
                            "Data: " (ex-data e) "\n"
                            "Cause: " (ex-cause e))))
           (catch Throwable t
             (is false (str "Komponentin käynnistäminen epäonnistui!\n"
                            "Viesti: " (.getMessage t)))))
      (doseq [komponentti komponentit]
        (or (contains? ei-statusta komponentti)
            (is (try (kp/status-ok? (get @jarjestelma komponentti))
                     (catch Throwable t
                       false))
                (str "Komponentin " komponentti " status ei ole ok uudelleen käynnistämisen jälkeen.")))))))
