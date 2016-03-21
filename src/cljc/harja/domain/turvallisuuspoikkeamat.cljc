(ns harja.domain.turvallisuuspoikkeamat)

(def turpo-tyypit {:tyotapaturma "Työtapaturma"
                   :vaaratilanne "Vaaratilanne"
                   :turvallisuushavainto "Turvallisuushavainto"
                   :muu "Muu"})

(def vahinkoluokittelu-tyypit {:henkilovahinko   "Henkilövahinko"
                               :omaisuusvahinko  "Omaisuusvahinko"
                               :ymparistovahinko "Ympäristövahinko"})

(def turpo-vakavuusasteet {:lieva "Lievä"
                           :vakava "Vakava"})