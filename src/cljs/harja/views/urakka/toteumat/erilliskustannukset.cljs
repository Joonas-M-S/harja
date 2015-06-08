(ns harja.views.urakka.toteumat.erilliskustannukset
  "Urakan 'Toteumat' välilehden Erilliskustannuksien osio"
  (:require [reagent.core :refer [atom] :as r]
            [bootstrap :as bs]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.modal :refer [modal] :as modal]
            [harja.ui.yleiset :refer [ajax-loader kuuntelija linkki sisalla? raksiboksi
                                      livi-pudotusvalikko +korostuksen-kesto+]]
            [harja.ui.napit :as napit]
            [harja.ui.viesti :as viesti]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.indeksit :as i]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.urakka.suunnittelu :as s]
            [harja.tiedot.urakka.toteumat :as toteumat]
            [harja.tiedot.istunto :as istunto]
            [harja.views.urakka.valinnat :as valinnat]

            [harja.ui.visualisointi :as vis]
            [harja.ui.lomake :refer [lomake]]
            [harja.loki :refer [log logt]]
            [harja.pvm :as pvm]
            [harja.fmt :as fmt]
            [cljs.core.async :refer [<! >! chan]]
            [clojure.string :as str]
            [cljs-time.core :as t]
            [cljs.core.async :refer [<! timeout]]
            [harja.ui.protokollat :refer [Haku hae]]
            [harja.domain.skeema :refer [+tyotyypit+]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction run!]]
                   [harja.atom :refer [reaction<!]]))

(defonce valittu-kustannus (atom nil))

(def +valitse-indeksi+
     "- Valitse indeksi -")

(def +ei-sidota-indeksiin+
     "Ei sidota indeksiin")

