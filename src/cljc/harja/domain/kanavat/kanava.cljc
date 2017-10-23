(ns harja.domain.kanavat.kanava
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [specql.transform :as xf]
    [clojure.set]
    [specql.rel :as rel]
    #?@(:clj  [
    [harja.kyselyt.specql-db :refer [define-tables]]
    [clojure.future :refer :all]]
        :cljs [[specql.impl.registry]])

    [harja.domain.muokkaustiedot :as m]
    [harja.domain.kanavat.kanavan-kohde :as kohde])
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(define-tables
  ["kan_kanava" ::kanava
   {::kohteet (specql.rel/has-many ::id
                                   ::harja.domain.kanavat.kanavan-kohde/kohde
                                   ::harja.domain.kanavat.kanavan-kohde/kanava-id)}])

(def perustiedot
  #{::id
    ::nimi})

(def perustiedot-ja-sijainti (conj perustiedot ::sijainti))

(def kohteet
  #{[::kohteet (clojure.set/union
                 harja.domain.kanavat.kanavan-kohde/perustiedot
                 harja.domain.kanavat.kanavan-kohde/metatiedot)]})

(def kohteet-sijainteineen
  #{[::kohteet harja.domain.kanavat.kanavan-kohde/perustiedot-ja-sijainti]})

;; Palvelut

(s/def ::hakuteksti string?)

(s/def ::hae-kanavat-ja-kohteet-vastaus
  (s/coll-of (s/keys :req [])))
