(ns harja.ui.taulukko.impl.solu
  "Määritellään taulukon osat täällä."
  (:refer-clojure :exclude [atom])
  (:require [reagent.core :refer [atom] :as r]
            [harja.loki :refer [warn]]
            [harja.ui.ikonit :as ikonit]
            [harja.ui.taulukko.protokollat.grid-osa :as gop]
            [harja.ui.taulukko.protokollat.solu :as sp]
            [harja.ui.taulukko.impl.grid :as grid]
            [harja.ui.taulukko.kaytokset :as kaytokset]))

(def ^:dynamic *this* nil)

(defn lisaa-kaytokset
  "Toiminnot on map, jossa avaimet vastaa input elementin saamia parametrejä. Nämä ovat siis on-change, on-key-down jne.
   Toiminnot arvot on funktio, joka saa parametrina kayttaytymisfunktion palauttaman arvon tai arvot.
   Kayttaytymiset on myös map, jossa avaimet vastaa toimintojen avaimia. Näiden avainten avulla kohdistetaan käyttäytymiset
   oikealle toiminnolle. Kayttaytymisen arvo taasen on funktioita sisältävä sequable, jonka järjestyksellä on väliä!

   Sequablen viimeinen funktio saa eventin argumentikseen. Tämän palauttama arvo annetaan toiseksi viimeiselle
   käyttäytymiselle jne. Lopuksi arvo annetaan itse toiminnolle.

   Funktioissa voi lisäksi käyttää *this* muuttujaa, jonka arvo on eventin laukaiseman osan record. Huom, *this* ei tulisi
   käyttää async kutsuissa.

   Esim.
   (let [tila-atom (atom nil)
         toiminnot {:on-change (fn [arvo] (reset! tila-atom arvo))}
         kayttaytymiset {:on-change [:positiivinen-numero :eventin-arvo]}

         ;; Tässä lopputuloksena on funktio, jossa ensin oletetaan saavan javascript event (:eventin-arvo käyttäytyminen),
         ;; jonka arvo annetaan :positiivinen-numero käyttäytymiselle, joka puolestaan antaa loppputuloksen toiminnolle,
         ;; joka resetoi tila-atomin arvon.
         f-map (lisaa-kaytokset toiminnot kayttaytymiset)]
     [:input {:value @tila-atom :on-change (:on-change f-map)}])"
  [toiminnot kayttaytymiset]
  (into {}
        (map (fn [[nimi f]]
               [nimi (fn [this]
                       (fn [event]
                         (binding [*this* this]
                           (f event))))])
             (merge-with (fn [kayttaytymiset toiminto]
                           (loop [[kaytos & loput-kaytokset] kayttaytymiset
                                  lopullinen-toiminto toiminto]
                             (if (nil? kaytos)
                               lopullinen-toiminto
                               (recur loput-kaytokset
                                      (kaytokset/lisaa-kaytos kaytos lopullinen-toiminto)))))
                         kayttaytymiset
                         toiminnot))))

(defn fmt-asetukset-oikein? [{:keys [fmt fmt-aktiivinen]}]
  (and (or (nil? fmt) (fn? fmt))
       (or (nil? fmt-aktiivinen) (fn? fmt-aktiivinen))))

(defn datan-kasittely-asetukset-oikein? [{:keys [filtteri arvo]}]
  (and (or (nil? filtteri) (fn? filtteri))
       (or (nil? arvo) (fn? arvo))))

(defn korjaa-NaN [arvo solu]
  (if (.isNaN js/Number arvo)
    (do (warn (str "Osan " (or (gop/nimi solu) "Nimetön") " (" (gop/id solu) ") arvo on NaN!"))
        nil)
    arvo))

(defrecord Teksti [id parametrit]
  gop/IPiirrettava
  (-piirra [this]
    (let [{:keys [id class]} (:parametrit this)
          taman-data (::grid/osan-derefable this)
          arvo (korjaa-NaN @taman-data this)]
      [:div.solu.solu-teksti {:class (when class
                                       (apply str (interpose " " class)))
                              :id id
                              :data-cy (::nimi this)}
       ((::fmt this) arvo)]))
  gop/IGridOsa
  (-id [this]
    (:id this))
  (-id? [this id]
    (= (:id this) id))
  (-nimi [this]
    (::nimi this))
  (-aseta-nimi [this nimi]
    (assoc this ::nimi nimi))
  sp/ISolu
  sp/IFmt
  (-lisaa-fmt [this f]
    (assoc this ::fmt f))
  (-lisaa-fmt-aktiiviselle [this f]
    this))

