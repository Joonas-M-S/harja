(ns harja.tiedot.urakka.yllapitokohteet.paikkaukset.paikkaukset-paikkauskohteet-kartalle
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [log]]
            [taoensso.timbre :as log]
            [harja.ui.kartta.esitettavat-asiat :refer [maarittele-feature kartalla-esitettavaan-muotoon]]
            [harja.ui.kartta.asioiden-ulkoasu :as asioiden-ulkoasu]
            [harja.ui.kartta.ikonit :refer [sijainti-ikoni pinni-ikoni nuoli-ikoni]])
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [reagent.ratom :refer [reaction]]))

(defn tila->vari [tila]
  (let [vari (case tila
               "ehdotettu" "#f1b371"
               "tilattu" "#274ac6"
               "valmis" "#58a006"
               :default "#f1b371")
        _ (js/console.log "vari->tila saatiin" (pr-str tila) "annettiin väri:" (pr-str vari))]
    vari))
(def karttataso-paikkauskohteet (atom []))
#_(def karttataso-paikkauskohteet (atom {:paikkauskohteet [{:id 1
                                                            :testinro "14566"
                                                            :testinimi "Kaislajärven suora"
                                                            :testitila "Ehdotettu"
                                                            :testimenetelma "UREM"
                                                            :testisijainti "23423/121231"
                                                            :testiaikataulu "1.6. - 31.8.2021 (arv.)"
                                                            :tierekisteriosoite {:tie 20, :aosa 19, :aet 1, :losa 19, :let 301}
                                                            :sijainti {:type :multiline
                                                                       :tyyppi-kartalla :paikkaukset-paikkauskohteet
                                                                       :stroke {:width 8
                                                                                :color (tila->vari "Ehdotettu")}
                                                                       :infopaneelin-tiedot {:nimi "Kaislajärven suora"}
                                                                       :ikonit {:tyyppi :merkki
                                                                                :paikka [:loppu]
                                                                                :zindex 21
                                                                                :img (pinni-ikoni "sininen")}

                                                                       :lines [{:type :line,
                                                                                :points [[454011.25093926466 7253055.999241401]
                                                                                         [459012.2494422446 7273055.94454406]]}]}}
                                                           {:id 2
                                                            :testinro "14567"
                                                            :testinimi "Mount Utajärvi VT1"
                                                            :testitila "Tilattu"
                                                            :testimenetelma "KTVA"
                                                            :testisijainti "23423/121231"
                                                            :testiaikataulu "1.6. - 31.8.2021 (arv.)"
                                                            :sijainti {:type :multiline,
                                                                       :tyyppi-kartalla :paikkaukset-paikkauskohteet
                                                                       :stroke {:width 8
                                                                                :color (tila->vari "Tilattu")}
                                                                       :infopaneelin-tiedot {:nimi "Mount Utajärvi VT1"}
                                                                       :lines [{:type :line
                                                                                :points [[449367.5486456644 7225602.7854130855] [449375.5789999999 7225606.495999999] [449426.18099999987 7225629.328000002] [449521.94600000046 7225673.467]
                                                                                         [449577.98000000045 7225699.182999998] [449633.01300000027 7225724.052000001] [449685.949 7225748.515000001] [449727.75100000016 7225767.732000001]
                                                                                         [449782.64800000004 7225793.627] [449820.1270000003 7225809.991999999] [449863.4060000004 7225830.239] [449905.9840000002 7225850.177000001] [449953.2750000004 7225871.772]
                                                                                         [449988.0449999999 7225887.306000002] [450029.12200000044 7225906.324000001] [450080.31799999997 7225929.9690000005] [450124.2039999999 7225949.818999998]
                                                                                         [450170.69299999997 7225971.204] [450216.2759999996 7225992.478999998] [450257.6540000001 7226011.5] [450305.44600000046 7226033.401000001] [450346.9230000004 7226052.623]
                                                                                         [450397.4210000001 7226075.658] [450430.88999999966 7226090.375] [450448.5259999996 7226098.298999999] [450583.7050000001 7226157.166000001] [450695.91899999976 7226207.318999998]
                                                                                         [450764.07799999975 7226237.147] [450783.3130000001 7226245.543000001] [450819.06599999964 7226261.149] [450874.0549999997 7226285.353] [450935.43599999975 7226312.875999998] [450946.2520000003 7226317.734999999] [450960.801 7226324.364] [450974.0959999999 7226329.333999999] [451011.9110000003 7226346.127] [451100.5650000004 7226384.57] [451175.4040000001 7226418.061999999] [451240.82100000046 7226446.787999999] [451348.58499999996 7226494.046] [451457.29200000037 7226541.237] [451543.7000000002 7226577.364999998] [451609.50100000016 7226604.809] [451632.0219999999 7226614.030000001] [451697.8729999997 7226640.807999998] [451752.05200000014 7226662.831] [451797.35500000045 7226679.471999999] [451830.642 7226690.8429999985] [451886.52300000004 7226708.965] [451932.9670000002 7226722.638999999] [451988.4809999997 7226738.949000001] [452054.6339999996 7226757.732999999] [452116.66899999976 7226776.177999999] [452172.4500000002 7226792.116999999] [452252.00600000005 7226815.690000001] [452287.7479999997 7226826.495999999] [452357.03000000026 7226846.827] [452403.5530000003 7226860.511] [452449.3590000002 7226874.239999998] [452455.2209999999 7226875.936999999] [452505.65699999966 7226890.535] [452564.47200000007 7226908.228999998] [452654.49399999995 7226934.441] [452710.3710000003 7226951.098000001] [452757.216 7226965.059] [452758.15199999977 7226965.331] [452768.05700000003 7226968.201000001] [452850.25600000005 7226992.022999998] [452882.90699999966 7227001.688999999] [452928.90500000026 7227015.304000001] [452939.3150000004 7227018.300000001] [452998.42200000025 7227035.318] [453072.5070000002 7227058.186999999] [453260.43400000036 7227112.723000001] [453564.9129999997 7227201.949000001] [453769.0329999998 7227260.627] [453836.2240000004 7227280.822999999] [453838.5120000001 7227281.471999999] [453936.801 7227309.355999999] [454204.40699999966 7227386.484000001] [454343.03199999966 7227426.522999998] [454486.08100000024 7227468.3379999995] [454623.1330000004 7227508.443999998] [454674.1038430387 7227522.976625074]]}]}}
                                                           {:id 3
                                                            :testinro "14568"
                                                            :testinimi "Kiikelin monttu"
                                                            :testitila "Valmis"
                                                            :testimenetelma "SIP"
                                                            :testisijainti "23423/121231"
                                                            :testiaikataulu "1.6. - 14.6.2021"
                                                            :sijainti {:type :multiline,
                                                                       :tyyppi-kartalla :paikkaukset-paikkauskohteet
                                                                       :stroke {:width 8
                                                                                :color (tila->vari "Valmis")}
                                                                       :ikonit [{:tyyppi :merkki
                                                                                 :paikka [:loppu]
                                                                                 ;:scale (laske-skaala valittu?)
                                                                                 :zindex 21
                                                                                 :img (pinni-ikoni "sininen")}]
                                                                       :infopaneelin-tiedot {:nimi "Kiikelin monttu"}
                                                                       :lines [{:type :line,
                                                                                :points [[453312.5618892033 7191846.896453289] [453312.7240000004 7191846.965999998] [453344.95600000024 7191859.495000001] [453372.8250000002 7191871.572999999] [453416.4009999996 7191889.352000002] [453448.96800000034 7191903.333999999] [453489.9009999996 7191919.546999998] [453507.4380000001 7191927.055] [453510.3150000004 7191928.285999998] [453546.7719999999 7191943.329] [453563.75100000016 7191951.136] [453597.51499999966 7191964.851] [453619.4110000003 7191974.179000001] [453637.64499999955 7191982.124000002] [453660.2790000001 7191991.693999998] [453693.5099999998 7192004.498] [453710.0870000003 7192010.8999999985] [453723.3909999998 7192014.923] [453738.13100000005 7192019.500999998] [453746.8150000004 7192021.785] [453754.1289999997 7192023.743000001] [453765.29700000025 7192026.272] [453785.44799999986 7192030.197999999] [453803.84499999974 7192033.013999999] [453822.00299999956 7192035.432] [453838.56400000025 7192036.581] [453857.75299999956 7192037.563999999] [453878.29200000037 7192037.748] [453896.2810000004 7192036.664999999] [453914.6679999996 7192035.66] [453928.0389999999 7192034.59] [453942.28500000015 7192033.199999999] [453966.9610000001 7192031.938999999] [453991.3169999998 7192030.202] [454027.716 7192027.364999998] [454089.88900000043 7192022.826000001] [454154.83100000024 7192018.614999998] [454190.4900000002 7192016.131999999] [454223.05200000014 7192014.984999999] [454267.2599999998 7192016.206] [454295.44799999986 7192018.835999999] [454329.21800000034 7192024.476] [454358.69400000013 7192032.195999999] [454385.8909999998 7192041.710000001] [454421.1950000003 7192055.623] [454460.4840000002 7192071.037999999] [454485.98699999973 7192081.4750000015] [454512.76400000043 7192092.227000002] [454560.18200000003 7192111.041999999] [454591.3439999996 7192124.171] [454613.8990000002 7192133.739999998] [454629.61899999995 7192140.136999998]]} {:type :line, :points [[454629.61899999995 7192140.136999998] [454675.40199999977 7192159.000999998] [454709.5120000001 7192172.598999999] [454728.78718418215 7192180.315676483]]}]}}]}))
