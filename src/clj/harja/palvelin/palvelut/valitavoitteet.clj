(ns harja.palvelin.palvelut.valitavoitteet
  "Palvelu välitavoitteiden hakemiseksi ja tallentamiseksi."
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.valitavoitteet :as q]
            [harja.kyselyt.konversio :as konv]
            [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [harja.domain.oikeudet :as oikeudet]))

(defn hae-urakan-valitavoitteet [db user urakka-id]
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-valitavoitteet user urakka-id)

  (into []
        (map konv/alaviiva->rakenne)
        (q/hae-urakan-valitavoitteet db urakka-id)))

(defn hae-valtakunnalliset-valitavoitteet [db user]
  (oikeudet/vaadi-lukuoikeus oikeudet/hallinta-valitavoitteet user)

  (into []
        (q/hae-valtakunnalliset-valitavoitteet db)))

(defn merkitse-valmiiksi! [db user {:keys [urakka-id valitavoite-id valmis-pvm kommentti] :as tiedot}]
  (log/info "merkitse valmiiksi: " tiedot)
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-valitavoitteet user urakka-id)
  (jdbc/with-db-transaction [c db]
    (and (= 1 (q/merkitse-valmiiksi! db (konv/sql-date valmis-pvm) kommentti
                                     (:id user) urakka-id valitavoite-id))
         (hae-urakan-valitavoitteet db user urakka-id))))

(defn tallennaurakan-valitavoitteet! [db user {:keys [urakka-id valitavoitteet]}]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-valitavoitteet user urakka-id)
  (log/debug "Tallenna välitavoitteet " (pr-str valitavoitteet))
  (jdbc/with-db-transaction [c db]
    ;; Poistetaan tietokannasta :poistettu merkityt
    (doseq [poistettava (filter :poistettu valitavoitteet)]
      (q/poista-valitavoite! c (:id user) urakka-id (:id poistettava)))

    ;; Luodaan uudet (FIXME: lisää kentät kun speksi valmis)
    (doseq [{:keys [takaraja nimi]} (filter
                                      #(and (< (:id %) 0)
                                            (not (:poistettu %)))
                                            valitavoitteet)]
      (q/lisaa-valitavoite<! c {:urakka urakka-id
                                :takaraja (konv/sql-date takaraja)
                                :nimi nimi
                                :luoja (:id user)}))

    ;; Päivitetään olemassaolevat (FIXME: lisää kentät kun speksi valmis)
    (doseq [{:keys [id takaraja nimi]} (filter #(> (:id %) 0) valitavoitteet)]
      (q/paivita-valitavoite! c nimi (konv/sql-date takaraja) (:id user) urakka-id id))

    ;; Lopuksi haetaan uudet tavoitteet
    (hae-urakan-valitavoitteet c user urakka-id)))

(defrecord Valitavoitteet []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this) :hae-urakan-valitavoitteet
                      (fn [user urakka-id]
                        (hae-urakan-valitavoitteet (:db this) user urakka-id)))
    (julkaise-palvelu (:http-palvelin this) :hae-valtakunnalliset-valitavoitteet
                      (fn [user _]
                        (hae-valtakunnalliset-valitavoitteet (:db this) user)))
    (julkaise-palvelu (:http-palvelin this) :merkitse-valitavoite-valmiiksi
                      (fn [user tiedot]
                        (merkitse-valmiiksi! (:db this) user tiedot)))
    (julkaise-palvelu (:http-palvelin this) :tallenna-urakan-valitavoitteet
                      (fn [user tiedot]
                        (tallennaurakan-valitavoitteet! (:db this) user tiedot)))
    this)

  (stop [this]
    (poista-palvelut (:http-palvelin this)
                     :hae-valtakunnalliset-valitavoitteet
                     :hae-urakan-valitavoitteet
                     :merkitse-valitavoite-valmiiksi
                     :tallenna-urakan-valitavoitteet)
    this))
