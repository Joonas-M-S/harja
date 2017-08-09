(ns harja.palvelin.palvelut.selainvirhe-test
  (:require [clojure.test :refer :all]
            [harja.palvelin.palvelut.selainvirhe :refer :all]
            [harja.testi :refer :all]
            [harja.pvm :as pvm]
            [clj-time.core :as t]
            [clj-time.coerce :as c]))

(deftest raportoi-yhteyskatkos-testi
  (let [kayttaja +kayttaja-jvh+
        ping-1 {:aika (pvm/luo-pvm 2000 1 1) :palvelu :ping}
        ping-2 {:aika (pvm/nyt) :palvelu :ping}
        hae-ilmoitukset {:aika (pvm/nyt) :palvelu :hae-ilmoitukset}
        yhteyskatkos {:yhteyskatkokset [ping-1 hae-ilmoitukset ping-2]}
        formatoitu-yhteyskatkos (formatoi-yhteyskatkos kayttaja yhteyskatkos)]
    (is (= formatoitu-yhteyskatkos {:text (str "Käyttäjä " (:kayttajanimi kayttaja) " (" (:id kayttaja) ")" " raportoi yhteyskatkoksista palveluissa:")
                                    :fields [{:title ":ping" :value (str "Katkoksia 2 kpl(slack-n)ensimmäinen: " (c/from-date (:aika ping-1))
                                                                         "(slack-n)viimeinen: " (c/from-date (:aika ping-2)))}
                                             {:title ":hae-ilmoitukset" :value (str "Katkoksia 1 kpl(slack-n)ensimmäinen: " (c/from-date (:aika hae-ilmoitukset))
                                                                                    "(slack-n)viimeinen: " (c/from-date (:aika hae-ilmoitukset)))}]}))))