(defn teksti
  ([] (teksti nil))
  ([{:keys [parametrit filtteri arvo fmt nimi rajapinta] :as asetukset}]
   {:pre [(fmt-asetukset-oikein? asetukset)
          ;; TODO tarkasta parametrit
          (datan-kasittely-asetukset-oikein? asetukset)]
    :post [(instance? Teksti %)]}
   (let [id (gensym "teksti")]
     (cond-> (->Teksti id parametrit)
             (nil? fmt) (sp/lisaa-fmt identity)
             fmt (sp/lisaa-fmt fmt)
             nimi (gop/aseta-nimi nimi)))))

(defrecord Tyhja [id]
  sp/ISolu
  gop/IPiirrettava
  (-piirra [this]
    [:div ""])
  gop/IGridOsa
  (-id [this]
    (:id this))
  (-id? [this id]
    (= (:id this) id))
  (-nimi [this]
    (::nimi this))
  (-aseta-nimi [this nimi]
    (assoc this ::nimi nimi)))

(defn tyhja []
  (->Tyhja (gensym "tyhja")))

(defrecord Linkki [id linkki parametrit]
  sp/ISolu
  gop/IPiirrettava
  (-piirra [this]
    (let [{:keys [id class]} (:parametrit this)
          taman-data (::grid/osan-derefable this)
          arvo (korjaa-NaN @taman-data this)]
      [:a.solu.solu-linkki {:class (when class
                                     (apply str (interpose " " class)))
                            :href linkki
                            :id id
                            :data-cy (::nimi this)}
       ((::fmt this) arvo)]))
  gop/IGridOsa
  (-id [this]
    (:id this))
  (-id? [this id]
    (= (:id this) id))
  (-nimi [this]
    (::nimi this))
  (-aseta-nimi [this nimi]
    (assoc this ::nimi nimi))
  sp/IFmt
  (-lisaa-fmt [this f]
    (assoc this ::fmt f))
  (-lisaa-fmt-aktiiviselle [this f]
    this))

(defn linkki
  ([] (linkki nil))
  ([{:keys [parametrit linkki filtteri arvo fmt nimi rajapinta] :as asetukset}]
   {:pre [
          ;; TODO Tarkasta parametrit ja linkki
          (fmt-asetukset-oikein? asetukset)
          (datan-kasittely-asetukset-oikein? asetukset)]
    :post [(instance? Linkki %)]}
   (let [id (gensym "linkki")]
     (cond-> (->Linkki id linkki parametrit)
             (nil? fmt) (sp/lisaa-fmt identity)
             fmt (sp/lisaa-fmt fmt)
             nimi (gop/aseta-nimi nimi)))))

