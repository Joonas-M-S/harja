(ns harja.tiedot.urakka.pot2.materiaalikirjasto
  "UI controlleri pot2 materiaalikirjastolle"
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<!]]
            [clojure.string :as str]
            [harja.loki :refer [log tarkkaile!]]
            [tuck.core :refer [process-event] :as tuck]
            [harja.tyokalut.tuck :as tuck-apurit]
            [harja.domain.pot2 :as pot2-domain]
            [harja.pvm :as pvm]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.tiedot.navigaatio :as nav]
            [harja.ui.napit :as napit]
            [harja.ui.varmista-kayttajalta :as varmista-kayttajalta]
            [harja.ui.lomake :as ui-lomake])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(def materiaalikirjastossa? (atom false))
(def nayta-materiaalikirjasto? (atom false))
(def materiaali-jo-kaytossa-str "Materiaali on jo käytössä, eikä sitä voi enää poistaa.")

(defrecord AlustaTila [])
(defrecord UusiMassa [])
(defrecord MuokkaaMassaa [rivi klooni?])
(defrecord UusiMurske [])
(defrecord MuokkaaMursketta [rivi klooni?])
(defrecord NaytaModal [avataanko?])

;; Massat
(defrecord TallennaLomake [data])
(defrecord TallennaMassaOnnistui [vastaus])
(defrecord TallennaMassaEpaonnistui [vastaus])
(defrecord TyhjennaLomake [data])
(defrecord PaivitaMassaLomake [data])
(defrecord PaivitaAineenTieto [polku arvo])
(defrecord LisaaSideaine [sideaineen-kayttotapa])
(defrecord PoistaSideaine [sideaineen-kayttotapa])

;; Murskeet
(defrecord TallennaMurskeLomake [data])
(defrecord TallennaMurskeOnnistui [vastaus])
(defrecord TallennaMurskeEpaonnistui [vastaus])
(defrecord TyhjennaMurskeLomake [data])
(defrecord PaivitaMurskeLomake [data])


;; Haut
(defrecord HaePot2MassatJaMurskeet [])
(defrecord HaePot2MassatJaMurskeetOnnistui [vastaus])
(defrecord HaePot2MassatJaMurskeetEpaonnistui [vastaus])

(defrecord HaeKoodistot [])
(defrecord HaeKoodistotOnnistui [vastaus])
(defrecord HaeKoodistotEpaonnistui [vastaus])

(defn- massan-murskeen-nimen-komp [ydin tarkennukset fmt toiminto-fn]
  (if (= :komponentti fmt)
    [(if toiminto-fn
       :a
       :span)
     {:on-click #(when toiminto-fn
                   (do
                     (.stopPropagation %)
                     (toiminto-fn)))
      :style {:cursor "pointer"}}
     [:span.bold ydin]
     [:span tarkennukset]]
    (str ydin tarkennukset)))

(defn massan-rikastettu-nimi
  "Formatoi massan nimen. Jos haluat Reagent-komponentin, anna fmt = :komponentti, muuten anna :string"
  ([massatyypit massa fmt]
   (massan-rikastettu-nimi massatyypit massa fmt nil))
  ([massatyypit massa fmt toiminto-fn]
  ;; esim AB16 (AN15, RC40, 2020/09/1234) tyyppi (raekoko, nimen tarkenne, DoP, Kuulamyllyluokka, RC%)
  (let [[ydin tarkennukset] (pot2-domain/massan-rikastettu-nimi massatyypit massa)]
    ;; vähän huonoksi ehkä meni tämän kanssa. Toinen funktiota kutsuva tarvitsee komponenttiwrapperin ja toinen ei
    ;; pitänee refaktoroida... fixme jos ehdit
    (if (= fmt :komponentti)
      [massan-murskeen-nimen-komp ydin tarkennukset fmt toiminto-fn]
      (massan-murskeen-nimen-komp ydin tarkennukset fmt toiminto-fn)))))

