(ns harja.views.urakka.aikataulu
  "Ylläpidon urakoiden aikataulunäkymä"
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log logt]]
            [harja.ui.komponentti :as komp]
            [harja.tiedot.urakka.aikataulu :as tiedot]
            [harja.ui.grid :as grid]
            [cljs.core.async :refer [<!]]
            [harja.tiedot.urakka :as u]
            [harja.tiedot.navigaatio :as nav]
            [harja.pvm :as pvm]
            [harja.domain.oikeudet :as oikeudet]
            [harja.ui.modal :as modal]
            [harja.ui.yleiset :refer [vihje]]
            [harja.ui.lomake :as lomake]
            [cljs-time.core :as t]
            [harja.ui.napit :as napit]
            [harja.tiedot.istunto :as istunto]
            [harja.domain.paallystysilmoitus :as pot]
            [harja.ui.yleiset :as yleiset]
            [harja.ui.dom :as dom]
            [harja.tyokalut.functor :refer [fmap]]
            [harja.domain.yllapitokohde :as yllapitokohteet-domain]
            [harja.tiedot.urakka.yllapito :as yllapito-tiedot]
            [harja.ui.viesti :as viesti]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.kumousboksi :as kumousboksi]
            [harja.tiedot.urakka.siirtymat :as siirtymat]
            [harja.views.urakka.valinnat :as valinnat]
            [harja.views.urakka.tarkka-aikataulu :as tarkka-aikataulu]
            [harja.ui.aikajana :as aikajana]
            [harja.domain.aikataulu :as aikataulu]
            [harja.ui.upotettu-raportti :as upotettu-raportti]
            [harja.tiedot.raportit :as raportit]
            [harja.ui.kentat :as kentat]
            [harja.views.urakka.yllapitokohteet :as yllapitokohteet-view]
            [harja.views.urakka.yllapitokohteet.yhteyshenkilot :as yllapito-yhteyshenkilot]
            [harja.ui.leijuke :as leijuke])
  (:require-macros [reagent.ratom :refer [reaction run!]]
                   [cljs.core.async.macros :refer [go]]))

(defn tiemerkintavalmius-modal
  "Modaali, jossa joko merkitään kohde valmiiksi tiemerkintään tai perutaan aiemmin annettu valmius."
  [data]
  (let [{kohde-id :kohde-id kohde-nimi :kohde-nimi
         urakka-id :urakka-id vuosi :vuosi} data
        valmis-tiemerkintaan-lomake? (= :valmis-tiemerkintaan (:valittu-lomake data))
        valmis-tallennettavaksi? (if valmis-tiemerkintaan-lomake?
                                   (some? (:valmis-tiemerkintaan (:lomakedata data)))
                                   true)]
    [modal/modal
     {:otsikko (if valmis-tiemerkintaan-lomake?
                 (str "Kohteen " kohde-nimi " merkitseminen valmiiksi tiemerkintään")
                 (str "Kohteen " kohde-nimi " tiemerkintävalmiuden peruminen"))
      :luokka "merkitse-valmiiksi-tiemerkintaan"
      :nakyvissa? (:nakyvissa? data)
      :sulje-fn #(swap! tiedot/modal-data assoc :nakyvissa? false)
      :footer [:div
               [napit/peruuta
                (if valmis-tiemerkintaan-lomake?
                  "Peruuta"
                  "Älä perukaan")
                #(swap! tiedot/modal-data assoc :nakyvissa? false)]

               [napit/palvelinkutsu-nappi
                (if valmis-tiemerkintaan-lomake?
                  "Merkitse"
                  "Vahvista peruutus")
                #(do (log "[AIKATAULU] Merkitään kohde valmiiksi tiemerkintää")
                     (tiedot/merkitse-kohde-valmiiksi-tiemerkintaan
                       {:kohde-id kohde-id
                        :tiemerkintapvm (:valmis-tiemerkintaan (:lomakedata data))
                        :kopio-itselle? (:kopio-itselle? (:lomakedata data))
                        :saate (:saate (:lomakedata data))
                        :urakka-id urakka-id
                        :sopimus-id (first @u/valittu-sopimusnumero)
                        :vuosi vuosi}))
                {:disabled (not valmis-tallennettavaksi?)
                 :luokka "nappi-myonteinen"
                 :ikoni (ikonit/check)
                 :kun-onnistuu (fn [vastaus]
                                 (log "[AIKATAULU] Kohde merkitty valmiiksi tiemerkintää")
                                 (reset! tiedot/aikataulurivit vastaus)
                                 (swap! tiedot/modal-data assoc :nakyvissa? false))}]]}
     [:div
      [vihje (if valmis-tiemerkintaan-lomake?
               "Päivämäärän asettamisesta lähetetään sähköpostilla tieto tiemerkintäurakan urakanvalvojalle, rakennuttajakonsultille ja vastuuhenkilölle."
               "Kohteen tiemerkintävalmiuden perumisesta lähetetään sähköpostilla tieto tiemerkintäurakan urakanvalvojalle, rakennuttajakonsultille ja vastuuhenkilölle.")]
      [lomake/lomake {:otsikko ""
                      :muokkaa! (fn [uusi-data]
                                  (reset! tiedot/modal-data (merge data {:lomakedata uusi-data})))}
       [(when valmis-tiemerkintaan-lomake?
          {:otsikko "Tiemerkinnän saa aloittaa"
           :nimi :valmis-tiemerkintaan :pakollinen? true :tyyppi :pvm})
        {:otsikko "Vapaaehtoinen saateviesti joka liitetään sähköpostiin"
         :koko [90 8]
         :nimi :saate :palstoja 3 :tyyppi :text}
        {:teksti "Lähetä sähköpostiini kopio viestistä"
         :nayta-rivina? true :palstoja 3
         :nimi :kopio-itselle? :tyyppi :checkbox}]

       (:lomakedata data)]]]))

