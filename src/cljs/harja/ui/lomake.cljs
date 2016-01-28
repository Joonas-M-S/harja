(ns harja.ui.lomake
  "Lomakeapureita"
  (:require [reagent.core :refer [atom] :as r]
            [harja.ui.validointi :as validointi]
            [harja.ui.yleiset :refer [virheen-ohje]]
            [harja.ui.kentat :refer [tee-kentta nayta-arvo atomina]]
            [harja.loki :refer [log logt tarkkaile!]]
            [harja.ui.komponentti :as komp]
            [taoensso.truss :as truss :refer-macros [have have! have?]]))

(defrecord Ryhma [otsikko optiot skeemat])

(defn ryhma [otsikko-tai-optiot & skeemat]
  (if-let [optiot (and (map? otsikko-tai-optiot) otsikko-tai-optiot)]
    (->Ryhma (:otsikko optiot)
             (merge {:ulkoasu :oletus}
                    optiot)
             skeemat)
    (->Ryhma otsikko-tai-optiot
             {:ulkoasu :oletus} skeemat)))

(defn rivi
  "Asettaa annetut skeemat vierekkäin samalle riville"
  [& skeemat]
  (->Ryhma nil {:rivi? true} skeemat))

(defn ryhma? [x]
  (instance? Ryhma x))

(defrecord ^:private Otsikko [otsikko])
(defn- otsikko? [x]
  (instance? Otsikko x))

(defn- pura-ryhmat
  "Purkaa skeemat ryhmistä yhdeksi flat listaksi, jossa ei ole nil arvoja.
Ryhmien otsikot lisätään väliin Otsikko record tyyppinä."
  [skeemat]
  (loop [acc []
         [s & skeemat] skeemat]
    (if-not s
      acc
      (cond
        (otsikko? s)
        (recur acc skeemat)

        (ryhma? s)
        (recur acc
               (concat (:skeemat s) skeemat))

        :default
        (recur (conj acc s)
               skeemat)))))

(defn- rivita
  "Rivittää kentät siten, että kaikki palstat tulee täyteen. 
  Uusi rivi alkaa kun palstat ovat täynnä, :uusi-rivi? true on annettu tai tulee uusi ryhmän otsikko."
  [skeemat]
  (loop [rivit []
         rivi []
         palstoja 0
         [s & skeemat] (remove nil? skeemat)             
         ]
    (if-not s
      (if-not (empty? rivi)
        (conj rivit rivi)
        rivit)
      (let [kentan-palstat (or (:palstoja s) 1)]
        (cond
          (and (ryhma? s) (:rivi? (:optiot s)))
          ;; Jos kyseessä on ryhmä, joka haluataan samalle riville, lisätään
          ;; ryhmän skeemat suoraan omana rivinään riveihin
          (recur (vec (concat (if (empty? rivi)
                                rivit
                                (conj rivit rivi))
                              [[(->Otsikko (:otsikko s))]
                               (with-meta
                                 (remove nil? (:skeemat s))
                                 {:rivi? true})]))
                 []
                 0
                 skeemat)
          
          (ryhma? s)
          ;; Muuten lisätään ryhmän otsikko ja jatketaan rivitystä normaalisti
          (recur rivit rivi palstoja
                 (concat [(->Otsikko (:otsikko s))] (remove nil? (:skeemat s)) skeemat))
          
          :default
          ;; Rivitä skeema
          (do (log "SKEEMA " (pr-str (select-keys s [:nimi :tyyppi :palstoja :otsikko])) ", palstoja: " palstoja)
              (if (or (otsikko? s)
                      (:uusi-rivi? s)
                      (> (+ palstoja kentan-palstat) 2))
                (do (log "SKEEMA UUSI RIVI ALKAA! ")
                    ;; Kenttä on uusi otsikko tai rivi menisi yli 2 palstan => aloitetaan uusi rivi tälle
                    (recur (if (empty? rivi)
                             rivit
                             (conj rivit rivi))
                           [s]
                           (if (otsikko? s) 0 kentan-palstat)
                           skeemat))
                ;; Mahtuu tälle riville, lisätään nykyiseen riviin
                (recur rivit
                       (conj rivi s)
                       (+ palstoja kentan-palstat)
                       skeemat))))))))


