(ns harja.views.urakka.yllapitokohteet
  "Ylläpitokohteet"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.grid :as grid]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.yleiset :refer [ajax-loader linkki livi-pudotusvalikko]]
            [harja.ui.komponentti :as komp]
            [harja.fmt :as fmt]
            [harja.loki :refer [log logt tarkkaile!]]
            [clojure.string :as str]
            [cljs.core.async :refer [<!]]
            [harja.tyokalut.vkm :as vkm]
            [harja.domain.tierekisteri :as tierekisteri-domain]
            [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]
            [harja.domain.paallystysilmoitus :as pot]
            [harja.tiedot.urakka.yhatuonti :as yha]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.napit :as napit]
            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.yllapitokohteet :as tiedot]
            [harja.tiedot.urakka :as u])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]
                   [harja.atom :refer [reaction<!]]))

(defn laske-sarakkeen-summa [sarake kohderivit]
  (reduce + (mapv
              (fn [rivi] (sarake rivi))
              kohderivit)))

(defn tr-virheilmoitus [tr-virheet]
  [:div.tr-virheet
   (for [virhe (into #{} (vals @tr-virheet))]
     ^{:key (hash virhe)}
     [:div.tr-virhe (ikonit/livicon-warning-sign)
      virhe])])

;; Ylläpitokohteiden sarakkeiden leveydet
(def haitari-leveys 5)
(def id-leveys 10)
(def kohde-leveys 15)
(def kvl-leveys 5)
(def yllapitoluokka-leveys 5)
(def nykyinen-paallyste-leveys 8)
(def tr-leveys 8)
(def tarjoushinta-leveys 10)
(def muutoshinta-leveys 10)
(def toteutunut-hinta-leveys 10)
(def arvonvahennykset-leveys 10)
(def bitumi-indeksi-leveys 10)
(def kaasuindeksi-leveys 10)
(def yhteensa-leveys 10)

;; Ylläpitokohdeosien sarakkeiden leveydet
(def nimi-leveys 20)
(def toimenpide-leveys 20)

(defn tierekisteriosoite-sarakkeet [perusleveys [nimi tie ajorata kaista aosa aet losa let]]
  (into []
        (remove
          nil?
          [(when nimi {:otsikko "Nimi" :nimi (:nimi nimi) :tyyppi :string
                       :leveys (+ perusleveys 5) :muokattava? (or (:muokattava? nimi) (constantly true))})
           {:otsikko "Tie\u00ADnu\u00ADme\u00ADro" :nimi (:nimi tie)
            :tyyppi :positiivinen-numero :leveys perusleveys :tasaa :oikea
            :validoi [[:ei-tyhja "Anna tienumero"]] :muokattava? (or (:muokattava? tie) (constantly true))}
           {:otsikko "Ajo\u00ADrata"
            :nimi (:nimi ajorata)
            :muokattava? (or (:muokattava? ajorata) (constantly true))
            :tyyppi :valinta
            :tasaa :oikea
            :valinta-arvo :koodi
            :valinta-nayta (fn [arvo muokattava?]
                             (if arvo (:koodi arvo) (if muokattava?
                                                      "- Valitse ajorata -"
                                                      "")))
            :valinnat pot/+ajoradat+
            :leveys perusleveys}
           {:otsikko "Kais\u00ADta"
            :muokattava? (or (:muokattava? kaista) (constantly true))
            :nimi (:nimi kaista)
            :tyyppi :valinta
            :tasaa :oikea
            :valinta-arvo :koodi
            :valinta-nayta (fn [arvo muokattava?]
                             (if arvo (:koodi arvo) (if muokattava?
                                                      "- Valitse kaista -"
                                                      "")))
            :valinnat pot/+kaistat+
            :leveys perusleveys}
           {:otsikko "Aosa" :nimi (:nimi aosa) :leveys perusleveys :tyyppi :positiivinen-numero :tasaa :oikea
            :validoi [[:ei-tyhja "Anna alkuosa"]] :muokattava? (or (:muokattava? aosa) (constantly true))}
           {:otsikko "Aet" :nimi (:nimi aet) :leveys perusleveys :tyyppi :positiivinen-numero :tasaa :oikea
            :validoi [[:ei-tyhja "Anna alkuetäisyys"]] :muokattava? (or (:muokattava? aet) (constantly true))}
           {:otsikko "Losa" :nimi (:nimi losa) :leveys perusleveys :tyyppi :positiivinen-numero :tasaa :oikea
            :validoi [[:ei-tyhja "Anna loppuosa"]] :muokattava? (or (:muokattava? losa) (constantly true))}
           {:otsikko "Let" :nimi (:nimi let) :leveys perusleveys :tyyppi :positiivinen-numero :tasaa :oikea
            :validoi [[:ei-tyhja "Anna loppuetäisyys"]] :muokattava? (or (:muokattava? let) (constantly true))}
           {:otsikko "Pit." :nimi :pituus :leveys perusleveys :tyyppi :numero :tasaa :oikea
            :muokattava? (constantly false) :hae (fn [rivi] (tierekisteri-domain/laske-tien-pituus rivi))}])))

(defn tr-osoite [rivi]
  (let [arvot (map rivi [:tr-numero :tr-alkuosa :tr-alkuetaisyys :tr-loppuosa :tr-loppuetaisyys])]
    (when (every? #(not (str/blank? %)) arvot)
      ;; Tierekisteriosoite on täytetty (ei tyhjiä kenttiä)
      (zipmap [:numero :alkuosa :alkuetaisyys :loppuosa :loppuetaisyys]
              arvot))))

(defn validoi-tr-osoite [grid tr-sijainnit-atom tr-virheet-atom]
  ; FIXME Pitäisi generisöidä jotta voi käyttää myös POT-lomakkeessa muokkausgridissä
  (log "VIRHEET:" (pr-str (grid/hae-virheet grid)))
  (let [haetut (into #{} (keys @tr-sijainnit-atom))]
    ;; jos on tullut uusi TR osoite, haetaan sille sijainti
    (doseq [[id rivi] (grid/hae-muokkaustila grid)]
      (if (:poistettu rivi)
        (swap! tr-virheet-atom dissoc id)
        (let [osoite (tr-osoite rivi)]
          (when (and osoite (not (haetut osoite)))
            (go
              (log "Haetaan TR osoitteen sijainti: " (pr-str osoite))
              (let [sijainti (<! (vkm/tieosoite->viiva osoite))]
                (when (= (get (grid/hae-muokkaustila grid) id) rivi) ;; ettei rivi ole uudestaan muuttunut
                  (if-let [virhe (when-not (vkm/loytyi? sijainti)
                                   "Virheellinen TR-osoite")]
                    (do (swap! tr-virheet-atom assoc id virhe)
                        (doseq [kentta [:tr-numero :tr-alkuosa :tr-alkuetaisyys :tr-loppuosa :tr-loppuetaisyys]]
                          (grid/aseta-virhe! grid id kentta "Tarkista tie")))
                    (do (swap! tr-virheet-atom dissoc id)
                        (doseq [kentta [:tr-numero :tr-alkuosa :tr-alkuetaisyys :tr-loppuosa :tr-loppuetaisyys]]
                          (grid/poista-virhe! grid id kentta))
                        (log "sain sijainnin " (clj->js sijainti))
                        (swap! tr-sijainnit-atom assoc osoite sijainti))))))))))))

(defn kasittele-paivittyneet-kohdeosat [kohteet]
  (let [uudet-kohteet
        ;; Kopioi kohteen N loppuosa kohtee N + 1 alkuosaksi
        ; FIXME Pitäisi tunnistaa kumpaa muokattiin, jotta kopiointi toimii myös toiseen suuntaan
        (into [] (map-indexed
                   (fn [index kohde]
                     (if (< index (- (count kohteet) 1))
                       (-> kohde
                           (assoc :tr-loppuosa (:tr-alkuosa (get kohteet (inc index))))
                           (assoc :tr-loppuetaisyys (:tr-alkuetaisyys (get kohteet (inc index)))))
                       kohde))
                   kohteet))]
    uudet-kohteet))

(defn lisaa-uusi-kohdeosa
  "Lisää uuden kohteen annetussa indeksissä olevan kohteen perään (alapuolelle)."
  [kohteet index]
  (log "[KOHDEOSA] Lisää uusi kohdeosa indeksin " index  " perään.")
  (let [uudet-kohteet (into [] (concat
                                 (take (inc index) kohteet)
                                 [{:nimi ""
                                  :tr-numero (:tr-numero (get kohteet index))
                                  :tr-alkuosa nil
                                  :tr-alkuetaisyys nil
                                  :tr-loppuosa (:tr-loppuosa (get kohteet index))
                                  :tr-loppuetaisyys (:tr-loppuetaisyys (get kohteet index))
                                  :toimenpide ""}]
                                 (drop (inc index) kohteet)))
        uudet-kohteet (assoc uudet-kohteet index (-> (get uudet-kohteet index)
                                                     (assoc :tr-loppuosa nil)
                                                     (assoc :tr-loppuetaisyys nil)))]
    uudet-kohteet))

(defn yllapitokohdeosat [kohdeosat yllapitokohde-id]
  (let [grid-data (atom (zipmap (iterate inc 1) kohdeosat))]
    (komp/luo
      (fn [kohdeosat]
        [:div
         [grid/muokkaus-grid
          {:otsikko "Tierekisterikohteet"
           ;; YHA-sidotuille kohdeosille on toteutettu custom lisäys ja poistologiikka
           :voi-lisata? false
           :piilota-toiminnot? true
           :paneelikomponentit [(fn []
                                  [napit/palvelinkutsu-nappi
                                   [yleiset/ikoni-ja-teksti (ikonit/tallenna) "Tallenna"]
                                   #(let [urakka-id (:id @nav/valittu-urakka)
                                          [sopimus-id _] @u/valittu-sopimusnumero]
                                     (tiedot/tallenna-yllapitokohdeosat! urakka-id
                                                                         sopimus-id
                                                                         yllapitokohde-id
                                                                         (vals @grid-data)))
                                   {:luokka "nappi-myonteinen grid-tallenna"
                                    :virheviesti "Tallentaminen epäonnistui."}])]
           :voi-poistaa? (constantly false)
           :muutos #(let [uudet-kohteet (kasittele-paivittyneet-kohdeosat (into [] (vals @grid-data)))]
                     (reset! grid-data (zipmap (iterate inc 1) uudet-kohteet)))}
          (into [] (remove
                     nil?
                     (concat
                       (tierekisteriosoite-sarakkeet
                         tr-leveys
                         [{:nimi :nimi}
                          {:nimi :tr-numero}
                          {:nimi :tr-ajorata}
                          {:nimi :tr-kaista}
                          {:nimi :tr-alkuosa :muokattava? (fn [_ index]
                                                            (> index 0))}
                          {:nimi :tr-alkuetaisyys :muokattava? (fn [_ index]
                                                                 (> index 0))}
                          {:nimi :tr-loppuosa :muokattava? (fn [_ index]
                                                             (< index (- (count kohdeosat) 1)))}
                          {:nimi :tr-loppuetaisyys :muokattava? (fn [_ index]
                                                                  (< index (- (count kohdeosat) 1)))}])
                       [{:otsikko "Toimenpide" :nimi :toimenpide :tyyppi :string :leveys toimenpide-leveys}
                        {:otsikko "Asdlol" :nimi :tr-muokkaus :tyyppi :komponentti :leveys tr-leveys
                           :komponentti (fn [_ index]
                                          [:button.nappi-ensisijainen
                                           #_{:on-click (fn []
                                                        (let [paivitetyt-kohdeosat (lisaa-uusi-kohdeosa (into [] kohdeosat) index)
                                                              paivitetyt-yllapitokohteet
                                                              (paivita-kohteen-kohdeosat @yllapitokohteet-atom
                                                                                         (:id rivi)
                                                                                         paivitetyt-kohdeosat)]
                                                          (reset! yllapitokohteet-atom paivitetyt-yllapitokohteet)))}
                                           (yleiset/ikoni-ja-teksti (ikonit/livicon-arrow-down) "Lisää")])}])))
          grid-data]
         #_[napit/palvelinkutsu-nappi
          "Tallenna kohdeosat"
          #(tallenna-kohdeosat (vals @grid-data))
          {:luokka "nappi-ensisijainen"
           :virheviesti "Tallentaminen epäonnistui."}]]))))

