(ns harja.tiedot.vesivaylat.urakka.toimenpiteet.jaettu
  (:require [reagent.core :refer [atom]]
            [tuck.core :as tuck]
            [harja.loki :refer [log error]]
            [harja.domain.vesivaylat.toimenpide :as to]
            [harja.domain.urakka :as ur]
            [harja.domain.vesivaylat.vayla :as va]
            [harja.domain.vesivaylat.turvalaite :as tu]
            [cljs.core.async :refer [<!]]
            [harja.tyokalut.spec-apurit :as spec-apurit]
            [harja.ui.viesti :as viesti]
            [harja.asiakas.kommunikaatio :as k]
            [harja.tyokalut.tuck :as tuck-tyokalut])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(def valintojen-avaimet [:urakka-id :sopimus-id :aikavali
                         :vaylatyyppi :vayla
                         :tyolaji :tyoluokka :toimenpide
                         :vain-vikailmoitukset?])

(defn arvot-pudotusvalikko-valinnoiksi [kartta]
  (into [nil] (distinct (vals kartta))))

(defn- toimenpiteet-tyolajilla [toimenpiteet tyolaji]
  (filterv #(= (::to/tyolaji %) tyolaji) toimenpiteet))

(defn kaikki-valittu? [tyolajin-toimenpiteet]
  (every? true? (map :valittu? tyolajin-toimenpiteet)))

(defn mitaan-ei-valittu? [tyolajin-toimenpiteet]
  (every? (comp not true?)
          (map :valittu? tyolajin-toimenpiteet)))

(defn valitut-toimenpiteet [toimenpiteet]
  (filter :valittu? toimenpiteet))

(defn poista-toimenpiteet [toimenpiteet poistettavat-toimenpide-idt]
  (filter #(not (poistettavat-toimenpide-idt (::to/id %))) toimenpiteet))

(defn valinnan-tila [tyolajin-toimenpiteet]
  (cond (kaikki-valittu? tyolajin-toimenpiteet) true
        (mitaan-ei-valittu? tyolajin-toimenpiteet) false
        :default :harja.ui.kentat/indeterminate))

(defn toimenpiteiden-toiminto-suoritettu [toimenpiteiden-lkm toiminto]
  (str toimenpiteiden-lkm " "
       (if (= 1 toimenpiteiden-lkm) "toimenpide" "toimenpidettä")
       " " toiminto "."))

(defn toimenpiteiden-hakukyselyn-argumentit [{:keys [urakka-id sopimus-id aikavali
                                                     vaylatyyppi vayla
                                                     tyolaji tyoluokka toimenpide
                                                     vain-vikailmoitukset?] :as valinnat}]
  (spec-apurit/poista-nil-avaimet {::to/urakka-id urakka-id
                                   ::to/sopimus-id sopimus-id
                                   ::va/vaylatyyppi vaylatyyppi
                                   ::to/vayla-id vayla
                                   ::to/reimari-tyolaji (when tyolaji (to/reimari-tyolaji-avain->koodi tyolaji))
                                   ::to/reimari-tyoluokat (when tyoluokka (to/reimari-tyoluokka-avain->koodi tyoluokka))
                                   ::to/reimari-toimenpidetyypit (when toimenpide (to/reimari-toimenpidetyyppi-avain->koodi toimenpide))
                                   :alku (first aikavali)
                                   :loppu (second aikavali)
                                   :vikailmoitukset? vain-vikailmoitukset?}))

(defn joku-valittu? [toimenpiteet]
  (some :valittu? toimenpiteet))

(defn yhdista-tilat! [mun-tila sen-tila]
  (swap! mun-tila update :valinnat #(merge % (:valinnat @sen-tila)))
  mun-tila)

(defn toimenpiteet-aikajarjestyksessa [toimenpiteet]
  (sort-by ::to/pvm toimenpiteet))

(defrecord ValitseToimenpide [tiedot toimenpiteet])
(defrecord ValitseTyolaji [tiedot toimenpiteet])
(defrecord ValitseVayla [tiedot toimenpiteet])
(defrecord AsetaInfolaatikonTila [tunniste uusi-tila])
(defrecord ToimenpiteetSiirretty [toimenpiteet])
(defrecord ToimenpiteetEiSiirretty [])
(defrecord LisaaToimenpiteelleLiite [tiedot])
(defrecord LiiteLisatty [vastaus tiedot])
(defrecord LiiteEiLisatty [])
(defrecord PoistaToimenpiteenLiite [tiedot])
(defrecord LiitePoistettu [vastaus tiedot])
(defrecord LiiteEiPoistettu [])

(defn siirra-valitut! [palvelu app]
  (tuck-tyokalut/palvelukutsu palvelu
                              {::to/urakka-id (get-in app [:valinnat :urakka-id])
                               ::to/idt (set (map ::to/id (valitut-toimenpiteet (:toimenpiteet app))))}
                              {:onnistui ->ToimenpiteetSiirretty
                               :epaonnistui ->ToimenpiteetEiSiirretty})
  (assoc app :siirto-kaynnissa? true))

(extend-protocol tuck/Event
  ValitseToimenpide
  (process-event [{tiedot :tiedot listan-toimenpiteet :toimenpiteet} {:keys [toimenpiteet] :as app}]
    (let [toimenpide-id (:id tiedot)
          valinta (:valinta tiedot)
          paivitetty-toimenpide (-> (to/toimenpide-idlla listan-toimenpiteet toimenpide-id)
                                    (assoc :valittu? valinta))]
      (assoc app :toimenpiteet (toimenpiteet-aikajarjestyksessa
                                 (mapv #(if (= (::to/id %) toimenpide-id) paivitetty-toimenpide %)
                                       toimenpiteet)))))

  ValitseTyolaji
  (process-event [{tiedot :tiedot listan-toimenpiteet :toimenpiteet} {:keys [toimenpiteet] :as app}]
    (let [tyolaji (:tyolaji tiedot)
          valinta (:valinta tiedot)
          paivitetyt-toimenpiteet (mapv #(if (= (::to/tyolaji %) tyolaji)
                                           (assoc % :valittu? valinta)
                                           %)
                                        listan-toimenpiteet)
          paivitetyt-idt (into #{} (map ::to/id paivitetyt-toimenpiteet))
          paivittamattomat (remove (comp paivitetyt-idt ::to/id) toimenpiteet)]
      (assoc app :toimenpiteet (toimenpiteet-aikajarjestyksessa
                                 (concat paivitetyt-toimenpiteet paivittamattomat)))))

  ValitseVayla
  (process-event [{tiedot :tiedot listan-toimenpiteet :toimenpiteet} {:keys [toimenpiteet] :as app}]
    (let [vayla-id (:vayla-id tiedot)
          valinta (:valinta tiedot)
          paivitetyt-toimenpiteet (mapv #(if (= (get-in % [::to/vayla ::va/id]) vayla-id)
                                           (assoc % :valittu? valinta)
                                           %)
                                        listan-toimenpiteet)
          paivitetyt-idt (into #{} (map ::to/id paivitetyt-toimenpiteet))
          paivittamattomat (remove (comp paivitetyt-idt ::to/id) toimenpiteet)]
      (assoc app :toimenpiteet (toimenpiteet-aikajarjestyksessa
                                 (concat paivitetyt-toimenpiteet paivittamattomat)))))

  AsetaInfolaatikonTila
  (process-event [{tunniste :tunniste
                   uusi-tila :uusi-tila} app]
    (if tunniste
      (assoc app :infolaatikko-nakyvissa (merge (:infolaatikko-nakyvissa app)
                                                {tunniste uusi-tila}))
      app))

  ToimenpiteetSiirretty
  (process-event [{toimenpiteet :toimenpiteet} app]
    (viesti/nayta! (toimenpiteiden-toiminto-suoritettu (count toimenpiteet) "siirretty") :success)
    (assoc app :toimenpiteet (toimenpiteet-aikajarjestyksessa
                               (poista-toimenpiteet (:toimenpiteet app) toimenpiteet))
               :siirto-kaynnissa? false))

  ToimenpiteetEiSiirretty
  (process-event [_ app]
    (viesti/nayta! "Toimenpiteiden siirto epäonnistui!" :danger)
    (assoc app :siirto-kaynnissa? false))

  LisaaToimenpiteelleLiite
  (process-event [{tiedot :tiedot} app]
    ;; TODO TESTI!
    (if (not (:liitteen-lisays-kaynnissa? app))
      (do (tuck-tyokalut/palvelukutsu :lisaa-toimenpiteelle-liite
                                      {::to/urakka-id (get-in app [:valinnat :urakka-id])
                                       ::to/liite-id (get-in tiedot [:liite :id])
                                       ::to/id (::to/id tiedot)}
                                      {:onnistui ->LiiteLisatty
                                       :onnistui-parametrit [tiedot]
                                       :epaonnistui ->LiiteEiLisatty})
          (assoc app :liitteen-lisays-kaynnissa? true))
      app))

  LiiteLisatty
  (process-event [{vastaus :vastaus tiedot :tiedot} app]
    (let [liite (:liite tiedot)
          toimenpide-id (::to/id tiedot)]
      (assoc app :toimenpiteet (map (fn [toimenpide]
                                      (if (= (::to/id toimenpide) toimenpide-id)
                                        (assoc toimenpide ::to/liitteet (conj (::to/liitteet toimenpide)
                                                                              liite))
                                        toimenpide))
                                    (:toimenpiteet app))
                 :liitteen-lisays-kaynnissa? false)))

  LiiteEiLisatty
  (process-event [_ app]
    ;; TODO TESTI!
    (viesti/nayta! "Liitteen lisäys epäonnistui!" :danger)
    (assoc app :liitteen-lisays-kaynnissa? false))

  PoistaToimenpiteenLiite
  (process-event [{tiedot :tiedot} app]
    ;; TODO TESTI!
    (if (not (:liitteen-poisto-kaynnissa? app))
      (do (tuck-tyokalut/palvelukutsu :poista-toimenpiteen-liite
                                      {::to/urakka-id (get-in app [:valinnat :urakka-id])
                                       ::to/liite-id (::to/liite-id tiedot)
                                       ::to/id (::to/id tiedot)}
                                      {:onnistui ->LiitePoistettu
                                       :onnistui-parametrit [tiedot]
                                       :epaonnistui ->LiiteEiPoistettu})
          (assoc app :liitteen-poisto-kaynnissa? true))
      app))

  LiitePoistettu
  (process-event [{vastaus :vastaus tiedot :tiedot} app]
    ;; TODO TESTI!
    (let [liite-id (::to/liite-id tiedot)
          toimenpide-id (::to/id tiedot)]
      (assoc app :toimenpiteet (map (fn [toimenpide]
                                      (if (= (::to/id toimenpide) toimenpide-id)
                                        (assoc toimenpide ::to/liitteet (filter #(not= (:id %) liite-id)
                                                                                (::to/liitteet toimenpide)))
                                        toimenpide))
                                    (:toimenpiteet app))
                 :liitteen-poisto-kaynnissa? false)))

  LiiteEiPoistettu
  (process-event [_ app]
    ;; TODO TESTI!
    (viesti/nayta! "Liitteen poistaminen epäonnistui!" :danger)
    (assoc app :liitteen-poisto-kaynnissa? false)))