(ns harja.domain.pot2
  "Ylläpidon päällystysurakoissa käytettävän POT2-lomakkeen skeemat."
  (:require [schema.core :as schema]
            [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]
            [specql.impl.registry]
            [specql.data-types]
            [harja.domain.urakka :as urakka]
            [harja.domain.muokkaustiedot :as m]
            [harja.domain.tierekisteri :as tr]
            [clojure.spec.alpha :as s]
            #?@(:clj [[harja.kyselyt.specql-db :refer [define-tables]]
                      ]))
  #?(:cljs
     (:require-macros [harja.kyselyt.specql-db :refer [define-tables]])))

(def +alustamenetelmat+
  "Kaikki alustan käsittelymenetelmät POT-lomake Excelistä ja
   :koodi 24 on asiakkaan jälkeenpäin pyytämä."
  [{:nimi "Massanvaihto" :lyhenne "MV" :koodi 1}
   {:nimi "Bitumiemusiostabilointi" :lyhenne "BEST" :koodi 11}
   {:nimi "Vaahtobitumistabilointi" :lyhenne "VBST" :koodi 12}
   {:nimi "Remix-stabilointi" :lyhenne "REST" :koodi 13}
   {:nimi "Sementtistabilointi" :lyhenne "SST" :koodi 14}
   {:nimi "Masuunihiekkastabilointi" :lyhenne "MHST" :koodi 15}
   {:nimi "Komposiittistabilointi" :lyhenne "KOST" :koodi 16}
   {:nimi "Kantavan kerroksen AB" :lyhenne "ABK" :koodi 21}
   {:nimi "Sidekerroksen AB" :lyhenne "ABS" :koodi 22}
   {:nimi "Murske" :lyhenne "MS" :koodi 23}
   {:nimi "Sekoitusjyrsintä" :lyhenne "SJYR" :koodi 24}
   {:nimi "Kuumennustasaus" :lyhenne "TASK" :koodi 31}
   {:nimi "Massatasaus" :lyhenne "TAS" :koodi 32}
   {:nimi "Tasausjyrsintä" :lyhenne "TJYR" :koodi 41}
   {:nimi "Laatikkojyrsintä" :lyhenne "LJYR" :koodi 42}
   {:nimi "Reunajyrsintä" :lyhenne "RJYR" :koodi 43}])

(def +alustamenetelmat-ja-nil+
  (conj +alustamenetelmat+ {:nimi "Ei menetelmää" :lyhenne "Ei menetelmää" :koodi nil}))

(def +alustamenetelma+ "Alustan käsittelymenetelmän valinta koodilla"
  (apply schema/enum (map :koodi +alustamenetelmat-ja-nil+)))

