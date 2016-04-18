(ns harja.palvelin.raportointi.raportit.sanktio
  (:require [jeesql.core :refer [defqueries]]
            [taoensso.timbre :as log]
            [harja.kyselyt.urakat :as urakat-q]
            [harja.kyselyt.hallintayksikot :as hallintayksikot-q]
            [harja.pvm :as pvm]
            [harja.domain.materiaali :as materiaalidomain]
            [harja.palvelin.raportointi.raportit.yleinen :refer [raportin-otsikko]]
            [harja.domain.laadunseuranta.sanktiot :as sanktiot-domain]
            [harja.kyselyt.konversio :as konv]
            [harja.fmt :as fmt]
            [harja.palvelin.raportointi.raportit.yleinen :as yleinen]
            [clojure.string :as str]))

(defqueries "harja/palvelin/raportointi/raportit/sanktiot.sql")

(defn talvihoito? [kantarivi]
  (= (str/lower-case (:toimenpidekoodi_taso2 kantarivi)) "talvihoito"))

(defn rivien-urakat [rivit]
  (-> (map (fn [rivi]
             {:id (:urakka_id rivi)
              :nimi (:urakka_nimi rivi)})
           rivit)
      distinct))

(defn urakan-rivit [rivit urakka-id]
  (filter
    (fn [rivi]
      (= (:urakka_id rivi) urakka-id))
    rivit))

(defn sakkojen-maara
  ([rivit] (sakkojen-maara rivit nil))
  ([rivit sakkoryhma] (count (filter
                               (fn [rivi]
                                 (and
                                   (sanktiot-domain/sakko? rivi)
                                   (or (nil? sakkoryhma)
                                       (= sakkoryhma (:sakkoryhma rivi)))))
                               rivit))))

(defn sakkojen-summa
  ([rivit] (sakkojen-summa rivit nil))
  ([rivit sakkoryhma] (let [laskettavat (filter
                                          (fn [rivi]
                                            (and
                                              (sanktiot-domain/sakko? rivi)
                                              (or (nil? sakkoryhma)
                                                  (= sakkoryhma (:sakkoryhma rivi)))))
                                          rivit)]
                        (reduce + (map
                                    #(or (:summa %) 0)
                                    laskettavat)))))

(defn muistutusten-maara [rivit]
  (let [laskettavat (filter
                      (fn [rivi]
                        (not (sanktiot-domain/sakko? rivi)))
                      rivit)]
    (count laskettavat)))



(defn luo-rivi-sakkoryhman-maara ([otsikko rivit]
                                  (luo-rivi-sakkoryhman-maara otsikko rivit nil))
  ([otsikko rivit sakkoryhma]
   (apply conj [otsikko "kpl"] (mapv (fn [urakka]
                                       (sakkojen-maara
                                         (urakan-rivit rivit (:id urakka))
                                         sakkoryhma))
                                     (rivien-urakat rivit)))))

(defn luo-rivi-sakkoryhman-summa
  ([otsikko rivit]
   (luo-rivi-sakkoryhman-summa otsikko rivit nil))
  ([otsikko rivit sakkoryhma]
   (apply conj [otsikko "€"] (mapv (fn [urakka]
                                     (sakkojen-summa
                                       (urakan-rivit rivit (:id urakka))
                                       sakkoryhma))
                                   (rivien-urakat rivit)))))

(defn luo-rivi-muistutuksien-maara [otsikko rivit]
  (apply conj [otsikko "kpl"] (mapv (fn [urakka]
                                      (muistutusten-maara
                                        (urakan-rivit rivit (:id urakka))))
                                    (rivien-urakat rivit))))