;; Syote record toimii geneerisenä input elementtinä. Jotkin toiminnot tehdään usein
;; (kuten tarkastetaan, että input on positiivinen), niin tällaiset yleiset käyttäytymiset
;; voidaan wrapata johonkin 'toiminnot' funktioon 'kayttaytymiset' parametrien avulla.
;; Käyttäytymiset määritellään eri ns:ssa.
(defrecord Syote [id toiminnot kayttaytymiset parametrit]
  sp/ISolu
  gop/IPiirrettava
  (-piirra [this]
    (let [aktiivinen? (atom false)
          {:keys [on-blur on-change on-click on-focus on-input on-key-down on-key-press
                  on-key-up]} (lisaa-kaytokset (merge-with (fn [kayttajan-lisaama tassa-lisatty]
                                                             (comp kayttajan-lisaama
                                                                   tassa-lisatty))
                                                           (:toiminnot this)
                                                           {:on-blur (fn [e]
                                                                       (reset! aktiivinen? false)
                                                                       e)
                                                            :on-focus (fn [e]
                                                                        (reset! aktiivinen? true)
                                                                        e)})
                                               (:kayttaytymiset this))]
      (fn [this]
        (let [{:keys [id class type name readonly? required? tabindex disabled?
                      checked? default-checked? indeterminate?
                      alt height src width
                      autocomplete max max-length min min-length pattern placeholder size]} (:parametrit this)
              taman-data (::grid/osan-derefable this)
              arvo (korjaa-NaN @taman-data this)
              parametrit (into {}
                               (remove (fn [[_ arvo]]
                                         (nil? arvo))
                                       {;; Inputin parametrit
                                        :class (when class
                                                 (apply str (interpose " " class)))
                                        :data-cy (:id this)
                                        :id id
                                        :type type
                                        :value (if @aktiivinen?
                                                 ((::fmt-aktiivinen this) arvo)
                                                 ((::fmt this) arvo))
                                        :name name
                                        :read-only readonly?
                                        :required required?
                                        :tab-index tabindex
                                        :disabled disabled?
                                        ;; checkbox or radio paramterit
                                        :checked checked?
                                        :default-checked default-checked?
                                        :indeterminate indeterminate?
                                        ;; kuvan parametrit
                                        :alt alt
                                        :height height
                                        :src src
                                        :width width
                                        ;; numero/teksti input
                                        :auto-complete autocomplete
                                        :max max
                                        :max-length max-length
                                        :min min
                                        :min-length min-length
                                        :pattern pattern
                                        :placeholder placeholder
                                        :size size
                                        ;; GlobalEventHandlers
                                        :on-blur (when on-blur
                                                   (on-blur this))
                                        :on-change (when on-change
                                                     (on-change this))
                                        :on-click (when on-click
                                                    (on-click this))
                                        :on-focus (when on-focus
                                                    (on-focus this))
                                        :on-input (when on-input
                                                    (on-input this))
                                        :on-key-down (when on-key-down
                                                       (on-key-down this))
                                        :on-key-press (when on-key-press
                                                        (on-key-press this))
                                        :on-key-up (when on-key-up
                                                     (on-key-up this))}))]
          [:input.solu.solu-syote parametrit]))))
  gop/IGridOsa
  (-id [this]
    (:id this))
  (-id? [this id]
    (= (:id this) id))
  (-nimi [this]
    (::nimi this))
  (-aseta-nimi [this nimi]
    (assoc this ::nimi nimi))
  sp/IFmt
  (-lisaa-fmt [this f]
    (assoc this ::fmt f))
  (-lisaa-fmt-aktiiviselle [this f]
    (assoc this ::fmt-aktiivinen f)))

(defn syote
  ([] (syote nil))
  ([{:keys [toiminnot kayttaytymiset parametrit filtteri arvo fmt fmt-aktiivinen nimi rajapinta] :as asetukset}]
   {:pre [
          ;; TODO Tarkasta parametrit, toiminnot ja kayttaytymiset
          (fmt-asetukset-oikein? asetukset)
          (datan-kasittely-asetukset-oikein? asetukset)]
    :post [(instance? Syote %)]}
   (let [id (gensym "syote")]
     (cond-> (->Syote id toiminnot kayttaytymiset parametrit)
             (nil? fmt) (sp/lisaa-fmt identity)
             fmt (sp/lisaa-fmt fmt)
             (nil? fmt-aktiivinen) (sp/lisaa-fmt-aktiiviselle identity)
             fmt-aktiivinen (sp/lisaa-fmt-aktiiviselle fmt-aktiivinen)
             nimi (gop/aseta-nimi nimi)))))

