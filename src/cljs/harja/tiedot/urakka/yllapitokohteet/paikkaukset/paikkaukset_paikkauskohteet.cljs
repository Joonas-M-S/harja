(ns harja.tiedot.urakka.yllapitokohteet.paikkaukset.paikkaukset-paikkauskohteet
  (:require [reagent.core :refer [atom] :as r]
            [tuck.core :as tuck]
            [harja.tyokalut.tuck :as tuck-apurit]
            [harja.loki :refer [log]]
            [harja.pvm :as pvm]
            [taoensso.timbre :as log]
            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.modal :as modal]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.kartta :as kartta-tiedot]
            [harja.tiedot.urakka.yllapitokohteet.paikkaukset.paikkaukset-paikkauskohteet-kartalle :as paikkauskohteet-kartalle]
            [harja.tiedot.urakka.urakka :as tila])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn virhe-modal [virhe otsikko]
  (modal/nayta!
    {:otsikko otsikko
     :otsikko-tyyli :virhe}
    (when virhe
      (doall
        (for [rivi virhe]
          ^{:key (hash rivi)}
          [:div
           [:b (get rivi "paikkauskohde")]
           (if (vector? (get rivi "virhe"))
             (doall
               (for [v (get rivi "virhe")]
                 ^{:key (str (hash v) (hash rivi))}
                 [:p (str v)]
                 ))
             [:p (str (get rivi "virhe"))])
           ]))
      )))

(defn- fmt-aikataulu [alkupvm loppupvm tila]
  (str
    (pvm/fmt-kuukausi-ja-vuosi-lyhyt alkupvm)
    " - "
    (pvm/fmt-p-k-v-lyhyt loppupvm)
    (when-not (= "valmis" tila)
      " (arv.)")))

(defn fmt-sijainti [tie alkuosa loppuosa alkuet loppuet]
  (str tie " - " alkuosa "/" alkuet " - " loppuosa "/" loppuet))

(defrecord AvaaLomake [lomake])
(defrecord SuljeLomake [])
(defrecord FiltteriValitseTila [uusi-tila])
(defrecord FiltteriValitseVuosi [uusi-vuosi])
(defrecord FiltteriValitseTyomenetelma [uusi-menetelma])
(defrecord TiedostoLadattu [vastaus])
(defrecord HaePaikkauskohteet [])
(defrecord HaePaikkauskohteetOnnistui [vastaus])
(defrecord HaePaikkauskohteetEpaonnistui [vastaus])
(defrecord PaivitaLomake [lomake])
(defrecord TallennaPaikkauskohde [paikkauskohde])
(defrecord TallennaPaikkauskohdeOnnistui [vastaus])
(defrecord TallennaPaikkauskohdeEpaonnistui [vastaus])
(defrecord TilaaPaikkauskohde [paikkauskohde])
(defrecord HylkaaPaikkauskohde [paikkauskohde])
(defrecord PoistaPaikkauskohde [paikkauskohde])
(defrecord PeruPaikkauskohteenTilaus [paikkauskohde])
(defrecord PeruPaikkauskohteenHylkays [paikkauskohde])
(defrecord UploadAttachment [input-html-element])


(defn- siivoa-ennen-lahetysta [lomake]
  (dissoc lomake
          :sijainti
          :harja.tiedot.urakka.urakka/validi?
          :harja.tiedot.urakka.urakka/validius))