(defn- paallystys-aloitettu-validointi
  "Validoinnit päällystys aloitettu -kentälle"
  [optiot]
  (as-> [[:pvm-kentan-jalkeen :aikataulu-kohde-alku
          "Päällystys ei voi alkaa ennen kohteen aloitusta."]] validointi

        ;; Päällystysnäkymässä validoidaan, että alku on annettu
        (if (= (:nakyma optiot) :paallystys)
          (conj validointi
                [:toinen-arvo-annettu-ensin :aikataulu-kohde-alku
                 "Päällystystä ei voi merkitä alkaneeksi ennen kohteen aloitusta."])
          validointi)))

(defn- oikeudet
  "Tarkistaa aikataulunäkymän tarvitsemat oikeudet"
  [urakka-id]
  (let [saa-muokata?
        (oikeudet/voi-kirjoittaa? oikeudet/urakat-aikataulu urakka-id)

        saa-asettaa-valmis-takarajan?
        (oikeudet/on-muu-oikeus? "TM-takaraja"
                                 oikeudet/urakat-aikataulu
                                 urakka-id
                                 @istunto/kayttaja)

        saa-merkita-valmiiksi?
        (oikeudet/on-muu-oikeus? "TM-valmis"
                                 oikeudet/urakat-aikataulu
                                 urakka-id
                                 @istunto/kayttaja)]
    {:saa-muokata? saa-muokata?
     :saa-asettaa-valmis-takarajan? saa-asettaa-valmis-takarajan?
     :saa-merkita-valmiiksi? saa-merkita-valmiiksi?
     :voi-tallentaa? (or saa-muokata?
                         saa-merkita-valmiiksi?
                         saa-asettaa-valmis-takarajan?)}))


(defn- otsikoi-aikataulurivit
  "Lisää väliotsikot valmiille, keskeneräisille ja aloittamatta oleville kohteille."
  [{:keys [valmis kesken aloittamatta] :as luokitellut-rivit}]
  (concat (when-not (empty? valmis)
            (into [(grid/otsikko "Valmiit kohteet")]
                  valmis))
          (when-not (empty? kesken)
            (into [(grid/otsikko "Keskeneräiset kohteet")]
                  kesken))
          (when-not (empty? aloittamatta)
            (into [(grid/otsikko "Aloittamatta olevat kohteet")]
                  aloittamatta))))

