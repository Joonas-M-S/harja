(ns harja.domain.tiemerkinta-toteumat
  "Tienäkymän tietojen spec-määritykset"
  (:require [clojure.spec :as s]
    #?@(:clj [[clojure.future :refer :all]])))

(s/def ::tiemerkinnan-yksikkohintainen-tyo
  (s/keys :req-un [::selite ::muutospvm ::hintatyyppi ::yllapitoluokka ::id
                   ::pituus ::hinta-kohteelle
                   ::yllapitokohde-id ::tr-numero ::hinta]))

;; Haut

(s/def ::hae-tiemerkinnan-yksikkohintaiset-tyot-kysely
  (s/keys :req-un [::urakka-id]))

(s/def ::hae-tiemerkinnan-yksikkohintaiset-tyot-vastaus
  (s/coll-of ::tiemerkinnan-yksikkohintainen-tyo))

;; Tallennukset

(s/def ::tallenna-tiemerkinnan-yksikkohintaiset-tyot-kysely
  (s/keys :req-un [::urakka-id int?
                   ::toteumat (s/coll-of ::tiemerkinnan-yksikkohintainen-tyo)]))

(s/def ::tallenna-tiemerkinnan-yksikkohintaiset-tyot-vastaus
  ::hae-tiemerkinnan-yksikkohintaiset-tyot-vastaus)



