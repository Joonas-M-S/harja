(ns harja.asiakas.ymparisto
  "Dev ympäristön spesifisiä asioita."
  (:require
   ;;[lively.core :as lively]
   [figwheel.client :as fw]

   ;; require kaikki testit
   [cljs.test :as test]
   [harja.app-test]))


(defn ^:export aja-testit []
  (test/run-tests 'harja.app-test))

(defn alusta
  "Alusta tämän ympäristön vaatimat asiat, Lively reload."
  [options]
  (.log js/console "Alustetaan koodin uudelleenlataus")
  ;;(lively/start "/js/harja.js"
  ;;              {:polling-rate 1000
  ;;               :on-reload (fn []
  ;;                           (.log js/console "Koodia ladattu uudelleen.")
  ;;                            (when-let [on-reload (:on-reload options)]
  ;;                              (on-reload)))})
  (fw/start {:websocket-url   "ws://localhost:3449/figwheel-ws"
             :on-jsload (fn [] (.log js/console "Koodia ladattu uudelleen")
                          (when-let [on-reload (:on-reload options)]
                            (on-reload)
                            (aja-testit)
                            ))})
  
  (.log js/console "Alustetaan less.js uudelleenlataus")
  (let [less (aget js/window "less")
        logger (aget less "logger")]
    (aset logger "info" (fn [& args])))
  (js/setInterval #(let [less (aget js/window "less")
                         refresh (aget less "refresh")]
                     (try
                       (refresh true)
                       (catch js/Object o
                         (.log js/console "Virhe Less päivityksessä: " o)))) 
                  5000))



