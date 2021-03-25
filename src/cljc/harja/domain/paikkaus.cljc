(ns harja.domain.paikkaus
  (:require
    [clojure.spec.alpha :as s]
    [harja.domain.muokkaustiedot :as muokkaustiedot]
    [harja.kyselyt.specql :as harja-specql]
    [harja.pvm :as pvm]

    #?@(:clj  [
               [harja.kyselyt.specql-db :refer [define-tables]]
               ]
        :cljs [[specql.impl.registry]]))
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]]))
  #?(:clj
     (:import (org.postgis PGgeometry))))

(define-tables
  ["paikkauskohde" ::paikkauskohde
   {"luoja-id" ::muokkaustiedot/luoja-id
    "luotu" ::muokkaustiedot/luotu
    "muokkaaja-id" ::muokkaustiedot/muokkaaja-id
    "muokattu" ::muokkaustiedot/muokattu
    "poistettu" ::muokkaustiedot/poistettu?
    "tarkistettu" ::tarkistettu
    "tarkistaja-id" ::tarkistaja-id
    "ilmoitettu-virhe" ::ilmoitettu-virhe
    ::paikkaukset (specql.rel/has-many ::id
                                       ::paikkaus
                                       ::paikkauskohde-id)
    ::kustannukset (specql.rel/has-many ::id
                                        ::paikkaustoteuma
                                        ::paikkauskohde-id)}]
  ["paikkaus" ::paikkaus
   {"luoja-id" ::muokkaustiedot/luoja-id
    "luotu" ::muokkaustiedot/luotu
    "muokkaaja-id" ::muokkaustiedot/muokkaaja-id
    "muokattu" ::muokkaustiedot/muokattu
    "poistaja-id" ::muokkaustiedot/poistaja-id
    "poistettu" ::muokkaustiedot/poistettu?
    ::paikkauskohde (specql.rel/has-one ::paikkauskohde-id
                                        ::paikkauskohde
                                        ::id)
    ::tienkohdat (specql.rel/has-many ::id
                                      ::paikkauksen-tienkohta
                                      ::paikkaus-id)
    ::materiaalit (specql.rel/has-many ::id
                                       ::paikkauksen_materiaali
                                       ::paikkaus-id)}
   #?(:clj {::sijainti (specql.transform/transform (harja.kyselyt.specql/->GeometryTierekisteri))})]
  ["paikkauksen_tienkohta" ::paikkauksen-tienkohta
   {"id" ::tienkohta-id}]
  ["paikkauksen_materiaali" ::paikkauksen_materiaali
   {"id" ::materiaali-id}]
  ["paikkaustoteuma" ::paikkaustoteuma
   {"luoja-id" ::muokkaustiedot/luoja-id
    "luotu" ::muokkaustiedot/luotu
    "muokkaaja-id" ::muokkaustiedot/muokkaaja-id
    "muokattu" ::muokkaustiedot/muokattu
    "poistaja-id" ::muokkaustiedot/poistaja-id
    "poistettu" ::muokkaustiedot/poistettu?}])

(def paikkauskohteen-perustiedot
  #{::id
    ::ulkoinen-id
    ::nimi
    ::yhalahetyksen-tila
    ::virhe
    ::tarkistettu
    ::tarkistaja-id
    ::ilmoitettu-virhe})

(def paikkauksen-perustiedot
  #{::id
    ::urakka-id
    ::paikkauskohde-id
    ::ulkoinen-id
    ::alkuaika
    ::loppuaika
    ::tierekisteriosoite
    ::tyomenetelma
    ::massatyyppi
    ::leveys
    ::massamenekki
    ::raekoko
    ::kuulamylly
    ::sijainti})

(def tienkohta-perustiedot
  #{::tienkohta-id
    ::ajorata
    ::reunat
    ::ajourat
    ::ajouravalit
    ::keskisaumat})

