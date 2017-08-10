(ns harja.palvelin.palvelut.vesivaylat.materiaalit
  "Vesiväylien materiaaliseurannan palvelut"
  (:require [specql.core :as specql]
            [harja.palvelin.komponentit.http-palvelin :as http-palvelin]
            [com.stuartsierra.component :as component]
            [harja.domain.vesivaylat.materiaali :as m]
            [harja.domain.muokkaustiedot :as muok]
            [harja.domain.oikeudet :as oikeudet]
            [harja.id :as id]
            [taoensso.timbre :as log]))

(defn vaadi-materiaali-kuuluu-urakkaan
  [db urakka-id materiaali-id]
  "Tarkistaa, että materiaali kuuluu annettuun urakkaan. Jos ei kuulu, heittää poikkeuksen."
  (assert urakka-id "Urakka-id puuttuu")
  (when (id/id-olemassa? materiaali-id)
    (let [materiaalin-urakka-id (::m/urakka-id (first (specql/fetch db
                                                                    ::m/materiaali
                                                                    #{::m/urakka-id}
                                                                    {::m/id materiaali-id})))]
      (when (not= materiaalin-urakka-id urakka-id)
        (throw (SecurityException. (str "Materiaali " materiaali-id " ei kuulu valittuun urakkaan "
                                        urakka-id " vaan urakkaan " materiaalin-urakka-id)))))))

(defn- hae-materiaalilistaus [db user params]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-vesivayla-materiaalit user (::m/urakka-id params))
  (specql/fetch db ::m/materiaalilistaus (specql/columns ::m/materiaalilistaus) params))

(defn- kirjaa-materiaali [db user materiaali]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-vesivayla-materiaalit user
                                  (::m/urakka-id materiaali))
  (specql/insert! db ::m/materiaali
                  (muok/lisaa-muokkaustiedot materiaali ::m/id user))
  (hae-materiaalilistaus db user (select-keys materiaali #{::m/urakka-id})))

(defn- poista-materiaalikirjaus [db user materiaali]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-vesivayla-materiaalit user
                                  (::m/urakka-id materiaali))
  (vaadi-materiaali-kuuluu-urakkaan db (::m/urakka-id materiaali) (::m/id materiaali))
  (specql/update! db ::m/materiaali
                  (muok/poistotiedot user)
                  {::m/id (::m/id materiaali)})
  (hae-materiaalilistaus db user (select-keys materiaali #{::m/urakka-id})))

(defrecord Materiaalit []
  component/Lifecycle
  (start [{db :db
           http :http-palvelin :as this}]
    (http-palvelin/julkaise-palvelu http :hae-vesivayla-materiaalilistaus
                                    (fn [user haku]
                                      (hae-materiaalilistaus db user haku))
                                    {:kysely-spec ::m/materiaalilistauksen-haku
                                     :vastaus-spec ::m/materiaalilistauksen-vastaus})
    (http-palvelin/julkaise-palvelu http :kirjaa-vesivayla-materiaali
                                    (fn [user materiaali]
                                      (kirjaa-materiaali db user materiaali))
                                    {:kysely-spec ::m/materiaalikirjaus
                                     :vastaus-spec ::m/materiaalilistauksen-vastaus})
    (http-palvelin/julkaise-palvelu http :poista-materiaalikirjaus
                                    (fn [user tiedot]
                                      (poista-materiaalikirjaus db user tiedot))
                                    {:kysely-spec ::m/poista-materiaalikirjaus
                                     :vastaus-spec ::m/materiaalilistauksen-vastaus})
    this)

  (stop [this]
    (http-palvelin/poista-palvelut
      (:http-palvelin this)
      :hae-vesivayla-materiaalilistaus
      :kirjaa-vesivayla-materiaali
      :poista-materiaalikirjaus)
    this))
