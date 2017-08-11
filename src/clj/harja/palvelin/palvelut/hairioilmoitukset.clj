(ns harja.palvelin.palvelut.hairioilmoitukset
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [clojure.core.async :as async]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.palvelin.palvelut.pois-kytketyt-ominaisuudet :refer [ominaisuus-kaytossa?]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.muokkaustiedot :as muok]
            [harja.domain.hairioilmoitus :as hairio]
            [specql.core :as specql]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(defn- hae-kaikki-hairioilmoitukset [db user]
  ;; TODO OIKEUSCHECK
  (specql/fetch db ::hairio/hairioilmoitus
                hairio/sarakkeet
                {}))

(defn- hae-tuorein-voimassaoleva-hairioilmoitus [db user]
  ;; TODO OIKEUSCHECK
  {:hairioilmoitus (-> (hae-kaikki-hairioilmoitukset db user)
                       (hairio/tuorein-voimassaoleva-hairio))})

(defn- aseta-kaikki-hairioilmoitukset-pois [db user]
  ;; TODO OIKEUSCHECK
  (specql/update! db ::hairio/hairioilmoitus
                  {::hairio/voimassa? false}
                  {::hairio/voimassa? true})
  (hae-kaikki-hairioilmoitukset db user))

(defn- aseta-hairioilmoitus [db user tiedot]
  ;; TODO OIKEUSCHECK
  (aseta-kaikki-hairioilmoitukset-pois db user)
  (specql/insert! db ::hairio/hairioilmoitus
                  {::hairio/viesti (::hairio/viesti tiedot)
                   ::hairio/pvm (c/to-sql-date  (t/now))
                   ::hairio/voimassa? true})
  (hae-kaikki-hairioilmoitukset db user))

(defrecord Hairioilmoitukset []
  component/Lifecycle
  (start [{http :http-palvelin
           db :db :as this}]
    (julkaise-palvelu
      http
      :hae-hairioilmoitukset
      (fn [user _]
        (hae-kaikki-hairioilmoitukset db user)))

    (julkaise-palvelu
      http
      :hae-tuorein-voimassaoleva-hairioilmoitus
      (fn [user _]
        (hae-tuorein-voimassaoleva-hairioilmoitus db user)))

    (julkaise-palvelu
      http
      :aseta-hairioilmoitus
      (fn [user tiedot]
        (aseta-hairioilmoitus db user tiedot)))

    (julkaise-palvelu
      http
      :aseta-kaikki-hairioilmoitukset-pois
      (fn [user _]
        (aseta-kaikki-hairioilmoitukset-pois db user)))
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-hairioilmoitukset
      :hae-tuorein-voimassaoleva-hairioilmoitus
      :aseta-hairioilmoitus
      :aseta-kaikki-hairioilmoitukset-pois)
    this))