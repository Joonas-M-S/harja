(ns harja.domain.vesivaylat.materiaali
  (:require
    [harja.domain.muokkaustiedot :as m]
    [clojure.spec.alpha :as s]
    #?@(:clj [
    [harja.kyselyt.specql-db :refer [define-tables]]
    [clojure.future :refer :all]]))
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(define-tables
  ["vv_materiaali" ::materiaali
   {"muokattu" ::m/muokattu
    "muokkaaja" ::m/muokkaaja-id
    "luotu" ::m/luotu
    "luoja" ::m/luoja-id}]
  ["vv_materiaali_muutos" ::muutos]
  ["vv_materiaalilistaus" ::materiaalilistaus])


(s/def ::materiaalilistauksen-haku (s/keys :req [::urakka-id]))
(s/def ::materiaalilistauksen-vastaus (s/coll-of ::materiaalilistaus))

(s/def ::materiaalikirjaus (s/keys :req [::urakka-id ::nimi ::nimi ::maara ::pvm]
                                   :opt [::lisatieto]))
