(ns harja.tiedot.urakka.urakka
  "MHU-urakoiden tila täällä. Hyvä olisi joskus saada muutkin tänne, yhden atomin alle."
  (:refer-clojure :exclude [atom])
  (:require [reagent.core :refer [atom cursor]]
            [clojure.core.async :refer [chan]]
            [harja.tiedot.navigaatio :as nav]
            [harja.loki :as loki]))

(def suunnittelu-default-arvot {:tehtavat             {:valinnat {:samat-kaikille false
                                                                  :toimenpide     nil
                                                                  :valitaso       nil
                                                                  :noudetaan      0}}
                                :kustannussuunnitelma {:hankintakustannukset        {:valinnat {:toimenpide                     :talvihoito
                                                                                                :maksetaan                      :molemmat
                                                                                                :kopioidaan-tuleville-vuosille? true
                                                                                                :laskutukseen-perustuen-valinta #{}}}
                                                       :hallinnolliset-toimenpiteet {:valinnat {:maksetaan :molemmat}}
                                                       :kaskytyskanava              (chan)}})

(defn hankinnat-testidata [maara]
  (into []
        (drop 9
              (drop-last 3
                         (mapcat (fn [vuosi]
                                   (map #(identity
                                           {:pvm   (harja.pvm/luo-pvm vuosi % 15)
                                            :maara maara})
                                        (range 0 12)))
                                 (range (harja.pvm/vuosi (harja.pvm/nyt)) (+ (harja.pvm/vuosi (harja.pvm/nyt)) 6)))))))

(defn validoi-fn
  [lomake]
  (loki/log "Validointi tehdään" (meta lomake))
  (loki/log "onko validoitavia lapsia")
  (if (nil? (meta lomake))
    lomake
    (let [lomake (with-meta
                   (reduce (fn [lomake [avain arvo]]
                             (loki/log lomake avain arvo)
                             (assoc lomake avain
                                           (cond
                                             (vector? arvo) (mapv validoi-fn arvo)
                                             (map? arvo) (validoi-fn arvo)
                                             :else (do
                                                     (loki/log "assosioin " lomake avain arvo)
                                                     arvo))))
                           {}
                           lomake)
                   (meta lomake))]
      (vary-meta lomake (fn [{:keys [validius validi?] :as lomake-meta} lomake]
                          (loki/log lomake-meta)
                          (reduce (fn [kaikki [avain {:keys [validointi] :as validius}]]
                                    (as-> kaikki kaikki
                                          (update
                                            kaikki
                                            :validius
                                            (fn [vs]
                                              (update vs
                                                      avain (fn [kentta]
                                                              (loki/log "avain" avain "lomake" lomake "avainlomake" (avain lomake))
                                                              (assoc kentta
                                                                :validointi validointi
                                                                :validi? (validointi
                                                                           (avain lomake)))))))
                                          (update
                                            kaikki
                                            :validi?
                                            (fn [v?]
                                              (not
                                                (some (fn [[avain {validi? :validi?}]]
                                                        (false? validi?)) (:validius kaikki)))))))
                                  lomake-meta
                                  validius))
                 lomake))))

(defn luo-validius-meta [& kentat-ja-validaatiot]
  (assoc {} :validius
            (reduce (fn [k [avain validointi-fn]]
                      (assoc k avain {:validointi validointi-fn
                                      :validi?    false
                                      :koskettu?  false}))
                    {}
                    (partition 2 kentat-ja-validaatiot))
            :validi? false
            :validoi validoi-fn
            ))

(def kulut-lomake-default (with-meta {:kohdistukset          [(with-meta
                                                                {:tehtavaryhma        nil
                                                                 :toimenpideinstanssi nil
                                                                 :summa               nil
                                                                 :rivi                0}
                                                                (luo-validius-meta
                                                                  :summa (fn [summa]
                                                                           (not (nil? summa)))
                                                                  :tehtavaryhma (fn [ryhma]
                                                                                  (loki/log "Valid ryhma " ryhma)
                                                                                  (not (nil? ryhma)))))]
                                      :aliurakoitsija        nil
                                      :koontilaskun-kuukausi nil
                                      :viite                 nil
                                      :laskun-numero         nil
                                      :lisatieto             nil
                                      :suorittaja-nimi       nil
                                      :erapaiva              nil}
                                     (luo-validius-meta
                                       :koontilaskun-kuukausi (fn [kk]
                                                                (loki/log "Valid kk " kk)
                                                                (not (nil? kk)))
                                       :erapaiva (fn [pvm]
                                                   (loki/log "Valid pvm " pvm)
                                                   (and (not (nil? pvm))))
                                       :kohdistukset (fn [kohdistukset]
                                                       (loki/log "Valid kohdistukset " kohdistukset)
                                                       (some false?
                                                             (mapv
                                                               (fn [kohdistus]
                                                                 (let [{validius :validius validoi :validoi} (meta kohdistus)]
                                                                   (some false?
                                                                         (map (fn [[avain validiudet]]
                                                                                (let [{:keys [validointi validi? koskettu?]}
                                                                                      validiudet]
                                                                                  (and
                                                                                    (true? koskettu?)
                                                                                    (validointi (avain kohdistus))))) validius))))
                                                               kohdistukset))))))

(def kulut-default {:kohdistetut-kulut {:parametrit  {:haetaan 0}
                                        :taulukko    nil
                                        :lomake      kulut-lomake-default
                                        :kulut       []
                                        :syottomoodi false}})

(defonce tila (atom {:yleiset     {:urakka {}}
                     :laskutus    kulut-default
                     :suunnittelu suunnittelu-default-arvot}))


(defonce laskutus-kohdistetut-kulut (cursor tila [:laskutus :kohdistetut-kulut]))

(defonce yleiset (cursor tila [:yleiset]))

(defonce suunnittelu-tehtavat (cursor tila [:suunnittelu :tehtavat]))

(defonce suunnittelu-kustannussuunnitelma (cursor tila [:suunnittelu :kustannussuunnitelma]))

(add-watch nav/valittu-urakka :urakan-id-watch
           (fn [_ _ _ uusi-urakka]
             (swap! tila (fn [tila]
                           (-> tila
                               (assoc-in [:yleiset :urakka] (dissoc uusi-urakka :alue))
                               (assoc :suunnittelu suunnittelu-default-arvot))))))