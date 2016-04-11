(ns harja.views.urakka.toteumat.muut-tyot
  "Urakan 'Toteumat' välilehden 'Muutos -ja lisätyöt' osio"
  (:require [reagent.core :refer [atom] :as r]

            [harja.atom :refer [paivita!] :refer-macros [reaction<!]]
            [harja.domain.roolit :as roolit]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.modal :refer [modal] :as modal]
            [harja.ui.yleiset :as yleiset :refer [ajax-loader linkki raksiboksi
                                                  livi-pudotusvalikko +korostuksen-kesto+ kuvaus-ja-avainarvopareja]]
            [harja.ui.napit :as napit]
            [harja.ui.viesti :as viesti]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.suunnittelu.muut-tyot :as muut-tyot]
            [harja.tiedot.urakka.toteumat.muut-tyot-kartalla :as muut-tyot-kartalla]
            [harja.tiedot.urakka.urakan-toimenpiteet :as urakan-toimenpiteet]
            [harja.views.urakka.valinnat :as valinnat]

            [harja.ui.lomake :as lomake :refer [lomake]]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.pvm :as pvm]
            [harja.fmt :as fmt]
            [cljs.core.async :refer [<! >! chan timeout]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]]
            [harja.views.kartta :as kartta]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-xf]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]))

(defn hae-muutoshintainen-tyo-tpklla [tpk]
  (first (filter (fn [muutoshinta]
                   (= (:tehtava muutoshinta) tpk))
                 @u/muutoshintaiset-tyot)))

