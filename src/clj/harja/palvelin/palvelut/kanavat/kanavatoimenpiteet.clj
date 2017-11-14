(ns harja.palvelin.palvelut.kanavat.kanavatoimenpiteet
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.toimenpidekoodi :as toimenpidekoodi]
            [harja.domain.kanavat.kanavan-toimenpide :as kanavan-toimenpide]
            [harja.kyselyt.kanavat.kanavan-toimenpide :as q-kanavan-toimenpide]))

(defn tarkista-kutsu [user urakka-id tyyppi]
  (assert urakka-id "Urakka-id puuttuu!")
  (case tyyppi
    :kokonaishintainen (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kanavat-kokonaishintaiset user urakka-id)
    :muutos-lisatyo (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kanavat-lisatyot user urakka-id)))

(defn hae-kanavatoimenpiteet [db user {urakka-id ::kanavan-toimenpide/urakka-id
                                       sopimus-id ::kanavan-toimenpide/sopimus-id
                                       alkupvm :alkupvm
                                       loppupvm :loppupvm
                                       toimenpidekoodi ::toimenpidekoodi/id
                                       tyyppi ::kanavan-toimenpide/kanava-toimenpidetyyppi
                                       :as hakuehdot}]
  (tarkista-kutsu user urakka-id tyyppi)
  (let [tyyppi (when tyyppi (name tyyppi))]
    (q-kanavan-toimenpide/hae-sopimuksen-toimenpiteet-aikavalilta
      db
      {:urakka urakka-id
       :sopimus sopimus-id
       :alkupvm alkupvm
       :loppupvm loppupvm
       :toimenpidekoodi toimenpidekoodi
       :tyyppi tyyppi})))

(defn tallenna-kanavatoimenpide [db user urakka-id {tyyppi ::kanavan-toimenpide/kanava-toimenpidetyyppi
                                                    :as kanavatoimenpide}]
  (tarkista-kutsu user urakka-id tyyppi)
  (q-kanavan-toimenpide/tallenna-toimenpide db kanavatoimenpide))

(defrecord Kanavatoimenpiteet []
  component/Lifecycle
  (start [{http :http-palvelin db :db :as this}]
    (julkaise-palvelu
      http
      :hae-kanavatoimenpiteet
      (fn [user hakuehdot]
        (hae-kanavatoimenpiteet db user hakuehdot))
      {:kysely-spec ::kanavan-toimenpide/hae-kanavatoimenpiteet-kysely
       :vastaus-spec ::kanavan-toimenpide/hae-kanavatoimenpiteet-vastaus})

    (julkaise-palvelu
      http
      :tallenna-kanavatoimenpide
      (fn [user kanavatoimenpide {urakka-id ::kanavan-toimenpide/urakka-id :as hakuehdot}]
        (tallenna-kanavatoimenpide db user urakka-id kanavatoimenpide)
        (hae-kanavatoimenpiteet db user hakuehdot))
      {:kysely-spec ::kanavan-toimenpide/kanava-toimenpide
       :vastaus-spec ::kanavan-toimenpide/hae-kanavatoimenpiteet-vastaus})
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-kanavat-ja-kohteet
      :tallenna-kanavatoimenpide)
    this))