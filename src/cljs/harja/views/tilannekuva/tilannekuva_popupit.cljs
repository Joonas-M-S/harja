(ns harja.views.tilannekuva.tilannekuva-popupit
  (:require [harja.tiedot.navigaatio :as nav]
            [harja.loki :refer [log]]
            [harja.pvm :as pvm]
            [harja.views.kartta :as kartta]
            [harja.tiedot.ilmoitukset :as ilmoitukset]
            [harja.tiedot.urakka.laadunseuranta.laatupoikkeamat :as laatupoikkeamat]
            [harja.tiedot.tilannekuva.historiakuva :as tiedot]
            [clojure.string :as str]))

(def klikattu-tyokone (atom nil))

(defn- rakenna [kartta]
  "Funktio, joka rakentaa mapistä hiccup-rakenteen. Ei ole tarkoituskaan olla kovin älykäs, vaan helpottaa lähinnä
  kehitystyötä."
  (log (pr-str kartta))
  (for [avain (keys kartta)]
    (cond
      (map? (get kartta avain)) (into [:div [:b (pr-str avain)]] (rakenna (get kartta avain)))

      (or (set? (get kartta avain)) (vector? (get kartta avain)))
      [:div [:b (pr-str avain)] (clojure.string/join ", " (get kartta avain))]

      :else [:div [:b (pr-str avain)] (pr-str (get kartta avain))])))

(defn tee-popup [otsikko otsikko-arvo-parit]
  [:div.kartta-popup
   [:p [:b otsikko]]
  [:table.otsikot-ja-arvot
  (for [[otsikko arvo] otsikko-arvo-parit]
    ^{:key otsikko}
    (when arvo
      [:tr
       [:td.otsikko otsikko]
       [:td.arvo arvo]]))]])

(defmulti nayta-popup :aihe)

(defn- viivan-keskella [tapahtuma]
  (if-let [reittipisteet (or (get-in tapahtuma [:alue :points])
                             (mapcat :points (get-in tapahtuma [:sijainti :lines])))]
    (nth reittipisteet (int (/ (count reittipisteet) 2)))))

(defn geometrian-koordinaatti [tapahtuma]
  (if-let [piste (get-in tapahtuma [:sijainti :coordinates])]
    piste
    (viivan-keskella tapahtuma)))

(defmethod nayta-popup :toteuma-klikattu [tapahtuma]
  (kartta/nayta-popup! (viivan-keskella tapahtuma)
                       [:div.kartta-toteuma-popup
                        [:p [:b "Toteuma"]]
                        [:p "Aika: " (pvm/pvm (:alkanut tapahtuma)) "-" (pvm/pvm (:paattynyt tapahtuma))]
                        (when (:suorittaja tapahtuma)
                          [:span
                           [:p "Suorittaja: " (get-in tapahtuma [:suorittaja :nimi])]])
                        (when-not (empty? (:tehtavat tapahtuma))
                          (doall
                            (for [tehtava (:tehtavat tapahtuma)]
                              [:span
                               [:p "Toimenpide: " (:toimenpide tehtava)]
                               [:p "Määrä: " (:maara tehtava)]
                               [:p "Päivän hinta: " (:paivanhinta tehtava)]
                               [:p "Lisätieto: " (:lisatieto tehtava)]])))
                        (when-not (empty? (:materiaalit tapahtuma))
                          (doall
                            (for [toteuma (:materiaalit tapahtuma)]
                              [:span
                               [:p "Materiaali: " (get-in toteuma [:materiaali :nimi])]
                               [:p "Määrä: " (:maara toteuma)]])))
                        (when (:lisatieto tapahtuma)
                          [:p "Lisätieto: " (:lisatieto tapahtuma)])]))


(defmethod nayta-popup :reittipiste-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       [:div.kartta-reittipiste-popup
                        [:p [:b "Reittipiste"]]
                        [:p "Aika: " (pvm/pvm (:aika tapahtuma))]
                        (when (get-in tapahtuma [:tehtava :id])
                          [:span
                           [:p "Toimenpide: " (get-in tapahtuma [:tehtava :toimenpide])]
                           [:p "Määrä: " (get-in tapahtuma [:tehtava :maara])]])
                        (when (get-in tapahtuma [:materiaali :id])
                          [:span
                           [:p "Materiaali: " (get-in tapahtuma [:materiaali :nimi])]
                           [:p "Määrä: " (get-in tapahtuma [:materiaali :maara])]])]))

