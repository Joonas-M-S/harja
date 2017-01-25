(ns harja-laadunseuranta.tarkastusreittimuunnin.ramppianalyysi
  "Tämä namespace tarjoaa funktion laadunseurannan mobiilityökalulla tehtyjen
   reittimerkintöjen ja niihin liittyvän geometrisoinnin korjaamiseen, mikäli
   geometrisointi on osunut virheellisesti rampille."
  (:require [taoensso.timbre :as log]
            [harja.domain.tierekisteri :as tr-domain]))

(defn- maarittele-alkavien-ramppien-indeksit
  "Palauttaa indeksit merkintöihin, joissa poiketaan rampille."
  [merkinnat]
  (let [tulos (reduce (fn [tulos seuraava]
                        (if
                          ;; Edellinen piste ei ollut rampilla, mutta seuraava on
                          ;; --> Otetaan indeksi talteen
                          (and (not (-> tulos :edellinen-piste :piste-rampilla?))
                               (:piste-rampilla? seuraava))
                          ;; Jatketaan...
                          (assoc tulos :edellinen-piste seuraava
                                       :kasiteltava-indeksi (inc (:kasiteltava-indeksi tulos))
                                       :ramppien-alut
                                       (conj (:ramppien-alut tulos) (inc (:kasiteltava-indeksi tulos))))
                          (assoc tulos :edellinen-piste seuraava
                                       :kasiteltava-indeksi (inc (:kasiteltava-indeksi tulos)))))
                      {:ramppien-alut []
                       :edellinen-piste nil
                       :kasiteltava-indeksi 0}
                      merkinnat)]
    (:ramppien-alut tulos)))

(defn projisoi-ramppi-moottoritielle [ramppia-edeltava-merkina rampin-merkinnat]
  rampin-merkinnat
  )

(defn- korjaa-vahapatoiset-rampit
  "Ottaa reittimerkinnät ramppitiedolla sekä indeksin, joissa siirrytään rampille.
   Mikäli rampilla ajo sisältää vähemmän kuin N pistettä, projisoi rampin takaisin moottoritielle.
   Muussa tapauksessa ei tee rampille mitään.

   Palauttaa kaikki merkinnät, joissa indeksin kuvaava ramppi-osa on korjattu."
  [merkinnat-ramppitiedoilla ramppi-indeksi n]
  (if (= ramppi-indeksi 0)
    merkinnat-ramppitiedoilla ;; Merkinnät alkavat rampilta, ei tehdä mitään.
    (let [indeksin-jalkeiset-pisteet (last (split-at ramppi-indeksi merkinnat-ramppitiedoilla))
          rampin-merkinnat (reduce (fn [tulos seuraava]
                                     (if (= seuraava true)
                                       (conj tulos seuraava)
                                       (reduced tulos)))
                                   []
                                   indeksin-jalkeiset-pisteet)
          korjattu-ramppi (if (< (count rampin-merkinnat) n)
                            (projisoi-ramppi-moottoritielle (nth merkinnat-ramppitiedoilla (dec ramppi-indeksi))
                                                            rampin-merkinnat)
                            rampin-merkinnat)
          osa-ennen-ramppia (take ramppi-indeksi merkinnat-ramppitiedoilla)
          osa-rampin-jalkeen (drop (+ ramppi-indeksi (count korjattu-ramppi)) merkinnat-ramppitiedoilla)]
      (concat
        osa-ennen-ramppia
        korjattu-ramppi
        osa-rampin-jalkeen))))

(defn projisoi-virheelliset-rampit-takaisin-moottoritielle [merkinnat-ramppitiedoilla]
  (let [alkavien-ramppien-indeksit (maarittele-alkavien-ramppien-indeksit merkinnat-ramppitiedoilla)
        korjatut-merkinnat merkinnat-ramppitiedoilla]
    korjatut-merkinnat))

(defn- lisaa-merkintoihin-ramppitiedot
  "Lisää merkintöihin tiedon siitä, onko piste projisoitu rampille."
  [merkinnat]
  (mapv #(do
           (assoc % :piste-rampilla? (tr-domain/tie-rampilla? (get-in % [:tr-osoite :tie]))))
        merkinnat))

(defn korjaa-virheelliset-rampit
  "Ottaa tarkastusreittimerkinnät, jotka on projisoitu tieverkolle ja joilla on myös
   tieto lähimmistä tierekisteriosoitteista. Etsii kohdat, joissa piste sijaitsee virheellisesti rampilla tai on
   virheellisesti prijisoitu rampille. Palauttaa uudet merkinnät, joissa
   tällaiset virheelliset pisteet on projisoitu takaisin moottoritielle.

   Jos yksikään piste ei sijaitse tai projisioidu rampille, palauttaa merkinnät sellaisenaan.

   Taustatarina: Moottoritiellä tarkastuksia tehdessä ramppien alut ovat usein päällekäin
   moottoritien kanssa. Tällä mekanismilla pyritään korjaamaan tilanne, jossa moottoritietä ajettaessa pisteet
   projisoituvat virheellisesti rampille, vaikka ajo tapahtuisikin moottoritiellä. On syytä huomata, että
   GPS:n epätarkkuudesta johtuen tämä funktio pelaa todennäköisyyksillä eikä tulos ole täysin varma."
  [merkinnat]
  (let [merkinnat-ramppitiedoilla (lisaa-merkintoihin-ramppitiedot merkinnat)
        korjatut-merkinnat (projisoi-virheelliset-rampit-takaisin-moottoritielle merkinnat-ramppitiedoilla)]
    korjatut-merkinnat))