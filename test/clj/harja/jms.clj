(ns harja.jms
  "JMS testit: hornetq"
  (:require [harja.palvelin.komponentit.sonja :as sonja]
            [clojure.core.async :refer [<! go] :as async]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log])
  (:import (javax.jms Message Session TextMessage)))

(defrecord FeikkiSonja [kuuntelijat viesti-id]
  component/Lifecycle
  (start [this]
    (log/info "Feikki Sonja käynnistetty")
    this)
  (stop [this]
    (log/info "Feikki Sonja lopetettu")
    this)

  sonja/Sonja
  (kuuntele [_ nimi kuuntelija]
    (swap! kuuntelijat
           update-in [nimi]
           (fn [vanhat-kuuntelijat]
             (if (nil? vanhat-kuuntelijat)
               #{kuuntelija}
               (conj vanhat-kuuntelijat kuuntelija))))
    #(swap! kuuntelijat update-in [nimi] disj kuuntelija))

  (laheta [_ nimi viesti]
    (log/info "Feikki Sonja lähettää jonoon: " nimi)
    (let [msg (sonja/luo-viesti viesti (reify javax.jms.Session
                                         (createTextMessage [this]
                                           (let [txt (atom nil)
                                                 id (str "ID:" (swap! viesti-id inc))]
                                             (reify TextMessage
                                               (getJMSMessageID [_] id)
                                               (setText [_ t] (reset! txt t))
                                               (getText [_] @txt))))))]
      (go (<! (async/timeout 100)) ;; sadan millisekunnin päästä lähetys
          (doseq [k (get @kuuntelijat nimi)]
            (k msg)))
      (.getJMSMessageID msg))))


(defn feikki-sonja []
  (->FeikkiSonja (atom nil) (atom 0)))

        
      
    