(def materiaalit-perustiedot
  #{::materiaali-id
    ::esiintyma
    ::kuulamylly-arvo
    ::muotoarvo
    ::sideainetyyppi
    ::pitoisuus
    ::lisa-aineet})

(def paikkaustoteuman-perustiedot
  #{::id
    ::urakka-id
    ::paikkauskohde-id
    ::ulkoinen-id
    ::toteuma-id
    ::kirjattu
    ::tyyppi
    ::selite
    ::hinta
    ::tyomenetelma
    ::valmistumispvm})

(s/def ::pvm (s/nilable (s/or :pvm pvm/pvm?
                              :date #(instance? #?(:cljs js/Date
                                                   :clj  java.util.Date) %))))

(s/def ::aikavali (s/nilable (s/coll-of ::pvm :kind? vector :count 2)))
(s/def ::paikkaus-idt (s/nilable (s/coll-of integer? :kind set?)))
(s/def ::tr (s/nilable map?))
(s/def ::tyomenetelmat (s/nilable set?))
(s/def ::ensimmainen-haku? boolean?)
(s/def ::teiden-pituudet (s/nilable map?))


(s/def ::urakan-paikkauskohteet-kysely (s/keys :req [::urakka-id]
                                               :opt-un [::aikavali ::paikkaus-idt ::tr ::tyomenetelmat ::ensimmainen-haku?]))

(s/def ::urakan-paikkauskohteet-vastaus (s/keys :req-un [::paikkaukset]
                                                :opt-un [::paikkauskohteet ::teiden-pituudet ::tyomenetelmat]))

(s/def ::paikkausurakan-kustannukset-kysely (s/keys :req [::urakka-id]
                                                    :opt-un [::aikavali ::paikkaus-idt ::tr ::tyomenetelmat ::ensimmainen-haku?]))

(s/def ::paikkausurakan-kustannukset-vastaus (s/keys :req-un [::kustannukset]
                                                     :opt-un [::paikkauskohteet ::tyomenetelmat]))

;; FIXME: keksitty lista. Hommaa YHA-jengiltä oikea lista
(def tyomenetelmat-jotka-lahetetaan-yhaan
  #{"massapintaus" "remix-pintaus"})

(defn pitaako-paikkauskohde-lahettaa-yhaan? [tyomenetelma]
  (boolean (tyomenetelmat-jotka-lahetetaan-yhaan tyomenetelma)))

(def paikkauskohteiden-tyomenetelmat
  {"ABPL" "AB-paikkaus levittäjällä"
   "PABPL" "PAB-paikkaus levittäjällä"
   "SMA" "SMA-paikkaus levittäjällä"
   "KTVA" "KT-valuasfalttipaikkaus (KTVA)"
   "REPA" "Konetiivistetty reikävaluasfalttipaikkaus (REPA)"
   "SIPU" "Sirotepuhalluspaikkaus (SIPU)"
   "SIPA" "Sirotepintauksena tehty lappupaikkaus (SIPA)"
   "UREM" "Urapaikkaus (UREM/RREM)"
   "HJYR" "Jyrsintä (HJYR/TJYR)"
   "KKSA" "Kannukaatosaumaus"
   "KTVASA" "KT-valuasfalttisaumaus"
   "AVSA" "Avarrussaumaus"
   "SKPPK" "Sillan kannen päällysteen päätysauman korjaukset"
   "RPVST" "Reunapalkin ja päällysteen välisen sauman tiivistäminen"
   "RLSAT" "Reunapalkin liikuntasauman tiivistäminen"
   "KTPP" "Käsin tehtävät paikkaukset pikapaikkausmassalla"
   "ABPK" "AB-paikkaus käsin"
   "PABPK" "PAB-paikkaus käsin"
   "MPA" "Muu päällysteiden paikkaustyö"})

(def paikkauskohteiden-yksikot
  #{"m2" "t" "kpl" "jm"})