(ns harja.palvelin.integraatiot.tierekisteri.sanomat.tietueen-lisayskutsu
  (:require [clojure.xml :refer [parse]]
            [clojure.zip :refer [xml-zip]]
            [taoensso.timbre :as log]
            [harja.tyokalut.xml :as xml]
            [hiccup.core :refer [html]])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def +xsd-polku+ "xsd/tierekisteri/schemas/")

(defn muodosta-xml-sisalto [tietue]
  [:ns2:lisaaTietue {:xmlns:ns2 "http://www.solita.fi/harja/tierekisteri/lisaaTietue"}
   [:lisaaja
    [:henkilo (get-in tietue [:lisaaja :henkilo])]
    [:jarjestelma (get-in tietue [:lisaaja :jarjestelma])]
    [:organisaatio (get-in tietue [:lisaaja :organisaatio])]
    [:yTunnus (get-in tietue [:lisaaja :yTunnus])]]
   [:tietue
    [:tunniste (get-in tietue [:tietue :tunniste])]
    [:alkupvm (get-in tietue [:tietue :alkupvm])]
    [:loppupvm (get-in tietue [:tietue :loppupvm])]
    [:karttapvm (get-in tietue [:tietue :karttapvm])]
    [:piiri (get-in tietue [:tietue :piiri])]
    [:kuntoluokka (get-in tietue [:tietue :kuntoluokka])]
    [:urakka (get-in tietue [:tietue :urakka])]
    [:sijainti
     [:tie
      [:numero (get-in tietue [:tietue :sijainti :tie :numero])]
      [:aet (get-in tietue [:tietue :sijainti :tie :aet])]
      [:aosa (get-in tietue [:tietue :sijainti :tie :aosa])]
      [:let (get-in tietue [:tietue :sijainti :tie :let])]
      [:losa (get-in tietue [:tietue :sijainti :tie :losa])]
      [:ajr (get-in tietue [:tietue :sijainti :tie :ajr])]
      [:puoli (get-in tietue [:tietue :sijainti :tie :puoli])]
      [:alkupvm (get-in tietue [:tietue :sijainti :tie :alkupvm])]]]
    [:tietolaji
     [:tietolajitunniste (get-in tietue [:tietue :tietolaji :tietolajitunniste])]
     [:arvot (get-in tietue [:tietue :tietolaji :arvot])]]]
   [:lisatty (:lisatty tietue)]])

(defn muodosta-kutsu [tietue]
  (let [sisalto (muodosta-xml-sisalto tietue)
        xml (xml/tee-xml-sanoma sisalto)]
    (if (xml/validoi +xsd-polku+ "lisaaTietue.xsd" xml)
      xml
      (do
        (log/error "Tietueenlisäyspyyntöä ei voida lähettää. Pyynnön XML ei ole validi.")
        (throw+
          {:type    :tietueen-lisays-epaonnistui
           :virheet [{:koodi :ei-validi-xml :viesti "Tietueen lisäyspyyntö Tierekisteriin ei ole validi"}]})))))