(defn valinnat [ur]
  (let [{aikajana? :nayta-aikajana?
         jarjestys :jarjestys
         :as valinnat} @tiedot/valinnat]
    [:span.aikataulu-valinnat
     [valinnat/urakan-vuosi ur]
     [valinnat/yllapitokohteen-kohdenumero yllapito-tiedot/kohdenumero]
     [valinnat/tienumero yllapito-tiedot/tienumero]

     [yleiset/pudotusvalikko
      "Järjestä kohteet"
      {:valinta jarjestys
       :valitse-fn tiedot/jarjesta-kohteet!
       :format-fn {:aika "Aloistusajan mukaan"
                   :kohdenumero "Kohdenumeron mukaan"
                   :tr "Tieosoitteen mukaan"}}
      [:aika :kohdenumero :tr]]

     [kentat/tee-otsikollinen-kentta
      {:otsikko "Aikajana"
       :luokka "label-ja-kentta-puolikas"
       :kentta-params {:tyyppi :toggle
                       :paalle-teksti "Näytä aikajana"
                       :pois-teksti "Piilota aikajana"
                       :toggle! tiedot/toggle-nayta-aikajana!}
       :arvo-atom tiedot/nayta-aikajana?}]
     [kentat/tee-otsikollinen-kentta
      {:otsikko "Aikajanan asetukset"
       :luokka "label-ja-kentta-puolikas"
       :kentta-params {:tyyppi :checkbox
                       :teksti "Näytä tarkka aikataulu"}
       :arvo-atom tiedot/nayta-tarkka-aikajana?}]

     [upotettu-raportti/raportin-vientimuodot
      (raportit/urakkaraportin-parametrit (:id ur) :yllapidon-aikataulu
                                          {:jarjestys jarjestys
                                           :nayta-tarkka-aikajana? @tiedot/nayta-tarkka-aikajana?})]]))

(defn- nayta-yhteystiedot?
  [rivi nakyma]
  (case nakyma
    :paallystys
    (:suorittava-tiemerkintaurakka rivi)

    true))

