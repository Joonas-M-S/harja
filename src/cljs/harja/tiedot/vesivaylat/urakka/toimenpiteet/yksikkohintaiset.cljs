(ns harja.tiedot.vesivaylat.urakka.toimenpiteet.yksikkohintaiset
  (:require [reagent.core :refer [atom]]
            [tuck.core :as tuck]
            [harja.loki :refer [log error]]
            [harja.domain.vesivaylat.toimenpide :as to]
            [harja.domain.urakka :as ur]
            [harja.domain.vesivaylat.vayla :as va]
            [harja.domain.vesivaylat.turvalaite :as tu]
            [harja.domain.vesivaylat.hinnoittelu :as h]
            [harja.domain.vesivaylat.hinta :as hinta]
            [harja.domain.vesivaylat.tyo :as tyo]
            [harja.domain.vesivaylat.turvalaitekomponentti :as tkomp]
            [harja.domain.vesivaylat.komponentin-tilamuutos :as komp-tila]
            [harja.domain.muokkaustiedot :as m]
            [harja.id :refer [id-olemassa?]]
            [cljs.core.async :as async :refer [<!]]
            [harja.pvm :as pvm]
            [harja.tiedot.urakka :as u]
            [harja.tyokalut.tuck :as tuck-tyokalut]
            [harja.tiedot.navigaatio :as nav]
            [harja.ui.protokollat :as protokollat]
            [harja.ui.viesti :as viesti]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tyokalut.spec-apurit :as spec-apurit]
            [cljs.spec.alpha :as s]
            [harja.tiedot.vesivaylat.urakka.toimenpiteet.jaettu :as jaettu]
            [harja.domain.urakka :as urakka]
            [reagent.core :as r])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(def alustettu-toimenpiteen-hinnoittelu
  {::to/id nil
   ::h/hinnat nil
   ::h/tyot []})

(def alustettu-hintaryhman-hinnoittelu
  {::h/id nil
   ::h/hinnat nil})

(defonce tila
  (atom {:valinnat {:urakka-id nil
                    :sopimus-id nil
                    :aikavali [nil nil]
                    :vaylatyyppi nil
                    :vayla-id nil
                    :turvalaite-id nil
                    :tyolaji nil
                    :tyoluokka nil
                    :toimenpide nil
                    :vain-vikailmoitukset? false}
         :nakymassa? false
         :toimenpiteiden-haku-kaynnissa? false
         :infolaatikko-nakyvissa {} ; tunniste -> boolean
         :uuden-hintaryhman-lisays? false
         :valittu-hintaryhma nil
         :uusi-hintaryhma ""
         :hintaryhman-tallennus-kaynnissa? false
         :hintaryhmien-poisto-kaynnissa? false
         :hintaryhmat nil
         :hintaryhmien-haku-kaynnissa? false
         :liitteen-lisays-kaynnissa? false
         :liitteen-poisto-kaynnissa? false
         :toimenpiteet nil
         :suunnitellut-tyot nil
         :suunniteltujen-toiden-haku-kaynnissa? false
         :hintaryhmien-liittaminen-kaynnissa? false
         :toimenpiteen-hinnoittelun-tallennus-kaynnissa? false
         :hintaryhman-hinnoittelun-tallennus-kaynnissa? false
         :hinnoittele-toimenpide alustettu-toimenpiteen-hinnoittelu
         :hinnoittele-hintaryhma alustettu-hintaryhman-hinnoittelu
         :turvalaitteet-kartalla nil
         :karttataso-nakyvissa? true
         :korostetut-turvalaitteet nil
         ;; korostettu-hintaryhma on false, kun hintaryhmää ei ole korostettu,
         ;; koska "kokonaishintaisista siirrettyjen" hintaryhmän id:n täytyy olla nil
         :korostettu-hintaryhma false}))

(defonce karttataso-yksikkohintaisten-turvalaitteet (r/cursor tila [:karttataso-nakyvissa?]))
(defonce turvalaitteet-kartalla (r/cursor tila [:turvalaitteet-kartalla]))

(def valinnat
  (reaction
    (when (:nakymassa? @tila)
      {:urakka-id (:id @nav/valittu-urakka)
       :sopimus-id (first @u/valittu-sopimusnumero)
       :aikavali @u/valittu-aikavali})))

(def vaylahaku
  (reify protokollat/Haku
    (hae [_ teksti]
      (go (let [vastaus (<! (k/post! :hae-vaylat {:hakuteksti teksti
                                                  :vaylatyyppi (get-in @tila [:valinnat :vaylatyyppi])}))]
            vastaus)))))

