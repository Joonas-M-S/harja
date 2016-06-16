(ns harja.ui.liitteet
  "Yleisiä UI-komponentteja liitteiden lataamisen hoitamiseksi."
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<! >! timeout]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.modal :as modal]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.ikonit :as ikonit]
            [harja.tietoturva.liitteet :as t-liitteet]
            [harja.domain.oikeudet :as oikeudet]
            [harja.ui.yleiset :as yleiset])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn naytettava-liite?
  "Kertoo, voidaanko liite näyttää käyttäjälle esim. modaalissa vai onko tarkoitus tarjota puhdas latauslinkki"
  [liite]
  (zero? (.indexOf (:tyyppi liite) "image/")))

(defn liitekuva-modalissa [liite]
  [:div.liite-ikkuna
   [:img {:src (k/liite-url (:id liite))}]])

(defn liitetiedosto
  "Näyttää liitteen pikkukuvan ja nimen. Näytettävä liite avataan modalissa, muuten tarjotaan normaali latauslinkki."
  [tiedosto]
  [:div.liite
   (if (naytettava-liite? tiedosto)
     [:span
      [:img.pikkukuva.klikattava {:src (k/pikkukuva-url (:id tiedosto))
                                  :on-click #(modal/nayta!
                                              {:otsikko (str "Liite: " (:nimi tiedosto))
                                               :leveys "80%"}
                                              (liitekuva-modalissa tiedosto))}]
      [:span.liite-nimi (:nimi tiedosto)]]
     [:a.liite-linkki {:target "_blank" :href (k/liite-url (:id tiedosto))} (:nimi tiedosto)])])

(defn liite-linkki [liite teksti]
  (if (naytettava-liite? liite)
    [:a.klikattava {:title (:nimi liite)
                    :on-click #(modal/nayta!
                                {:otsikko (str "Liite: " (:nimi liite))
                                 :leveys "80%"}
                                (liitekuva-modalissa liite))}
     teksti]
    [:a.klikattava {:title (:nimi liite)
                    :href (k/liite-url (:id liite))
                    :target "_blank"}
     teksti]))

(defn liitteet-numeroina
  "Listaa liitteet numeroina. Näytettävät liitteet avataan modalissa, muuten tarjotaan normaali latauslinkki."
  [liitteet]
  [:div.liitteet-numeroina
   (map-indexed
     (fn [index liite]
       ^{:key (:id liite)}
       [:span
        [liite-linkki liite (inc index)]
        [:span " "]])
     liitteet)])

(defn liitteet-ikoneina
  "Listaa liitteet ikoneita. Näytettävät liitteet avataan modalissa, muuten tarjotaan normaali latauslinkki."
  [liitteet]
  ;; PENDING Olisipa kiva jos ikoni heijastelisi tiedoston tyyppiä :-)
  [:span.liitteet-ikoneina
   (map
     (fn [liite]
       ^{:key (:id liite)}
       [:span
        [liite-linkki liite (ikonit/file)]
        [:span " "]])
     liitteet)])

(defn liitteet-listalla [liitteet]
  [:ul.livi-alasvetolista.liitelistaus
   (doall
     (for [liite liitteet]
       ^{:key (hash liite)}
       [:li.harja-alasvetolistaitemi
        [liite-linkki liite (:nimi liite)]]))])

(defn lisaa-liite
  "Liitetiedosto (file input) komponentti yhden tiedoston lataamiselle.
  Lataa tiedoston serverille ja palauttaa callbackille tiedon onnistuneesta
  tiedoston lataamisesta.

  Optiot voi sisältää:
  grid?              Jos true, optimoidaan näytettäväksi gridissä
  nappi-teksti       Teksti, joka napissa näytetään (vakiona 'Lisää liite')
  liite-ladattu      Funktio, jota kutsutaan kun liite on ladattu onnistuneesti.
                     Parametriksi annetaan mäppi, jossa liitteen tiedot:
                     :id, :nimi, :tyyppi, :pikkukuva-url, :url"
  [urakka-id opts]
  (let [;; Ladatun tiedoston tiedot, kun lataus valmis
        tiedosto (atom nil)
        ;; Edistyminen, kun lataus on menossa (nil jos ei lataus menossa)
        edistyminen (atom nil)
        virheviesti (atom nil)]
    (fn [urakka-id {:keys [liite-ladattu nappi-teksti grid?] :as opts}]
      [:span
       (if-let [tiedosto @tiedosto]
         [liitetiedosto tiedosto]) ;; Tiedosto ladattu palvelimelle, näytetään se
       (if-let [edistyminen @edistyminen]
         [:progress {:value edistyminen :max 100}] ;; Siirto menossa, näytetään progress
         [:span.liitekomponentti
          [:div {:class (str "file-upload nappi-toissijainen " (when grid? "nappi-grid"))}
           [yleiset/ikoni-ja-teksti
            (ikonit/livicon-upload)
            (if @tiedosto
              "Vaihda liite"
              (or nappi-teksti "Lisää liite"))]
           [:input.upload
            {:type "file"
             :on-change #(let [ch (k/laheta-liite! (.-target %) urakka-id)]
                          (go
                            (loop [ed (<! ch)]
                              (if (number? ed)
                                (do (reset! edistyminen ed)
                                    (recur (<! ch)))
                                (if (and ed (not (k/virhe? ed)))
                                  (do
                                    (reset! edistyminen nil)
                                    (reset! virheviesti nil)
                                    (when liite-ladattu
                                      (liite-ladattu (reset! tiedosto ed))))
                                  (do
                                    (log "Virhe: " (pr-str ed))
                                    (reset! edistyminen nil)
                                    (reset! virheviesti (str "Liitteen lisääminen epäonnistui"
                                                             (if (:viesti ed)
                                                               (str " (" (:viesti ed) ")"))))))))))}]]
          [:div.liite-virheviesti @virheviesti]])])))

(defn liitteet
  "Listaa liitteet ja näyttää Lisää liite -napin.

  Optiot voi sisältää:
  uusi-liite-teksti               Teksti uuden liitteen lisäämisen nappiin
  uusi-liite-atom                 Atomi, johon uuden liitteen tiedot tallennetaan
  grid?                           Jos true, optimoidaan näytettäväksi gridissä"
  [urakka-id liitteet {:keys [uusi-liite-teksti uusi-liite-atom grid?]}]
  [:span
   ;; Näytä olemassaolevat liitteet
   (when (oikeudet/voi-lukea? oikeudet/urakat-liitteet urakka-id)
     (for [liite liitteet]
       ^{:key (:id liite)}
       [liitetiedosto liite]))
   ;; Uuden liitteen lähetys
   (when (oikeudet/voi-kirjoittaa? oikeudet/urakat-liitteet urakka-id)
     (when uusi-liite-atom
       [lisaa-liite urakka-id {:liite-ladattu #(reset! uusi-liite-atom %)
                               :nappi-teksti uusi-liite-teksti
                               :grid? grid?}]))])
