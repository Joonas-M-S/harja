(ns harja.domain.vesivaylat.turvalaite
  "Turvalaitteen tiedot"
  (:require
    [clojure.spec :as s]
    #?@(:clj [[harja.kyselyt.specql-db :refer [define-tables]]
              [clojure.future :refer :all]]))
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(define-tables
  ["reimari_turvalaite" ::reimari-turvalaite]
  ["vv_turvalaite" ::turvalaite])