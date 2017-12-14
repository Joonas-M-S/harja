(ns harja.palvelin.palvelut.kanavat.hairiotilanteet
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.kanavat.hairiotilanne :as hairio]
            [harja.domain.kanavat.kohde :as kohde]
            [harja.domain.kanavat.kohteenosa :as osa]
            [harja.tyokalut.tietoturva :as tietoturva]
            [harja.domain.vesivaylat.materiaali :as materiaali]
            [harja.domain.muokkaustiedot :as muok]
            [harja.kyselyt.vesivaylat.materiaalit :as m-q]
            [clojure.java.jdbc :as jdbc]
            [harja.palvelin.palvelut.vesivaylat.materiaalit :as materiaali-palvelu]
            [harja.kyselyt.kanavat.kanavan-hairiotilanne :as q-hairiotilanne]))

(defn hae-hairiotilanteet [db kayttaja hakuehdot]
  (let [urakka-id (::hairio/urakka-id hakuehdot)]
    (assert urakka-id "Häiriötilanteiden hakua ei voi tehdä ilman urakka id:tä")
    (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laadunseuranta-hairiotilanteet kayttaja urakka-id)
    (reverse (sort-by ::hairio/havaintoaika (q-hairiotilanne/hae-sopimuksen-hairiotilanteet-aikavalilta db hakuehdot)))))

(defn tallenna-hairiotilanne [db fim email
                              {kayttaja-id :id :as kayttaja}
                              {urakka-id ::hairio/urakka-id :as hairiotilanne}
                              materiaalikirjaukset
                              materiaalipoistot]
  (assert urakka-id "Häiriötilannetta ei voi tallentaa ilman urakka id:tä")
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laadunseuranta-hairiotilanteet kayttaja urakka-id)
  ;; Häiriötilanne kuuluu urakkaan
  (tietoturva/vaadi-linkitys db ::hairio/hairiotilanne ::hairio/id (::hairio/id hairiotilanne)
                             ::hairio/urakka-id urakka-id)
  ;; Häiriötilanteen kohde kuuluu urakkaan
  (when (::hairio/kohde-id hairiotilanne)
    (tietoturva/vaadi-ainakin-yksi-linkitys db ::kohde/kohde<->urakka ::kohde/kohde-id (::hairio/kohde-id hairiotilanne)
                                            ::kohde/urakka-id urakka-id))
  ;; Häiriötilanteen kohdeosa kuuluu kohteeseen
  (when (::hairio/kohteenosa-id hairiotilanne)
    (tietoturva/vaadi-linkitys db ::osa/kohteenosa ::osa/id (::hairio/kohteenosa-id hairiotilanne)
                               ::osa/kohde-id (::hairio/kohde-id hairiotilanne)))
  (jdbc/with-db-transaction [db db]
    (let [{hairio-id ::hairio/id} (q-hairiotilanne/tallenna-hairiotilanne db kayttaja-id hairiotilanne)
          hairio-id (or hairio-id (::hairio/id hairiotilanne))]
      (doseq [mk (map #(assoc % ::materiaali/hairiotilanne hairio-id) materiaalikirjaukset)]
        (m-q/kirjaa-materiaali db kayttaja mk)
        (materiaali-palvelu/hoida-halytysraja db mk fim email))
      (doseq [mk materiaalipoistot]
        (m-q/poista-materiaalikirjaus db kayttaja (::materiaali/id mk))))))

(defrecord Hairiotilanteet []
  component/Lifecycle
  (start [{http :http-palvelin
           db :db
           fim :fim
           email :sonja-sahkoposti :as this}]
    (julkaise-palvelu
      http
      :hae-hairiotilanteet
      (fn [kayttaja hakuehdot]
        (hae-hairiotilanteet db kayttaja hakuehdot))
      {:kysely-spec ::hairio/hae-hairiotilanteet-kysely
       :vastaus-spec ::hairio/hae-hairiotilanteet-vastaus})

    (julkaise-palvelu
      http
      :tallenna-hairiotilanne
      (fn [kayttaja {hairiotilanne ::hairio/hairiotilanne
                     materiaalikirjaukset ::materiaali/materiaalikirjaukset
                     materiaalipoistot ::materiaali/poista-materiaalikirjauksia
                     hakuehdot ::hairio/hae-hairiotilanteet-kysely}]
        (jdbc/with-db-transaction [db db]
          (tallenna-hairiotilanne db fim email kayttaja hairiotilanne materiaalikirjaukset materiaalipoistot)
          {:hairiotilanteet (hae-hairiotilanteet db kayttaja hakuehdot)
           :materiaalilistaukset (m-q/hae-materiaalilistaus db {::materiaali/urakka-id (::hairio/urakka-id hairiotilanne)})}))
      {:kysely-spec ::hairio/tallenna-hairiotilanne-kutsu
       :vastaus-spec ::hairio/tallenna-hairiotilanne-vastaus})
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-hairiotilanteet)
    this))
