(ns harja-laadunseuranta.testidata
  "Testidataa yksikkötestejä varten"
  (:require [harja.testi :refer [db]]
            [harja-laadunseuranta.tietokanta :as tietokanta]
            [clj-time.coerce :as c]
            [clj-time.core :as time]))

(tietokanta/aseta-tietokanta! db)

(def havainto
  {:id 1
   :sijainti [65 25]

   :havainnot [17 20 :epatasainen-polanne]

   ;; seuraavat voivat olla nil jos ei annettu
   :kitkamittaus 0.3
   :lampotila 12
   :tasaisuus 2
   :lumisuus 3
   :kuvaus "foo"
   :kuva 2})

(def mockattu-tierekisteri
  {;; Tarkastus samalla tiellä samaan suuntaan
   [464681.5999816895 7230492.000024414] {:tie 20, :aosa 10, :aet 3921}
   [465321.5999816895 7230676.000024414] {:tie 20, :aosa 10, :aet 4587}
   [465641.5999816895 7230780.000024414] {:tie 20, :aosa 10, :aet 4924}
   [466089.5999816895 7230916.000024414] {:tie 20, :aosa 10, :aet 5392}
   [466409.5999816895 7230996.000024414] {:tie 20, :aosa 11, :aet 5721}
   [467009.5999816895 7231180.000024414] {:tie 20, :aosa 11, :aet 6349}
   [467257.5999816895 7231252.000024414] {:tie 20, :aosa 11, :aet 6607}
   [467545.5999816895 7231332.000024414] {:tie 20, :aosa 11, :aet 6906}
   [467753.5999816895 7231404.000024414] {:tie 20, :aosa 11, :aet 7126}

   ;; Toinen tarkastus, niin ikään samalla tiellä samaan suuntaan
   [468064.79999084474 7231495.200001526] {:tie 20, :aosa 11, :aet 237}
   [468256.79999084474 7231555.200001526] {:tie 20, :aosa 11, :aet 438}
   [468568.79999084474 7231643.200001526] {:tie 20, :aosa 11, :aet 762}
   [468768.79999084474 7231707.200001526] {:tie 20, :aosa 11, :aet 972}
   [468932.79999084474 7231763.200001526] {:tie 20, :aosa 11, :aet 1146}
   [469168.79999084474 7231867.200001526] {:tie 20, :aosa 11, :aet 1404}
   [469300.79999084474 7231931.200001526] {:tie 20, :aosa 11, :aet 1551}
   [469492.79999084474 7232011.200001526] {:tie 20, :aosa 11, :aet 1759}

   ;; Tarkastus, jossa ajetaan risteykseen ja tie vaihtuu
   [455421.19997024536 7227742.400009155] {:tie 20, :aosa 7, :aet 8847}
   [455493.19997024536 7227764.400009155] {:tie 20, :aosa 7, :aet 8923}
   [455505.19997024536 7227760.400009155] {:tie 20, :aosa 7, :aet 8933}
   [455519.19997024536 7227720.400009155] {:tie 8341, :aosa 4, :aet 2597}
   [455555.19997024536 7227594.400009155] {:tie 8341, :aosa 4, :aet 2466}

   ;; Tarkastus, jossa käännytään ympäri
   [454881.19997024536 7227586.400009155] {:tie 20, :aosa 7, :aet 8285}
   [455041.19997024536 7227634.400009155] {:tie 20, :aosa 7, :aet 8452}
   [455165.19997024536 7227666.400009155] {:tie 20, :aosa 7, :aet 8580}
   [455041.19997024536 7227634.400009156] {:tie 20, :aosa 7, :aet 8453}
   [454881.19997024536 7227586.400009157] {:tie 20, :aosa 7, :aet 8284}

   ;; Tarkastus, jossa ajetaan tiellä 20 osalta 1 osalle 3 (osaa 2 ei ole olemassa)
   [430569 7212525] {:tie 20, :aosa 1, :aet 3671}
   [430599 7212541] {:tie 20, :aosa 1, :aet 3705}
   [430665 7212590] {:tie 20, :aosa 3, :aet 18}
   [430702 7212607] {:tie 20, :aosa 3, :aet 58}

   ;; Tarkastus, jossa ajetaan kevyen liikenteen väylää 70012 ja tulee iso osahyppy.
   ;; Väliin jäävät osat sijaitsevat maantieteellisesti jossain muualla.
   [427949 6761002] {:tie 70012, :aosa 443, :aet 38}
   [427950 6761002] {:tie 70012, :aosa 443, :aet 38}
   [427951 6761002] {:tie 70012, :aosa 443, :aet 39}
   [427974 6761011] {:tie 70012, :aosa 491, :aet 219}
   [427989 6761012] {:tie 70012, :aosa 491, :aet 234}
   [428006 6761010] {:tie 70012, :aosa 491, :aet 250}})

