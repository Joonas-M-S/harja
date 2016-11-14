(ns harja.tiedot.palaute
  (:require [clojure.string :as string]
            [harja.ui.ikonit :as ikonit]
            [clojure.string :as str]
            [reagent.core :refer [atom]]
            [harja.tiedot.istunto :as istunto]
            [harja.asiakas.tapahtumat :as t]
            [harja.pvm :as pvm]))

(def sahkoposti-kehitystiimi "harjapalaute@solita.fi")
(def sahkoposti-paakayttaja "harjapalaute@solita.fi")

(defn- mailto-kehitystiimi []
  (str "mailto:" sahkoposti-kehitystiimi))

(defn- mailto-paakayttaja []
  (str "mailto:" sahkoposti-paakayttaja))

;; Huomaa että rivinvaihto tulee mukaan tekstiin
(def palaute-otsikko
  "")

(def virhe-otsikko
  "")

(defn tekniset-tiedot
  ([kayttaja url user-agent] (tekniset-tiedot kayttaja url user-agent nil))
  ([kayttaja url user-agent palautetyyppi]
   (let [enc #(.encodeURIComponent js/window %)]
     (str "\n---\n"
          (when palautetyyppi (str "Palautetyyppi: " palautetyyppi "\n"))
          "Sijainti Harjassa: " (enc url) "\n"
          "Aika: " (pvm/pvm-aika-opt (pvm/nyt)) "\n"
          "User agent: " (enc user-agent) "\n"
          "Käyttäjä: " (enc (pr-str kayttaja))))))

(defn palaute-body []
  (str "Kerro meille mitä yritit tehdä, ja millaiseen ongelmaan törmäsit. Harkitse kuvakaappauksen "
       "mukaan liittämistä, ne ovat meille erittäin hyödyllisiä. "
       "Ota kuvakaappaukseen mukaan koko selainikkuna."))

(defn virhe-body [virheviesti]
  (str
    "\n---\n"
    "Kirjoita ylle, mitä olit tekemässä, kun virhe tuli vastaan. Kuvakaappaukset ovat meille myös "
    "hyvä apu. Ethän pyyhi alla olevia virheen teknisiä tietoja pois."
    "\n---\nTekniset tiedot:\n"
    virheviesti))

(defn- ilman-valimerkkeja [str]
  (-> str
      (string/replace " " "%20")
      (string/replace "\n" "%0A")))

(defn- lisaa-kentta
  ([kentta pohja lisays] (lisaa-kentta kentta pohja lisays "&"))
  ([kentta pohja lisays valimerkki] (if (empty? lisays)
                                      pohja
                                      (str pohja valimerkki kentta (ilman-valimerkkeja lisays)))))

(defn- subject
  ([pohja lisays] (lisaa-kentta "subject=" pohja lisays))
  ([pohja lisays valimerkki] (lisaa-kentta "subject=" pohja lisays valimerkki)))

(defn- body
  ([pohja lisays] (lisaa-kentta "body=" pohja lisays))
  ([pohja lisays valimerkki] (lisaa-kentta "body=" pohja lisays valimerkki)))

(defn mailto-linkki
  ([vastaanottaja sisalto] (mailto-linkki vastaanottaja sisalto nil))
  ([vastaanottaja sisalto palautetyyppi]
   (-> vastaanottaja
       (subject palaute-otsikko "?")
       (body (str
               sisalto
               (tekniset-tiedot
                 @istunto/kayttaja
                 (-> js/window .-location .-href)
                 (-> js/window .-navigator .-userAgent)
                 palautetyyppi))
             (if-not (empty? palaute-otsikko) "&" "?")))))