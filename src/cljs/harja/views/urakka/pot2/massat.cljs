(ns harja.views.urakka.pot2.massat
  "POT2 massalistaukset"
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<! chan]]
            [reagent.core :refer [atom] :as r]
            [tuck.core :as tuck]

            [harja.ui.grid :as grid]
            [harja.ui.debug :refer [debug]]
            [harja.ui.dom :as dom]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.kentat :as kentat]
            [harja.ui.komponentti :as komp]
            [harja.ui.lomake :as ui-lomake]
            [harja.ui.modal :as modal]
            [harja.ui.napit :as napit]
            [harja.ui.varmista-kayttajalta :as varmista-kayttajalta]
            [harja.ui.yleiset :refer [ajax-loader linkki livi-pudotusvalikko virheen-ohje] :as yleiset]
            [harja.domain.paallystysilmoitus :as paallystysilmoitus-domain]
            [harja.domain.pot2 :as pot2-domain]
            [harja.tiedot.urakka.pot2.validoinnit :as pot2-validoinnit]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.pot2.materiaalikirjasto :as mk-tiedot]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.tiedot.urakka.pot2.pot2-tiedot :as pot2-tiedot]
            [harja.validointi :as v]
            [harja.views.urakka.pot2.massa-ja-murske-yhteiset :as mm-yhteiset])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(defn- aineen-otsikko-checkbox
  [e! {:keys [otsikko arvo label-luokka polku]}]
  [kentat/tee-kentta
   {:tyyppi :checkbox :label-luokka label-luokka
    :teksti otsikko :vayla-tyyli? true
    :nayta-rivina? true :iso-clickalue? true}
   (r/wrap
     arvo
     (fn [uusi-arvo]
       (e! (mk-tiedot/->PaivitaAineenTieto polku uusi-arvo))))])

(defn- otsikko-ja-kentta
  [e! {:keys [otsikko tyyppi arvo polku pakollinen? leveys placeholder
              valinnat valinta-nayta valinta-arvo validoi-kentta-fn
              desimaalien-maara kokonaisluku?]}]
  [:div.otsikko-ja-kentta.inline-block
   [:div
    [:span.kentan-label otsikko]
    (when pakollinen? [:span.required-tahti " *"])]
   [kentat/tee-kentta {:tyyppi tyyppi :teksti otsikko :nayta-rivina? false
                       :leveys leveys :placeholder placeholder
                       :valinnat valinnat :valinta-nayta valinta-nayta
                       :valinta-arvo valinta-arvo
                       :validoi-kentta-fn validoi-kentta-fn
                       :desimaalien-maara desimaalien-maara
                       :kokonaisluku? kokonaisluku?}
    (r/wrap
      arvo
      (fn [uusi-arvo]
        (e! (mk-tiedot/->PaivitaAineenTieto polku uusi-arvo))))]])