(defn tallenna-erilliskustannus [muokattu]
  (go (let [sopimus-id (first (:sopimus muokattu))
            tpi-id (:tpi_id (:toimenpideinstanssi muokattu))
            tyyppi (name (:tyyppi muokattu))
            indeksi (if (= +ei-sidota-indeksiin+ (:indeksin_nimi muokattu))
                      nil
                      (:indeksin_nimi muokattu))
            rahasumma (if (= (:maksaja muokattu) :urakoitsija)
                        (- (:rahasumma muokattu))
                        (:rahasumma muokattu))
            vanhat-idt (into #{} (map #(:id %) @u/erilliskustannukset-hoitokaudella))
            res (<! (toteumat/tallenna-erilliskustannus (assoc muokattu
                                                          :urakka-id (:id @nav/valittu-urakka)
                                                          :alkupvm (first @u/valittu-hoitokausi)
                                                          :loppupvm (second @u/valittu-hoitokausi)
                                                          :sopimus sopimus-id
                                                          :toimenpideinstanssi tpi-id
                                                          :tyyppi tyyppi
                                                          :rahasumma rahasumma
                                                          :indeksin_nimi indeksi)))
            uuden-id (:id (first (filter #(not (vanhat-idt (:id %)))
                                   res)))]
        (reset! u/erilliskustannukset-hoitokaudella res)
        (or uuden-id true))))

(def +valitse-tyyppi+
  "- Valitse tyyppi -")

(defn erilliskustannustyypin-teksti [avainsana]
  "Erilliskustannustyypin teksti avainsanaa vastaan"
  (case avainsana
    :vahinkojen_korjaukset "Vahinkojen korjaukset"
    :asiakastyytyvaisyysbonus "Asiakastyytyväisyysbonus"
    :muu "Muu"
    :tilaajan_maa-aines "Tilaajan maa-aines"
    +valitse-tyyppi+))

;; "tilaajan_maa-aines" -tyyppisiä erilliskustannuksia otetaan vastaan Aura-konversiossa
;; mutta ei anneta enää syöttää Harjaan käyttöliittymän kautta. -Anne L. palaverissa 2015-06-02"
(def +erilliskustannustyypit+
  [:vahinkojen_korjaukset :asiakastyytyvaisyysbonus :muu])

(defn maksajavalinnan-teksti [avain]
  (case avain
    :tilaaja "Tilaaja"
    :urakoitsija "Urakoitsija"
    "Maksajaa ei asetettu"))

(def +maksajavalinnat+
  [:tilaaja :urakoitsija])


(def korostettavan-rivin-id (atom nil))

;; FIXME: siirrä rivin korostuksen funktio gridiin josta sitä voi käyttää
(defn korosta-rivia
  ([id] (korosta-rivia id +korostuksen-kesto+))
  ([id kesto]
   (reset! korostettavan-rivin-id id)
   (go (<! (timeout kesto))
     (reset! korostettavan-rivin-id nil))))

(def rivin-luokka (atom "korosta"))

(defn aseta-rivin-luokka [rivi]
  (if (= @korostettavan-rivin-id (:id rivi))
    "korosta"
    ""))

(defn erilliskustannusten-toteuman-muokkaus
  "Erilliskustannuksen muokkaaminen ja lisääminen"
  []
  (let [muokattu (atom (if (:id @valittu-kustannus)
                         (assoc @valittu-kustannus
                           :sopimus @u/valittu-sopimusnumero
                           :toimenpideinstanssi @u/valittu-toimenpideinstanssi
                           ;; jos maksaja on urakoitsija, rahasumma kannassa miinusmerkkisenä
                           :maksaja (if (neg? (:rahasumma @valittu-kustannus))
                                      :urakoitsija
                                      :tilaaja)
                           :rahasumma (Math/abs (:rahasumma @valittu-kustannus)))
                         (assoc @valittu-kustannus
                           :sopimus @u/valittu-sopimusnumero
                           :toimenpideinstanssi @u/valittu-toimenpideinstanssi
                           :maksaja :tilaaja
                           :indeksin_nimi +ei-sidota-indeksiin+)))
        valmis-tallennettavaksi? (reaction (let [m @muokattu]
                                             (not (and
                                                    (:toimenpideinstanssi m)
                                                    (:tyyppi m)
                                                    (:pvm m)
                                                    (:rahasumma m)))))
        tallennus-kaynnissa (atom false)]

    (komp/luo
      (fn [ur]
        [:div.erilliskustannuksen-tiedot
         [:button.nappi-toissijainen {:on-click #(reset! valittu-kustannus nil)}
          (ikonit/chevron-left) " Takaisin kustannusluetteloon"]
         (if (:id @valittu-kustannus)
           [:h3 "Muokkaa kustannusta"]
           [:h3 "Luo uusi kustannus"])

         [lomake {:luokka   :horizontal
                  :muokkaa! (fn [uusi]
                              (log "MUOKATAAN " (pr-str uusi))
                              (reset! muokattu uusi))
                  :footer   [:span
                             [napit/palvelinkutsu-nappi
                              " Tallenna kustannus"
                              #(tallenna-erilliskustannus @muokattu)
                              {:luokka "nappi-ensisijainen"
                               :disabled @valmis-tallennettavaksi?
                               :kun-onnistuu #(let [muokatun-id (or (:id @muokattu) %)]
                                               (do
                                                 (korosta-rivia muokatun-id)
                                                 (reset! tallennus-kaynnissa false)
                                                 (reset! valittu-kustannus nil)))
                               :kun-virhe  (reset! tallennus-kaynnissa false)}]
                             (when (:id @muokattu)
                               [:button.nappi-kielteinen
                                {:class (when @tallennus-kaynnissa "disabled")
                                 :on-click
                                        (fn []
                                          (modal/nayta! {:otsikko "Erilliskustannuksen poistaminen"
                                                         :footer  [:span
                                                                   [:button.nappi-toissijainen {:type     "button"
                                                                                                :on-click #(do (.preventDefault %)
                                                                                                               (modal/piilota!))}
                                                                    "Peruuta"]
                                                                   [:button.nappi-kielteinen {:type     "button"
                                                                                              :on-click #(do (.preventDefault %)
                                                                                                             (modal/piilota!)
                                                                                                             (reset! tallennus-kaynnissa true)
                                                                                                             (go (let [res (tallenna-erilliskustannus
                                                                                                                             (assoc @muokattu :poistettu true))]
                                                                                                                   (if res
                                                                                                                     ;; Tallennus ok
                                                                                                                     (do (viesti/nayta! "Kustannus poistettu")
                                                                                                                         (reset! tallennus-kaynnissa false)
                                                                                                                         (reset! valittu-kustannus nil))

                                                                                                                     ;; Epäonnistui jostain syystä
                                                                                                                     (reset! tallennus-kaynnissa false)))))}
                                                                    "Poista kustannus"]]}
                                            [:div (str "Haluatko varmasti poistaa erilliskustannuksen "
                                                    (Math/abs (:rahasumma @muokattu)) "€ päivämäärällä "
                                                    (pvm/pvm (:pvm @muokattu)) "?")]))}
                                (ikonit/trash) " Poista kustannus"])]}

          [{:otsikko       "Sopimusnumero" :nimi :sopimus
            :tyyppi        :valinta :valinta-arvo identity
            :valinta-nayta second
            :valinnat      (:sopimukset @nav/valittu-urakka)
            :fmt           second
            :leveys-col    3}
           {:otsikko       "Toimenpide" :nimi :toimenpideinstanssi
            :tyyppi        :valinta :valinta-arvo identity
            :valinta-nayta #(:tpi_nimi %)
            :valinnat      @u/urakan-toimenpideinstanssit
            :fmt           #(:tpi_nimi %)
            :leveys-col    3}
           {:otsikko "Tyyppi" :nimi :tyyppi
            :tyyppi        :valinta :valinta-arvo identity
            :valinta-nayta #(if (nil? %) +valitse-tyyppi+ (erilliskustannustyypin-teksti %))
            :valinnat      +erilliskustannustyypit+
            :fmt           #(erilliskustannustyypin-teksti %)
            :validoi       [[:ei-tyhja "Anna kustannustyyppi"]]
            :leveys-col    3}
           {:otsikko "Toteutunut pvm" :nimi :pvm :tyyppi :pvm  :validoi [[:ei-tyhja "Anna tarkastuksen päivämäärä"]] :leveys-col 3}
           {:otsikko "Rahamäärä" :nimi :rahasumma :tyyppi :numero :validoi [[:ei-tyhja "Anna rahamäärä"]] :leveys-col 3}
           {:otsikko       "Indeksi" :nimi :indeksin_nimi :tyyppi :valinta :valinta-arvo identity
            :valinta-nayta str
            :valinnat      (conj @i/indeksien-nimet +ei-sidota-indeksiin+)
            :fmt           #(if (nil? %) +valitse-indeksi+ str)
            :leveys-col    3
            }
           {:otsikko       "Maksaja" :nimi :maksaja :tyyppi :valinta :valinta-arvo identity
            :valinta-nayta #(maksajavalinnan-teksti %)
            :valinnat      +maksajavalinnat+
            :fmt           #(maksajavalinnan-teksti %)
            :leveys-col    3
            }
           {:otsikko "Lisätieto" :nimi :lisatieto :tyyppi :text
            :placeholder "Kirjoita tähän lisätietoa" :koko [80 :auto]}
           ]

          @muokattu]]))))

