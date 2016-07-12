(ns harja.palvelin.integraatiot.api.sanomat.paallystysilmoitus
  (:require [cheshire.core :as cheshire]
            [harja.domain.paallystys-ja-paikkaus :as paallystys-ja-paikkaus]
            [harja.domain.paallystysilmoitus :as paallystysilmoitus]))

(defn rakenna-alikohteet [paallystysilmoitus]
  (mapv (fn [alikohde]
          (let [kivi (:kivi-ja-sideaine (first (:kivi-ja-sideaineet alikohde)))]
            {:kohdeosa-id (:id alikohde)
             :rc% (:rc-prosentti alikohde)
             :leveys (:leveys alikohde)
             :km-arvo (:km-arvo kivi)
             :raekoko (:raekoko alikohde)
             :pinta-ala (:pinta-ala alikohde)
             :kuulamylly (paallystysilmoitus/kuulamylly-koodi-nimella (:kuulamylly alikohde))
             :esiintyma (:esiintyma kivi)
             :muotoarvo (:muotoarvo kivi)
             :pitoisuus (:pitoisuus kivi)
             :lisaaineet (:lisa-aineet kivi)
             :massamenekki (:massamenekki alikohde)
             :tyomenetelma (paallystysilmoitus/tyomenetelman-koodi-nimella (:tyomenetelma alikohde))
             :sideainetyyppi (paallystysilmoitus/sideainetyypin-koodi-nimella (:sideainetyyppi kivi))
             :paallystetyyppi (paallystys-ja-paikkaus/hae-koodi-apin-paallysteella (:paallystetyyppi alikohde))
             :kokonaismassamaara (:kokonaismassamaara alikohde)
             :edellinen-paallystetyyppi (paallystys-ja-paikkaus/hae-koodi-apin-paallysteella (:edellinen-paallystetyyppi alikohde))}))
        (get-in paallystysilmoitus [:yllapitokohde :alikohteet])))

(defn rakenna-alustatoimet [paallystysilmoitus]
  (mapv
    (fn [alustatoimi]
      (let [sijainti (:sijainti alustatoimi)]
        {:aosa (:aosa sijainti)
         :aet (:aet sijainti)
         :losa (:losa sijainti)
         :let (:let sijainti)
         :paksuus (:paksuus alustatoimi)
         :verkkotyyppi (paallystysilmoitus/verkkotyyppi-koodi-nimella (:verkkotyyppi alustatoimi))
         :verkon-sijainti (paallystysilmoitus/verkon-sijainti-koodi-nimella (:verkon-sijainti alustatoimi))
         :verkon-tarkoitus (paallystysilmoitus/verkon-tarkoitus-koodi-nimella (:verkon-tarkoitus alustatoimi))
         :kasittelymenetelma (paallystysilmoitus/alustamenetelma-koodi-nimella (:kasittelymenetelma alustatoimi))
         :tekninen-toimenpide (paallystysilmoitus/tekninentoimenpide-koodi-nimella (:tekninen-toimenpide alustatoimi))}))
    (:alustatoimenpiteet paallystysilmoitus)))

(defn rakenna-tyot [paallystysilmoitus]
  (mapv (fn [tyo]
          {:tyo (:tyotehtava tyo)
           :tyyppi (paallystysilmoitus/paallystystyontyyppi-avain-nimella (:tyyppi tyo))
           :yksikko (:yksikko tyo)
           :yksikkohinta (:yksikkohinta tyo)
           :tilattu-maara (:tilattu-maara tyo)
           :toteutunut-maara (:tilattu-maara tyo)})
        (:tyot paallystysilmoitus)))

(defn rakenna [paallystysilmoitus]
  (let [data {:osoitteet (rakenna-alikohteet paallystysilmoitus)
              :alustatoimet (rakenna-alustatoimet paallystysilmoitus)
              :tyot (rakenna-tyot paallystysilmoitus)}]
    (cheshire/encode data)))