(defonce karttataso-nakyvissa? (atom true))
;; Tehdään set, jossa on määriteltynä mitä kohteita kartalla näytetään
;; Mikäli mitään ei ole valittu, näytetään kaikki
(defonce valitut-kohteet-atom (atom #{}))

(defonce paikkauskohteet-kartalla
         (reaction
           (let [;; Testataan mahdollisuutta näyttää vain valittu kohde kartalle
                 _ (js/console.log "paikkauskohteet-kartalla: kohteet" (pr-str @karttataso-paikkauskohteet))
                 valitut-kohteet (if (= (count @valitut-kohteet-atom) 0)
                                   nil
                                   @valitut-kohteet-atom)
                 kohteet (keep (fn [kohde]
                                 (when
                                   (and (or (nil? valitut-kohteet)
                                            (contains? valitut-kohteet (:id kohde)))
                                        (:sijainti kohde))
                                   kohde))
                               @karttataso-paikkauskohteet)
                 _ (js/console.log "valitut-kohteet:" (pr-str valitut-kohteet)
                                   "valitut-kohteet-atom:" (pr-str @valitut-kohteet-atom))
                 ;alueet
                 #_(with-meta (mapv (fn [kohde]
                                      (when (:sijainti kohde)
                                        {:alue (merge {:stroke {:width 8
                                                                :color (tila->vari "Valmis")}}
                                                      (:sijainti kohde))
                                         :tyyppi-kartalla :paikkaukset-paikkauskohteet
                                         :stroke {:width 8
                                                  :color (tila->vari "Valmis")}
                                         :selite {:teksti "Paikkauskohde jee"
                                                  :img (pinni-ikoni "sininen")}
                                         :infopaneelin-tiedot {:nro (:nro kohde)
                                                               :nimi (:nimi kohde)
                                                               :tila (:paikkauskohteen-tila kohde)
                                                               :menetelma (:tyomenetelma kohde)
                                                               :aikataulu (:formatoitu-aikataulu kohde)}
                                         :ikonit [{:tyyppi :merkki
                                                   :paikka [:loppu]
                                                   :zindex 21
                                                   :img (pinni-ikoni "sininen")}]}))
                                    kohteet)
                              {:selitteet [{:vari (map :color asioiden-ulkoasu/paikkaukset)
                                            :teksti "Paikkauskohteet"}]})]
             (when (and (not-empty kohteet) @karttataso-nakyvissa?)
               (with-meta (mapv (fn [kohde]
                                  (when (:sijainti kohde)
                                    {:alue (merge #_ {:stroke {:width 8
                                                            :color (tila->vari "Valmis")}}
                                                  (:sijainti kohde))
                                     :tyyppi-kartalla :paikkaukset-paikkauskohteet
                                     :stroke {:width 8
                                              :color (tila->vari (:paikkauskohteen-tila kohde))}
                                     :selite {:teksti "Paikkauskohde jee"
                                              :img (pinni-ikoni "sininen")}
                                     :infopaneelin-tiedot {:nro (:nro kohde)
                                                           :nimi (:nimi kohde)
                                                           :tila (:paikkauskohteen-tila kohde)
                                                           :menetelma (:tyomenetelma kohde)
                                                           :aikataulu (:formatoitu-aikataulu kohde)}
                                     :ikonit [{:tyyppi :merkki
                                               :paikka [:loppu]
                                               :zindex 21
                                               :img (pinni-ikoni "sininen")}]}))
                                kohteet)
                          {:selitteet [{:vari (map :color asioiden-ulkoasu/paikkaukset)
                                        :teksti "Paikkauskohteet"}]})))))