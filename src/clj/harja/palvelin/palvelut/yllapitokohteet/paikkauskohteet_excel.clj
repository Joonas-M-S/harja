(ns harja.palvelin.palvelut.yllapitokohteet.paikkauskohteet-excel
  "Luetaan paikkauskohteet excelistä tiedot ulos"
  (:require [dk.ative.docjure.spreadsheet :as xls]
            [harja.palvelin.raportointi.excel :as excel]
            [harja.kyselyt.paikkaus :as q]
            [harja.kyselyt.urakat :as q-urakat]
            [harja.pvm :as pvm])
  (:import (org.apache.poi.ss.util CellRangeAddress)))

(defn erottele-paikkauskohteet [workbook]
  (let [sivu (first (xls/sheet-seq workbook)) ;; Käsitellään excelin ensimmäinen sivu tai tabi
        ;; Esimerkki excelissä paikkauskohteet alkavat vasta viidenneltä riviltä.
        ;; Me emme voi olla tästä kuitenkaan ihan varmoja, niin luetaan varalta kaikki data excelistä ulos
        raaka-data (xls/select-columns {:A :nro, :B :nimi :C :tie :D :ajorata :E :aosa :F :aet :G :losa
                                        :H :let :I :pituus :J :alkupvm :K :loppupvm :L :tyomenetelma
                                        :M :suunniteltu-maara :N :yksikko :O :suunniteltu-hinta :P :lisatiedot}
                                       sivu)

        ;; Tämä toimii nykyisellä excel-pohjalla toistaiseksi.
        ;; Katsotaan, millä rivillä otsikkorivi on, oletuksena että sieltä löytyy ainakin "Nro." ja "kohde" otsikot.
        ;; Ja otetaan otsikon jälkeiset rivit, joissa on nimi. Päästetään tässä vaiheessa myös selvästi virheelliset
        ;; rivit läpi, jotta voidaan palauttaa validaatiovirheet.
        otsikko-idx (first (keep-indexed (fn [idx rivi] (when (and (= "Nro." (:nro rivi)) (= "Kohde" (:nimi rivi))) idx)) raaka-data))
        paikkauskohteet (remove #(nil? (:nimi %)) (subvec raaka-data (inc otsikko-idx)))]
    paikkauskohteet))

(def lahtotiedot-sisalto
  [["Paikkausmenetelmät" "Yksikkö"]
   ["AB-paikkaus levittäjällä" "t"]
   ["PAB-paikkaus levittäjällä" "m2"]
   ["KT-valuasfalttipaikkaus (KTVA) " "jm"]
   ["Konetiivistetty reikävaluasfalttipaikkaus (REPA)" "kpl"]
   ["Sirotepuhalluspaikkaus (SIPU)" ""]
   ["Sirotepintauksena tehty lappupaikkaus (SIPA)" ""]
   ["Urapaikkaus (UREM/RREM)" ""]
   ["Kannukaatosaumaus" ""]
   ["KT-valuasfalttisaumaus" ""]
   ["Avarrussaumaus" ""]
   ["Sillan kannen päällysteen päätysauman korjaukset" ""]
   ["Reunapalkin ja päällysteen välisen sauman tiivistäminen" ""]
   ["Reunapalkin liikuntasauman tiivistäminen" ""]
   ["Käsin tehtävät paikkaukset pikapaikkausmassalla" ""]
   ["AB-paikkaus käsin" ""]
   ["PAB-paikkaus käsin" ""]
   ["Muu päällysteiden paikkaustyö" ""]])

(def paikkauskohteet-otsikot
  ["Nro." "Kohde" "Tienro" "Ajr" "Aosa"
   "Aet" "Losa" "Let" "Arvioitu aloitus pvm"
   "Arvioitu lopetus pvm" "Työmenetelmä" "Määrä"
   "Yksikkö" "Kustannusarvio" "Lisätiedot"])

(defn tee-excel [tiedostonimi data]
  (let [wb (xls/create-workbook "Paikkaukset"
                                [["Paikkausehdotukset"]
                                 []
                                 []
                                 paikkauskohteet-otsikot]
                                "Lähtötiedot"
                                lahtotiedot-sisalto
                                )
        ps (xls/select-sheet "Paikkaukset" wb)
        _ (do
            (doall (map #(.autoSizeColumn ps %) (range 16)))
            (xls/set-cell-style! (xls/select-cell "A1" ps)
                                 (xls/create-cell-style! wb {:font {:size 14 :name "Arial" :bold true}
                                                             :valign :center}))
            (xls/set-row-style! (nth (xls/row-seq ps) 3) (xls/create-cell-style! wb {:font {:name "Arial" :bold true :size 10}
                                                                                     :border-bottom :thin
                                                                                     :border-top :thin
                                                                                     :border-left :thin
                                                                                     :border-right :thin})))
        file (xls/save-workbook-into-file! tiedostonimi wb)]
    file))

(defn- rivita-kohteet [kohteet]
  (concat
    (when (> (count kohteet) 0)
      (mapcat
        (fn [rivi]
          (let [suunniteltu-summa (or (:suunniteltu-hinta rivi) 0)]
            [{:rivi [(str (:nro rivi))
                     (:nimi rivi)
                     (:tie rivi)
                     nil
                     (:aosa rivi)
                     (:aet rivi)
                     (:losa rivi)
                     (:let rivi)
                     (pvm/pvm-opt (:alkupvm rivi))
                     (pvm/pvm-opt (:loppupvm rivi))
                     (or (:tyomenetelma rivi) nil)
                     (:suunniteltu-maara rivi)
                     (:yksikko rivi)
                     suunniteltu-summa
                     (:lisatiedot rivi)]
              :lihavoi? false}]))
        kohteet))))

(defn paikkauskohteet-excel
  [db workbook user tiedot]
  ;;TODO: Tarkistetaanko oikeudet tässä?
  (let [urakka (first (q-urakat/hae-urakka db (:urakka-id tiedot)))
        _ (println "urakka" (pr-str urakka))
        kohteet (q/paikkauskohteet db user tiedot)
        sarakkeet [{:otsikko "Nro." :tasaa :oikea}
                   {:otsikko "Kohde"}
                   {:otsikko "Tienro"}
                   {:otsikko "Ajr"}
                   {:otsikko "Aosa"}
                   {:otsikko "Aet"}
                   {:otsikko "Losa"}
                   {:otsikko "Let"}
                   {:otsikko "Arvioitu aloitus pvm" :tasaa :oikea}
                   {:otsikko "Arvioitu lopetus pvm" :tasaa :oikea}
                   {:otsikko "Työmenetelmä"}
                   {:otsikko "Määrä"}
                   {:otsikko "Yksikkö" :tasaa :oikea}
                   {:otsikko "Kustannusarvio" :tasaa :oikea}
                   {:otsikko "Lisätiedot"}]

        rivit (rivita-kohteet kohteet)
        optiot {:nimi "Paikkauskohteet"
                :sheet-nimi "Paikkauskohteet"
                :tyhja (if (empty? kohteet) "Ei paikkauskohteita.")
                :lista-tyyli? true
                :rivi-ennen [{:teksti "Paikkauskohteet" :sarakkeita 2}
                             {:teksti (str (pvm/pvm-opt (:alkupvm tiedot)) " - " (pvm/pvm-opt (:loppupvm tiedot))) :sarakkeita 7}]}
        taulukot [[:taulukko optiot sarakkeet
                   rivit]]
        tiedostonimi (str (:nimi urakka) "-Paikkauskohteet-" (pvm/vuosi (:alkupvm tiedot)))
        ;; Nimi: <urakkan_nimi>-Paikkauskohteet-<vuosi>
        taulukko (concat
                   [:raportti {:nimi tiedostonimi
                               :orientaatio :landscape}]
                   (if (empty? taulukot)
                     [[:taulukko optiot nil [["Ei paikkauskohteita"]]]]
                     taulukot))]
    (excel/muodosta-excel (vec taulukko)
                          workbook)))
