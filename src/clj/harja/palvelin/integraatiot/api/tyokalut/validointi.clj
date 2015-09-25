(ns harja.palvelin.integraatiot.api.tyokalut.validointi
  "Yleisiä API-kutsuihin liittyviä apufunktioita"
  (:require [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet]
            [harja.kyselyt.urakat :as q]
            [taoensso.timbre :as log]
            [harja.kyselyt.kayttajat :as kayttajat]
            [harja.domain.roolit :as roolit])
  (:use [slingshot.slingshot :only [throw+]]))

(defn tarkista-urakka [db urakkaid]
  (log/debug "Validoidaan urakkaa id:llä" urakkaid)
  (when (not (q/onko-olemassa? db urakkaid))
    (do
      (log/warn "Urakkaa id:llä " urakkaid " ei löydy.")
      (throw+ {:type    virheet/+sisainen-kasittelyvirhe+
               :virheet [{:koodi  virheet/+tuntematon-urakka-koodi+
                          :viesti (str "Urakkaa id:llä " urakkaid " ei löydy.")}]}))))

(defn tarkista-kayttajan-oikeudet-urakkaan [db urakka-id kayttaja]
  (when-not
    (or (roolit/roolissa? kayttaja roolit/jarjestelmavastuuhenkilo)
        (kayttajat/onko-kayttaja-urakan-organisaatiossa? db urakka-id (:id kayttaja)))
    (throw+ {:type    virheet/+viallinen-kutsu+
             :virheet [{:koodi  virheet/+kayttajalla-puutteelliset-oikeudet+
                        :viesti (str "Käyttäjällä: " (:kayttajanimi kayttaja) " ei ole oikeuksia urakkaan: " urakka-id)}]})))

(defn tarkista-onko-kayttaja-organisaatiossa [db ytunnus kayttaja]
  (when-not
    (or (roolit/roolissa? kayttaja roolit/jarjestelmavastuuhenkilo)
        (kayttajat/onko-kayttaja-organisaatiossa? db ytunnus (:id kayttaja)))
    (throw+ {:type    virheet/+viallinen-kutsu+
             :virheet [{:koodi  virheet/+kayttajalla-puutteelliset-oikeudet+
                        :viesti (str "Käyttäjällä: " (:kayttajanimi kayttaja) " ei ole oikeuksia organisaatioon: " ytunnus)}]})))

(defn tarkista-onko-kayttaja-organisaation-jarjestelma [db ytunnus kayttaja]
  (tarkista-onko-kayttaja-organisaatiossa db ytunnus kayttaja)
  (when (not (:jarjestelma kayttaja))
    (throw+ {:type    virheet/+viallinen-kutsu+
             :virheet [{:koodi  virheet/+tuntematon-kayttaja-koodi+
                        :viesti (str "Käyttäjä " (:kayttajanimi kayttaja) "ei ole järjestelmä")}]})))

(defn tarkista-urakka-ja-kayttaja [db urakka-id kayttaja]
  (tarkista-urakka db urakka-id)
  (tarkista-kayttajan-oikeudet-urakkaan db urakka-id kayttaja))
