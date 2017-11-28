(ns harja.domain.kanavat.kohde
  (:require
    [clojure.string :as str]
    [clojure.spec.alpha :as s]
    [specql.transform :as xf]
    [clojure.set :as set]
    [specql.rel :as rel]

    #?@(:clj  [
    [harja.kyselyt.specql-db :refer [define-tables]]
    [clojure.future :refer :all]]
        :cljs [[specql.impl.registry]])

    [harja.domain.muokkaustiedot :as m]
    [harja.domain.kanavat.kohdekokonaisuus :as kok]
    [harja.domain.kanavat.kohteenosa :as osa])
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(define-tables
  ["kohteen_tyyppi" ::kohteen-tyyppi (specql.transform/transform (specql.transform/to-keyword))]
  ["kan_kohde" ::kohde
   harja.domain.muokkaustiedot/muokkaustiedot
   harja.domain.muokkaustiedot/poistaja-sarake
   harja.domain.muokkaustiedot/poistettu?-sarake
   {::kohdekokonaisuus (specql.rel/has-one ::kohdekokonaisuus-id
                                           :harja.domain.kanavat.kohdekokonaisuus/kohdekokonaisuus
                                           :harja.domain.kanavat.kohdekokonaisuus/id)
    ::ylos (specql.rel/has-one ::ylos-id
                               ::kohde
                               ::id)
    ::alas (specql.rel/has-one ::alas-id
                               ::kohde
                               ::id)
    ::kohteenosat (specql.rel/has-many ::id
                                       :harja.domain.kanavat.kohteenosa/kohteenosa
                                       :harja.domain.kanavat.kohteenosa/kohde-id)}]
  ["kan_kohde_urakka" ::kohde<->urakka
   harja.domain.muokkaustiedot/muokkaustiedot
   harja.domain.muokkaustiedot/poistaja-sarake
   harja.domain.muokkaustiedot/poistettu?-sarake
   {::linkin-urakka (specql.rel/has-one ::urakka-id
                                        :harja.domain.urakka/urakka
                                        :harja.domain.urakka/id)
    ::linkin-kohde (specql.rel/has-one ::kohde-id
                                       ::kohde
                                       ::id)}])

(def perustiedot
  #{::id
    ::nimi})

(def kohde-ylos
  #{[::ylos perustiedot]})

(def kohde-alas
  #{[::alas perustiedot]})

(def kohteenosat
  #{[::kohteenosat osa/perustiedot]})

(def perustiedot+osat
  (set/union perustiedot kohteenosat))

(def metatiedot m/muokkauskentat)

(def perustiedot-ja-sijainti (conj perustiedot ::sijainti))

(def kohteen-urakkatiedot #{[::linkin-urakka #{:harja.domain.urakka/id :harja.domain.urakka/nimi}]})

(def kohteen-kohdekokonaisuus #{[::kohdekokonaisuus kok/perustiedot]})

;; Domain-funktiot

(defn tyyppi->str [kohde]
  ({:silta "silta"
    :sulku "sulku"
    :sulku-ja-silta "sulku ja silta"}
    kohde))

(defn fmt-kohteen-nimi
  "Ottaa mapin, jossa on kohteen tiedot ja ::kohdekokonaisuus avaimen takana kanavan tiedot."
  [kohde]
  (::nimi kohde))

(defn fmt-kohteenosan-nimi
  [kohde osa]
  (str
    (::nimi kohde)
    (cond
      (::osa/nimi osa) (str ", " (::osa/nimi osa))
      (::osa/tyyppi osa) (str ", " (::osa/tyyppi osa))
      :else "")))

(defn silta? [osa]
  (or (= :silta (::tyyppi osa))
      (= :rautatiesilta (::tyyppi osa))))

(defn sulku? [osa]
  (= :sulku (::tyyppi osa)))