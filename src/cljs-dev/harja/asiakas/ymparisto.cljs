(ns harja.asiakas.ymparisto
  "Dev ympäristön spesifisiä asioita."
  (:require [figwheel.client :as fw]
            [harja.ui.viesti :as viesti]))

(def raportoi-selainvirheet? false)

(defn alusta
  "Alusta tämän ympäristön vaatimat asiat, figwheel reload."
  [options]
  (.log js/console "Alustetaan koodin uudelleenlataus")
  (when (.-harja_testmode js/window)
    (.log js/console "E2E test mode"))

  (fw/start {:websocket-url   "ws://localhost:3449/figwheel-ws"
             ;;:debug true
             ;:reload-dependents false
             :on-jsload (fn [] (.log js/console "Koodia ladattu uudelleen")
                          (when-let [on-reload (:on-reload options)]
                            (on-reload)))})

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
