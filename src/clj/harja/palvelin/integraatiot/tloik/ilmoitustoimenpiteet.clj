(ns harja.palvelin.integraatiot.tloik.ilmoitustoimenpiteet
  (:require [taoensso.timbre :as log]
            [harja.kyselyt.ilmoitukset :as ilmoitukset]
            [harja.kyselyt.konversio :as konversio]
            [harja.palvelin.integraatiot.tloik.sanomat.ilmoitustoimenpide-sanoma :as toimenpide-sanoma]
            [harja.palvelin.tyokalut.lukot :as lukko]
            [harja.pvm :as pvm])
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:import (java.util UUID)))

(defn laheta [jms-lahettaja db id]
  (let [viesti-id (str (UUID/randomUUID))
        data (konversio/alaviiva->rakenne (first (ilmoitukset/hae-ilmoitustoimenpide db id)))
        xml (toimenpide-sanoma/muodosta data viesti-id)]
    (if xml
      (do
        (jms-lahettaja xml viesti-id)
        (ilmoitukset/merkitse-ilmoitustoimenpide-odottamaan-vastausta! db viesti-id id)
        (log/debug (format "Ilmoitustoimenpiteen (id: %s) lähetys T-LOIK:n onnistui." id)))
      (do
        (log/error (format "Ilmoitustoimenpiteen (id: %s) lähetys T-LOIK:n epäonnistui." id))
        (ilmoitukset/merkitse-ilmoitustoimenpidelle-lahetysvirhe! db id)))))

(defn laheta-ilmoitustoimenpide [jms-lahettaja db id]
  (log/debug (format "Lähetetään ilmoitustoimenpide (id: %s) T-LOIK:n." id))
  (try
    (lukko/aja-lukon-kanssa db "tloik-ilm.toimenpidelahetys" (fn [] (laheta jms-lahettaja db id)))
    (catch Exception e
      (log/error e (format "Ilmoitustoimenpiteen (id: %s) lähetyksessä T-LOIK:n tapahtui poikkeus." id))
      (ilmoitukset/merkitse-ilmoitustoimenpidelle-lahetysvirhe! db id)
      (throw e))))

(defn laheta-lahettamattomat-ilmoitustoimenpiteet [jms-lahettaja db]
  (lukko/yrita-ajaa-lukon-kanssa
    db
    "tloik-kuittausten-uudelleenlahetys"
    #(do
      (log/debug "Lähetetään lähettämättömät ilmoitustoimenpiteet T-LOIK:n.")
      (let [idt (mapv :id (ilmoitukset/hae-lahettamattomat-ilmoitustoimenpiteet db))]
        (doseq [id idt]
          (try
            (laheta-ilmoitustoimenpide jms-lahettaja db id)
            (catch Exception _))))
      (log/debug "Ilmoitustoimenpiteiden lähetys T-LOIK:n valmis."))))

(defn vastaanota-kuittaus [db viesti-id onnistunut]
  (if onnistunut
    (do
      (log/debug (format "Ilmoitustoimenpide kuitattiin T-LOIK:sta onnistuneeksi viesti-id:llä: %s" viesti-id))
      (ilmoitukset/merkitse-ilmoitustoimenpide-lahetetyksi! db viesti-id))

    (do
      (log/error (format "Ilmoitustoimenpide kuitattiin T-LOIK:sta epäonnistuneeksi viesti-id:llä: %s" viesti-id))
      (ilmoitukset/merkitse-ilmoitustoimenpidelle-lahetysvirhe! db viesti-id))))

(defn tallenna-ilmoitustoimenpide [db ilmoitus ilmoitusid vapaateksti toimenpide paivystaja]
  (:id (ilmoitukset/luo-ilmoitustoimenpide<!
         db
         {:ilmoitus ilmoitus
          :ilmoitusid ilmoitusid
          :kuitattu (pvm/nyt)
          :vakiofraasi nil
          :vapaateksti vapaateksti
          :kuittaustyyppi toimenpide
          :kuittaaja_henkilo_etunimi (:etunimi paivystaja)
          :kuittaaja_henkilo_sukunimi (:sukunimi paivystaja)
          :kuittaaja_henkilo_tyopuhelin (:tyopuhelin paivystaja)
          :kuittaaja_henkilo_matkapuhelin (:matkapuhelin paivystaja)
          :kuittaaja_henkilo_sahkoposti (:sahkoposti paivystaja)
          :kuittaaja_organisaatio_nimi (:nimi paivystaja)
          :kuittaaja_organisaatio_ytunnus (:ytunnus paivystaja)
          :kasittelija_henkilo_etunimi nil
          :kasittelija_henkilo_sukunimi nil
          :kasittelija_henkilo_matkapuhelin nil
          :kasittelija_henkilo_tyopuhelin nil
          :kasittelija_henkilo_sahkoposti nil
          :kasittelija_organisaatio_nimi nil
          :kasittelija_organisaatio_ytunnus nil})))
