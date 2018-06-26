(ns harja.palvelin.integraatiot.sonja.sahkoposti
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.integraatiot.integraatiopisteet.jms :as jms]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.palvelin.integraatiot.sonja.sahkoposti.sanomat :as sanomat]
            [harja.kyselyt.integraatiot :as q]
            [harja.tyokalut.xml :as xml]
            [harja.palvelin.integraatiot.sahkoposti :refer [Sahkoposti]]
            [taoensso.timbre :as log])
  (:import (java.util UUID)))

(defn- lokittaja [{il :integraatioloki db :db} nimi]
  (integraatioloki/lokittaja il db "sonja" nimi))

(defn- tee-vastaanottokuuntelija [{:keys [db sonja] :as this} sahkoposti-sisaan-jono kuuntelijat]
  (if (not (empty? sahkoposti-sisaan-jono))
    (jms/kuuntele (lokittaja this "sahkoposti-vastaanotto")
                  sonja
                  sahkoposti-sisaan-jono
                  sanomat/lue-sahkoposti
                  #(try
                     (doseq [kuuntelija @kuuntelijat]
                       (kuuntelija %))
                     (catch Exception e
                       (log/error e "Sähköpostin vastaanotossa tapahtui poikkeus"))))
    (constantly nil)))

(defn- tee-lahetyksen-kuittauskuuntelija [{:keys [db sonja] :as this} sahkoposti-ulos-kuittausjono jono-tunniste]
  (if (not (empty? sahkoposti-ulos-kuittausjono))
    (let [integraatiotunniste (case jono-tunniste
                                 :sahkoposti "sahkoposti-lahetys"
                                 :sahkoposti-ja-liite "sahkoposti-ja-liite-lahetys")
          integraatio (q/integraation-id db "sonja" integraatiotunniste)]
      (jms/kuittausjonokuuntelija (lokittaja this integraatiotunniste) sonja sahkoposti-ulos-kuittausjono
                                  sanomat/lue-kuittaus :viesti-id :onnistunut
                                  (fn [viesti viesti-id onnistunut]
                                    (q/kuittaa-integraatiotapahtuma! db onnistunut "" integraatio viesti-id))))
    (constantly nil)))

(defrecord SonjaSahkoposti [vastausosoite jonot kuuntelijat]
  component/Lifecycle
  (start [{sonja :sonja :as this}]
    (assoc this
      :saapuva (tee-vastaanottokuuntelija this (:sahkoposti-sisaan-jono jonot) kuuntelijat)
      :lahteva (tee-lahetyksen-kuittauskuuntelija this (:sahkoposti-ulos-kuittausjono jonot) :sahkoposti)
      :jms-lahettaja (jms/jonolahettaja (lokittaja this "sahkoposti-lahetys")
                                        sonja
                                        (:sahkoposti-ulos-jono jonot))
      :jms-lahettaja-sahkoposti-ja-liite (jms/jonolahettaja (lokittaja this "sahkoposti-ja-liite-lahetys")
                                                            sonja
                                                            (:sahkoposti-ja-liite-ulos-jono jonot))
      :lahteva-sahkoposti-ja-liite-kuittauskuuntelija (tee-lahetyksen-kuittauskuuntelija this (:sahkoposti-ja-liite-ulos-kuittausjono jonot) :sahkoposti-ja-liite)))

  (stop [this]
    ((:saapuva this))
    ((:lahteva this))
    (dissoc this :saapuva :lahteva))

  Sahkoposti
  (rekisteroi-kuuntelija! [this kuuntelija-fn]
    (swap! kuuntelijat conj kuuntelija-fn)
    #(swap! kuuntelijat disj kuuntelija-fn))

  (laheta-viesti! [{jms-lahettaja :jms-lahettaja} lahettaja vastaanottaja otsikko sisalto]
    (let [viesti-id (str (UUID/randomUUID))
          sahkoposti (sanomat/sahkoposti viesti-id lahettaja vastaanottaja otsikko sisalto)
          viesti (xml/tee-xml-sanoma sahkoposti)]
      (jms-lahettaja viesti viesti-id)))

  (laheta-viesti-ja-liite! [{jms-lahettaja :jms-lahettaja-sahkoposti-ja-liite} lahettaja vastaanottaja otsikko sisalto]
    (let [viesti-id (str (UUID/randomUUID))
          sahkoposti (sanomat/sahkoposti viesti-id lahettaja vastaanottaja otsikko (:viesti sisalto))
          viesti (xml/tee-xml-sanoma sahkoposti)]
      (jms-lahettaja {:xml-viesti viesti :pdf-liite (:pdf-liite sisalto)} viesti-id)))

  (vastausosoite [this]
    vastausosoite))

(defn luo-sahkoposti [vastausosoite jonot]
  (->SonjaSahkoposti vastausosoite jonot (atom #{})))
