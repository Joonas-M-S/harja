(ns harja.palvelin.komponentit.sonja
  "Komponentti Sonja-väylän JMS-jonoihin liittymiseksi."
  (:require [com.stuartsierra.component :as component]
            [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [cheshire.core :as cheshire]
            [clojure.core.async :refer [go-loop go <! >! thread >!! timeout] :as async]
            [taoensso.timbre :as log]
            [hiccup.core :refer [html]]
            [clojure.string :as str]
            [harja.pvm :as pvm]
            [harja.kyselyt.harjatila :as q]
            [harja.fmt :as fmt])
  (:import (javax.jms Session ExceptionListener JMSException MessageListener)
           (java.lang.reflect Proxy InvocationHandler)
           (java.net InetAddress))
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def JMS-alkutila
  {:yhteys nil :jonot {} :saikeiden-maara nil})

(def ei-jms-yhteytta {:type :jms-yhteysvirhe
                      :virheet [{:koodi :ei-yhteytta
                                 :viesti "Sonja yhteyttä ei saatu. Viestiä ei voida lähettää."}]})
(defprotocol LuoViesti
  (luo-viesti [x istunto]))

(extend-protocol LuoViesti
  String
  (luo-viesti [s istunto]
    (doto (.createTextMessage istunto)
      (.setText s)))
  ;; Luodaan multipart viesti
  clojure.lang.PersistentArrayMap
  (luo-viesti [{:keys [xml-viesti pdf-liite]} istunto]
    (if (and xml-viesti pdf-liite)
      (let [mm (.createMultipartMessage istunto)
            viesti-osio (.createMessagePart mm (luo-viesti xml-viesti istunto))
            liite-osio (.createMessagePart mm (doto (.createBytesMessage istunto)
                                                (.writeBytes pdf-liite)))]
        (doto mm
          (.addPart viesti-osio)
          (.addPart liite-osio)))
      (throw+ {:type :puutteelliset-multipart-parametrit
               :virheet [(if xml-viesti
                           "XML viesti annettu"
                           {:koodi :ei-xml-viestia
                            :viesti "XML-viestiä ei annettu"})
                         (if pdf-liite
                           "PDF liite annettu"
                           {:koodi :ei-pdf-liitetta
                            :viest "PDF-liitettä ei annettu"})]}))))

(defprotocol Sonja
  (kuuntele! [this jonon-nimi kuuntelija-fn]
    "Lisää uuden kuuntelijan annetulle jonolle. Jos jonolla on monta kuuntelijaa,
    viestit välitetään jokaiselle kuuntelijalle.
    Kuuntelijafunktiolle annetaan suoraan javax.jms.Message objekti.
    Kuuntelija blokkaa käsittelyn ajan, joten samasta jonosta voidaan lukea vain yksi viesti kerrallaan.
    Kuuntelija ei saa luoda uutta säijettä, koska AUTO_ACKNOWLEDGE on päällä. Tämä tarkoittaa sitä, että jos viestin
    käsittely epäonnistuu uudessa säikeessä ja varsinainen consumer on lopettanut jo hommansa, niin viesti on jo poistettu
    jonosta.")

  (laheta [this jono viesti] [this jono viesti otsikot]
    "Lähettää viestin nimettyyn jonoon. Palauttaa message id:n.")

  (aloita-yhteys [this]
    "Aloita sonja yhteys"))

(def jms-driver-luokka {:activemq "org.apache.activemq.ActiveMQConnectionFactory"
                        :sonicmq "progress.message.jclient.QueueConnectionFactory"})

(defmacro exception-wrapper [olio metodi]
  `(when ~olio
     (try (. ~olio ~metodi)
          "ACTIVE"
          ~(list 'catch 'javax.jms.IllegalStateException 'e
            "CLOSED"))))

(defn hae-jonon-viestit [istunto jono]
  (when (and istunto jono)
    (let [selailija (.createBrowser istunto jono)]
      (try (loop [viestit (.getEnumeration selailija)
                  viesti-idt []]
             (if (.hasMoreElements viestit)
               (recur viestit
                      (conj viesti-idt (->> viestit .nextElement .getJMSMessageID)))
               viesti-idt))
           (catch JMSException e
             nil)
           (finally
             (when selailija
               (.close selailija)))))))

(defn aloita-sonja-yhteyden-tarkkailu [JMS-oliot tyyppi db]
  (go-loop []
    (let [{yhteys :yhteys jonot :jonot} @JMS-oliot
          yhteyden-tila (when yhteys
                          (if (= tyyppi :activemq)
                            (if (.isClosed yhteys)
                              "CLOSED"
                              "ACTIVE")
                            (.getConnectionState yhteys)))
          olioiden-tilat {:yhteyden-tila yhteyden-tila
                          :istunnot (mapv (fn [[jonon-nimi {:keys [istunto jono tuottaja vastaanottaja]}]]
                                            ;; SonicMQ API:n avulla ei voi tarkistella suoraan onko sessio, vastaanottaja tai tuottaja sulettu,
                                            ;; joten täytyy yrittää käyttää sitä objektia johonkin ja katsoa nakataanko IllegalStateException
                                            (let [istunnon-tila (exception-wrapper istunto getAcknowledgeMode)
                                                  jonon-viestit (hae-jonon-viestit istunto jono)
                                                  _ (if (not (empty? jonon-viestit))
                                                      (println "AJFOAJFOAJFAO: " jonon-viestit))
                                                  tuottajan-tila (exception-wrapper tuottaja getDeliveryMode)
                                                  vastaanottajan-tila (exception-wrapper vastaanottaja getMessageListener)]
                                              {:istunnon-tila istunnon-tila
                                               :jono {jonon-nimi {:jonon-viestit jonon-viestit
                                                                  :tuottajat (when tuottaja
                                                                               {:tuottajan-tila tuottajan-tila})
                                                                  :vastaanottajat (when vastaanottaja
                                                                                    {:vastaanottajan-tila vastaanottajan-tila})}}}))
                                          jonot)}]
      (q/tallenna-sonjan-tila<! db {:tila (cheshire/encode olioiden-tilat)
                                    :palvelin (fmt/leikkaa-merkkijono 512
                                                                      (.toString (InetAddress/getLocalHost)))
                                    :osa-alue "sonja"}))
    (<! (timeout 5000))
    (recur)))

(declare aloita-yhdistaminen
         yhdista-kuuntelija
         tee-jms-poikkeuskuuntelija)

(defn- poista-consumerit [{jonot :jonot :as tila}]
  (reduce (fn [tila jono]
            (assoc-in tila [:jonot jono :consumer] nil))
          tila
          (keys jonot)))



#_(defn- luodaanko-uusi-sonja-yhteys? [kulunut-aika {:keys [yhteys jonot] :as tila} {:keys [tyyppi]}]
  (let [yhteyden-tila (when (= :sonicmq tyyppi)
                        (.getConnectionState yhteys))
        #_#_jonojen-tila (when )]
    (if (> kulunut-aika 120)
      true
      )))

#_(defn yhdista-uudelleen [{:keys [yhteys jonot] :as tila} JMS-oliot asetukset yhteys-ok?]
  (let [katkos-alkoi (pvm/nyt-suomessa)
        nukkumisaika 5000]
    (log/info "Yritetään yhdistään JMS-yhteys uudelleen. Katkos alkoi: " (str katkos-alkoi))
    (loop [kulunut-aika (pvm/aikavali-sekuntteina katkos-alkoi (pvm/nyt-suomessa))
           kaynnistetaan-uudestaan? (luodaanko-uusi-sonja-yhteys? kulunut-aika @JMS-oliot asetukset)]
      (if kaynnistetaan-uudestaan?
        (do
          (when yhteys
            (try
              (.close yhteys)
              (catch Exception e
                (log/error e "JMS-yhteyden sulkemisessa tapahtui poikkeus: " (.getMessage e)))))
          (loop [tila (aloita-yhdistaminen (poista-consumerit tila) asetukset (tee-jms-poikkeuskuuntelija JMS-oliot asetukset yhteys-ok?) yhteys-ok?)
                 [[jonon-nimi kuuntelija] & kuuntelijat]
                 (mapcat (fn [[jonon-nimi {kuuntelijat :kuuntelijat}]]
                           (log/info (format "Yhdistetään uudestaan kuuntelijat jonoon: %s" jonon-nimi))
                           (map (fn [k] [jonon-nimi k]) @kuuntelijat))
                         jonot)]
            (if (not jonon-nimi)
              tila
              (recur (yhdista-kuuntelija tila jonon-nimi kuuntelija)
                     kuuntelijat))))
        (do (Thread/sleep nukkumisaika)
            (recur (pvm/aikavali-sekuntteina katkos-alkoi (pvm/nyt-suomessa))
                   (luodaanko-uusi-sonja-yhteys? kulunut-aika @JMS-oliot asetukset)))))))

(defn tee-sonic-jms-tilamuutoskuuntelija []
  (let [lokita-tila #(case %
                      0 (log/info "Sonja JMS yhteyden tila: ACTIVE")
                      1 (log/info "Sonja JMS yhteyden tila: RECONNECTING")
                      2 (log/error "Sonja JMS yhteyden tila: FAILED")
                      3 (log/info "Sonja JMS yhteyden tila: CLOSED"))
        kasittelija (reify InvocationHandler (invoke [_ _ _ args] (lokita-tila (first args))))
        luokka (Class/forName "progress.message.jclient.ConnectionStateChangeListener")
        instanssi (Proxy/newProxyInstance (.getClassLoader luokka) (into-array Class [luokka]) kasittelija)]
    instanssi))

(defn tee-jms-poikkeuskuuntelija [JMS-oliot asetukset yhteys-ok?]
  (reify ExceptionListener
    (onException [_ e]
      (log/error e (str "Tapahtui JMS-poikkeus: " (.getMessage e)))

      #_(send-off JMS-oliot yhdista-uudelleen JMS-oliot asetukset yhteys-ok?))))

(defn konfiguroi-sonic-jms-connection-factory [connection-factory]
  (doto connection-factory
    (.setFaultTolerant true)
    (.setFaultTolerantReconnectTimeout (int 300))))

(defn- luo-connection-factory [url tyyppi]
  (let [connection-factory (-> tyyppi
                               jms-driver-luokka
                               Class/forName
                               (.getConstructor (into-array Class [String]))
                               (.newInstance (into-array Object [url])))]
    (if (= tyyppi :activemq)
      connection-factory
      (konfiguroi-sonic-jms-connection-factory connection-factory))))

(defn- yhdista [{:keys [kayttaja salasana tyyppi] :as asetukset} poikkeuskuuntelija qcf aika]
  (try
    (let [yhteys (.createQueueConnection qcf kayttaja salasana)]
      (when (= tyyppi :sonicmq)
        (.setConnectionStateChangeListener yhteys (tee-sonic-jms-tilamuutoskuuntelija)))
      (.setExceptionListener yhteys poikkeuskuuntelija)
      yhteys)
    (catch JMSException e
      (log/warn (format "Ei saatu yhteyttä Sonjan JMS-brokeriin. Yritetään uudestaan %s millisekunnin päästä. " aika) e)
      (Thread/sleep aika)
      (yhdista asetukset poikkeuskuuntelija qcf (min (* 2 aika) 600000)))
    (catch Exception e
      (log/error "JMS brokeriin yhdistäminen epäonnistui: " e)
      nil)))

(defn aloita-yhdistaminen [{:keys [url tyyppi] :as asetukset} poikkeuskuuntelija]
  (log/info "Yhdistetään " (if (= tyyppi :activemq) "ActiveMQ" "Sonic") " JMS-brokeriin URL:lla:" url)
  (let [qcf (luo-connection-factory url tyyppi)
        yhteys (yhdista asetukset poikkeuskuuntelija qcf 10000)]
    (when (= :sonicmq tyyppi)
      (log/info "Yhteyden metadata: " (when-let [meta-data (.getMetaData yhteys)]
                                        meta-data)))
    (when yhteys
      (do
        (log/info "Saatiin yhteys Sonjan JMS-brokeriin.")
        {:yhteys yhteys :qcf qcf}))))

(defn kasittele-viesti [vastaanottaja kuuntelijat]
  (.setMessageListener vastaanottaja
                       (with-meta
                         (reify MessageListener
                           (onMessage [_ message]
                             (doseq [kuuntelija kuuntelijat]
                               (try
                                 (kuuntelija message)
                                 (catch Throwable t
                                   (log/warn (str "Jonoon " (-> vastaanottaja .getQueue .getQueueName) " tuli viesti "
                                                  (if (instance? javax.jms.TextMessage message)
                                                    (.getText message)
                                                    message)
                                                  " ja sen käsittely epäonnistui funktiolta " kuuntelija))
                                   (throw t))))))
                         {:kuuntelijoiden-maara (count kuuntelijat)})))

(defn poista-kuuntelija [tila jonon-nimi kuuntelija-fn]
  (let [{:keys [istunto kuuntelijat vastaanottaja jono] :as jonon-tiedot} (get-in @tila [:jonot jonon-nimi])]
    (if (contains? kuuntelijat kuuntelija-fn)
      (let [kuuntelijat (disj kuuntelijat kuuntelija-fn)]
        (if (empty? kuuntelijat)
          (do
            (.close istunto)
            (swap! tila assoc-in [:jonot jonon-nimi] nil))
          (do
            ;; Jos viestiä käsitellään, kun vaihdetaan messageListeneriä, niin sen seuraukset ovat määrittelemättömät.
            ;; Sen takia ensin sammutetaan nykyinen kuuntelija, koska se ensin käsittelee käsitteilä olevat viestit.
            ;; Tämän jälkeen luodaan uusi vastaanottaja.
            (.close vastaanottaja)
            (let [vastaanottaja (.createReceiver istunto jono)]
              (kasittele-viesti vastaanottaja kuuntelijat)
              (swap! tila update-in [:jonot jonon-nimi]
                     (fn [jonon-tiedot]
                       (assoc jonon-tiedot :vastaanottaja vastaanottaja
                                           :kuuntelijat kuuntelijat)))))))
      jonon-tiedot)))

(defn luo-istunto [yhteys]
  (.createQueueSession yhteys false Session/AUTO_ACKNOWLEDGE))

(defn yhdista-kuuntelija [tila jonon-nimi kuuntelijat]
  (log/debug (format "Yhdistetään kuuntelija jonoon: %s. Tila: %s." jonon-nimi tila))
  (let [{:keys [yhteys]} @tila
        istunto (luo-istunto yhteys)
        jono (.createQueue istunto jonon-nimi)
        vastaanottaja (.createReceiver istunto jono)]
    (kasittele-viesti vastaanottaja kuuntelijat)
    (swap! tila update-in [:jonot jonon-nimi]
           (fn [jonon-tila]
             (assoc jonon-tila
               :istunto istunto
               :jono jono
               :vastaanottaja vastaanottaja)))))

(defn varmista-jms-objektit [tila jonon-nimi]
  (let [{jonot :jonot yhteys :yhteys} @tila
        {:keys [istunto jono tuottaja]} (get jonot jonon-nimi)]
    ;Käytetään swap! vaan, jos on tarvis
    (when-not (and istunto jono tuottaja)
      (swap! tila update-in [:jonot jonon-nimi]
             (fn [{:keys [istunto jono tuottaja] :as jonon-tiedot}]
               (let [istunto (or istunto (luo-istunto yhteys))
                     jono (or jono (.createQueue istunto jonon-nimi))
                     tuottaja (or tuottaja (.createProducer istunto jono))]
                 (assoc jonon-tiedot
                   :istunto istunto
                   :jono jono
                   :tuottaja tuottaja)))))))

(defn laheta-viesti [{yhteys :yhteys jonot :jonot} jonon-nimi viesti correlation-id]
  (if yhteys
    (try
      (let [{:keys [istunto tuottaja]} (get jonot jonon-nimi)
            msg (luo-viesti viesti istunto)]
        (log/debug "Lähetetään JMS viesti ID:llä " (.getJMSMessageID msg))
        (when correlation-id
          (.setJMSCorrelationID msg correlation-id))
        (.send tuottaja msg)
        (.getJMSMessageID msg))
      (catch Exception e
        (log/error e "Virhe JMS-viestin lähettämisessä jonoon: " jonon-nimi)))
    (throw+ ei-jms-yhteytta)))

(defrecord SonjaYhteys [asetukset tila yhteys-ok? yhteys-future]
  component/Lifecycle
  (start [{db :db
           :as this}]
    (let [JMS-oliot (atom JMS-alkutila)
          yhteys-ok? (atom false)
          poikkeus-kuuntelija (tee-jms-poikkeuskuuntelija JMS-oliot asetukset yhteys-ok?)
          yhteys-future (future
                          (aloita-yhdistaminen asetukset poikkeus-kuuntelija))]
      (assoc this
        :tila JMS-oliot
        :yhteys-ok? yhteys-ok?
        :yhteys-future yhteys-future
        :yhteyden-tiedot (aloita-sonja-yhteyden-tarkkailu JMS-oliot (:tyyppi asetukset) db))))

  (stop [this]
    (when @yhteys-ok?
      (let [tila @tila]
        (some-> tila :yhteys .close)))
    (assoc this
      :tila nil))

  Sonja
  (kuuntele! [this jonon-nimi kuuntelija-fn]
    (if (some? jonon-nimi)
      (do
        (swap! (:tila this) update-in [:jonot jonon-nimi]
               (fn [{kuuntelijat :kuuntelijat}]
                 {:kuuntelijat (conj (or kuuntelijat #{}) kuuntelija-fn)}))
        #(poista-kuuntelija (:tila this) jonon-nimi kuuntelija-fn))
      (do
        (log/warn "jonon nimeä ei annettu, JMS-jonon kuuntelijaa ei käynnistetä")
        (constantly nil))))

  (laheta [this jonon-nimi viesti {:keys [correlation-id]}]
    (if-not @yhteys-ok?
      (throw+ ei-jms-yhteytta)
      (do
        (varmista-jms-objektit tila jonon-nimi)
        (laheta-viesti (-> this :tila deref) jonon-nimi viesti correlation-id))))

  (laheta [this jonon-nimi viesti]
    (laheta this jonon-nimi viesti nil))

  (aloita-yhteys [{:keys [tila yhteys-ok? yhteys-future] :as this}]
    ;; Ensin varmistetaan, että yhteys sonjaan on saatu. futuren dereffaaminen blokkaa, kunnes saadaan
    ;; joku arvo pihalle.
    (let [yhteys-olio @yhteys-future]
      (swap! tila merge yhteys-olio)
      ;; Alustetaan jvm oliot
      (doseq [[jonon-nimi jonon-tila] (:jonot @tila)]
        (yhdista-kuuntelija tila jonon-nimi (:kuuntelijat jonon-tila)))
      ;; Aloita yhteys
      (.start (:yhteys @tila))
      (reset! yhteys-ok? true))))

(defn luo-oikea-sonja [asetukset]
  (->SonjaYhteys asetukset nil nil nil))

(defn luo-feikki-sonja []
  (reify
    component/Lifecycle
    (start [this] this)
    (stop [this] this)

    Sonja
    (kuuntele! [this jonon-nimi kuuntelija-fn]
      (log/debug "Feikki Sonja, aloita muka kuuntelu jonossa: " jonon-nimi)
      (constantly nil))
    (laheta [this jonon-nimi viesti otsikot]
      (log/debug "Feikki Sonja, lähetä muka viesti jonoon: " jonon-nimi)
      (str "ID:" (System/currentTimeMillis)))
    (laheta [this jonon-nimi viesti]
      (laheta this jonon-nimi viesti nil))
    (aloita-yhteys [this]
      (log/debug "Feikki Sonja, aloita muka yhteys"))))

(defn luo-sonja [asetukset]
  (if (and asetukset (not (str/blank? (:url asetukset))))
    (luo-oikea-sonja asetukset)
    (luo-feikki-sonja)))
