(ns harja.palvelin.palvelut.kulut.pdf
  (:require [harja.tyokalut.xsl-fo :as xsl-fo]))

(def ^:private border "solid 0.1mm black")

(def ^:private borders {:border-bottom border
                        :border-top    border
                        :border-left   border
                        :border-right  border})

(defn- valiotsikko-rivi [otsikko])

(defn- rivi [otsikko & sisaltosolut]
  [:fo:table-row
   (if true
     [:fo:table-cell
      [:fo:block {:margin-bottom "2mm"}
       [:fo:block {:font-weight "bold" :font-size 8} otsikko]]])
   (for [sisalto sisaltosolut]
     [:fo:table-cell
      [:fo:block sisalto]])])

(defn- taulukko [rivit]
  [:fo:table (merge borders {:table-layout "fixed"})
   [:fo:table-column {:column-width "20%"}]
   [:fo:table-column {:column-width "30%"}]
   [:fo:table-column {:column-width "30%"}]
   [:fo:table-column {:column-width "10%"}]
   [:fo:table-column {:column-width "10%"}]
   [:fo:table-body
    (for [{:keys [summa tehtavaryhma toimenpide maksuera erapaiva]} rivit]
      (rivi erapaiva toimenpide (or tehtavaryhma
                                    "Lisätyö") maksuera summa))]])

(defn kulu-pdf
  [kulut]
  (with-meta
    (xsl-fo/dokumentti
      {:margin {:left "5mm" :right "5mm" :top "5mm" :bottom "5mm"
                :body "0mm"}}
      [:fo:wrapper {:font-size 8}
       [:fo:block {:text-align "center"}
        [:fo:block {:font-weight "bold"}
         [:fo:block "MHU Laskukooste WIP"]]]
       (taulukko
         kulut)])
    {:tiedostonimi (str "kulut.pdf")}))