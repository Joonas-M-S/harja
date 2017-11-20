(ns harja.palvelin.palvelut.kanavat.kanavatoimenpiteet
  (:require [com.stuartsierra.component :as component]
            [clojure.set :as set]
            [taoensso.timbre :as log]
            [specql.core :as specql]
            [specql.op :as op]
            [clojure.java.jdbc :as jdbc]
            [harja.id :as id]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.toimenpidekoodi :as toimenpidekoodi]
            [harja.domain.kanavat.kanavan-toimenpide :as toimenpide]
            [harja.domain.kanavat.hinta :as hinta]
            [harja.domain.kanavat.tyo :as tyo]
            [harja.kyselyt.kanavat.kanavan-toimenpide :as q-toimenpide]))

(defn hae-kanavatoimenpiteet [db user {urakka-id ::toimenpide/urakka-id
                                       sopimus-id ::toimenpide/sopimus-id
                                       alkupvm :alkupvm
                                       loppupvm :loppupvm
                                       toimenpidekoodi ::toimenpidekoodi/id
                                       tyyppi ::toimenpide/kanava-toimenpidetyyppi
                                       :as hakuehdot}]

  (assert urakka-id "Urakka-id puuttuu!")
  (case tyyppi
    :kokonaishintainen (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kanavat-kokonaishintaiset user urakka-id)
    :muutos-lisatyo (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kanavat-lisatyot user urakka-id))

  (let [tyyppi (when tyyppi (name tyyppi))]
    (q-toimenpide/hae-sopimuksen-toimenpiteet-aikavalilta
     db
     {:urakka urakka-id
      :sopimus sopimus-id
      :alkupvm alkupvm
      :loppupvm loppupvm
      :toimenpidekoodi toimenpidekoodi
      :tyyppi tyyppi})))

(defn- vaadi-toimenpiteet-kuuluvat-urakkaan* [toimenpiteet toimenpide-idt urakka-id]
  (when (or
         (nil? urakka-id)
         (not (->> toimenpiteet
                   (map ::toimenpide/urakka-id)
                   (every? (partial = urakka-id)))))
    (throw (SecurityException. (str "Toimenpiteet " toimenpide-idt " eivät kuulu urakkaan " urakka-id)))))

(defn vaadi-toimenpiteet-kuuluvat-urakkaan [db toimenpide-idt urakka-id]
  (vaadi-toimenpiteet-kuuluvat-urakkaan*
   (specql/fetch
    db
    ::toimenpide/kanava-toimenpide
    (set/union toimenpide/perustiedot toimenpide/viittaus-idt)
    {::toimenpide/id (op/in toimenpide-idt)})
   toimenpide-idt
   urakka-id))

