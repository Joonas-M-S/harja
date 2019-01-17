(ns harja.palvelin.palvelut.pohjavesialueet
  "Palvelu pohjavesialueiden hakemiseksi, palauttaa alueet tietokannan pohjavesialue taulusta."
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelu]]
            [harja.geo :as geo]
            [harja.kyselyt.pohjavesialueet :as q]
            [harja.kyselyt.materiaalit :as m]

            [harja.domain.oikeudet :as oikeudet]))


(defn hae-pohjavesialueet [db user hallintayksikko]
  (into []
        (geo/muunna-pg-tulokset :alue)
        (q/hae-pohjavesialueet db hallintayksikko)))

(defn hae-urakan-pohjavesialueet [db user urakka-id]
  (into [] (q/hae-urakan-pohjavesialueet db urakka-id)))

(defn hae-pohjavesialueen-suolatoteuma [db user pohjavesialue alkupvm loppupvm]
  (into [] (m/hae-pohjavesialueen-suolatoteuma db {:pohjavesialue pohjavesialue
                                                   :alkupvm alkupvm
                                                   :loppupvm loppupvm})))

(defrecord Pohjavesialueet []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this)
                      :hae-pohjavesialueet
                      (fn [user hallintayksikko]
                        (oikeudet/ei-oikeustarkistusta!)
                        (hae-pohjavesialueet (:db this) user hallintayksikko)))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-urakan-pohjavesialueet
                      (fn [user urakka-id]
                        (oikeudet/ei-oikeustarkistusta!)
                        (hae-urakan-pohjavesialueet (:db this) user urakka-id)))
    (julkaise-palvelu (:http-palvelin this)
                      :hae-pohjavesialueen-suolatoteuma
                      (fn [user tiedot]
                        (oikeudet/ei-oikeustarkistusta!)
                        (hae-pohjavesialueen-suolatoteuma (:db this) user (:pohjavesialue tiedot) (:alkupvm tiedot) (:loppupvm tiedot))))
    this)

  (stop [this]
    (poista-palvelu (:http-palvelin this) :hae-pohjavesialueet)
    this))