(def tarkastukset-joissa-jatkuvat-havainnot-muuttuu
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [; tarkastus 1
   {:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot []}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot []}

   ;; tarkastus 2
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.2}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [17]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [17]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [17]}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [17]}
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.3}

   ;; tarkastus 3
   {:id 8 :sijainti [467753.5999816895 7231404.000024414]
    :jatkuvat-havainnot []}])

(def tarkastus-jossa-lumisuus
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :lumisuus 1}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]
    :lumisuus 2}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]}
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :kitkamittaus 0.3
    :lumisuus 3}])

(def tarkastus-jossa-talvihoito-tasaisuus
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :talvihoito-tasaisuus 10}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]}
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :talvihoito-tasaisuus 100}])

(def tarkastus-jossa-soratie-tasaisuus
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 3}])

(def tarkastus-jossa-soratie-tasaisuus-jatkuu
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 2}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 2}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 3
    :polyavyys 1}])

(def tarkastus-jossa-soratie-kiinteys
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :kiinteys 3
    :soratie-tasaisuus 3}])

(def tarkastus-jossa-soratie-kiinteys-jatkuu-vaikka-gps-sekoaa
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   {:id 4 :sijainti nil ;; GPS sekoaa, mutta sama tarkastus jatkuu kunnes todistettavasti on syytä katkaista
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   {:id 5 :sijainti nil
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :kiinteys 3}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1}])

(def tarkastus-jossa-soratie-polyavyys
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :polyavyys 1
    :kiinteys 3}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]
    :polyavyys 3}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :kiinteys 3
    :soratie-tasaisuus 3}])

(def tarkastus-jossa-soratie-sivukaltevuus
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [3]
    :soratie-tasaisuus 1
    :sivukaltevuus 3}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [3]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [3]}
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [3]}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [3]}
   ;; -- Mittausarvot muuttuu, uusi tarkastus alkaa
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [3]
    :sivukaltevuus 3
    :soratie-tasaisuus 3}])

(def tarkastus-jossa-piste-ei-osu-tielle
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.3}
   ;; Mittausvirhe: piste ei osu tielle. Silti osa samaa tarkastusta, joka jatkuu tämän pisteen jälkeenkin.
   {:id 1 :sijainti nil
    :jatkuvat-havainnot [17]}
   {:id 2 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.3}])

(def tarkastus-jossa-sijainti-puuttuu
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot [17]}
   {:id 1 :sijainti nil
    :jatkuvat-havainnot [17]}
   {:id 2 :sijainti nil
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.3}])

(def tarkastus-jossa-sijainti-puuttuu-alusta
  [{:id 0 :sijainti nil
    :jatkuvat-havainnot [17]}
   ;; Tullaan pimennosta tielle --> reitti katkeaa
   {:id 1 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot [17]}
   ;; Pimentoon --> reitin on tarkoitus jatkua kunnes tie oikeasti vaihtuu
   {:id 2 :sijainti nil
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.3}])

(def tarkastus-jossa-ajallinen-aukko
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/now))}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 3)))}
   ;; GPS-signaali katkeaa tässä joksikin aikaa
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 190)))}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 250)))}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 290)))}])

(def tarkastus-jossa-ajallinen-aukko-ja-sitten-tyhja-tie
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/now))}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 3)))}
   ;; GPS-signaali katkeaa tässä joksikin aikaa
   {:id 2 :sijainti nil
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 190)))}
   {:id 3 :sijainti nil
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 250)))}
   ;; Tullaan pimennosta tielle --> reitin tulee katketa ja alkaa tästä uusi
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 290)))}
   ;; Tie vaihtuu
   {:id 5 :sijainti [455519.19997024536 7227720.400009155]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 291)))}
   {:id 6 :sijainti [455555.19997024536 7227594.400009155]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 292)))}
   ;; Tie vaihtuu taas
   {:id 7 :sijainti [454881.19997024536 7227586.400009155]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 293)))}
   {:id 8 :sijainti [455041.19997024536 7227634.400009155]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 294)))}])

