(ns harja.tiedot.kanavat.hallinta.kohteiden-luonti
  (:require [reagent.core :refer [atom]]
            [tuck.core :as tuck]
            [cljs.core.async :as async :refer [<!]]
            [clojure.set :as set]
            [harja.pvm :as pvm]
            [harja.id :refer [id-olemassa?]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.viesti :as viesti]
            [harja.tyokalut.tuck :as tt]
            [namespacefy.core :refer [namespacefy]]

            [harja.domain.kanavat.kohdekokonaisuus :as kok]
            [harja.domain.kanavat.kohde :as kohde]
            [harja.domain.muokkaustiedot :as m]
            [harja.domain.urakka :as ur]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def tila (atom {:nakymassa? false
                 :kohteiden-haku-kaynnissa? false
                 :urakoiden-haku-kaynnissa? false
                 :kohdekokonaisuuslomake-auki? false
                 :liittaminen-kaynnissa? false
                 :kohderivit nil
                 :kohdekokonaisuudet nil
                 :valittu-urakka nil
                 :uudet-urakkaliitokset {}})) ; Key on vector, jossa [kohde-id urakka-id] ja arvo on boolean

;; Yleiset

(defrecord Nakymassa? [nakymassa?])

;; Grid

(defrecord AloitaUrakoidenHaku [])
(defrecord UrakatHaettu [urakat])
(defrecord UrakatEiHaettu [virhe])

(defrecord HaeKohteet [])
(defrecord KohteetHaettu [tulos])
(defrecord KohteetEiHaettu [virhe])

(defrecord ValitseUrakka [urakka])

(defrecord AsetaKohteenUrakkaliitos [kohde-id urakka-id liitetty?])
(defrecord PaivitaKohteidenUrakkaliitokset [])
(defrecord LiitoksetPaivitetty [tulos])
(defrecord LiitoksetEiPaivitetty [])

;; Lomake

(defrecord AvaaKohdekokonaisuusLomake [])
(defrecord SuljeKohdekokonaisuusLomake [])
(defrecord LisaaKohdekokonaisuuksia [tiedot])

(defrecord TallennaKohteet [])
(defrecord KohteetTallennettu [tulos])
(defrecord KohteetEiTallennettu [virhe])

(defrecord TallennaKohdekokonaisuudet [kokonaisuudet])
(defrecord KohdekokonaisuudetTallennettu [tulos])
(defrecord KohdekokonaisuudetEiTallennettu [virhe])

(defn hae-kanava-urakat! [tulos! fail!]
  (go
    (let [vastaus (<! (k/post! :hallintayksikot
                               {:liikennemuoto :vesi}))
          hy-id (:id (some
                       (fn [hy] (when (= (:nimi hy)
                                         "Kanavat ja avattavat sillat")
                                  hy))
                       vastaus))]
      (if (or (k/virhe? vastaus) (not hy-id))
        (fail! vastaus)

        (let [vastaus (<! (k/post! :hallintayksikon-urakat
                                   hy-id))]
          (if (k/virhe? vastaus)
            (fail! vastaus)

            (tulos! vastaus)))))))

(defn kohderivit [tulos]
  (mapcat
    (fn [kohdekokonaisuus]
      (map
        (fn [kohde]
          ;; Liitetään kohteelle kohdekokonaisuuden (kanavan) tiedot
          ;; Tarvitaan mm. ryhmittelyä varten.
          (-> kohde
              (assoc ::kok/id (::kok/id kohdekokonaisuus))
              (assoc ::kok/nimi (::kok/nimi kohdekokonaisuus))))
        (::kok/kohteet kohdekokonaisuus)))
    tulos))

(defn kohdekokonaisuudet [tulos]
  (-> (map #(select-keys % #{::kok/id ::kok/nimi}) tulos)
      set
      vec))

(defn kohteet-voi-tallentaa? [kohteet]
  (boolean
    (and (:kanava kohteet)
         (not-empty (:kohteet kohteet))
         (every?
           (fn [kohde]
             (and (::kohde/tyyppi kohde)))
           (:kohteet kohteet)))))

(defn kohdekokonaisuudet-voi-tallentaa? [kokonaisuudet]
  (boolean
    (and (every? #(not-empty (::kok/nimi %)) (remove #(and (not (id-olemassa? (::kok/id %)))
                                                           (:poistettu %))
                                                     kokonaisuudet)))))

(defn muokattavat-kohteet [app]
  (get-in app [:lomakkeen-tiedot :kohteet]))