(defn tallenna-muu-tyo [muokattu]
  (go (let [urakka @nav/valittu-urakka
            urakka-id (:id urakka)
            urakan-alkupvm (:alkupvm urakka)
            urakan-loppupvm (:loppupvm urakka)
            [sop _] @u/valittu-sopimusnumero
            [hk-alkupvm hk-loppupvm] @u/valittu-hoitokausi
            toteuma (assoc-in
                      (assoc muokattu
                        :urakka-id urakka-id
                        :urakan-alkupvm urakan-alkupvm
                        :urakan-loppupvm urakan-loppupvm
                        :sopimus-id sop
                        :hoitokausi-aloituspvm hk-alkupvm
                        :hoitokausi-lopetuspvm hk-loppupvm
                        ;; jos käyttäjä syöttää yksikköhinnan, eikä sitä vielä ollut suunnittelupuolelle syötetty,
                        ;; tallennetaan tässä yhteydessä hinta implisiittisesti myös suunnittelupuolelle (muutoshintainen_tyo-tauluun)
                        :uusi-muutoshintainen-tyo (if (and (not (:yksikkohinta-suunniteltu? muokattu))
                                                           (:yksikkohinta muokattu)
                                                           (get-in muokattu [:tehtava :toimenpidekoodi]))
                                                    (get-in muokattu [:tehtava :toimenpidekoodi])
                                                    nil)
                        :suorittajan-nimi (get-in muokattu [:suorittajan :nimi])
                        :suorittajan-ytunnus (get-in muokattu [:suorittajan :ytunnus]))
                      [:tehtava :paivanhinta]
                      (if (= :paivanhinta (:hinnoittelu muokattu))
                        (get-in muokattu [:tehtava :paivanhinta])
                        nil))
            vanhat-idt (into #{} (map #(get-in % [:toteuma :id]) @u/muut-tyot-hoitokaudella))
            res (<! (muut-tyot/tallenna-muiden-toiden-toteuma toteuma))
            uuden-id (get-in (first (filter #(not (vanhat-idt (get-in % [:toteuma :id])))
                                            res)) [:toteuma :id])]
        (reset! u/muut-tyot-hoitokaudella res)
        (paivita! u/muutoshintaiset-tyot)
        (or uuden-id true))))

(def +valitse-tyyppi+
  "- Valitse tyyppi -")

(defn muun-tyon-tyypin-teksti [avainsana]
  "Muun työn tyypin teksti avainsanaa vastaan"
  (case avainsana
    :muutostyo "Muutostyö"
    :lisatyo "Lisätyö"
    :akillinen-hoitotyo "Äkillinen hoitotyö"
    :vahinkojen-korjaukset "Vahinkojen korjaukset"
    +valitse-tyyppi+))

(defn muun-tyon-tyypin-teksti-genetiivissa [avainsana]
  "Muun työn tyypin teksti avainsanaa vastaan"
  (case avainsana
    :muutostyo "muutostyön"
    :lisatyo "lisätyön"
    :akillinen-hoitotyo "äkillisen hoitotyön"
    :vahinkojen-korjaukset "vahinkojen korjaukset"
    "työn jonka tyyppi on tuntematon"))

(def +muun-tyon-tyypit+
  [:muutostyo :lisatyo :akillinen-hoitotyo :vahinkojen-korjaukset])


(def korostettavan-rivin-id (atom nil))

(defn korosta-rivia
  ([id] (korosta-rivia id +korostuksen-kesto+))
  ([id kesto]
   (reset! korostettavan-rivin-id id)
   (go (<! (timeout kesto))
       (reset! korostettavan-rivin-id nil))))

(def +rivin-luokka+ "korosta")

(defn aseta-rivin-luokka [korostettavan-rivin-toteuman-id]
  (fn [rivi]
    (if (= korostettavan-rivin-toteuman-id (get-in rivi [:toteuma :id]))
      +rivin-luokka+
      "")))

(defn toteutuneen-muun-tyon-muokkaus
  "Muutos-, lisä- ja äkillisen hoitotyön toteuman muokkaaminen ja lisääminen"
  []
  (komp/luo
    (let [muokattu (reaction (if (get-in @muut-tyot/valittu-toteuma [:toteuma :id])
                           (assoc @muut-tyot/valittu-toteuma
                             :sopimus @u/valittu-sopimusnumero
                             :toimenpideinstanssi @u/valittu-toimenpideinstanssi)
                           ;; alustetaan arvoja uudelle toteumalle
                           (assoc @muut-tyot/valittu-toteuma
                             :sopimus @u/valittu-sopimusnumero
                             :toimenpideinstanssi @u/valittu-toimenpideinstanssi
                             :tyyppi :muutostyo
                             :hinnoittelu :yksikkohinta)))]
      (fn []
        (let [valmis-tallennettavaksi? (reaction (let [m @muokattu]
                                                   (and
                                                     (get-in m [:tehtava :toimenpidekoodi])
                                                     (:tyyppi m)
                                                     (:alkanut m)
                                                     (:paattynyt m)
                                                     (or (if (= (:hinnoittelu @muokattu) :paivanhinta)
                                                           (get-in m [:tehtava :paivanhinta])
                                                           (get-in m [:tehtava :maara]))))))
              tallennus-kaynnissa (atom false)
              tehtavat-tasoineen @u/urakan-muutoshintaiset-toimenpiteet-ja-tehtavat
              tehtavat (map #(nth % 3) tehtavat-tasoineen)
              toimenpideinstanssit @u/urakan-toimenpideinstanssit
              jarjestelman-lisaama-toteuma? (:jarjestelmasta @muokattu)
              lomaketta-voi-muokata? (and
                                       (roolit/voi-kirjata-toteumia? (:id @nav/valittu-urakka))
                                       (not jarjestelman-lisaama-toteuma?))
              aseta-tehtava (fn [rivi arvo]
                               (let [jo-suunniteltu-yksikko
                                     (:yksikko (urakan-toimenpiteet/tehtava-idlla arvo tehtavat))
                                     jo-suunniteltu-yksikkohinta
                                     (:yksikkohinta (hae-muutoshintainen-tyo-tpklla arvo))]
                                 (assoc
                                  (assoc-in rivi
                                            [:tehtava :toimenpidekoodi] arvo)
                                  :yksikko jo-suunniteltu-yksikko
                                  :yksikkohinta jo-suunniteltu-yksikkohinta
                                  :yksikkohinta-suunniteltu? jo-suunniteltu-yksikkohinta)))
              aseta-toimenpide (fn [rivi arvo]
                                 (-> rivi
                                     (assoc :toimenpideinstanssi arvo)
                                     (aseta-tehtava nil)))]
          [:div.muun-tyon-tiedot
           [napit/takaisin " Takaisin muiden töiden luetteloon" #(reset! muut-tyot/valittu-toteuma nil)]
           [lomake {:otsikko      (if (get-in @muut-tyot/valittu-toteuma [:tehtava :id])
                                    (if lomaketta-voi-muokata?
                                      "Muokkaa toteumaa"
                                      "Tarkastele toteumaa")
                                    "Luo uusi toteuma")
                    :voi-muokata? lomaketta-voi-muokata?
                    :muokkaa!     (fn [uusi]
                                    (reset! muokattu uusi))
                    :footer       (when (roolit/voi-kirjata-toteumia? (:id @nav/valittu-urakka))
                                    [:span
                                     [napit/palvelinkutsu-nappi
                                      " Tallenna toteuma"
                                      #(tallenna-muu-tyo @muokattu)
                                      {:luokka       "nappi-ensisijainen"
                                       :disabled     (or (not lomaketta-voi-muokata?)
                                                         (not @valmis-tallennettavaksi?))
                                       :kun-onnistuu #(let [muokatun-id (or (get-in @muokattu [:toteuma :id]) %)]
                                                       (do
                                                         (korosta-rivia muokatun-id)
                                                         (reset! tallennus-kaynnissa false)
                                                         (reset! muut-tyot/valittu-toteuma nil)))

                                       :kun-virhe    (reset! tallennus-kaynnissa false)}]
                                     (when (and (not jarjestelman-lisaama-toteuma?)
                                                (get-in @muokattu [:toteuma :id]))

                                       (let [m @muokattu]
                                         [:button.nappi-kielteinen
                                          {:class (when @tallennus-kaynnissa "disabled")
                                           :on-click
                                                  (fn []
                                                    (modal/nayta! {:otsikko "Toteuman poistaminen"
                                                                   :footer  [:span
                                                                             [:button.nappi-toissijainen {:type     "button"
                                                                                                          :on-click #(do (.preventDefault %)
                                                                                                                         (modal/piilota!))}
                                                                              "Peruuta"]
                                                                             [:button.nappi-kielteinen {:type     "button"
                                                                                                        :on-click #(do (.preventDefault %)
                                                                                                                       (modal/piilota!)
                                                                                                                       (reset! tallennus-kaynnissa true)
                                                                                                                       (go (let [res (tallenna-muu-tyo
                                                                                                                                       (assoc m :poistettu true))]
                                                                                                                             (if res
                                                                                                                               ;; Tallennus ok
                                                                                                                               (do (viesti/nayta! "Toteuma poistettu")
                                                                                                                                   (reset! tallennus-kaynnissa false)
                                                                                                                                   (reset! muut-tyot/valittu-toteuma nil))


                                                                                                                               ;; Epäonnistui jostain syystä
                                                                                                                               (reset! tallennus-kaynnissa false)))))}
                                                                              "Poista toteuma"]]}
                                                                  [kuvaus-ja-avainarvopareja
                                                                   (str "Haluatko varmasti poistaa " (muun-tyon-tyypin-teksti-genetiivissa (:tyyppi m)) "?")
                                                                   "Pvm:" (pvm/pvm (:alkanut m))
                                                                   "Tehtävä:" (get-in m [:tehtava :nimi])
                                                                   "Kustannus:" (fmt/euro-opt
                                                                                  (if (= (:hinnoittelu m) :yksikkohinta)
                                                                                    (* (get-in m [:tehtava :maara]) (:yksikkohinta m))
                                                                                    (get-in m [:tehtava :paivanhinta]))
                                                                                  )]))}
                                          (ikonit/trash) " Poista toteuma"]))])}

            [(when jarjestelman-lisaama-toteuma?
               {:otsikko     "Lähde" :nimi :luoja :tyyppi :string
                :vihje       "Tietojärjestelmästä tulleen toteuman muokkaus ei ole sallittu."
                :hae         (fn [rivi] (str "Järjestelmä (" (:kayttajanimi rivi) " / " (:organisaatio rivi) ")"))
                :muokattava? (constantly false)})
             {:otsikko       "Sopimusnumero" :nimi :sopimus
              :pakollinen?   true
              :tyyppi        :valinta
              :valinta-nayta second
              :valinnat      (:sopimukset @nav/valittu-urakka)
              :fmt           second
              :palstoja 1}
             
             {:otsikko       "Tyyppi" :nimi :tyyppi
              :tyyppi        :valinta
              :pakollinen?   true
              :valinta-nayta #(if (nil? %) +valitse-tyyppi+ (muun-tyon-tyypin-teksti %))
              :valinnat      +muun-tyon-tyypit+
              :validoi       [[:ei-tyhja "Anna kustannustyyppi"]]
              :palstoja 1}
             {:otsikko       "Toimenpide" :nimi :toimenpideinstanssi
              :tyyppi        :valinta
              :pakollinen?   true
              :valinta-nayta #(:tpi_nimi %)
              :valinnat      toimenpideinstanssit
              :fmt           #(:tpi_nimi %)
              :aseta aseta-toimenpide
              :palstoja 1}
             {:otsikko       "Tehtävä" :nimi :tehtava
              :pakollinen?   true
              :hae           #(get-in % [:tehtava :toimenpidekoodi])
              :valinta-arvo  #(:id (nth % 3))
              :valinta-nayta #(if % (:nimi (nth % 3)) "- Valitse tehtävä -")
              :tyyppi        :valinta
              :valinnat-fn   #(urakan-toimenpiteet/toimenpideinstanssin-tehtavat
                               (get-in @muokattu [:toimenpideinstanssi :tpi_id])
                               toimenpideinstanssit tehtavat-tasoineen)
              :validoi       [[:ei-tyhja "Valitse tehtävä"]]
              :aseta         aseta-tehtava
              :palstoja 1}

             {:otsikko "Aloitus" :pakollinen? true :nimi :alkanut :tyyppi :pvm
              :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))
              :uusi-rivi? true
              :aseta   (fn [rivi arvo]
                         (assoc
                          (if
                              (or
                               (not (:paattynyt rivi))
                               (pvm/jalkeen? arvo (:paattynyt rivi)))
                            (assoc rivi :paattynyt arvo)
                            rivi)
                          :alkanut
                          arvo))
              :validoi [[:ei-tyhja "Valitse päivämäärä"]]
              :huomauta [[:urakan-aikana-ja-hoitokaudella]]}
             {:otsikko     "Lopetus" :pakollinen? true :nimi :paattynyt :tyyppi :pvm
              :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))
              :palstoja 1
              :validoi     [[:ei-tyhja "Valitse päivämäärä"]
                            [:pvm-kentan-jalkeen :alkanut "Lopetuksen pitää olla aloituksen jälkeen"]]}

             (lomake/rivi
              {:otsikko       "Hinnoittelu" :nimi :hinnoittelu
               :pakollinen?   true
               :tyyppi        :valinta
               :valinta-arvo  first
               :valinta-nayta second
               :valinnat      [[:yksikkohinta "Muutoshinta"] [:paivanhinta "Päivän hinta"]]
               :palstoja 1}


              {:otsikko     "Määrä" :nimi :maara :tyyppi :positiivinen-numero
               :pakollinen? (= :yksikkohinta (:hinnoittelu @muokattu))
               :hae         #(get-in % [:tehtava :maara])
           
               :aseta       (fn [rivi arvo] (assoc-in rivi [:tehtava :maara] arvo))
               :validoi     (when (= (:hinnoittelu @muokattu) :yksikkohinta)
                              [[:ei-tyhja "Määrä antamatta."]])
               :yksikko     (if (:yksikko @muokattu) (:yksikko @muokattu) nil) :palstoja 1}
              (when (= (:hinnoittelu @muokattu) :paivanhinta)
                {:otsikko     "Päivän hinta"
                 :nimi        :paivanhinta
                 :pakollinen? (= :paivanhinta (:hinnoittelu @muokattu))
                 :hae         #(get-in % [:tehtava :paivanhinta])
                 :yksikko     "€"
                 :aseta       (fn [rivi arvo] (assoc-in rivi [:tehtava :paivanhinta] arvo))
                 :tyyppi      :positiivinen-numero
                 :validoi     [[:ei-tyhja "Anna rahamäärä"]]
                 :palstoja    1})
              (when (= (:hinnoittelu @muokattu) :yksikkohinta)
                {:otsikko     "Sopimushinta" :nimi :yksikkohinta
                 :tyyppi      :positiivinen-numero :validoi [[:ei-tyhja "Anna rahamäärä"]]
                 :muokattava? #(not (:yksikkohinta-suunniteltu? %))
                 :yksikko     (str "€ / " (:yksikko @muokattu))
                 :palstoja    1})
              {:otsikko     "Kustannus" :nimi :kustannus
               :muokattava? (constantly false)
               :tyyppi      :numero
               :hae         #(if (= (:hinnoittelu %) :yksikkohinta)
                               (* (get-in % [:tehtava :maara]) (:yksikkohinta %))
                               (get-in % [:tehtava :paivanhinta]))
               :fmt         fmt/euro-opt
               :palstoja    1})

             {:tyyppi :komponentti :nimi :rahaohje
              :palstoja 2
              :komponentti [yleiset/vihje
                            (str (if (= :paivanhinta (:hinnoittelu @muokattu))
                                    "Voit syöttää tehdyn työn määrän mutta se ei vaikuta kokonaishintaan. "
                                    "Kokonaiskustannus on muutoshinta kerrottuna tehdyn työn määrällä. ")
                                 (if (:yksikkohinta-suunniteltu? @muokattu)
                                   "Ylläoleva sopimushinta on muutos- ja lisätöiden hintaluettelosta. Hinnasto löytyy Suunnittelun Muutos- ja lisätyöt -osiosta."
                                   "Syötä tähän työn sopimushinta muutos- ja lisätöiden hintaluettelosta. Hinta tallennetaan seuraavaa käyttökertaa
                                  varten Suunnittelun Muutos- ja lisätyöt -osioon."))]}
             
             {:otsikko "Suorittaja" :nimi :suorittajan-nimi
              :hae     #(if (get-in @muokattu [:suorittajan :nimi])
                          (get-in @muokattu [:suorittajan :nimi])
                          (:nimi @u/urakan-organisaatio))
              :aseta   (fn [rivi arvo] (assoc-in rivi [:suorittajan :nimi] arvo))
              :tyyppi  :string :muokattava? (constantly (not jarjestelman-lisaama-toteuma?)) :pituus-max 256}
             {:otsikko "Suorittajan Y-tunnus" :nimi :suorittajan-ytunnus :pituus-max 256
              :hae     #(if (get-in @muokattu [:suorittajan :ytunnus])
                          (get-in @muokattu [:suorittajan :ytunnus])
                          (:ytunnus @u/urakan-organisaatio))
              :aseta   (fn [rivi arvo] (assoc-in rivi [:suorittajan :ytunnus] arvo))
              :tyyppi  :string :muokattava? (constantly (not jarjestelman-lisaama-toteuma?))}
             {:otsikko     "Lisätieto" :nimi :lisatieto :tyyppi :text :pituus-max 256
              :placeholder "Kirjoita tähän lisätietoa" :koko [80 :auto]}

             ]

            @muokattu]
           (when-not (roolit/voi-kirjata-toteumia?  (:id @nav/valittu-urakka))
             "Käyttäjäroolillasi ei ole oikeutta muokata tätä toteumaa.")])))))

