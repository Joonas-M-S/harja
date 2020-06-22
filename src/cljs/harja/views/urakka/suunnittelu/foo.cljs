(ns harja.views.urakka.suunnittelu.foo
  (:require [reagent.core :as r :refer [atom]]
            [tuck.core :as tuck]
            [harja.ui.komponentti :as komp]
            [harja.tyokalut.tuck :as tuck-apurit]
            [harja.ui.taulukko.grid-osan-vaihtaminen :as gov]
            [harja.ui.taulukko.grid-pohjat :as g-pohjat]
            [harja.ui.taulukko.grid :as grid]
            [harja.ui.taulukko.impl.solu :as solu]
            [harja.ui.taulukko.grid-oletusarvoja :as konf]
            [harja.ui.taulukko.protokollat :as p]
            [harja.ui.napit :as napit]
            [clojure.string :as clj-str]
            [harja.fmt :as fmt])
  (:require-macros [harja.ui.taulukko.grid :refer [jarjesta-data triggeroi-seurannat]]))

(defonce ^:mutable e! nil)

(defrecord JarjestaData [otsikko])
(defrecord LisaaRivi [])
(defrecord MuokkaaUudenRivinNimea [nimi])

(extend-protocol tuck/Event
  JarjestaData
  (process-event [{:keys [otsikko]} {g :grid :as app}]
    (when-not (nil? otsikko)
      (let [jarjestys-fn (with-meta (fn [datarivit]
                                      (if (= :rivi otsikko)
                                        ;; Jos järjestetään otsikkosarakkeen mukaan, käytetään aakkosjärjestystä
                                        (sort-by key datarivit)
                                        ;; Jos järjestetään minkään muun sarakkeen mukaan, käytetään rivin yhteensä arvoa ja järkätään ASCENDING järjestyksessä
                                        (sort-by (fn [[_ v]]
                                                   (reduce #(+ %1 (:a %2))
                                                           0
                                                           v))
                                                 datarivit)))
                                    ;; Annetaan metana tämän funktion nimi, jotta ei tarvitse ylikirjoittaa gridillä jo olevaa järjestysfunktiota, jos kyseessä on saman niminen funktio
                                    {:nimi (str "jarjesta-otsikot-" otsikko)})]
        ;; Halutaan järjestää juurikin dataosia jarjestys-fn mukaisesti. Eli grid, jonka nimi on ::data eikä ::otsikko tai ::yhteenveto osioita.
        ;; ::data osion rajapinnaksi on määritetty :datarivit ja halutaan järjestää kaikki data siinä rajapinnassa, joten syvyydeksi määritetään 0.
        (grid/lisaa-jarjestys-fn-gridiin! g
                                          :datarivit
                                          0
                                          jarjestys-fn)
        ;; Yllä oleva rivi lisäsi tuon funktion, muttei triggeröinyt järjestystä. Tämä rivi triggeröi järjestyksen.
        (grid/jarjesta-grid-data! g :datarivit)))
    ;; Ei haluta muuttaa app statea mitenkään, niin palautetaan vain app
    app)
  LisaaRivi
  (process-event [_ {:keys [uusi-rivi data] :as app}]
    ;; Rivillä on fixatut 3 lasta, joten lisätään kaikille niille data
    (let [uuden-rivin-nimi (keyword uusi-rivi)
          rivi-olemassa? (some #(= uuden-rivin-nimi (:rivi %))
                               data)
          seuraava-rivitunnistin (inc (reduce #(max %1 (get %2 ::rivitunnistin)) 0 data))]
      ;; Jos yritetään luoda jo olemassa oleva rivi uudelleen, palautetaan app state ilman muutoksia
      (if rivi-olemassa?
        app
        (-> app
            ;; Lisätään uuden rivin kolme datapointtia
            (update :data (fn [data]
                            (apply conj data (map (fn [i]
                                                    {:rivi uuden-rivin-nimi
                                                     ::rivitunnistin i})
                                                  (range seuraava-rivitunnistin (+ seuraava-rivitunnistin 3))))))
            ;; Tyhjennetään "Lisää rivi" kenttä
            (assoc :uusi-rivi nil)))))
  MuokkaaUudenRivinNimea
  (process-event [{:keys [nimi]} app]
    (assoc app :uusi-rivi nimi)))

;; Data on vektori mappeja. Pidtään myös Otsikkorivin arvot omalla rivillään
(defonce alkudata {:data [{:rivi :foo :a 1 :b 2 :c 3}
                          {:rivi :foo :a 2 :b 3 :c 4}
                          {:rivi :foo :a 3 :b 4 :c 5}
                          {:rivi :bar :a 10 :b 20 :c 30}
                          {:rivi :bar :a 20 :b 30 :c 40}
                          {:rivi :bar :a 30 :b 40 :c 50}
                          {:rivi :baz :a 400 :b 200 :c 300}
                          {:rivi :baz :a 200 :b 300 :c 400}
                          {:rivi :baz :a 300 :b 400 :c 500}]
                   :gridin-otsikot ["RIVIN NIMI" "A" "B" "C"]})

(def tila (atom alkudata))

(defn summa-formatointi [teksti]
  ;; Halutaan aina näyttää jokin numero. Joten jos arvona on jokin "tyjä" arvo, näytetään "0,00". js/isNaN palauttaa false,
  ;; jos "teksti" arvo sisältää pilkun.
  (if (nil? teksti)
    "0,00"
    ;; Korvataan piste erotin pilkulla
    (let [teksti (clj-str/replace (str teksti) "," ".")]
      (if (or (= "" teksti) (js/isNaN teksti))
        "0,00"
        ;; Sallitaan vain kahden desimaalin tarkkuus
        (fmt/desimaaliluku teksti 2 true)))))

(defn summa-formatointi-aktiivinen [teksti]
  ;; Summa-formatointi-aktiivinen eroaa summa-formatointi funktiosta siten, että sallitaan tyhjä ("") arvo
  (let [teksti-ilman-pilkkua (clj-str/replace (str teksti) "," ".")]
    (cond
      (or (nil? teksti) (= "" teksti)) ""
      ;; fmt/desimaaliluku poistaa "turhat" nollat lopusta, niin laitetaan ne takaisin
      (re-matches #".*\.0*$" teksti-ilman-pilkkua) (apply str (fmt/desimaaliluku teksti-ilman-pilkkua nil true)
                                                          (drop 1 (re-find #".*(\.|,)(0*)" teksti)))
      :else (fmt/desimaaliluku teksti-ilman-pilkkua nil true))))

(defn poista-tyhjat [arvo]
  (clj-str/replace arvo #"\s" ""))

(defn paivita-raidat! [g]
  (let [paivita-luokat (fn [luokat odd?]
                         (if odd?
                           (-> luokat
                               (conj "table-default-odd")
                               (disj "table-default-even"))
                           (-> luokat
                               (conj "table-default-even")
                               (disj "table-default-odd"))))]
    (loop [[rivi & loput-rivit] (grid/nakyvat-rivit g)
           index 0]
      (if rivi
        (let [rivin-nimi (grid/hae-osa rivi :nimi)]
          (grid/paivita-grid! rivi
                              :parametrit
                              (fn [parametrit]
                                (update parametrit :class (fn [luokat]
                                                            (if (= ::valinta rivin-nimi)
                                                              (paivita-luokat luokat (not (odd? index)))
                                                              (paivita-luokat luokat (odd? index)))))))
          (recur loput-rivit
                 (if (= ::valinta rivin-nimi)
                   index
                   (inc index))))))))

(defn laajenna-solua-klikattu
  ([solu auki? dom-id polku-dataan] (laajenna-solua-klikattu solu auki? dom-id polku-dataan nil))
  ([solu auki? dom-id polku-dataan
    {:keys [aukeamis-polku sulkemis-polku]
     :or {aukeamis-polku [:.. :.. 1]
          sulkemis-polku [:.. :.. 1]}}]
   (if auki?
     (do (grid/nayta! (grid/osa-polusta solu aukeamis-polku))
         (paivita-raidat! (grid/osa-polusta (grid/root solu) polku-dataan)))
     (do (grid/piillota! (grid/osa-polusta solu sulkemis-polku))
         (paivita-raidat! (grid/osa-polusta (grid/root solu) polku-dataan))))))

(defn paivita-solun-arvo! [{:keys [paivitettava-asia arvo solu ajettavat-jarejestykset triggeroi-seuranta?]
                           :or {ajettavat-jarejestykset false triggeroi-seuranta? false}}]
  (jarjesta-data ajettavat-jarejestykset
    (triggeroi-seurannat triggeroi-seuranta?
      (grid/aseta-rajapinnan-data!
        (grid/osien-yhteinen-asia solu :datan-kasittelija)
        paivitettava-asia
        arvo
        (grid/solun-asia solu :tunniste-rajapinnan-dataan)))))

(defn tayta-alla-olevat-rivit! [asettajan-nimi rivit-alla arvo]
  (when (and arvo (not (empty? rivit-alla)))
    (doseq [rivi rivit-alla
            :let [a-sarakkeen-solu (grid/get-in-grid rivi [1])]]
      (paivita-solun-arvo! {:paivitettava-asia asettajan-nimi
                            :arvo arvo
                            :solu a-sarakkeen-solu
                            :ajettavat-jarejestykset false
                            :triggeroi-seuranta? false}))))

(defn jarjesta-fn! []
  (e! (->JarjestaData (grid/hae-osa solu/*this* :nimi))))

(defn foo* [_ _]
  (komp/luo
    (komp/piirretty (fn [_]
                      (let [dom-id "foo"
                            g (grid/grid {:nimi ::root
                                          :dom-id dom-id
                                          :root-fn (fn [] (get-in @tila [:grid]))
                                          :paivita-root! (fn [f]
                                                           (swap! tila
                                                                  (fn [tila]
                                                                    (update-in tila [:grid] f))))
                                          :alueet [{:sarakkeet [0 1] :rivit [0 3]}]
                                          :koko (-> konf/auto
                                                    (assoc-in [:rivi :nimet]
                                                              {::otsikko 0
                                                               ::data 1
                                                               ::yhteenveto 2})
                                                    (assoc-in [:rivi :korkeudet] {0 "40px"
                                                                                  2 "40px"}))
                                          :osat [(grid/rivi {:nimi ::otsikko
                                                             :koko (-> konf/livi-oletuskoko
                                                                       (assoc-in [:sarake :leveydet] {0 "3fr"
                                                                                                      3 "1fr"})
                                                                       (assoc-in [:sarake :oletus-leveys] "2fr"))
                                                             :osat (mapv (fn [nimi] (solu/otsikko {:jarjesta-fn! jarjesta-fn!
                                                                                                   :parametrit {:class #{"table-default" "table-default-header"}}
                                                                                                   :nimi nimi})
                                                                           #_(solu/teksti {:parametrit {:class #{"table-default" "table-default-header"}}}))
                                                                         [:rivi :a :b :c])
                                                             :luokat #{"salli-ylipiirtaminen"}}
                                                            [{:sarakkeet [0 4] :rivit [0 1]}])
                                                 (grid/dynamic-grid {:nimi ::data
                                                                     :alueet [{:sarakkeet [0 1] :rivit [0 1]}]
                                                                     :koko konf/auto
                                                                     :luokat #{"salli-ylipiirtaminen"}
                                                                     :osien-maara-muuttui! (fn [g _] (paivita-raidat! (grid/osa-polusta (grid/root g) [::data])))
                                                                     :toistettavan-osan-data (fn [rivit]
                                                                                               rivit)
                                                                     :toistettava-osa (fn [rivit-ryhmiteltyna]
                                                                                        (mapv (fn [[rivi rivien-arvot]]
                                                                                                (with-meta
                                                                                                  (grid/grid {:alueet [{:sarakkeet [0 1] :rivit [0 2]}]
                                                                                                              :nimi ::datarivi
                                                                                                              :koko (-> konf/auto
                                                                                                                        (assoc-in [:rivi :nimet]
                                                                                                                                  {::data-yhteenveto 0
                                                                                                                                   ::data-sisalto 1}))
                                                                                                              :luokat #{"salli-ylipiirtaminen"}
                                                                                                              :osat [(with-meta (grid/rivi {:nimi ::data-yhteenveto
                                                                                                                                            :koko {:seuraa {:seurattava ::otsikko
                                                                                                                                                            :sarakkeet :sama
                                                                                                                                                            :rivit :sama}}
                                                                                                                                            :osat [(solu/laajenna {:aukaise-fn
                                                                                                                                                                   (fn [this auki?]
                                                                                                                                                                     (laajenna-solua-klikattu this auki? dom-id [::data] #_{:sulkemis-polku [:.. :.. :.. 1]}))
                                                                                                                                                                   :auki-alussa? false
                                                                                                                                                                   :parametrit {:class #{"table-default" "lihavoitu"}}})
                                                                                                                                                   (solu/teksti {:parametrit {:class #{"table-default"}}
                                                                                                                                                                 :fmt summa-formatointi})
                                                                                                                                                   (solu/teksti {:parametrit {:class #{"table-default"}}
                                                                                                                                                                 :fmt summa-formatointi})
                                                                                                                                                   (solu/teksti {:parametrit {:class #{"table-default" "harmaa-teksti"}}
                                                                                                                                                                 :fmt summa-formatointi})]
                                                                                                                                            :luokat #{"salli-ylipiirtaminen"}}
                                                                                                                                           [{:sarakkeet [0 4] :rivit [0 1]}])
                                                                                                                                {:key (str rivi "-yhteenveto")})
                                                                                                                     (with-meta
                                                                                                                       (grid/taulukko {:nimi ::data-sisalto
                                                                                                                                       :alueet [{:sarakkeet [0 1] :rivit [0 3]}]
                                                                                                                                       :koko konf/auto
                                                                                                                                       :luokat #{"piillotettu" "salli-ylipiirtaminen"}}
                                                                                                                                      (mapv
                                                                                                                                        (fn [index]
                                                                                                                                          (with-meta
                                                                                                                                            (grid/rivi {:koko {:seuraa {:seurattava ::otsikko
                                                                                                                                                                        :sarakkeet :sama
                                                                                                                                                                        :rivit :sama}}
                                                                                                                                                        :osat [(with-meta
                                                                                                                                                                 (solu/tyhja)
                                                                                                                                                                 {:key (str rivi "-" index "-otsikko")})
                                                                                                                                                               (with-meta
                                                                                                                                                                 (g-pohjat/->SyoteTaytaAlas (gensym "a")
                                                                                                                                                                                            false
                                                                                                                                                                                            (fn [rivit-alla arvo]
                                                                                                                                                                                              (let [grid (grid/root (first rivit-alla))]
                                                                                                                                                                                                (tayta-alla-olevat-rivit! :aseta-arvo! rivit-alla arvo)
                                                                                                                                                                                                (paivita-solun-arvo! {:paivitettava-asia :aseta-arvo!
                                                                                                                                                                                                                      :arvo arvo
                                                                                                                                                                                                                      :solu solu/*this*
                                                                                                                                                                                                                      :ajettavat-jarejestykset :deep
                                                                                                                                                                                                                      :triggeroi-seuranta? true})
                                                                                                                                                                                                (grid/jarjesta-grid-data! grid
                                                                                                                                                                                                                          (keyword (str "data-" rivi)))))
                                                                                                                                                                                            {:on-change (fn [arvo]
                                                                                                                                                                                                          (when arvo
                                                                                                                                                                                                            (paivita-solun-arvo! {:paivitettava-asia :aseta-arvo!
                                                                                                                                                                                                                                 :arvo arvo
                                                                                                                                                                                                                                 :solu solu/*this*
                                                                                                                                                                                                                                 :ajettavat-jarejestykset false})))
                                                                                                                                                                                             :on-focus (fn [_]
                                                                                                                                                                                                         (grid/paivita-osa! solu/*this*
                                                                                                                                                                                                                            (fn [solu]
                                                                                                                                                                                                                              (assoc solu :nappi-nakyvilla? true))))
                                                                                                                                                                                             :on-blur (fn [arvo]
                                                                                                                                                                                                        (when arvo
                                                                                                                                                                                                          (paivita-solun-arvo! {:paivitettava-asia :aseta-arvo!
                                                                                                                                                                                                                               :arvo arvo
                                                                                                                                                                                                                               :solu solu/*this*
                                                                                                                                                                                                                               :ajettavat-jarejestykset :deep
                                                                                                                                                                                                                               :triggeroi-seuranta? true})))
                                                                                                                                                                                             :on-key-down (fn [event]
                                                                                                                                                                                                            (when (= "Enter" (.. event -key))
                                                                                                                                                                                                              (.. event -target blur)))}
                                                                                                                                                                                            {:on-change [{:positiivinen-numero {:desimaalien-maara 2}}
                                                                                                                                                                                                         {:eventin-arvo {:f poista-tyhjat}}]
                                                                                                                                                                                             :on-blur [:positiivinen-numero
                                                                                                                                                                                                       {:eventin-arvo {:f poista-tyhjat}}]}
                                                                                                                                                                                            {:size 2
                                                                                                                                                                                             :class #{"input-default"}}
                                                                                                                                                                                            summa-formatointi
                                                                                                                                                                                            summa-formatointi-aktiivinen)
                                                                                                                                                                 {:key (str rivi "-" index "-maara")})
                                                                                                                                                               (with-meta
                                                                                                                                                                 (solu/teksti {:parametrit {:class #{"table-default"}}
                                                                                                                                                                               :fmt summa-formatointi})
                                                                                                                                                                 {:key (str rivi "-" index "-yhteensa")})
                                                                                                                                                               (with-meta
                                                                                                                                                                 (solu/teksti {:parametrit {:class #{"table-default"}}})
                                                                                                                                                                 {:key (str rivi "-" index "-indeksikorjattu")})]
                                                                                                                                                        :luokat #{"salli-ylipiirtaminen"}}
                                                                                                                                                       [{:sarakkeet [0 4] :rivit [0 1]}])
                                                                                                                                            {:key (str rivi "-" index)}))
                                                                                                                                        (range 3)))
                                                                                                                       {:key (str rivi "-data-sisalto")})]})
                                                                                                  {:key rivi}))
                                                                                              rivit-ryhmiteltyna))})
                                                 (grid/rivi {:nimi ::yhteenveto
                                                             :koko {:seuraa {:seurattava ::otsikko
                                                                             :sarakkeet :sama
                                                                             :rivit :sama}}
                                                             :osat (conj (vec (repeatedly 2 (fn []
                                                                                              (solu/teksti {:parametrit {:class #{"table-default" "table-default-sum"}}}))))
                                                                         (solu/teksti {:parametrit {:class #{"table-default" "table-default-sum"}}
                                                                                       :fmt summa-formatointi})
                                                                         (solu/teksti {:parametrit {:class #{"table-default" "table-default-sum" "harmaa-teksti"}}
                                                                                       :fmt summa-formatointi}))}
                                                            [{:sarakkeet [0 4] :rivit [0 1]}])]})

                            rajapinta {:otsikot any?
                                       :yhteensarivit any?
                                       :datarivit any?
                                       :footer any?

                                       :aseta-arvo! any?
                                       :aseta-yhteenveto! any?}
                            yhteensa-data-paivitetty (fn [data]
                                                       (reduce (fn [m {:keys [rivi] :as data-map}]
                                                                 (update m rivi #(merge-with + % (dissoc data-map :rivi))))
                                                               {}
                                                               data))]
                        (e! (tuck-apurit/->MuutaTila [:grid] g))
                        (grid/rajapinta-grid-yhdistaminen! g
                                                           rajapinta
                                                           (grid/datan-kasittelija tila
                                                                                   rajapinta
                                                                                   {:otsikot {:polut [[:gridin-otsikot]]
                                                                                              :haku identity}
                                                                                    :yhteensarivit {:polut [[:data-yhteensa]]
                                                                                                    :haku identity}
                                                                                    :datarivit {:polut [[:data]]
                                                                                                :luonti-init (fn [tila data]
                                                                                                               (update tila
                                                                                                                       :data
                                                                                                                       (fn [data]
                                                                                                                         (vec (map-indexed (fn [index m]
                                                                                                                                             (assoc m ::rivitunnistin index))
                                                                                                                                           data)))))
                                                                                                :haku (fn [data]
                                                                                                        (group-by :rivi data))}
                                                                                    :footer {:polut [[:data-yhteensa]]
                                                                                             :haku (fn [data-yhteensa]
                                                                                                     (reduce-kv (fn [m _ {:keys [a b c]}]
                                                                                                                  (-> m
                                                                                                                      (update :a + a)
                                                                                                                      (update :b + b)
                                                                                                                      (update :c + c)))
                                                                                                                {:rivin-otsikko "Yhteensä"}
                                                                                                                data-yhteensa))}

                                                                                    :data-yhteenveto {:polut [[:data-yhteensa]]
                                                                                                      :luonti (fn [data-yhteensa]
                                                                                                                (vec
                                                                                                                  (map (fn [[rivin-otsikko _]]
                                                                                                                         ;; Luonnissa, luotavan nimi on tärkeä, sillä sitä vasten tarkistetaan olemassa olo
                                                                                                                         {(keyword (str "data-yhteenveto-" rivin-otsikko)) ^{:args [rivin-otsikko]} [[:data-yhteensa rivin-otsikko]]})
                                                                                                                       data-yhteensa)))
                                                                                                      :haku (fn [yhteenvetorivin-data yhteenvetorivin-nimi]
                                                                                                              (assoc yhteenvetorivin-data :rivin-otsikko yhteenvetorivin-nimi))}
                                                                                    :data-sisalto {:polut [[:data]]
                                                                                                   :luonti (fn [data]
                                                                                                             (mapv (fn [[rivin-otsikko _]]
                                                                                                                     {(keyword (str "data-" rivin-otsikko)) ^{:args [rivin-otsikko]} [[:data] [:kirjoitettu-data]]})
                                                                                                                   (group-by :rivi data)))
                                                                                                   :haku (fn [data kirjoitettu-data rivin-otsikko]
                                                                                                           (vec
                                                                                                             (keep (fn [{rivi :rivi rivitunnistin ::rivitunnistin :as rivin-data}]
                                                                                                                     (when (= rivi rivin-otsikko)
                                                                                                                       (if (contains? kirjoitettu-data rivitunnistin)
                                                                                                                         (merge rivin-data (get kirjoitettu-data rivitunnistin))
                                                                                                                         rivin-data)))
                                                                                                                   data)))
                                                                                                   :identiteetti {1 (fn [arvo]
                                                                                                                      (::rivitunnistin arvo))
                                                                                                                  2 (fn [arvo]
                                                                                                                      (key arvo))}}}

                                                                                   {:aseta-arvo! (fn [tila arvo {:keys [rivitunnistin arvon-avain]}]
                                                                                                   (let [numeerinen-arvo (try (js/Number (clj-str/replace (or arvo "") "," "."))
                                                                                                                              (catch :default _
                                                                                                                                arvo))]
                                                                                                     (-> tila
                                                                                                         (update :data (fn [data]
                                                                                                                         (mapv (fn [{tama-rivitunnistin ::rivitunnistin :as rivin-data}]
                                                                                                                                 (if (= tama-rivitunnistin rivitunnistin)
                                                                                                                                   (assoc rivin-data arvon-avain numeerinen-arvo)
                                                                                                                                   rivin-data))
                                                                                                                               data)))
                                                                                                         (assoc-in [:kirjoitettu-data rivitunnistin arvon-avain] arvo))))}
                                                                                   {:yhteenveto-seuranta {:polut [[:data]]
                                                                                                          :init (fn [tila]
                                                                                                                  (assoc tila :data-yhteensa (yhteensa-data-paivitetty (:data tila))))
                                                                                                          :aseta (fn [tila data]
                                                                                                                   (assoc tila :data-yhteensa (yhteensa-data-paivitetty data)))}
                                                                                    :b-sarakkeen-seuranta {:polut [[:data]]
                                                                                                           :luonti (fn [data]
                                                                                                                     (vec
                                                                                                                       (map-indexed (fn [index _]
                                                                                                                                      ;; Luonnissa, luotavan nimi on tärkeä, sillä sitä vasten tarkistetaan olemassa olo
                                                                                                                                      {(keyword (str "b-sarakkeen-arvo-" index)) ^{:args [index]} [[:data index :a]]})
                                                                                                                                    data)))
                                                                                                           :aseta (fn [tila a index]
                                                                                                                    (assoc-in tila [:data index :b] (* 10 a)))}})
                                                           {[::otsikko] {:rajapinta :otsikot
                                                                         :solun-polun-pituus 1
                                                                         :datan-kasittely identity}
                                                            [::yhteenveto] {:rajapinta :footer
                                                                            :solun-polun-pituus 1
                                                                            :jarjestys [[:rivin-otsikko :a :b :c]]
                                                                            :datan-kasittely (fn [yhteensa]
                                                                                               (mapv (fn [[_ nimi]]
                                                                                                       nimi)
                                                                                                     yhteensa))}
                                                            [::data] {:rajapinta :datarivit
                                                                      :solun-polun-pituus 0
                                                                      :jarjestys [{:keyfn key
                                                                                   :comp (fn [a b]
                                                                                           (compare a b))}]
                                                                      :datan-kasittely identity
                                                                      :luonti (fn [data-ryhmiteltyna-nimen-perusteella]
                                                                                (let [data-avaimet #{:rivi :a :b :c}]
                                                                                  (map-indexed (fn [index [rivin-otsikko _]]
                                                                                                 {[:. index ::data-yhteenveto] {:rajapinta (keyword (str "data-yhteenveto-" rivin-otsikko))
                                                                                                                                :solun-polun-pituus 1
                                                                                                                                :jarjestys [^{:nimi :mapit} [:rivin-otsikko :a :b :c]]
                                                                                                                                :datan-kasittely (fn [yhteenveto]
                                                                                                                                                   (mapv (fn [[_ v]]
                                                                                                                                                           v)
                                                                                                                                                         yhteenveto))
                                                                                                                                :tunnisteen-kasittely (fn [osat _]
                                                                                                                                                        (mapv (fn [osa]
                                                                                                                                                                (when (instance? solu/Syote osa)
                                                                                                                                                                  {:osa :maara
                                                                                                                                                                   :rivin-otsikko rivin-otsikko}))
                                                                                                                                                              (grid/hae-grid osat :lapset)))}
                                                                                                  [:. index ::data-sisalto] {:rajapinta (keyword (str "data-" rivin-otsikko))
                                                                                                                             :solun-polun-pituus 2
                                                                                                                             :jarjestys [{:keyfn :a
                                                                                                                                          :comp (fn [a1 a2]
                                                                                                                                                  (let [muuta-numeroksi (fn [x]
                                                                                                                                                                          (try (js/Number (clj-str/replace (or x "") "," "."))
                                                                                                                                                                               (catch :default _
                                                                                                                                                                                 x)))]
                                                                                                                                                    (compare (muuta-numeroksi a1) (muuta-numeroksi a2))))}
                                                                                                                                         ^{:nimi :mapit} [:rivi :a :b :c]]
                                                                                                                             :datan-kasittely (fn [data]
                                                                                                                                                (mapv (fn [rivi]
                                                                                                                                                        (mapv (fn [[k v]]
                                                                                                                                                                (when (contains? data-avaimet k)
                                                                                                                                                                  v))
                                                                                                                                                              rivi))
                                                                                                                                                      data))
                                                                                                                             :tunnisteen-kasittely (fn [_ data]
                                                                                                                                                     (vec
                                                                                                                                                       (map-indexed (fn [i rivi]
                                                                                                                                                                      (let [rivitunnistin (some #(when (= ::rivitunnistin (first %))
                                                                                                                                                                                                (second %))
                                                                                                                                                                                             rivi)]
                                                                                                                                                                        (vec
                                                                                                                                                                          (keep-indexed (fn [j [k _]]
                                                                                                                                                                                          (when-not (= k ::rivitunnistin)
                                                                                                                                                                                            {:rivitunnistin rivitunnistin
                                                                                                                                                                                             :arvon-avain k
                                                                                                                                                                                             :osan-paikka [i j]}))
                                                                                                                                                                                        rivi))))
                                                                                                                                                                    data)))}})
                                                                                               data-ryhmiteltyna-nimen-perusteella)))}}))))
    (fn [e*! {:keys [grid uusi-rivi] :as app}]
      ;; Asetetaan tämän nimiavaruuden e! arvoksi e*!, jotta tuota tuckin muutosfunktiota ei tarvitse passata jokaiselle komponentille
      (set! e! e*!)
      [:div
       ;; Piirretään grid, vasta kun sen määrittäminen on valmis
       (if grid
         [grid/piirra grid]
         [:span "Odotellaan..."])
       [:div {:style {:margin-top "15px" :margin-bottom "5px"}}
        [:label {:for "rivin-nimi"} "Rivin nimi"]
        ;; Tällä voi lisätä uuden datarivin, joka sitten näytetään UI:lla
        [:input#rivin-nimi {:on-change #(e! (->MuokkaaUudenRivinNimea (.. % -target -value)))
                            ;; Uuden rivin voi lisätä painamalla entteriä
                            :on-key-down (fn [event]
                                           (when (= "Enter" (.. event -key))
                                             (e! (->LisaaRivi))))
                            :value uusi-rivi
                            :style {:display "block"}}]]
       [napit/uusi "Lisää rivi"
        (fn []
          ;; Uuden rivin voi lisätä painamalla nappia
          (e! (->LisaaRivi)))]])))

(defn foo []
  [tuck/tuck tila foo*])