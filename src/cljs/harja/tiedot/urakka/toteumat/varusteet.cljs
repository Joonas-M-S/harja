(ns harja.tiedot.urakka.toteumat.varusteet
  (:require [reagent.core :refer [atom] :as r]
            [cljs.core.async :refer [<!]]
            [harja.loki :refer [log tarkkaile!]]
            [harja.tiedot.urakka :as urakka]
            [harja.tiedot.navigaatio :as nav]
            [harja.asiakas.kommunikaatio :as k]
            [harja.ui.kartta.esitettavat-asiat :refer [kartalla-esitettavaan-muotoon kartalla-xf]]
            [harja.pvm :as pvm]
            [harja.geo :as geo]
            [tuck.core :as t]
            [harja.tiedot.urakka.toteumat.varusteet.viestit :as v]
            [reagent.core :as r])
  (:require-macros [reagent.ratom :refer [reaction]]
                   [cljs.core.async.macros :refer [go]]))

(defonce valinnat
  (reaction {:urakka-id (:id @nav/valittu-urakka)
             :sopimus-id (first @urakka/valittu-sopimusnumero)
             :hoitokausi @urakka/valittu-hoitokausi
             :kuukausi @urakka/valittu-hoitokauden-kuukausi}))

(defonce varusteet
  (atom {:nakymassa? false

         :tienumero nil

         ;; Valinnat (urakka, sopimus, hk, kuukausi)
         :valinnat nil

         ;; Ajastetun toteumahaun id
         :toteumahaku-id nil

         ;; Toteumat, jotka on haettu nykyisten valintojen perusteella
         :toteumat nil

         ;; Karttataso varustetoteumille
         :karttataso-nakyvissa? false
         :karttataso nil

         ;; Valittu varustetoteuma
         :varustetoteuma nil}))

(defn- hae [{valinnat :valinnat toteumahaku-id :toteumahaku-id :as app}]
  (when toteumahaku-id
    (.clearTimeout js/window toteumahaku-id))
  (assoc app
         :toteumahaku-id (.setTimeout js/window
                                      (t/send-async! v/->HaeVarusteToteumat)
                                      500)
         :toteumat nil))

(def varuste-toimenpide->string {nil         "Kaikki"
                                 :lisatty    "Lisätty"
                                 :paivitetty "Päivitetty"
                                 :poistettu  "Poistettu"
                                 :tarkastus  "Tarkastus"})

(def varustetoteumatyypit
  (vec varuste-toimenpide->string))

(def tietolaji->selitys
  {"tl523" "Tekninen piste"
   "tl501" "Kaiteet"
   "tl517" "Portaat"
   "tl507" "Bussipysäkin varusteet"
   "tl508" "Bussipysäkin katos"
   "tl506" "Liikennemerkki"
   "tl522" "Reunakivet"
   "tl513" "Reunapaalut"
   "tl196" "Bussipysäkit"
   "tl519" "Puomit ja kulkuaukot"
   "tl505" "Jätehuolto"
   "tl195" "Tienkäyttäjien palvelualueet"
   "tl504" "WC"
   "tl198" "Kohtaamispaikat ja levikkeet"
   "tl518" "Kivetyt alueet"
   "tl514" "Melurakenteet"
   "tl509" "Rummut"
   "tl515" "Aidat"
   "tl503" "Levähdysalueiden varusteet"
   "tl510" "Viheralueet"
   "tl512" "Viemärit"
   "tl165" "Välikaistat"
   "tl516" "Hiekkalaatikot"
   "tl511" "Viherkuviot"})

(defn- selite [{:keys [toimenpide tietolaji alkupvm]}]
  (str
   (pvm/pvm alkupvm) " "
   (varuste-toimenpide->string toimenpide)
   " "
   (tietolaji->selitys tietolaji)))

(defn- varustetoteumat-karttataso [toteumat]
  (kartalla-esitettavaan-muotoon
   toteumat
   nil nil
   (keep (fn [toteuma]
           (when-let [sijainti (some-> toteuma :sijainti geo/pisteet first)]
             (assoc toteuma
                    :tyyppi-kartalla :varustetoteuma
                    :selitys-kartalla (selite toteuma)
                    :sijainti {:type :point
                               :coordinates sijainti}))))))

(defn- hae-toteumat [urakka-id sopimus-id [alkupvm loppupvm] tienumero]
  (k/post! :urakan-varustetoteumat
           {:urakka-id  urakka-id
            :sopimus-id sopimus-id
            :alkupvm    alkupvm
            :loppupvm   loppupvm
            :tienumero tienumero}))

(defn uusi-varustetoteuma
  "Luo uuden tyhjän varustetoteuman lomaketta varten."
  []
  {})


(extend-protocol t/Event
  v/YhdistaValinnat
  (process-event [{valinnat :valinnat} app]
    (hae (update app :valinnat merge valinnat)))

  v/HaeVarusteToteumat
  (process-event [_ {valinnat :valinnat :as app}]
    (let [tulos! (t/send-async! v/->VarusteToteumatHaettu)]
      (go
        (let [{:keys [urakka-id sopimus-id kuukausi hoitokausi tienumero]} valinnat]
          (tulos! (<! (hae-toteumat urakka-id sopimus-id
                                    (or kuukausi hoitokausi)
                                    tienumero)))))
      (assoc app
             :toteumahaku-id nil)))

  v/VarusteToteumatHaettu
  (process-event [{toteumat :toteumat} app]
    (assoc app
           :karttataso (varustetoteumat-karttataso toteumat)
           :karttataso-nakyvissa? true
           :toteumat toteumat))

  v/ValitseToteuma
  (process-event [{toteuma :toteuma} app]
    (assoc app
           :varustetoteuma toteuma))
  v/TyhjennaValittuToteuma
  (process-event [_ app]
    (assoc app :varustetoteuma nil))
  v/UusiVarusteToteuma
  (process-event [_ app]
    (assoc app :varustetoteuma (uusi-varustetoteuma))))


(defonce karttataso-varustetoteuma (r/cursor varusteet [:karttataso-nakyvissa?]))
(defonce varusteet-kartalla (r/cursor varusteet [:karttataso]))
