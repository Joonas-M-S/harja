(ns harja.palvelin.palvelut.vesivaylat.kiintiot
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [clojure.core.async :as async]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.palvelin.palvelut.pois-kytketyt-ominaisuudet :refer [ominaisuus-kaytossa?]]
            [harja.domain.oikeudet :as oikeudet]

            [harja.domain.urakka :as ur]
            [harja.domain.roolit :as roolit]
            [harja.domain.vesivaylat.kiintio :as kiintio]
            [harja.geo :as geo]
            [harja.transit :as transit]
            [harja.pvm :as pvm]
            [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.vesivaylat.kiintiot :as q]))

(defn hae-kiintiot [db user tiedot]
  (when (ominaisuus-kaytossa? :vesivayla)
    (let [urakka-id (::kiintio/urakka-id tiedot)]
      (assert urakka-id "Urakka-id puuttuu!")
      (oikeudet/ei-oikeustarkistusta!)
      (oikeudet/vaadi-lukuoikeus oikeudet/urakat-vesivaylasuunnittelu-kiintiot user urakka-id)
      (q/hae-kiintiot db tiedot))))

(defn tallenna-kiintiot [db user tiedot]
  (when (ominaisuus-kaytossa? :vesivayla)
    (let [urakka-id (::kiintio/urakka-id tiedot)]
      (assert urakka-id "Urakka-id puuttuu!")
      (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-vesivaylasuunnittelu-kiintiot user urakka-id)
      (q/vaadi-kiintiot-kuuluvat-urakkaan! db
                                           (keep ::kiintio/id (::kiintio/tallennettavat-kiintiot tiedot))
                                           urakka-id)
      (q/tallenna-kiintiot! db user tiedot))))

(defrecord Kiintiot []
  component/Lifecycle
  (start [{http :http-palvelin
           db :db :as this}]
    (julkaise-palvelu
      http
      :hae-kiintiot
      (fn [user tiedot]
        (hae-kiintiot db user tiedot))
      {:kysely-spec ::kiintio/hae-kiintiot-kysely
       :vastaus-spec ::kiintio/hae-kiintiot-vastaus})

    (julkaise-palvelu
      http
      :tallenna-kiintiot
      (fn [user tiedot]
       (tallenna-kiintiot db user tiedot))
      {:kysely-spec ::kiintio/tallenna-kiintiot-kysely
       :vastaus-spec ::kiintio/tallenna-kiintiot-vastaus})

    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-kiintiot
      :tallenna-kiintiot)
    this))