(defn murskeen-rikastettu-nimi
  ([mursketyypit murske fmt]
   (murskeen-rikastettu-nimi mursketyypit murske fmt nil))
  ([mursketyypit murske fmt toiminto-fn]
   ;; esim KaM LJYR 2020/09/3232 (0/40, LA30)
   ;; tyyppi Kalliomurske, tarkenne LJYR, rakeisuus 0/40, iskunkestävyys (esim LA30)
   (let [[ydin tarkennukset] (pot2-domain/mursken-rikastettu-nimi mursketyypit murske)]
     (if (= fmt :komponentti)
       [massan-murskeen-nimen-komp ydin tarkennukset fmt toiminto-fn]
       (massan-murskeen-nimen-komp ydin tarkennukset fmt toiminto-fn)))))

(def sideaineen-kayttotavat
  [{::pot2-domain/nimi "Lopputuotteen sideaine"
    ::pot2-domain/koodi :lopputuote}
   {::pot2-domain/nimi "Lisätty sideaine"
    ::pot2-domain/koodi :lisatty}])

(defn lisatty-sideaine-mahdollinen?
  [rivi]
  (let [asfalttirouhetta? ((set (keys (::pot2-domain/runkoaineet rivi))) 2)
        bitumikaterouhetta? ((set (keys (::pot2-domain/lisaaineet rivi))) 4)]
    (or asfalttirouhetta? bitumikaterouhetta?)))