(defn- runkoaineiden-kentat [tiedot polun-avaimet]
  (let [{:runkoaine/keys [esiintyma fillerityyppi kuulamyllyarvo litteysluku
                          massaprosentti kuvaus]} tiedot
        aineen-koodi (second polun-avaimet)]
    ;; Käyttöliittymäsuunnitelman mukaisesti tietyillä runkoaineilla on tietyt kentät
    ;; ja ainekohtaisia eroja käsitellään tässä contains? funktion avulla
    (remove
      nil?
      [(when-not (contains? #{3 7} aineen-koodi)
         {:otsikko "Kiviainesesiintymä"
          :tyyppi :string :pakollinen? true
          :arvo esiintyma :leveys "150px"
          :polku (conj polun-avaimet :runkoaine/esiintyma)})
       (when (contains? #{3} aineen-koodi)
         {:otsikko "Tyyppi" :valinnat pot2-domain/erikseen-lisattava-fillerikiviaines
          :tyyppi :valinta :pakollinen? true
          :arvo fillerityyppi :leveys "150px"
          :polku (conj polun-avaimet :runkoaine/fillerityyppi)})
       (when-not (contains? #{3 7} aineen-koodi)
         {:otsikko "Kuulamyllyarvo" :pakollinen? true
          :tyyppi :numero :desimaalien-maara 1
          :arvo kuulamyllyarvo :leveys "55px"
          :validoi-kentta-fn (fn [numero] (v/validoi-numero numero 0 40 1))
          :polku (conj polun-avaimet :runkoaine/kuulamyllyarvo)})
       (when-not (contains? #{3 7} aineen-koodi)
         {:otsikko "Litteysluku" :pakollinen? true
          :tyyppi :numero :desimaalien-maara 1
          :arvo litteysluku :leveys "68px"
          :validoi-kentta-fn (fn [numero] (v/validoi-numero numero 0 40 1))
          :polku (conj polun-avaimet :runkoaine/litteysluku)})
       (when (contains? #{7} aineen-koodi)
         {:otsikko "Kuvaus" :placeholder "Anna ainetta kuvaava nimi"
          :tyyppi :string :pakollinen? true
          :arvo kuvaus :leveys "160px"
          :polku (conj polun-avaimet :runkoaine/kuvaus)})
       {:otsikko "Massa-%" :pakollinen? true
        :tyyppi :numero :kokonaisluku? true
        :arvo massaprosentti :leveys "55px"
        :validoi-kentta-fn (fn [numero] (v/validoi-numero numero 0 100 0))
        :polku (conj polun-avaimet :runkoaine/massaprosentti)}])))

(defn- sideaineiden-kentat [tiedot polun-avaimet idx]
  (let [{:sideaine/keys [tyyppi pitoisuus]
         sideainetyypit :sideainetyypit} tiedot]
    [{:otsikko "Tyyppi" :valinnat sideainetyypit
      :tyyppi :valinta :pakollinen? true
      :valinta-nayta ::pot2-domain/nimi
      :valinta-arvo ::pot2-domain/koodi
      :arvo tyyppi :leveys "250px"
      :polku (conj polun-avaimet :aineet idx :sideaine/tyyppi)}
     {:otsikko "Pitoisuus"
      :tyyppi :numero :pakollinen? true
      :arvo pitoisuus :leveys "55px"
      :validoi-kentta-fn (fn [numero] (v/validoi-numero numero 0 100 1))
      :polku (conj polun-avaimet :aineet idx :sideaine/pitoisuus)}]))

(defn- lisaaineiden-kentat [tiedot polun-avaimet]
  (let [{:lisaaine/keys [pitoisuus]} tiedot]
    [{:otsikko "Pitoisuus" :pakollinen? true
      :tyyppi :numero :desimaalien-maara 1
      :arvo pitoisuus :leveys "70px"
      :validoi-kentta-fn (fn [numero] (v/validoi-numero numero 0 100 1))
      :polku (conj polun-avaimet :lisaaine/pitoisuus)}]))

(defn- tyypin-kentat [{:keys [tyyppi tiedot polun-avaimet]}]
  (case tyyppi
    :runkoaineet (runkoaineiden-kentat tiedot polun-avaimet)
    :sideaineet (sideaineiden-kentat tiedot polun-avaimet 0)
    :lisaaineet (lisaaineiden-kentat tiedot polun-avaimet)

    nil))


(defn- sideaineet-komponentti [e! rivi kayttotapa polun-avaimet sideainetyypit]
  (let [aineet (or (get-in rivi (cons :data [::pot2-domain/sideaineet kayttotapa :aineet]))
                   {0 mk-tiedot/tyhja-sideaine})
        jo-valitut-sideainetyypit (into #{} (map :sideaine/tyyppi (vals aineet)))]
    [:div
     (map-indexed (fn [idx [_ sideaine]]
                    ;; Tässä tarjotaan käyttäjälle mahdollisena sideainetyyppinä kaikki sellaiset, joita ei ole vielä ko. massalle
                    ;; valittu.
                    (let [sideainetyyppivalinnat (vec (remove #(and (jo-valitut-sideainetyypit (::pot2-domain/koodi %))
                                                                    ;;  Jotta nykyinen valinta näkyy, se pitää jättää poistamatta
                                                                    (not= (::pot2-domain/koodi %)
                                                                          (:sideaine/tyyppi sideaine)))
                                                              sideainetyypit))]
                      ^{:key (str idx)}
                      [:div
                       (for [sak (sideaineiden-kentat (merge
                                                        sideaine
                                                        {:sideainetyypit sideainetyyppivalinnat})
                                                      polun-avaimet idx)]

                         ^{:key (str idx (:otsikko sak))}
                         [:div.inline-block {:style {:margin-right "6px"}}
                          [otsikko-ja-kentta e! sak]])]))
                  aineet)
     (when (= :lisatty kayttotapa)
       [:div
        [napit/uusi "Lisää uusi"
         #(e! (mk-tiedot/->LisaaSideaine kayttotapa))
         {:luokka (str "napiton-nappi lisaa-sideaine")}]
        [napit/poista "Poista viimeisin"
         #(e! (mk-tiedot/->PoistaSideaine kayttotapa))
         {:luokka (str "napiton-nappi poista-sideaine")}]])]))

(defn- ainevalintalaatikot [tyyppi aineet]
  (if (= tyyppi :sideaineet)
    mk-tiedot/sideaineen-kayttotavat
    aineet))

(defn- massan-aineen-lukutila [{:keys [tiedot tyyppi aineen-otsikko]}]
  (let [{:runkoaine/keys [massaprosentti kuulamyllyarvo litteysluku pitoisuus
                          esiintyma]} tiedot]
    [:div.lukutilan-tausta
     [:div.aineen-pitoisuus-ja-nimi
      [:div.pitoisuus massaprosentti]
      [:div.nimi aineen-otsikko]]
     [:div.massan-yksityiskohdat
      (when kuulamyllyarvo [:span (str "KM-arvo " kuulamyllyarvo)])
      (when litteysluku [:span (str "Litteysluku " litteysluku)])
      (when esiintyma [:span esiintyma])
      ]
     ]))

(defn- ainevalinta-kentat [e! {:keys [rivi tyyppi aineet voi-muokata?]}]
  [:div.ainevalinta-kentat
   (for [t (ainevalintalaatikot tyyppi aineet)]
     (let [polun-avaimet [(keyword "harja.domain.pot2" (name tyyppi)) (::pot2-domain/koodi t)]
           {:keys [valittu?] :as tiedot} (get-in rivi (cons :data polun-avaimet))]
       (if voi-muokata?
         ^{:key t}
         [:div {:class (str "aineiden-muokkaustila ainevalinta " (when valittu? "valittu"))}
          (when (or (not= polun-avaimet [:harja.domain.pot2/sideaineet :lisatty])
                    (mk-tiedot/lisatty-sideaine-mahdollinen? (:data rivi)))
            [aineen-otsikko-checkbox e! {:otsikko (::pot2-domain/nimi t)
                                         :arvo valittu? :label-luokka (when valittu? "bold")
                                         :polku (conj polun-avaimet :valittu?)}])

          (when valittu?
            [:div.kentat-haitari
             (case tyyppi
               (:runkoaineet :lisaaineet)
               (for [k (tyypin-kentat {:tyyppi tyyppi
                                       :tiedot tiedot
                                       :polun-avaimet polun-avaimet
                                       :voi-muokata? voi-muokata?})]
                 ^{:key (:otsikko k)}
                 [:div.inline-block {:style {:margin-right "6px"}}
                  [otsikko-ja-kentta e! k]])

               :sideaineet
               [sideaineet-komponentti e! rivi (::pot2-domain/koodi t)
                polun-avaimet aineet])])]
         (when valittu?
           ^{:key t}
           [:div.aineiden-lukutila
            (case tyyppi
              :runkoaineet
              [massan-aineen-lukutila {:tiedot tiedot :tyyppi tyyppi :aineen-otsikko (::pot2-domain/nimi t)}]

              :sideaineet
              [:div (str "Sideaine" tiedot)]

              :lisaaineet
              [:div (str "Lisäaine" tiedot)])]))))])


(defn massa-lomake [e! {:keys [pot2-massa-lomake materiaalikoodistot] :as app} {:keys [sivulle?]}]
  (let [{:keys [massatyypit runkoainetyypit sideainetyypit lisaainetyypit]} materiaalikoodistot
        voi-muokata? (:voi-muokata? pot2-massa-lomake)
        massa-id (::pot2-domain/massa-id pot2-massa-lomake)
        muut-validointivirheet (pot2-validoinnit/runko-side-ja-lisaaineen-validointivirheet pot2-massa-lomake materiaalikoodistot)
        materiaali-kaytossa (::pot2-domain/kaytossa pot2-massa-lomake)]
    [:div
     (when sivulle?
       [napit/sulje-ruksi #(e! (pot2-tiedot/->SuljeMateriaalilomake))])
     [ui-lomake/lomake
      {:muokkaa! #(e! (mk-tiedot/->PaivitaMassaLomake (ui-lomake/ilman-lomaketietoja %)))
       :luokka (when sivulle? "overlay-oikealla overlay-leveampi") :voi-muokata? voi-muokata?
       :otsikko-komp (fn [data]
                       [:div.lomake-otsikko-pieni (cond
                                                    (false? voi-muokata?)
                                                    "Massan tiedot"

                                                    massa-id
                                                    "Muokkaa massaa"

                                                    :else
                                                    "Uusi massa")])
       :footer-fn (fn [data]
                    [mm-yhteiset/tallennus-ja-puutelistaus e! {:data data
                                                               :validointivirheet muut-validointivirheet
                                                               :tallenna-fn #(e! (mk-tiedot/->TallennaLomake data))
                                                               :voi-tallentaa? (not (ui-lomake/voi-tallentaa? data))
                                                               :peruuta-fn #(e! (mk-tiedot/->TyhjennaLomake data))
                                                               :poista-fn #(e! (mk-tiedot/->TallennaLomake (merge data {::pot2-domain/poistettu? true})))
                                                               :tyyppi :massa
                                                               :id massa-id
                                                               :materiaali-kaytossa materiaali-kaytossa
                                                               :voi-muokata? voi-muokata?}])
       :vayla-tyyli? true}
      [{:otsikko "" :muokattava? (constantly false) :nimi ::pot2-domain/massan-nimi :tyyppi :string :palstoja 3
        :piilota-label? true :vayla-tyyli? true :kentan-arvon-luokka "fontti-20"
        :hae (fn [rivi]
               (if-not (::pot2-domain/tyyppi rivi)
                 "Nimi muodostuu automaattisesti lomakkeeseen täytettyjen tietojen perusteella"
                 (mm-yhteiset/materiaalin-rikastettu-nimi {:tyypit massatyypit
                                                           :materiaali rivi
                                                           :fmt :string})))}
       (when-not voi-muokata?
         (mm-yhteiset/muokkaa-nappi #(e! (mk-tiedot/->AloitaMuokkaus :pot2-massa-lomake))))
       (when-not voi-muokata? (ui-lomake/lomake-spacer {}))
       (ui-lomake/rivi
         {:otsikko "Massatyyppi"
          :nimi ::pot2-domain/tyyppi :tyyppi :valinta
          :valinta-nayta ::pot2-domain/nimi :valinta-arvo ::pot2-domain/koodi :valinnat massatyypit
          :pakollinen? true :vayla-tyyli? true}
         {:otsikko "Max raekoko" :nimi ::pot2-domain/max-raekoko
          :tyyppi :valinta
          :valinta-nayta (fn [rivi]
                           (str rivi))
          :vayla-tyyli? true
          :valinta-arvo identity
          :valinnat pot2-domain/massan-max-raekoko
          :pakollinen? true}
         {:otsikko "Nimen tarkenne" :nimi ::pot2-domain/nimen-tarkenne :tyyppi :string
          :vayla-tyyli? true})
       (ui-lomake/rivi
         {:otsikko "Kuulamyllyluokka"
          :nimi ::pot2-domain/kuulamyllyluokka
          :tyyppi :valinta :valinta-nayta (fn [rivi]
                                            (str (:nimi rivi)))
          :vayla-tyyli? true :valinta-arvo :nimi
          :valinnat paallystysilmoitus-domain/+kyylamyllyt-ja-nil+
          :pakollinen? true}
         {:otsikko "Litteyslukuluokka"
          :nimi ::pot2-domain/litteyslukuluokka :tyyppi :valinta
          :valinta-nayta (fn [rivi]
                           (str rivi))
          :vayla-tyyli? true
          :valinta-arvo identity
          :valinnat pot2-domain/litteyslukuluokat
          :pakollinen? true}
         {:otsikko "DoP" :nimi ::pot2-domain/dop-nro :tyyppi :string
          :validoi [[:ei-tyhja "Anna DoP nro"]]
          :vayla-tyyli? true :pakollinen? true})

       (ui-lomake/lomake-spacer {})

       {:nimi ::pot2-domain/runkoaineet :otsikko "Runkoaineen materiaali" :tyyppi :komponentti :palstoja 2
        :kaariva-luokka (str "mk-aine " (when-not voi-muokata? "lukutila"))
        :komponentti (fn [rivi]
                       [ainevalinta-kentat e! {:rivi rivi
                                               :tyyppi :runkoaineet
                                               :aineet runkoainetyypit
                                               :voi-muokata? voi-muokata?}])}
       {:nimi ::pot2-domain/sideaineet :otsikko "Sideaineet" :tyyppi :komponentti :palstoja 2
        :kaariva-luokka (str "mk-aine " (when-not voi-muokata? "lukutila"))
        :komponentti (fn [rivi] [ainevalinta-kentat e! {:rivi rivi
                                                        :tyyppi :sideaineet
                                                        :aineet sideainetyypit
                                                        :voi-muokata? voi-muokata?}])}
       {:nimi ::pot2-domain/lisaaineet :otsikko "Lisäaineet" :tyyppi :komponentti :palstoja 2
        :kaariva-luokka (str "mk-aine " (when-not voi-muokata? "lukutila"))
        :komponentti (fn [rivi] [ainevalinta-kentat e! {:rivi rivi
                                                        :tyyppi :lisaaineet
                                                        :aineet lisaainetyypit
                                                        :voi-muokata? voi-muokata?}])}
       (ui-lomake/lomake-spacer {})]

      pot2-massa-lomake]]))

(defn- massan-runkoaineet
  [rivi ainetyypit]
  [:span
   (for [{:runkoaine/keys [kuulamyllyarvo
                           litteysluku] :as aine}
         (reverse
           (sort-by :runkoaine/massaprosentti
                    (:harja.domain.pot2/runkoaineet rivi)))
         :let [aineen-otsikko (str (pot2-domain/ainetyypin-koodi->nimi ainetyypit (:runkoaine/tyyppi aine))
                                   (if (:runkoaine/esiintyma aine)
                                     (yleiset/str-suluissa-opt (:runkoaine/esiintyma aine))
                                     (when (= 7 (:runkoaine/tyyppi aine))
                                       (yleiset/str-suluissa-opt (:runkoaine/kuvaus aine)))))
               otsikko-rivitettyna (let [[aine tiedot] (str/split aineen-otsikko #"\(")
                                         tiedot (if (str/includes? tiedot ")")
                                                  (str "(" tiedot)
                                                  tiedot)]
                                     [:div [:div aine] [:div tiedot]])]]
     ^{:key (:runkoaine/id aine)}
     [:span
      [:div
       (when (or kuulamyllyarvo litteysluku)
         [yleiset/wrap-if true
          [yleiset/tooltip {} :%
           [yleiset/avain-arvo-tooltip otsikko-rivitettyna
            {:width "300px"}
            "KM-arvo" kuulamyllyarvo
            "Litteysluku" litteysluku]]
          [:span {:style {:color "#004D99"
                          :margin-right "8px"
                          :position "relative"
                          :top "1px"}}
           [ikonit/livicon-info-circle]]])
       [:span
        aineen-otsikko
        [:span.pull-right (str (:runkoaine/massaprosentti aine) "%")]]]])])

(defn- massan-sideaineet [rivi ainetyypit]
  [:span
   (for [aine (reverse
                (sort-by :sideaine/pitoisuus
                         (:harja.domain.pot2/sideaineet rivi)))]
     ^{:key (:sideaine/id aine)}
     [:span
      [:div
       (str (pot2-domain/ainetyypin-koodi->nimi ainetyypit (:sideaine/tyyppi aine)))
       [:span.pull-right (str (:sideaine/pitoisuus aine) "%")]]])])

(defn- massan-lisaaineet [rivi ainetyypit]
  [:span
   (for [aine (reverse
                (sort-by :lisaaine/pitoisuus
                         (:harja.domain.pot2/lisaaineet rivi)))]
     ^{:key (:lisaaine/id aine)}
     [:span
      [:div
       (str (pot2-domain/ainetyypin-koodi->nimi ainetyypit (:lisaaine/tyyppi aine)))
       [:span.pull-right (str (:lisaaine/pitoisuus aine) "%")]]])])

(defn massat-taulukko [e! {:keys [massat materiaalikoodistot] :as app}]
  [grid/grid
   {:otsikko "Massat"
    :tunniste ::pot2-domain/massa-id
    :tyhja (if (nil? massat)
             [ajax-loader "Haetaan massatyyppejä..."]
             "Urakalle ei ole vielä lisätty massoja")
    :rivi-klikattu #(e! (mk-tiedot/->MuokkaaMassaa % false))
    :voi-lisata? false :voi-kumota? false
    :voi-poistaa? (constantly false) :voi-muokata? true
    :custom-toiminto {:teksti "Luo uusi massa"
                      :toiminto #(e! (mk-tiedot/->UusiMassa))
                      :opts {:ikoni (ikonit/livicon-plus)
                             :luokka "napiton-nappi"}}}
   [{:otsikko "Nimi" :tyyppi :komponentti :leveys 8
     :komponentti (fn [rivi]
                    [mm-yhteiset/materiaalin-rikastettu-nimi {:tyypit (:massatyypit materiaalikoodistot)
                                                              :materiaali rivi
                                                              :fmt :komponentti}])}
    {:otsikko "Runkoaineet" :nimi ::pot2-domain/runkoaineet :fmt #(or % "-") :tyyppi :komponentti :leveys 6
     :komponentti (fn [rivi]
                    [massan-runkoaineet rivi (:runkoainetyypit materiaalikoodistot)])}
    {:otsikko "Sideaineet" :nimi ::pot2-domain/sideaineet :fmt  #(or % "-") :tyyppi :komponentti :leveys 5
     :komponentti (fn [rivi]
                    [massan-sideaineet rivi (:sideainetyypit materiaalikoodistot)])}
    {:otsikko "Lisäaineet" :nimi ::pot2-domain/lisaaineet :fmt  #(or % "-") :tyyppi :komponentti :leveys 4
     :komponentti (fn [rivi]
                    [massan-lisaaineet rivi (:lisaainetyypit materiaalikoodistot)])}
    {:otsikko "" :nimi :toiminnot :tyyppi :komponentti :leveys 3
     :komponentti (fn [rivi]
                    [mm-yhteiset/materiaalirivin-toiminnot e! rivi])}]
   massat])