(defn yllapitokohteet [kohteet-atom optiot]
  (let [tr-sijainnit (atom {}) ;; onnistuneesti haetut TR-sijainnit
        tr-virheet (atom {}) ;; virheelliset TR sijainnit
        tallenna (reaction (if (and @yha/yha-kohteiden-paivittaminen-kaynnissa? (:yha-sidottu? optiot))
                             :ei-mahdollinen
                             (:tallenna optiot)))]
    (komp/luo
      (fn [kohteet-atom optiot]
        [:div.yllapitokohteet
         [grid/grid
          {:otsikko (:otsikko optiot)
           :tyhja (if (nil? @kohteet-atom) [ajax-loader "Haetaan kohteita..."] "Ei kohteita")
           :vetolaatikot (into {} (map (juxt :id
                                             (fn [rivi]
                                               [yllapitokohdeosat (into [] (:kohdeosat rivi)) (:id rivi)]))
                                       @kohteet-atom))
           :tallenna @tallenna
           :muutos (fn [grid]
                     (validoi-tr-osoite grid tr-sijainnit tr-virheet))
           :voi-lisata? (not (:yha-sidottu? optiot))
           :voi-poistaa? (constantly (not (:yha-sidottu? optiot)))
           :esta-poistaminen? (fn [rivi] (or (not (nil? (:paallystysilmoitus-id rivi)))
                                             (not (nil? (:paikkausilmoitus-id rivi)))))
           :esta-poistaminen-tooltip (fn [_] "Kohteelle on kirjattu ilmoitus, kohdetta ei voi poistaa.")}
          (into []
                (concat
                  [{:tyyppi :vetolaatikon-tila :leveys haitari-leveys}
                   {:otsikko "Koh\u00ADde\u00ADnu\u00ADme\u00ADro" :nimi :kohdenumero :tyyppi :string :leveys id-leveys
                    :validoi [[:uniikki "Sama kohdenumero voi esiintyä vain kerran."]]}
                   {:otsikko "Koh\u00ADteen ni\u00ADmi" :nimi :nimi
                    :tyyppi :string :leveys kohde-leveys}]
                  (tierekisteriosoite-sarakkeet
                    tr-leveys
                    [nil
                     {:nimi :tr-numero :muokattava? (constantly (not (:yha-sidottu? optiot)))}
                     {:nimi :tr-ajorata}
                     {:nimi :tr-kaista}
                     {:nimi :tr-alkuosa}
                     {:nimi :tr-alkuetaisyys}
                     {:nimi :tr-loppuosa}
                     {:nimi :tr-loppuetaisyys}])
                  [{:otsikko "KVL"
                    :nimi :keskimaarainen-vuorokausiliikenne :tyyppi :numero :leveys kvl-leveys
                    :muokattava? (constantly (not (:yha-sidottu? optiot)))}
                   {:otsikko "Yl\u00ADlä\u00ADpi\u00ADto\u00ADluok\u00ADka"
                    :nimi :yllapitoluokka :tyyppi :numero :leveys yllapitoluokka-leveys
                    :muokattava? (constantly (not (:yha-sidottu? optiot)))}
                   {:otsikko "Ny\u00ADkyi\u00ADnen pääl\u00ADlys\u00ADte"
                    :nimi :nykyinen-paallyste
                    :fmt #(paallystys-ja-paikkaus/hae-paallyste-koodilla %)
                    :tyyppi :valinta
                    :valinta-arvo :koodi
                    :valinnat paallystys-ja-paikkaus/+paallystetyypit+
                    :valinta-nayta :nimi
                    :leveys nykyinen-paallyste-leveys
                    :muokattava? (constantly (not (:yha-sidottu? optiot)))}
                   (when (= (:nakyma optiot) :paallystys)
                     {:otsikko "Tar\u00ADjous\u00ADhinta" :nimi :sopimuksen-mukaiset-tyot
                      :fmt fmt/euro-opt :tyyppi :numero :leveys tarjoushinta-leveys :tasaa :oikea})
                   (when (= (:nakyma optiot) :paallystys)
                     {:otsikko "Muutok\u00ADset" :nimi :muutoshinta :muokattava? (constantly false)
                      :fmt fmt/euro-opt :tyyppi :numero :leveys muutoshinta-leveys :tasaa :oikea})
                   (when (= (:nakyma optiot) :paikkaus)
                     {:otsikko "Toteutunut hinta" :nimi :toteutunut-hinta :muokattava? (constantly false)
                      :fmt fmt/euro-opt :tyyppi :numero :leveys toteutunut-hinta-leveys :tasaa :oikea})
                   {:otsikko "Ar\u00ADvon\u00ADväh." :nimi :arvonvahennykset :fmt fmt/euro-opt
                    :tyyppi :numero :leveys arvonvahennykset-leveys :tasaa :oikea}
                   {:otsikko "Bi\u00ADtumi-in\u00ADdek\u00ADsi" :nimi :bitumi-indeksi :fmt fmt/euro-opt
                    :tyyppi :numero :leveys bitumi-indeksi-leveys :tasaa :oikea}
                   {:otsikko "Kaa\u00ADsu\u00ADindeksi" :nimi :kaasuindeksi :fmt fmt/euro-opt
                    :tyyppi :numero :leveys kaasuindeksi-leveys :tasaa :oikea}
                   {:otsikko "Ko\u00ADko\u00ADnais\u00ADhinta (ind\u00ADek\u00ADsit mu\u00ADka\u00ADna)" :muokattava? (constantly false)
                    :nimi :kokonaishinta :fmt fmt/euro-opt :tyyppi :numero :leveys yhteensa-leveys :tasaa :oikea
                    :hae (fn [rivi] (+ (:sopimuksen-mukaiset-tyot rivi)
                                       (:muutoshinta rivi)
                                       (:toteutunut-hinta rivi)
                                       (:arvonvahennykset rivi)
                                       (:bitumi-indeksi rivi)
                                       (:kaasuindeksi rivi)))}]))
          (sort-by tierekisteri-domain/tiekohteiden-jarjestys @kohteet-atom)]
         [tr-virheilmoitus tr-virheet]]))))