(def turvalaitehaku
  (reify protokollat/Haku
    (hae [_ teksti]
      (go (let [vastaus (<! (k/post! :hae-turvalaitteet-tekstilla {:hakuteksti teksti}))]
            vastaus)))))

;; Yleiset eventit
(defrecord Nakymassa? [nakymassa?])
(defrecord PaivitaValinnat [tiedot])
;; Siirto
(defrecord SiirraValitutKokonaishintaisiin [])
;; Suunnitellut työt
(defrecord TyhjennaSuunnitellutTyot [])
(defrecord HaeSuunnitellutTyot [])
(defrecord SuunnitellutTyotHaettu [vastaus])
(defrecord SuunnitellutTyotEiHaettu [])
;; Toimenpiteet
(defrecord HaeToimenpiteet [valinnat])
(defrecord ToimenpiteetHaettu [toimenpiteet])
(defrecord ToimenpiteetEiHaettu [virhe])
;; Hintaryhmät
(defrecord UudenHintaryhmanLisays? [lisays-auki?])
(defrecord UudenHintaryhmanNimeaPaivitetty [nimi])
(defrecord LuoHintaryhma [nimi])
(defrecord HintaryhmaLuotu [vastaus])
(defrecord HintaryhmaEiLuotu [virhe])
(defrecord HaeHintaryhmat [])
(defrecord HintaryhmatHaettu [vastaus])
(defrecord HintaryhmatEiHaettu [virhe])
(defrecord ValitseHintaryhma [hintaryhma])
(defrecord LiitaValitutHintaryhmaan [hintaryhma valitut])
(defrecord ValitutLiitettyHintaryhmaan [])
(defrecord ValitutEiLiitettyHintaryhmaan [virhe])
(defrecord PoistaHintaryhmat [hintaryhma-idt])
(defrecord HintaryhmatPoistettu [vastaus])
(defrecord HintaryhmatEiPoistettu [])
;; Toimenpiteen hinnoittelu
(defrecord AloitaToimenpiteenHinnoittelu [toimenpide-id])
(defrecord PeruToimenpiteenHinnoittelu [])
(defrecord AsetaHintakentalleTiedot [tiedot])
(defrecord AsetaTyorivilleTiedot [tiedot])
(defrecord AsetaKomponenttirivilleTiedot [tiedot])
(defrecord LisaaHinnoiteltavaTyorivi [])
(defrecord LisaaHinnoiteltavaKomponenttirivi [])
(defrecord LisaaMuuKulurivi [])
(defrecord LisaaMuuTyorivi [])
(defrecord PoistaHinnoiteltavaTyorivi [tiedot])
(defrecord PoistaMuuKulurivi [rivi])
(defrecord PoistaMuuTyorivi [tiedot])
(defrecord PoistaHinnoiteltavaKomponenttirivi [tiedot])
(defrecord TallennaToimenpiteenHinnoittelu [tiedot])
(defrecord ToimenpiteenHinnoitteluTallennettu [vastaus])
(defrecord ToimenpiteenHinnoitteluEiTallennettu [virhe])
;; Hintaryhmän hinnoittelu
(defrecord AloitaHintaryhmanHinnoittelu [hintaryhma-id])
(defrecord PeruHintaryhmanHinnoittelu [])
(defrecord AsetaHintaryhmakentalleTiedot [tiedot])
(defrecord TallennaHintaryhmanHinnoittelu [tiedot])
(defrecord HintaryhmanHinnoitteluTallennettu [vastaus])
(defrecord HintaryhmanHinnoitteluEiTallennettu [virhe])
;; Kartta
(defrecord KorostaHintaryhmaKartalla [hintaryhma])
(defrecord PoistaHintaryhmanKorostus [])

(defn hintaryhma-korostettu? [hintaryhma {:keys [korostettu-hintaryhma]}]
  (boolean
    (when-not (false? korostettu-hintaryhma)
      (= (::h/id hintaryhma) korostettu-hintaryhma))))

(def kokonaishintaisista-siirretyt-hintaryhma
  {::h/nimi "Kokonaishintaisista siirretyt, valitse tilaus."
   ::h/id -1})

(def reimarin-lisatyot-hintaryhma
  {::h/nimi "Reimarissa lisätyöksi merkityt, valitse tilaus."
   ::h/id -2})

(defn kokonaishintaisista-siirretyt-hintaryhma? [hintaryhma]
  (= (::h/id hintaryhma) (::h/id kokonaishintaisista-siirretyt-hintaryhma)))

