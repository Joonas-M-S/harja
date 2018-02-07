(ns harja.palvelin.palvelut.liitteet
  (:require [com.stuartsierra.component :as component]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut transit-vastaus]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.params :refer [wrap-params]]
            [harja.kyselyt.toteumat :as tot-q]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.liitteet :as liitteet]
            [harja.domain.liite :as liite-domain]
            [specql.core :as specql]
            [harja.domain.turvallisuuspoikkeama :as turpo]
            [harja.domain.oikeudet :as oikeudet]
            [harja.palvelin.palvelut.toteumat-tarkistukset :as tarkistukset]
            [harja.tyokalut.tietoturva :as tietoturva])
  (:import (java.io ByteArrayOutputStream ByteArrayInputStream)))

(defn tallenna-liite
  "Tallentaa liitteen kantaan, mutta ei linkitä sitä mihinkään domain-asiaan."
  [liitteet req]
  (let [parametrit (:params req)
        liite (get parametrit "liite")
        urakka (Integer/parseInt (get parametrit "urakka"))]

    (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-liitteet (:kayttaja req) urakka)
    (if liite
      (let [{:keys [filename content-type tempfile size kuvaus]} liite
            uusi-liite (liitteet/luo-liite liitteet (:id (:kayttaja req)) urakka filename content-type size tempfile kuvaus "harja-ui")]
        (log/debug "Tallennettu liite " filename " (" size " tavua)")
        (transit-vastaus (-> uusi-liite
                             (dissoc :liite_oid :pikkukuva :luoja :luotu))))
      {:status 400
       :body "Ei liitettä"})))


(defn lataa-liite [liitteet req]
  (let [id (Integer/parseInt (get (:params req) "id"))
        {:keys [tyyppi koko urakka data]} (liitteet/lataa-liite liitteet id)]
    (oikeudet/vaadi-lukuoikeus oikeudet/urakat-liitteet (:kayttaja req) urakka)
    {:status 200
     :headers {"Content-Type" tyyppi
               "Content-Length" koko}
     :body (ByteArrayInputStream. data)}))

(defn lataa-pikkukuva [liitteet req]
  (let [id (Integer/parseInt (get (:params req) "id"))
        {:keys [pikkukuva urakka]} (liitteet/lataa-pikkukuva liitteet id)]
    (oikeudet/vaadi-lukuoikeus oikeudet/urakat-liitteet (:kayttaja req) urakka)
    (log/debug "Ladataan pikkukuva " id)
    (if pikkukuva
      {:status 200
       :headers {"Content-Type" "image/png"
                 "Content-Length" (count pikkukuva)}
       :body (ByteArrayInputStream. pikkukuva)}
      {:status 404
       :body "Annetulle liittelle ei pikkukuvaa."})))

(def liitteen-poisto-domainin-mukaan
  {:turvallisuuspoikkeama {:linkkitaulu ::liite-domain/turvallisuuspoikkeama<->liite
                           :linkkitaulu-domain-id ::liite-domain/turvallisuuspoikkeama-id
                           :linkkitaulu-liite-id ::liite-domain/liite-id
                           :domain-taulu ::turpo/turvallisuuspoikkeama
                           :domain-taulu-id ::turpo/id
                           :domain-taulu-urakka-id ::turpo/urakka-id
                           :oikeustarkistus (fn [user urakka-id]
                                              (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-turvallisuus user urakka-id))}})

(defn poista-liite-linkitys
  "Poistaa liitteen linkityksen tietystä domain-asiasta. Liitettä ei näy enää missään, mutta se jää kuitenkin meille talteen."
  [db user {:keys [urakka-id domain liite-id domain-id]}]
  (let [domain-tiedot (domain liitteen-poisto-domainin-mukaan)
        oikeustarkistus-fn (:oikeustarkistus domain-tiedot)]
    ;; TODO Testi tälle
    (oikeustarkistus-fn user urakka-id)
    (tietoturva/vaadi-linkitys db
                               (:domain-taulu domain-tiedot)
                               (:domain-taulu-id domain-tiedot)
                               domain-id
                               (:domain-taulu-urakka-id domain-tiedot)
                               urakka-id)
    (specql/delete! db
                    (:linkkitaulu domain-tiedot)
                    {(:linkkitaulu-domain-id domain-tiedot) domain-id
                     (:linkkitaulu-liite-id domain-tiedot) liite-id})))

(defrecord Liitteet []
  component/Lifecycle
  (start [{:keys [http-palvelin db] :as this}]
    (julkaise-palvelu http-palvelin :tallenna-liite
                      (wrap-multipart-params (fn [req] (tallenna-liite (:liitteiden-hallinta this) req)))
                      {:ring-kasittelija? true})
    (julkaise-palvelu http-palvelin :lataa-liite
                      (wrap-params (fn [req]
                                     (lataa-liite (:liitteiden-hallinta this) req)))
                      {:ring-kasittelija? true})
    (julkaise-palvelu http-palvelin :lataa-pikkukuva
                      (wrap-params (fn [req]
                                     (lataa-pikkukuva (:liitteiden-hallinta this) req)))
                      {:ring-kasittelija? true})
    (julkaise-palvelu http-palvelin :poista-liite-linkki
                      (fn [user {:keys [domain liite-id domain-id urakka-id]}]
                        (poista-liite-linkitys db user {:urakka-id urakka-id
                                                        :domain domain
                                                        :liite-id liite-id
                                                        :domain-id domain-id})))
    this)

  (stop [{:keys [http-palvelin] :as this}]
    (poista-palvelut http-palvelin :tallenna-liite :lataa-liite :lataa-pikkukuva :poista-liite-linkki)
    this))