(defn tallenna-kanavatoimenpiteen-hinnoittelu! [db user tiedot]
  (let [urakka-id (::toimenpide/urakka-id tiedot)
        toimenpide-id (::toimenpide/id tiedot)]
    (assert urakka-id "Urakka-id puuttuu!")
    (oikeudet/vaadi-oikeus "hinnoittele-toimenpide" oikeudet/urakat-vesivaylatoimenpiteet-yksikkohintaiset user urakka-id) ;; FIXME
    (vaadi-toimenpiteet-kuuluvat-urakkaan db #{(::toimenpide/id tiedot)} urakka-id)
    (let [olemassa-olevat-hinta-idt (->> (keep ::hinta/id (::hinta/tallennettavat-hinnat tiedot))
                                         (filter id/id-olemassa?)
                                         (set))
          olemassa-olevat-tyo-idt (->> (keep ::tyo/id (::hinta/tallennettavat-tyot tiedot))
                                       (filter id/id-olemassa?)
                                       (set))]
      #_(vaadi-hinnat-kuuluvat-toimenpiteeseen db olemassa-olevat-hinta-idt toimenpide-id) ;; FIXME
      #_(vaadi-tyot-kuuluvat-toimenpiteeseen db olemassa-olevat-tyo-idt toimenpide-id))

    (jdbc/with-db-transaction [db db]
      (q-toimenpide/tallenna-toimenpiteen-omat-hinnat!
       {:db db
        :user user
        :hinnat (::hinta/tallennettavat-hinnat tiedot)})
      (q-toimenpide/tallenna-toimenpiteen-tyot!
       {:db db
        :user user
        :tyot (::tyo/tallennettavat-tyot tiedot)})
      (q-toimenpide/hae-toimenpiteen-oma-hinnoittelu db toimenpide-id))))
(defn tarkista-kutsu [user urakka-id tyyppi]
  (assert urakka-id "Kanavatoimenpiteellä ei ole urakkaa.")
  (assert tyyppi "Kanavatoimenpiteellä ei ole tyyppiä.")
  (case tyyppi
    :kokonaishintainen (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kanavat-kokonaishintaiset user urakka-id)
    :muutos-lisatyo (oikeudet/vaadi-lukuoikeus oikeudet/urakat-kanavat-lisatyot user urakka-id)))

(defn hae-kanavatoimenpiteet [db user {urakka-id ::toimenpide/urakka-id
                                       sopimus-id ::toimenpide/sopimus-id
                                       alkupvm :alkupvm
                                       loppupvm :loppupvm
                                       toimenpidekoodi ::toimenpidekoodi/id
                                       tyyppi ::toimenpide/kanava-toimenpidetyyppi}]

  (tarkista-kutsu user urakka-id tyyppi)
  (let [tyyppi (name tyyppi)]
    (q-toimenpide/hae-sopimuksen-toimenpiteet-aikavalilta
      db
      {:urakka urakka-id
       :sopimus sopimus-id
       :alkupvm alkupvm
       :loppupvm loppupvm
       :toimenpidekoodi toimenpidekoodi
       :tyyppi tyyppi})))

(defn tallenna-kanavatoimenpide [db user {tyyppi ::toimenpide/tyyppi
                                          urakka-id ::toimenpide/urakka-id
                                          :as kanavatoimenpide}]
  (tarkista-kutsu user urakka-id tyyppi)
  (q-toimenpide/tallenna-toimenpide db (:id user) kanavatoimenpide))

(defrecord Kanavatoimenpiteet []
  component/Lifecycle
  (start [{http :http-palvelin db :db :as this}]
    (julkaise-palvelu
     http
     :hae-kanavatoimenpiteet
     (fn [user hakuehdot]
       (hae-kanavatoimenpiteet db user hakuehdot))
     {:kysely-spec ::toimenpide/hae-kanavatoimenpiteet-kysely
      :vastaus-spec ::toimenpide/hae-kanavatoimenpiteet-vastaus})

    (julkaise-palvelu
     http
     :tallenna-kanavatoimenpiteen-hinnoittelu
     (fn [user hakuehdot]
       (tallenna-kanavatoimenpiteen-hinnoittelu! db user hakuehdot))
     {:kysely-spec ::toimenpide/tallenna-kanavatoimenpiteen-hinnoittelu-kysely
      :vastaus-spec ::toimenpide/tallenna-kanavatoimenpiteen-hinnoittelu-vastaus})

    (julkaise-palvelu
      http
      :tallenna-kanavatoimenpide
      (fn [user {toimenpide ::toimenpide/kanava-toimenpide
                 hakuehdot ::toimenpide/hae-kanavatoimenpiteet-kysely}]
        (tallenna-kanavatoimenpide db user toimenpide)
        (hae-kanavatoimenpiteet db user hakuehdot))
      {:kysely-spec ::toimenpide/tallenna-kanavatoimenpide-kutsu
       :vastaus-spec ::toimenpide/hae-kanavatoimenpiteet-vastaus})
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-kanavatoimenpiteet
      :tallenna-kanavatoimenpiteen-hinnoittelu
      :tallenna-kanavatoimenpide
      )
    this))
