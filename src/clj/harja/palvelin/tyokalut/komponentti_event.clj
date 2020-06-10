(ns harja.palvelin.tyokalut.komponentti-event
  (:require [com.stuartsierra.component :as component]
            [clojure.core.async :as async]
            [clojure.walk :as walk]
            [clojure.spec.alpha :as s]
            [harja.palvelin.tyokalut.interfact :as i]
            [taoensso.timbre :as log]))

(s/def ::tyyppi-spec #{:perus :viimeisin})
(s/def ::event-spec (s/or :keyword keyword?
                          :string string?))

(defn- komponentti-event-parametrien-alustus []
  {:eventit (atom {})
   :perus-broadcast (i/perus-broadcast ::event)
   :viimeisin-broadcast (i/viimeisin-broadcast ::event)})

(defonce ^{:private true
           :doc "Halutaan luoda singleton KomponenttiEvent:istä, joten pakotetaan se ottamaan parametrinsa
                täältä. Tehdään tästä myös private, jotta atomin arvoja voidaan muokata vain KomponenttiEvent
                rekordin kautta."}
         komponentti-event-parametrit
         (komponentti-event-parametrien-alustus))

#_(defonce ^{:private true}
         perus-broadcast
         (let [b (i/perus-broadcast)]
           {:broadcast b
            :pub (async/pub (.jono b) ::event (constantly (async/sliding-buffer 1000)))}))

#_(defonce ^{:private true}
         viimeisin-broadcast
         (let [b (i/viimeisin-broadcast ::event)]
           {:broadcast b
            :pub (async/pub (.jono b) ::event (constantly (async/sliding-buffer 1000)))
            :cache (.cache jono)}))

(defmulti kuuntelija!
          (fn [tyyppi _ _ _]
            tyyppi))

(defmethod kuuntelija! :perus
  [_ {bc :perus-broadcast} event kuuntelija-kanava]
  (async/sub (.broadcast bc) event kuuntelija-kanava)
  (swap! (.subscribers bc) conj kuuntelija-kanava))

(defmethod kuuntelija! :viimeisin
  [_ {bc :viimeisin-broadcast} event kuuntelija-kanava]
  (println (str "VIIMEISIN SUB EVENTILLE: " event))
  (when-let [arvo (get @(.cache bc) event)]
    (println "----> VIIMEISIN ARVO: " arvo " EVENTILLE: " event)
    (async/put! kuuntelija-kanava arvo))
  (async/sub (.broadcast bc) event kuuntelija-kanava)
  (swap! (.subscribers bc) conj kuuntelija-kanava))

(defrecord KomponenttiEvent [eventit perus-broadcast viimeisin-broadcast]
  component/Lifecycle
  (start [this]
    this)
  (stop [this]
    (doseq [bc [perus-broadcast viimeisin-broadcast]]
      (doseq [sub @(.subscribers bc)]
        (async/close! sub))
      (reset! (.subscribers bc) [])
      (async/close! (.kanava bc)))
    (reset! (.cache viimeisin-broadcast) nil)
    (reset! (:eventit komponentti-event-parametrit) {})
    (alter-var-root komponentti-event-parametrit (komponentti-event-parametrien-alustus))
    this)
  i/IEvent
  (lisaa-jono [this event tyyppi]
    (swap! eventit
           (fn [eventit]
             (assoc eventit event tyyppi)))
    this)
  (lisaa-jono [this event]
    (i/lisaa-jono this event :perus))
  (eventin-kuuntelija [this event]
    (when (get @eventit event)
      (let [kuuntelija-kanava (async/chan 1000
                                          (map (fn [v]
                                                 (println (str "FOOFOFOFOFOFOFOFOFOFOFOFOF " v))
                                                 (::data v)))
                                          (fn [t]
                                            (log/error t (str "Kuuntelija kanavassa error eventille " event))))]
        (println (str "LUODAAN KUUNTELIJAN KANAVA EVENTILLE: " event))
        (kuuntelija! (get @eventit event) this event kuuntelija-kanava)
        #_(swap! eventit
               (fn [eventit]
                 (update eventit
                         event
                         kuuntelija
                         this
                         event
                         kuuntelija-kanava)))
        kuuntelija-kanava)))
  (julkaise-event [_ event data]
    (let [julkaisu-kanavan-tyyppi (get @eventit event)
          julkaisu-kanava (case julkaisu-kanavan-tyyppi
                            :perus (.kanava perus-broadcast)
                            :viimeisin (.kanava viimeisin-broadcast))]
      (if julkaisu-kanavan-tyyppi
        (do
          (println (str "--> JULKAISE KANAVAAN: " julkaisu-kanavan-tyyppi " EVENT: " event " DATA: " data))
          (let [onnistui? (boolean (async/put! julkaisu-kanava {::event event ::data data}))]
            (println (str "ONNISTUI?: " onnistui?))
            onnistui?))
        false))))

(defn komponentti-event []
  (let [{:keys [eventit perus-broadcast viimeisin-broadcast]} komponentti-event-parametrit]
    (->KomponenttiEvent eventit perus-broadcast viimeisin-broadcast)))