(defn visuaalinen-aikataulu
  [{:keys [urakka-id sopimus-id aikataulurivit urakkatyyppi aikajana? optiot
           vuosi voi-muokata-paallystys? voi-muokata-tiemerkinta?]}]
  (when aikajana?
    [:div
     [kumousboksi/kumousboksi {:nakyvissa? (kumousboksi/ehdotetaan-kumamomista?)
                               :nakyvissa-sijainti {:top "250px" :right 0 :bottom "auto" :left "auto"}
                               :piilossa-sijainti {:top "250px" :right "-170px" :bottom "auto" :left "auto"}
                               :kumoa-fn #(kumousboksi/kumoa-muutos! (fn [edellinen-tila kumottu-fn]
                                                                       (tiedot/tallenna-aikataulu
                                                                         urakka-id sopimus-id vuosi edellinen-tila
                                                                         (fn [vastaus]
                                                                           (reset! tiedot/aikataulurivit vastaus)
                                                                           (kumottu-fn)))))
                               :sulje-fn kumousboksi/ala-ehdota-kumoamista!}]
     [leijuke/otsikko-ja-vihjeleijuke "Aikajana"
      {:otsikko "Visuaalisen muokkauksen ohje"}
      [leijuke/multipage-vihjesisalto
       [:div
        [:h6 "Aikajanan alun / lopun venytys"]
        [:figure
         [:img {:src "images/yllapidon_aikataulu_visuaalisen_muokkauksen_ohje_raahaus.gif"
                ;; Kuva ei lataudu heti -> leijukkeen korkeus määrittyy väärin -> avautumissuunta määrittyy väärin -> asetetaan height
                :style {:height "200px"}}]
         [:figcaption
          [:p "Tartu hiiren kursorilla kiinni janan alusta tai lopusta, raahaa eteen- tai taaksepäin pitämällä nappia pohjassa ja päästämällä irti. Muutos tallennetaan heti."]]]]
       [:div
        [:h6 "Aikajanan siirtäminen"]
        [:figure
         [:img {:src "images/yllapidon_aikataulu_visuaalisen_muokkauksen_ohje_raahaus2.gif"
                :style {:height "200px"}}]
         [:figcaption
          [:p "Tartu hiiren kursorilla kiinni janan keskeltä, raahaa eteen- tai taaksepäin pitämällä nappia pohjassa ja päästämällä irti. Muutos tallennetaan heti."]]]]
       [:div
        [:h6 "Usean aikajanan siirtäminen"]
        [:figure
         [:img {:src "images/yllapidon_aikataulu_visuaalisen_muokkauksen_ohje_raahaus3.gif"
                :style {:height "200px"}}]
         [:figcaption
          [:p "Paina CTRL-painike pohjaan ja klikkaa aikajanaa valitakseksi sen. Siirrä aikajanaa normaalisti, jolloin kaikki aikajanat liikkuvat samaan suuntaan yhtä paljon."]
          [:p "Voit perua janan valinnan CTRL-klikkaamalla sitä uudestaan. Voit perua kaikkien janojen valinnan klikkaamalla tyhjään alueeseen. Valinnat poistuvat myös sivua vaihtamalla."]]]]
       [:div
        [:h6 "Usean aikajanan alun / lopun venytys"]
        [:figure
         [:img {:src "images/yllapidon_aikataulu_visuaalisen_muokkauksen_ohje_raahaus4.gif"
                :style {:height "200px"}}]
         [:figcaption
          [:p "Paina CTRL-painike pohjaan ja klikkaa aikajanaa valitakseksi sen. Venytä aikajanaa normaalisti alusta tai lopusta, jolloin kaikki aikajanat venyvät samaan suuntaan yhtä paljon."]]]]]]
     [aikajana/aikajana
      {:muuta! (fn [drag]
                 (go (let [paivitetty-aikataulu (aikataulu/raahauksessa-paivitetyt-aikataulurivit aikataulurivit drag)
                           paivitetyt-aikataulu-idt (set (map :id paivitetty-aikataulu))
                           paivitettyjen-vanha-tila (filter #(paivitetyt-aikataulu-idt (:id %)) @tiedot/aikataulurivit)]

                       (if (aikataulu/aikataulu-validi? paivitetty-aikataulu)
                         (when (not (empty? paivitetty-aikataulu)) ; Tallenna ja ehdota kumoamista vain jos muutoksia
                           (<! (tiedot/tallenna-aikataulu urakka-id sopimus-id vuosi paivitetty-aikataulu
                                                          (fn [vastaus]
                                                            (reset! tiedot/aikataulurivit vastaus)
                                                            (kumousboksi/ehdota-kumoamista! paivitettyjen-vanha-tila)))))
                         (viesti/nayta! "Virheellistä päällystysajankohtaa ei voida tallentaa!" :danger)))))}
      (map #(aikataulu/aikataulurivi-jana % {:nakyma (:nakyma optiot)
                                             :urakka-id urakka-id
                                             :voi-muokata-paallystys? voi-muokata-paallystys?
                                             :voi-muokata-tiemerkinta? voi-muokata-tiemerkinta?
                                             :nayta-tarkka-aikajana? @tiedot/nayta-tarkka-aikajana?})
           aikataulurivit)]]))

