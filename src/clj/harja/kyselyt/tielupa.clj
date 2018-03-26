(ns harja.kyselyt.tielupa
  (:require
    [harja.kyselyt.specql-db :refer [define-tables]]
    [specql.core :refer [fetch update! insert! upsert!]]
    [specql.op :as op]
    [jeesql.core :refer [defqueries]]
    [clojure.set :as set]
    [harja.id :refer [id-olemassa?]]
    [harja.domain.tielupa :as tielupa]
    [harja.pvm :as pvm]
    [harja.domain.muokkaustiedot :as muokkaustiedot]))

(defqueries "harja/kyselyt/tielupa.sql"
            {:positional? true})

(defn hae-tieluvat [db hakuehdot]
  (fetch db
         ::tielupa/tielupa
         (set/union
           harja.domain.tielupa/perustiedot
           harja.domain.tielupa/hakijan-tiedot
           harja.domain.tielupa/urakoitsijan-tiedot
           harja.domain.tielupa/liikenneohjaajan-tiedot
           harja.domain.tielupa/tienpitoviranomaisen-tiedot
           harja.domain.tielupa/johto-ja-kaapeliluvan-tiedot)
         hakuehdot))

(defn hae-tielupien-hakijat [db hakuteksti]
  (set
    (fetch db
           ::tielupa/tielupa
           #{::tielupa/hakija-nimi}
           {::tielupa/hakija-nimi (op/ilike (str hakuteksti "%"))})))

(defn hae-ulkoisella-tunnistella [db ulkoinen-id]
  (first (hae-tieluvat db {::tielupa/ulkoinen-tunniste ulkoinen-id})))

(defn onko-olemassa-ulkoisella-tunnisteella? [db ulkoinen-id]
  (and
    (number? ulkoinen-id)
    (not (empty? (hae-tieluvat db {::tielupa/ulkoinen-tunniste ulkoinen-id})))))

(defn tallenna-tielupa [db tielupa]
  (let [id (::tielupa/id tielupa)
        ulkoinen-tunniste (::tielupa/ulkoinen-tunniste tielupa)
        uusi (assoc tielupa ::muokkaustiedot/luotu (pvm/nyt))
        muokattu (assoc tielupa ::muokkaustiedot/muokattu (pvm/nyt))]
    (if (id-olemassa? id)
      (update! db ::tielupa/tielupa muokattu {::tielupa/id id})
      (if (onko-olemassa-ulkoisella-tunnisteella? db ulkoinen-tunniste)
        (update! db ::tielupa/tielupa muokattu {::tielupa/ulkoinen-tunniste ulkoinen-tunniste})
        (insert! db ::tielupa/tielupa uusi)))))


