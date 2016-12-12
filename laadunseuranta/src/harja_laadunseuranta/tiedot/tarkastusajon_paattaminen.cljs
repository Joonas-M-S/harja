(ns harja-laadunseuranta.tiedot.tarkastusajon-paattaminen
  (:require [reagent.core :as reagent :refer [atom]]
            [harja-laadunseuranta.tiedot.comms :as comms]
            [harja-laadunseuranta.ui.napit :refer [nappi]]
            [harja-laadunseuranta.tiedot.sovellus :as s]
            [harja-laadunseuranta.tiedot.reitintallennus :as reitintallennus])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn- alusta-uusi-tarkastusajo [sovellus]
  (-> sovellus
      (assoc
        ;; Tarkastusajon perustiedot
        :valittu-urakka nil
        :tarkastusajo-id nil
        :tarkastusajo-kaynnissa? false
        :tarkastusajo-paattymassa? false
        ;; Ajonaikaiset tiedot
        :reittipisteet []
        :tr-tiedot {:tr-osoite {:tie nil
                                :aosa nil
                                :aet nil}
                    :talvihoitoluokka nil}
        ;; Havainnot
        :jatkuvat-havainnot #{}
        ;; Mittaukset
        :mittaussyotto {:nykyinen-syotto nil
                        :syotot []}
        :soratiemittaussyotto {:tasaisuus 5
                               :kiinteys 5
                               :polyavyys 5}
        :mittaustyyppi nil
        ;; Lomake
        :havaintolomake-auki? false
        :havaintolomakedata {:kayttajanimi nil
                             :tr-osoite nil
                             :aikaleima nil
                             :laadunalitus? false
                             :kuvaus ""
                             :kuva nil}
        ;; Kartta
        :kirjauspisteet []
        ;; Muut
        :ilmoitukset [])

      ;; UI:sta resetoidaan vain näkyvyys
      (assoc-in [:ui :paanavigointi :nakyvissa?] true)))

(defn- pysayta-tarkastusajo! []
  (swap! s/sovellus alusta-uusi-tarkastusajo))

(defn paattaminen-peruttu! []
  (reset! s/tarkastusajo-paattymassa? false))

(defn aseta-ajo-paattymaan! []
  (reset! s/tarkastusajo-paattymassa? true))

(defn paata-ajo! []
  (go-loop []
           (if (<! (comms/paata-ajo! @s/tarkastusajo-id @s/valittu-urakka))
             (pysayta-tarkastusajo!)

             ;; yritä uudelleen kunnes onnistuu, spinneri pyörii
             (do (<! (timeout 1000))
                 (recur)))))

(defn pakota-ajon-lopetus! []
  (let [ajo @s/palautettava-tarkastusajo]
    (reitintallennus/poista-tarkastusajo @s/idxdb (get ajo "tarkastusajo"))
    (reitintallennus/tyhjenna-reittipisteet @s/idxdb))
  (reset! s/palautettava-tarkastusajo nil))