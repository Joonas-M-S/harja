(ns harja.domain.ely
  "ELY-alueiden oma domain (kannassa tallentuu organisaatio-tauluun)"
  #?@(:clj  [
             (:require [clojure.spec.alpha :as s]
                       [harja.kyselyt.specql-db :refer [define-tables]]
                       [clojure.future :refer :all])]
      :cljs [(:require [clojure.spec.alpha :as s]
               [specql.impl.registry]
               [specql.data-types])
             (:require-macros
               [harja.kyselyt.specql-db :refer [define-tables]])]))

(def elynumero->lyhenne {1 "Uusimaa"
                         2 "Varsinais-Suomi"
                         3 "Kaakkois-Suomi"
                         4 "Pirkanmaa"
                         8 "Pohjois-Savo"
                         9 "Keski-Suomi"
                         10 "Etelä-Pohjanmaa"
                         12 "Pohjois-Pohjanmaa ja Kainuu"
                         14 "Lappi"})

(def elynumero->nimi {1 "UUD"
                      2 "VAR"
                      3 "KAS"
                      4 "PIR"
                      8 "POR"
                      9 "KES"
                      10 "EPO"
                      12 "POP"
                      14 "LAP"})