(ns harja.palvelin.integraatiot.sahke.sanomat.urakkasanoma
  (:require [harja.tyokalut.xml :as xml]
            [harja.pvm :as pvm]))

(defn urakka-hiccup [{:keys [id
                             alkupvm
                             loppupvm
                             alueurakkanumero
                             nimi
                             hanke-id]}
                     viesti-id]
  [:Sampo2harja
   {:xmlns:xsi "http://www.w3.org/2001/XMLSchema-instance"
    :xsi:noNamespaceSchemaLocation "SampToharja.xsd"}
   [:Project
    {:id (str "HAR-" id)
     :financialDepartmentHash "-"
     :schedule_finish (xml/formatoi-aikaleima loppupvm)
     :name nimi
     :vv_alueurakkanro alueurakkanumero
     :resourceId "-"
     :schedule_start (xml/formatoi-aikaleima alkupvm)
     :message_Id viesti-id
     :programId (str "HAR-" hanke-id)
     :vv_transferred_harja (xml/formatoi-aikaleima (pvm/nyt))}
    [:documentLinks]]])

(defn muodosta [urakka viesti-id]
  (let [xml (xml/tee-xml-sanoma (urakka-hiccup urakka viesti-id))]
    (when (not (xml/validi-xml? "xsd/sampo/inbound/" "Sampo2Harja.xsd" xml))
      (throw (new RuntimeException
                  "Sähkeeseen lähetettävä XML-sanoma ei ole XSD-skeeman Sampo2Harja.xsd mukaan validi.")))
    xml))