(defrecord Nappi [id toiminnot kayttaytymiset sisalto parametrit]
  sp/ISolu
  gop/IPiirrettava
  (-piirra [this]
    (let [{:keys [on-blur on-change on-click on-focus on-input on-key-down on-key-press
                  on-key-up]} (lisaa-kaytokset (:toiminnot this)
                                               (:kayttaytymiset this))]
      (fn [this]
        (let [{:keys [id class type value name tabindex disabled? size]} (:parametrit this)
              taman-data (::grid/osan-derefable this)
              arvo (korjaa-NaN @taman-data this)
              parametrit (into {}
                               (remove (fn [[_ arvo]]
                                         (nil? arvo))
                                       {;; Inputin parametrit
                                        :class (when class
                                                 (apply str (interpose " " class)))
                                        :data-cy (:id this)
                                        :id id
                                        :type type
                                        :value ((::fmt this) arvo)
                                        :name name
                                        :tab-index tabindex
                                        :disabled disabled?
                                        :size size
                                        ;; GlobalEventHandlers
                                        :on-blur (when on-blur
                                                   (on-blur this))
                                        :on-change (when on-change
                                                     (on-change this))
                                        :on-click (when on-click
                                                    (on-click this))
                                        :on-focus (when on-focus
                                                    (on-focus this))
                                        :on-input (when on-input
                                                    (on-input this))
                                        :on-key-down (when on-key-down
                                                       (on-key-down this))
                                        :on-key-press (when on-key-press
                                                        (on-key-press this))
                                        :on-key-up (when on-key-up
                                                     (on-key-up this))}))]
          [:button.solu.solu-nappi parametrit sisalto]))))
  gop/IGridOsa
  (-id [this]
    (:id this))
  (-id? [this id]
    (= (:id this) id))
  (-nimi [this]
    (::nimi this))
  (-aseta-nimi [this nimi]
    (assoc this ::nimi nimi))
  sp/IFmt
  (-lisaa-fmt [this f]
    (assoc this ::fmt f))
  (-lisaa-fmt-aktiiviselle [this f]
    this))

(defn nappi
  ([] (nappi nil))
  ([{:keys [toiminnot kayttaytymiset parametrit sisalto filtteri arvo fmt nimi rajapinta] :as asetukset}]
   {:pre [
          ;; TODO Tarkasta parametrit, toiminnot, sisalto ja kayttaytymiset
          (fmt-asetukset-oikein? asetukset)
          (datan-kasittely-asetukset-oikein? asetukset)]
    :post [(instance? Nappi %)]}
   (let [id (gensym "nappi")]
     (cond-> (->Nappi id toiminnot kayttaytymiset sisalto parametrit)
             (nil? fmt) (sp/lisaa-fmt identity)
             fmt (sp/lisaa-fmt fmt)
             nimi (gop/aseta-nimi nimi)))))

(defrecord Laajenna [id aukaise-fn auki-alussa? parametrit]
  sp/ISolu
  gop/IPiirrettava
  (-piirra [this]
    (let [auki? (atom auki-alussa?)]
      (fn [this]
        (let [{:keys [id class ikoni]} (:parametrit this)
              taman-data (::grid/osan-derefable this)
              arvo (korjaa-NaN @taman-data this)
              ikoni (or ikoni "chevron")
              ikoni-auki (if (= ikoni "chevron")
                           ikonit/livicon-chevron-down
                           ikonit/oi-caret-bottom)
              ikoni-kiinni (if (= ikoni "chevron")
                             ikonit/livicon-chevron-up
                             ikonit/oi-caret-top)]
          [:span.solu.klikattava.solu-laajenna
           {:class (when class
                     (apply str (interpose " " class)))
            :id id
            :data-cy (:id this)
            :on-click
            #(do (.preventDefault %)
                 (swap! auki? not)
                 (aukaise-fn this @auki?))}
           [:span.laajenna-teksti ((::fmt this) arvo)]
           (if @auki?
             ^{:key "laajenna-auki"}
             [ikoni-auki]
             ^{:key "laajenna-kiini"}
             [ikoni-kiinni])]))))
  gop/IGridOsa
  (-id [this]
    (:id this))
  (-id? [this id]
    (= (:id this) id))
  (-nimi [this]
    (::nimi this))
  (-aseta-nimi [this nimi]
    (assoc this ::nimi nimi))
  sp/IFmt
  (-lisaa-fmt [this f]
    (assoc this ::fmt f))
  (-lisaa-fmt-aktiiviselle [this f]
    this))

(defn laajenna
  ([] (laajenna nil))
  ([{:keys [aukaise-fn auki-alussa? parametrit filtteri arvo fmt nimi rajapinta] :as asetukset}]
   {:pre [
          ;; TODO Tarkasta aukaise-fn ja parametrit
          (or (nil? auki-alussa?) (boolean? auki-alussa?))
          (fmt-asetukset-oikein? asetukset)
          (datan-kasittely-asetukset-oikein? asetukset)]
    :post [(instance? Laajenna %)]}
   (let [id (gensym "laajenna")]
     (cond-> (->Laajenna id aukaise-fn auki-alussa? parametrit)
             (nil? fmt) (sp/lisaa-fmt identity)
             fmt (sp/lisaa-fmt fmt)
             nimi (gop/aseta-nimi nimi)))))