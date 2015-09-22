(ns harja.ui.grid
  "Harjan käyttöön soveltuva geneerinen muokattava ruudukkokomponentti."
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log tarkkaile! logt]]
            [harja.ui.yleiset :refer [ajax-loader linkki livi-pudotusvalikko virheen-ohje]]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.kentat :refer [tee-kentta nayta-arvo vain-luku-atomina]]
            [harja.ui.validointi :as validointi]

            [cljs.core.async :refer [<! put! chan]]
            [clojure.string :as str]
            [schema.core :as s :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))


;; Otsikot
;; Rivi gridin datassa voi olla Otsikko record, jolloin se näytetään väliotsikkona.
;;

(defrecord Otsikko [teksti])

(defn otsikko
  "Luo otsikon annetulla tekstillä."
  [teksti]
  (assert (not (nil? teksti)) "Anna otsikolle teksti.")
  (->Otsikko teksti))


(defn otsikko? [x]
  (instance? Otsikko x))


;; Ohjausprotokolla
;; Grid protokolla määrittelee protokollan, jolla gridin toimintoja voi ohjata
;; ulkopuolelta.

(defprotocol Grid
  "Ohjausprotokolla, jolla gridin muokkaustilaa voidaan kysellä ja manipuloida."

  (lisaa-rivi! [g rivin-tiedot] "Lisää muokkaustilassa uuden rivin.
Annettu rivin-tiedot voi olla tyhjä tai se voi alustaa kenttien arvoja.")

  (hae-muokkaustila [g] "Hakee tämänhetkisen muokkaustilan, joka on mäppi id:stä rivin tietoihin.")


  (hae-virheet [g] "Hakee tämänhetkisen muokkaustilan mukaiset validointivirheet.")

  (hae-varoitukset [g] "Hakee tämänhetkisen muokkaustilan mukaiset validointivaroitukset.")

  (nollaa-historia! [g] "Nollaa muokkaushistorian, tämä on lähinnä muokkaus-grid versiota varten. Tällä voi kertoa gridille, että data on täysin muuttunut eikä muokkaushistoria ole enää relevantti.")
  ;; PENDING: oisko "jemmaa muokkaushistoria", jolla sen saisi avaimella talteen ja otettua takaisin?
  (hae-viimeisin-muokattu-id [g] "Hakee viimeisimmän muokatun id:n")
  (muokkaa-rivit! [this funktio args] "Muokkaa kaikki taulukon rivit funktion avulla.")

  (vetolaatikko-auki? [this id] "Tarkista onko vetolaatikko auki annetulla rivin id:llä.")

  (avaa-vetolaatikko! [this id] "Avaa vetolaatikko rivin id:llä.")

  (sulje-vetolaatikko! [this id] "sulje vetolaatikko rivin id:llä.")

  (aseta-virhe! [this rivin-id kentta virheteksti] "Asettaa ulkoisesti virheen rivin kentälle")
  (poista-virhe! [this rivin-id kentta] "Poistaa rivin kentän virheen ulkoisesti")

  ;; PENDING: lisää tänne tarvittaessa muita metodeja
  )

(defprotocol GridKahva
  "Sisäinen protokolla, jolle grid asettaa itsensä."
  (aseta-grid [this grid] "Asetaa gridin, jolle ohjauskutsut pitäisi välittää."))

(defn grid-ohjaus
  "Tekee grid ohjausinstanssin, jonka kutsuva puoli voi antaa gridille."
  []
  (let [gridi (atom nil)]
    (reify
      Grid
      (lisaa-rivi! [_ rivin-tiedot]
        (lisaa-rivi! @gridi rivin-tiedot))

      (hae-muokkaustila [_]
        (hae-muokkaustila @gridi))

      (hae-virheet [_]
        (hae-virheet @gridi))

      (hae-varoitukset [_]
        (hae-varoitukset @gridi))

      (nollaa-historia! [_]
        (nollaa-historia! @gridi))

      (hae-viimeisin-muokattu-id [_]
        (hae-viimeisin-muokattu-id @gridi))

      (muokkaa-rivit! [this funktio args]
        (apply muokkaa-rivit! @gridi funktio args))

      (vetolaatikko-auki? [_ id]
        (vetolaatikko-auki? @gridi id))

      (avaa-vetolaatikko! [_ id]
        (avaa-vetolaatikko! @gridi id))

      (sulje-vetolaatikko! [_ id]
        (sulje-vetolaatikko! @gridi id))

      (aseta-virhe! [_ rivin-id kentta virheteksti]
        (aseta-virhe! @gridi rivin-id kentta virheteksti))
      (poista-virhe! [_ rivin-id kentta]
        (poista-virhe! @gridi rivin-id kentta))

      GridKahva
      (aseta-grid [_ grid]
        (reset! gridi grid)))))

(defn avaa-tai-sulje-vetolaatikko!
  "Vaihtaa vetolaatikon tilaa. Avaa vetolaatikon, jos se on suljettu, muuten sulkee sen."
  [g id]
  (if (vetolaatikko-auki? g id)
    (sulje-vetolaatikko! g id)
    (avaa-vetolaatikko! g id)))

(defn tayta-tiedot-alas
  "Täyttää rivin tietoja alaspäin."
  [rivit s lahtorivi tayta-fn]
  (let [tayta-fn (or tayta-fn

                     ;; Oletusfunktio kopioi tiedon sellaisenaan
                     (let [nimi (:nimi s)
                           lahtoarvo ((or (:hae s) nimi) lahtorivi)
                           aseta (or (:aseta s)
                                     (fn [rivi arvo]
                                       (assoc rivi nimi arvo)))]
                       (fn [_ taytettava]
                         (aseta taytettava lahtoarvo))))]
    (loop [uudet-rivit (list)
           alku false
           [rivi & rivit] rivit]
      (if-not rivi
        uudet-rivit
        (if (= lahtorivi rivi)
          (recur (conj uudet-rivit rivi) true rivit)
          (if-not alku
            (recur (conj uudet-rivit rivi) false rivit)
            (recur (conj uudet-rivit
                         (tayta-fn lahtorivi rivi))
                   true
                   rivit)))))))


;; UI-komponentit
;; Itse gridin UI-komponentit

(defn- vetolaatikon-tila [ohjaus vetolaatikot id]
  (let [vetolaatikko? (contains? vetolaatikot id)]
    ^{:key (str "vetolaatikontila" id)}
    [:td.vetolaatikon-tila.klikattava {:on-click (when vetolaatikko? #(avaa-tai-sulje-vetolaatikko! ohjaus id))}
     (when vetolaatikko?
       (if (vetolaatikko-auki? ohjaus id)
         (ikonit/chevron-down)
         (ikonit/chevron-right)))]))

(defn- vetolaatikko-rivi
  "Funktio, joka palauttaa vetolaatikkorivin tai nil. Huom: kutsu tätä funktiona, koska voi palauttaa nil."
  [vetolaatikot vetolaatikot-auki id colspan]
  (when-let [vetolaatikko (get vetolaatikot id)]
    (let [auki (@vetolaatikot-auki id)]
      ^{:key (str "vetolaatikko" id)}
      [:tr.vetolaatikko {:class (if auki "vetolaatikko-auki" "vetolaatikko-kiinni")}
       [:td {:colSpan colspan}
        [:div.vetolaatikko-sisalto
         (when auki
           vetolaatikko)]]])))




(defn- muokkaus-rivi [{:keys [ohjaus id muokkaa! luokka rivin-virheet rivin-varoitukset voi-poistaa? esta-poistaminen? esta-poistaminen-tooltip
                              fokus aseta-fokus! tulevat-rivit vetolaatikot]} skeema rivi]
  [:tr.muokataan {:class luokka}
   (for [{:keys [nimi hae aseta fmt muokattava? tasaa tyyppi] :as s} skeema]
     (if (= :vetolaatikon-tila tyyppi)
       ^{:key (str "vetolaatikontila" id)}
       [vetolaatikon-tila ohjaus vetolaatikot id]

       (let [s (assoc s :rivi rivi)
             hae (or hae
                     #(get % nimi))
             arvo (hae rivi)
             kentan-virheet (get rivin-virheet nimi)
             kentan-varoitukset (get rivin-varoitukset nimi)
             tasaus-luokka (if (= tasaa :oikea) "tasaa-oikealle" "")
             fokus-id [id nimi]]

         (if (or (nil? muokattava?) (muokattava? rivi))
           ^{:key (str nimi)}
           [:td {:class (str tasaus-luokka (if-not (empty? kentan-virheet)
                                             " sisaltaa-virheen")
                             (when-not (empty? kentan-varoitukset)
                               " sisaltaa-varoituksen"))}
            (if-not (empty? kentan-virheet)
              (virheen-ohje kentan-virheet)
              (if-not (empty? kentan-varoitukset)
                (virheen-ohje kentan-varoitukset :varoitus)))


            ;; Jos skeema tukee kopiointia, näytetään kopioi alas nappi
            (when-let [tayta-alas (:tayta-alas? s)]
              (when (and (= fokus fokus-id)
                         (tayta-alas arvo)

                         ;; Sallitaan täyttö, vain jos tulevia rivejä on ja kaikkien niiden arvot ovat tyhjiä
                         (not (empty? tulevat-rivit))
                         (every? str/blank? (map hae tulevat-rivit)))

                [:div {:class (if (= :oikea (:tasaa s))
                                "pull-left"
                                "pull-right")}
                 [:div {:style {:position "absolute" :display "inline-block"}}
                  [:button {:class    (str "nappi-toissijainen nappi-tayta" (when (:kelluta-tayta-nappi s) " kelluta-tayta-nappi"))
                            :title    (:tayta-tooltip s)
                            :style    {:position "absolute"
                                       :left     (when (= :oikea (:tasaa s)) 0)
                                       :right    (when-not (= :oikea (:tasaa s)) "100%")}
                            :on-click #(muokkaa-rivit! ohjaus tayta-tiedot-alas [s rivi (:tayta-fn s)])}
                   (ikonit/arrow-down) " Täytä"]]]))

            ;;(log "tehdään kenttä " (pr-str fokus-id) ", nykyinen fokus: " (pr-str fokus))
            [tee-kentta (assoc s
                          :focus (= fokus fokus-id)
                          :on-focus #(aseta-fokus! fokus-id)
                          :pituus-max (:pituus-max s))
             (r/wrap
               arvo
               (fn [uusi]
                 (if aseta
                   (muokkaa! id (fn [rivi]
                                  (aseta rivi uusi)))
                   (muokkaa! id assoc nimi uusi))))]]
           ^{:key (str nimi)}
           [:td {:class tasaus-luokka}
            ((or fmt str) (hae rivi))]))))
   [:td.toiminnot
    (when (or (nil? voi-poistaa?) (voi-poistaa? rivi))
      (if (or (nil? esta-poistaminen?) (false? (esta-poistaminen? rivi)))
        [:span.klikattava {:on-click #(do (.preventDefault %)
                                          (muokkaa! id assoc :poistettu true))}
         (ikonit/trash)]
        [:span (ikonit/trash-disabled (esta-poistaminen-tooltip rivi))]))
      (when-not (empty? rivin-virheet)                      ; true ;-not (empty? rivin-virheet)
        [:span.rivilla-virheita
         (ikonit/warning-sign)])]])

(defn- naytto-rivi [{:keys [luokka rivi-klikattu ohjaus id vetolaatikot tallenna]} skeema rivi]
  [:tr {:class    luokka
        :on-click (when rivi-klikattu
                    #(rivi-klikattu rivi))}
   (for [{:keys [nimi hae fmt tasaa tyyppi komponentti nayta-max-merkkia]} skeema]
     (if (= :vetolaatikon-tila tyyppi)
       ^{:key (str "vetolaatikontila" id)}
       [vetolaatikon-tila ohjaus vetolaatikot id]
       ^{:key (str nimi)}
       [:td {:class
             (if (= tasaa :oikea) "tasaa-oikealle" "")}
        (if (= tyyppi :komponentti)
          (komponentti rivi)
          (let [haettu-arvo (if hae
                          (hae rivi)
                          (get rivi nimi))
                arvon-pituus-rajattu (if nayta-max-merkkia
                                       (if (> (count haettu-arvo) nayta-max-merkkia)
                                           (str (subs haettu-arvo 0 nayta-max-merkkia) "...")
                                           haettu-arvo)
                                       haettu-arvo)]
            (if fmt
              (fmt arvon-pituus-rajattu)
              [nayta-arvo skeema (vain-luku-atomina arvon-pituus-rajattu)])))]))
   (when tallenna [:td.toiminnot])])

(defn laske-sarakkeiden-leveys [skeema]
  (if (every? number? (map :leveys skeema))
    ;; Jos kaikki leveydet ovat numeroita (ei siis prosentti stringejä),
    ;; voidaan niille laskea suhteelliset leveydet
    (let [yhteensa (reduce + (map :leveys skeema))]
      (mapv (fn [{lev :leveys :as kentta}]
              (assoc kentta
                :leveys (str (.toFixed (* 100.0 (/ lev yhteensa)) 1) "%")))
            skeema))
    skeema))

(defn grid
  "Taulukko, jossa tietoa voi tarkastella ja muokata. Skeema on vektori joka sisältää taulukon sarakkeet.
Jokainen skeeman itemi on mappi, jossa seuraavat avaimet:
  :nimi            kentän hakufn
  :fmt             kentän näyttämis fn (oletus str)
  :otsikko         ihmiselle näytettävä otsikko
  :tunniste        rivin tunnistava kenttä, oletuksena :id
  :voi-poistaa?    voiko rivin poistaa (funktio)
  :esta-poistaminen?          funktio, joka palauttaa true tai false. Jos palauttaa true, roskakori disabloidaan erikseen annetun tooltipin kera.
  :esta-poistaminen-tooltip   funktio, joka palauttaa tooltipin. ks. ylempi.
  :voi-lisata?     voiko rivin lisätä (boolean)
  :tyyppi          kentän tietotyyppi,  #{:string :puhelin :email :pvm}
  :ohjaus          gridin ohjauskahva, joka on luotu (grid-ohjaus) kutsulla
  
Tyypin mukaan voi olla lisäavaimia, jotka määrittelevät tarkemmin kentän validoinnin.

Optiot on mappi optioita:
  :tallenna        funktio, jolle kaikki muutokset, poistot ja lisäykset muokkauksen päätyttyä
                   jos tallenna funktiota ei ole annettu, taulukon muokkausta ei sallita eikä nappia näytetään
                   jos tallenna arvo on :ei-mahdollinen, näytetään Muokkaa-nappi himmennettynä
  :tallenna-vain-muokatut boolean jos päällä, tallennetaan vain muokatut. Oletuksena true
  :peruuta         funktio jota kutsutaan kun käyttäjä klikkaa Peruuta-nappia muokkausmoodissa
  :rivi-klikattu   funktio jota kutsutaan kun käyttäjä klikkaa riviä näyttömoodissa (parametrinä rivin tiedot)
  :muokkaa-footer  optionaalinen footer komponentti joka muokkaustilassa näytetään, parametrina Grid ohjauskahva
  :muokkaa-aina    jos true, grid on aina muokkaustilassa, eikä tallenna/peruuta nappeja ole
  :muutos          jos annettu, kaikista gridin muutoksista tulee kutsu tähän funktioon.
                   Parametrina Grid ohjauskahva
  :prosessoi-muutos  funktio, jolla voi prosessoida muutoksenjälkeisen datan, esim. päivittää laskettuja kenttiä.
                     parametrina muokkausdata, palauttaa uuden muokkausdatan 
  :rivin-luokka    funktio joka palauttaa rivin luokan
  :uusi-rivi       jos annettu uuden rivin tiedot käsitellään tällä funktiolla 
  :vetolaatikot    {id komponentti} lisäriveistä, jotka näytetään normaalirivien välissä
                   jos rivin id:llä on avain tässä mäpissä, näytetään arvona oleva komponentti
                   rivin alla
  :luokat          Päätason div-elementille annettavat lisäkuokat (vectori stringejä)
  :napit-alaskin?  Boolean: näytetäänkö taulukon muokkauskontrollit alhaallakin. Oletus: false

  
  "
  [{:keys [otsikko tallenna tallenna-vain-muokatut peruuta tyhja tunniste voi-poistaa? voi-lisata? rivi-klikattu esta-poistaminen? esta-poistaminen-tooltip
           muokkaa-footer muokkaa-aina muutos rivin-luokka napit-alaskin? prosessoi-muutos
           uusi-rivi vetolaatikot luokat] :as opts} skeema tiedot]
  (let [muokatut (atom nil)                                 ;; muokattu datajoukko
        jarjestys (atom nil)                                ;; id:t indekseissä (tai otsikko)
        uusi-id (atom 0)                                    ;; tästä dekrementoidaan aina uusia id:tä
        historia (atom [])
        virheet (atom {})                                   ;; validointivirheet: (:id rivi) => [virheet]
        varoitukset (atom {})                               ;; validointivaroitukset: (:id rivi) => [varoitukset]
        viime-assoc (atom nil)                              ;; edellisen muokkauksen, jos se oli assoc-in, polku
        viimeisin-muokattu-id (atom nil)
        skeema (keep identity skeema)
        tallenna-vain-muokatut (if (nil? tallenna-vain-muokatut)
                                 true
                                 tallenna-vain-muokatut)

        fokus (atom nil)                                    ;; nyt fokusoitu item [id :sarake]

        vetolaatikot-auki (atom (into #{}
                                      (:vetolaatikot-auki opts)))
        validoi-ja-anna-virheet (fn [virheet uudet-tiedot tyyppi]
                                  (into {}
                                        (keep (fn [rivi]
                                                (if (::poistettu rivi)
                                                  nil
                                                  (let [virheet (validointi/validoi-rivi uudet-tiedot rivi skeema tyyppi)]
                                                    (if (empty? virheet)
                                                      nil
                                                      [((or tunniste :id) rivi) virheet]))))
                                              (vals uudet-tiedot))))
        ohjaus (reify Grid
                 (lisaa-rivi! [this rivin-tiedot]
                   (let [id (or ((or tunniste :id) rivin-tiedot) (swap! uusi-id dec))
                         vanhat-tiedot @muokatut
                         vanhat-virheet @virheet
                         vanhat-varoitukset @varoitukset
                         vanha-jarjestys @jarjestys
                         uudet-tiedot (swap! muokatut assoc id
                                             ((or uusi-rivi identity)
                                               (merge rivin-tiedot {(or tunniste :id) id :koskematon true})))
                         uusi-jarjestys (swap! jarjestys conj id)]
                     (swap! historia conj [vanhat-tiedot vanhat-virheet vanhat-varoitukset vanha-jarjestys])
                     (swap! virheet (fn [virheet]
                                      (validoi-ja-anna-virheet virheet uudet-tiedot :validoi)))
                     (swap! varoitukset (fn [varoitukset]
                                          (validoi-ja-anna-virheet varoitukset uudet-tiedot :varoita)))
                     (log "VIRHEET: " (pr-str @virheet))
                     (when muutos
                       (muutos this))))
                 (hae-muokkaustila [_]
                   @muokatut)
                 (hae-virheet [_]
                   @virheet)
                 (hae-varoitukset [_]
                   @varoitukset)
                 (nollaa-historia! [_]
                   (reset! historia []))
                 (hae-viimeisin-muokattu-id [_]
                   @viimeisin-muokattu-id)

                 (aseta-virhe! [_ rivin-id kentta virheteksti]
                   (swap! virheet assoc-in [rivin-id kentta] [virheteksti]))
                 (poista-virhe! [_ rivin-id kentta]
                   (swap! virheet
                          (fn [virheet]
                            (let [virheet (update-in virheet [rivin-id] dissoc kentta)]
                              (if (empty? (get virheet rivin-id))
                                (dissoc virheet rivin-id)
                                virheet)))))
                 
                 (muokkaa-rivit! [this funktio args]
                   (let [vanhat-tiedot @muokatut
                         vanhat-virheet @virheet
                         vanhat-varoitukset @varoitukset
                         vanha-jarjestys @jarjestys
                         uudet-tiedot (swap! muokatut (fn [muokatut]
                                                        (let [muokatut-jarjestyksessa (map (fn [id]
                                                                                             (get muokatut id))
                                                                                           @jarjestys)]
                                                          (into {}
                                                                (map (juxt (or tunniste :id) #(dissoc % :koskematon)))
                                                                (apply funktio muokatut-jarjestyksessa args)))))]
                     (when-not (= vanhat-tiedot uudet-tiedot)
                       (reset! viimeisin-muokattu-id nil)   ;; bulk muutoksesta ei jätetä viimeisintä muokkausta
                       (swap! historia conj [vanhat-tiedot vanhat-virheet vanhat-varoitukset vanha-jarjestys])
                       (swap! virheet (fn [virheet]
                                        (validoi-ja-anna-virheet virheet uudet-tiedot :validoi)))
                       (swap! varoitukset (fn [varoitukset]
                                            (validoi-ja-anna-virheet varoitukset uudet-tiedot :varoita))))

                     (when muutos
                       (muutos this))))

                 (vetolaatikko-auki? [_ id]
                   (@vetolaatikot-auki id))

                 (avaa-vetolaatikko! [_ id]
                   (swap! vetolaatikot-auki conj id))

                 (sulje-vetolaatikko! [_ id]
                   (swap! vetolaatikot-auki disj id))
                 )

        ;; Tekee yhden muokkauksen säilyttäen undo historian
        muokkaa! (fn [id funktio & argumentit]
                   (let [vanhat-tiedot @muokatut
                         vanhat-virheet @virheet
                         vanhat-varoitukset @varoitukset
                         vanha-jarjestys @jarjestys
                         uudet-tiedot (swap! muokatut
                                             (fn [muokatut]
                                               (let [uusi-data (update-in muokatut [id]
                                                                          (fn [rivi]
                                                                            (apply funktio (dissoc rivi :koskematon) argumentit)))]
                                                 (if prosessoi-muutos
                                                   (prosessoi-muutos uusi-data)
                                                   uusi-data))))]
                     (when-not (= vanhat-tiedot uudet-tiedot)
                       ;;(log "VANHAT: " (pr-str vanhat-tiedot) "\nUUDET: " (pr-str uudet-tiedot))
                       (reset! viimeisin-muokattu-id id)
                       (swap! historia conj [vanhat-tiedot vanhat-virheet vanhat-varoitukset vanha-jarjestys])
                       (swap! virheet (fn [virheet]
                                        (let [uusi-rivi (get uudet-tiedot id)
                                              rivin-virheet (when-not (:poistettu uusi-rivi)
                                                              (validointi/validoi-rivi uudet-tiedot uusi-rivi skeema :validoi))]
                                          (if (empty? rivin-virheet)
                                            (dissoc virheet id)
                                            (assoc virheet id rivin-virheet)))))
                       (swap! varoitukset (fn [varoitukset]
                                            (let [uusi-rivi (get uudet-tiedot id)
                                                  rivin-varoitukset (when-not (:poistettu uusi-rivi)
                                                                      (validointi/validoi-rivi uudet-tiedot uusi-rivi skeema :varoita))]
                                              (if (empty? rivin-varoitukset)
                                                (dissoc varoitukset id)
                                                (assoc varoitukset id rivin-varoitukset))))))
                     (when muutos
                       (muutos ohjaus))))





        ;; Peruu yhden muokkauksen
        peru! (fn []
                (let [[muok virh var jarj] (peek @historia)]
                  (reset! muokatut muok)
                  (reset! virheet virh)
                  (reset! varoitukset var)
                  (reset! jarjestys jarj))
                (swap! historia pop)
                (when muutos
                  (muutos ohjaus)))

        nollaa-muokkaustiedot! (fn []
                                 (reset! virheet {})
                                 (reset! varoitukset {})
                                 (reset! muokatut nil)
                                 (reset! jarjestys nil)
                                 (reset! historia nil)
                                 (reset! viime-assoc nil)
                                 (reset! uusi-id 0))
        aloita-muokkaus! (fn [tiedot]
                           (nollaa-muokkaustiedot!)
                           (loop [muok {}
                                  jarj []
                                  [r & rivit] tiedot]
                             (if-not r
                               (do
                                 (reset! muokatut muok)
                                 (reset! jarjestys jarj))
                               (if (otsikko? r)
                                 (recur muok
                                        (conj jarj r)
                                        rivit)
                                 (let [id ((or tunniste :id) r)]
                                   (recur (assoc muok
                                            id (assoc r :koskematon true)
                                            )
                                          (conj jarj id)
                                          rivit)))))
                           nil)
        ]

    (when-let [ohj (:ohjaus opts)]
      (aseta-grid ohj ohjaus))

    (when muokkaa-aina
      (aloita-muokkaus! tiedot))

    (r/create-class
      {:component-will-receive-props
       (fn [this new-argv]
         ;; jos gridin data vaihtuu, muokkaustila on peruttava, jotta uudet datat tulevat näkyviin
         (nollaa-muokkaustiedot!)
         (when muokkaa-aina
           (aloita-muokkaus! (nth new-argv 3))))

       :reagent-render
       (fn [{:keys [otsikko tallenna tallenna-vain-muokatut peruuta voi-poistaa? voi-lisata? rivi-klikattu
                    muokkaa-footer muokkaa-aina rivin-luokka napit-alaskin? uusi-rivi tyhja vetolaatikot] :as opts} skeema tiedot]
         (let [skeema (laske-sarakkeiden-leveys (keep identity skeema))
               colspan (inc (count skeema))
               muokataan (not (nil? @muokatut))
               muokkauspaneeli (fn [nayta-otsikko?]
                                 [:div.panel-heading
                                  (if-not muokataan
                                    [:span.pull-right.muokkaustoiminnot
                                     (when tallenna
                                       [:button.nappi-ensisijainen (if (= :ei-mahdollinen tallenna)
                                                                     {:disabled (= :ei-mahdollinen tallenna)}
                                                                     {:on-click #(do (.preventDefault %)
                                                                                     (aloita-muokkaus! tiedot))})
                                        [:span.livicon-pen.grid-muokkaa " Muokkaa"]])]
                                    [:span.pull-right.muokkaustoiminnot
                                     [:button.nappi-toissijainen
                                      {:disabled (empty? @historia)
                                       :on-click #(do (.stopPropagation %)
                                                      (.preventDefault %)
                                                      (peru!))}
                                      [:span.livicon-rotate-left " Kumoa"]]

                                     (when-not (= false voi-lisata?)
                                       [:button.nappi-toissijainen.grid-lisaa {:on-click #(do (.preventDefault %)
                                                                                              (lisaa-rivi! ohjaus {}))}
                                        [:span.livicon-plus (or (:lisaa-rivi opts) " Lisää rivi")]])


                                     (when-not muokkaa-aina
                                       [:button.nappi-myonteinen.grid-tallenna
                                        {:disabled (not (empty? @virheet))
                                         :on-click #(let [kaikki-rivit (mapv second @muokatut)
                                                          tallennettavat
                                                          (if tallenna-vain-muokatut
                                                            (filter (fn [rivi] (not (:koskematon rivi))) kaikki-rivit)
                                                            kaikki-rivit)]
                                                     (do (.preventDefault %)
                                                         (go (if (<! (tallenna tallennettavat)))
                                                             (nollaa-muokkaustiedot!))))} ;; kutsu tallenna-fn: määrittele paluuarvo?
                                        [:span.livicon-check " Tallenna"]])

                                     (when-not muokkaa-aina
                                       [:button.nappi-kielteinen.grid-peru
                                        {:on-click #(do
                                                     (.preventDefault %)
                                                     (nollaa-muokkaustiedot!)
                                                     (when peruuta (peruuta))
                                                     nil)}
                                        [:span.livicon-ban " Peruuta"]])
                                     ])
                                  (when nayta-otsikko? [:h6.panel-title otsikko])
                                  ])]
           [:div.panel.panel-default.livi-grid {:class (clojure.string/join " " luokat)}
            (muokkauspaneeli true)
            [:div.panel-body
             (if (nil? tiedot)
               (ajax-loader)
               [:table.grid
                [:thead
                 [:tr
                  (for [{:keys [otsikko leveys nimi]} skeema]
                    ^{:key (str nimi)}
                    [:th {:width (or leveys "5%")} otsikko])
                  (when tallenna [:th.toiminnot {:width "40px"} " "])]]

                [:tbody
                 (if muokataan
                   ;; Muokkauskäyttöliittymä
                   (let [muokatut @muokatut
                         jarjestys @jarjestys
                         tulevat-rivit (fn [aloitus-idx]
                                         ;;(log "TULEVAT RIVIT, alk: " (pr-str aloitus-idx))
                                         ;;(log "jarjestys: " (pr-str (drop (inc aloitus-idx) jarjestys)))
                                         (map #(get muokatut %) (drop (inc aloitus-idx) jarjestys)))]
                     (if (empty? muokatut)
                       [:tr.tyhja [:td {:colSpan colspan} tyhja]]
                       (let [kaikki-virheet @virheet
                             kaikki-varoitukset @varoitukset
                             nykyinen-fokus @fokus]
                         (doall (mapcat #(keep identity %)
                                        (map-indexed
                                          (fn [i id]
                                            (if (otsikko? id)
                                              (let [teksti (:teksti id)]
                                                [^{:key teksti}
                                                [:tr.otsikko
                                                 [:td {:colSpan colspan}
                                                  [:h5 teksti]]]])
                                              (let [rivi (get muokatut id)
                                                    rivin-virheet (get kaikki-virheet id)
                                                    rivin-varoitukset (get kaikki-varoitukset id)]
                                                (when-not (:poistettu rivi)
                                                  [^{:key id}
                                                  [muokkaus-rivi {:ohjaus                   ohjaus
                                                                  :vetolaatikot             vetolaatikot
                                                                  :muokkaa!                 muokkaa!
                                                                  :luokka                   (str (if (even? (+ i 1))
                                                                                                   "parillinen"
                                                                                                   "pariton"))
                                                                  :id                       id
                                                                  :rivin-virheet            rivin-virheet
                                                                  :rivin-varoitukset        rivin-varoitukset
                                                                  :voi-poistaa?             voi-poistaa?
                                                                  :esta-poistaminen?        esta-poistaminen?
                                                                  :esta-poistaminen-tooltip esta-poistaminen-tooltip
                                                                  :fokus                    nykyinen-fokus
                                                                  :aseta-fokus!             #(reset! fokus %)
                                                                  :tulevat-rivit            (tulevat-rivit i)}
                                                   skeema rivi]
                                                   (vetolaatikko-rivi vetolaatikot vetolaatikot-auki id colspan)]))))
                                          jarjestys))))))

                   ;; Näyttömuoto
                   (let [rivit tiedot]
                     (if (empty? rivit)
                       [:tr.tyhja [:td {:col-span colspan} tyhja]]
                       (doall
                         (let [rivit-jarjestetty (sort-by
                                                   (fn [rivi] (if (:yhteenveto rivi) 1 0)) ; Yhteenveto-rivin tulee olla aina viimeisenä
                                                   rivit)]
                           (mapcat #(keep identity %)
                                   (map-indexed
                                     (fn [i rivi]
                                       (if (otsikko? rivi)
                                         [^{:key (:teksti rivi)}
                                         [:tr.otsikko
                                          [:td {:colSpan (inc (count skeema))}
                                           [:h5 (:teksti rivi)]]]]

                                         (let [id ((or tunniste :id) rivi)]
                                           [^{:key id}
                                           [naytto-rivi {:ohjaus        ohjaus
                                                         :vetolaatikot  vetolaatikot
                                                         :id            id
                                                         :tallenna      tallenna
                                                         :luokka        (str (if (even? (+ i 1)) "parillinen" "pariton")
                                                                             (when rivi-klikattu
                                                                               " klikattava ")
                                                                             (when (:yhteenveto rivi) " yhteenveto ")
                                                                             (when rivin-luokka
                                                                               (rivin-luokka rivi)))
                                                         :rivi-klikattu rivi-klikattu}
                                            skeema rivi]
                                            (vetolaatikko-rivi vetolaatikot vetolaatikot-auki id (inc (count skeema)))
                                            ])))
                                     rivit-jarjestetty)))))))]])
             (when (and muokataan muokkaa-footer)
               [muokkaa-footer ohjaus])]
            ;taulukon allekin saa muokkaustoiminnot jos haluaa, tähän panel-heading ilman otsikkoa
            (when napit-alaskin?
              [:span.gridin-napit-alhaalla
               (muokkauspaneeli false)])]))})))


(defn muokkaus-grid
  "Versio gridistä, jossa on vain muokkaustila. Tilan tulee olla muokkauksen vaatimassa {<id> <tiedot>} array mapissa.
  Tiedot tulee olla atomi tai wrapatty data, jota tietojen muokkaus itsessään manipuloi.

Optiot on mappi optioita:
  :muokkaa-footer  optionaalinen footer komponentti joka muokkaustilassa näytetään, parametrina Grid ohjauskahva
  :muutos          jos annettu, kaikista gridin muutoksista tulee kutsu tähän funktioon.
                   Parametrina Grid ohjauskahva
  :uusi-rivi       jos annettu uuden rivin tiedot käsitellään tällä funktiolla 
  :voi-muokata?    jos false, tiedot eivät ole muokattavia ollenkaan 
  :voi-lisata?     jos false, uusia rivejä ei voi lisätä
  :voi-kumota?     jos false, kumoa-nappia ei näytetä
  :voi-poistaa?    funktio, joka palauttaa true tai false.
  :rivinumerot?    Lisää ylimääräisen sarakkeen, joka listaa rivien numerot alkaen ykkösestä
  :jarjesta        jos annettu funktio, sortataan rivit tämän mukaan
  :luokat          Päätason div-elementille annettavat lisäkuokat (vectori stringejä)
  :virheet         atomi gridin virheitä {rivinid {:kentta (\"virhekuvaus\")}}, jos ei anneta
                   luodaan sisäisesti atomi virheille
  :uusi-id         seuraavan uuden luotavan rivin id, jos ei anneta luodaan uusia id:tä
                   sarjana -1, -2, -3, ...
  :validoi-aina?   jos true, validoidaan tiedot aina renderissä (ei vain muutoksessa).
                   Tämä on hyödyllinen, jos gridin tieto muuttuu ulkoisesta syystä.
  "
  [{:keys [otsikko tyhja tunniste voi-poistaa? rivi-klikattu rivinumerot? voi-kumota?
           voi-muokata? voi-lisata? jarjesta
           muokkaa-footer muutos uusi-rivi luokat validoi-aina?] :as opts} skeema muokatut]
  (let [uusi-id (atom 0)                                    ;; tästä dekrementoidaan aina uusia id:tä
        historia (atom [])
        virheet-atom (or (:virheet opts) (atom {}))              ;; validointivirheet: (:id rivi) => [virheet]
        viime-assoc (atom nil)                              ;; edellisen muokkauksen, jos se oli assoc-in, polku
        vetolaatikot-auki (atom (into #{}
                                      (:vetolaatikot-auki opts)))
        ohjaus-fn (fn [muokatut virheet]
                    (reify Grid
                      (lisaa-rivi! [this rivin-tiedot]
                        (let [id (or (:id rivin-tiedot) (swap! uusi-id dec))
                              vanhat-tiedot @muokatut
                              vanhat-virheet @virheet
                              uudet-tiedot (swap! muokatut assoc id
                                                  ((or uusi-rivi identity)
                                                    (merge rivin-tiedot {:id id :koskematon true})))]
                          (swap! historia conj [vanhat-tiedot vanhat-virheet])
                          (swap! virheet (fn [virheet]
                                           (let [rivin-virheet (validointi/validoi-rivi uudet-tiedot (get uudet-tiedot id) skeema)]
                                             (if (empty? rivin-virheet)
                                               (dissoc virheet id)
                                               (assoc virheet id rivin-virheet)))))
                          (when muutos
                            (muutos this))))
                      (hae-muokkaustila [_]
                        @muokatut)
                      (hae-virheet [_]
                        @virheet)
                      (nollaa-historia! [_]
                        (reset! historia []))

                      (vetolaatikko-auki? [_ id]
                        (@vetolaatikot-auki id))
                      (avaa-vetolaatikko! [_ id]
                        (swap! vetolaatikot-auki conj id))
                      (sulje-vetolaatikko! [_ id]
                        (swap! vetolaatikot-auki disj id))
                      ))

        ;; Tekee yhden muokkauksen säilyttäen undo historian
        muokkaa! (fn [muokatut virheet id funktio & argumentit]
                   (let [vanhat-tiedot @muokatut
                         vanhat-virheet @virheet
                         uudet-tiedot (swap! muokatut
                                             (fn [muokatut]
                                               (update-in muokatut [id]
                                                          (fn [rivi]
                                                            (apply funktio (dissoc rivi :koskematon) argumentit)))))]
                     (log "VANHAT TIEDOT: " (pr-str vanhat-tiedot))
                     (log "UUDET TIEDOT: " (pr-str uudet-tiedot))
                          
                     (when-not (= vanhat-tiedot uudet-tiedot)
                       (swap! historia conj [vanhat-tiedot vanhat-virheet])
                       (swap! virheet (fn [virheet]
                                        (let [uusi-rivi (get uudet-tiedot id)
                                              rivin-virheet (when-not (:poistettu uusi-rivi)
                                                              (validointi/validoi-rivi uudet-tiedot uusi-rivi skeema))]
                                          (if (empty? rivin-virheet)
                                            (dissoc virheet id)
                                            (assoc virheet id rivin-virheet))))))
                     (when muutos
                       (muutos (ohjaus-fn muokatut virheet)))))


        ;; Peruu yhden muokkauksen
        peru! (fn [muokatut virheet]
                (let [[muok virh] (peek @historia)]
                  (reset! muokatut muok)
                  (reset! virheet virh))
                (swap! historia pop)
                (when muutos
                  (muutos (ohjaus-fn muokatut virheet))))

        ]


    (r/create-class
      {:component-will-receive-props
       (fn [this [_ {:keys [validoi-aina? virheet]} skeema muokatut]]
         (when validoi-aina?
           (let [muokatut @muokatut]
             (reset! (or virheet virheet-atom)
                     (into {}
                           (keep (fn [[id rivi]]
                                   (let [rivin-virheet (when-not (:poistettu rivi)
                                                         (validointi/validoi-rivi muokatut rivi skeema))]
                                     (when-not (empty? rivin-virheet)
                                       [id rivin-virheet]))))
                           muokatut)))))

       :reagent-render
       (fn [{:keys [otsikko tallenna jarjesta voi-poistaa? voi-muokata? voi-lisata? voi-kumota? rivi-klikattu rivinumerot?
                    muokkaa-footer muokkaa-aina uusi-rivi tyhja vetolaatikot uusi-id validoi-aina?] :as opts} skeema muokatut]
         (let [virheet (or (:virheet opts) virheet-atom)
               skeema (laske-sarakkeiden-leveys skeema)
               colspan (inc (count skeema))
               ohjaus (ohjaus-fn muokatut virheet)
               voi-muokata? (if (nil? voi-muokata?)
                              true
                              voi-muokata?)
               ]
           (when-let [ohj (:ohjaus opts)]
             (aseta-grid ohj ohjaus))

           [:div.panel.panel-default.livi-grid
            {:class (clojure.string/join " " luokat)}
            [:div.panel-heading
             (when otsikko [:h6.panel-title otsikko])
             (when (not= false voi-muokata?)
               [:span.pull-right.muokkaustoiminnot
                (when (not= false voi-kumota?)
                  [:button.nappi-toissijainen
                   {:disabled (empty? @historia)
                    :on-click #(do (.stopPropagation %)
                                   (.preventDefault %)
                                   (peru! muokatut virheet))}
                   (ikonit/peru) " Kumoa"])
                (when (not= false voi-lisata?)
                  [:button.nappi-toissijainen.grid-lisaa
                   {:on-click #(do (.preventDefault %)
                                   (lisaa-rivi! ohjaus
                                                (if uusi-id
                                                  {:id uusi-id}
                                                  {})))}
                   (ikonit/plus) (or (:lisaa-rivi opts) " Lisää rivi")])])]
            [:div.panel-body
             [:table.grid
              [:thead
               [:tr
                (if rivinumerot? [:th {:width "40px"} " "])
                (for [{:keys [otsikko leveys nimi]} skeema]
                  ^{:key (str nimi)}
                  [:th {:width (or leveys "5%")} otsikko])
                [:th.toiminnot {:width "40px"} " "]]]

              [:tbody
               (let [muokatut-atom muokatut
                     muokatut @muokatut]
                 (if (every? :poistettu (vals muokatut))
                   [:tr.tyhja [:td {:col-span (inc (count skeema))} tyhja]]
                   (let [kaikki-virheet @virheet]
                     (doall
                       (mapcat
                         #(keep identity %)
                         (map-indexed
                           (fn [i [id rivi]]
                             (let [rivin-virheet (get kaikki-virheet id)]
                               (when-not (:poistettu rivi)
                                 [
                                  ^{:key id}
                                  [:tr.muokataan {:class (str (if (even? (+ i 1))
                                                                "parillinen"
                                                                "pariton"))}
                                   (if rivinumerot? [:td (+ i 1)])
                                   (for [{:keys [nimi hae aseta fmt muokattava? tyyppi] :as s} skeema]
                                     (if (= :vetolaatikon-tila tyyppi)
                                       ^{:key (str "vetolaatikontila" id)}
                                       [vetolaatikon-tila ohjaus vetolaatikot id]
                                       (let [s (assoc s :rivi rivi)
                                             arvo (if hae
                                                    (hae rivi)
                                                    (get rivi nimi))
                                             kentan-virheet (get rivin-virheet nimi)]
                                         (if (or (nil? muokattava?) (muokattava? rivi i))
                                           ^{:key (str nimi)}
                                           [:td {:class (str (when-not (empty? kentan-virheet)
                                                               "sisaltaa-virheen"))}
                                            (when-not (empty? kentan-virheet)
                                              (virheen-ohje kentan-virheet))

                                            (if voi-muokata?
                                              [tee-kentta s (r/wrap
                                                              arvo
                                                              (fn [uusi]
                                                                (if aseta
                                                                  (muokkaa! muokatut-atom virheet
                                                                            id (fn [rivi]
                                                                                 (aseta rivi uusi)))
                                                                  (muokkaa! muokatut-atom virheet id assoc nimi uusi))))]
                                              [nayta-arvo s (vain-luku-atomina arvo)])]

                                           ^{:key (str nimi)}
                                           [:td ((or fmt str) (if hae
                                                                (hae rivi)
                                                                (get rivi nimi)))]))))
                                   [:td.toiminnot
                                    (when (and (not= false voi-muokata?)
                                               (or (nil? voi-poistaa?) (voi-poistaa? rivi)))
                                      [:span.klikattava {:on-click #(do (.preventDefault %)
                                                                        (muokkaa! muokatut-atom virheet id assoc :poistettu true))}
                                       (ikonit/trash)])
                                    (when-not (empty? rivin-virheet)
                                      [:span.rivilla-virheita
                                       (ikonit/warning-sign)])]]

                                  (vetolaatikko-rivi vetolaatikot vetolaatikot-auki id colspan)])))
                           (if jarjesta
                             (sort-by (comp jarjesta second) (seq muokatut))
                             (seq muokatut))))))))]]
             (when (and (not= false voi-muokata?) muokkaa-footer)
               [muokkaa-footer ohjaus])
             ]]))})))

; Apufunktiot

(defn filteroi-uudet-poistetut
  "Ottaa datan muokkausgrid-muodossa (avaimet kokonaislukuja, jotka mappautuvat riveihin) ja palauttaa sellaiset
  rivit, jotka eivät ole uusia ja poistettuja. Paluuarvo on vectori mappeja."
  [rivit]
  (filter
    #(not (and (true? (:poistettu %))
               (neg? (:id %)))) (vals rivit)))

(defn poista-idt
  "Ottaa mapin ja polun. Olettaa, että polun päässä on vector.
  Palauttaa mapin, jossa polussa olevasta vectorista on jokaisesta itemistä poistettu id"
  [lomake polku] (assoc-in lomake polku (mapv
                                          (fn [rivi] (dissoc rivi :id))
                                          (get-in lomake polku))))