(defmethod nayta-popup :ilmoitus-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       [:div.kartta-ilmoitus-popup
                        [:p [:b (if (= :toimenpidepyynto (:ilmoitustyyppi tapahtuma))
                                  "Toimenpidepyyntö"
                                  (str/capitalize (name (:ilmoitustyyppi tapahtuma))))]]
                        [:p "Ilmoitettu: " (pvm/pvm-aika-sek (:ilmoitettu tapahtuma))]
                        [:p "Vapaateksti: " (:lyhytselite tapahtuma)]
                        [:p (count (:kuittaukset tapahtuma)) " kuittausta."]
                        [:a {:href     "#"
                             :on-click #(do (.preventDefault %)
                                            (let [putsaa (fn [asia]
                                                           (dissoc asia :type :alue))]
                                              (reset! nav/sivu :ilmoitukset)
                                              (reset! ilmoitukset/haetut-ilmoitukset
                                                      (map putsaa (filter
                                                                    (fn [asia] (= (:type asia) :ilmoitus))
                                                                    @tiedot/historiakuvan-asiat-kartalla)))
                                              (reset! ilmoitukset/valittu-ilmoitus (putsaa tapahtuma))))}
                         "Siirry ilmoitusnäkymään"]]))

(defmethod nayta-popup :tyokone-klikattu [tapahtuma]
  (reset! klikattu-tyokone (:tyokoneid tapahtuma))
  (kartta/keskita-kartta-pisteeseen (:sijainti tapahtuma))

  (kartta/nayta-popup! (:sijainti tapahtuma)
                       [:div.kartta-tyokone-popup
                        [:p [:b "Työkone"]]
                        [:div "Tyyppi: " (:tyokonetyyppi tapahtuma)]
                        [:div "Viimeisin paikkatieto (lähetetty): " (pvm/pvm-aika-sek (:lahetysaika tapahtuma))]
                        [:div "Organisaatio: " (:organisaationimi tapahtuma)]
                        [:div "Urakka: " (:urakkanimi tapahtuma)]
                        [:div "Tehtävät: "
                         (let [tehtavat (str/join ", " (:tehtavat tapahtuma))]
                           [:span tehtavat])]]))

(defmethod nayta-popup :uusi-tyokonedata [data]
  (when-let [tk @klikattu-tyokone]
    (when-let [haettu (first (filter #(= tk (:tyokoneid %))
                                     (:tyokoneet data)))]
      (kartta/keskita-kartta-pisteeseen (:sijainti haettu))
      (kartta/poista-popup!)
      (nayta-popup (assoc haettu :aihe :tyokone-klikattu)))))


(defmethod nayta-popup :laatupoikkeama-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (tee-popup "Laatupoikkeama"
                                  [["Aika" (pvm/pvm-aika-sek (:aika tapahtuma))]
                                   ["Tekijä" (:tekijanimi tapahtuma) ", " (name (:tekija tapahtuma))]
                                   ["Päätös" (str (laatupoikkeamat/kuvaile-paatostyyppi (get-in tapahtuma [:paatos :paatos]))
                                                  " ("  (pvm/pvm-aika (get-in tapahtuma [:paatos :kasittelyaika])) ")")]])))

(defmethod nayta-popup :tarkastus-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (tee-popup (str/capitalize (name (:tyyppi tapahtuma)))
                                  [["Aika" (pvm/pvm-aika-sek (:aika tapahtuma))]
                                   ["Mittaaja" (:mittaaja tapahtuma)]])))

(defmethod nayta-popup :turvallisuuspoikkeama-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       (tee-popup "Turvallisuuspoikkeama"
                                  [["Tapahtunut" (pvm/pvm-aika (:tapahtunut tapahtuma)) " - " (pvm/pvm-aika (:paattynyt tapahtuma))]
                                   ["Käsitelty" (pvm/pvm-aika (:kasitelty tapahtuma))]
                                   ["Työ\u00ADtehtävä" (:tyontekijanammatti tapahtuma) ", " (:tyotehtava tapahtuma)]
                                   ["Vammat" (:vammat tapahtuma)]
                                   ["Sairaala\u00ADvuorokaudet" (:sairaalavuorokaudet tapahtuma)]
                                   ["Sairaus\u00ADpoissaolo\u00ADpäivät" (:sairauspoissaolopaivat tapahtuma)]
                                   ["Kuvaus" (:kuvaus tapahtuma)]
                                   ["Korjaavat toimen\u00ADpiteet" (count (filter :suoritettu (:korjaavattoimenpiteet tapahtuma)))
                                    "/" (count (:korjaavattoimenpiteet tapahtuma))]])))

(defmethod nayta-popup :paallystys-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       [:div.kartta-popup
                        [:p [:b "Päällystyskohde"]]
                        [:div (:nimi tapahtuma)]
                        [:div "Toimenpide: " (:toimenpide tapahtuma)]
                        [:div "Nykyinen päällyste: " (:nykyinen_paallyste tapahtuma)]]))

(defmethod nayta-popup :paikkaus-klikattu [tapahtuma]
  (kartta/nayta-popup! (geometrian-koordinaatti tapahtuma)
                       [:div.kartta-popup
                        [:p [:b "Paikkaustoteuma"]]
                        [:div (:nimi tapahtuma)]
                        [:div "Toimenpide: " (:toimenpide tapahtuma)]
                        [:div "Nykyinen päällyste: " (:nykyinen_paallyste tapahtuma)]]))
