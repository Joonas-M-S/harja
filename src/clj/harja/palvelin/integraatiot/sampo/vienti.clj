(ns harja.palvelin.integraatiot.sampo.vienti
  (:require [taoensso.timbre :as log]
            [hiccup.core :refer [html]]
            [clj-time.core :as t]
            [harja.kyselyt.maksuerat :as qm]
            [harja.kyselyt.kustannussuunnitelmat :as qk]
            [harja.palvelin.integraatiot.sampo.sanomat.kuittaus-samposta-sanoma :as kuittaus-sampoon-sanoma]
            [harja.palvelin.integraatiot.sampo.kasittely.maksuerat :as maksuera]
            [harja.palvelin.integraatiot.sampo.kasittely.kustannussuunnitelmat :as kustannussuunnitelma]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]))

(def +xsd-polku+ "xsd/sampo/outbound/")

(defn kasittele-kuittaus [integraatioloki db viesti jono]
  (log/debug "Vastaanotettiin Sampon kuittausjonosta viesti: " viesti)
  (let [kuittaus-xml (.getText viesti)]
    ;; Validointia ei tehdä, koska jostain syystä Sampon itsensä lähettämät kuittaukset eivät mene läpi validoinnista
    ;; (if (xml/validi-xml? +xsd-polku+ "status.xsd" kuittaus-xml)
    (let [kuittaus (kuittaus-sampoon-sanoma/lue-kuittaus kuittaus-xml)
          onnistunut (not (contains? kuittaus :virhe))]
      (log/debug "Luettiin kuittaus: " kuittaus)
      (if-let [viesti-id (:viesti-id kuittaus)]
        (let [lahetystyyppi (if (= :maksuera (:viesti-tyyppi kuittaus))
                              "maksuera-lähetys"
                              "kustannussuunnitelma-lahetys")]
          (integraatioloki/kirjaa-saapunut-jms-kuittaus
            integraatioloki
            kuittaus-xml
            viesti-id
            lahetystyyppi
            onnistunut jono)
          (if (= :maksuera (:viesti-tyyppi kuittaus))
            (maksuera/kasittele-maksuera-kuittaus db kuittaus viesti-id)
            (kustannussuunnitelma/kasittele-kustannussuunnitelma-kuittaus db kuittaus viesti-id)))
        (log/error "Sampon kuittauksesta ei voitu hakea viesti-id:tä.")))
    #_(log/error "Samposta vastaanotettu kuittaus ei ole validia XML:ää.")))

(defn aja-paivittainen-lahetys [sonja integraatioloki db lahetysjono-ulos]
  (log/debug "Maksuerien päivittäinen lähetys käynnistetty: " (t/now))
  (let [maksuerat (qm/hae-likaiset-maksuerat db)
        kustannussuunnitelmat (qk/hae-likaiset-kustannussuunnitelmat db)
        urakkaidt (distinct (map :urakkaid maksuerat))
        urakoiden-summat (group-by :urakka-id
                                   (mapcat #(qm/hae-urakan-maksuerien-summat db %) urakkaidt))]
    (log/debug "Lähetetään " (count maksuerat) " maksuerää ja " (count kustannussuunnitelmat) " kustannussuunnitelmaa.")
    (doseq [{maksuera-numero :numero urakkaid :urakkaid} maksuerat]
      (let [summat (urakoiden-summat urakkaid)
            _ (log/debug " ---> summat: " (pr-str summat))]
        (maksuera/laheta-maksuera sonja integraatioloki db lahetysjono-ulos maksuera-numero summat)))
    (doseq [kustannussuunnitelma kustannussuunnitelmat]
      (kustannussuunnitelma/laheta-kustannussuunitelma sonja integraatioloki db lahetysjono-ulos (:maksuera kustannussuunnitelma)))))