(def tarkastus-jossa-tyhja-tie-ja-sitten-ajallinen-aukko-tielle
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/now))}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 3)))}
   {:id 2 :sijainti nil
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 4)))}
   ;; GPS-signaali katkeaa tässä joksikin aikaa
   {:id 3 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 190)))}
   {:id 4 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 250)))}
   {:id 5 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 290)))}
   ;; Tie vaihtuu
   {:id 6 :sijainti [455519.19997024536 7227720.400009155]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 292)))}])

(def tarkastus-jossa-tyhja-tie-ja-sitten-ajallinen-aukko-ja-sitten-tyhja-tie-ja-sitten-tie
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/now))}
   {:id 1 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 5)))}
   {:id 2 :sijainti nil
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 190)))}
   ;; GPS-signaali katkeaa tässä joksikin aikaa
   {:id 3 :sijainti nil
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 190)))}
   ;; Tullaan pimennosta tielle --> reitin tulee katketa ja alkaa tästä uusi
   {:id 4 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 250)))}
   {:id 5 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 290)))}
   ;; Tie vaihtuu
   {:id 6 :sijainti [455519.19997024536 7227720.400009155]
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 295)))}])

(def tarkastus-jossa-tie-vaihtuu
  [{:id 0 :sijainti [455421.19997024536 7227742.400009155]
    :jatkuvat-havainnot []}
   {:id 1 :sijainti [455493.19997024536 7227764.400009155]
    :jatkuvat-havainnot []}
   {:id 2 :sijainti [455505.19997024536 7227760.400009155]
    :jatkuvat-havainnot []}
   {:id 3 :sijainti [455519.19997024536 7227720.400009155]
    :jatkuvat-havainnot []}
   {:id 4 :sijainti [455555.19997024536 7227594.400009155]
    :jatkuvat-havainnot []}])

(def tarkastus-jossa-pistemainen-havainto
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [;; tarkastus 1
   {:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.2}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot [17]}
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [17]}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [17]}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [17]}

   ;; tarkastus 2
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.2
    :pistemainen-havainto 20}

   ;; tarkastus 1 jatkuu...
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.3}])

