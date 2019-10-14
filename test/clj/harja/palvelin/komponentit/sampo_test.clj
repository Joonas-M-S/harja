(ns ^:integraatio harja.palvelin.komponentit.sampo-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as a :refer [<!! <! >!! >! go go-loop thread timeout put! alts!! chan poll!]]
            [com.stuartsierra.component :as component]
            [cheshire.core :as cheshire]
            [harja.palvelin.asetukset :as asetukset]
            [harja.testi :refer :all]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.palvelin.integraatiot.sampo
             [tyokalut :as sampo-tk]
             [sampo-komponentti :as sampo]
             [vienti :as vienti]]
            [harja.palvelin.palvelut.budjettisuunnittelu :as bs]
            [harja.pvm :as pvm]
            [harja.palvelin.main :as sut]
            [harja.palvelin.komponentit.sonja :as sonja]
            [harja.palvelin.komponentit.tapahtumat :as tapahtumat]
            [harja.palvelin.integraatiot.sonja.tyokalut :as s-tk]))

(defonce asetukset {:sonja {:url "tcp://localhost:61617"
                            :kayttaja ""
                            :salasana ""
                            :tyyppi :activemq}
                    :sampo {:lahetysjono-sisaan sampo-tk/+lahetysjono-sisaan+
                            :kuittausjono-sisaan sampo-tk/+kuittausjono-sisaan+
                            :lahetysjono-ulos sampo-tk/+lahetysjono-ulos+
                            :kuittausjono-ulos sampo-tk/+kuittausjono-ulos+
                            :paivittainen-lahetysaika nil}})

(defonce ai-port 8162)

