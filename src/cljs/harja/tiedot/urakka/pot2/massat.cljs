(ns harja.tiedot.urakka.pot2.massat
  "UI controlleri pot2 massoille"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<!]]
            [harja.loki :refer [log tarkkaile!]]
            [tuck.core :refer [process-event] :as tuck]
            [harja.tyokalut.tuck :as tuck-apurit]
            [harja.domain.pot2 :as pot2-domain]
            [harja.pvm :as pvm]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.tiedot.navigaatio :as nav])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(def pot2-nakymassa? (atom false))
(def nayta-materiaalikirjasto? (atom false))

(defrecord AlustaTila [])
(defrecord UusiMassa [avaa-massa-lomake?])
(defrecord NaytaModal [avataanko?])
(defrecord NaytaListaus [nayta])

(defrecord TallennaLomake [data])
(defrecord TallennaMassaOnnistui [vastaus])
(defrecord TallennaMassaEpaonnistui [vastaus])
(defrecord TyhjennaLomake [data])
(defrecord PaivitaLomake [data])
(defrecord PaivitaAineenTieto [polku arvo])
(defrecord LisaaSideaine [sideaineen-kayttotapa])

;; Haut
(defrecord HaePot2Massat [])
(defrecord HaePot2MassatOnnistui [vastaus])
(defrecord HaePot2MassatEpaonnistui [vastaus])

(defrecord HaeKoodistot [])
(defrecord HaeKoodistotOnnistui [vastaus])
(defrecord HaeKoodistotEpaonnistui [vastaus])


(defn- hae-massat [_]
  (tuck-apurit/post! :hae-urakan-pot2-massat
                     {:urakka-id (-> @tila/tila :yleiset :urakka :id)}
                     {:onnistui ->HaePot2MassatOnnistui
                      :epaonnistui ->HaePot2MassatEpaonnistui}))

(extend-protocol tuck/Event

  AlustaTila
  (process-event [_ {:as app}]
    (assoc app :pot2-massa-lomake nil))

  NaytaModal
  (process-event [{avataanko? :avataanko?} app]
    (do
      (reset! nayta-materiaalikirjasto? avataanko?)
      app))

  HaePot2Massat
  (process-event [_ app]
    (-> app
        (hae-massat)))

  HaePot2MassatOnnistui
  (process-event [{vastaus :vastaus} {:as app}]
    (assoc app :massat vastaus))

  HaePot2MassatEpaonnistui
  (process-event [{vastaus :vastaus} app]
    app)

  HaeKoodistot
  (process-event [_ app]
    (if-not (:materiaalikoodistot app)
      (-> app
         (tuck-apurit/post! :hae-pot2-koodistot
                            {:urakka-id (-> @tila/tila :yleiset :urakka :id)}
                            {:onnistui ->HaeKoodistotOnnistui
                             :epaonnistui ->HaeKoodistotEpaonnistui}))
      app))

  HaeKoodistotOnnistui
  (process-event [{vastaus :vastaus} {:as app}]
    (assoc app :materiaalikoodistot vastaus))

  HaeKoodistotEpaonnistui
  (process-event [{vastaus :vastaus} app]
    app)

  UusiMassa
  (process-event [{avaa-massa-lomake? :avaa-massa-lomake?} app]
    (js/console.log "Uusi massa lomake avataan" avaa-massa-lomake?)
    (assoc app :avaa-massa-lomake? avaa-massa-lomake?))

  NaytaListaus
  (process-event [{nayta :nayta} app]
    (js/console.log "NaytaListaus" nayta)
    (assoc app :pot2-massat? nayta))

  PaivitaLomake
  (process-event [{data :data} app]
    (let [uusiapp (-> app
                      (update :pot2-massa-lomake merge data))]
      uusiapp))

  PaivitaAineenTieto
  (process-event [{polku :polku arvo :arvo} app]
    (log "PaivitaAineenTieto polku " (pr-str polku), " arvo:" (pr-str arvo))
    (let [uusiapp (assoc-in app
                    (vec (cons :pot2-massa-lomake polku)) arvo)]
    uusiapp))

  LisaaSideaine
  (process-event [{sideaineen-kayttotapa :sideaineen-kayttotapa} app]
    (let [aineiden-lkm
          (count (get-in app
                         [:pot2-massa-lomake :sideaineet sideaineen-kayttotapa :aineet]))]
      (assoc-in app
                [:pot2-massa-lomake :sideaineet sideaineen-kayttotapa :aineet aineiden-lkm]
                {:tyyppi nil :pitoisuus nil})))

  TallennaLomake
  (process-event [{data :data} app]
    (let [massa (:pot2-massa-lomake app)
          lisa-aineet (get-in app [:pot2-massa-lomake :lisa-aineet]) ;; Todo: Sisältää nyt tasan yhden arvon setissä
          lisa-aineet [{:pitoisuus 0.2M :nimi (first lisa-aineet)}]
          sideaineet (get-in app [:pot2-massa-lomake :sideaineet]) ;; Todo: Sisältää nyt tasan yhden arvon setissä
          sideaineet [{:pitoisuus 5M :tyyppi (first sideaineet) :lopputuote? true}]
          _ (js/console.log "TallennaLomake data" (pr-str data))
          _ (js/console.log "TallennaLomake massa" (pr-str massa))]
      (tuck-apurit/post! :tallenna-urakan-pot2-massa
                         (-> massa
                             (assoc ::pot2-domain/urakka-id (-> @tila/tila :yleiset :urakka :id))
                             (assoc :lisa-aineet lisa-aineet)
                             (assoc :sideaineet sideaineet))
                         {:onnistui ->TallennaMassaOnnistui
                          :epaonnistui ->TallennaMassaEpaonnistui}))
    app)

  TallennaMassaOnnistui
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Massa tallennettu!")
    (hae-massat nil)
    (assoc app :avaa-massa-lomake? false))

  TallennaMassaEpaonnistui
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Massan tallennus epäonnistui!" :danger)
    app)

  TyhjennaLomake
  (process-event [{data :data} app]
    (js/console.log "TyhjennaLomake" (pr-str data))
    (-> app
        (assoc :pot2-massa-lomake nil
               :avaa-massa-lomake? false))))