(ns harja.ui.lomake
  "Lomakeapureita"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.kentat :refer [tee-kentta atomina]]))

(defmulti kentan-otsikko (fn [luokka kentan-nimi teksti] luokka))

(defmethod kentan-otsikko :horizontal [_ kentan-nimi teksti]
  [:label.col-sm-2.control-label {:for kentan-nimi}
   teksti])

(defmethod kentan-otsikko :default [_ kentan-nimi teksti]
  [:label {:for kentan-nimi} teksti])

(defmulti kentan-komponentti (fn [luokka komponentti] luokka))

(defmethod kentan-komponentti :horizontal [_ komponentti]
  [:div.col-sm-10
   komponentti])

(defmethod kentan-komponentti :default [_ komponentti]
  komponentti)


(defn lomake [{:keys [muokkaa! luokka] :as opts} kentat data]
  (let [luokka (or luokka :default)]
    [:form {:class (case luokka
                     :inline "form-inline"
                     :horizontal "form-horizontal"
                     :default "")}
     (for [{:keys [muokattava? fmt hae nimi] :as kentta} kentat]
       ^{:key (:nimi kentta)}
       [:div.form-group
        [kentan-otsikko luokka (name nimi) (:otsikko kentta)]

        [kentan-komponentti luokka
         (if-let [komponentti (:komponentti kentta)]
           komponentti
           (if (or (nil? muokattava?)
                   (muokattava? data))
             ;; Muokattava tieto, tehdään sille kenttä
             [tee-kentta (assoc kentta :lomake? true)
              (atomina kentta data muokkaa!)]

             ;; Ei muokattava, näytetään
             [:div.form-control-static
              ((or fmt str) ((or hae #(get % nimi)) data))]))]])]))
  
