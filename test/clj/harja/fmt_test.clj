(ns harja.fmt-test
  (:require
    [clojure.test :refer :all]
    [harja.fmt :as fmt]
    [taoensso.timbre :as log]))

(deftest kuvaile-aikavali-toimii
  (is (thrown? AssertionError (fmt/kuvaile-paivien-maara nil)))
  (is (thrown? AssertionError (fmt/kuvaile-paivien-maara -4)))
  (is (= (fmt/kuvaile-paivien-maara 0) ""))
  (is (= (fmt/kuvaile-paivien-maara 1) "1 päivä"))
  (is (= (fmt/kuvaile-paivien-maara 6) "6 päivää"))
  (is (= (fmt/kuvaile-paivien-maara 7) "1 viikko"))
  (is (= (fmt/kuvaile-paivien-maara 10) "1 viikko"))
  (is (= (fmt/kuvaile-paivien-maara 15) "2 viikkoa"))
  (is (= (fmt/kuvaile-paivien-maara 30) "1 kuukausi"))
  (is (= (fmt/kuvaile-paivien-maara 90) "3 kuukautta"))
  (is (= (fmt/kuvaile-paivien-maara 365) "1 vuosi"))
  (is (= (fmt/kuvaile-paivien-maara 850) "2 vuotta"))

  (is (= (fmt/kuvaile-paivien-maara 0 {:lyhenna-yksikot? true}) ""))
  (is (= (fmt/kuvaile-paivien-maara 1 {:lyhenna-yksikot? true}) "1pv"))
  (is (= (fmt/kuvaile-paivien-maara 6 {:lyhenna-yksikot? true}) "6pv"))
  (is (= (fmt/kuvaile-paivien-maara 7 {:lyhenna-yksikot? true}) "1vk"))
  (is (= (fmt/kuvaile-paivien-maara 10 {:lyhenna-yksikot? true}) "1vk"))
  (is (= (fmt/kuvaile-paivien-maara 15 {:lyhenna-yksikot? true}) "2vk"))
  (is (= (fmt/kuvaile-paivien-maara 30 {:lyhenna-yksikot? true}) "1kk"))
  (is (= (fmt/kuvaile-paivien-maara 90 {:lyhenna-yksikot? true}) "3kk"))
  (is (= (fmt/kuvaile-paivien-maara 365 {:lyhenna-yksikot? true}) "1v"))
  (is (= (fmt/kuvaile-paivien-maara 850 {:lyhenna-yksikot? true}) "2v")))