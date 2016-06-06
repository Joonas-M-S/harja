(ns harja.views.urakka.laadunseuranta
  (:require [reagent.core :refer [atom]]
            [harja.ui.bootstrap :as bs]

            [harja.tiedot.navigaatio :as nav]
            [harja.tiedot.urakka.laadunseuranta :as urakka-laadunseuranta]
            [harja.views.urakka.laadunseuranta.tarkastukset :as tarkastukset]
            [harja.views.urakka.laadunseuranta.laatupoikkeamat :as laatupoikkeamat]
            [harja.views.urakka.laadunseuranta.sanktiot :as sanktiot]
            [harja.ui.komponentti :as komp]
            [harja.loki :refer [log]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.views.urakka.siltatarkastukset :as siltatarkastukset]))

(defn laadunseuranta [ur]
  (komp/luo
    (komp/lippu urakka-laadunseuranta/laadunseurannassa?)
    (fn [{:keys [id tyyppi] :as ur}]
      [bs/tabs
       {:style :tabs :classes "tabs-taso2"
        :active (nav/valittu-valilehti-atom :laadunseuranta)}

       "Tarkastukset" :tarkastukset
       (when (oikeudet/urakat-laadunseuranta-tarkastukset id)
         [tarkastukset/tarkastukset])

       "Laatupoikkeamat" :laatupoikkeamat
       (when (oikeudet/urakat-laadunseuranta-laatupoikkeamat id)
         [laatupoikkeamat/laatupoikkeamat])

       "Sanktiot" :sanktiot
       (when (oikeudet/urakat-laadunseuranta-sanktiot id)
         [sanktiot/sanktiot])

       "Siltatarkastukset" :siltatarkastukset
       (when (and (= :hoito tyyppi)
                  (oikeudet/urakat-laadunseuranta-siltatarkastukset id))
         ^{:key "siltatarkastukset"}
         [siltatarkastukset/siltatarkastukset])])))

