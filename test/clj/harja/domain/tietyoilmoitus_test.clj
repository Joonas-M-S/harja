(ns harja.domain.tietyoilmoitus-test
  (:require [clojure.test :refer [deftest is]]
            [harja.testi :refer :all]
            [harja.domain.tietyoilmoitus :as tietyoilmoitukset]
            [harja.domain.muokkaustiedot :as muokkaustiedot]))

(def urakoitsijakayttaja {:organisaatio {:tyyppi "urakoitsija" :id 1} :id 1})
(def tilaajakayttaja {:organisaatio {:tyyppi "liikennevirasto" :id 2} :id 2})
(def kayttajan-urakat #{1 2 3})

(deftest tallentamisen-oikeustarkistus

  (is (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                        kayttajan-urakat
                                        {::tietyoilmoitukset/urakka-id 1})
      "Urakoitsija saa tallentaa uuden ilmoituksen omaan urakkaan")

  (is (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                        kayttajan-urakat
                                        {::tietyoilmoitukset/urakka-id 1 ::tietyoilmoitukset/id 1})
      "Urakoitsija saa ilmoitusta, joka on luotu hänen urakkaansa")

  (is (nil? (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                              kayttajan-urakat
                                              {::tietyoilmoitukset/urakka-id 666}))
      "Urakoitsija ei saa tallentaa uutta ilmoitusta urakkaan, joka ei ole hänen listassa")
  (is (nil? (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                              kayttajan-urakat
                                              {::tietyoilmoitukset/id 1
                                               ::tietyoilmoitukset/urakka-id 666}))
      "Urakoitsija ei saa päivittää ilmoitusta urakkaan, joka ei ole hänen listassa")


  (is (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                        kayttajan-urakat
                                        {})
      "Urakoitsija saa luoda ilmoituksen, jolla ei ole urakkaa")

  (is (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                        kayttajan-urakat
                                        {::muokkaustiedot/luoja-id (:id urakoitsijakayttaja)})
      "Urakoitsija saa päivittää urakattoman ilmoituksen, jonka on itse luonut")

  (is (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                        kayttajan-urakat
                                        {::tietyoilmoitukset/urakoitsija-id (get-in urakoitsijakayttaja [:organisaatio :id])})
      "Urakoitsija saa päivittää urakattoman ilmoituksen, joka on luotu hänen organisaationsa urakkaan")


  (is (nil? (tietyoilmoitukset/voi-tallentaa? urakoitsijakayttaja
                                              kayttajan-urakat
                                              {::tietyoilmoitukset/id 1
                                               ::muokkaustiedot/luoja-id 666}))
      "Urakoitsija ei saa päivittää urakatonta ilmoitusta, jota ei ole itse luonut tai luotu hänen organisaatiostaan
      tai hänen urakkaansa")

  (is  (tietyoilmoitukset/voi-tallentaa? tilaajakayttaja
                                             kayttajan-urakat
                                             {::tietyoilmoitukset/urakka-id 666})
      "Tilaaja saa luoda uuden tietyöilmoituksen urakkan, joka ei ole listattu hänelle"))