(defn erilliskustannusten-toteumalistaus
  "Erilliskustannusten toteumat"
  []
  (let [urakka @nav/valittu-urakka
        valitut-kustannukset
        (reaction (let [[sopimus-id _] @u/valittu-sopimusnumero
                        toimenpideinstanssi (:tpi_id @u/valittu-toimenpideinstanssi)]
                    (reverse (sort-by :pvm (filter #(and
                                             (= sopimus-id (:sopimus %))
                                             (= (:toimenpideinstanssi %) toimenpideinstanssi))
                                     @u/erilliskustannukset-hoitokaudella)))))]

    (komp/luo
      (fn []
        [:div.erilliskustannusten-toteumat
         [:div  "Tämä toiminto on keskeneräinen. Älä raportoi bugeja."]
         [valinnat/urakan-sopimus-ja-hoitokausi-ja-toimenpide urakka]
         [:button.nappi-ensisijainen {:on-click #(reset! valittu-kustannus {})}
          (ikonit/plus-sign) " Lisää kustannus"]

         [grid/grid
          {:otsikko       (str "Erilliskustannukset ")
           :tyhja         (if (nil? @valitut-kustannukset)
                            [ajax-loader "Erilliskustannuksia haetaan..."]
                            "Ei erilliskustannuksia saatavilla.")
           :rivi-klikattu #(reset! valittu-kustannus %)
           ;; kutsu gridin sisällä olevaa funktiota grid/korosta (jota ei vielä ole fixme)
           :rivin-luokka  #(aseta-rivin-luokka %)}
          [{:otsikko "Tyyppi" :nimi :tyyppi :fmt erilliskustannustyypin-teksti :leveys "20%"}
           {:otsikko "Pvm" :tyyppi :pvm :fmt pvm/pvm :nimi :pvm :leveys "10%"}
           {:otsikko "Rahamäärä (€)" :tyyppi :string :nimi :rahasumma :hae #(Math/abs (:rahasumma %)) :fmt fmt/euro-opt :leveys "10%"}
           {:otsikko "Maksaja" :tyyppi :string :nimi :maksaja
            :hae #(if (neg? (:rahasumma %)) "Urakoitsija" "Tilaaja") :leveys "10%"}
           {:otsikko "Lisätieto"  :nimi :lisatieto :leveys "45%"}
           {:otsikko "Indeksi" :nimi :indeksin_nimi :leveys "10%"}
           ]
          @valitut-kustannukset
          ]]))))



(defn erilliskustannusten-toteumat []
  (if @valittu-kustannus
    [erilliskustannusten-toteuman-muokkaus]
    [erilliskustannusten-toteumalistaus]))