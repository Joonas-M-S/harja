(ns harja.views.urakka.yleiset
  "Urakan 'Yleiset' välilehti: perustiedot ja yhteyshenkilöt"
  (:require [reagent.core :refer [atom] :as reagent]
            [bootstrap :as bs]
            [harja.ui.grid :as grid]
            [harja.ui.yleiset :as yleiset]
            [harja.tiedot.urakka.yhteystiedot :as yht]
            [harja.loki :refer [log]]
            [cljs.core.async :refer [<!]]

            [cljs-time.format :as df]
            )
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [harja.ui.yleiset :refer [deftk]]))
 

(def fi-date (df/formatter "dd.MM.yyyy"))

(deftk yleiset [ur]
  [yhteyshenkilot (<! (yht/hae-urakan-yhteyshenkilot (:id ur)))
   paivystajat nil]

  (do
    (log "URAKKANI ON: " ur " ALKAA: " (:alkupvm ur) " JA LOPPUU " (:loppupvm ur))
    [:div
     [bs/panel {}
      "Yleiset tiedot"
      [yleiset/tietoja {}
       "Urakan nimi:" (:nimi ur)
       "Urakan tunnus:" (:sampoid ur)
       "Aikaväli:" (fi-date (:alkupvm ur)) " -- " (fi-date (:loppupvm ur))
       "Urakoitsija:" (:nimi (:urakoitsija ur))]]
        
     [grid/grid
      {:otsikko "Yhteyshenkilöt"}
      [{:otsikko "Rooli" :nimi :rooli :tyyppi :string}
       {:otsikko "Organisaatio" :hae #(get-in % [:organisaatio :nimi]) :tyyppi :string}
       {:otsikko "Nimi" :hae #(str (:etunimi %) " " (:sukunimi %)) :tyyppi :string}
       {:otsikko "Puhelin (virka)" :nimi :tyopuhelin :tyyppi :string}
       {:otsikko "Puhelin (gsm)" :nimi :matkapuhelin :tyyppi :string}
       {:otsikko "Sähköposti" :nimi :sahkoposti :tyyppi :email}]
      @yhteyshenkilot
      ] 
        
     [grid/grid
      {:otsikko "Päivystystiedot"}
      [{:otsikko "Rooli" :nimi :rooli :tyyppi :string}
       {:otsikko "Organisaatio" :nimi :organisaatio :tyyppi :string}
       {:otsikko "Nimi" :nimi :nimi :tyyppi :string}
       {:otsikko "Puhelin (virka)" :nimi :puhelin :tyyppi :string}
       {:otsikko "Puhelin (gsm)" :nimi :gsm :tyyppi :string} ;; mieti eri tyyppejä :puhelin / :email / jne...
       {:otsikko  "Sähköposti" :nimi :sahkoposti :tyyppin :email}]
      @paivystajat
      ]
       
     ]))



(comment
(defn yleiset
  "Yleiset välilehti"
  [ur]
  (let [paivita (fn [this]
                  (let [{:keys [yhteyshenkilot paivystajat]} (reagent/state this)]
                    (go
                      (log "haetaan urakan henkilöitä: " (:id ur))
                      (let [henkilot (<! (yht/hae-urakan-yhteyshenkilot (:id ur)))]
                        (log "urakan henkilöt: " (pr-str henkilot))
                        (reset! yhteyshenkilot henkilot ;(vec (filter #(= (:rooli %) :yhteyshenkilo)))
                                )))))]
                  
    (reagent/create-class
     {:display-name "urakka-yleiset"
      :get-initial-state
      (fn [this] {:yhteyshenkilot (atom nil)
                  :paivystajat (atom nil)})

      :component-did-mount
      (fn [this]
        (paivita this))
      :reagent-render
      (fn [ur]
        (let [{:keys [yhteyshenkilot paivystajat]} (reagent/state (reagent/current-component))]
          (log "urakka-yleiset: " (pr-str yhteyshenkilot))
          [:div
           "Urakan tunnus: foo" [:br]
           "Aikaväli: 123123" [:br]
           "Hallintayksikkö: sehän näkyy jo murupolussa" [:br]
           "Urakoitsija: Urakkapojat Oy" [:br]
       
           [grid/grid
            {:otsikko "Yhteyshenkilöt"}
            [{:otsikko "Rooli" :nimi :rooli :tyyppi :string}
             {:otsikko "Organisaatio" :hae #(get-in % [:organisaatio :nimi]) :tyyppi :string}
             {:otsikko "Nimi" :hae #(str (:etunimi %) " " (:sukunimi %)) :tyyppi :string}
             {:otsikko "Puhelin (virka)" :nimi :tyopuhelin :tyyppi :string}
             {:otsikko "Puhelin (gsm)" :nimi :matkapuhelin :tyyppi :string} ;; mieti eri tyyppejä :puhelin / :email / jne...
             {:otsikko "Sähköposti" :nimi :sahkoposti :tyyppi :email}]
            @yhteyshenkilot
            ]
       
           [grid/grid
            {:otsikko "Päivystystiedot"}
            [{:otsikko "Rooli" :nimi :rooli :tyyppi :string}
             {:otsikko "Organisaatio" :nimi :organisaatio :tyyppi :string}
             {:otsikko "Nimi" :nimi :nimi :tyyppi :string}
             {:otsikko "Puhelin (virka)" :nimi :puhelin :tyyppi :string}
             {:otsikko "Puhelin (gsm)" :nimi :gsm :tyyppi :string} ;; mieti eri tyyppejä :puhelin / :email / jne...
             {:otsikko "Sähköposti" :nimi :sahkoposti :tyyppin :email}]
            @paivystajat
            ]
       
           ]))}))))