(defn- hae-paikkauskohteet [urakka-id tila vuosi menetelma]
  (let [alkupvm (pvm/->pvm (str "1.1." vuosi))
        loppupvm (pvm/->pvm (str "31.12." vuosi))]
    (tuck-apurit/post! :paikkauskohteet-urakalle
                       {:urakka-id urakka-id
                        :tila tila
                        :alkupvm alkupvm
                        :loppupvm loppupvm
                        :tyomenetelmat #{menetelma}}
                       {:onnistui ->HaePaikkauskohteetOnnistui
                        :epaonnistui ->HaePaikkauskohteetEpaonnistui
                        :paasta-virhe-lapi? true})))

(defn- tallenna-paikkauskohde [paikkauskohde]
  (tuck-apurit/post! :tallenna-paikkauskohde-urakalle
                     paikkauskohde
                     {:onnistui ->TallennaPaikkauskohdeOnnistui
                      :epaonnistui ->TallennaPaikkauskohdeEpaonnistui
                      :paasta-virhe-lapi? true}))

(defn validoinnit
  ([avain lomake]
   (avain {:nimi [tila/ei-nil tila/ei-tyhja]
           :tyomenetelma [tila/ei-nil tila/ei-tyhja]
           :tie [tila/ei-nil tila/ei-tyhja tila/numero]
           :aosa [tila/ei-nil tila/ei-tyhja tila/numero]
           :losa [tila/ei-nil tila/ei-tyhja tila/numero]
           :aet [tila/ei-nil tila/ei-tyhja tila/numero]
           :let [tila/ei-nil tila/ei-tyhja tila/numero]
           :alkupvm [tila/ei-nil tila/ei-tyhja tila/paivamaara]
           :loppupvm [tila/ei-nil tila/ei-tyhja tila/paivamaara
                      (tila/silloin-kun #(not (nil? (:alkupvm lomake)))
                                        (fn [arvo]
                                          ;; Validointi vaatii "nil" vastauksen, kun homma on pielessä ja kentän arvon, kun kaikki on ok
                                          (when (pvm/ennen? (:alkupvm lomake) arvo)
                                            arvo)))]
           :suunniteltu-maara [tila/ei-nil tila/ei-tyhja tila/numero]
           :yksikko [tila/ei-nil tila/ei-tyhja]
           :suunniteltu-hinta [tila/ei-nil tila/ei-tyhja tila/numero]
           }))
  ([avain]
   (validoinnit avain {})))

(defn lomakkeen-validoinnit [lomake]
  [[:nimi] (validoinnit :nimi lomake)
   [:tyomenetelma] (validoinnit :tyomenetelma lomake)
   [:tie] (validoinnit :tie lomake)
   [:aosa] (validoinnit :aosa lomake)
   [:losa] (validoinnit :losa lomake)
   [:aet] (validoinnit :aet lomake)
   [:let] (validoinnit :let lomake)
   [:alkupvm] (validoinnit :alkupvm lomake)
   [:loppupvm] (validoinnit :loppupvm lomake)
   [:suunniteltu-maara] (validoinnit :suunniteltu-maara lomake)
   [:yksikko] (validoinnit :yksikko lomake)
   [:suunniteltu-hinta] (validoinnit :suunniteltu-hinta lomake)])

(defn- validoi-lomake [lomake]
  (apply tila/luo-validius-tarkistukset [[:nimi] (validoinnit :nimi lomake)
                                         [:tyomenetelma] (validoinnit :tyomenetelma lomake)
                                         [:tie] (validoinnit :tie lomake)
                                         [:aosa] (validoinnit :aosa lomake)
                                         [:losa] (validoinnit :losa lomake)
                                         [:aet] (validoinnit :aet lomake)
                                         [:let] (validoinnit :let lomake)
                                         [:alkupvm] (validoinnit :alkupvm lomake)
                                         [:loppupvm] (validoinnit :loppupvm lomake)
                                         [:suunniteltu-maara] (validoinnit :suunniteltu-maara lomake)
                                         [:yksikko] (validoinnit :yksikko lomake)
                                         [:suunniteltu-hinta] (validoinnit :suunniteltu-hinta lomake)]))

(extend-protocol tuck/Event

  UploadAttachment
  (process-event [{input-html-element :input-html-element} app]
    (let [urakka-id (-> @tila/yleiset :urakka :id)
          ;; TODO: siirrä tämä jotenki pois letistä ehkä.
          #_(k/laheta-tiedosto! "lue-paikkauskohteet-excelista" input-html-element urakka-id)]

      (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) (:valittu-tila app)
                           (:valittu-vuosi app) (:valittu-tyomenetelma app))
      app))

  AvaaLomake
  (process-event [{lomake :lomake} app]
    (let [{:keys [validoi] :as validoinnit} (validoi-lomake lomake)
          {:keys [validi? validius]} (validoi validoinnit lomake)]
      (-> app
          (assoc :lomake lomake)
          (assoc-in [:lomake ::tila/validius] validius)
          (assoc-in [:lomake ::tila/validi?] validi?))))

  SuljeLomake
  (process-event [_ app]
    (dissoc app :lomake))

  FiltteriValitseTila
  (process-event [{uusi-tila :uusi-tila} app]
    (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) uusi-tila (:valittu-vuosi app) (:valittu-tyomenetelma app))
    (assoc app :valittu-tila uusi-tila))

  FiltteriValitseVuosi
  (process-event [{uusi-vuosi :uusi-vuosi} app]
    (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) (:valittu-tila app) uusi-vuosi (:valittu-tyomenetelma app))
    (assoc app :valittu-vuosi uusi-vuosi))

  FiltteriValitseTyomenetelma
  (process-event [{uusi-menetelma :uusi-menetelma} app]
    (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) (:valittu-tila app) (:valittu-vuosi app) uusi-menetelma)
    (assoc app :valittu-tyomenetelma uusi-menetelma))

  TiedostoLadattu
  (process-event [{vastaus :vastaus} app]
    (do
      ;(js/console.log "TiedostoLadattu :: error?" (pr-str (:status vastaus)) (pr-str (get-in vastaus [:response "virheet"])))

      ;; Excelissä voi mahdollisesti olla virheitä, jos näin on, niin avataan modaali, johon virheet kirjoitetaan
      ;; Jos taas kaikki sujui kuten Strömssössä, niin näytetään onnistumistoasti
      (if (not= 200 (:status vastaus))
        (do
          (viesti/nayta-toast! "Ladatun tiedoston käsittelyssä virhe"
                                 :danger viesti/viestin-nayttoaika-lyhyt)
          (virhe-modal (get-in vastaus [:response "virheet"]) "Virhe ladattaessa kohteita tiedostosta")
          (assoc app :excel-virhe (get-in vastaus [:response "virheet"])))
        (do
          ;; Ladataan uudet paikkauskohteet
          (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) (:valittu-tila app)
                               (:valittu-vuosi app) (:valittu-tyomenetelma app))
          (viesti/nayta-toast! "Paikkauskohteet ladattu onnistuneesti"
                                 :success viesti/viestin-nayttoaika-lyhyt)
          (dissoc app :excel-virhe)))))

  HaePaikkauskohteet
  (process-event [_ app]
    (do
      ; (js/console.log "HaePaikkauskohteet -> tehdään serverihaku")
      (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) (:valittu-tila app) (:valittu-vuosi app) (:valittu-tyomenetelma app))
      app))

  HaePaikkauskohteetOnnistui
  (process-event [{vastaus :vastaus} app]
    (let [
          paikkauskohteet (map (fn [kohde]
                                 (-> kohde
                                     (assoc :formatoitu-aikataulu
                                            (fmt-aikataulu (:alkupvm kohde) (:loppupvm kohde) (:paikkauskohteen-tila kohde)))
                                     (assoc :formatoitu-sijainti
                                            (fmt-sijainti (:tie kohde) (:aosa kohde) (:losa kohde) (:aet kohde) (:let kohde)))))
                               vastaus)
          zoomattavat-geot (into [] (concat (mapv (fn [p]
                                                    (when (and
                                                            (not (nil? (:sijainti p)))
                                                            (not (empty? (:sijainti p))))
                                                      (harja.geo/extent (:sijainti p))))
                                                  paikkauskohteet)))
          _ (js/console.log "HaePaikkauskohteetOnnistui :: zoomattavat-geot" (pr-str zoomattavat-geot))
          ;_ (js/console.log "HaePaikkauskohteetOnnistui :: paikkauskohteet" (pr-str paikkauskohteet))
          ]
      (do
        (when (and (not (nil? paikkauskohteet))
                   (not (empty? paikkauskohteet))
                   (not (nil? zoomattavat-geot))
                   (not (empty? zoomattavat-geot)))
          (reset! paikkauskohteet-kartalle/karttataso-paikkauskohteet paikkauskohteet)
          (kartta-tiedot/keskita-kartta-alueeseen! zoomattavat-geot))
        (-> app
            (assoc :lomake nil) ;; Sulje mahdollinen lomake
            (assoc :paikkauskohteet paikkauskohteet)))))

  HaePaikkauskohteetEpaonnistui
  (process-event [{vastaus :vastaus} app]
    (do
      (js/console.log "Haku epäonnistui, vastaus " (pr-str vastaus))
      app))

  PaivitaLomake
  (process-event [{lomake :lomake} app]
    (let [{:keys [validoi] :as validoinnit} (validoi-lomake lomake)
          {:keys [validi? validius]} (validoi validoinnit lomake)]
      (-> app
          (assoc :lomake lomake)
          (assoc-in [:lomake ::tila/validius] validius)
          (assoc-in [:lomake ::tila/validi?] validi?))))

  TallennaPaikkauskohde
  (process-event [{paikkauskohde :paikkauskohde} app]
    (let [;; Muutetaan paikkauskohteen tilaa vain, jos sitä ei ole asetettu
          paikkauskohde (if (nil? (:paikkauskohteen-tila paikkauskohde))
                          (assoc paikkauskohde :paikkauskohteen-tila "ehdotettu")
                          paikkauskohde)
          paikkauskohde (-> paikkauskohde
                            (siivoa-ennen-lahetysta)
                            (assoc :urakka-id (-> @tila/tila :yleiset :urakka :id)))]
      (do
        (js/console.log "Tallennetaan paikkauskohde" (pr-str paikkauskohde))
        (tallenna-paikkauskohde paikkauskohde)
        app)))

  TilaaPaikkauskohde
  (process-event [{paikkauskohde :paikkauskohde} app]
    (let [paikkauskohde (assoc paikkauskohde :paikkauskohteen-tila "tilattu")]
      (do
        (println "Merkitään paikkauskohde [" (:nimi paikkauskohde) "] tilatuksi")
        (tallenna-paikkauskohde paikkauskohde)
        app)))

  HylkaaPaikkauskohde
  (process-event [{paikkauskohde :paikkauskohde} app]
    (let [paikkauskohde (assoc paikkauskohde :paikkauskohteen-tila "hylatty")]
      (do
        (println "Merkitään paikkauskohde [" (:nimi paikkauskohde) "] hylätyksi")
        (tallenna-paikkauskohde paikkauskohde)
        app)))

  PoistaPaikkauskohde
  (process-event [{paikkauskohde :paikkauskohde} app]
    (let [paikkauskohde (assoc paikkauskohde :poistettu true)]
      (do
        (js/console.log "Merkitään paikkauskohde " (:nimi paikkauskohde) "poistetuksi")
        (tallenna-paikkauskohde paikkauskohde)
        app)))

  TallennaPaikkauskohdeOnnistui
  (process-event [{vastaus :vastaus} app]
    (let [_ (js/console.log "Paikkauskohteen tallennus onnistui" (pr-str vastaus))
          _ (hae-paikkauskohteet (-> @tila/yleiset :urakka :id) (:valittu-tila app) (:valittu-vuosi app) (:valittu-tyomenetelma app))
          _ (modal/piilota!)]
      (viesti/nayta-toast! "Paikkauskohteen tallennus onnistui."
                           :success viesti/viestin-nayttoaika-pitka)
      (dissoc app :lomake)))

  TallennaPaikkauskohdeEpaonnistui
  (process-event [{vastaus :vastaus} app]
    (do
      (js/console.log "Paikkauskohteen tallennus epäonnistui" (pr-str vastaus))
      (viesti/nayta! "Paikkauskohteen tallennus epäonnistui" viesti/viestin-nayttoaika-keskipitka :danger)
      ;;TODO: tämä antaa warningin
      ;(harja.ui.yleiset/virheviesti-sailio "Paikkauskohteen tallennus epäonnistui")
      #_(dissoc app :lomake)
      app))

  ;; TODO: Mieti siistimisen yhteydessä, yhdistetäänkö nämä kaksi yhdeksi. Ainoa ero on logitus tällä hetkellä
  PeruPaikkauskohteenTilaus
  (process-event [{paikkauskohde :paikkauskohde} app]
    (let [paikkauskohde (assoc paikkauskohde :paikkauskohteen-tila "ehdotettu")]
      (do
        (println "Merkitään paikkauskohde [" (:nimi paikkauskohde) "] tilatusta ehdotetuksi")
        (tallenna-paikkauskohde paikkauskohde)
        app)))

  PeruPaikkauskohteenHylkays
  (process-event [{paikkauskohde :paikkauskohde} app]
    (let [paikkauskohde (assoc paikkauskohde :paikkauskohteen-tila "ehdotettu")]
      (do
        (println "Merkitään paikkauskohde [" (:nimi paikkauskohde) "] hylätystä ehdotetuksi")
        (tallenna-paikkauskohde paikkauskohde)
        app)))
  )

(def tyomenetelmat
  ["Kaikki" "MPA" "KTVA" "SIPA" "SIPU" "REPA" "UREM" "Muu"])

