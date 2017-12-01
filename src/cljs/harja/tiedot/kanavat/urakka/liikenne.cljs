(ns harja.tiedot.kanavat.urakka.liikenne
  (:require [reagent.core :refer [atom]]
            [clojure.spec.alpha :as s]
            [tuck.core :as tuck]
            [harja.tyokalut.tuck :as tt]
            [cljs.core.async :as async]
            [harja.pvm :as pvm]
            [harja.id :refer [id-olemassa?]]
            [harja.asiakas.kommunikaatio :as k]
            [harja.loki :refer [log tarkkaile!]]
            [harja.ui.viesti :as viesti]
            [harja.tiedot.navigaatio :as nav]
            [namespacefy.core :refer [namespacefy]]
            [harja.tiedot.urakka :as u]
            [reagent.core :as r]
            [harja.tiedot.istunto :as istunto]
            [clojure.string :as str]

            [harja.domain.urakka :as ur]
            [harja.domain.sopimus :as sop]
            [harja.domain.muokkaustiedot :as m]
            [harja.domain.kayttaja :as kayttaja]
            [harja.domain.kanavat.kohde :as kohde]
            [harja.domain.kanavat.kohteenosa :as osa]
            [harja.domain.kanavat.kohdekokonaisuus :as kok]
            [harja.domain.kanavat.liikennetapahtuma :as lt]
            [harja.domain.kanavat.lt-alus :as lt-alus]
            [harja.domain.kanavat.lt-toiminto :as toiminto]
            [clojure.set :as set]
            [harja.ui.modal :as modal])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(def tila (atom {:nakymassa? false
                 :liikennetapahtumien-haku-kaynnissa? false
                 :tallennus-kaynnissa? false
                 :valittu-liikennetapahtuma nil
                 :tapahtumarivit nil
                 :valinnat {::ur/id nil
                            ::sop/id nil
                            :aikavali nil
                            ::lt/kohde nil
                            ::lt-alus/suunta nil
                            ::lt/sulku-toimenpide nil
                            ::lt-alus/laji nil
                            :niput? false}}))

(defn uusi-tapahtuma
  ([]
   (uusi-tapahtuma istunto/kayttaja u/valittu-sopimusnumero nav/valittu-urakka (pvm/nyt)))
  ([kayttaja sopimus urakka aika]
   {::lt/kuittaaja (namespacefy @kayttaja {:ns :harja.domain.kayttaja})
    ::lt/aika aika
    ::lt/sopimus {::sop/id (first @sopimus)
                  ::sop/nimi (second @sopimus)}
    ::lt/urakka {::ur/id (:id @urakka)}}))

(def valinnat
  (reaction
    (when (:nakymassa? @tila)
      {::ur/id (:id @nav/valittu-urakka)
       :aikavali @u/valittu-aikavali
       ::sop/id (first @u/valittu-sopimusnumero)})))

(def valintojen-avaimet
  [::ur/id ::sop/id :aikavali ::lt/kohde ::lt-alus/suunta ::lt/sulku-toimenpide ::lt-alus/laji :niput?])

(defrecord Nakymassa? [nakymassa?])
(defrecord HaeLiikennetapahtumat [])
(defrecord LiikennetapahtumatHaettu [tulos])
(defrecord LiikennetapahtumatEiHaettu [virhe])
(defrecord ValitseTapahtuma [tapahtuma])
(defrecord HaeEdellisetTiedot [tapahtuma])
(defrecord EdellisetTiedotHaettu [tulos])
(defrecord EdellisetTiedotEiHaettu [virhe])
(defrecord PaivitaValinnat [uudet])
(defrecord TapahtumaaMuokattu [tapahtuma])
(defrecord MuokkaaAluksia [alukset virheita?])
(defrecord VaihdaSuuntaa [alus])
(defrecord TallennaLiikennetapahtuma [tapahtuma])
(defrecord TapahtumaTallennettu [tulos])
(defrecord TapahtumaEiTallennettu [virhe])

(defn valinta-wrap [e! app polku]
  (r/wrap (get-in app [:valinnat polku])
          (fn [u]
            (e! (->PaivitaValinnat {polku u})))))

(defn hakuparametrit [app]
  ;; Ei nil arvoja
  (when (and (::sop/id (:valinnat app))
             (::ur/id (:valinnat app)))
    (into {} (filter val (:valinnat app)))))

(defn palvelumuoto->str [tapahtuma]
  (str/join ", " (into #{} (sort (map lt/fmt-palvelumuoto (filter ::toiminto/palvelumuoto (::lt/toiminnot tapahtuma)))))))

(defn toimenpide->str [tapahtuma]
  (str/join ", " (into #{} (sort (map (comp lt/toimenpide->str ::toiminto/toimenpide)
                                       (remove
                                         (comp
                                           (partial = :ei-avausta)
                                           ::toiminto/toimenpide)
                                         (::lt/toiminnot tapahtuma)))))))

(defn tapahtumarivit [tapahtuma]
  (let [alustiedot
        (map
          (fn [alus]
            (merge
              tapahtuma
              alus))
          (::lt/alukset tapahtuma))]

    (if (empty? alustiedot)
      ;; Alustiedot ovat tyhjiä,
      ;; jos rivi on vain itsepalveluiden kirjaamista.
      [tapahtuma]
      alustiedot)))

(defn koko-tapahtuma [rivi {:keys [haetut-tapahtumat]}]
  (some #(when (= (::lt/id rivi) (::lt/id %)) %) haetut-tapahtumat))

(defn tapahtumat-haettu [app tulos]
  (-> app
      (assoc :liikennetapahtumien-haku-kaynnissa? false)
      (assoc :haetut-tapahtumat tulos)
      (assoc :tapahtumarivit (mapcat tapahtumarivit tulos))))

(defn tallennusparametrit [t]
  (-> t
      (dissoc :grid-virheita?)
      (assoc ::lt/kuittaaja-id (get-in t [::lt/kuittaaja ::kayttaja/id]))
      (dissoc ::lt/kuittaaja)
      (assoc ::lt/kohde-id (get-in t [::lt/kohde ::kohde/id]))
      (dissoc ::lt/kohde)
      (assoc ::lt/urakka-id (:id @nav/valittu-urakka))
      (dissoc ::lt/urakka)
      (assoc ::lt/sopimus-id (get-in t [::lt/sopimus ::sop/id]))
      (dissoc ::lt/sopimus)
      (update ::lt/alukset (fn [alukset] (map
                                           #(-> %
                                                (set/rename-keys {:poistettu ::m/poistettu?})
                                                (dissoc :id)
                                                (dissoc :harja.ui.grid/virheet))
                                           alukset)))
      (update ::lt/toiminnot (fn [toiminnot] (map (fn [toiminto]
                                                    (->
                                                      (->> (keys toiminto)
                                                          (filter #(= (namespace %) "harja.domain.kanavat.lt-toiminto"))
                                                          (select-keys toiminto))
                                                      (update ::toiminto/palvelumuoto #(if (= :ei-avausta (::toiminto/toimenpide toiminto))
                                                                                         nil
                                                                                         %))))
                                                  toiminnot)))))

(defn voi-tallentaa? [t]
  (and (not (:grid-virheita? t))
       (empty? (filter :koskematon (::lt/alukset t)))
       (every? #(some? (::lt-alus/suunta %)) (::lt/alukset t))))

(defn sama-alusrivi? [a b]
  ;; Tunnistetaan muokkausgridin rivi joko aluksen id:llä, tai jos rivi on uusi, gridin sisäisellä id:llä
  (or
    (and
      (some? (::lt-alus/id a))
      (some? (::lt-alus/id b))
      (= (::lt-alus/id a)
         (::lt-alus/id b)))
    (and
      (some? (:id a))
      (some? (:id b))
      (= (:id a)
         (:id b)))))

(defn paivita-toiminnon-tiedot [tapahtuma toiminto]
  (assoc
    tapahtuma
    ::lt/toiminnot
    (mapcat val
            (assoc
              (group-by ::toiminto/kohteenosa-id (::lt/toiminnot tapahtuma))
              ;; Etsi palvelumuoto kohteenosan id:llä, ja korvaa/luo arvo
              (::toiminto/kohteenosa-id toiminto)
              [toiminto]))))

(defn kohteenosatiedot-toimintoihin
  "Ottaa tapahtuman ja kohteen, ja yhdistää tapahtuman toimintoihin kohteen kohteenosien tiedot.
  Jos kyseessä on olemassaoleva tapahtuma, liitetään vanhoihin toiminto tietoihin mm. kohteenosan nimi.
  Jos kyseessä on uusi tapahtuma, luodaan tapahtumalle tyhjät toiminto tiedot, jotka täytetään loppuun lomakkeella."
  [tapahtuma kohde]
  (-> tapahtuma
      (assoc ::lt/kohde kohde)
      (update ::lt/toiminnot
              (fn [osat]
                (let [vanhat (group-by ::toiminto/kohteenosa-id osat)]
                  (map
                    (fn [osa]
                      (merge
                        (-> osa
                           (set/rename-keys {::osa/id ::toiminto/kohteenosa-id
                                             ::osa/kohde-id ::toiminto/kohde-id})
                           (assoc ::toiminto/lkm 1)
                           (assoc ::toiminto/palvelumuoto (::osa/oletuspalvelumuoto osa))
                           (assoc ::toiminto/toimenpide (when (kohde/silta? osa) :ei-avausta)))
                        (first (vanhat (::osa/id osa)))))
                    (::kohde/kohteenosat kohde)))))))

(extend-protocol tuck/Event
  Nakymassa?
  (process-event [{nakymassa? :nakymassa?} app]
    (assoc app :nakymassa? nakymassa?))

  HaeLiikennetapahtumat
  (process-event [_ app]
    (if-not (:liikennetapahtumien-haku-kaynnissa? app)
      (if-let [params (hakuparametrit app)]
        (-> app
            (tt/post! :hae-liikennetapahtumat
                      params
                      {:onnistui ->LiikennetapahtumatHaettu
                       :epaonnistui ->LiikennetapahtumatEiHaettu})
            (assoc :liikennetapahtumien-haku-kaynnissa? true))

        app)

      app))

  LiikennetapahtumatHaettu
  (process-event [{tulos :tulos} app]
    (tapahtumat-haettu app tulos))

  LiikennetapahtumatEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Liikennetapahtumien haku epäonnistui! " :danger)
    (assoc app :liikennetapahtumien-haku-kaynnissa? false))

  ValitseTapahtuma
  (process-event [{t :tapahtuma} app]
    (assoc app :valittu-liikennetapahtuma (when-let [tapahtuma (if (::lt/id t) (koko-tapahtuma t app) t)]
                                            (kohteenosatiedot-toimintoihin tapahtuma (::lt/kohde tapahtuma)))))

  HaeEdellisetTiedot
  (process-event [{t :tapahtuma} app]
    (let [params {::lt/urakka-id (get-in t [::lt/urakka ::ur/id])
                  ::lt/kohde-id (get-in t [::lt/kohde ::kohde/id])
                  ::lt/sopimus-id (get-in t [::lt/sopimus ::sop/id])}]
      (tt/post! :hae-edelliset-tapahtumat
                params
                {:onnistui ->EdellisetTiedotHaettu
                 :epaonnistui ->EdellisetTiedotEiHaettu})
      (assoc app :edellisten-haku-kaynnissa? true)))

  EdellisetTiedotHaettu
  (process-event [{t :tulos} app]
    (-> app
        (assoc-in [:edelliset :tama] (:kohde t))
        (assoc-in [:valittu-liikennetapahtuma ::lt/vesipinta-alaraja] (get-in t [:kohde ::lt/vesipinta-alaraja]))
        (assoc-in [:valittu-liikennetapahtuma ::lt/vesipinta-ylaraja] (get-in t [:kohde ::lt/vesipinta-ylaraja]))
        (assoc-in [:edelliset :ylos] (:ylos t))
        (assoc-in [:edelliset :alas] (:alas t))
        (assoc :edellisten-haku-kaynnissa? false)))

  EdellisetTiedotEiHaettu
  (process-event [_ app]
    (viesti/nayta! "Virhe edellisten tapahtumien haussa!" :danger)
    (-> app
        (assoc :edellisten-haku-kaynnissa? false)))

  PaivitaValinnat
  (process-event [{u :uudet} app]
    (let [uudet-valinnat (merge (:valinnat app)
                                (select-keys u valintojen-avaimet))
          haku (tuck/send-async! ->HaeLiikennetapahtumat)]
      (go (haku uudet-valinnat))
      (assoc app :valinnat uudet-valinnat)))

  TapahtumaaMuokattu
  (process-event [{t :tapahtuma} app]
    (assoc app :valittu-liikennetapahtuma t))

  MuokkaaAluksia
  (process-event [{alukset :alukset v :virheita?} app]
    (if (:valittu-liikennetapahtuma app)
      (-> app
          (assoc-in [:valittu-liikennetapahtuma ::lt/alukset] alukset)
          (assoc-in [:valittu-liikennetapahtuma :grid-virheita?] v))

      app))

  VaihdaSuuntaa
  (process-event [{alus :alus} app]
    (let [uusi (if (= :ylos (::lt-alus/suunta alus))
                 (assoc alus ::lt-alus/suunta :alas)
                 (assoc alus ::lt-alus/suunta :ylos))]
      (update app :valittu-liikennetapahtuma
              (fn [t]
                (update t ::lt/alukset
                        (fn [alukset]
                          (map #(if (sama-alusrivi? uusi %) uusi %) alukset)))))))

  TallennaLiikennetapahtuma
  (process-event [{t :tapahtuma} {:keys [tallennus-kaynnissa?] :as app}]
    (if-not tallennus-kaynnissa?
      (let [params (-> (tallennusparametrit t)
                       (assoc :hakuparametrit (hakuparametrit app)))]
        (-> app
            (tt/post! :tallenna-liikennetapahtuma
                      params
                      {:onnistui ->TapahtumaTallennettu
                       :epaonnistui ->TapahtumaEiTallennettu})
            (assoc :tallennus-kaynnissa? true)))

      app))

  TapahtumaTallennettu
  (process-event [{t :tulos} app]
    (when (modal/nakyvissa?) (modal/piilota!))
    (-> app
        (assoc :tallennus-kaynnissa? false)
        (assoc :valittu-liikennetapahtuma nil)
        (tapahtumat-haettu t)))

  TapahtumaEiTallennettu
  (process-event [_ app]
    (viesti/nayta! "Virhe tapahtuman tallennuksessa!" :danger)
    (assoc app :tallennus-kaynnissa? false)))