(defn yllapitokohteet-yhteensa [kohteet-atom optiot]
  (let [yhteensa (reaction (let [kohteet @kohteet-atom
                                 sopimuksen-mukaiset-tyot-yhteensa (laske-sarakkeen-summa :sopimuksen-mukaiset-tyot kohteet)
                                 toteutunut-hinta-yhteensa (laske-sarakkeen-summa :toteutunut-hinta kohteet)
                                 muutoshinta-yhteensa (laske-sarakkeen-summa :muutoshinta kohteet)
                                 arvonvahennykset-yhteensa (laske-sarakkeen-summa :arvonvahennykset kohteet)
                                 bitumi-indeksi-yhteensa (laske-sarakkeen-summa :bitumi-indeksi kohteet)
                                 kaasuindeksi-yhteensa (laske-sarakkeen-summa :kaasuindeksi kohteet)
                                 kokonaishinta (+ sopimuksen-mukaiset-tyot-yhteensa
                                                  toteutunut-hinta-yhteensa
                                                  muutoshinta-yhteensa
                                                  arvonvahennykset-yhteensa
                                                  bitumi-indeksi-yhteensa
                                                  kaasuindeksi-yhteensa)]
                             [{:id 0
                               :sopimuksen-mukaiset-tyot sopimuksen-mukaiset-tyot-yhteensa
                               :muutoshinta muutoshinta-yhteensa
                               :toteutunut-hinta toteutunut-hinta-yhteensa
                               :arvonvahennykset arvonvahennykset-yhteensa
                               :bitumi-indeksi bitumi-indeksi-yhteensa
                               :kaasuindeksi kaasuindeksi-yhteensa
                               :kokonaishinta kokonaishinta}]))]
    [grid/grid
     {:otsikko "Yhteensä"
      :tyhja (if (nil? {}) [ajax-loader "Lasketaan..."] "")}
     [{:otsikko "" :nimi :tyhja :tyyppi :string :leveys haitari-leveys}
      {:otsikko "" :nimi :kohdenumero :tyyppi :string :leveys id-leveys}
      {:otsikko "" :nimi :nimi :tyyppi :string :leveys kohde-leveys}
      {:otsikko "" :nimi :tr-numero :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :tr-ajorata :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :tr-kaista :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :tr-alkuosa :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :tr-alkuetaisyys :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :tr-loppuosa :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :tr-loppuetaisyys :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :pit :tyyppi :string :leveys tr-leveys}
      {:otsikko "" :nimi :yllapitoluokka :tyyppi :string :leveys yllapitoluokka-leveys}
      {:otsikko "" :nimi :nimi :tyyppi :string :leveys kohde-leveys}
      {:otsikko "" :nimi :keskimaarainen-vuorokausiliikenne :tyyppi :string :leveys kvl-leveys}
      {:otsikko "" :nimi :nykyinen-paallyste :tyyppi :string :leveys nykyinen-paallyste-leveys}
      (when (= (:nakyma optiot) :paallystys)
        {:otsikko "Tarjous\u00ADhinta" :nimi :sopimuksen-mukaiset-tyot :fmt fmt/euro-opt :tyyppi :numero
         :leveys tarjoushinta-leveys :tasaa :oikea})
      (when (= (:nakyma optiot) :paallystys)
        {:otsikko "Muutok\u00ADset" :nimi :muutoshinta :fmt fmt/euro-opt :tyyppi :numero
         :leveys muutoshinta-leveys :tasaa :oikea})
      (when (= (:nakyma optiot) :paikkaus)
        {:otsikko "Toteutunut hinta" :nimi :toteutunut-hinta :fmt fmt/euro-opt :tyyppi :numero
         :leveys toteutunut-hinta-leveys :tasaa :oikea})
      {:otsikko "Arvon\u00ADväh." :nimi :arvonvahennykset :fmt fmt/euro-opt :tyyppi :numero
       :leveys arvonvahennykset-leveys :tasaa :oikea}
      {:otsikko "Bitumi-indeksi" :nimi :bitumi-indeksi :fmt fmt/euro-opt :tyyppi :numero
       :leveys bitumi-indeksi-leveys :tasaa :oikea}
      {:otsikko "Kaasu\u00ADindeksi" :nimi :kaasuindeksi :fmt fmt/euro-opt :tyyppi :numero
       :leveys kaasuindeksi-leveys :tasaa :oikea}
      {:otsikko "Kokonais\u00ADhinta (indeksit mukana)" :nimi :kokonaishinta :fmt fmt/euro-opt
       :tyyppi :numero :leveys yhteensa-leveys :tasaa :oikea}
      {:otsikko "" :nimi :muokkaustoiminnot-tyhja :tyyppi :string :leveys 3}]
     @yhteensa]))