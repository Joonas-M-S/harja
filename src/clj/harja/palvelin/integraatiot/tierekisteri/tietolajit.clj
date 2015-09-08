(ns harja.palvelin.integraatiot.tierekisteri.tietolajit
  (:require [taoensso.timbre :as log]
            [clojure.string :as string]
            [harja.palvelin.integraatiot.tierekisteri.sanomat.tietolajin-hakukutsu :as kutsusanoma]
            [harja.palvelin.integraatiot.tierekisteri.sanomat.vastaus :as vastaussanoma]
            [harja.palvelin.integraatiot.integraatiopisteet.http :as http])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn validoi-tunniste [tunniste]
  (when (not
          (contains? #{"tl523" "tl501" "tl517" "tl507" "tl508" "tl506" "tl522" "tl513" "tl196" "tl519" "tl505" "tl195"
                       "tl504" "tl198" "tl518" "tl514" "tl509" "tl515" "tl503" "tl510" "tl512" "tl165" "tl516" "tl511"}
                     tunniste))
    (throw+ {:type :tierekisteri-kutsu-epaonnistui :error (str "Tietolajia ei voida hakea. Tuntematon tietolaji: " tunniste)})))

(defn kasittele-virheet [url tunniste muutospvm virheet]
  (throw+ {:type  :tierekisteri-kutsu-epaonnistui
           :error (str "Tietolajin haku epäonnistui (URL: " url ") tunnisteella: " tunniste
                       " & muutospäivämäärällä: " muutospvm "."
                       "Virheet: " (string/join virheet))}))

(defn kirjaa-varoitukset [url tunniste muutospvm virheet]
  (log/warn (str "Tietolajin haku palautti virheitä (URL: " url ") tunnisteella: " tunniste
                 " & muutospäivämäärällä: " muutospvm "."
                 "Virheet: " (string/join virheet))))

(defn kasittele-vastaus [url tunniste muutospvm vastausxml]
  (let [vastausdata (vastaussanoma/lue vastausxml)
        onnistunut (:onnistunut vastausdata)
        virheet (:virheet vastausdata)]
    (if (not onnistunut)
      (kasittele-virheet url tunniste muutospvm virheet)
      (do
        (when (not-empty virheet)
          (kirjaa-varoitukset url tunniste muutospvm virheet))
        vastausdata))))

(defn hae-tietolajit [integraatioloki url tunniste muutospvm]
  (validoi-tunniste tunniste)
  (log/debug "Hae tietolajin: " tunniste " ominaisuudet muutospäivämäärällä: " nil " Tierekisteristä")
  (let [kutsudata (kutsusanoma/muodosta tunniste muutospvm)
        palvelu-url (str url "/haetietolajit")
        otsikot {"Content-Type" "text/xml"}
        vastausdata (http/laheta-post-kutsu
                      integraatioloki
                      "hae-tietolaji"
                      "tierekisteri"
                      palvelu-url
                      otsikot
                      nil
                      kutsudata
                      (fn [vastaus-xml] (kasittele-vastaus palvelu-url tunniste muutospvm vastaus-xml)))]
    vastausdata))