(defn- sideaine-kayttoliittyman-muotoon
  "UI kilkkeet tarvitsevat runko-, side- ja lisäaineet muodossa {tyyppi {tiedot}}"
  [aineet]
  (let [lopputuotteen (remove #(false? (:sideaine/lopputuote? %)) aineet)
        lisatyt (remove #(true? (:sideaine/lopputuote? %)) aineet)
        aineet-map (fn [aineet]
                     (if (empty? aineet)
                       {}
                       {:valittu? true
                        :aineet (into {}
                                      (map-indexed
                                        (fn [idx aine]
                                          {idx aine})
                                        (vec aineet)))}))]
    {:lopputuote (aineet-map lopputuotteen)
     :lisatty (aineet-map lisatyt)}))

(defn- aine-kayttoliittyman-muotoon
  "UI kilkkeet tarvitsevat runko- ja lisäaineet muodossa {tyyppi {tiedot}}"
  [aineet avain]
  (into {}
        (map (fn [aine]
               {(avain aine) (assoc aine :valittu? true)})
             (vec aineet))))

(defn- hae-massat-ja-murskeet [app]
  (tuck-apurit/post! app
                     :hae-urakan-massat-ja-murskeet
                     {:urakka-id (-> @tila/tila :yleiset :urakka :id)}
                     {:onnistui ->HaePot2MassatJaMurskeetOnnistui
                      :epaonnistui ->HaePot2MassatJaMurskeetEpaonnistui}))
(def tyhja-sideaine
  {:sideaine/tyyppi nil :sideaine/pitoisuus nil})

(defn mursketyyppia? [mursketyypit nimi lomake]
  (= (pot2-domain/ainetyypin-nimi->koodi mursketyypit nimi)
     (::pot2-domain/tyyppi lomake)))

(defn mursketyyppia-bem-tai-muu? [mursketyypit lomake]
  (or (mursketyyppia? mursketyypit "(UUSIO) Betonimurske I" lomake)
      (mursketyyppia? mursketyypit "(UUSIO) Betonimurske II" lomake)
      (mursketyyppia? mursketyypit "Muu" lomake)))

(def nayta-lahde mursketyyppia-bem-tai-muu?)

(defn materiaalin-kaytto
  [materiaali-kaytossa]
  (when-not (empty? materiaali-kaytossa)
    (let [lukittu? (some #(str/includes? % "lukittu")
                         (map :tila materiaali-kaytossa))
          lukitut-kohteet (filter #(when (str/includes? (:tila %) "lukittu")
                                     %)
                                  materiaali-kaytossa)
          materiaali-kaytossa (if-not (empty? lukitut-kohteet)
                                lukitut-kohteet
                                materiaali-kaytossa)]
      [:div
       [:h3 (if (not (empty? lukitut-kohteet))
              "Materiaalia on kirjattu päällystysilmoitukseen jonka tila on lukittu. Muokkaamista tai poistamista ei enää sallita. Lukitut kohteet: "
              (str "Materiaalia on kirjattu seuraavissa päällystysilmoituksissa: "))]
       [:ul
        (for [{kohdenumero :kohdenumero
               nimi :nimi
               kohteiden-lkm :kohteiden-lkm} materiaali-kaytossa]
          ^{:key kohdenumero}
          [:li (str "#" kohdenumero " " nimi " (" kohteiden-lkm " riviä)")])]])))

(defn puutelistaus [data muut-validointivirheet]
  (when-not (and (empty? (ui-lomake/puuttuvat-pakolliset-kentat data))
                 (empty? muut-validointivirheet))
    [:div
     [:div "Seuraavat pakolliset kentät pitää täyttää ennen tallentamista: "]
     [:ul
      (for [puute (concat
                    (ui-lomake/puuttuvien-pakollisten-kenttien-otsikot data)
                    muut-validointivirheet)]
        ^{:key (name puute)}
        [:li (name puute)])]]))

(defn tallenna-materiaali-nappi
  [materiaali-kaytossa toiminto-fn disabled tyyppi]
  (assert (#{:massa :murske} tyyppi) "Tallennettavan tyyppi oltava massa tai murske")
  (let [lukittu? (some #(str/includes? % "lukittu")
                       (map :tila materiaali-kaytossa))]
    [napit/tallenna
     "Tallenna"
     (let [materiaalin-str (if (= :murske tyyppi) "Murskeen" "Massan")]
       (if (empty? materiaali-kaytossa)
         toiminto-fn
         (when-not lukittu?
           (fn []
             (varmista-kayttajalta/varmista-kayttajalta
               {:otsikko (str materiaalin-str " tallentaminen")
                :sisalto
                [:div
                 [:div (str "Materiaali on käytössä päällystysilmoituksissa joita ei ole vielä lukittu, joten muokkaaminen on mahdollista. Jos muokkaat kyseistä materiaalia, tiedot päivittyvät kaikkialla missä materiaalia on käytetty.")]
                 [materiaalin-kaytto materiaali-kaytossa]
                 [:div "Haluatko varmasti tallentaa muutokset? Voit myös halutessasi luoda tästä massasta kopion ja muokata sitä."]]
                :toiminto-fn toiminto-fn
                :hyvaksy "Kyllä"})))))
     {:vayla-tyyli? true
      :luokka "suuri"
      :disabled (or disabled lukittu?)}]))

(defn poista-materiaali-nappi
  [materiaali-kaytossa toiminto-fn tyyppi]
  (assert (#{:massa :murske} tyyppi) "Poistettavan tyyppi oltava massa tai murske")
  (let [lukittu? (some #(str/includes? % "lukittu")
                       (map :tila materiaali-kaytossa))
        materiaalin-str (if (= :murske tyyppi) "Murskeen" "Massan")]
    [:div {:style {:width "160px"}}
     [napit/poista
      "Poista"
      (fn []
        (varmista-kayttajalta/varmista-kayttajalta
          {:otsikko (str materiaalin-str " poistaminen")
           :sisalto
           [:div (str "Haluatko ihan varmasti poistaa tämän " (clojure.string/lower-case materiaalin-str) "?")]
           :toiminto-fn toiminto-fn
           :hyvaksy "Kyllä"}))
      {:disabled (not (empty? materiaali-kaytossa))
       :vayla-tyyli? true
       :luokka "suuri"}]
     (when (and (not lukittu?)
                (not (empty? materiaali-kaytossa)))
       [harja.ui.yleiset/vihje materiaali-jo-kaytossa-str])]))

(defn tallennus-ja-puutelistaus
  [e! {:keys [data validointivirheet tallenna-fn voi-tallentaa?
              peruuta-fn poista-fn tyyppi id materiaali-kaytossa]}]
  [:div
   [puutelistaus (ui-lomake/puuttuvat-pakolliset-kentat data) validointivirheet]
   [:div.flex-row {:style {:margin-top "2rem" :align-items "start"}}
    [:div.tallenna-peruuta
     [tallenna-materiaali-nappi materiaali-kaytossa tallenna-fn
      voi-tallentaa?
      tyyppi]
     [napit/yleinen "Peruuta" :toissijainen peruuta-fn
      {:vayla-tyyli? true
       :luokka "suuri"}]]

    (when id
      [poista-materiaali-nappi materiaali-kaytossa poista-fn tyyppi])]
   [materiaalin-kaytto materiaali-kaytossa]])

(defn rivi->massa-tai-murske [rivi {:keys [massat murskeet]}]
  (if (:murske rivi)
    (first (filter #(= (::pot2-domain/murske-id (:murske rivi)))
                   murskeet))
    (first (filter #(= (::pot2-domain/massa-id (:massa rivi)))
                   massat))))

(extend-protocol tuck/Event

  AlustaTila
  (process-event [_ {:as app}]
    (assoc app :pot2-massa-lomake nil))

  NaytaModal
  (process-event [{avataanko? :avataanko?} app]
    (do
      (reset! nayta-materiaalikirjasto? avataanko?)
      app))

  HaePot2MassatJaMurskeet
  (process-event [_ app]
    (-> app
        (hae-massat-ja-murskeet)))

  HaePot2MassatJaMurskeetOnnistui
  (process-event [{{massat :massat
                    murskeet :murskeet} :vastaus} {:as app}]
    (assoc app :massat massat
               :murskeet murskeet))

  HaePot2MassatJaMurskeetEpaonnistui
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Massojen haku epäonnistui!" :danger)
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
    (viesti/nayta! "Materiaalikoodistojen haku epäonnistui!" :danger)
    app)

  UusiMassa
  (process-event [_ app]
    (js/console.log "Uusi massa lomake avataan")
    (assoc app :pot2-massa-lomake {}))

  MuokkaaMassaa
  (process-event [{rivi :rivi klooni? :klooni?} app]
    ;; jos käyttäjä luo uuden massan kloonaamalla vanhan, nollataan id:t
    (let [massan-id (if klooni?
                      nil
                      (::pot2-domain/massa-id rivi))
          runkoaineet (aine-kayttoliittyman-muotoon (map
                                                      ;; Koska luodaan uusi massa olemassaolevan tietojen pohjalta, täytyy vanhan massan viittaukset poistaa
                                                      #(if klooni?
                                                         (dissoc % :runkoaine/id ::pot2-domain/massa-id)
                                                         (identity %))
                                                      (:harja.domain.pot2/runkoaineet rivi)) :runkoaine/tyyppi)
          sideaineet (sideaine-kayttoliittyman-muotoon (map
                                                         #(if klooni?
                                                            (dissoc % :sideaine/id ::pot2-domain/massa-id)
                                                            (identity %))
                                                         (:harja.domain.pot2/sideaineet rivi)))
          lisaaineet (aine-kayttoliittyman-muotoon (map
                                                     #(if klooni?
                                                        (dissoc % :lisaaine/id ::pot2-domain/massa-id)
                                                        (identity %))
                                                     (:harja.domain.pot2/lisaaineet rivi)) :lisaaine/tyyppi)]
      (-> app
          (assoc :pot2-massa-lomake rivi)
          (assoc-in [:pot2-massa-lomake ::pot2-domain/massa-id] massan-id)
          (assoc-in [:pot2-massa-lomake :harja.domain.pot2/runkoaineet] runkoaineet)
          (assoc-in [:pot2-massa-lomake :harja.domain.pot2/sideaineet] sideaineet)
          (assoc-in [:pot2-massa-lomake :harja.domain.pot2/lisaaineet] lisaaineet))))

  UusiMurske
  (process-event [_ app]
    (js/console.log "Uusi murskelomake avataan")
    (assoc app :pot2-murske-lomake {}))

  MuokkaaMursketta
  (process-event [{rivi :rivi klooni? :klooni?} app]
    ;; jos käyttäjä luo uuden massan kloonaamalla vanhan, nollataan id:t ja käytössäoleminen
    (let [murske-id (if klooni? nil (::pot2-domain/murske-id rivi))
          kaytossa (if klooni? nil (::pot2-domain/kaytossa rivi))]
      (-> app
          (assoc :pot2-murske-lomake rivi)
          (assoc-in [:pot2-murske-lomake ::pot2-domain/murske-id] murske-id)
          (assoc-in [:pot2-murske-lomake ::pot2-domain/kaytossa] kaytossa))))

  PaivitaMassaLomake
  (process-event [{data :data} app]
    (update app :pot2-massa-lomake merge data))

  PaivitaAineenTieto
  (process-event [{polku :polku arvo :arvo} app]
    (assoc-in app
              (vec (cons :pot2-massa-lomake polku)) arvo))

  LisaaSideaine
  (process-event [{sideaineen-kayttotapa :sideaineen-kayttotapa} app]
    (let [aineiden-lkm
          (count (get-in app
                         [:pot2-massa-lomake ::pot2-domain/sideaineet sideaineen-kayttotapa :aineet]))]
      (assoc-in app
                [:pot2-massa-lomake ::pot2-domain/sideaineet sideaineen-kayttotapa :aineet aineiden-lkm]
                tyhja-sideaine)))

  PoistaSideaine
  (process-event [{sideaineen-kayttotapa :sideaineen-kayttotapa} app]
    (let [aineiden-lkm
          (count (get-in app
                         [:pot2-massa-lomake ::pot2-domain/sideaineet sideaineen-kayttotapa :aineet]))]
      (update-in app [:pot2-massa-lomake ::pot2-domain/sideaineet sideaineen-kayttotapa :aineet]
                 dissoc (dec aineiden-lkm))))

  TallennaLomake
  (process-event [{data :data} app]
    (let [massa (:pot2-massa-lomake app)
          poistettu? {::harja.domain.pot2/poistettu? (boolean
                                                      (:harja.domain.pot2/poistettu? data))}
          _ (js/console.log "TallennaLomake data" (pr-str data))
          _ (js/console.log "TallennaLomake massa" (pr-str massa))]
      (tuck-apurit/post! :tallenna-urakan-massa
                         (-> (merge massa
                                    poistettu?)
                             (assoc ::pot2-domain/urakka-id (-> @tila/tila :yleiset :urakka :id)))
                         {:onnistui ->TallennaMassaOnnistui
                          :epaonnistui ->TallennaMassaEpaonnistui}))
    app)

  TallennaMassaOnnistui
  (process-event [{vastaus :vastaus} app]
    (if (::pot2-domain/poistettu? vastaus)
      (viesti/nayta! "Massa poistettu!")
      (viesti/nayta! "Massa tallennettu!"))
    (hae-massat-ja-murskeet app)
    (assoc app :pot2-massa-lomake nil))

  TallennaMassaEpaonnistui
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Massan tallennus epäonnistui!" :danger)
    app)

  TyhjennaLomake
  (process-event [{data :data} app]
    (js/console.log "TyhjennaLomake" (pr-str data))
    (-> app
        (assoc :pot2-massa-lomake nil)))

  PaivitaMurskeLomake
  (process-event [{data :data} app]
    (update app :pot2-murske-lomake merge data))

  TallennaMurskeLomake
  (process-event [{data :data} app]
    (let [murske (:pot2-murske-lomake app)
          poistettu? {::harja.domain.pot2/poistettu? (boolean
                                                       (:harja.domain.pot2/poistettu? data))}
          _ (js/console.log "TallennaMurskeLomake data" (pr-str data))
          _ (js/console.log "TallennaMurskeLomake murske" (pr-str murske))]
      (tuck-apurit/post! :tallenna-urakan-murske
                         (-> (merge murske
                                    poistettu?)
                             (assoc ::pot2-domain/urakka-id (-> @tila/tila :yleiset :urakka :id)))
                         {:onnistui ->TallennaMurskeOnnistui
                          :epaonnistui ->TallennaMurskeEpaonnistui}))
    app)

  TallennaMurskeOnnistui
  (process-event [{vastaus :vastaus} app]
    (if (::pot2-domain/poistettu? vastaus)
      (viesti/nayta! "Murske poistettu!")
      (viesti/nayta! "Murske tallennettu!"))
    (hae-massat-ja-murskeet app)
    (assoc app :pot2-murske-lomake nil))

  TallennaMurskeEpaonnistui
  (process-event [{vastaus :vastaus} app]
    (viesti/nayta! "Murskeen tallennus epäonnistui!" :danger)
    app)

  TyhjennaMurskeLomake
  (process-event [{data :data} app]
    (js/console.log "TyhjennaLomake" (pr-str data))
    (-> app
        (assoc :pot2-murske-lomake nil)))
  )