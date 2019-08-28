(ns harja.ui.taulukko.kaytokset
  (:require [harja.loki :as loki]))

(defn numero-re
  ([] (numero-re {}))
  ([{kokonaisosan-maara :kokonaisosan-maara desimaalien-maara :desimaalien-maara
     positiivinen? :positiivinen? kokonaisluku? :kokonaisluku?
     :or {kokonaisosan-maara 10 desimaalien-maara 10 positiivinen? false kokonaisluku? false}}]
   (str (when-not positiivinen? "-?")
        "\\d{1," kokonaisosan-maara "}"
        (when-not kokonaisluku? (str "((\\.|,)\\d{0," desimaalien-maara "})?")))))

(defn positiivinen-numero-re
  ([] (positiivinen-numero-re {}))
  ([asetukset]
   (numero-re (assoc asetukset :positiivinen? true))))

(defmulti lisaa-kaytos
          (fn [kaytos _]
            (cond
              (map? kaytos) (first (keys kaytos))
              (keyword? kaytos) kaytos
              :else nil)))

(defmethod lisaa-kaytos :eventin-arvo
  [_ toiminto]
  (comp toiminto
        (fn [event]
          (.. event -target -value))))

(defmethod lisaa-kaytos :positiivinen-numero
  [_ toiminto]
  (comp toiminto
        (fn [arvo]
          (let [positiivinen-arvo? (re-matches (re-pattern (positiivinen-numero-re)) arvo)]
            (when (or (= "" arvo) positiivinen-arvo?)
              arvo)))))

(defmethod lisaa-kaytos :str->int
  [_ toiminto]
  (comp toiminto
        (fn [arvo]
          (when (re-matches (re-pattern (numero-re)) arvo)
            (js/parseInt arvo)))))

(defmethod lisaa-kaytos :str->number
  [_ toiminto]
  (comp toiminto
        (fn [arvo]
          (when (re-matches (re-pattern (numero-re)) arvo)
            (js/Number arvo)))))

(defmethod lisaa-kaytos :default
  [kaytos toiminto]
  (loki/warn "KAYTOSTÄ: " kaytos " EI OLE MÄÄRITETTY!")
  toiminto)