(ns harja.kyselyt.specql
  "Määritellään yleisiä clojure.spec tyyppejä."
  (:require [specql.data-types :as d]
            [harja.domain.tierekisteri :as tr]
            [clojure.spec.alpha :as s]
    #?@(:clj [
            [harja.kyselyt.specql-db :refer [define-tables]]
            ]))
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(s/def ::d/geometry any?)

(define-tables
  ["tr_osoite" ::tr/osoite])

;; TODO ALLA OLEVAT MÄÄRITYKSET PITÄISI TEHDÄ OIKEASTI SPECQL-KIRJASTOON
;; Kun tehty, voi myös harja.domain.liitteeltä poistaa require tähän ns:ään.

#?(:clj
   (defmethod specql.impl.composite/parse-value :specql.data-types/int4 [_ string]
     (Long/parseLong string)))

(s/def :specql.data-types/uint4 (s/int-in 0 4294967295))
(s/def :specql.data-types/oid :specql.data-types/uint4)