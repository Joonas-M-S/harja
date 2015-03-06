(ns harja.palvelin.main
  (:require

   ;; Yleiset palvelinkomponenti
   [harja.palvelin.komponentit.tietokanta :as tietokanta]
   [harja.palvelin.komponentit.http-palvelin :as http-palvelin]
   [harja.palvelin.komponentit.todennus :as todennus]
   
   ;; Harjan bisneslogiikkapalvelut
   [harja.palvelin.palvelut.kayttajatiedot :as kayttajatiedot]
   [harja.palvelin.palvelut.urakoitsijat :as urakoitsijat]
   [harja.palvelin.palvelut.hallintayksikot :as hallintayksikot]
   [harja.palvelin.palvelut.indeksit :as indeksit]
   [harja.palvelin.palvelut.urakat :as urakat]
   [harja.palvelin.palvelut.yksikkohintaiset-tyot :as yksikkohintaiset-tyot]
   [harja.palvelin.palvelut.toimenpidekoodit :as toimenpidekoodit]
   [harja.palvelin.palvelut.yhteyshenkilot]
   [harja.palvelin.palvelut.kayttajat :as kayttajat]
   
   [com.stuartsierra.component :as component]
   [harja.palvelin.asetukset :refer [lue-asetukset konfiguroi-lokitus]]

   [clojure.tools.namespace.repl :refer [refresh]])
  (:gen-class))

(defn luo-jarjestelma [asetukset]
  (let [{:keys [tietokanta http-palvelin kehitysmoodi]} asetukset]
    (component/system-map
     :db (tietokanta/luo-tietokanta (:palvelin tietokanta)
                                    (:portti tietokanta)
                                   (:tietokanta tietokanta)
                                   (:kayttaja tietokanta)
                                    (:salasana tietokanta))
     :todennus (component/using
                (if false ;; kehitysmoodi
                  (todennus/feikki-http-todennus {:etunimi "Tero" :sukunimi "Toripolliisi" :id 1 :kayttajanimi "LX123456789"})
                  (todennus/http-todennus))
                [:db])
     :http-palvelin (component/using
                     (http-palvelin/luo-http-palvelin (:portti http-palvelin)
                                                      kehitysmoodi)
                     [:todennus])

     ;; Frontille tarjottavat palvelut 
     :kayttajatiedot (component/using
                      (kayttajatiedot/->Kayttajatiedot)
                      [:http-palvelin])
     :urakoitsijat (component/using
                       (urakoitsijat/->Urakoitsijat)
                       [:http-palvelin :db])
     :hallintayksikot (component/using
                       (hallintayksikot/->Hallintayksikot)
                       [:http-palvelin :db])
     :indeksit (component/using
                       (indeksit/->Indeksit)
                       [:http-palvelin :db])
     :urakat (component/using
              (urakat/->Urakat)
              [:http-palvelin :db])
     :yksikkohintaiset-tyot (component/using
              (yksikkohintaiset-tyot/->Yksikkohintaiset-tyot)
              [:http-palvelin :db])
     :yhteyshenkilot (component/using
                      (harja.palvelin.palvelut.yhteyshenkilot/->Yhteyshenkilot)
                      [:http-palvelin :db])
     :toimenpidekoodit (component/using
                        (toimenpidekoodit/->Toimenpidekoodit)
                        [:http-palvelin :db])
     :kayttajat (component/using
                 (kayttajat/->Kayttajat)
                 [:http-palvelin :db])
     )))

(defonce harja-jarjestelma nil)

(defn -main [& argumentit]
  (alter-var-root #'harja-jarjestelma
                  (constantly
                   (-> (lue-asetukset (or (first argumentit) "asetukset.edn"))
                       luo-jarjestelma
                       component/start)))
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (component/stop harja-jarjestelma)))))

(defn dev-start []
  (alter-var-root #'harja-jarjestelma component/start))

(defn dev-stop []
  (when harja-jarjestelma
    (alter-var-root #'harja-jarjestelma component/stop)))

(defn dev-restart []
  (dev-stop)
  (-main))

(defn dev-refresh []
  (dev-stop)
  (clojure.tools.namespace.repl/set-refresh-dirs "src/clj")
  (refresh :after 'harja.palvelin.main/-main))

(defn dev-julkaise
  "REPL käyttöön: julkaise uusi palvelu (poistaa ensin vanhan samalla nimellä)."
  [nimi fn]
  (http-palvelin/poista-palvelu (:http-palvelin harja-jarjestelma) nimi)
  (http-palvelin/julkaise-palvelu (:http-palvelin harja-jarjestelma) nimi fn))

(defn q
  "Kysele Harjan kannasta, REPL kehitystä varten"
  [& sql]
  (with-open [c (.getConnection (:datasource (:db harja-jarjestelma)))
              ps (.prepareStatement c (reduce str sql))
              rs (.executeQuery ps)]
    (let [cols (-> (.getMetaData rs) .getColumnCount)]
      (loop [res []
             more? (.next rs)]
        (if-not more?
          res
          (recur (conj res (loop [row []
                                  i 1]
                             (if (<= i cols)
                               (recur (conj row (.getObject rs i)) (inc i))
                               row)))
                 (.next rs)))))))

(defn u
  "UPDATE Harjan kantaan"
  [& sql]
  (with-open [c (.getConnection (:datasource (:db harja-jarjestelma)))
              ps (.prepareStatement c (reduce str sql))]
    (.executeUpdate ps)))