(defn aikataulu-grid
  [{:keys [urakka-id sopimus-id aikataulurivit urakkatyyppi
           vuosi voi-muokata-paallystys? voi-muokata-tiemerkinta?
           voi-tallentaa? saa-muokata? optiot saa-asettaa-valmis-takarajan?
           saa-merkita-valmiiksi? voi-muokata-paallystys? voi-muokata-tiemerkinta?]}]
  (let [otsikoidut-aikataulurivit (if (= :aika (:jarjestys @tiedot/valinnat))
                                    (otsikoi-aikataulurivit
                                      (tiedot/aikataulurivit-valmiuden-mukaan aikataulurivit urakkatyyppi))
                                    aikataulurivit)]
    [grid/grid
     {:otsikko [:span
                "Kohteiden aikataulu"
                [yllapitokohteet-view/vasta-muokatut-lihavoitu]]
      :voi-poistaa? (constantly false)
      :voi-lisata? false
      :piilota-toiminnot? true
      :salli-valiotsikoiden-piilotus? true
      :tyhja (if (nil? @tiedot/aikataulurivit)
               [yleiset/ajax-loader "Haetaan kohteita..."] "Ei kohteita")
      :ennen-muokkausta (fn []
                          (kumousboksi/ala-ehdota-kumoamista!))
      :tallenna (if voi-tallentaa?
                  #(tiedot/tallenna-aikataulu urakka-id sopimus-id vuosi %
                                              (fn [vastaus] (reset! tiedot/aikataulurivit vastaus)))
                  :ei-mahdollinen)
      :vetolaatikot (into {}
                          (map (juxt :id
                                     (fn [rivi]
                                       [tarkka-aikataulu/tarkka-aikataulu
                                        {:rivi rivi
                                         :vuosi vuosi
                                         :nakyma (:nakyma optiot)
                                         :voi-muokata-paallystys? (voi-muokata-paallystys?)
                                         :voi-muokata-tiemerkinta? (voi-muokata-tiemerkinta? rivi)
                                         :urakka-id urakka-id
                                         :sopimus-id sopimus-id}]))
                               aikataulurivit))}
     [{:tyyppi :vetolaatikon-tila :leveys 2}
      {:otsikko "Koh\u00ADde\u00ADnu\u00ADme\u00ADro" :leveys 3 :nimi :kohdenumero :tyyppi :string
       :pituus-max 128 :muokattava? voi-muokata-paallystys?}
      {:otsikko "Koh\u00ADteen nimi" :leveys 9 :nimi :nimi :tyyppi :string :pituus-max 128
       :muokattava? voi-muokata-paallystys?}
      {:otsikko "Tie\u00ADnu\u00ADme\u00ADro" :nimi :tr-numero
       :tyyppi :positiivinen-numero :leveys 3 :tasaa :oikea
       :muokattava? (constantly false)}
      {:otsikko "Ajo\u00ADrata"
       :nimi :tr-ajorata
       :muokattava? (constantly false)
       :tyyppi :string :tasaa :oikea
       :fmt #(pot/arvo-koodilla pot/+ajoradat-numerona+ %)
       :leveys 3}
      {:otsikko "Kais\u00ADta"
       :muokattava? (constantly false)
       :nimi :tr-kaista
       :tyyppi :string
       :tasaa :oikea
       :fmt #(pot/arvo-koodilla pot/+kaistat+ %)
       :leveys 3}
      {:otsikko "Aosa" :nimi :tr-alkuosa :leveys 3
       :tyyppi :positiivinen-numero
       :tasaa :oikea
       :muokattava? (constantly false)}
      {:otsikko "Aet" :nimi :tr-alkuetaisyys :leveys 3
       :tyyppi :positiivinen-numero
       :tasaa :oikea
       :muokattava? (constantly false)}
      {:otsikko "Losa" :nimi :tr-loppuosa :leveys 3
       :tyyppi :positiivinen-numero
       :tasaa :oikea
       :muokattava? (constantly false)}
      {:otsikko "Let" :nimi :tr-loppuetaisyys :leveys 3
       :tyyppi :positiivinen-numero
       :tasaa :oikea
       :muokattava? (constantly false)}
      {:otsikko "YP-lk"
       :nimi :yllapitoluokka :leveys 4 :tyyppi :string
       :fmt yllapitokohteet-domain/yllapitoluokkanumero->lyhyt-nimi
       :muokattava? (constantly false)}
      (when (= (:nakyma optiot) :paallystys) ;; Asiakkaan mukaan ei tarvi näyttää tiemerkkareille
        {:otsikko "Koh\u00ADteen aloi\u00ADtus" :leveys 8 :nimi :aikataulu-kohde-alku
         :tyyppi :pvm :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
         :muokattava? voi-muokata-paallystys?})
      {:otsikko "Pääl\u00ADlystyk\u00ADsen aloi\u00ADtus" :leveys 8 :nimi :aikataulu-paallystys-alku
       :tyyppi :pvm :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :muokattava? voi-muokata-paallystys?
       :pvm-tyhjana #(:aikataulu-kohde-alku %)
       :validoi (paallystys-aloitettu-validointi optiot)}
      {:otsikko "Pääl\u00ADlystyk\u00ADsen lope\u00ADtus" :leveys 8 :nimi :aikataulu-paallystys-loppu
       :tyyppi :pvm :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :pvm-tyhjana #(:aikataulu-paallystys-alku %)
       :muokattava? voi-muokata-paallystys?
       :validoi [[:toinen-arvo-annettu-ensin :aikataulu-paallystys-alku
                  "Päällystystä ei ole merkitty aloitetuksi."]
                 [:pvm-kentan-jalkeen :aikataulu-paallystys-alku
                  "Valmistuminen ei voi olla ennen aloitusta."]
                 [:ei-tyhja-jos-toinen-arvo-annettu :valmis-tiemerkintaan
                  "Arvoa ei voi poistaa, koska kohde on merkitty valmiiksi tiemerkintään"]
                 [:ei-tyhja-jos-toinen-arvo-annettu :aikataulu-paallystys-alku
                  "Anna päällystyksen valmistumisen aika tai aika-arvio."]]}
      (when (= (:nakyma optiot) :paallystys)
        {:otsikko "Tie\u00ADmer\u00ADkin\u00ADnän suo\u00ADrit\u00ADta\u00ADva u\u00ADrak\u00ADka"
         :leveys 10 :nimi :suorittava-tiemerkintaurakka
         :tyyppi :valinta
         :fmt (fn [arvo]
                (:nimi (some
                         #(when (= (:id %) arvo) %)
                         @tiedot/tiemerkinnan-suorittavat-urakat)))
         :valinta-arvo :id
         :valinta-nayta #(if % (:nimi %) "- Valitse urakka -")
         :valinnat @tiedot/tiemerkinnan-suorittavat-urakat
         :nayta-ryhmat [:sama-hallintayksikko :eri-hallintayksikko]
         :ryhmittely #(if (= (:hallintayksikko %) (:id (:hallintayksikko urakka)))
                        :sama-hallintayksikko
                        :eri-hallintayksikko)
         :ryhman-otsikko #(case %
                            :sama-hallintayksikko "Hallintayksikön tiemerkintäurakat"
                            :eri-hallintayksikko "Muut tiemerkintäurakat")
         :muokattava? (fn [rivi] (and saa-muokata? (:tiemerkintaurakan-voi-vaihtaa? rivi)))})
      (when (= (:nakyma optiot) :tiemerkinta)
        {:otsikko "Pääl\u00ADlys\u00ADtys\u00ADurak\u00ADka"
         :leveys 13
         :nimi :paallystysurakka
         :tyyppi :string})
      {:otsikko "Yh\u00ADte\u00ADys\u00ADtie\u00ADdot"
       :leveys 4
       :nimi :yhteystiedot
       :tasaa :keskita
       :tyyppi :komponentti
       :komponentti (fn [rivi]
                      [napit/yleinen-toissijainen ""
                       #(yllapito-yhteyshenkilot/nayta-yhteyshenkilot-modal!
                          (:id rivi)
                          (case (:nakyma optiot)
                            :tiemerkinta :paallystys
                            :paallystys :tiemerkinta))
                       {:disabled (not (nayta-yhteystiedot? rivi (:nakyma optiot)))
                        :ikoni (ikonit/user)
                        :luokka "btn-xs"}])}
      {:otsikko "Val\u00ADmis tie\u00ADmerkin\u00ADtään" :leveys 10
       :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :pvm-tyhjana #(:aikataulu-paallystys-loppu %)
       :nimi :valmis-tiemerkintaan :tyyppi :komponentti :muokattava? (constantly saa-muokata?)
       :komponentti (fn [rivi {:keys [muokataan?]}]
                      (let [paallystys-valmis? (some? (:aikataulu-paallystys-loppu rivi))
                            suorittava-urakka-annettu? (some? (:suorittava-tiemerkintaurakka rivi))
                            modalin-params {:kohde-id (:id rivi)
                                            :kohde-nimi (:nimi rivi)
                                            :urakka-id urakka-id
                                            :vuosi vuosi
                                            :paallystys-valmis? (some? (:aikataulu-paallystys-loppu rivi))
                                            :suorittava-urakka-annettu? (some? (:suorittava-tiemerkintaurakka rivi))
                                            :lomakedata {:kopio-itselle? true}}]
                        ;; Jos ei olla päällystyksessä, read only
                        (if-not (= (:nakyma optiot) :paallystys)
                          (if (:valmis-tiemerkintaan rivi)
                            [:span (pvm/pvm-ilman-samaa-vuotta (:valmis-tiemerkintaan rivi) vuosi)]
                            [:span "Ei"])
                          ;; Jos päällystyksessä, sopivilla oikeuksilla saa asettaa tai perua valmiuden
                          (if muokataan?
                            [:div (pvm/pvm-ilman-samaa-vuotta (:valmis-tiemerkintaan rivi) vuosi)]
                            [:div {:title (cond (not paallystys-valmis?) "Päällystys ei ole valmis."
                                                (not suorittava-urakka-annettu?) "Tiemerkinnän suorittava urakka puuttuu."
                                                :default nil)}

                             (grid/arvo-ja-nappi
                               {:sisalto (cond (not voi-muokata-paallystys?) :pelkka-arvo
                                               (not (:valmis-tiemerkintaan rivi)) :pelkka-nappi
                                               :default :arvo-ja-nappi)
                                :pelkka-nappi-teksti "Aseta päivä\u00ADmäärä"
                                :pelkka-nappi-toiminto-fn #(reset! tiedot/modal-data (merge modalin-params
                                                                                            {:nakyvissa? true
                                                                                             :valittu-lomake :valmis-tiemerkintaan}))
                                :arvo-ja-nappi-napin-teksti "Peru"
                                :arvo-ja-nappi-toiminto-fn #(reset! tiedot/modal-data (merge modalin-params
                                                                                             {:nakyvissa? true
                                                                                              :valittu-lomake :peru-valmius-tiemerkintaan}))
                                :nappi-optiot {:disabled (or
                                                           (not paallystys-valmis?)
                                                           (not suorittava-urakka-annettu?))}
                                :arvo (pvm/pvm-opt (:valmis-tiemerkintaan rivi))})]))))}
      {:otsikko "Tie\u00ADmerkin\u00ADtä val\u00ADmis vii\u00ADmeis\u00ADtään"
       :leveys 6 :nimi :aikataulu-tiemerkinta-takaraja :tyyppi :pvm
       :pvm-tyhjana #(:aikataulu-paallystys-loppu %)
       :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :muokattava? (fn [rivi]
                      (and saa-asettaa-valmis-takarajan?
                           (:valmis-tiemerkintaan rivi)))}
      {:otsikko "Tiemer\u00ADkinnän aloi\u00ADtus"
       :leveys 6 :nimi :aikataulu-tiemerkinta-alku :tyyppi :pvm
       :pvm-tyhjana #(:aikataulu-paallystys-loppu %)
       :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :muokattava? voi-muokata-tiemerkinta?}
      {:otsikko "Tiemer\u00ADkinnän lope\u00ADtus"
       :leveys 6 :nimi :aikataulu-tiemerkinta-loppu :tyyppi :pvm
       :pvm-tyhjana #(:aikataulu-tiemerkinta-alku %)
       :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :muokattava? voi-muokata-tiemerkinta?
       :validoi [[:toinen-arvo-annettu-ensin :aikataulu-tiemerkinta-alku
                  "Tiemerkintää ei ole merkitty aloitetuksi."]
                 [:pvm-kentan-jalkeen :aikataulu-tiemerkinta-alku
                  "Valmistuminen ei voi olla ennen aloitusta."]
                 [:ei-tyhja-jos-toinen-arvo-annettu :aikataulu-tiemerkinta-alku
                  "Anna tiemerkinnän valmistumisen aika tai aika-arvio."]]}
      {:otsikko "Pääl\u00ADlystys\u00ADkoh\u00ADde val\u00ADmis" :leveys 6 :nimi :aikataulu-kohde-valmis :tyyppi :pvm
       :fmt #(pvm/pvm-ilman-samaa-vuotta % vuosi)
       :muokattava? voi-muokata-paallystys?
       :pvm-tyhjana #(:aikataulu-paallystys-loppu %)
       :validoi [[:pvm-kentan-jalkeen :aikataulu-kohde-alku
                  "Kohde ei voi olla valmis ennen kuin se on aloitettu."]]}

      (when (istunto/ominaisuus-kaytossa? :tietyoilmoitukset)
        {:otsikko "Tie\u00ADtyö\u00ADilmoi\u00ADtus"
         :leveys 6
         :nimi :tietyoilmoitus
         :tyyppi :komponentti
         :komponentti (fn [{tietyoilmoitus-id :tietyoilmoitus-id :as kohde}]
                        [:button.nappi-toissijainen.nappi-grid
                         {:on-click #(siirtymat/avaa-tietyoilmoitus kohde)}
                         (if tietyoilmoitus-id
                           [ikonit/ikoni-ja-teksti (ikonit/livicon-eye) " Avaa"]
                           [ikonit/ikoni-ja-teksti (ikonit/livicon-plus) " Lisää"])])})]
     (yllapitokohteet-domain/lihavoi-vasta-muokatut otsikoidut-aikataulurivit)]))

