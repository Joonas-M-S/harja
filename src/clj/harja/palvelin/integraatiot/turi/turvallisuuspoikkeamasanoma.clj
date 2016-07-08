(ns harja.palvelin.integraatiot.turi.turvallisuuspoikkeamasanoma
  (:require [taoensso.timbre :as log]
            [harja.tyokalut.xml :as xml]
            [harja.geo :as geo]
            [harja.domain.turvallisuuspoikkeamat :as turpodomain]
            [harja.palvelin.integraatiot.api.tyokalut.liitteet :as liitteet])
  (:use [slingshot.slingshot :only [throw+]]))

(def +xsd-polku+ "xsd/turi/")

(defn rakenna-lista [lista-avain elementti-avain data]
  (when data
    (let [lista (vec (.getArray data))]
      (when (not-empty lista)
        (apply conj []
               lista-avain
               (mapv #(vector elementti-avain %) lista))))))

(defn rakenna-luokittelulista [lista-elementti elementti tyypit]
  (let [tyypit (vec (.getArray tyypit))]
    (apply conj [] lista-elementti (mapv #(vector elementti %) tyypit))))

(defn rakenna-korjaavat-toimenpiteet [data]
  (let [korjaavat-toimenpiteet (:korjaavat-toimenpiteet data)]
    (when (not-empty korjaavat-toimenpiteet)
      (apply conj []
             :turi:korjaavat-toimenpiteet
             (mapv #(vector
                     :turi:korjaava-toimenpide
                     [:turi:kuvaus (:kuvaus %)]
                     [:turi:suoritettu (when (:suoritettu %) (xml/formatoi-paivamaara (:suoritettu %)))]
                     [:turi:vastaavahenkilo
                      [:turi:etunimi (:vastaavahenkilo %)]
                      [:turi:sukunimi (:vastaavahenkilo %)]]) korjaavat-toimenpiteet)))))

(defn rakenna-kommentit [data]
  (let [kommentit (:kommentit data)]
    (when (not-empty kommentit)
      (apply conj []
             :turi:kommentit
             (mapv #(vector :turi:kommentti (:kommentti %)) kommentit)))))

(defn rakenna-liitteet [data]
  (let [liitteet (:liitteet data)]
    (when (not-empty liitteet)
      (apply conj []
             :turi:liitteet
             (mapv #(vector
                     :turi:liite
                     [:turi:tiedostonimi (:nimi %)]
                     [:turi:tyyppi (:tyyppi %)]
                     [:turi:kuvaus (:kuvaus %)]
                     [:turi:sisalto (String. (liitteet/enkoodaa-base64 (:data (:sisalto %))))])
                   liitteet)))))

(defn rakenna-sijainti [data]
  [:turi:sijainti
   (if-let [koordinaatit (:coordinates (geo/pg->clj (:sijainti data)))]
     [:turi:koordinaatit
      (when (:tr_numero data) [:turi:tienumero (:tr_numero data)])
      [:turi:x (first koordinaatit)]
      [:turi:y (second koordinaatit)]]
     [:turi:tierekisteriosoite
      [:turi:tienumero (:tr_numero data)]
      [:turi:aosa (:tr_alkuosa data)]
      [:turi:aet (:tr_alkuetaisyys data)]
      [:turi:losa (:tr_loppuosa data)]
      [:turi:let (:tr_loppuetaisyys data)]])])

(defn rakenna-henkilovahinko [data]
  [:turi:henkilovahinko

   (when (:tyontekijanammatti data)
     [:turi:tyontekijan-ammatti
      [:turi:koodi (:tyontekijanammatti data)]
      [:turi:selite (:tyontekijanammattimuu data)]])

   (rakenna-lista :turi:vamman-laatu :turi:vamma (:vammat data))
   (rakenna-lista :turi:vahingoittuneet-ruumiinosat :turi:ruumiinosa (:vahingoittuneetruumiinosat data))

   (when (:sairauspoissaolopaivat data) [:turi:sairauspoissaolopaivat (:sairauspoissaolopaivat data)])
   (when (:sairaalavuorokaudet data) [:turi:sairaalahoitovuorokaudet (:sairaalavuorokaudet data)])

   [:turi:jatkuuko-sairaspoissaolo (true? (:sairauspoissaolojatkuu data))]])

(defn rakenna-turvallisuuskoordinaattori [data]
  [:turi:turvallisuuskoordinaattori
   [:turi:etunimi (:turvallisuuskoordinaattorietunimi data)]
   [:turi:sukunimi (:turvallisuuskoordinaattorisukunimi data)]])

(defn rakenna-laatija [data]
  [:turi:laatija
   [:turi:etunimi (:laatijaetunimi data)]
   [:turi:sukunimi (:laatijasukunimi data)]])

(defn rakenna-ilmoittaja [data]
  [:turi:ilmoittaja
   [:turi:etunimi (:ilmoittaja_etunimi data)]
   [:turi:sukunimi (:ilmoittaja_sukunimi data)]]
  [:turi:kuvaus (:kuvaus data)])



(defn muodosta-viesti [data]
  [:turi:turvallisuuspoikkeama
   {:xmlns:turi "http://www.liikennevirasto.fi/xsd/turi"}
   [:turi:tunniste (:id data)]
   [:turi:vaylamuoto (:vaylamuoto data)]
   (when (:tapahtunut data) [:turi:tapahtunut (xml/formatoi-aikaleima (:tapahtunut data))])
   (when (:paattynyt data) [:turi:paattynyt (xml/formatoi-aikaleima (:paattynyt data))])
   (when (:kasitelty data) [:turi:kasitelty (xml/formatoi-aikaleima (:kasitelty data))])
   [:turi:toteuttaja (:toteuttaja data)]
   [:turi:tilaaja (:tilaaja data)]
   (rakenna-turvallisuuskoordinaattori data)
   (rakenna-laatija data)
   (rakenna-ilmoittaja data)
   [:turi:tyotehtava (:tyotehtava data)]
   (rakenna-luokittelulista :turi:luokittelut :turi:luokittelu (:tyyppi data))
   (rakenna-luokittelulista :turi:vahinkoluokittelut :turi:luokittelu (:vahinkoluokittelu data))
   [:turi:vakavuusaste (:vakavuusaste data)]
   [:turi:aiheutuneet-seuraukset (:aiheutuneet_seuraukset data)]
   (rakenna-korjaavat-toimenpiteet data)
   (rakenna-kommentit data)
   (rakenna-liitteet data)
   (rakenna-sijainti data)
   (rakenna-henkilovahinko data)])

;; ----- Yllä oleva koodi on wanhaa ------ ;;

(def poikkeamatyyppi->numero
  {:tyotapaturma 8
   :vaaratilanne 32
   :turvallisuushavainto 64
   :muu 16})

(defn poikkeamatyypit->numerot [tyypit]
  (mapv
    (fn [tyyppi] [:tyyppi (poikkeamatyyppi->numero tyyppi)])
    tyypit))

(def ammatti->numero
  {:aluksen_paallikko 1
   :asentaja 2
   :asfalttityontekija 3
   :harjoittelija 4
   :hitsaaja 5
   :kunnossapitotyontekija 6
   :kansimies 7
   :kiskoilla_liikkuvan_tyokoneen_kuljettaja 8
   :konemies 9
   :kuorma-autonkuljettaja 10
   :liikenteenohjaaja 11
   :mittamies 12
   :panostaja 13
   :peramies 14
   :porari 15
   :rakennustyontekija 16
   :ratatyontekija 17
   :ratatyosta_vastaava 18
   :sukeltaja 19
   :sahkotoiden_ammattihenkilo 20
   :tilaajan_edustaja 21
   :turvalaiteasentaja 22
   :turvamies 23
   :tyokoneen_kuljettaja 24
   :tyonjohtaja 25
   :valvoja 26
   :veneenkuljettaja 27
   :vaylanhoitaja 28
   :muu_tyontekija 29
   :tyomaan_ulkopuolinen 30})

(def vamma->numero
  {:haavat_ja_pinnalliset_vammat 1
   :luunmurtumat 2
   :sijoiltaan_menot_nyrjahdykset_ja_venahdykset 3
   :amputoitumiset_ja_irti_repeamiset 4
   :tarahdykset_ja_sisaiset_vammat_ruhjevammat 5
   :palovammat_syopymat_ja_paleltumat 6
   :myrkytykset_ja_tulehdukset 7
   :hukkuminen_ja_tukehtuminen 8
   :aanen_ja_varahtelyn_vaikutukset 9
   :aarilampotilojen_valon_ja_sateilyn_vaikutukset 10
   :sokki 11
   :useita_samantasoisia_vammoja 12
   :muut 13
   :ei_tietoa 14})

(defn vammat->numerot [vammat]
  (mapv
    (fn [vammat] [:vammanlaatu (vamma->numero vammat)])
    vammat))

(def vahingoittunut-ruumiinosa->numero
  {:paan_alue 1
   :silmat 2
   :niska_ja_kaula 3
   :selka 4
   :vartalo 5
   :sormi_kammen 6
   :ranne 7
   :muu_kasi 8
   :nilkka 9
   :jalkatera_ja_varvas 10
   :muu_jalka 11
   :koko_keho 12
   :ei_tietoa 13})

(defn vahingoittuneet-ruumiinosat->numerot [vammat]
  (mapv
    (fn [vammat] [:vahingoittunutruumiinosa (vahingoittunut-ruumiinosa->numero vammat)])
    vammat))

(defn rakenna-tapahtumatiedot [data]
  (into [:tapahtumantiedot]
        (concat
          (poikkeamatyypit->numerot (:tyyppi data))
          [[:tapahtumapvm (xml/formatoi-paivamaara (:tapahtunut data))]
           [:tapahtumaaika (xml/formatoi-kellonaika (:tapahtunut data))]
           [:kuvaus (:kuvaus data)]])))

(defn rakenna-tapahtumapaikka [data]
  [:tapahtumapaikka
   [:paikka (:paikan-kuvaus data)]])

(defn rakenna-syyt-ja-seuraukset [data]
  (into [:syytjaseuraukset]
        (concat
          [[:seuraukset (:seuraukset data)]
           [:ammatti (ammatti->numero (:tyontekijanammatti data))]
           [:ammattimuutarkenne (:tyontekijanammattimuu data)]]
          (vammat->numerot (:vammat data))
          (vahingoittuneet-ruumiinosat->numerot (:vahingoittuneetruumiinosat data))
          [[:sairauspoissaolot (:sairauspoissaolopaivat data)]
           [:sairauspoissaolojatkuu (:sairauspoissaolojatkuu data)]
           [:sairaalahoitovuorokaudet (:sairaalavuorokaudet data)]])))

(defn rakenna-tapahtumakasittely [data]
  [:tapahtumankasittely
   [:otsikko (:tapahtuman-otsikko data)]
   [:luontipvm (xml/formatoi-paivamaara (:luotu data))]
   [:tila (turpodomain/kuvaa-turpon-tila (:tila data))]])

(defn rakenna-poikkeamatoimenpide [data]
  [:poikkeamatoimenpide
   [:otsikko "string"]
   [:kuvaus "string"]
   [:toteuttaja "string"]
   [:tila "1"]])

(defn rakenna-poikkeamaliite [data]
  [:poikkeamaliite
   [:tiedostonimi "string"]
   [:tiedosto "ZGVkaXQ="]])

(defn muodosta-viesti [data]
  ;; Harjaa koskemattomat XML-sanoman tagit jätetty koodiin kommentoituna, jotta
  ;; voidaan edelleen havainnollistaa koko sanoma
  [:imp:poikkeama
   {:xmlns:imp "http://importexport.xml.turi.oikeatoliot.fi"}
   (rakenna-tapahtumatiedot data)
   (rakenna-tapahtumapaikka data)
   (rakenna-syyt-ja-seuraukset data)
   (rakenna-tapahtumakasittely data)
   (rakenna-poikkeamatoimenpide data)
   (rakenna-poikkeamaliite data)])

(defn muodosta [data]
  (let [sisalto (muodosta-viesti data)
        xml (xml/tee-xml-sanoma sisalto)]
    (if (xml/validoi +xsd-polku+ "poikkeama-rest.xsd" xml)
      xml
      (let [virheviesti "Turvallisuuspoikkeamaa ei voida lähettää. XML ei ole validia."]
        (log/error virheviesti)
        (throw+ {:type :invalidi-turvallisuuspoikkeama-xml
                 :error virheviesti})))))
