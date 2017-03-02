(ns harja.tiedot.urakka.yllapitokohteet.muut-kustannukset
  "Päällystyskohteiden Muut kustannukset-taulukon tiedot.

  Tässä taulukossa näytetään sanktiot jotka eivät liity mihinkään ylläpitokohteeseen,
  sekä käsin syötetyt vapaamuotoiset muut kustannukset. Sanktioita ei voi muokata tai lisätä.

  Nämä kustannukset lasketaan mukaan kustannusyhteenvetotaulukkoon käyttäen kohteet-reaktiota."
  (:require
   [reagent.core :refer [atom] :as r]
   [harja.loki :refer [log tarkkaile!]]
   [harja.tiedot.urakka :as tiedot-urakka]
   [harja.tiedot.urakka.laadunseuranta.sanktiot :as tiedot-sanktiot]
   [harja.tiedot.navigaatio :as nav]

   [cljs.core.async :refer [<! pipe chan]]
   [harja.asiakas.kommunikaatio :as k]
   [harja.pvm :as pvm]
   [clojure.string :as s]
   [clojure.set :refer [rename-keys]])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [harja.atom :refer [reaction<!]]
                   [cljs.core.async.macros :refer [go]]))

(defonce nakymassa? (atom nil)) ;; komp/lippu päivittää tätä
(defonce riveja-tallennettu-laskuri (atom 0)) ;; käytetään potkaisemaan hakureaktiota tallennuksen jälkeen

(defn hae-muiden-kustannusten-tiedot! [urakka-id sopimus-id [alkupvm loppupvm]]
  (k/post! :hae-yllapito-toteumat {:urakka urakka-id :sopimus sopimus-id :alkupvm alkupvm :loppupvm loppupvm}))

(def muiden-kustannusten-tiedot
  (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
               vuosi @tiedot-urakka/valittu-urakan-vuosi
               [valittu-sopimus-id _] @tiedot-urakka/valittu-sopimusnumero
               nakymassa? @nakymassa?
               riveja-tallennettu-laskuri @riveja-tallennettu-laskuri]
              {:nil-kun-haku-kaynnissa? true}
              (log "reaction mkt: kutsuttu")
              (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                (log "reaction mkt: paivitetaan")

                (hae-muiden-kustannusten-tiedot!
                 valittu-urakka-id valittu-sopimus-id
                 (pvm/vuoden-aikavali vuosi)))))

(def kohdistamattomien-sanktioiden-tiedot
  (reaction<! [valittu-urakka-id (:id @nav/valittu-urakka)
               vuosi @tiedot-urakka/valittu-urakan-vuosi
               [valittu-sopimus-id _] @tiedot-urakka/valittu-sopimusnumero
               nakymassa? @nakymassa?
               riveja-tallennettu-laskuri @riveja-tallennettu-laskuri]
              {:nil-kun-haku-kaynnissa? true}
              (log "reaction kst: kutsuttu")
              (when (and valittu-urakka-id valittu-sopimus-id nakymassa?)
                (log "reaction kst: paivitetaan")
                (pipe (tiedot-sanktiot/hae-urakan-sanktiot
                       valittu-urakka-id (pvm/vuoden-aikavali vuosi))
                      (chan 1 (filter #(do (log "filtteroidaan" (-> % :yllapitokohde :id nil?))
                                           (-> % :yllapitokohde :id nil?))))))))

(defn- grid-tiedot* [muut-kustannukset-tiedot kohdistamattomat-tiedot]
  (let [mk-id #(str "ypt-" (:id %))
        ks-id #(str "sanktio-" (:id %))
        ks->grid (fn [ks] {:hinta  (-> ks :summa -) ;; käännetään sanktioiden etumerkki ladattaessa (näitä ei tallenneta, ovat read-only gridissä)
                           :pvm    (-> ks :laatupoikkeama :aika)
                           :selite (-> ks :tyyppi :nimi)
                           :id     (-> ks :id)})]
    (concat
     (map #(assoc % :muokattava true :id (mk-id %)) muut-kustannukset-tiedot)
     (map #(-> % ks->grid (assoc :muokattava false :id (ks-id %))) kohdistamattomat-tiedot))))

(def grid-tiedot
  (reaction (grid-tiedot* @muiden-kustannusten-tiedot @kohdistamattomien-sanktioiden-tiedot)))

(defn- kohteet* [tiedot]
  (map #(rename-keys % {:hinta :muut-hinta})
       tiedot))

(defonce kohteet (reaction (kohteet* @grid-tiedot)))

(defn tallenna-toteuma! [toteuman-tiedot]
  (k/post! :tallenna-yllapito-toteuma toteuman-tiedot))

(defn tallenna-lomake! [urakka data-atomi grid-data]
  (let [toteuman-avaimet-gridista #(select-keys % [:id :poistettu :toteuma :alkupvm :loppupvm :selite :pvm :hinta])
        [sopimus-id sopimus-nimi] @tiedot-urakka/valittu-sopimusnumero
        ;; tulee vain ypt-id:llä olevia, koska muut eivät ole muokattavia
        palauta-ypt-id #(if (neg? %)
                          nil
                          (-> % (s/replace "ypt-" "") js/parseInt))
        grid-data-ilman-poistettuja-lisayksia (remove #(and (-> % :id neg?) (-> % :poistettu))
                                                      grid-data)]
    (go
      (mapv #(-> %
                 toteuman-avaimet-gridista
                 (assoc :urakka (:id urakka)
                        :sopimus sopimus-id)
                 (update :id palauta-ypt-id)
                 tallenna-toteuma!)
            grid-data-ilman-poistettuja-lisayksia)))
  (swap! riveja-tallennettu-laskuri inc)
  (chan))
