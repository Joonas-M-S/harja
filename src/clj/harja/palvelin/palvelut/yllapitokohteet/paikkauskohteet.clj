(ns harja.palvelin.palvelut.yllapitokohteet.paikkauskohteet
  (:require [com.stuartsierra.component :as component]
            [slingshot.slingshot :refer [throw+ try+]]
            [clojure.spec.alpha :as s]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.roolit :as roolit]
            [harja.pvm :as pvm]
            [harja.kyselyt.konversio :as konv]
            [harja.geo :as geo]
            [harja.kyselyt.paikkaus :as q]
            [harja.kyselyt.urakat :as q-urakat]
            [harja.kyselyt.yllapitokohteet :as q-yllapitokohteet]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [taoensso.timbre :as log]
            [clojure.string :as str]))

(defn validi-pvm-vali? [validointivirheet alku loppu]
  (if (and (not (nil? alku)) (not (nil? loppu)) (.after alku loppu))
    (conj validointivirheet "Loppuaika tulee ennen alkuaikaa.")
    validointivirheet))

(defn validit-tr_osat? [validointivirheet tie alkuosa alkuetaisyys loppuosa loppuetaisyys]
  (if (and tie alkuosa alkuetaisyys loppuosa loppuetaisyys
           (>= loppuosa alkuosa))
    validointivirheet
    (conj validointivirheet "Tierekisterissä virhe.")))

