(ns harja.tiedot.urakka.urakka
  "MHU-urakoiden tila täällä. Hyvä olisi joskus saada muutkin tänne, yhden atomin alle."
  (:refer-clojure :exclude [atom])
  (:require [reagent.core :refer [atom cursor]]
            [harja.tiedot.navigaatio :as nav]))

(defonce tila (atom {:yleiset {:urakka {}}
                     :suunnittelu {:tehtavat
                                   {:tehtava-ja-maaraluettelo [{:id "rivin-id-1" :nimi "Laajenna" :tehtavaryhmatyyppi "ylataso" :vanhempi nil :piillotettu? false}
                                                               {:id "rivin-id-2" :nimi "Laajenna-valitaso" :tehtavaryhmatyyppi "valitaso" :vanhempi "rivin-id-1" :piillotettu? true}
                                                               {:id "rivin-id-3" :nimi "Teksti 2" :tehtavaryhmatyyppi "alitaso" :maara 30 :vanhempi "rivin-id-2" :piillotettu? true}
                                                               {:id "rivin-id-4" :nimi "Teksti 1" :tehtavaryhmatyyppi "alitaso" :maara 100 :vanhempi "rivin-id-2" :piillotettu? true}
                                                               {:id "rivin-id-5" :nimi "Laajenna-valitaso" :tehtavaryhmatyyppi "valitaso" :vanhempi "rivin-id-1" :piillotettu? true}
                                                               {:id "rivin-id-6" :nimi "Teksti 3" :tehtavaryhmatyyppi "alitaso" :maara 150 :vanhempi "rivin-id-5" :piillotettu? true}
                                                               {:id "rivin-id-7" :nimi "Laajenna-valitaso-b" :tehtavaryhmatyyppi "valitaso" :vanhempi "rivin-id-1" :piillotettu? true}
                                                               {:id "rivin-id-8" :nimi "Teksti 1" :tehtavaryhmatyyppi "alitaso" :maara 20 :vanhempi "rivin-id-7" :piillotettu? true}]}
                                   :kustannussuunnitelma {:tavoitehinnat [{:vuosi 1 :summa 2500} {:vuosi 2 :summa 400} {:vuosi 3 :summa 8900} {:vuosi 4 :summa 3000} {:vuosi 5 :summa 1000}]
                                                          :kattohinnat [{:vuosi 1 :summa 4000} {:vuosi 2 :summa 4000} {:vuosi 3 :summa 4000} {:vuosi 4 :summa 4000} {:vuosi 5 :summa 4000}]
                                                          :hankintakustannukset {:yhteenveto [{:vuosi 1 :summa 4000} {:vuosi 2 :summa 4000} {:vuosi 3 :summa 4000} {:vuosi 4 :summa 4000} {:vuosi 5 :summa 4000}]
                                                                                 :valinnat {:toimenpide :talvihoito
                                                                                            :maksetaan :talvikausi}
                                                                                 :toimenpiteet {:talvihoito {:hankinnat (into [] (mapcat (fn [vuosi]
                                                                                                                                           (map #(identity
                                                                                                                                                   {:pvm (harja.pvm/luo-pvm vuosi % 15)
                                                                                                                                                    :maara (rand-int 10000)})
                                                                                                                                                (range 1 13)))
                                                                                                                                         (range 1 6)))
                                                                                                             :korjaukset 2
                                                                                                             :akilliset-hoitotyot 2
                                                                                                             :muut-rahavaraukset 2}
                                                                                                :liikenneympariston-hoito {:hankinnat (into [] (mapcat (fn [vuosi]
                                                                                                                                                         (map #(identity
                                                                                                                                                                 {:pvm (harja.pvm/luo-pvm vuosi % 15)
                                                                                                                                                                  :maara (rand-int 10000)})
                                                                                                                                                              (range 1 13)))
                                                                                                                                                       (range 1 6)))
                                                                                                                           :korjaukset 2
                                                                                                                           :akilliset-hoitotyot 2
                                                                                                                           :muut-rahavaraukset 2}
                                                                                                :sorateiden-hoito {:hankinnat (into [] (mapcat (fn [vuosi]
                                                                                                                                                 (map #(identity
                                                                                                                                                         {:pvm (harja.pvm/luo-pvm vuosi % 15)
                                                                                                                                                          :maara (rand-int 10000)})
                                                                                                                                                      (range 1 13)))
                                                                                                                                               (range 1 6)))
                                                                                                                   :korjaukset 2
                                                                                                                   :akilliset-hoitotyot 2
                                                                                                                   :muut-rahavaraukset 2}
                                                                                                :paallystepaikkaukset {:hankinnat (into [] (mapcat (fn [vuosi]
                                                                                                                                                     (map #(identity
                                                                                                                                                             {:pvm (harja.pvm/luo-pvm vuosi % 15)
                                                                                                                                                              :maara (rand-int 10000)})
                                                                                                                                                          (range 1 13)))
                                                                                                                                                   (range 1 6)))
                                                                                                                       :korjaukset 2
                                                                                                                       :akilliset-hoitotyot 2
                                                                                                                       :muut-rahavaraukset 2}
                                                                                                :mhu-yllapito {:hankinnat (into [] (mapcat (fn [vuosi]
                                                                                                                                             (map #(identity
                                                                                                                                                     {:pvm (harja.pvm/luo-pvm vuosi % 15)
                                                                                                                                                      :maara (rand-int 10000)})
                                                                                                                                                  (range 1 13)))
                                                                                                                                           (range 1 6)))
                                                                                                               :korjaukset 2
                                                                                                               :akilliset-hoitotyot 2
                                                                                                               :muut-rahavaraukset 2}
                                                                                                :mhu-korvausinvestointi {:hankinnat (into [] (mapcat (fn [vuosi]
                                                                                                                                                       (map #(identity
                                                                                                                                                               {:pvm (harja.pvm/luo-pvm vuosi % 15)
                                                                                                                                                                :maara (rand-int 10000)})
                                                                                                                                                            (range 1 13)))
                                                                                                                                                     (range 1 6)))
                                                                                                                         :korjaukset 2
                                                                                                                         :akilliset-hoitotyot 2
                                                                                                                         :muut-rahavaraukset 2}}}
                                                          :hallinnolliset-toimenpiteet {:yhteenveto [{:vuosi 1 :summa 4000} {:vuosi 2 :summa 4000} {:vuosi 3 :summa 4000} {:vuosi 4 :summa 4000} {:vuosi 5 :summa 4000}]
                                                                                        :erillishankinnat [{:vuosi 1 :maara-kk 300} {:vuosi 2 :maara-kk 300} {:vuosi 3 :maara-kk 300} {:vuosi 4 :maara-kk 300} {:vuosi 5 :maara-kk 300}]
                                                                                        :hoidonjohtopalkkio []
                                                                                        :johto-ja-hallintokorvaus []}

                                                          :suunnitellut-hankinnat {}}}}))

(defonce yleiset (cursor tila [:yleiset]))

(defonce suunnittelu-tehtavat (cursor tila [:suunnittelu :tehtavat]))

(defonce suunnittelu-kustannussuunnitelma (cursor tila [:suunnittelu :kustannussuunnitelma]))

(add-watch nav/valittu-urakka :urakan-id-watch
           (fn [_ _ _ uusi-urakka]
             (swap! tila assoc-in [:yleiset :urakka] (dissoc uusi-urakka :alue))))