(def monipuolinen-tarkastus
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [; tarkastus 1 alkaa
   {:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}

   ;; tarkastus 1 päättyy, sillä liukkaus alkaa. Tästä alkaa
   ;; uusi tarkastus 2, jossa liukasta ja kaksi kitkamittausta
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [17]
    :pistemainen-havainto nil}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [17]
    :pistemainen-havainto nil
    :kitkamittaus 0.2}

   ;; tarkastus 3, jossa pistemäinen havainto
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [17]
    :pistemainen-havainto 20}

   ;; tarkastus 2 jatkuu, sillä jatkuvat havainnot eivät muutu
   {:id 5 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [17]
    :pistemainen-havainto nil
    :kitkamittaus 0.4}
   {:id 6 :sijainti [467257.5999816895 7231252.000024414]
    :jatkuvat-havainnot [17]
    :pistemainen-havainto nil}
   {:id 7 :sijainti [467545.5999816895 7231332.000024414]
    :jatkuvat-havainnot [17]
    :pistemainen-havainto nil}

   ;; tarkastus 3 päättyy, sillä liukkaus päättyy.
   ;; Tässä uusi tarkastus 4, jossa pistemäinen havainto
   {:id 8 :sijainti [467753.5999816895 7231404.000024414]
    :jatkuvat-havainnot []
    :pistemainen-havainto 20}

   ;; tarkastus 5, alkaa uudelta tieosalta
   {:id 9 :sijainti [468064.79999084474 7231495.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}
   {:id 10 :sijainti [468256.79999084474 7231555.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}

   ;; tarkastus 6, kuva ja seliteteksti
   {:id 11 :sijainti [468568.79999084474 7231643.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil
    :kuva 1
    :kuvaus "Tiessä reikä"}

   ;; tarkastus 5 jatkuu...
   {:id 12 :sijainti [468768.79999084474 7231707.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}

   ;; tarkastus 6 päättyy ja alkaa uusi tarkastus 7, jossa uusi jatkuva havainto
   {:id 13 :sijainti [468932.79999084474 7231763.200001526]
    :jatkuvat-havainnot [:epatasainen-polanne]
    :pistemainen-havainto nil}

   ;; tarkastus 7 päättyy ja alkaa uusi tarkastus 8
   {:id 14 :sijainti [469168.79999084474 7231867.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}
   {:id 15 :sijainti [469300.79999084474 7231931.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}
   {:id 16 :sijainti [469492.79999084474 7232011.200001526]
    :jatkuvat-havainnot []
    :pistemainen-havainto nil}])

(def tarkastus-jossa-alkuosa-vaihtuu
  [{:id 1 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [1]
    :pistemainen-havainto nil}
   {:id 2 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [1]
    :pistemainen-havainto nil}
   {:id 3 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [1]
    :pistemainen-havainto nil}
   {:id 4 :sijainti [467009.5999816895 7231180.000024414]
    :jatkuvat-havainnot [1]
    :pistemainen-havainto nil}])

(def tarkastus-jossa-kaikki-mittaukset
  [{:id 1 :sijainti [465641.5999816895 7230780.000024414]
    :tarkastusajo 666
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/now))
    :lumisuus 1
    :talvihoito-tasaisuus 2
    :kitkamittaus 3
    :lampotila 4
    :soratie-tasaisuus 1
    :kiinteys 2
    :polyavyys 3
    :sivukaltevuus 4
    :pistemainen-havainto nil}
   {:id 2 :sijainti [466089.5999816895 7230916.000024414]
    :tarkastusajo 666
    :jatkuvat-havainnot []
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 3)))
    :lumisuus 1
    :talvihoito-tasaisuus 2
    :kitkamittaus 3
    :lampotila 4
    :soratie-tasaisuus 1
    :kiinteys 2
    :polyavyys 3
    :sivukaltevuus 4
    :pistemainen-havainto nil}])

(def tarkastus-jossa-kaikki-pisteet-samassa-sijainnissa
  [{:id 1 :sijainti [465641.5999816895 7230780.000024414]}
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]}
   {:id 3 :sijainti [465641.5999816895 7230780.000024414]}
   {:id 4 :sijainti [465641.5999816895 7230780.000024414]}])

(def tarkastus-jossa-yksi-piste
  [{:id 1 :sijainti [465641.5999816895 7230780.000024414]}])

(def tarkastus-jossa-jatkuva-laadunalitus
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :jatkuvat-havainnot [17]
    :kitkamittaus 0.2}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :jatkuvat-havainnot [17]}
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :jatkuvat-havainnot [17]}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :jatkuvat-havainnot [17]
    :laadunalitus true}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :jatkuvat-havainnot [17]
    :laadunalitus true}])

(def tarkastus-jossa-yhden-pisteen-aikaleima-on-aiemmin
  "Tällaista dataa ei pitäisi tulla, mutta kerran tällainen kuitenkin saatiin.
   Muunnos ei saa kaatua, vaikka data olisi tältä osin huonoa."
  [{:id 0 :sijainti [464681.5999816895 7230492.000024414]
    :aikaleima (c/to-timestamp (time/now))}
   {:id 1 :sijainti [465321.5999816895 7230676.000024414]
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 1)))}
   {:id 2 :sijainti [465641.5999816895 7230780.000024414]
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 2)))}
   {:id 3 :sijainti [466089.5999816895 7230916.000024414]
    :aikaleima (c/to-timestamp (time/minus (time/now) (time/seconds 3)))}
   {:id 4 :sijainti [466409.5999816895 7230996.000024414]
    :aikaleima (c/to-timestamp (time/plus (time/now) (time/seconds 4)))}])