(defn kentan-vihje [{vihje :vihje}]
  (when vihje
    [:div {:class
           (str "inline-block lomake-vihje")}
     [:div.vihjeen-sisalto
      (harja.ui.ikonit/info-sign)
      (str " " vihje)]]))

(defn yleinen-huomautus
  "Yleinen huomautus, joka voidaan näyttää esim. lomakkeen tallennuksen yhteydessä"
  [teksti]
  [:div.lomake-yleinen-huomautus (harja.ui.ikonit/info-sign) (str " " teksti)])

(defn lomake-lukittu-huomautus
  ;; FIXME: siirrä jonnekin muualle, ei liity lomakkeeseen mitenkään
  [nykyinen-lukko]
  [:div.lomake-lukittu-huomautus (harja.ui.ikonit/info-sign) (str " Lomakkeen muokkaaminen on estetty, sillä toinen käyttäjä"
   (when (and (:etunimi nykyinen-lukko)
              (:sukunimi nykyinen-lukko))
     (str " (" (:etunimi nykyinen-lukko) " " (:sukunimi nykyinen-lukko) ")"))
    " muokkaa parhaillaan lomaketta. Yritä hetken kuluttua uudelleen.")])

(def +piilota-label+ #{:boolean :tierekisteriosoite})

(defn kentta
  "UI yhdelle kentälle, renderöi otsikon ja "
  [{:keys [palstoja nimi otsikko tyyppi hae fmt col-luokka yksikko pakollinen?] :as s} data atom-fn muokattava?
   muokattu? virheet varoitukset]
  (let [arvo (atom-fn s)]
    [:div.form-group {:class (str (or col-luokka
                                      (case (or palstoja 1)
                                        1 "col-xs-12 col-sm-6 col-md-5 col-lg-4"
                                        2 "col-xs-12 col-sm-12 col-md-10 col-lg-8"))
                                  (when pakollinen?
                                    " required")
                                  (when-not (empty? virheet)
                                    " sisaltaa-virheen")
                                  (when-not (empty? varoitukset)
                                    " sisaltaa-varoituksen"))}
     (when-not (+piilota-label+ tyyppi)
       [:label.control-label {:for nimi}
        [:span
         [:span.kentan-label otsikko]
         (when yksikko [:span.kentan-yksikko yksikko])]])
     (if (= tyyppi :komponentti)
       [:div.komponentti (:komponentti s)]
       (if muokattava?
         (do (have #(contains? % :tyyppi) s)
             [tee-kentta (assoc s :lomake? true) arvo])
         [:div.form-control-static
          (if fmt
            (fmt ((or hae #(get % nimi)) data))
            (nayta-arvo s arvo))]))

     (when (and muokattu?
                (not (empty? virheet)))
       [virheen-ohje virheet :virhe])
     (when (and muokattu?
                (not (empty? varoitukset)))
       [virheen-ohje varoitukset :varoitus])
     
     [kentan-vihje s]]))

(def ^:private col-luokat
  ;; PENDING: hyvin vaikea sekä 2 että 3 komponentin määrät saada alignoitua
  ;; bootstrap col-luokilla, pitäisi tehdä siten, että rivi on aina saman
  ;; määrän colleja kuin 2 palstaa ja sen sisällä on jaettu osien width 100%/(count skeemat)
  {2 "col-xs-6 col-md-5 col-lg-3"
   3 "col-xs-3 col-sm-2 col-md-3 col-lg-2"
   4 "col-xs-3"})

(defn nayta-rivi
  "UI yhdelle riville"
  [skeemat data atom-fn voi-muokata? nykyinen-fokus aseta-fokus! muokatut virheet varoitukset]
  (let [rivi? (-> skeemat meta :rivi?)
        col-luokka (when rivi?
                     (col-luokat (count skeemat)))]
    [:div.row.lomakerivi
     (doall
      (for [{:keys [nimi muokattava?] :as s} skeemat
            :let [muokattava? (and voi-muokata?
                                   (or (nil? muokattava?)
                                       (muokattava? data)))]]
        ^{:key nimi}
        [kentta (assoc s
                       :col-luokka col-luokka)
         data atom-fn muokattava?
         (get muokatut nimi)
         (get virheet nimi)
         (get varoitukset nimi)]))]))

;; FIXME: ulkoinen validointitila on huono idea, pistetään data mäppiin vain nekin

(defn lomake
  "Geneerinen lomakekomponentti, joka käyttää samaa kenttien määrittelymuotoa kuin grid.
  Ottaa kolme parametria: optiot, skeeman (vektori kenttiä) sekä datan (mäppi).

  Kenttiä voi poistaa käytöstä ehdollisesti, kaikki nil kentät jätetään näyttämättä.
  Kenttä voi olla myös ryhmä (ks. ryhma funktio), jolla on otsikko ja kentät.
  Ryhmille annetaan oma väliotsikko lomakkeelle.

  Optioissa voi olla seuraavat avaimet:

  :muokkaa!       callback, jolla kaikki muutokset tehdään, ottaa sisään uudet tiedot
                  ja tekee sille jotain (oletettavasti swap! tai reset! atomille,
                  joka sisältää lomakkeen tiedot)

  :footer         Komponentti, joka asetetaan lomakkeen footer sijaintiin, yleensä
                  submit nappi tms.

  :footer-fn      vaihtoehto :footer'lle, jolle annetaan footerin muodostava funktio
                  funktiolle annetaan virheet ja varoitukset parametrina 

  :virheet        atomi, joka sisältää mäpin kentän nimestä virheisiin, jos tätä ei
                  anneta, lomake luo sisäisesti uuden atomin.
  :varoitukset    atomi, joka sisältää mäpin kentän nimestä varoituksiin, jos tätä ei
                  anneta, lomake luo sisäisesti uuden atomin.
  :voi-muokata?   voiko lomaketta muokata, oletuksena true
  "

  [{:keys [otsikko muokkaa! luokka footer footer-fn virheet varoitukset voi-muokata?] :as opts} skeema
   {muokatut ::muokatut
    fokus ::fokus
    virheet ::virheet
    varoitukset ::varoitukset
    :as  data}]
  (let [voi-muokata? (if (some? voi-muokata?)
                       voi-muokata?
                       true)
        muokkaa-kenttaa-fn (fn [nimi]
                             (fn [uudet-tiedot]
                               (log "muokkaa kenttää " (pr-str nimi))
                               (let [kaikki-skeemat (pura-ryhmat skeema)
                                     kaikki-virheet (validointi/validoi-rivi nil uudet-tiedot kaikki-skeemat :validoi)
                                     kaikki-varoitukset (validointi/validoi-rivi nil uudet-tiedot kaikki-skeemat :varoita)]
                                 (log "UUDET-TIEDOT: " (pr-str uudet-tiedot))
                                 (muokkaa! (assoc uudet-tiedot
                                                  ::muokatut (conj (or muokatut #{}) nimi)
                                                  ::virheet kaikki-virheet
                                                  ::varoitukset kaikki-varoitukset)))))]
    [:form.lomake
     (when otsikko
       [:h3.lomake-otsikko otsikko])
     (map-indexed (fn [i skeemat]
                    (log "SKEEMA RIVI " (count skeemat) " itemiä, " (pr-str (map :nimi skeemat)))
                    (let [otsikko (when (otsikko? (first skeemat))
                                    (first skeemat))
                          skeemat (if otsikko
                                    (rest skeemat)
                                    skeemat)
                          rivi-ui [nayta-rivi skeemat
                                   data
                                   #(atomina % data (muokkaa-kenttaa-fn (:nimi %)))
                                   voi-muokata?
                                   fokus
                                   #(swap! data assoc ::fokus %)
                                   muokatut
                                   virheet
                                   varoitukset]]
                      (if otsikko
                        ^{:key (:otsikko otsikko)}
                        [:span
                         [:h3.lomake-ryhman-otsikko (:otsikko otsikko)]
                         rivi-ui]
                        ^{:key i}
                        rivi-ui)))
                  (rivita skeema))
     
     (when-let [footer (if footer-fn
                         (footer-fn virheet varoitukset)
                         footer)]
       [:div.lomake-footer.row
        [:div.col-md-12 footer]])]))