(defn tallennusparametrit [lomake]
  (let [kanava-id (get-in lomake [:kanava ::kok/id])
        params (->> (:kohteet lomake)
                    (map #(assoc % ::kohde/kanava-id kanava-id))
                    (map #(set/rename-keys % {:id ::kohde/id
                                              :poistettu ::m/poistettu?}))
                    (map #(select-keys
                            %
                            [::kohde/nimi
                             ::kohde/id
                             ::kohde/kanava-id
                             ::kohde/tyyppi
                             ::m/poistettu?])))]
    params))

(defn kohteen-urakat [kohde]
  (str/join ", " (sort (map ::ur/nimi (::kohde/urakat kohde)))))

(defn kohde-kuuluu-urakkaan? [app kohde urakka]
  (let [kohteella-ui-urakkaliitos? (contains? (:uudet-urakkaliitokset app)
                                              [(::kohde/id kohde) (::ur/id urakka)])
        kohteen-ui-urakkaliitos (get (:uudet-urakkaliitokset app) [(::kohde/id kohde) (::ur/id urakka)])]
    ;; Ensisijaisesti tutkitaan käyttäjän asettamat, tallentamattomat linkit.
    ;; Sen jälkeen tutkitaan, kuuluuko kohde urakkaan kannan palauttaman datan perusteella
    (if kohteella-ui-urakkaliitos?
      kohteen-ui-urakkaliitos
      (boolean
        ((set (map ::ur/id (::kohde/urakat kohde))) (::ur/id urakka))))))