(defn muut-tyot-toteumalistaus
  "Muiden töiden toteumat"
  []
  (let [urakka @nav/valittu-urakka

        valitut-muut-tyot (reaction (let [toimenpideinstanssi @u/valittu-toimenpideinstanssi
                                          muut-tyot-hoitokaudella @u/muut-tyot-hoitokaudella]
                                      (reverse (sort-by :alkanut (filter #(= (get-in % [:tehtava :emo])
                                                                             (:id toimenpideinstanssi))
                                                                         muut-tyot-hoitokaudella)))))
        tyorivit
        (reaction
          (let [muutoshintaiset-tyot @u/muutoshintaiset-tyot
                valitut-muut-tyot @valitut-muut-tyot]
            (map (fn [muu-tyo]
                   (let [muutoshintainen-tyo
                         (first (filter (fn [muutoshinta]
                                          (= (:tehtava muutoshinta)
                                             (get-in muu-tyo [:tehtava :toimenpidekoodi]))) muutoshintaiset-tyot))
                         yksikkohinta (:yksikkohinta muutoshintainen-tyo)]
                     (assoc muu-tyo
                       :hinnoittelu (if (get-in muu-tyo [:tehtava :paivanhinta]) :paivanhinta :yksikkohinta)
                       :yksikko (:yksikko muutoshintainen-tyo)
                       :yksikkohinta yksikkohinta
                       :yksikkohinta-suunniteltu? yksikkohinta)))
                 valitut-muut-tyot)))]

    (komp/luo
      (fn []
        (reset! muut-tyot/haetut-muut-tyot @tyorivit)
        (let [aseta-rivin-luokka (aseta-rivin-luokka @korostettavan-rivin-id)]
          [:div.muut-tyot-toteumat
           [valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide urakka]
           [:button.nappi-ensisijainen {:on-click #(reset! muut-tyot/valittu-toteuma {})
                                        :disabled (not (roolit/voi-kirjata-toteumia? (:id @nav/valittu-urakka)))}
            (ikonit/plus) " Lisää toteuma"]

           [grid/grid
            {:otsikko       (str "Toteutuneet muutos-, lisä- ja äkilliset hoitotyöt sekä vahinkojen korjaukset")
             :tyhja         (if (nil? @valitut-muut-tyot)
                              [ajax-loader "Toteumia haetaan..."]
                              "Ei toteumia saatavilla.")
             :rivi-klikattu #(reset! muut-tyot/valittu-toteuma %)
             :rivin-luokka  #(aseta-rivin-luokka %)
             :tunniste      #(get-in % [:toteuma :id])}
            [{:otsikko "Pvm" :tyyppi :pvm :fmt pvm/pvm :nimi :alkanut :leveys "10%"}
             {:otsikko "Tyyppi" :nimi :tyyppi :fmt muun-tyon-tyypin-teksti :leveys "15%"}
             {:otsikko "Tehtävä" :tyyppi :string :nimi :tehtavan_nimi
              :hae     #(get-in % [:tehtava :nimi]) :leveys "25%"}
             {:otsikko "Määrä" :pakollinen? true :tyyppi :string :nimi :maara
              :hae     #(if (get-in % [:tehtava :maara]) (get-in % [:tehtava :maara]) "-")
              :leveys  "10%"}
             {:otsikko "Yksikkö"
              :nimi    :yksikko :tyyppi :string :muokattava? (constantly false) :leveys "10%"}
             {:otsikko     "Muutos\u00ADhinta" :nimi :yksikkohinta :tasaa :oikea :tyyppi :positiivinen-numero
              :hae         #(if (get-in % [:tehtava :paivanhinta])
                             nil
                             (:yksikkohinta %))
              :muokattava? (constantly false) :fmt fmt/euro-opt :leveys "10%"}
             {:otsikko "Hin\u00ADnoit\u00ADtelu" :tyyppi :string :nimi :hinnoittelu
              :hae     #(if (get-in % [:tehtava :paivanhinta]) "Päivän hinta" "Muutos\u00ADhinta") :leveys "10%"}
             {:otsikko "Kus\u00ADtannus (€)" :tyyppi :string :nimi :kustannus :tasaa :oikea
              ;; kustannus on päivän hinta jos se on annettu, muutoin yksikköhinta * määrä
              :hae     #(if (get-in % [:tehtava :paivanhinta])
                         (get-in % [:tehtava :paivanhinta])
                         (if (and (get-in % [:tehtava :maara]) (:yksikkohinta %))
                           (* (get-in % [:tehtava :maara]) (:yksikkohinta %))
                           "Ei voi laskea"))
              :fmt     #(if (number? %) (fmt/euro-opt %) (str %))
              :leveys  "10%"}
             {:otsikko "Lähde"
              :nimi    :lahde
              :hae     #(if (:jarjestelmasta %)
                         "Urak. järj."
                         "Harja")
              :tyyppi  :string :muokattava? (constantly false) :leveys "10%"}]
            @tyorivit]])))))



(defn muut-tyot-toteumat []
  (komp/luo
    (komp/lippu muut-tyot-kartalla/karttataso-muut-tyot)
    (komp/kuuntelija :toteuma-klikattu #(reset! muut-tyot/valittu-toteuma %2))
    (komp/ulos (kartta/kuuntele-valittua! muut-tyot/valittu-toteuma)) ;;Palauttaa funktion jolla kuuntelu lopetetaan
    (fn []
      [:span
       [kartta/kartan-paikka]
       (if @muut-tyot/valittu-toteuma
         [toteutuneen-muun-tyon-muokkaus]
         [muut-tyot-toteumalistaus])])))
