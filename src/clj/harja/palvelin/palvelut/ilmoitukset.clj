(ns harja.palvelin.palvelut.ilmoitukset
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.kyselyt.konversio :as konv]
            [taoensso.timbre :as log]
            [clj-time.coerce :refer [from-sql-time]]
            [harja.kyselyt.ilmoitukset :as q]
            [harja.palvelin.palvelut.urakat :as urakat]
            [harja.palvelin.integraatiot.tloik.tloik-komponentti :as tloik]
            [harja.kyselyt.konversio :as konversio])
  (:import (java.util Date)))

(defn hakuehto-annettu? [p]
  (cond
    (number? p) true
    (instance? Date p) true
    (map? p) (some true? (map #(hakuehto-annettu? (val %)) p))
    (empty? p) false
    :else true))

(defn- viesti [mille mista ilman]
  (str ", "
       (if (hakuehto-annettu? mille)
         (str mista " " (pr-str mille))
         (str ilman))))

(defn hae-ilmoitukset
  [db user {:keys [hallintayksikko urakka urakoitsija urakkatyyppi tilat tyypit kuittaustyypit aikavali hakuehto selite]}]
  (let [aikavali-alku (when (first aikavali)
                        (konv/sql-date (first aikavali)))
        aikavali-loppu (when (second aikavali)
                         (konv/sql-date (second aikavali)))
        urakat (urakat/kayttajan-urakat-aikavalilta db user
                                                    urakka urakoitsija urakkatyyppi hallintayksikko
                                                    (first aikavali) (second aikavali))
        tyypit (mapv name tyypit)
        selite-annettu? (boolean (and selite (first selite)))
        selite (if selite-annettu? (name (first selite)) "")
        debug-viesti (str "Haetaan ilmoituksia: "
                          (viesti urakat "urakoista" "ilman urakoita")
                          (viesti aikavali-alku "alkaen" "ilman alkuaikaa")
                          (viesti aikavali-loppu "päättyen" "ilman päättymisaikaa")
                          (viesti tyypit "tyypeistä" "ilman tyyppirajoituksia")
                          (viesti kuittaustyypit "kuittaustyypeistä" "ilman kuittaustyyppirajoituksia")
                          (viesti selite "selitteellä:" "ilman selitettä")
                          (viesti hakuehto "hakusanoilla:" "ilman tekstihakua")
                          (cond
                            (:avoimet tilat) ", mutta vain avoimet."
                            (and (:suljetut tilat) (:avoimet tilat)) ", ja näistä avoimet JA suljetut."
                            (:suljetut tilat) ", ainoastaan suljetut."))
        _ (log/debug debug-viesti)
        tulos (when-not (empty? urakat)
                (mapv
                  #(assoc % :uusinkuittaus
                            (when-not (empty? (:kuittaukset %))
                              (:kuitattu (last (sort-by :kuitattu (:kuittaukset %))))))
                  (konv/sarakkeet-vektoriin
                    (into []
                          (comp
                            (harja.geo/muunna-pg-tulokset :sijainti)
                            (map konv/alaviiva->rakenne)
                            (map #(assoc % :urakkatyyppi (keyword (:urakkatyyppi %))))
                            (map #(konv/array->vec % :selitteet))
                            (map #(assoc % :selitteet (mapv keyword (:selitteet %))))
                            (map #(assoc-in % [:kuittaus :kuittaustyyppi] (keyword (get-in % [:kuittaus :kuittaustyyppi]))))
                            (map #(assoc % :ilmoitustyyppi (keyword (:ilmoitustyyppi %))))
                            (map #(assoc-in % [:ilmoittaja :tyyppi] (keyword (get-in % [:ilmoittaja :tyyppi])))))
                          (q/hae-ilmoitukset db
                                             urakat
                                             (hakuehto-annettu? aikavali-alku) (hakuehto-annettu? aikavali-loppu)
                                             aikavali-alku aikavali-loppu
                                             (hakuehto-annettu? tyypit) tyypit
                                             (hakuehto-annettu? hakuehto) (str "%" hakuehto "%")
                                             selite-annettu? selite))
                    {:kuittaus :kuittaukset})))]
    (log/debug "Löydettiin ilmoitukset: " (map :id tulos))
    (log/debug "Jokaisella on kuittauksia " (map #(count (:kuittaukset %)) tulos) "kappaletta")
    tulos))

(defn tallenna-ilmoitustoimenpide [db tloik _ ilmoitustoimenpide]
  (log/debug (format "Tallennetaan uusi ilmoitustoimenpide: %s" ilmoitustoimenpide))
  (let [toimenpide (q/luo-ilmoitustoimenpide<!
                     db
                     (:ilmoituksen-id ilmoitustoimenpide)
                     (:ulkoinen-ilmoitusid ilmoitustoimenpide)
                     (harja.pvm/nyt)
                     (:vapaateksti ilmoitustoimenpide)
                     (name (:tyyppi ilmoitustoimenpide))
                     (:ilmoittaja-etunimi ilmoitustoimenpide)
                     (:ilmoittaja-sukunimi ilmoitustoimenpide)
                     (:ilmoittaja-tyopuhelin ilmoitustoimenpide)
                     (:ilmoittaja-matkapuhelin ilmoitustoimenpide)
                     (:ilmoittaja-sahkoposti ilmoitustoimenpide)
                     (:ilmoittaja-organisaatio ilmoitustoimenpide)
                     (:ilmoittaja-ytunnus ilmoitustoimenpide)
                     (:kasittelija-etunimi ilmoitustoimenpide)
                     (:kasittelija-sukunimi ilmoitustoimenpide)
                     (:kasittelija-tyopuhelin ilmoitustoimenpide)
                     (:kasittelija-matkapuhelin ilmoitustoimenpide)
                     (:kasittelija-sahkoposti ilmoitustoimenpide)
                     (:kasittelija-organisaatio ilmoitustoimenpide)
                     (:kasittelija-ytunnus ilmoitustoimenpide))]
    (tloik/laheta-ilmoitustoimenpide tloik (:id toimenpide))
    (-> toimenpide
        (assoc-in [:kuittaaja :etunimi] (:kuittaaja_henkilo_etunimi toimenpide))
        (assoc-in [:kuittaaja :sukunimi] (:kuittaaja_henkilo_sukunimi toimenpide))
        (assoc-in [:kuittaaja :matkapuhelin] (:kuittaaja_henkilo_matkapuhelin toimenpide))
        (assoc-in [:kuittaaja :tyopuhelin] (:kuittaaja_henkilo_tyopuhelin toimenpide))
        (assoc-in [:kuittaaja :sahkoposti] (:kuittaaja_henkilo_sahkoposti toimenpide))
        (assoc-in [:kuittaaja :organisaatio] (:kuittaaja_organisaatio_nimi toimenpide))
        (assoc-in [:kuittaaja :ytunnus] (:kuittaaja_organisaatio_ytunnus toimenpide))
        (assoc-in [:ilmoittaja :etunimi] (:ilmoittaja_henkilo_etunimi toimenpide))
        (assoc-in [:ilmoittaja :sukunimi] (:ilmoittaja_henkilo_sukunimi toimenpide))
        (assoc-in [:ilmoittaja :matkapuhelin] (:ilmoittaja_henkilo_matkapuhelin toimenpide))
        (assoc-in [:ilmoittaja :tyopuhelin] (:ilmoittaja_henkilo_tyopuhelin toimenpide))
        (assoc-in [:ilmoittaja :sahkoposti] (:ilmoittaja_henkilo_sahkoposti toimenpide))
        (assoc-in [:ilmoittaja :organisaatio] (:ilmoittaja_organisaatio_nimi toimenpide))
        (assoc-in [:ilmoittaja :ytunnus] (:ilmoittaja_organisaatio_ytunnus toimenpide))
        (assoc-in [:kasittelija :etunimi] (:kasittelija_henkilo_etunimi toimenpide))
        (assoc-in [:kasittelija :sukunimi] (:kasittelija_henkilo_sukunimi toimenpide))
        (assoc-in [:kasittelija :matkapuhelin] (:kasittelija_henkilo_matkapuhelin toimenpide))
        (assoc-in [:kasittelija :tyopuhelin] (:kasittelija_henkilo_tyopuhelin toimenpide))
        (assoc-in [:kasittelija :sahkoposti] (:kasittelija_henkilo_sahkoposti toimenpide))
        (assoc-in [:kasittelija :organisaatio] (:kasittelija_organisaatio_nimi toimenpide))
        (assoc-in [:kasittelija :ytunnus] (:kasittelija_organisaatio_ytunnus toimenpide)))))

(defrecord Ilmoitukset []
  component/Lifecycle
  (start [this]
    (julkaise-palvelu (:http-palvelin this)
                      :hae-ilmoitukset
                      (fn [user tiedot]
                        (hae-ilmoitukset (:db this) user tiedot)))
    (julkaise-palvelu (:http-palvelin this)
                      :tallenna-ilmoitustoimenpide
                      (fn [user tiedot]
                        (tallenna-ilmoitustoimenpide (:db this) (:tloik this) user tiedot)))
    this)

  (stop [this]
    (poista-palvelut (:http-palvelin this) :hae-ilmoitukset :tallenna-ilmoitustoimenpide)
    this))
