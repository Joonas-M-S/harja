(ns harja.views.urakka.kohdeluettelo.toteumat
  "Urakan kohdeluettelon toteumat"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.lomake :as lomake]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko]]
            [harja.ui.komponentti :as komp]
            [harja.domain.paallystys.pot :as pot]

            [harja.tiedot.navigaatio :as nav]
            [harja.fmt :as fmt]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.kentat :refer [tee-kentta]]
            [harja.asiakas.kommunikaatio :as k]
            [cljs.core.async :refer [<!]]
            [harja.tiedot.urakka :as u]
            [harja.ui.lomake :refer [lomake]]
            [harja.tiedot.urakka.paallystys :as paallystys]
            [harja.domain.roolit :as roolit]
            [harja.ui.kommentit :as kommentit])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(defn tila-keyword->string [tila]
  (case tila
    :aloitettu "Aloitettu"
    :valmis "Valmis, odottaa käsittelyä"
    :lukittu "Lukittu"
    "-"))

(defn paatos-keyword->string [tila]
  (case tila
    :hyvaksytty "Hyväksytty"
    :hylatty "Palautettu urakoitsijalle"
    ""))

(def lomakedata (atom nil))                                 ; Vastaa rakenteeltaan päällystysilmoitus-taulun sisältöä

(def urakkasopimuksen-mukainen-kokonaishinta (reaction (:tarjoushinta @lomakedata)))
(def muutokset-kokonaishintaan                              ; Lasketaan jokaisesta työstä muutos tilattuun hintaan (POT-Excelistä "Muutos hintaan") ja summataan yhteen.
  (reaction (reduce + (mapv
                        (fn [tyo]
                          (* (- (:toteutunut-maara tyo) (:tilattu-maara tyo)) (:yksikkohinta tyo)))
                        (:tyot (:ilmoitustiedot @lomakedata))))))

(def yhteensa (reaction (+ @urakkasopimuksen-mukainen-kokonaishinta @muutokset-kokonaishintaan)))


(tarkkaile! "PÄÄ Lomakedata: " lomakedata)

(defn yhteenveto []
  (let []
    [:div.pot-yhteenveto
     [:table
      [:tr
       [:td.pot-yhteenveto-nimi [:span "Urakkasopimuksen mukainen kokonaishinta: "]]
       [:td.pot-yhteenveto-summa [:span (fmt/euro-opt (or @urakkasopimuksen-mukainen-kokonaishinta 0))]]]
      [:tr
       [:td.pot-yhteenveto-nimi [:span "Muutokset kokonaishintaan ilman kustannustasomuutoksia: "]]
       [:td.pot-yhteenveto-summa [:span (fmt/euro-opt (or @muutokset-kokonaishintaan 0))]]]
      [:tr
       [:td.pot-yhteenveto-nimi [:span "Yhteensä: "]]
       [:td.pot-yhteenveto-summa [:span (fmt/euro-opt @yhteensa)]]]]]))

(defn kuvaile-paatostyyppi [paatos]
  (case paatos
    :hyvaksytty "Hyväksytty"
    :hylatty "Palauta urakoitsijalle"))

(defn kasittely
  "Ilmoituksen käsittelyosio, kun ilmoitus on valmis. Tilaaja voi muokata, urakoitsija voi tarkastella."
  [valmis-kasiteltavaksi?]
  (let [muokattava? (constantly (and
                                  (roolit/roolissa? roolit/urakanvalvoja)
                                  (and (not (= (:tila @lomakedata) :lukittu)))))
        paatostiedot (r/wrap {:paatos        (:paatos @lomakedata)
                              :perustelu     (:perustelu @lomakedata)
                              :kasittelyaika (:kasittelyaika @lomakedata)}
                             (fn [uusi-arvo] (reset! lomakedata (-> (assoc @lomakedata :paatos (:paatos uusi-arvo))
                                                                    (assoc :perustelu (:perustelu uusi-arvo))
                                                                    (assoc :kasittelyaika (:kasittelyaika uusi-arvo))))))]
    (when @valmis-kasiteltavaksi?
      [:div.pot-kasittely
       [:h3 "Käsittely ja päätös"]
       [lomake/lomake
        {:luokka   :horizontal
         :muokkaa! (fn [uusi]
                     (reset! paatostiedot uusi))}
        [{:otsikko     "Käsittelyn pvm"
          :nimi        :kasittelyaika
          :tyyppi      :pvm-aika
          :validoi     [[:ei-tyhja "Anna käsittelypäivämäärä"]]
          :muokattava? muokattava?}

         {:otsikko       "Päätös"
          :nimi          :paatos
          :tyyppi        :valinta
          :valinnat      [:hyvaksytty :hylatty]
          :muokattava?   muokattava?
          :validoi       [[:ei-tyhja "Anna päätös"]]
          :valinta-nayta #(if % (kuvaile-paatostyyppi %) (if (muokattava?) "- Valitse päätös -" ""))
          :leveys-col    4}

         (when (:paatos @paatostiedot)
           {:otsikko     "Päätöksen selitys"
            :nimi        :perustelu
            :tyyppi      :text
            :koko        [80 4]
            :leveys-col  6
            :validoi     [[:ei-tyhja "Anna päätöksen selitys"]]
            :muokattava? muokattava?})]
        @paatostiedot]])))

