(ns harja.views.urakka.pot2.paallyste-ja-alusta-yhteiset
  (:require [harja.ui.napit :as napit]
            [harja.ui.ikonit :as ikonit]
            [harja.tiedot.urakka.yllapitokohteet :as yllapitokohteet]))


(defn rivin-toiminnot-sarake
  [rivi osa e! app kirjoitusoikeus? rivit-atom tyyppi voi-muokata?]
  (assert (#{:alusta :paallystekerros} tyyppi) "Tyypin on oltava päällystekerros tai alusta")
  (let [kohdeosat-muokkaa! (fn [uudet-kohdeosat-fn]
                             (let [vanhat-kohdeosat @rivit-atom
                                   uudet-kohdeosat (uudet-kohdeosat-fn vanhat-kohdeosat)]
                               (swap! rivit-atom (fn [_]
                                                       uudet-kohdeosat))))
        lisaa-osa-fn (fn [index]
                       (kohdeosat-muokkaa! (fn [vanhat-kohdeosat]
                                             (yllapitokohteet/lisaa-uusi-kohdeosa vanhat-kohdeosat (inc index) {})
                                             (yllapitokohteet/lisaa-uusi-pot2-alustarivi vanhat-kohdeosat (inc index) {}))))
        poista-osa-fn (fn [index]
                        (kohdeosat-muokkaa! (fn [vanhat-kohdeosat]
                                              (yllapitokohteet/poista-kohdeosa vanhat-kohdeosat (inc index)))))]
    (fn [rivi {:keys [index]} e! app kirjoitusoikeus? rivit-atom tyyppi voi-muokata?]
      (let [nappi-disabled? (or (not voi-muokata?)
                                (not kirjoitusoikeus?))]
        [:span.tasaa-oikealle
         [napit/yleinen-ensisijainen ""
          lisaa-osa-fn
          {:ikoni (ikonit/livicon-plus)
           :disabled nappi-disabled?
           :luokka "napiton-nappi btn-xs"
           :toiminto-args [index]}]
         [napit/kielteinen ""
          poista-osa-fn
          {:ikoni (ikonit/livicon-trash)
           :disabled nappi-disabled?
           :luokka "napiton-nappi btn-xs"
           :toiminto-args [index]}]]))))


