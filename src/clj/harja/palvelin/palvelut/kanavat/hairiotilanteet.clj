(ns harja.palvelin.palvelut.kanavat.hairiotilanteet
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.kanavat.hairiotilanne :as hairio]
            [harja.domain.kanavat.kohde :as kohde]
            [harja.domain.kanavat.kohteenosa :as osa]
            [harja.tyokalut.tietoturva :as tietoturva]
            [harja.kyselyt.kanavat.kanavan-hairiotilanne :as q-hairiotilanne]))

(defn hae-hairiotilanteet [db kayttaja hakuehdot]
  (let [urakka-id (::hairio/urakka-id hakuehdot)]
    (assert urakka-id "Häiriötilanteiden hakua ei voi tehdä ilman urakka id:tä")
    (oikeudet/vaadi-lukuoikeus oikeudet/urakat-laadunseuranta-hairiotilanteet kayttaja urakka-id)
    (reverse (sort-by ::hairio/pvm (q-hairiotilanne/hae-sopimuksen-hairiotilanteet-aikavalilta db hakuehdot)))))

(defn tallenna-hairiotilanne [db {kayttaja-id :id :as kayttaja} {urakka-id ::hairio/urakka-id :as hairiotilanne}]
  (assert urakka-id "Häiriötilannetta ei voi tallentaa ilman urakka id:tä")
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-laadunseuranta-hairiotilanteet kayttaja urakka-id)
  (tietoturva/tarkista-linkitys db ::hairio/hairiotilanne ::hairio/id (::hairio/id hairiotilanne)
                                ::hairio/urakka-id urakka-id)
  (tietoturva/tarkista-linkitys db ::osa/kohteenosa ::osa/id (::hairio/kohteenosa-id hairiotilanne)
                                ::osa/kohde-id (::hairio/kohde-id hairiotilanne))
  (q-hairiotilanne/tallenna-hairiotilanne db kayttaja-id hairiotilanne))

(defrecord Hairiotilanteet []
  component/Lifecycle
  (start [{http :http-palvelin
           db :db :as this}]
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
                     hakuehdot ::hairio/hae-hairiotilanteet-kysely}]
        (tallenna-hairiotilanne db kayttaja hairiotilanne)
        (hae-hairiotilanteet db kayttaja hakuehdot))
      {:kysely-spec ::hairio/tallenna-hairiotilanne-kutsu
       :vastaus-spec ::hairio/hae-hairiotilanteet-vastaus})
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-hairiotilanteet)
    this))