(def tarkastus-jossa-liittyvia-pistemaisia-merkintoja
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [;; Tehdään pistemäinen havainto
   {:id 1 :sijainti [465641.5999816895 7230780.000024414]
    :pistemainen-havainto 20}
   ;; Normaali tarkastus jatkuu
   {:id 2 :sijainti [466089.5999816895 7230916.000024414]}
   {:id 3 :sijainti [466409.5999816895 7230996.000024414]}
   {:id 4 :sijainti [467009.5999816895 7231180.000024414]}
   ;; Kirjataan lisätietoja aiemmin tehtyyn pistemäiseen havaintoon
   {:id 5 :sijainti [467257.5999816895 7231252.000024414]
    :liittyy-merkintaan 1
    :kuvaus "Tässä on nyt jotain mätää"
    :laadunalitus true
    :kuva 1}
   ;; Kirjataan lisää lisätietoja
   ;; Tässä ei ole laadunalitusta, mutta koko pisteessä kuuluisi olla, koska
   ;; edellisessä liittyvässä merkinnässä sellainen havaittiin.
   {:id 6 :sijainti [467257.5999816896 7231252.000024413]
    :liittyy-merkintaan 1
    :kuvaus "Tässä vielä toinen kuva"
    :kuva 2}])

(def tarkastus-jossa-laadunalitus-ja-liittyva-merkinta
  "Tämä tarkastus on ajettu yhteen suuntaan suht. lyhyellä tieosuudella."
  [;; Tehdään pistemäinen havainto, jossa laadunalitus
   {:id 1 :sijainti [465641.5999816895 7230780.000024414]
    :pistemainen-havainto 20
    :laadunalitus true}
   ;; Normaali tarkastus jatkuu
   {:id 2 :sijainti [466089.5999816895 7230916.000024414]}
   {:id 3 :sijainti [466409.5999816895 7230996.000024414]}
   {:id 4 :sijainti [467009.5999816895 7231180.000024414]}
   ;; Kirjataan lisätietoja aiemmin tehtyyn pistemäiseen havaintoon
   ;; Tässä ei ole laadunalitusta, mutta koko pisteessä tulisi olla, koska
   ;; sellainen siihen oli alkujaan merkitty
   {:id 5 :sijainti [467257.5999816895 7231252.000024414]
    :liittyy-merkintaan 1
    :kuvaus "Tässä on nyt jotain mätää"
    :kuva 1}])

(def tarkastus-jossa-pisteiden-sijainti-eri
  [;; Ajetaan tietä 20 pohjoiseen
   {:id 0 :sijainti [446405 7224577]}
   {:id 1 :sijainti [446533 7224624]}
   {:id 2 :sijainti [446636 7224654]}
   {:id 3 :sijainti [446763 7224696]}
   ;; Seuraavat merkinnät ovat huomattavan paljon kauempana kuin edellinen, joskin edelleen samalla tiellä.
   ;; Reitin tulisi katketa kerran tässä kohtaa
   {:id 4 :sijainti [454620 7227506]}
   {:id 5 :sijainti [454699 7227528]}
   {:id 6 :sijainti [454768 7227546]}])

(def tarkastus-jossa-iso-osamuutos
  "Ajetaan kevyen liikenteen väylää 70012 ja tulee iso osamuutos"
  [;; Noin osat 441, 442, 443
   {:id 0 :sijainti [427949 6761002]}
   {:id 1 :sijainti [427950 6761002]}
   {:id 2 :sijainti [427951 6761002]}
   ;; Siirrytään osalle 491, joka on suuresta numerosta huolimatta maantieteellisesti hyvin lähellä edellistä.
   ;; Vaikka tienumero on sama, niin osahyppy on niin suuri, että reitin pitää katketa tästä.
   ;; Muuten tarkastukselle muodostuu virheellinen TR-osoite, joka alkaa osalta 441 ja päättyy osalle 491.
   ;; Väliin jäisi runsaasti osia, jotka ovat jossain ihan muualla.
   {:id 3 :sijainti [427974 6761011]}
   {:id 4 :sijainti [427989 6761012]}
   {:id 5 :sijainti [428006 6761010]}])

(def tarkastus-jossa-valiin-jaa-olematon-osa
  "Ajetaan tietä 20 osalta 1 osalle 3 (osaa 2 ei ole olemassa, joten muodostuu yksi tarkastus)"
  [;; Osa 1
   {:id 0 :sijainti [430569 7212525]}
   {:id 1 :sijainti [430599 7212541]}
   ;; Osa 3 (edellistä osaa ei ole, tarkastus saa jatkua)
   {:id 2 :sijainti [430665 7212590]}
   {:id 3 :sijainti [430702 7212607]}])