(defn- sallittu-tilamuutos? [uusi vanha rooli]
  (let [ehdotettu? #(= "ehdotettu" %)
        tilattu? #(= "tilattu" %)
        hylatty? #(= "hylatty" %)]
    (if (or (= uusi vanha) (nil? vanha))
      true
      (if (= rooli :tilaaja)
        ;; tilaaja saa tehdä seuraavat tilamuutokset.
        (or
          (and (ehdotettu? vanha) (or (tilattu? uusi) (hylatty? uusi)))
          (and (or (tilattu? vanha) (hylatty? vanha)) (ehdotettu? uusi)))
        false))))

(defn validi-paikkauskohteen-tilamuutos? [validointivirheet uusi vanha rooli]
  (if (sallittu-tilamuutos? (:paikkauskohteen-tila uusi) (:paikkauskohteen-tila vanha) rooli)
    validointivirheet
    (conj validointivirheet
          (str "Virhe tilan muutoksessa "
               (name (:paikkauskohteen-tila vanha)) " -> " (name (:paikkauskohteen-tila uusi))))))

(defn- validi-aika? [aika]
  (if (and
        (.after aika (pvm/->pvm "01.01.2000"))
        (.before aika (pvm/->pvm "01.01.2100")))
    true
    false))

(defn- validi-nimi? [nimi]
  (if (or (nil? nimi) (= "" nimi))
    false
    true))

(defn- validi-paikkauskohteen-tila? [tila]
  (boolean (some #(= tila %) ["ehdotettu" "tilattu" "hylatty" "valmis" "hyvaksytty"])))

(s/def ::nimi (s/and string? #(validi-nimi? %)))
(s/def ::alkupvm (s/and #(inst? %) #(validi-aika? %)))
(s/def ::loppupvm (s/and #(inst? %) #(validi-aika? %)))
(s/def ::paikkauskohteen-tila (s/and string? #(validi-paikkauskohteen-tila? %)))

(defn paikkauskohde-validi? [kohde vanha-kohde rooli]
  (let [validointivirheet (as-> #{} virheet
                                (if (s/valid? ::nimi (:nimi kohde))
                                  virheet
                                  (conj virheet "Paikkauskohteen nimi puuttuu."))
                                (if (s/valid? ::alkupvm (:alkupvm kohde))
                                  virheet
                                  (conj virheet "Paikkauskohteen alkupäivässä virhe."))
                                (if (s/valid? ::loppupvm (:loppupvm kohde))
                                  virheet
                                  (conj virheet "Paikkauskohteen loppupäivässä virhe."))
                                (if (s/valid? ::paikkauskohteen-tila (:paikkauskohteen-tila kohde))
                                  virheet
                                  (conj virheet "Paikkauskohteen tilassa virhe."))
                                (validi-pvm-vali? virheet (:alkupvm kohde) (:loppupvm kohde))
                                (validit-tr_osat? virheet (:tie kohde) (:aosa kohde) (:losa kohde) (:aet kohde) (:let kohde))
                                (validi-paikkauskohteen-tilamuutos? virheet kohde vanha-kohde rooli))]
    validointivirheet))

(defn- hae-urakkatyyppi [db urakka-id]
  (keyword (:tyyppi (first (q-yllapitokohteet/hae-urakan-tyyppi db {:urakka urakka-id})))))

(defn- siivoa-paikkauskohteet [paikkauskohteet]
  (map (fn [p]
         (-> p
             (assoc :sijainti (geo/pg->clj (:geometria p)))
             (dissoc :geometria)))
       paikkauskohteet))

(defn- hae-paikkauskohteet-geometrialla [db urakka-id tila alkupvm loppupvm menetelmat]
  (let [;ely (q-urakat/hae-urakan-ely db {:urakkaid urakka-id})
        ;; ELYllä on olemassa aluegeometria, jota voidaan hyödyntää haussa,
        ;; mutta ely on myös merkittynä paikkauskohteen omistavan urakan hallintayksiköksi, joten
        ;; tarvittavat paikkauskohteet saadaan myös tätä kautta
        ;; hox tästä nyt puuttuu tuo elyn käyttö haussa.

        ;; Haetaan paikkauskohteet hoito ja teiden-hoito tyyppisille urakoille näiden urakoiden geometrian perusteella
        paikkauskohteet (q/paikkauskohteet-geometrialla db {:urakka-id urakka-id
                                                            :tila tila
                                                            :alkupvm alkupvm
                                                            :loppupvm loppupvm
                                                            :tyomenetelmat menetelmat})]
    paikkauskohteet))

(defn paikkauskohteet [db user {:keys [vastuuyksikko tila alkupvm loppupvm tyomenetelmat urakka-id] :as tiedot}]
  (println oikeudet/urakat-paikkaukset-paikkauskohteet)
  (oikeudet/vaadi-lukuoikeus oikeudet/urakat-paikkaukset-paikkauskohteet user (:urakka-id tiedot))
  (let [_ (println "paikkauskohteet :: tiedot" (pr-str tiedot))

        ;; Paikkauskohteiden hakeminen on eri urakkatyypeille vaihtelee.
        ;; Paikkaus ja Päällystys urakoille haetaan normaalisti vain paikkauskohteet, mutta
        ;; Jos alueurakalle (jolla siis tarkoitetaan hoito ja teiden-hoito) haetaan paikkauskohteita,
        ;; niin silloin turvaudutaan paikkauskohteen maantieteelliseen
        ;; sijaintiin eikä urakan-id:seen.
        urakan-tyyppi (hae-urakkatyyppi db (:urakka-id tiedot))

        tila (if (or (nil? tila) (= "kaikki" (str/lower-case tila)))
               nil                                          ;; kaikkia haettaessa käytetään nil arvoa
               tila)
        menetelmat (disj tyomenetelmat "Kaikki")
        menetelmat (when (> (count menetelmat) 0)
                     menetelmat)
        urakan-paikkauskohteet (if (or (= :hoito urakan-tyyppi) (= :teiden-hoito urakan-tyyppi))
                                 (hae-paikkauskohteet-geometrialla db urakka-id tila alkupvm loppupvm menetelmat)
                                 (q/paikkauskohteet-urakalle db {:urakka-id urakka-id
                                                                 :tila tila
                                                                 :alkupvm alkupvm
                                                                 :loppupvm loppupvm
                                                                 :tyomenetelmat menetelmat}))
        urakan-paikkauskohteet (siivoa-paikkauskohteet urakan-paikkauskohteet)
        _ (println "paikkauskohteet :: urakan-paikkauskohteet" (pr-str urakan-paikkauskohteet))
        ;; Tarkistetaan käyttäjän käyttöoikeudet suhteessa kustannuksiin.
        ;; Mikäli käyttäjälle ei ole nimenomaan annettu oikeuksia nähdä summia, niin poistetaan ne
        urakan-paikkauskohteet (if (oikeudet/voi-lukea? oikeudet/urakat-paikkaukset-paikkauskohteetkustannukset (:urakka-id tiedot) user)
                                 ;; True - on oikeudet kustannuksiin
                                 urakan-paikkauskohteet
                                 ;; False - ei ole oikeuksia kustannuksiin, joten poistetaan ne
                                 (map (fn [kohde]
                                        (dissoc kohde :suunniteltu-hinta :toteutunut-hinta))
                                      urakan-paikkauskohteet))
        ]
    urakan-paikkauskohteet))


(defn tallenna-paikkauskohde! [db user kohde]
  (println "tallenna-paikkauskohde! voi voi-lukea? " (pr-str (oikeudet/voi-kirjoittaa? oikeudet/urakat-paikkaukset-paikkauskohteetkustannukset (:urakka-id kohde) user)) (pr-str (roolit/osapuoli user)))
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-paikkaukset-paikkauskohteetkustannukset user (:urakka-id kohde))
  (let [_ (println "tallenna-paikkauskohde! :: kohde " (pr-str (dissoc kohde :sijainti)))
        kayttajarooli (roolit/osapuoli user)
        on-kustannusoikeudet? (oikeudet/voi-kirjoittaa? oikeudet/urakat-paikkaukset-paikkauskohteetkustannukset (:urakka-id kohde) user)
        kohde-id (:id kohde)
        vanha-kohde (when kohde-id (first (q/paikkauskohde-urakalle db {:id kohde-id})))
        ;; Tarkista pakolliset tiedot ja tietojen oikeellisuus
        validointivirheet (paikkauskohde-validi? kohde vanha-kohde kayttajarooli) ;;rooli on null?
        _ (println "tallenna-paikkauskohde! :: validointivirheet" (pr-str validointivirheet))
        ;; Jos annetulla kohteella on olemassa id, niin päivitetään. Muuten tehdään uusi
        kohde (when (empty? validointivirheet)
                (if kohde-id
                  (do
                    (q/paivita-paikkauskohde! db
                                              (merge
                                                (when on-kustannusoikeudet?
                                                  {:suunniteltu-hinta (:suunniteltu-hinta kohde)})
                                                {:id kohde-id
                                                 :ulkoinen-id (:ulkoinen-id kohde)
                                                 :nimi (:nimi kohde)
                                                 :poistettu (or (:poistettu kohde) false)
                                                 :muokkaaja-id (:id user)
                                                 :muokattu (pvm/nyt)
                                                 :yhalahetyksen-tila (:yhalahetyksen-tila kohde)
                                                 :virhe (:virhe kohde)
                                                 :tarkistettu (or (:tarkistettu kohde) nil)
                                                 :tarkistaja-id (or (:tarkistaja-id kohde) nil)
                                                 :ilmoitettu-virhe (or (:ilmoitettu-virhe kohde) nil)
                                                 :nro (:nro kohde)
                                                 :alkupvm (:alkupvm kohde)
                                                 :loppupvm (:loppupvm kohde)
                                                 :tyomenetelma (or (:tyomenetelma kohde) nil)
                                                 :tyomenetelma-kuvaus (or (:tyomenetelma-kuvaus kohde) nil)
                                                 :tie (:tie kohde)
                                                 :aosa (:aosa kohde)
                                                 :losa (:losa kohde)
                                                 :aet (:aet kohde)
                                                 :let (:let kohde)
                                                 :paikkauskohteen-tila (:paikkauskohteen-tila kohde)
                                                 :suunniteltu-maara (:suunniteltu-maara kohde)
                                                 :yksikko (:yksikko kohde)
                                                 :lisatiedot (:lisatiedot kohde)}))
                    kohde)
                  (do
                    (println "Tallennettiin uusi :: antamalla " (pr-str kohde))
                    (q/luo-uusi-paikkauskohde<! db
                                                (merge
                                                  (when on-kustannusoikeudet?
                                                    {:suunniteltu-hinta (:suunniteltu-hinta kohde)})
                                                  {:luoja-id (:id user)
                                                   :ulkoinen-id (:ulkoinen-id kohde)
                                                   :nimi (:nimi kohde)
                                                   :urakka-id (:urakka-id kohde)
                                                   :luotu (or (:luotu kohde) (pvm/nyt))
                                                   :yhalahetyksen-tila (:yhalahetyksen-tila kohde)
                                                   :virhe (:virhe kohde)
                                                   :nro (:nro kohde)
                                                   :alkupvm (:alkupvm kohde)
                                                   :loppupvm (:loppupvm kohde)
                                                   :tyomenetelma (:tyomenetelma kohde)
                                                   :tyomenetelma-kuvaus (:tyomenetelma-kuvaus kohde)
                                                   :tie (:tie kohde)
                                                   :aosa (:aosa kohde)
                                                   :losa (:losa kohde)
                                                   :aet (:aet kohde)
                                                   :let (:let kohde)
                                                   :paikkauskohteen-tila (:paikkauskohteen-tila kohde)
                                                   :suunniteltu-maara (:suunniteltu-maara kohde)
                                                   :yksikko (:yksikko kohde)
                                                   :lisatiedot (:lisatiedot kohde)
                                                   })))))

        _ (println "kohde: " (pr-str kohde))
        ]
    (if (empty? validointivirheet)
      kohde
      (throw+ {:type "Error"
               :virheet [{:koodi "ERROR" :viesti validointivirheet}]}))
    ))

(defn poista-paikkauskohde! [db user kohde]
  (oikeudet/vaadi-kirjoitusoikeus oikeudet/urakat-paikkaukset-paikkauskohteetkustannukset user (:urakka-id kohde))
  (let [_ (println "poista-paikkauskohde! :: kohde " (pr-str (dissoc kohde :sijainti)))
        id (:id kohde)
        ;; Tarkistetaan, että haluttu paikkauskohde on olemassa eikä sitä ole vielä poistettu
        poistettava (first (q/hae-paikkauskohde db {:id (:id kohde) :urakka-id (:urakka-id kohde)}))
        _ (q/poista-paikkauskohde! db id)]
    (if (empty? poistettava)
      (throw+ {:type "Error"
               :virheet [{:koodi "ERROR" :viesti "Paikkauskohdetta ei voitu poistaa, koska sitä ei löydy."}]})
      ;; Palautetaan poistettu paikkauskohde
      (assoc poistettava :poistettu true))))

(defrecord Paikkauskohteet []
  component/Lifecycle
  (start [this]
    (let [http (:http-palvelin this)
          ;email (:sonja-sahkoposti this)
          db (:db this)]
      (julkaise-palvelu http :paikkauskohteet-urakalle
                        (fn [user tiedot]
                          (paikkauskohteet db user tiedot)))
      (julkaise-palvelu http :tallenna-paikkauskohde-urakalle
                        (fn [user kohde]
                          (tallenna-paikkauskohde! db user kohde)))
      (julkaise-palvelu http :poista-paikkauskohde
                        (fn [user kohde]
                          (poista-paikkauskohde! db user kohde)))
      this))

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :paikkauskohteet-urakalle
      :tallenna-paikkauskohde-urakalle
      :poista-paikkauskohde)
    this))
