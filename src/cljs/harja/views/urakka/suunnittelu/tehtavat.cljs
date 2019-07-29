(ns harja.views.urakka.suunnittelu.tehtavat
  (:require [reagent.core :as r]
            [tuck.core :as tuck]
            [harja.ui.debug :as debug]
            [harja.tiedot.urakka.urakka :as tila]
            [harja.tiedot.urakka.suunnittelu.mhu-tehtavat :as t]
            [harja.ui.taulukko.taulukko :as taulukko]
            [harja.ui.taulukko.jana :as jana]
            [harja.ui.taulukko.osa :as osa]
            [harja.ui.komponentti :as komp]
            [harja.ui.yleiset :as yleiset]))

(defn luo-taulukon-tehtavat
  [e! tehtavat]
  (map (fn [{:keys [id tehtavaryhmatyyppi maara nimi piillotettu? vanhempi]}]
         (with-meta (jana/->Rivi id
                                 [(if (= tehtavaryhmatyyppi "alitaso")
                                    (osa/->Teksti (str id "-tehtava") nimi nil)
                                    (osa/luo-tilallinen-laajenna (str id "-laajenna") nimi #(e! (t/->LaajennaSoluaKlikattu %1 %2)) {}))
                                  (osa/->Teksti (str id "-maara") maara nil)]
                                 (if piillotettu?
                                   #{"piillotettu"}
                                   #{}))
                    {:vanhempi vanhempi}))
       tehtavat))

(defn tehtavat*
  [e! app]
  (komp/luo
    (komp/sisaan (fn [this]
                   (let [taulukon-tehtavat (luo-taulukon-tehtavat e! (get app :tehtava-ja-maaraluettelo))]
                     (e! (t/->MuutaTila [:tehtavat-taulukko] taulukon-tehtavat)))))
    (fn [e! app]
      (let [{taulukon-tehtavat :tehtavat-taulukko} app]
        [:div
         [debug/debug app]
         (if taulukon-tehtavat
           [taulukko/taulukko taulukon-tehtavat]
           [yleiset/ajax-loader])]))))

(defn tehtavat []
  (tuck/tuck tila/suunnittelu-tehtavat-tila tehtavat*))