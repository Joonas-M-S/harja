(ns harja.ui.kentat-test
  "Lomakekenttien komponenttitestejä"
  (:require [harja.ui.kentat :as kentat]
            [cljs.test :as t :refer-macros [deftest is testing async]]
            [harja.testutils :as u]
            [cljs.core.async :as async]
            [reagent.core :as r]
            [cljs-react-test.simulate :as sim]
            [harja.pvm :as pvm]
            [clojure.string :as str])
  (:require-macros [harja.testutils.macros :refer [komponenttitesti]]))

(t/use-fixtures :each
  u/komponentti-fixture
  u/fake-palvelut-fixture)

(deftest valinta
 (let [data (r/atom nil)]
  (komponenttitesti
   [kentat/tee-kentta {:nimi :foo :tyyppi :valinta
                       :valinta-nayta #(if (nil? %) "Valitse" %)
                       :valinnat ["abc" "kissa kävelee" "tikapuita pitkin taivaseen"]}
    data]

   "Aluksi arvo on Valitse ja data nil"
   (is (= "Valitse" (u/text :.valittu)))
   (is (nil? @data))
   --

   "Ennen klikkaamistakin kolme vaikkei näkyvissä"
   (is (= 3 (count (u/sel :li.harja-alasvetolistaitemi))))

   "Ennen klikkaamista valinnat eivät ole näkyvissä"
   (is (nil? (u/sel1 :div.dropdown.open)))

   "Klikkaaminen avaa pulldownin"
   (u/click :button.nappi-alasveto)
   --
   (is (= 3 (count (u/sel :li.harja-alasvetolistaitemi))))
   (is (some? (u/sel1 :div.dropdown.open)))

   "Valitaan kissa kävelee"
   (u/click ".harja-alasvetolistaitemi:nth-child(2) > a")
   --
   (is (= "kissa kävelee" (u/text :.valittu)))
   (is (= @data "kissa kävelee"))

   "Valinnan jälkeen lista piiloon"
   (is (nil? (u/sel1 :div.dropdown.open))))))


(deftest numero
  (let [data (r/atom nil)
        val! #(u/change :input %)
        val #(some-> :input u/sel1 .-value)]
    (komponenttitesti
     [kentat/tee-kentta {:desimaalien-maara 2
                         :nimi :foo :tyyppi :numero}
      data]

     "aluksi arvo on tyhjä"
     (is (= "" (val)))

     "Normaali kokonaisluku päivittyy oikein"
     (val! "80")
     --
     (is (= "80" (val)))
     (is (= 80 @data))

     "Keskeneräinen numero ei päivitä dataa"
     (val! "-")
     --
     (is (= "-" (val)))
     (is (nil? @data))

     "Negatiivinen luku"
     (val! "-42")
     --
     (is (= "-42" (val)))
     (is (= -42 @data))

     "Keskeneräinen desimaaliluku"
     (val! "0.")
     --
     (is (= "0." (val)))
     (is (zero? @data))

     "Desimaaliluku"
     (val! "0.42")
     --
     (is (= "0.42" (val)))
     (is (= 0.42 @data))

     "Kentän blur poistaa tekstin"
     (sim/blur (u/sel1 :input) nil)
     --
     (is (= "0,42" (val)))

     "Datasta tuleva arvo päivittää tekstin"
     (reset! data 0.66)
     --
     (is (= "0,66" (val))))))

(deftest pvm
  (let [data (r/atom nil)
        val! #(u/change :input %)
        val #(some-> :input u/sel1 .-value)]
    (komponenttitesti
     [kentat/tee-kentta {:tyyppi :pvm :placeholder "anna pvm"} data]

     "Alkutilanteessa arvo on tyhjä ja placeholder on asetettu"
     (is (= "" (val)))
     (is (= "anna pvm" (.getAttribute (u/sel1 :input) "placeholder")))

     "Virheellistä tekstiä ei voi syöttää"
     (val! "66...")
     --
     (is (= "" (val)))
     (is (nil? @data))

     "Keskeneräinen pvm on ok"
     (val! "12.")
     --
     (is (= "12." (val)))
     (is (nil? @data))

     "Täytetty pvm asettaa arvon"
     (val! "7.7.2010")
     --
     (is (= "7.7.2010" (val)))
     (is (nil? @data)) ;; arvoa ei aseteta ennen blur tai selectiä
     (sim/blur (u/sel1 :input) nil)
     --
     (is (= (pvm/->pvm "7.7.2010") @data))

     "Picker ei ole näkyvissä"
     (is (nil? (u/sel1 :table.pvm-valinta)))

     "Klikkauksesta picker tulee näkyviin"
     (u/click :input)
     --
     (is (u/sel1 :table.pvm-valinta))

     "Seuraava kk napin klikkaaminen elokuun 2010"
     (u/click :.pvm-seuraava-kuukausi)
     --
     (is (= "Elo 2010" (u/text ".pvm-kontrollit tr td:nth-child(2)")))

     "Viidestoista päivä klikkaus (su, 3. rivi)"
     (u/click ".pvm-paivat tr:nth-child(3) td:nth-child(7)")
     --
     (is (= "15.08.2010" (val)))
     (is (pvm/sama-pvm? (pvm/->pvm "15.8.2010") @data)))))

(def +tie20-osa1-alkupiste+ {:type :point, :coordinates [426938.1807000004 7212765.558800001]})
(def +tr-vastaukset+ {{:alkuosa 1, :numero 20, :alkuetaisyys 0}
                      [+tie20-osa1-alkupiste+]})

(deftest tierekisteriosoite
  (let [data (r/atom nil)
        sijainti (r/atom nil)
        tr-sel {:tr-numero :input.tr-numero
                :tr-alkuosa :input.tr-alkuosa
                :tr-alkuetaisyys :input.tr-alkuetaisyys
                :tr-loppuosa :input.tr-loppuosa
                :tr-loppuetaisyys :input.tr-loppuetaisyys}
        tr-kentat [:tr-numero :tr-alkuosa :tr-alkuetaisyys :tr-loppuosa :tr-loppuetaisyys]
        arvo (fn [kentta]
               (.-value (u/sel1 (tr-sel kentta))))
        aseta! (fn [kentta arvo]
                 (u/change (tr-sel kentta) arvo))
        hae-tr-viivaksi (u/fake-palvelukutsu
                         :hae-tr-viivaksi
                         (fn [payload]
                           (.log js/console ":hae-tr-viivaksi => " payload)
                           (get +tr-vastaukset+ payload)))]
    (komponenttitesti
     [kentat/tee-kentta {:tyyppi :tierekisteriosoite :sijainti sijainti} data]

     "Alkutilassa kaikki kentät ovat tyhjiä"
     (is (every? str/blank? (map arvo tr-kentat)))
     (aseta! :tr-numero "20")
     --
     (is (= "20" (arvo :tr-numero)))

     "Tien sekä alkuosan ja -etäisyyden asettaminen hakee osoitteen"
     (aseta! :tr-alkuosa "1")
     (aseta! :tr-alkuetaisyys "0")
     --
     (u/blur (tr-sel :tr-alkuetaisyys))
     --
     (<! hae-tr-viivaksi)
     --
     (is (= @sijainti +tie20-osa1-alkupiste+) "Sijainti on päivittynyt oikein")
     )))
