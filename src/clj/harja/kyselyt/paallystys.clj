(ns harja.kyselyt.paallystys
  (:require [jeesql.core :refer [defqueries]]
            [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.yllapitokohteet :as yllapitokohteet-q]
            [harja.geo :as geo]))

(defqueries "harja/kyselyt/paallystys.sql")

(def kohdeosa-xf (geo/muunna-pg-tulokset :sijainti))

(defn hae-urakan-paallystysilmoitukset-kohteineen [db urakka-id sopimus-id vuosi]
  (into []
        (comp
          (map #(konv/string-poluista->keyword % [[:paatos-taloudellinen-osa]
                                                  [:paatos-tekninen-osa]
                                                  [:tila]]))
          (map #(assoc % :kohdeosat
                         (into []
                               kohdeosa-xf
                               (yllapitokohteet-q/hae-urakan-yllapitokohteen-yllapitokohdeosat
                                 db {:urakka urakka-id
                                     :sopimus sopimus-id
                                     :yllapitokohde (:paallystyskohde-id %)})))))
        (harja.kyselyt.paallystys/hae-urakan-paallystysilmoitukset db {:urakka urakka-id
                                                                       :sopimus sopimus-id
                                                                       :vuosi vuosi})))