(defn jarjestelma-fixture [testit]
  (alter-var-root #'jarjestelma
                  (fn [_]
                    (component/start
                      (component/system-map
                        :db ds
                        :http-palvelin (testi-http-palvelin)
                        :sonja (component/using
                                 (sonja/luo-oikea-sonja (:sonja asetukset))
                                 [:db])
                        :integraatioloki (component/using (integraatioloki/->Integraatioloki nil)
                                                          [:db])
                        :pois-kytketyt-ominaisuudet testi-pois-kytketyt-ominaisuudet
                        :sampo (component/using (let [sampo (:sampo asetukset)]
                                                  (sampo/->Sampo (:lahetysjono-sisaan sampo)
                                                                 (:kuittausjono-sisaan sampo)
                                                                 (:lahetysjono-ulos sampo)
                                                                 (:kuittausjono-ulos sampo)
                                                                 (:paivittainen-lahetysaika sampo)))
                                                [:sonja :db :integraatioloki :pois-kytketyt-ominaisuudet])
                        :klusterin-tapahtumat (component/using
                                                (tapahtumat/luo-tapahtumat)
                                                [:db])))))
  ;; aloita-sonja palauttaa kanavan.
  (<!! (sut/aloita-sonja jarjestelma))
  ;; Merkataan kaikki kannassa oleva testidata lähetetyksi ennen testien ajoa ja purgetaan jono.
  ;; Mikäli joku testi riippuu siitä, että testidataa ei olla merkattu lähetetyksi, tämä aiheuttaa
  ;; sen epäonnistumisen
  (vienti/aja-paivittainen-lahetys (:sonja jarjestelma) (:integraatioloki jarjestelma) (:db jarjestelma) (get-in asetukset [:sampo :lahetysjono-ulos]))
  (s-tk/sonja-jolokia-jono (get-in asetukset [:sampo :lahetysjono-ulos]) nil :purge)
  (testit)
  (alter-var-root #'jarjestelma component/stop))

(use-fixtures :each (compose-fixtures tietokanta-fixture jarjestelma-fixture))

;; muu = vahinkojen korjaukset

;{:paallystepaikkaukset     "20107"
;   :mhu-yllapito             "20191"
;   :talvihoito               "23104"
;   :liikenneympariston-hoito "23116"
;   :sorateiden-hoito         "23124"
;   :mhu-korvausinvestointi   "14301"
;   :mhu-johto                "23151"}
(deftest merkataan-budjettitavotteita-likaiseksi-ja-lähetetään-ne-Sampoon
  (let [{urakka-id :id alkupvm :alkupvm} (first (q-map (str "SELECT id, alkupvm
                                                             FROM   urakka
                                                             WHERE  nimi = 'Ivalon MHU testiurakka (uusi)'")))
        urakan-aloitusvuosi (pvm/vuosi alkupvm)
        kokonaishintaisten-toimenpide-avaimet #{:paallystepaikkaukset :mhu-yllapito :talvihoito :liikenneympariston-hoito :sorateiden-hoito :mhu-korvausinvestointi}
        kokonaishintainen-tyo (fn [toimenpide-avain]
                                {:urakka-id urakka-id
                                 :toimenpide-avain toimenpide-avain
                                 :ajat (into []
                                             (mapcat (fn [hoitokausi]
                                                       (let [kuukaudet (case hoitokausi
                                                                         1 (range 10 13)
                                                                         5 (range 1 10)
                                                                         (range 1 13))
                                                             vuosi (+ urakan-aloitusvuosi (dec hoitokausi))]
                                                         (map (fn [kuukausi]
                                                                {:kuukausi kuukausi
                                                                 :vuosi vuosi})
                                                              kuukaudet)))
                                                     (range 1 6)))
                                 :summa 1000})
        kokonaishintaisten-toimenpideinstanssit (flatten
                                                  (q (str "WITH toimenpide_idt AS (
                                                             SELECT id
                                                             FROM toimenpidekoodi
                                                             WHERE taso = 3 AND
                                                                   koodi IN ('20107', '20191', '23104', '23116', '23124', '14301')
                                                           )
                                                           SELECT id
                                                           FROM toimenpideinstanssi
                                                           WHERE urakka = " urakka-id " AND
                                                                 toimenpide IN (SELECT id FROM toimenpide_idt);")))
        ]
    (testing "Kiinteahintaisen työn tallentaminen merkkaa maksuerän likaiseksi"
      (let [vastaukset (doall (for [toimenpide-avain kokonaishintaisten-toimenpide-avaimet]
                                (bs/tallenna-kiinteahintaiset-tyot (:db jarjestelma) +kayttaja-jvh+ (kokonaishintainen-tyo toimenpide-avain))))
            kustannussuunnitelmat (q-map (str "SELECT ks.*, tpi.*
                                           FROM kustannussuunnitelma ks
                                             JOIN maksuera m ON m.numero = ks.maksuera
                                             JOIN toimenpideinstanssi tpi ON tpi.id = m.toimenpideinstanssi
                                           WHERE m.tyyppi = 'kokonaishintainen' AND
                                                 tpi.id IN (" (apply str (interpose ", " kokonaishintaisten-toimenpideinstanssit)) ");"))]
        (is (every? :onnistui? vastaukset) "Kiinteähintaisen työn tallentaminen epäonnistui")
        (is (= (count kokonaishintaisten-toimenpide-avaimet) (count kustannussuunnitelmat)))
        (is (every? :likainen kustannussuunnitelmat))))
    (testing "Sampokomponentin päivittäinen työ lähettää likaiseksimerkatun kustannuksen Sampoon"
      (vienti/aja-paivittainen-lahetys (:sonja jarjestelma) (:integraatioloki jarjestelma) (:db jarjestelma) (get-in asetukset [:sampo :lahetysjono-ulos]))
      (let [sonja-broker-tila (fn [jonon-nimi attribuutti]
                                (-> (s-tk/sonja-jolokia-jono jonon-nimi attribuutti nil) :body (cheshire/decode) (get "value")))
            viestit-jonossa (- (sonja-broker-tila (get-in asetukset [:sampo :lahetysjono-ulos])
                                                  :enqueue-count)
                               (sonja-broker-tila (get-in asetukset [:sampo :lahetysjono-ulos])
                                                  :dequeue-count))]
        (is (= (count kokonaishintaisten-toimenpide-avaimet) viestit-jonossa))))))
