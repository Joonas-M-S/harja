(ns harja.kyselyt.specql-db
  (:require [specql.core :as specql]))

(defmacro define-tables [& tables]
  `(specql/define-tables
     {:connection-uri "jdbc:postgresql://localhost/harjatest_template?user=postgres"}
     ~@tables))
