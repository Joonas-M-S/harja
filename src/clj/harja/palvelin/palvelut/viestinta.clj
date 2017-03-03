(ns harja.palvelin.palvelut.viestinta
  "Yleisiä apufunktioita domain-asioihin liittyvään sähköposti- ja tekstiviestiviestintään.

   Domain-asioiden tarkemmat viestinvälitysfunktiot löytyvät asioiden omista namespaceista, ks. esim:
   - harja.palvelin.palvelut.laadunseuranta.viestinta
   - harja.palvelin.palvelut.yllapitokohteet.viestinta"
  (:require [taoensso.timbre :as log]
            [clojure.core.match :refer [match]]
            [harja.palvelin.integraatiot.sahkoposti :as sahkoposti]
            [hiccup.core :refer [html]]
            [harja.palvelin.komponentit.fim :as fim])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn laheta-sposti-fim-kayttajarooleille
  "Yrittää lähettää sähköpostin annetun urakan FIM-käyttäjille, jotka ovat
   annetussa roolissa. Jos viestin lähetys epäonnistuu, logittaa virheen.

   Parametrit:
   fim                    FIM-komponentti
   email                  Sähköpostikomponentti (sonja-sahkoposti)
   urakka-sampoid         Sen urakan sampo-id, jonka käyttäjiä etsitään FIMistä
   fim-kayttajaroolit     Setti rooleja, joissa oleville henkilöille viesti lähetetään. Huomioi kirjoitusasu!
                            Esim. #{\"urakan vastuuhenkilö\"}
   viesti-otsikko         Sähköpostiviestin otsikko, johon lisätään prefix 'Harja: '
   viesti-body            Sähköpostiviestin body"
  [{:keys [fim email urakka-sampoid fim-kayttajaroolit viesti-otsikko viesti-body]}]
  (log/debug (format "Lähetetään sähköposti FIM-käyttäjille %s. Aihe: %s" fim-kayttajaroolit viesti-otsikko))
  (try+
    (let [viestin-saajat (fim/hae-urakan-kayttajat-jotka-roolissa fim urakka-sampoid fim-kayttajaroolit)]
      (if (empty? viestin-saajat)
        (log/warn (format "Urakalle %s ei löydy FIM:stä yhtään henkiöä, jolle lähettää sähköposti." urakka-sampoid))
        (doseq [henkilo viestin-saajat]
          (try+
            (sahkoposti/laheta-viesti!
              email
              (sahkoposti/vastausosoite email)
              (:sahkoposti henkilo)
              (str "Harja: " viesti-otsikko)
              viesti-body)
            (catch Object e
              (log/error (format "Sähköpostin lähetys FIM-käyttäjälle %s epäonnistui: %s"
                                 (pr-str henkilo) (when (instance? Throwable e) (.printStackTrace e)))))))))
    (catch Object e
      (log/error (format "Sähköpostia ei voitu lähettää urakan %s FIM-käyttäjille %s. Virhe: %s"
                         urakka-sampoid fim-kayttajaroolit (when (instance? Throwable e) (.printStackTrace e)))))))