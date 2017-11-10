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
    [harja.domain.urakka :as ur])
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(define-tables
  ["kan_kanava" ::kanava
   {::kohteet (specql.rel/has-many ::id
                                   :harja.domain.kanavat.kanavan-kohde/kohde
                                   :harja.domain.kanavat.kanavan-kohde/kanava-id)}])

(def perustiedot
  #{::id
    ::nimi})

(def perustiedot-ja-sijainti (conj perustiedot ::sijainti))

(def kohteet
  #{[::kohteet #{:harja.domain.kanavat.kanavan-kohde/id
                 :harja.domain.kanavat.kanavan-kohde/nimi
                 :harja.domain.kanavat.kanavan-kohde/tyyppi}]})

(def kohteet-sijainteineen
  #{[::kohteet #{:harja.domain.kanavat.kanavan-kohde/id
                 :harja.domain.kanavat.kanavan-kohde/nimi
                 :harja.domain.kanavat.kanavan-kohde/tyyppi
                 :harja.domain.kanavat.kanavan-kohde/sijainti}]})

;; Palvelut

(s/def ::hakuteksti string?)

(s/def ::hae-kanavat-ja-kohteet-vastaus
  (s/coll-of (s/keys :req [::id ::nimi ::kohteet])))

(s/def ::lisaa-kanavalle-kohteita-kysely
  (s/coll-of (s/keys :req [:harja.domain.kanavat.kanavan-kohde/kanava-id :harja.domain.kanavat.kanavan-kohde/id :harja.domain.kanavat.kanavan-kohde/tyyppi]
                     :opt [:harja.domain.kanavat.kanavan-kohde/nimi ::m/poistettu?])))

(s/def ::lisaa-kanavalle-kohteita-vastaus ::hae-kanavat-ja-kohteet-vastaus)

(s/def ::liita-kohde-urakkaan-kysely (s/keys :req-un [::urakka-id :harja.domain.kanavat.kanavan-kohde/id ::poistettu?]))

(s/def ::poista-kohde-kysely (s/keys :req-un [:harja.domain.kanavat.kanavan-kohde/id]))

(s/def ::hae-urakan-kohteet-kysely (s/keys :req [::ur/id]))
(s/def ::hae-urakan-kohteet-vastaus (s/coll-of (s/keys :req [:harja.domain.kanavat.kanavan-kohde/id
                                                             :harja.domain.kanavat.kanavan-kohde/tyyppi
                                                             :harja.domain.kanavat.kanavan-kohde/kohteen-kanava]
                                                       :opt [:harja.domain.kanavat.kanavan-kohde/nimi])))
