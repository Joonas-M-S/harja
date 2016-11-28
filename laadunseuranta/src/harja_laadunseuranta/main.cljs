(ns harja-laadunseuranta.main
  (:require [reagent.core :as reagent :refer [atom]]
            [cljs-time.local :as l]
            [harja-laadunseuranta.ui.kartta :as kartta]
            [harja-laadunseuranta.tiedot.asetukset.kuvat :as kuvat]
            [harja-laadunseuranta.tiedot.sovellus :as s]
            [harja-laadunseuranta.ui.ilmoitukset :as ilmoitukset]
            [harja-laadunseuranta.ui.alustus :as alustus]
            [harja-laadunseuranta.ui.ylapalkki :as ylapalkki]
            [harja-laadunseuranta.ui.paanavigointi :refer [paanavigointi]]
            [harja-laadunseuranta.ui.tr-haku :as tr-haku]
            [harja-laadunseuranta.ui.havaintolomake :refer [havaintolomake]]
            [harja-laadunseuranta.ui.tarkastusajon-paattaminen :as tarkastusajon-luonti]
            [harja-laadunseuranta.utils :refer [flip erota-havainnot]]
            [cljs.core.async :refer [<! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn- spinneri [lahettamattomia]
  (when (> @lahettamattomia 0)
    [:img.spinner {:src kuvat/+spinner+}]))

(defn- paanakyma []
  [:div.toplevel
   [ylapalkki/ylapalkki]

   [:div.paasisalto-container
    [kartta/kartta]

    (when @s/piirra-paanavigointi?
      [paanavigointi])

    [ilmoitukset/ilmoituskomponentti s/ilmoitus]

    (when @s/havaintolomake-auki
      [havaintolomake])

    (when @s/tarkastusajo-paattymassa
      [:div.tarkastusajon-paattaminen-dialog-container
       [tarkastusajon-luonti/tarkastusajon-paattamisdialogi s/lahettamattomia-merkintoja]])

    (when (and @s/palautettava-tarkastusajo (not (= "?relogin=true" js/window.location.search)))
      [:div.tarkastusajon-paattaminen-dialog-container
       [tarkastusajon-luonti/tarkastusajon-jatkamisdialogi]])

    [spinneri s/lahettamattomia-merkintoja]
    [tr-haku/tr-selailukomponentti s/tr-tiedot-nakyvissa? s/tr-tiedot]]])

(defn main []
  (if @s/sovellus-alustettu
    [paanakyma]
    [alustus/alustuskomponentti
     {:selain-vanhentunut s/selain-vanhentunut
      :gps-tuettu s/gps-tuettu
      :ensimmainen-sijainti s/ensimmainen-sijainti
      :idxdb-tuettu s/idxdb
      :kayttaja s/kayttajanimi
      :verkkoyhteys s/verkkoyhteys
      :selain-tuettu s/selain-tuettu}]))
