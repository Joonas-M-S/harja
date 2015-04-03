(ns harja.palvelin.palvelut.materiaalit
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.materiaalit :as q]
            [harja.palvelin.oikeudet :as oik]
            [harja.kyselyt.konversio :as konv]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

(defn hae-materiaalikoodit [db]
  (into []
        (map #(assoc % :urakkatyyppi (keyword (:urakkatyyppi %))))
        (q/hae-materiaalikoodit db)))

(defn hae-urakan-materiaalit [db user urakka-id]
  (oik/vaadi-lukuoikeus-urakkaan user urakka-id)
  (into []
        (comp (map konv/alaviiva->rakenne)
              (map #(if (:id (:pohjavesialue %))
                      %
                      (dissoc % :pohjavesialue)))
              (map #(assoc % :maara (double (:maara %)))))
        (q/hae-urakan-materiaalit db urakka-id)))

  
(defn tallenna-urakan-materiaalit [db user {:keys [urakka-id sopimus-id materiaalit]}]
  (oik/vaadi-rooli-urakassa user oik/rooli-urakanvalvoja urakka-id)
  
  (jdbc/with-db-transaction [c db]
    (let [vanhat-materiaalit (filter #(= sopimus-id (:sopimus %))
                                     (hae-urakan-materiaalit c user urakka-id))
          materiaali-avain (juxt :alkupvm :loppupvm  (comp :id :materiaali) (comp :id :pohjavesialue))
          materiaalit-kannassa (into {}
                                     (map (juxt materiaali-avain identity)
                                          vanhat-materiaalit))]

      ;; Käydään läpi poistot
      ;; Poistetaan kaikki materiaalit, joita ei enää ole
      (doseq [{:keys [alkupvm loppupvm materiaali pohjavesialue]} (filter :poistettu materiaalit)]
        (if pohjavesialue
          (q/poista-pohjavesialueen-materiaali! c (:id user)
                              urakka-id sopimus-id
                              (konv/sql-date alkupvm) (konv/sql-date loppupvm)
                              (:id materiaali) (:id pohjavesialue))
          (q/poista-materiaali! c (:id user)
                                urakka-id sopimus-id
                                (konv/sql-date alkupvm) (konv/sql-date loppupvm)
                                (:id materiaali))))

      
      
      ;; Käydään läpi frontin lähettämät uudet materiaalit
      ;; Jos materiaali on kannassa, päivitetään sen määrä tarvittaessa
      ;; Jos materiaali ei ole kannassa, syötetään se uutena
      (doseq [materiaali materiaalit
              :let [avain (materiaali-avain materiaali)]]
        (if-let [materiaali-kannassa (materiaalit-kannassa avain)]
          ;; Materiaali on jo kannassa, päivitä, jos muuttunut
          (do (log/info "TÄMÄ MATSKU ON KANNASSA: " avain)
              (if (== (:maara materiaali) (:maara materiaali-kannassa))
                (do (log/info "Ei muutosta määrään, ei päivitetä."))
                (do (log/info "Määrä muuttunut " (:maara materiaali-kannassa) " => " (:maara materiaali) ", päivitetään!")
                    (q/paivita-materiaalin-maara! c (:id user) (:maara materiaali) (:id materiaali-kannassa))
                    )))
          
          (let [{:keys [alkupvm loppupvm maara materiaali pohjavesialue]} materiaali]
            (log/info "TÄYSIN UUSI MATSKU: " alkupvm loppupvm maara materiaali pohjavesialue)
            (q/luo-materiaali<! c (konv/sql-date alkupvm) (konv/sql-date loppupvm) maara (:id materiaali) urakka-id sopimus-id
                                (:id pohjavesialue) (:id user)))))

      
            
      (hae-urakan-materiaalit c user urakka-id))))


(defrecord Materiaalit []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this)
                      :hae-materiaalikoodit
                      (fn [user]
                        (hae-materiaalikoodit (:db this))))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-urakan-materiaalit
                      (fn [user urakka-id]
                        (hae-urakan-materiaalit (:db this) user urakka-id)))
    (julkaise-palvelu (:http-palvelin this)
                      :tallenna-urakan-materiaalit
                      (fn [user tiedot]
                        (tallenna-urakan-materiaalit (:db this) user tiedot)))
                           
    this)

  (stop [this]
    (poista-palvelut (:http-palvelin this)
                     :hae-materiaalikoodit
                     :hae-urakan-materiaalit
                     :tallenna-urakan-materiaalit)
                    
    this))