(defn reimarin-lisatyot-hintaryhma? [hintaryhma]
  (= (::h/id hintaryhma) (::h/id reimarin-lisatyot-hintaryhma)))

(defn valiaikainen-hintaryhma? [hintaryhma]
  (or (kokonaishintaisista-siirretyt-hintaryhma? hintaryhma)
      (reimarin-lisatyot-hintaryhma? hintaryhma)))

(defn hintaryhmattomat-toimenpiteet-valiaikaisiin-ryhmiin [toimenpiteet]
  (for [to toimenpiteet]
    (assoc to ::to/hintaryhma-id (or (::to/hintaryhma-id to)
                                     (when (::to/reimari-lisatyo? to) (::h/id reimarin-lisatyot-hintaryhma))
                                     (::h/id kokonaishintaisista-siirretyt-hintaryhma)))))

(defn poista-hintaryhmien-korostus [app]
  (assoc app :korostettu-hintaryhma false))

(defn- hintakentta
  [hinta]
  (merge
    {::hinta/summa (cond
                     (or (nil? (::hinta/ryhma hinta))
                         (#{:muu} (::hinta/ryhma hinta)))
                     0

                     (#{:komponentti :tyo} (::hinta/ryhma hinta))
                     nil)
     ::hinta/yleiskustannuslisa 0}
    hinta))

(defn- tyokentta
  [tyo]
  (merge
    {::tyo/maara 0}
    tyo))

;; Toimenpiteen hinnoittelun yhteydessä tarjottavat vakiokentät (vectori, koska järjestys tärkeä)
(def vakiohinnat ["Yleiset materiaalit" "Matkakulut" "Muut kulut"])

(defn vakiohintakentta? [otsikko]
  (boolean ((set vakiohinnat) otsikko)))

(defn- toimenpiteen-hintakentat [hinnat]
  (vec (concat
         ;; Vakiohintakentät näytetään aina riippumatta siitä onko niille annettu hintaa
         (map-indexed (fn [index otsikko]
                        (let [olemassa-oleva-hinta (hinta/hinta-otsikolla hinnat otsikko)]
                          (hintakentta
                            (merge
                              {::hinta/id (dec (- index))
                               ::hinta/otsikko otsikko
                               ::hinta/ryhma :muu}
                              olemassa-oleva-hinta))))
                      vakiohinnat)
         ;; Loput kentät ovat käyttäjän itse lisäämiä
         (map
           hintakentta
           (remove #((set vakiohinnat) (::hinta/otsikko %))
                   hinnat)))))

;; Hintaryhmän hinta tallennetaan aina tällä hardkoodatulla nimellä
(def hintaryhman-hintakentta-otsikko "Ryhmähinta")

(defn muut-hinnat [app]
  (filter
    #(and (= (::hinta/ryhma %) :muu) (not (::m/poistettu? %)))
    (get-in app [:hinnoittele-toimenpide ::h/hinnat])))

(defn muut-tyot [app]
  (filter
    #(and (= (::hinta/ryhma %) :tyo) (not (::m/poistettu? %)))
    (get-in app [:hinnoittele-toimenpide ::h/hinnat])))

(defn komponenttien-hinnat [app]
  (filter
    #(and (= (::hinta/ryhma %) :komponentti) (not (::m/poistettu? %)))
    (get-in app [:hinnoittele-toimenpide ::h/hinnat])))

(defn ainoa-otsikon-vakiokentta? [hinnat otsikko]
  (and
    (vakiohintakentta? otsikko)
    (-> (group-by ::hinta/otsikko hinnat)
       (get otsikko)
       count
       (= 1))))

(defn- hintaryhman-hintakentat [hinnat]
  (let [ryhmahinta (hinta/hinta-otsikolla hinnat hintaryhman-hintakentta-otsikko)]
    ;; Luodaan ryhmähinnalle hintakenttä olemassa olevan ryhmähinnan perusteella.
    ;; Jos ei ole aiempaa ryhmähintaa, luo uuden hintakentän ilman id:tä.
    [(hintakentta
       (merge
         ryhmahinta
         {::hinta/otsikko hintaryhman-hintakentta-otsikko}))]))

(defn- hinnoittelun-voi-tallentaa? [app]
  (let [tyot (get-in app [:hinnoittele-toimenpide ::h/tyot])
        hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
        komponenttien-hinnat (filter #(= :komponentti (::hinta/ryhma %)) hinnat)
        muut-tyot (filter #(= :tyo (::hinta/ryhma %)) hinnat)
        muut (filter #(= :muu (::hinta/ryhma %)) hinnat)
        ;; TODO: Liitä otsikoihin toimenpidekoodien otsikot
        hintojen-otsikot (map (juxt ::hinta/otsikko ::hinta/yksikko) (remove ::m/poistettu? hinnat))]
    (and (every? #(and (::tyo/toimenpidekoodi-id %)
                       (::tyo/maara %))
                 tyot)
         (every? #(and (not-empty (::hinta/otsikko %))
                       (::hinta/maara %)
                       (::hinta/yksikkohinta %)
                       (::hinta/yksikko %))
                 muut-tyot)
         (every? #(and (not-empty (::hinta/otsikko %))
                       (::hinta/maara %)
                       (::hinta/yksikkohinta %)
                       (::hinta/yksikko %))
                 komponenttien-hinnat)
         (every? #(and (not-empty (::hinta/otsikko %))
                       (::hinta/summa %))
                 muut)
         (or (empty? hintojen-otsikot)
             (apply distinct? hintojen-otsikot)))))

(defn hinnoiteltava-toimenpide [app]
  (some
    #(when (= (get-in app [:hinnoittele-toimenpide ::to/id])
              (::to/id %))
       %)
    (:toimenpiteet app)))

