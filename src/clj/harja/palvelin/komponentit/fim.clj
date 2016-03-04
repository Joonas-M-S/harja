(ns harja.palvelin.komponentit.fim
  "Komponentti FIM käyttäjätietojen hakemiseen."
  (:require [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [clojure.data.zip.xml :as z]
            [com.stuartsierra.component :as component]
            [harja.palvelin.integraatiot.integraatiopisteet.http :as http]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

;; Kentät, joita voidaan hakea:
;; ObjectID EmployeeEndDate
;; AccountName FirstName MiddleName LastName
;; JobTitle Company OfficePhone OfficeLocation Toimiala Department Yksikko
;; MobilePhone Email City roomnumber Manager KayttonimiPaivystysnumero

(def +fim-elementit+
  "Mäppäys FIM elementeistä suomenkielisiin avaimiin ja mahdollisiin prosessointeihin"
  {:ObjectID    :tunniste
   :AccountName :kayttajatunnus
   :FirstName   :etunimi
   :LastName    :sukunimi
   :Email       :sahkoposti
   :MobilePhone [:puhelin #(str/replace % " " "")]
   :Company     :organisaatio})

(defn lue-fim-vastaus
  "Lukee FIM REST vastaus annetusta XML zipperistä. Palauttaa sekvenssin käyttäjä mäppejä."
  [xml]
  (z/xml-> xml
           :person
           (fn [p]
             (into {}
                   (map (fn [[elementti avain]]
                          (if (vector? avain)
                            (let [[avain muunnos] avain]
                              [avain (z/xml1-> p elementti z/text muunnos)])
                            [avain (z/xml1-> p elementti z/text)])))
                   +fim-elementit+))))

(defn lue-xml [bytet]
  (xml-zip (parse (java.io.ByteArrayInputStream. bytet))))


(defn hae-kayttajatunnus
  "Hakee FIM palvelusta käyttäjätunnuksella."
  [fim kayttajatunnus integraatioloki]
  (http/laheta-get-kutsu
    integraatioloki
    "tuo-fim-kayttaja"
    "fim"
    (:url fim)
    nil
    {:filterproperty "AccountName"
     :filter         kayttajatunnus
     :fetch          "AccountName,FirstName,LastName,Email,MobilePhone,Company"}
    (fn [body _]
      (first (lue-fim-vastaus (lue-xml body))))))


(defrecord FIM [url]
  component/Lifecycle
  (start [this]
    this)

  (stop [this]
    this))