(defn poista-kohde [kohteet kohde]
  (into [] (disj (into #{} kohteet) kohde)))

(defn lopeta-liittaminen [app kohde-id urakka-id]
  (update app :liittaminen-kaynnissa
          (fn [kohde-ja-urakat]
            (when (kohde-ja-urakat kohde-id)
              (update kohde-ja-urakat kohde-id disj urakka-id)))))

(defn kohteet-haettu [app tulos]
  (-> app
      (assoc :kohderivit (kohderivit tulos))
      (assoc :kohdekokonaisuudet (kohdekokonaisuudet tulos))))

(defn kohteiden-lkm-kokonaisuudessa [{:keys [kohderivit] :as app} kokonaisuus]
  (let [kokonaisuus-id (::kok/id kokonaisuus)]
    (count (filter #(= (::kok/id %) kokonaisuus-id) kohderivit))))

(defn kokonaisuuden-voi-poistaa? [app kokonaisuus]
  (= 0 (kohteiden-lkm-kokonaisuudessa app kokonaisuus)))

(defn kohdekokonaisuudet-tallennusparametrit [kok]
  (as-> kok $
        (remove :koskematon $)
        (map #(set/rename-keys % {:id ::kok/id
                                  :poistettu ::m/poistettu?})
             $)))

(extend-protocol tuck/Event
  Nakymassa?
  (process-event [{nakymassa? :nakymassa?} app]
    (let [uudet-urakkaliitokset (if (false? nakymassa?)
                                  {}
                                  (:uudet-urakkaliitokset app))]
      (assoc app :nakymassa? nakymassa?
                 :uudet-urakkaliitokset uudet-urakkaliitokset)))

  HaeKohteet
  (process-event [_ app]
    (if-not (:kohteiden-haku-kaynnissa? app)
      (-> app
          (tt/get! :hae-kohdekokonaisuudet-ja-kohteet
                   {:onnistui ->KohteetHaettu
                    :epaonnistui ->KohteetEiHaettu})
          (assoc :kohteiden-haku-kaynnissa? true))

      app))

  KohteetHaettu
  (process-event [{tulos :tulos} app]
    (-> app
        (kohteet-haettu tulos)
        (assoc :kohteiden-haku-kaynnissa? false)))

  KohteetEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Kohteiden haku epäonnistui!" :danger)
    (-> app
        (assoc :kohteiden-haku-kaynnissa? false)))

  AvaaKohdekokonaisuusLomake
  (process-event [_ app]
    (assoc app :kohdekokonaisuuslomake-auki? true))

  SuljeKohdekokonaisuusLomake
  (process-event [_ app]
    (-> app
        (update :kohdekokonaisuudet #(filter (comp id-olemassa? ::kok/id) %))
        (update :kohdekokonaisuudet (fn [k] (map #(dissoc % :poistettu) k)))
        (assoc :kohdekokonaisuuslomake-auki? false)))

  LisaaKohdekokonaisuuksia
  (process-event [{tiedot :tiedot} app]
    (assoc-in app [:kohdekokonaisuudet] tiedot))

  TallennaKohteet
  (process-event [_ {tiedot :lomakkeen-tiedot :as app}]
    (if-not (:kohteiden-tallennus-kaynnissa? app)
      (-> app
          (tt/post! :lisaa-kanavalle-kohteita
                    (tallennusparametrit tiedot)
                    {:onnistui ->KohteetTallennettu
                     :epaonnistui ->KohteetEiTallennettu})

          (assoc :kohteiden-tallennus-kaynnissa? true))

      app))

  KohteetTallennettu
  (process-event [{tulos :tulos} app]
    (-> app
        (assoc :kohderivit (kohderivit tulos))
        (assoc :kohdekokonaisuudet (kohdekokonaisuudet tulos))
        (assoc :kohdekokonaisuuslomake-auki? false)
        (assoc :lomakkeen-tiedot nil)
        (assoc :kohteiden-tallennus-kaynnissa? false)))

  KohteetEiTallennettu
  (process-event [_ app]
    (viesti/nayta! "Kohteiden tallennus epäonnistui!" :danger)
    (-> app
        (assoc :kohteiden-tallennus-kaynnissa? false)))

  TallennaKohdekokonaisuudet
  (process-event [{kokonaisuudet :kokonaisuudet} app]
    (if-not (:kohdekokonaisuuksien-tallennus-kaynnissa? app)
      (-> app
          (tt/post! :tallenna-kohdekokonaisuudet
                    (kohdekokonaisuudet-tallennusparametrit kokonaisuudet)
                    {:onnistui ->KohdekokonaisuudetTallennettu
                     :epaonnistui ->KohdekokonaisuudetEiTallennettu})

          (assoc :kohdekokonaisuuksien-tallennus-kaynnissa? true))

      app))

  KohdekokonaisuudetTallennettu
  (process-event [{tulos :tulos} app]
    (-> app
        (kohteet-haettu tulos)
        (assoc :kohdekokonaisuuslomake-auki? false)
        (assoc :lomakkeen-tiedot nil)
        (assoc :kohdekokonaisuuksien-tallennus-kaynnissa? false)))

  KohdekokonaisuudetEiTallennettu
  (process-event [_ app]
    (viesti/nayta! "Kohteiden tallennus epäonnistui!" :danger)
    (-> app
        (assoc :kohdekokonaisuuksien-tallennus-kaynnissa? false)))

  AloitaUrakoidenHaku
  (process-event [_ app]
    (hae-kanava-urakat! (tuck/send-async! ->UrakatHaettu)
                        (tuck/send-async! ->UrakatEiHaettu))
    (assoc app :urakoiden-haku-kaynnissa? true))


  UrakatHaettu
  (process-event [{ur :urakat} app]
    (-> app
        (assoc :urakoiden-haku-kaynnissa? false)
        (assoc :urakat (as-> ur $
                             (map #(select-keys % [:id :nimi]) $)
                             (namespacefy $ {:ns :harja.domain.urakka})))))

  UrakatEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Virhe urakoiden haussa!" :danger)
    (assoc app :urakoiden-haku-kaynnissa? false))

  ValitseUrakka
  (process-event [{ur :urakka} app]
    (assoc app :valittu-urakka ur))

  AsetaKohteenUrakkaliitos
  (process-event [{kohde-id :kohde-id
                   urakka-id :urakka-id
                   liitetty? :liitetty?}
                  app]
    (let [liitokset (:uudet-urakkaliitokset app)]
      (assoc app :uudet-urakkaliitokset
                 (assoc liitokset
                   [kohde-id urakka-id]
                   liitetty?))))

  PaivitaKohteidenUrakkaliitokset
  (process-event [_ app]
    (tt/post! :liita-kohteet-urakkaan
              {:liitokset (:uudet-urakkaliitokset app)}
              {:onnistui ->LiitoksetPaivitetty
               :epaonnistui ->LiitoksetEiPaivitetty})
    (assoc app :liittaminen-kaynnissa? true))

  LiitoksetPaivitetty
  (process-event [{tulos :tulos} app]
    (viesti/nayta! "Kohteiden urakkaliitokset tallennettu." :success)
    (assoc app :liittaminen-kaynnissa? false
               :uudet-urakkaliitokset {}
               :kohderivit (kohderivit tulos)))

  LiitoksetEiPaivitetty
  (process-event [{kohde-id :kohde urakka-id :urakka} app]
    (viesti/nayta! "Virhe urakkaliitoksien tallennuksessa!" :danger)
    (assoc app :liittaminen-kaynnissa? false)))