(defn sanktiot-raportille [kantarivit]
  (let [talvihoito-rivit (filter talvihoito? kantarivit)
        muut-tuotteet (filter (comp not talvihoito?) kantarivit)]
    [{:otsikko "Talvihoito"}
     (luo-rivi-muistutuksien-maara "Muistutukset" talvihoito-rivit)
     (luo-rivi-sakkoryhman-summa "Sakko A" talvihoito-rivit :A)
     ["- Päätiet" "€" "?"]                                  ; TODO
     ["- Muut tiet" "€" "?"]                                ; TODO
     (luo-rivi-sakkoryhman-summa "Sakko B" talvihoito-rivit :B)
     ["- Päätiet" "€" "?"]                                  ; TODO
     ["- Muut tiet" "€" "?"]                                ; TODO
     (luo-rivi-sakkoryhman-summa "Talvihoito, sakot yht." talvihoito-rivit)
     ["- Talvihoito, indeksit yht." "€" "?"]                ; TODO

     {:otsikko "Muut tuotteet"}
     (luo-rivi-muistutuksien-maara "Muistutukset" talvihoito-rivit)
     (luo-rivi-sakkoryhman-summa "Sakko A" muut-tuotteet :A) ; TODO
     ["- Liikenneymp. hoito" "€" "?"]                       ; TODO
     ["- Sorateiden hoito" "€" "?"]                         ; TODO
     (luo-rivi-sakkoryhman-summa "Sakko B" muut-tuotteet :B)
     ["- Liikenneymp. hoito" "€" "?"]                       ; TODO
     ["- Sorateiden hoito" "€" "?"]                         ; TODO
     (luo-rivi-sakkoryhman-summa "Muut tuotteet, sakot yht." muut-tuotteet)
     ["- Muut tuotteet, indeksit yht." "€" "?"]             ; TODO

     {:otsikko "Ryhmä C"}
     (luo-rivi-sakkoryhman-summa "Ryhmä C, sakot yht." kantarivit :C)
     ["Ryhmä C, indeksit yht." "€" "?"]                     ; TODO

     {:otsikko "Yhteensä"}
     (luo-rivi-muistutuksien-maara "Muistutukset yht." kantarivit)
     ["Indeksit yht." "€" "?"]                              ; TODO
     (luo-rivi-sakkoryhman-summa "Kaikki sakot yht." kantarivit)
     ["Kaikki yht." "€" "?"]]))                             ; TODO

(defn suorita [db user {:keys [alkupvm loppupvm
                               urakka-id hallintayksikko-id
                               urakkatyyppi] :as parametrit}]
  (let [konteksti (cond urakka-id :urakka
                        hallintayksikko-id :hallintayksikko
                        :default :koko-maa)
        kantarivit (into []
                         (comp
                           (map #(konv/string->keyword % :sakkoryhma))
                           (map #(konv/array->set % :sanktiotyyppi_laji keyword)))
                         (hae-sanktiot db
                                       {:urakka urakka-id
                                        :hallintayksikko hallintayksikko-id
                                        :urakkatyyppi (when urakkatyyppi (name urakkatyyppi))
                                        :alku alkupvm
                                        :loppu loppupvm}))
        raportin-otsikot (apply conj
                                [{:otsikko "" :leveys 10}
                                 {:otsikko "Yks." :leveys 3}]
                                (mapv
                                  (fn [urakka]
                                    {:otsikko (:nimi urakka) :leveys 20})
                                  (rivien-urakat kantarivit)))
        raporttidata (sanktiot-raportille kantarivit)
        raportin-nimi "Sanktioraportti"
        otsikko (raportin-otsikko
                  (case konteksti
                    :urakka (:nimi (first (urakat-q/hae-urakka db urakka-id)))
                    :hallintayksikko (:nimi (first (hallintayksikot-q/hae-organisaatio
                                                     db hallintayksikko-id)))
                    :koko-maa "KOKO MAA")
                  raportin-nimi alkupvm loppupvm)]

    [:raportti {:nimi raportin-nimi
                :orientaatio :landscape}
     [:taulukko {:otsikko otsikko}
      raportin-otsikot
      raporttidata]]))