(defn aikataulu
  [urakka optiot]
  (komp/luo
    (komp/lippu tiedot/aikataulu-nakymassa?)
    (fn [urakka optiot]
      (let [{urakka-id :id :as ur} @nav/valittu-urakka
            sopimus-id (first @u/valittu-sopimusnumero)
            aikataulurivit @tiedot/aikataulurivit-suodatettu-jarjestetty
            urakkatyyppi (:tyyppi urakka)
            vuosi @u/valittu-urakan-vuosi
            {:keys [voi-tallentaa? saa-muokata?
                    saa-asettaa-valmis-takarajan?
                    saa-merkita-valmiiksi?]} (oikeudet urakka-id)

            voi-muokata-paallystys? #(boolean (and (= (:nakyma optiot) :paallystys)
                                                   saa-muokata?))
            voi-muokata-tiemerkinta? (fn [rivi] (boolean (and (= (:nakyma optiot) :tiemerkinta)
                                                              saa-merkita-valmiiksi?
                                                              (:valmis-tiemerkintaan rivi))))
            aikajana? (:nayta-aikajana? @tiedot/valinnat)]
        [:div.aikataulu
         [valinnat ur]
         [visuaalinen-aikataulu {:urakka-id urakka-id
                                 :aikajana? aikajana?
                                 :sopimus-id sopimus-id
                                 :aikataulurivit aikataulurivit
                                 :urakkatyyppi urakkatyyppi
                                 :vuosi vuosi
                                 :optiot optiot
                                 :voi-muokata-paallystys? voi-muokata-paallystys?
                                 :voi-muokata-tiemerkinta? voi-muokata-tiemerkinta?}]
         [aikataulu-grid {:urakka-id urakka-id
                          :sopimus-id sopimus-id
                          :aikataulurivit aikataulurivit
                          :urakkatyyppi urakkatyyppi
                          :vuosi vuosi
                          :voi-tallentaa? voi-tallentaa?
                          :saa-muokata? saa-muokata?
                          :optiot optiot
                          :saa-asettaa-valmis-takarajan? saa-asettaa-valmis-takarajan?
                          :saa-merkita-valmiiksi? saa-merkita-valmiiksi?
                          :voi-muokata-paallystys? voi-muokata-paallystys?
                          :voi-muokata-tiemerkinta? voi-muokata-tiemerkinta?}]

         (if (= (:nakyma optiot) :tiemerkinta)
           [vihje "Tänään tai aiemmin valmistuneista kohteista lähetetään heti sähköpostilla tieto päällystysurakan urakanvalvojalle, rakennuttajakonsultille ja vastuuhenkilölle. Tulevaisuuteen merkityistä kohteista lähetetään sähköposti valmistumispäivänä."])
         [tiemerkintavalmius-modal @tiedot/modal-data]]))))