(extend-protocol tuck/Event

  Nakymassa?
  (process-event [{nakymassa? :nakymassa?} app]
    (assoc app :nakymassa? nakymassa?
               :karttataso-nakyvissa? nakymassa?))

  PaivitaValinnat
  ;; Valintojen päivittäminen laukaisee aina myös kantahaun uusimmilla valinnoilla (ellei ole jo käynnissä),
  ;; jotta näkymä pysyy synkassa valintojen kanssa
  (process-event [{tiedot :tiedot} app]
    (let [uudet-valinnat (merge (:valinnat app)
                                (select-keys tiedot jaettu/valintojen-avaimet))
          haku (tuck/send-async! ->HaeToimenpiteet)]
      (go (haku uudet-valinnat))
      (assoc app :valinnat uudet-valinnat)))

  SiirraValitutKokonaishintaisiin
  (process-event [_ app]
    (jaettu/siirra-valitut! :siirra-toimenpiteet-kokonaishintaisiin app))

  TyhjennaSuunnitellutTyot
  (process-event [_ app]
    (assoc app :suunnitellut-tyot nil))

  HaeSuunnitellutTyot
  (process-event [_ app]
    (let [urakka-id (get-in app [:valinnat :urakka-id])]
      (if (and (not (:suunniteltujen-toiden-haku-kaynnissa? app)) (some? urakka-id))
        (do (tuck-tyokalut/palvelukutsu :yksikkohintaiset-tyot
                                        {:urakka urakka-id}
                                        {:onnistui ->SuunnitellutTyotHaettu
                                         :epaonnistui ->SuunnitellutTyotEiHaettu})
            (assoc app :suunniteltujen-toiden-haku-kaynnissa? true))
        app)))

  SuunnitellutTyotHaettu
  (process-event [{vastaus :vastaus} app]
    (assoc app :suunnitellut-tyot vastaus
               :suunniteltujen-toiden-haku-kaynnissa? false))

  SuunnitellutTyotEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Suunniteltujen töiden haku epäonnistui!" :danger)
    (assoc app :suunniteltujen-toiden-haku-kaynnissa? false))

  HaeToimenpiteet
  (process-event [{valinnat :valinnat} app]
    (if (and (not (:toimenpiteiden-haku-kaynnissa? app))
             (some? (:urakka-id valinnat)))
      (do (tuck-tyokalut/palvelukutsu :hae-yksikkohintaiset-toimenpiteet
                                      (jaettu/toimenpiteiden-hakukyselyn-argumentit valinnat)
                                      {:onnistui ->ToimenpiteetHaettu
                                       :epaonnistui ->ToimenpiteetEiHaettu})
          (assoc app :toimenpiteiden-haku-kaynnissa? true))
      app))

  ToimenpiteetHaettu
  (process-event [{toimenpiteet :toimenpiteet} app]
    (let [turvalaitteet-kartalle (tuck/send-async! jaettu/->HaeToimenpiteidenTurvalaitteetKartalle)]
      (go (turvalaitteet-kartalle toimenpiteet))
      (assoc app :toimenpiteet (-> toimenpiteet
                                   hintaryhmattomat-toimenpiteet-valiaikaisiin-ryhmiin
                                   jaettu/toimenpiteet-aikajarjestyksessa)
                 :toimenpiteiden-haku-kaynnissa? false)))

  ToimenpiteetEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Toimenpiteiden haku epäonnistui!" :danger)
    (assoc app :toimenpiteiden-haku-kaynnissa? false))

  UudenHintaryhmanLisays?
  (process-event [{lisays-auki? :lisays-auki?} app]
    (assoc app :uuden-hintaryhman-lisays? lisays-auki?))

  UudenHintaryhmanNimeaPaivitetty
  (process-event [{nimi :nimi} app]
    (assoc app :uusi-hintaryhma nimi))

  LuoHintaryhma
  (process-event [{nimi :nimi} app]
    (if-not (:hintaryhman-tallennus-kaynnissa? app)
      (do (tuck-tyokalut/palvelukutsu :luo-hinnoittelu
                                      {::h/nimi nimi
                                       ::urakka/id (get-in app [:valinnat :urakka-id])}
                                      {:onnistui ->HintaryhmaLuotu
                                       :epaonnistui ->HintaryhmaEiLuotu})
          (assoc app :hintaryhman-tallennus-kaynnissa? true))
      app))

  HintaryhmaLuotu
  (process-event [{vastaus :vastaus} app]
    (-> app
        (update :hintaryhmat conj vastaus)
        (assoc :hintaryhman-tallennus-kaynnissa? false
               :uusi-hintaryhma nil
               :uuden-hintaryhman-lisays? false)))

  HintaryhmaEiLuotu
  (process-event [_ app]
    (viesti/nayta! "Tilauksen tallennus epäonnistui!" :danger)
    (assoc app :hintaryhman-tallennus-kaynnissa? false
               :uusi-hintaryhma nil
               :uuden-hintaryhman-lisays? false))

  HaeHintaryhmat
  (process-event [_ app]
    (if-not (:hintaryhmien-haku-kaynnissa? app)
      (do (tuck-tyokalut/palvelukutsu :hae-hintaryhmat
                                      {::urakka/id (get-in app [:valinnat :urakka-id])}
                                      {:onnistui ->HintaryhmatHaettu
                                       :epaonnistui ->HintaryhmatEiHaettu})
          (assoc app :hintaryhmien-haku-kaynnissa? true))
      app))

  HintaryhmatHaettu
  (process-event [{vastaus :vastaus} app]
    (assoc app :hintaryhmat vastaus
               :hintaryhmien-haku-kaynnissa? false))

  HintaryhmatEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Tilauksien haku epäonnistui!" :danger)
    (assoc app :hintaryhmien-haku-kaynnissa? false))

  ValitseHintaryhma
  (process-event [{hintaryhma :hintaryhma} app]
    (assoc app :valittu-hintaryhma hintaryhma))

  LiitaValitutHintaryhmaan
  (process-event [{hintaryhma :hintaryhma valitut :valitut} app]
    (if-not (:hintaryhmien-liittaminen-kaynnissa? app)
      (do (tuck-tyokalut/palvelukutsu :liita-toimenpiteet-hinnoitteluun
                                      {::to/idt (map ::to/id valitut)
                                       ::h/id (::h/id hintaryhma)
                                       ::urakka/id (get-in app [:valinnat :urakka-id])}
                                      {:onnistui ->ValitutLiitettyHintaryhmaan
                                       :epaonnistui ->ValitutEiLiitettyHintaryhmaan})
          (assoc app :hintaryhmien-liittaminen-kaynnissa? true))
      app))

  ValitutLiitettyHintaryhmaan
  (process-event [_ app]
    (let [toimenpidehaku (tuck/send-async! ->HaeToimenpiteet)
          hintaryhmahaku (tuck/send-async! ->HaeHintaryhmat)]
      (go (toimenpidehaku (:valinnat app)))
      (go (hintaryhmahaku nil)) ;; Tarvitaan tieto siitä, miten tieto tyhjistä hintaryhmistä muuttuu
      (assoc app :hintaryhmien-liittaminen-kaynnissa? false)))

  ValitutEiLiitettyHintaryhmaan
  (process-event [_ app]
    (viesti/nayta! "Toimenpiteiden liittäminen tilaukseen epäonnistui!" :danger)
    (assoc app :hintaryhmien-liittaminen-kaynnissa? false))

  PoistaHintaryhmat
  (process-event [{hintaryhma-idt :hintaryhma-idt} app]
    (if-not (:hintaryhmien-poisto-kaynnissa? app)
      (do (tuck-tyokalut/palvelukutsu :poista-tyhjat-hinnoittelut
                                      {::h/urakka-id (get-in app [:valinnat :urakka-id])
                                       ::h/idt hintaryhma-idt}
                                      {:onnistui ->HintaryhmatPoistettu
                                       :epaonnistui ->HintaryhmatEiPoistettu})
          (assoc app :hintaryhmien-poisto-kaynnissa? true))
      app))

  HintaryhmatPoistettu
  (process-event [{vastaus :vastaus} app]
    (assoc app :hintaryhmien-poisto-kaynnissa? false
               :hintaryhmat (h/hinnoittelut-ilman (:hintaryhmat app)
                                                  (::h/idt vastaus))))

  HintaryhmatEiPoistettu
  (process-event [_ app]
    (viesti/nayta! "Tilauksen poisto epäonnistui!" :danger)
    (assoc app :hintaryhmien-poisto-kaynnissa? false))

  AloitaToimenpiteenHinnoittelu
  (process-event [{toimenpide-id :toimenpide-id} app]
    (let [hinnoiteltava-toimenpide (to/toimenpide-idlla (:toimenpiteet app) toimenpide-id)
          toimenpiteen-oma-hinnoittelu (::to/oma-hinnoittelu hinnoiteltava-toimenpide)
          hinnat (or (::h/hinnat toimenpiteen-oma-hinnoittelu) [])
          tyot (or (::h/tyot toimenpiteen-oma-hinnoittelu) [])]
      (assoc app :hinnoittele-toimenpide
                 {::to/id toimenpide-id
                  ::h/hinnat (toimenpiteen-hintakentat hinnat)
                  ::h/tyot tyot})))

  PeruToimenpiteenHinnoittelu
  (process-event [_ app]
    (assoc app :toimenpiteen-hinnoittelun-tallennus-kaynnissa? false
               :hinnoittele-toimenpide alustettu-toimenpiteen-hinnoittelu))

  AsetaHintakentalleTiedot
  (process-event [{tiedot :tiedot} app]
    (assoc-in app [:hinnoittele-toimenpide ::h/hinnat]
              (hinta/paivita-hintajoukon-hinnan-tiedot-idlla (get-in app [:hinnoittele-toimenpide
                                                                          ::h/hinnat]) tiedot)))

  AsetaTyorivilleTiedot
  (process-event [{tiedot :tiedot} app]
    (assoc-in app [:hinnoittele-toimenpide ::h/tyot]
              (tyo/paivita-tyon-tiedot-idlla (get-in app [:hinnoittele-toimenpide ::h/tyot])
                                             tiedot)))

  AsetaKomponenttirivilleTiedot
  (process-event [{tiedot :tiedot} app]
    (assoc-in app [:hinnoittele-toimenpide ::h/hinnat]
              (hinta/paivita-hintajoukon-hinnan-tiedot-idlla
                (get-in app [:hinnoittele-toimenpide
                             ::h/hinnat])
                tiedot)))

  LisaaHinnoiteltavaTyorivi
  (process-event [_ app]
    (let [tyot (get-in app [:hinnoittele-toimenpide ::h/tyot])
          tyo-idt (map ::tyo/id tyot)
          seuraava-vapaa-id (dec (apply min (conj tyo-idt 0)))
          paivitetyt-tyot (conj tyot (tyokentta {::tyo/id seuraava-vapaa-id}))]
      (assoc-in app [:hinnoittele-toimenpide ::h/tyot] paivitetyt-tyot)))

  LisaaHinnoiteltavaKomponenttirivi
  (process-event [_ app]
    (let [hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
          hinta-idt (map ::hinta/id hinnat)
          seuraava-vapaa-id (dec (apply min (conj hinta-idt 0)))
          paivitetyt-hinnat (conj hinnat (hintakentta
                                           {::hinta/id seuraava-vapaa-id
                                            ::hinta/otsikko ""
                                            ::hinta/ryhma :komponentti}))]
      (assoc-in app [:hinnoittele-toimenpide ::h/hinnat] paivitetyt-hinnat)))

  LisaaMuuKulurivi
  (process-event [_ app]
    (let [hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
          hinta-idt (map ::hinta/id hinnat)
          seuraava-vapaa-id (dec (apply min (conj hinta-idt 0)))
          paivitetyt-hinnat (conj hinnat (hintakentta
                                           {::hinta/id seuraava-vapaa-id
                                            ::hinta/otsikko ""
                                            ::hinta/summa 0
                                            ::hinta/ryhma :muu}))]
      (assoc-in app [:hinnoittele-toimenpide ::h/hinnat] paivitetyt-hinnat)))

  LisaaMuuTyorivi
  (process-event [_ app]
    (let [hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
          hinta-idt (map ::hinta/id hinnat)
          seuraava-vapaa-id (dec (apply min (conj hinta-idt 0)))
          paivitetyt-hinnat (conj hinnat (hintakentta
                                           {::hinta/id seuraava-vapaa-id
                                            ::hinta/otsikko ""
                                            ::hinta/summa nil
                                            ::hinta/ryhma :tyo}))]
      (assoc-in app [:hinnoittele-toimenpide ::h/hinnat] paivitetyt-hinnat)))

  PoistaHinnoiteltavaTyorivi
  (process-event [{tiedot :tiedot} app]
    (let [id (::tyo/id tiedot)
          tyot (get-in app [:hinnoittele-toimenpide ::h/tyot])
          ;; TODO Vaikuttaa pitkälti samalta kuin hintarivin poisto. Tee yhteinen funkkari?
          paivitetyt-tyot
          (if-not (id-olemassa? id)
            ;; Uusi lisätty rivi poistetaan kokonaan
            (filterv #(not= (::tyo/id %) id) tyot)
            ;; Kannassa oleva rivi merkitään poistetuksi
            (mapv #(if (= (::tyo/id %) id)
                     (assoc % ::m/poistettu? true)
                     %)
                  tyot))]
      (assoc-in app [:hinnoittele-toimenpide ::h/tyot] paivitetyt-tyot)))

  PoistaMuuKulurivi
  (process-event [{rivi :rivi} app]
    (let [id (::hinta/id rivi)
          hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
          paivitetyt-hinnat
          (if-not (id-olemassa? id)
            ;; Uusi lisätty rivi poistetaan kokonaan
            (filterv #(not= (::hinta/id %) id) hinnat)
            ;; Kannassa oleva rivi merkitään poistetuksi
            (mapv #(if (= (::hinta/id %) id)
                     (assoc % ::m/poistettu? true)
                     %)
                  hinnat))]
      (assoc-in app [:hinnoittele-toimenpide ::h/hinnat] paivitetyt-hinnat)))

  PoistaMuuTyorivi
  (process-event [{tiedot :tiedot} app]
    (let [id (::hinta/id tiedot)
          hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
          paivitetyt-hinnat
          (if-not (id-olemassa? id)
            ;; Uusi lisätty rivi poistetaan kokonaan
            (filterv #(not= (::hinta/id %) id) hinnat)
            ;; Kannassa oleva rivi merkitään poistetuksi
            (mapv #(if (= (::hinta/id %) id)
                     (assoc % ::m/poistettu? true)
                     %)
                  hinnat))]
      (assoc-in app [:hinnoittele-toimenpide ::h/hinnat] paivitetyt-hinnat)))

  PoistaHinnoiteltavaKomponenttirivi
  (process-event [{tiedot :tiedot} app]
    (let [id (::hinta/id tiedot)
          hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
          paivitetyt-hinnat
          (if-not (id-olemassa? id)
            ;; Uusi lisätty rivi poistetaan kokonaan
            (filterv #(not= (::hinta/id %) id) hinnat)
            ;; Kannassa oleva rivi merkitään poistetuksi
            (mapv #(if (= (::hinta/id %) id)
                     (assoc % ::m/poistettu? true)
                     %)
                  hinnat))]
      (assoc-in app [:hinnoittele-toimenpide ::h/hinnat] paivitetyt-hinnat)))

  TallennaToimenpiteenHinnoittelu
  (process-event [{tiedot :tiedot} app]
    (if-not (:toimenpiteen-hinnoittelun-tallennus-kaynnissa? app)
      (do (tuck-tyokalut/palvelukutsu
            :tallenna-toimenpiteelle-hinta
            {::to/urakka-id (get-in app [:valinnat :urakka-id])
             ::to/id (get-in app [:hinnoittele-toimenpide ::to/id])
             ::h/tallennettavat-hinnat (get-in app [:hinnoittele-toimenpide ::h/hinnat])
             ::h/tallennettavat-tyot (get-in app [:hinnoittele-toimenpide ::h/tyot])}
            {:onnistui ->ToimenpiteenHinnoitteluTallennettu
             :epaonnistui ->ToimenpiteenHinnoitteluEiTallennettu})
          (assoc app :toimenpiteen-hinnoittelun-tallennus-kaynnissa? true))
      app))

  ToimenpiteenHinnoitteluTallennettu
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Hinnoittelu tallennettu!" :success)
    (let [paivitettava-toimenpide (to/toimenpide-idlla (:toimenpiteet app)
                                                       (get-in app [:hinnoittele-toimenpide ::to/id]))
          paivitetty-toimenpide (assoc paivitettava-toimenpide ::to/oma-hinnoittelu vastaus)
          paivitetyt-toimenpiteet (mapv
                                    (fn [toimenpide]
                                      (if (= (::to/id toimenpide) (::to/id paivitettava-toimenpide))
                                        paivitetty-toimenpide
                                        toimenpide))
                                    (:toimenpiteet app))]
      (assoc app :toimenpiteet paivitetyt-toimenpiteet
                 :toimenpiteen-hinnoittelun-tallennus-kaynnissa? false
                 :hinnoittele-toimenpide alustettu-toimenpiteen-hinnoittelu)))

  ToimenpiteenHinnoitteluEiTallennettu
  (process-event [_ app]
    (viesti/nayta! "Hinnoittelun tallennus epäonnistui!" :danger)
    (assoc app :toimenpiteen-hinnoittelun-tallennus-kaynnissa? false))

  AloitaHintaryhmanHinnoittelu
  (process-event [{hintaryhma-id :hintaryhma-id} app]
    (let [hinnoiteltava-hintaryhma (h/hinnoittelu-idlla (:hintaryhmat app) hintaryhma-id)
          hinnat (::h/hinnat hinnoiteltava-hintaryhma)]
      (assoc app :hinnoittele-hintaryhma
                 {::h/id hintaryhma-id
                  ::h/hinnat (hintaryhman-hintakentat hinnat)})))

  PeruHintaryhmanHinnoittelu
  (process-event [_ app]
    (assoc app :hintaryhman-hinnoittelun-tallennus-kaynnissa? false
               :hinnoittele-hintaryhma alustettu-hintaryhman-hinnoittelu))

  AsetaHintaryhmakentalleTiedot
  (process-event [{tiedot :tiedot} app]
    (assoc-in app [:hinnoittele-hintaryhma ::h/hinnat]
              (hinta/paivita-hintajoukon-hinnan-tiedot-otsikolla (get-in app [:hinnoittele-hintaryhma
                                                                              ::h/hinnat]) tiedot)))

  TallennaHintaryhmanHinnoittelu
  (process-event [{tiedot :tiedot} app]
    (if-not (:hintaryhman-hinnoittelun-tallennus-kaynnissa? app)
      (do (tuck-tyokalut/palvelukutsu :tallenna-hintaryhmalle-hinta
                                      {::ur/id (get-in app [:valinnat :urakka-id])
                                       ::h/id (get-in app [:hinnoittele-hintaryhma ::h/id])
                                       ::h/tallennettavat-hinnat (mapv
                                                                   (fn [hinta]
                                                                     (merge
                                                                       (when-let [id (::hinta/id hinta)]
                                                                         {::hinta/id id})
                                                                       {::hinta/otsikko (::hinta/otsikko hinta)
                                                                        ::hinta/summa (::hinta/summa hinta)
                                                                        ::hinta/ryhma :muu
                                                                        ::hinta/yleiskustannuslisa (::hinta/yleiskustannuslisa hinta)}))
                                                                   (get-in app [:hinnoittele-hintaryhma ::h/hinnat]))}
                                      {:onnistui ->HintaryhmanHinnoitteluTallennettu
                                       :epaonnistui ->HintaryhmanHinnoitteluEiTallennettu})
          (assoc app :hintaryhman-hinnoittelun-tallennus-kaynnissa? true))
      app))

  HintaryhmanHinnoitteluTallennettu
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Hinnoittelu tallennettu!" :success)
    (assoc app :hintaryhmat vastaus
               :hintaryhman-hinnoittelun-tallennus-kaynnissa? false
               :hinnoittele-hintaryhma alustettu-hintaryhman-hinnoittelu))

  HintaryhmanHinnoitteluEiTallennettu
  (process-event [_ app]
    (viesti/nayta! "Hinnoittelun tallennus epäonnistui!" :danger)
    (assoc app :hintaryhman-hinnoittelun-tallennus-kaynnissa? false))

  KorostaHintaryhmaKartalla
  (process-event [{hintaryhma :hintaryhma} {:keys [toimenpiteet] :as app}]
    (let [korostettavat-turvalaitteet (->>
                                        toimenpiteet
                                        (filter #(= (::to/hintaryhma-id %) (::h/id hintaryhma)))
                                        (map (comp ::tu/turvalaitenro ::to/turvalaite))
                                        (into #{}))]
      (-> (jaettu/korosta-kartalla korostettavat-turvalaitteet app)
          (assoc :korostettu-hintaryhma (::h/id hintaryhma)))))

  PoistaHintaryhmanKorostus
  (process-event [_ app]
    (->> app
         (poista-hintaryhmien-korostus)
         (jaettu/korosta-kartalla nil))))