(defn tallennus
  [valmis-tallennettavaksi?]
  (let [huomautusteksti (reaction (let [valmispvm (:valmistumispvm @lomakedata)]
                                    (if (not valmispvm)
                                      "Valmistusmispäivämäärää ei annettu, ilmoitus tallennetaan keskeneräisenä.")))
        urakka-id (:id @nav/valittu-urakka)
        [sopimus-id _] @u/valittu-sopimusnumero]

    [:div.pot-tallennus
     [:div.pot-huomaus @huomautusteksti]

     [harja.ui.napit/palvelinkutsu-nappi
      "Tallenna"
      #(let [lahetettava-data @lomakedata]
        (log "PÄÄ Lähetetään lomake: " (pr-str lahetettava-data))
        (paallystys/tallenna-paallystysilmoitus urakka-id sopimus-id lahetettava-data))
      {:luokka       "nappi-ensisijainen"
       :disabled     (false? @valmis-tallennettavaksi?)
       :kun-onnistuu (fn [vastaus]
                       (log "PÄÄ Lomake tallennettu, vastaus: " (pr-str vastaus))
                       (reset! paallystys/paallystystoteumat vastaus)
                       (reset! lomakedata nil))}]]))

(defn paallystysilmoituslomake []
  (let [kohteen-tiedot (r/wrap {:aloituspvm     (:aloituspvm @lomakedata)
                                :valmistumispvm (:valmistumispvm @lomakedata)
                                :takuupvm       (:takuupvm @lomakedata)
                                :hinta          (fmt/euro-opt (+ @urakkasopimuksen-mukainen-kokonaishinta @muutokset-kokonaishintaan))}
                               (fn [uusi-arvo]
                                 (reset! lomakedata (-> (assoc @lomakedata :aloituspvm (:aloituspvm uusi-arvo))
                                                        (assoc :valmistumispvm (:valmistumispvm uusi-arvo))
                                                        (assoc :takuupvm (:takuupvm uusi-arvo))
                                                        (assoc :hinta (:hinta uusi-arvo))))))

        ; Sisältää päällystystoimenpiteen tiedot, koska one-to-one -suhde.
        toteutuneet-osoitteet
        (r/wrap (zipmap (iterate inc 1) (:osoitteet (:ilmoitustiedot @lomakedata)))
                (fn [uusi-arvo] (reset! lomakedata
                                        (assoc-in @lomakedata [:ilmoitustiedot :osoitteet] (grid/filteroi-uudet-poistetut uusi-arvo)))))

        ; Kiviaines sisältää sideaineen, koska one-to-one -suhde
        kiviaines
        (r/wrap (zipmap (iterate inc 1) (:kiviaines (:ilmoitustiedot @lomakedata)))
                (fn [uusi-arvo] (reset! lomakedata
                                        (assoc-in @lomakedata [:ilmoitustiedot :kiviaines] (grid/filteroi-uudet-poistetut uusi-arvo)))))
        alustalle-tehdyt-toimet
        (r/wrap (zipmap (iterate inc 1) (:alustatoimet (:ilmoitustiedot @lomakedata)))
                (fn [uusi-arvo] (reset! lomakedata
                                        (assoc-in @lomakedata [:ilmoitustiedot :alustatoimet] (grid/filteroi-uudet-poistetut uusi-arvo)))))
        toteutuneet-maarat
        (r/wrap (zipmap (iterate inc 1) (:tyot (:ilmoitustiedot @lomakedata)))
                (fn [uusi-arvo] (reset! lomakedata
                                        (assoc-in @lomakedata [:ilmoitustiedot :tyot] (grid/filteroi-uudet-poistetut uusi-arvo)))))

        alikohteet-virheet (atom {})
        paallystystoimenpide-virheet (atom {})
        alustalle-tehdyt-toimet-virheet (atom {})
        toteutuneet-maarat-virheet (atom {})
        kiviaines-virheet (atom {})

        valmis-tallennettavaksi? (reaction
                                   (let [alikohteet-virheet @alikohteet-virheet
                                         paallystystoimenpide-virheet @paallystystoimenpide-virheet
                                         alustalle-tehdyt-toimet-virheet @alustalle-tehdyt-toimet-virheet
                                         toteutuneet-maarat-virheet @toteutuneet-maarat-virheet
                                         kiviaines-virheet @kiviaines-virheet
                                         tila (:tila @lomakedata)]
                                     (and
                                       (not (= tila :lukittu))
                                       ;(empty? alikohteet-virheet) FIXME Ei validoi oikein kun lisätään rivi? Liittyy jotenkin tienumeron kopiointiin muille riveille.
                                       ;(empty? paallystystoimenpide-virheet)
                                       (empty? alustalle-tehdyt-toimet-virheet)
                                       (empty? toteutuneet-maarat-virheet)
                                       (empty? kiviaines-virheet))))
        valmis-kasiteltavaksi? (reaction
                                 (let [valmispvm (:valmistumispvm @lomakedata)
                                       toteutuneet-osoitteet (:osoitteet (:ilmoitustiedot @lomakedata))
                                       toteutuneet-maarat (:tyot (:ilmoitustiedot @lomakedata))
                                       tila (:tila @lomakedata)]
                                   (and (not (= tila :aloitettu))
                                        (not (nil? valmispvm))
                                        (not (empty? toteutuneet-osoitteet))
                                        (not (empty? toteutuneet-maarat)))))]

    (komp/luo
      (fn []
        [:div.paallystysilmoituslomake

         [:button.nappi-toissijainen {:on-click #(reset! lomakedata nil)}
          (ikonit/chevron-left) " Takaisin toteumaluetteloon"]

         [:h3 "Kohteen tiedot"]

         [lomake {:luokka   :horizontal
                  :muokkaa! (fn [uusi]
                              (log "PÄÄ Muokataan kohteen tietoja: " (pr-str uusi))
                              (reset! kohteen-tiedot uusi))}
          [{:otsikko "Kohde" :nimi :kohde :hae (fn [_] (str "#" (:kohdenumero @lomakedata) " " (:kohdenimi @lomakedata))) :muokattava? (constantly false)}
           {:otsikko "Aloitettu" :nimi :aloituspvm :tyyppi :pvm}
           {:otsikko "Valmistunut" :nimi :valmistumispvm :tyyppi :pvm}
           {:otsikko "Takuupvm" :nimi :takuupvm :tyyppi :pvm}
           {:otsikko "Toteutunut hinta" :nimi :hinta :tyyppi :numero :leveys-col 2 :muokattava? (constantly false)}
           (when (not (= :aloitettu (:tila @lomakedata)))
             {:otsikko     "Kommentit" :nimi :kommentit
              :komponentti [kommentit/kommentit {:voi-kommentoida? true
                                                 :voi-liittaa      false
                                                                   :placeholder "Kirjoita kommentti..."
                                                                   :uusi-kommentti (r/wrap (:uusi-kommentti @lomakedata)
                                                                                           #(swap! lomakedata assoc :uusi-kommentti %))}
                            (:kommentit @lomakedata)]})
           ]
          @kohteen-tiedot]

         [:h3 "Tekninen puoli"]

         [grid/muokkaus-grid
          {:otsikko      "Toteutuneet alikohteet"
           :tunniste     :tie
           :rivinumerot? true
           :muutos       (fn [g]
                           (let [grid-data (into [] (vals (grid/hae-muokkaustila g)))]
                             (log "PÄÄ grid-data " (pr-str grid-data))
                             (reset! toteutuneet-osoitteet (zipmap (iterate inc 1) (mapv (fn [rivi] (assoc rivi :tie (:tie (first grid-data)))) grid-data)))
                             (reset! alikohteet-virheet (grid/hae-virheet g))))}
          [{:otsikko     "Tie#" :nimi :tie :tyyppi :numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]
            :muokattava? (fn [rivi index] (if (> index 0) false true))}
           {:otsikko       "Ajorata"
            :nimi          :ajorata
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse ajorata -")
            :valinnat      pot/+ajoradat+
            :leveys        "20%"
            :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko       "Suunta"
            :nimi          :suunta
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse suunta -")
            :valinnat      pot/+suunnat+
            :leveys        "20%"
            :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko       "Kaista"
            :nimi          :kaista
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse kaista -")
            :valinnat      pot/+kaistat+
            :leveys        "20%"
            :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Alkutieosa" :nimi :aosa :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Alkuetäisyys" :nimi :aet :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Lopputieosa" :nimi :losa :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Loppuetäisyys" :nimi :let :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Pituus (m)" :nimi :pituus :leveys "10%" :tyyppi :numero :muokattava? (constantly false) :hae (fn [rivi] (- (:let rivi) (:losa rivi)))}] ; FIXME Onko oikein laskettu?
          toteutuneet-osoitteet]

         [grid/muokkaus-grid
          {:otsikko      "Päällystystoimenpiteen tiedot"
           :voi-lisata?  false
           :voi-poistaa? (constantly false)
           :rivinumerot? true
           :muutos       #(reset! paallystystoimenpide-virheet (grid/hae-virheet %))}
          [{:otsikko       "Päällyste"
            :nimi          :paallystetyyppi
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse päällyste -")
            :valinnat      pot/+paallystetyypit+
            :leveys        "30%"
            :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Raekoko" :nimi :raekoko :tyyppi :numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Massa (kg/m2)" :nimi :massa :tyyppi :numero :leveys "10%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "RC-%" :nimi :rc% :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko       "Pääl. työmenetelmä"
            :nimi          :tyomenetelma
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse menetelmä -")
            :valinnat      pot/+tyomenetelmat+
            :leveys        "30%"
            :validoi       [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Leveys (m)" :nimi :leveys :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Massa (kg/m2)" :nimi :massamaara :leveys "15%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Pinta-ala (m2)" :nimi :pinta-ala :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko       "Edellinen päällyste"
            :nimi          :edellinen-paallystetyyppi
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse päällyste -")
            :valinnat      pot/+paallystetyypit+
            :leveys        "30%"}]
          toteutuneet-osoitteet]

         [grid/muokkaus-grid
          {:otsikko "Kiviaines ja sideaine"
           :muutos  #(reset! kiviaines-virheet (grid/hae-virheet %))}
          [{:otsikko "Kiviaines-esiintymä" :nimi :esiintyma :tyyppi :string :pituus-max 256 :leveys "30%"}
           {:otsikko "KM-arvo" :nimi :km-arvo :tyyppi :string :pituus-max 256 :leveys "20%"}
           {:otsikko "Muotoarvo" :nimi :muotoarvo :tyyppi :string :pituus-max 256 :leveys "20%"}
           {:otsikko "Sideaine-tyyppi" :nimi :sideainetyyppi :leveys "30%" :tyyppi :string :pituus-max 256}
           {:otsikko "Pitoisuus" :nimi :pitoisuus :leveys "20%" :tyyppi :numero}
           {:otsikko "Lisäaineet" :nimi :lisaaineet :leveys "20%" :tyyppi :string :pituus-max 256}]
          kiviaines]

         [grid/muokkaus-grid
          {:otsikko "Alustalle tehdyt toimet"
           :muutos  #(reset! alustalle-tehdyt-toimet-virheet (grid/hae-virheet %))}
          [{:otsikko "Alkutieosa" :nimi :aosa :tyyppi :string :leveys "10%" :pituus-max 256}
           {:otsikko "Alkuetäisyys" :nimi :aet :tyyppi :numero :leveys "10%"}
           {:otsikko "Lopputieosa" :nimi :losa :tyyppi :numero :leveys "10%"}
           {:otsikko "Loppuetäisyys" :nimi :let :leveys "10%" :tyyppi :numero}
           {:otsikko "Pituus (m)" :nimi :pituus :leveys "10%" :tyyppi :numero :muokattava? (constantly false) :hae (fn [rivi] (- (:let rivi) (:losa rivi)))} ; FIXME Onko oikein laskettu?
           {:otsikko       "Käsittelymenetelmä"
            :nimi          :kasittelymenetelma
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse menetelmä -")
            :valinnat      pot/+alustamenetelmat+
            :leveys        "30%"}
           {:otsikko "Käsittelypaks. (cm)" :nimi :paksuus :leveys "10%" :tyyppi :numero}
           {:otsikko       "Verkkotyyppi"
            :nimi          :verkkotyyppi
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse verkkotyyppi -")
            :valinnat      pot/+verkkotyypit+
            :leveys        "30%"}
           {:otsikko       "Tekninen toimenpide"
            :nimi          :tekninen-toimenpide
            :tyyppi        :valinta
            :valinta-arvo  :koodi
            :valinta-nayta #(if % (:nimi %) "- Valitse toimenpide -")
            :valinnat      pot/+tekniset-toimenpiteet+
            :leveys        "30%"}]
          alustalle-tehdyt-toimet]

         [:h3 "Talouspuoli"]

         [grid/muokkaus-grid
          {:otsikko "Toteutuneet määrät"
           :muutos  #(reset! toteutuneet-maarat-virheet (grid/hae-virheet %))}
          [{:otsikko       "Päällystetyön tyyppi"
            :nimi          :tyyppi
            :tyyppi        :valinta
            :valinta-arvo  :avain
            :valinta-nayta #(if % (:nimi %) "- Valitse työ -")
            :valinnat      pot/+paallystystyon-tyypit+
            :leveys        "30%"}
           {:otsikko "Yks." :nimi :yksikko :tyyppi :string :leveys "10%" :pituus-max 256}
           {:otsikko "Tilattu määrä" :nimi :tilattu-maara :tyyppi :numero :leveys "15%" :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Toteutunut määrä" :nimi :toteutunut-maara :leveys "15%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Ero" :nimi :ero :leveys "15%" :tyyppi :numero :muokattava? (constantly false) :hae (fn [rivi] (- (:toteutunut-maara rivi) (:tilattu-maara rivi)))}
           {:otsikko "Yks.hinta" :nimi :yksikkohinta :leveys "10%" :tyyppi :numero :validoi [[:ei-tyhja "Tieto puuttuu"]]}
           {:otsikko "Muutos hintaan" :nimi :muutos-hintaan :leveys "15%" :muokattava? (constantly false) :tyyppi :numero :hae (fn [rivi] (* (- (:toteutunut-maara rivi) (:tilattu-maara rivi)) (:yksikkohinta rivi)))}]
          toteutuneet-maarat]

         (yhteenveto)
         (kasittely valmis-kasiteltavaksi?)
         (tallennus valmis-tallennettavaksi?)]))))

(defn toteumaluettelo
  []
  (let []

    (komp/luo
      (fn []
        [:div
         [grid/grid
          {:otsikko  "Toteumat"
           :tyhja    (if (nil? @paallystys/paallystystoteumat) [ajax-loader "Haetaan toteumia..."] "Ei toteumia")
           :tunniste :kohdenumero}
          [{:otsikko "#" :nimi :kohdenumero :muokattava? (constantly false) :tyyppi :numero :leveys "10%"}
           {:otsikko "Nimi" :nimi :nimi :muokattava? (constantly false) :tyyppi :string :leveys "50%"}
           {:otsikko "Tila" :nimi :tila :muokattava? (constantly false) :tyyppi :string :leveys "20%" :hae (fn [rivi]
                                                                                                             (if (nil? (:tila rivi))
                                                                                                               "-"
                                                                                                               (if (nil? (:paatos rivi))
                                                                                                                 (str (tila-keyword->string (:tila rivi)))
                                                                                                                 (paatos-keyword->string (:paatos rivi)))))}
           {:otsikko     "Päällystysilmoitus" :nimi :paallystysilmoitus :muokattava? (constantly false) :leveys "25%" :tyyppi :komponentti
            :komponentti (fn [rivi] (if (:tila rivi) [:button.nappi-toissijainen.nappi-grid {:on-click #(go
                                                                                                         (let [urakka-id (:id @nav/valittu-urakka)
                                                                                                               [sopimus-id _] @u/valittu-sopimusnumero
                                                                                                               vastaus (<! (paallystys/hae-paallystysilmoitus-paallystyskohteella urakka-id sopimus-id (:paallystyskohde_id rivi)))]
                                                                                                           (log "PÄÄ Rivi: " (pr-str rivi))
                                                                                                           (log "PÄÄ Vastaus: " (pr-str vastaus))
                                                                                                           (reset! lomakedata (assoc vastaus :paallystyskohde-id (:paallystyskohde_id rivi)))))}
                                                      [:span (ikonit/eye-open) " Päällystysilmoitus"]]
                                                     [:button.nappi-toissijainen.nappi-grid {:on-click #(reset! lomakedata {:kohdenumero        (:kohdenumero rivi)
                                                                                                                            :kohdenimi          (:nimi rivi)
                                                                                                                            :paallystyskohde-id (:paallystyskohde_id rivi)
                                                                                                                            :tarjoushinta       (:sopimuksen_mukaiset_tyot rivi)})}
                                                      [:span " Tee päällystysilmoitus"]]))}]
          (sort-by
            (fn [toteuma] (case (:tila toteuma)
                            :lukittu 0
                            :valmis 1
                            :aloitettu 3
                            4))
            @paallystys/paallystystoteumat)]]))))

(defn toteumat []
  (if @lomakedata
    [paallystysilmoituslomake]
    [toteumaluettelo]))