(defn alustamenetelma-koodi-nimella [nimi]
  (:koodi (first (filter #(= nimi (:nimi %)) +alustamenetelmat-ja-nil+))))

(def +verkkotyypit+
  "Verkkotyypit POT-lomake Excelistä"
  [{:nimi "Teräsverkko" :koodi 1}
   {:nimi "Lasikuituverkko" :koodi 2}
   {:nimi "Muu" :koodi 9}])

(def +verkkotyypit-ja-nil+
  (conj +verkkotyypit+ {:nimi "Ei verkkotyyppiä" :koodi nil}))

(def +verkkotyyppi-tai-nil+ "Verkkotyypin valinta koodilla"
  (apply schema/enum (map :koodi +verkkotyypit-ja-nil+)))

(defn verkkotyyppi-koodi-nimella [nimi]
  (:koodi (first (filter #(= nimi (:nimi %)) +verkkotyypit-ja-nil+))))

(def +tekniset-toimenpiteet+
  "Tekniset toimenpidetyypit POT-lomake Excelistä"
  [{:nimi "Rakentaminen" :koodi 1}
   {:nimi "Suuntauksen parantaminen" :koodi 2}
   {:nimi "Raskas rakenteen parantaminen" :koodi 3}
   {:nimi "Kevyt rakenteen parantaminen" :koodi 4}])

(def +tekniset-toimenpiteet-ja-nil+
  (conj +tekniset-toimenpiteet+ {:nimi "Ei toimenpidettä" :koodi nil}))

(def +tekninen-toimenpide-tai-nil+ "Teknisen toimenpiteen valinta koodilla"
  (apply schema/enum (map :koodi +tekniset-toimenpiteet-ja-nil+)))

(defn tekninentoimenpide-koodi-nimella [nimi]
  (:koodi (first (filter #(= nimi (:nimi %)) +tekniset-toimenpiteet-ja-nil+))))

(def +ajoradat-tekstina+
  "Ajoratavalinnat"
  [{:nimi "Yksiajoratainen" :koodi 0}
   {:nimi "Kaksiajorataisen ensimmäinen" :koodi 1}
   {:nimi "Kaksiajorataisen toinen ajorata" :koodi 2}])

(def +ajoradat-numerona+
  "Ajoratavalinnat"
  [{:nimi "0" :koodi 0}
   {:nimi "1" :koodi 1}
   {:nimi "2" :koodi 2}])

(def +kaistat+
  "Kaistavalinnat"
  [{:nimi "1" :koodi 1}
   {:nimi "11" :koodi 11}
   {:nimi "12" :koodi 12}
   {:nimi "13" :koodi 13}
   {:nimi "21" :koodi 21}
   {:nimi "22" :koodi 22}
   {:nimi "23" :koodi 23}
   {:nimi "14" :koodi 14}
   {:nimi "15" :koodi 15}
   {:nimi "16" :koodi 16}
   {:nimi "17" :koodi 17}
   {:nimi "18" :koodi 18}
   {:nimi "19" :koodi 19}
   {:nimi "24" :koodi 24}
   {:nimi "25" :koodi 25}
   {:nimi "26" :koodi 26}
   {:nimi "27" :koodi 27}
   {:nimi "28" :koodi 28}
   {:nimi "29" :koodi 29}
   {:nimi "31" :koodi 31}])

(def +verkon-tarkoitukset+
  [{:nimi "Pituushalkeamien ehkäisy" :koodi 1}
   {:nimi "Muiden routavaurioiden ehkäisy" :koodi 2}
   {:nimi "Levennyksen tukeminen" :koodi 3}
   {:nimi "Painumien ehkäisy" :koodi 4}
   {:nimi "Moniongelmaisen tukeminen" :koodi 5}
   {:nimi "Muu tarkoitus" :koodi 9}])

(def +verkon-tarkoitukset-ja-nil+
  (conj +verkon-tarkoitukset+ {:nimi "Ei tarkoitusta" :koodi nil}))

(def +verkon-tarkoitus-tai-nil+
  "Verkon tarkoituksen valinta koodilla"
  (apply schema/enum (map :koodi +verkon-tarkoitukset-ja-nil+)))

(defn verkon-tarkoitus-koodi-nimella [koodi]
  (:koodi (first (filter #(= koodi (:nimi %)) +verkon-tarkoitukset-ja-nil+))))

(def +verkon-sijainnit+
  [{:nimi "Päällysteessä" :koodi 1}
   {:nimi "Kantavan kerroksen yläpinnassa" :koodi 2}
   {:nimi "Kantavassa kerroksessa" :koodi 3}
   {:nimi "Kantavan kerroksen alapinnassa" :koodi 4}
   {:nimi "Muu sijainti" :koodi 9}])

(def +verkon-sijainnit-ja-nil+
  (conj +verkon-sijainnit+ {:nimi "Ei sijaintia" :koodi nil}))

(def +verkon-sijainti-tai-nil+
  "Verkon sijainnin valinta koodilla"
  (apply schema/enum (map :koodi +verkon-sijainnit-ja-nil+)))

(defn verkon-sijainti-koodi-nimella [koodi]
  (:koodi (first (filter #(= koodi (:nimi %)) +verkon-sijainnit-ja-nil+))))

(def +paallystystyon-tyypit+
  "Päällystystyön tyypit"
  [{:nimi "Ajoradan päällyste" :koodi :ajoradan-paallyste}
   {:nimi "Pienaluetyöt" :koodi :pienaluetyot}
   {:nimi "Tasaukset" :koodi :tasaukset}
   {:nimi "Jyrsinnät" :koodi :jyrsinnat}
   {:nimi "Muut" :koodi :muut}])

(defn paallystystyon-tyypin-nimi-koodilla [koodi]
  (:nimi (first (filter
                  #(= koodi (:koodi %))
                  +paallystystyon-tyypit+))))

(defn paattele-ilmoituksen-tila
  [valmis-kasiteltavaksi tekninen-osa-hyvaksytty]
  (cond
    tekninen-osa-hyvaksytty
    "lukittu"

    valmis-kasiteltavaksi
    "valmis"

    :default
    "aloitettu"))

(defn arvo-koodilla [koodisto koodi]
  (:nimi (first (filter #(= (:koodi %) koodi) koodisto))))

(define-tables
  ["pot2" ::pot2
   {"paatos_tekninen_osa" ::paatos-tekninen-osa
    "luoja" ::m/luoja-id
    "muokkaaja" ::m/muokkaaja-id
    "paallystyskohde" ::paallystyskohde-id}]
  ["pot2_kulutuskerros_toimenpide" ::pot2-kulutuskerros-toimenpide]
  ["pot2_paallystekerros" ::pot2-paallystekerros
   {"kuulamyllyarvo" ::paallystysiedot-km-arvo}]
  ["pot2_massa_runkoaine" ::pot2-massa-runkoaine
   {"id" :runkoaine/id
    "pot2_massa_id" :runkoaine/pot2-massa-id
    "kuulamyllyarvo" :runkoaine/kuulamyllyarvo
    "kiviaine_esiintyma" :runkoaine/kiviaine_esiintyma
    "erikseen_lisattava_fillerikiviaines" :runkoaine/erikseen-lisattava-fillerikiviaines
    "muotoarvo" :runkoaine/muotoarvo
    "massaprosentti" :runkoaine/massaprosentti}]
  ["pot2_massa_lisaaine" ::pot2-massa-lisaaine
   {"id" :lisaaine/id
    "pot2_massa_id" :lisaaine/pot2-massa-id
    "nimi" :lisaaine/nimi
    "pitoisuus" :lisaaine/pitoisuus}]
  ["pot2_massa_sideaine" ::pot2-massa-sideaine
   {"id" :sideaine/id
    "pot2_massa_id" :sideaine/pot2-massa-id
    "tyyppi" :sideaine/tyyppi
    "pitoisuus" :sideaine/pitoisuus
    "lopputuote?" :sideaine/lopputuote?}]
  ["pot2_massa" ::pot2-massa
   {"id" :pot2-massa/id
    "pot2_id" ::pot2-id
    "urakka_id" ::urakka-id
    "nimi" ::nimi
    "massatyyppi" ::massatyyppi
    "max_raekoko" ::max_raekoko
    "asfalttiasema" ::asfalttiasema
    "kuulamyllyluokka" ::kuulamyllyluokka
    "litteyslukuluokka" ::litteyslukuluokka
    "dop_nro" ::dop_nro
    "poistettu" ::poistettu?

    "muokkaaja" ::m/muokkaaja-id
    "muokattu" ::m/muokattu
    "luoja" ::m/luoja-id
    "luotu" ::m/luotu}
   {::runkoaineet (specql.rel/has-many :pot2-massa/id
                                       ::pot2-massa-runkoaine
                                       :runkoaine/pot2-massa-id)}
   {::lisa-aineet (specql.rel/has-many :pot2-massa/id
                                       ::pot2-massa-lisaaine
                                       :lisaaine/pot2-massa-id)}
   {::sideaineet (specql.rel/has-many :pot2-massa/id
                                      ::pot2-massa-sideaine
                                      :sideaine/pot2-massa-id)}]
  ["pot2_runkoaine" ::pot2-runkoaine])

(def massan-max-raekoko [5, 8, 11, 16, 22, 31])
(def litteyslukuluokat [1, 2, 3, 4, 5, 6])

(def erikseen-lisattava-fillerikiviaines
  ["Kalkkifilleri (KF)", "Lentotuhka (LT)", "Muu fillerikiviaines"])

(def massan-lisaineet
  ["Kuitu", "Tartuke", "Sementti", "Luonnonasfaltti", "Kumi- tai muovirouhe", "Väriaine",
   "Muu kemiallinen aine"])

(def paallystemassan-sideaineet
  ["Bitumi, 20/30"
   "Bitumi, 35/50"
   "Bitumi, 50/70"
   "Bitumi, 70/100"
   "Bitumi, 100/150"
   "Bitumi, 160/220"
   "Bitumi, 250/330"
   "Bitumi, 330/430"
   "Bitumi, 500/650"
   "Bitumi, 650/900"
   "Bitumi, V1500"
   "Bitumi, V3000"
   "Polymeerimodifioitu bitumi, PMB 75/130-65"
   "Polymeerimodifioitu bitumi, PMB 75/130-70"
   "Polymeerimodifioitu bitumi, PMB 40/100-70"
   "Polymeerimodifioitu bitumi, PMB 40/100-75"
   "Bitumiliuokset ja fluksatut bitumit, BL0"
   "Bitumiliuokset ja fluksatut bitumit, BL5"
   "Bitumiliuokset ja fluksatut bitumit, BL2Bio"
   "Bitumiemulsiot, BE-L"
   "Bitumiemulsiot, PBE-L"
   "Bitumiemulsiot, BE-SIP"
   "Bitumiemulsiot, BE-SOP"
   "Bitumiemulsiot, BE-AB"
   "Bitumiemulsiot, BE-PAB"])