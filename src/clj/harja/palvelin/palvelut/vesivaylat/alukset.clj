(ns harja.palvelin.palvelut.vesivaylat.alukset
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [clojure.core.async :as async]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.palvelin.palvelut.pois-kytketyt-ominaisuudet :refer [ominaisuus-kaytossa?]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.roolit :as roolit]
            [harja.geo :as geo]
            [harja.transit :as transit]
            [harja.pvm :as pvm]
            [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.vesivaylat.alukset :as alukset-q]
            [harja.domain.vesivaylat.alus :as alus]
            [harja.domain.urakka :as urakka]
            [harja.domain.muokkaustiedot :as m]
            [harja.domain.organisaatio :as organisaatio]
            [namespacefy.core :refer [namespacefy]]
            [specql.core :as specql]
            [specql.op :as op]
            [namespacefy.core :as namespacefy]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.set :as set]))

(defn hae-urakan-alukset [db user tiedot]
  ;; TODO Oikeustarkistus
  (namespacefy
    (alukset-q/hae-urakan-alukset db {:urakka (::urakka/id tiedot)})
    {:ns :harja.domain.vesivaylat.alus
     :custom {:lisatiedot ::alus/urakan-aluksen-kayton-lisatiedot}}))

(defn hae-urakoitsijan-alukset [db user tiedot]
  ;; TODO Oikeustarkistus
  (namespacefy/namespacefy
    (alukset-q/hae-urakoitsijan-alukset db {:urakoitsija (::organisaatio/id tiedot)})
    {:ns :harja.domain.vesivaylat.alus}))

(defn hae-kaikki-alukset [db user]
  ;; TODO Oikeustarkistus
  (sort-by ::alus/mmsi (specql/fetch
                         db
                         ::alus/alus
                         (set/union alus/perustiedot alus/viittaukset)
                         {})))

(defn tallenna-urakan-alukset [db user tiedot]
  ;; TODO Oikeustarkistus + testi sille
  (let [urakka-id (::urakka/id tiedot)
        alukset (::alus/urakan-tallennettavat-alukset tiedot)]
    (jdbc/with-db-transaction [db db]
      (doseq [alus alukset]
        (if (first (alukset-q/hae-urakan-alus-mmsilla db {:mmsi (::alus/mmsi alus)
                                                          :urakka urakka-id}))
          (specql/update!
            db
            ::alus/urakan-aluksen-kaytto
            {::alus/urakan-alus-mmsi (::alus/mmsi alus)
             ::alus/urakan-aluksen-kayton-lisatiedot (::alus/urakan-aluksen-kayton-lisatiedot alus)
             ::m/muokattu (c/to-sql-time (t/now))
             ::m/poistettu? (or (:poistettu alus) false)}
            {::alus/urakan-alus-mmsi (::alus/mmsi alus)})
          (specql/insert!
            db
            ::alus/urakan-aluksen-kaytto
            {::alus/urakan-alus-mmsi (::alus/mmsi alus)
             ::alus/urakka-id urakka-id
             ::m/luoja-id (:id user)
             ::m/luotu (c/to-sql-time (t/now))
             ::alus/urakan-aluksen-kayton-lisatiedot (::alus/urakan-aluksen-kayton-lisatiedot alus)})))
      (hae-urakan-alukset db user tiedot))))

(defn tallenna-alukset [db user tiedot]
  (log/debug "TODO")
  ;; TODO Toteutus
  ;; TODO Oikeustarkistus + testi sille
  )

(defn hae-alusten-reitit
  ([db user tiedot] (hae-alusten-reitit db user tiedot false))
  ([db _ tiedot pisteet?]
   (oikeudet/ei-oikeustarkistusta!)
   (if pisteet?
     (alukset-q/alusten-reitit-pisteineen db tiedot)
     (alukset-q/alusten-reitit db tiedot))))

(defrecord Alukset []
  component/Lifecycle
  (start [{http :http-palvelin
           db :db :as this}]
    (julkaise-palvelu
      http
      :hae-urakan-alukset
      (fn [user tiedot]
        (hae-urakan-alukset db user tiedot))
      {:kysely-spec ::alus/hae-urakan-alukset-kysely
       :vastaus-spec ::alus/hae-urakan-alukset-vastaus})
    (julkaise-palvelu
      http
      :hae-urakoitsijan-alukset
      (fn [user tiedot]
        (hae-urakoitsijan-alukset db user tiedot))
      {:kysely-spec ::alus/hae-urakoitsijan-alukset-kysely
       :vastaus-spec ::alus/hae-urakoitsijan-alukset-vastaus})
    (julkaise-palvelu
      http
      :hae-kaikki-alukset
      (fn [user tiedot]
        (hae-kaikki-alukset db user))
      {:kysely-spec ::alus/hae-kaikki-alukset-kysely
       :vastaus-spec ::alus/hae-kaikki-alukset-vastaus})
    (julkaise-palvelu
      http
      :tallenna-urakan-alukset
      (fn [user tiedot]
        (tallenna-urakan-alukset db user tiedot))
      {:kysely-spec ::alus/tallenna-urakan-alukset-kysely
       :vastaus-spec ::alus/hae-urakan-alukset-vastaus})
    (julkaise-palvelu
      http
      :tallenna-alukset
      (fn [user tiedot]
        (tallenna-alukset db user tiedot))
      {:kysely-spec ::alus/tallenna-alukset-kysely
       :vastaus-spec ::alus/hae-kaikki-alukset-vastaus})
    (julkaise-palvelu
      http
      :hae-alusten-reitit-pisteineen
      (fn [user tiedot]
        (hae-alusten-reitit db user tiedot true))
      {:kysely-spec ::alus/hae-alusten-reitit-pisteineen-kysely
       :vastaus-spec ::alus/hae-alusten-reitit-pisteineen-vastaus})
    (julkaise-palvelu
      http
      :hae-alusten-reitit
      (fn [user tiedot]
        (hae-alusten-reitit db user tiedot))
      {:kysely-spec ::alus/hae-alusten-reitit-kysely
       :vastaus-spec ::alus/hae-alusten-reitit-vastaus})
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-urakan-alukset
      :hae-urakoitsijan-alukset
      :hae-kaikki-alukset
      :tallenna-urakan-alukset
      :tallenna-alukset
      :hae-alusten-reitit-pisteineen
      :hae-alusten-reitit)
    this))