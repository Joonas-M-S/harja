(ns harja.tyokalut.dev-tyokalut
  "Tänne funktioita, jotka ovat hyödyllisiä devatessa, mutta ei tarvitse käyttää prodissa.
   Jos on käytetty tuota defn-tyokalu makroa, niin pitäisi olla turvallista vaikka jokin
   funktiokutsu jäisikin koodiin prodiin mentäessä"
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [clojure.pprint :as pprint]
            [harja.tyokalut.env :as env]))

(defonce ^{:private true
           :doc "Arvot, jotka haetaan environment:istä. Näiden tulisi siis olla saatavana jo käännösaikana"}
         envrionment
         {:dev? (true? (env/env "HARJA_DEV_YMPARISTO"))})

(defonce ^{:doc "Tässä pidetään runtime configuraatiota. Hyödyllinen REPL:in kanssa."}
         config
         (atom {:kirjoita-tiedostoon? true}))

(defn merge-config! [conf]
  (swap! config merge conf))

(defn- luo-arity [fn-params]
  (let [args# (first fn-params)
        pre-post?# (and (map? (second fn-params))
                        (or (contains? (second fn-params) :pre)
                            (contains? (second fn-params) :post)))
        pre-post# (when pre-post?#
                    (second fn-params))
        body# (if pre-post?#
                (drop 2 fn-params)
                (rest fn-params))
        dev-environment? (:dev? envrionment)]
    (cond-> (list args#)
            (and dev-environment? pre-post?#) (concat [pre-post#])
            dev-environment? (concat body#))))

(defmacro defn-tyokalu
  "Luo annetun funktion. Funktion body luodaan vain, jos HARJA_DEV_YMPARISTO ympäristömuuttuja on true.
   Näin turhaan ajettavaa koodia ei synny tuotantoon."
  [& fn-params]
  (let [nimi (first fn-params)
        docstring? (string? (second fn-params))
        rest-fn-params (if docstring?
                         (drop 2 fn-params)
                         (rest fn-params))
        multiarity? (list? (first rest-fn-params))
        fn-body (if multiarity?
                  (map luo-arity rest-fn-params)
                  (luo-arity rest-fn-params))]
    `(defn ~nimi
       ~@(if docstring?
           (apply list
                  (second fn-params)
                  fn-body)
           fn-body))))

(defn- str-n [n st]
  (reduce str (repeat n st)))

(defn- viesti-*out*
  "Printtaa viestit *out* kanavaan. Output on muotoa:
   ##########
   #        #
   # rivi-1 #
   # ...    #
   # rivi-n #
   #        #
   ##########"
  [& viestit]
  (let [pisin-viesti (apply max (map count viestit))
        padding 1
        aloitus-ja-lopetus-rivi (str-n (+ (* 2 padding)
                                          2
                                          pisin-viesti)
                                       "#")
        tyhja-rivi (str "#"
                        (str-n (+ (* 2 padding)
                                  pisin-viesti)
                               " ")
                        "#")
        kirjoitettavat-rivit (reduce (fn [rivit viesti]
                                       (let [tyhjat-merkit (str-n (- pisin-viesti
                                                                     (count viesti))
                                                                  " ")]
                                         (conj rivit
                                               (str "#"
                                                    (str-n padding " ")
                                                    viesti
                                                    tyhjat-merkit
                                                    (str-n padding " ")
                                                    "#"))))
                                     []
                                     viestit)
        viesti (reduce (fn [koottu-viesti viesti]
                         (str koottu-viesti "\n" viesti))
                       (concat (cons aloitus-ja-lopetus-rivi
                                     (repeat padding tyhja-rivi))
                               kirjoitettavat-rivit
                               (repeat padding tyhja-rivi)
                               [aloitus-ja-lopetus-rivi]))]
    (println viesti)))

(defn-tyokalu kirjoita-tiedostoon
  "Kirjoittaa annetun Clojure datan .edn tiedostoon dev-resources/tmp kansioon"
  ([input tiedoston-nimi] (kirjoita-tiedostoon input tiedoston-nimi true))
  ([input tiedoston-nimi ylikirjoita?]
   (async/go
     (when (:kirjoita-tiedostoon? @config)
       (let [tiedoston-polku (str "dev-resources/tmp/" tiedoston-nimi ".edn")
             tiedosto-olemassa? (.exists (io/file tiedoston-polku))]
         (if (and tiedosto-olemassa?
                  (not ylikirjoita?))
           (viesti-*out* (str "TIEDOSTO: " tiedoston-polku " on jo olemassa")
                         "Ei ylikirjoiteta")
           (try (pprint/pprint input (io/writer tiedoston-polku))
                (catch Throwable t
                  (viesti-*out* (str "Datan kirjoittaminen tiedostoon: " tiedoston-polku " epäonnistui!"))))))))))

(defn-tyokalu lue-edn-tiedosto
  "Ottaa .edn tiedostopolun ja palauttaa Clojure datan.
   HUOM! .edn tiedosto luetaan muistiin, joten tiedoston sisätlämä
   datastruktuuri ei saisi olla aivan valtava."
  [tiedosto]
  (-> tiedosto slurp read-string))