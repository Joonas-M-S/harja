(ns harja.palvelin.integraatiot.integraatiopisteet.http
  "Yleiset apurit kutsujen lähettämiseen ulkoisiin järjestelmiin.
  Sisältää automaattiset lokitukset integraatiolokiin."
  (:require [taoensso.timbre :as log]
            [org.httpkit.client :as http]
            [harja.palvelin.integraatiot.integraatioloki :as integraatioloki]
            [harja.palvelin.integraatiot.api.tyokalut.virheet :as virheet])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(def timeout-aika-ms 10000)

(defn rakenna-http-kutsu [{:keys [metodi otsikot parametrit kayttajatunnus salasana kutsudata timeout] :as optiot}]
  (let [kutsu {}]
    (-> kutsu
        (cond-> (not-empty otsikot) (assoc :headers otsikot)
                (not-empty parametrit) (assoc :query-params parametrit)
                (and (not-empty kayttajatunnus)) (assoc :basic-auth [kayttajatunnus salasana])
                (or (= metodi "post") (= metodi "put")) (assoc :body kutsudata)
                timeout (assoc :timeout timeout)))))

(defn tee-http-kutsu [integraatioloki jarjestelma integraatio tapahtuma-id url metodi otsikot parametrit kayttajatunnus salasana kutsudata]
  (try
    (let [kutsu (rakenna-http-kutsu {:metodi metodi :otsikot otsikot :parametrit parametrit :kayttajatunnus kayttajatunnus
                                     :salasana salasana :kutsudata kutsudata :timeout timeout-aika-ms})]
      (case metodi
        "post" @(http/post url kutsu)
        "get" @(http/get url kutsu)
        "put" @(http/put url kutsu)
        "delete" @(http/delete url kutsu)
        "head" @(http/head url kutsu)
        (throw+
          {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
           :virheet [{:koodi :tuntematon-http-metodi :viesti (str "Tuntematon HTTP metodi:" metodi)}]})))
    (catch Exception e
      (log/error e (format "HTTP-kutsukäsittelyssä tapahtui poikkeus.  (järjestelmä: %s, integraatio: %s, URL: %s)" jarjestelma integraatio url))
      (integraatioloki/kirjaa-epaonnistunut-integraatio integraatioloki nil (str " Tapahtui poikkeus: " e) tapahtuma-id nil)
      (throw+
        {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
         :virheet [{:koodi :poikkeus :viesti (str "HTTP-kutsukäsittelyssä tapahtui odottamaton virhe.")}]}))))

(defn laheta-kutsu
  ([integraatioloki integraatio jarjestelma url metodi otsikot parametrit kutsudata kasittele-vastaus]
   (laheta-kutsu integraatioloki integraatio jarjestelma url metodi otsikot parametrit nil nil kutsudata kasittele-vastaus))
  ([integraatioloki integraatio jarjestelma url metodi otsikot parametrit kayttajatunnus salasana kutsudata kasittele-vastaus]
   (log/debug (format "Lähetetään HTTP %s -kutsu integraatiolle: %s, järjestelmään: %s, osoite: %s, metodi: %s, data: %s, otsikkot: %s, parametrit: %s"
                      metodi integraatio jarjestelma url metodi kutsudata otsikot parametrit))

   (let [tapahtuma-id (integraatioloki/kirjaa-alkanut-integraatio integraatioloki jarjestelma integraatio nil nil)
         sisaltotyyppi (get otsikot " Content-Type ")]

     (integraatioloki/kirjaa-rest-viesti integraatioloki tapahtuma-id "ulos" url sisaltotyyppi kutsudata otsikot (str parametrit))
     (let [{:keys [status body error headers]} (tee-http-kutsu integraatioloki jarjestelma integraatio tapahtuma-id url metodi otsikot parametrit kayttajatunnus salasana kutsudata)
           lokiviesti (integraatioloki/tee-rest-lokiviesti "sisään" url sisaltotyyppi body headers nil)]
       (log/debug (format " Palvelu palautti: tila: %s , otsikot: %s , data: %s" status headers body))

       (if (or error
               (not (= 200 status)))
         (do
           (log/error (format "Kutsu palveluun: %s epäonnistui. Virhe: %s " url error))
           (log/error "Virhetyyppi: " (type error))
           (integraatioloki/kirjaa-epaonnistunut-integraatio integraatioloki lokiviesti (str " Virhe: " error) tapahtuma-id nil)
           ;; Virhetilanteissa Httpkit ei heitä kiinni otettavia exceptioneja, vaan palauttaa error-objektin.
           ;; Siksi erityyppiset virheet käsitellään instance-tyypin selvittämisellä.
           (cond (or (instance? java.net.ConnectException error)
                     (instance? org.httpkit.client.TimeoutException error))
                 (throw+ {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
                          :virheet [{:koodi :ulkoinen-jarjestelma-palautti-virheen :viesti "Ulkoiseen järjestelmään ei saada yhteyttä."}]})
                 :default
                 (throw+ {:type    virheet/+ulkoinen-kasittelyvirhe-koodi+
                          :virheet [{:koodi :ulkoinen-jarjestelma-palautti-virheen :viesti "Ulkoisen järjestelmän kommunikoinnissa tapahtui odottaman virhe."}]})))
         (do
           (let [vastausdata (kasittele-vastaus body headers)]
             (log/debug (format "Kutsu palveluun: %s onnistui." url))
             (integraatioloki/kirjaa-onnistunut-integraatio integraatioloki lokiviesti nil tapahtuma-id nil)
             vastausdata)))))))

(defn laheta-get-kutsu
  "Tekee synkronisen HTTP GET -kutsun"
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kayttajatunnus salasana kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "get" otsikot parametrit kayttajatunnus salasana nil kasittele-vastaus-fn))
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "get" otsikot parametrit nil kasittele-vastaus-fn)))

(defn laheta-post-kutsu
  "Tekee synkronisen HTTP POST -kutsun"
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kayttajatunnus salasana kutsudata kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "post" otsikot parametrit kayttajatunnus salasana kutsudata kasittele-vastaus-fn))
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kutsudata kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "post" otsikot parametrit kutsudata kasittele-vastaus-fn)))

(defn laheta-head-kutsu
  "Tekee synkronisen HTTP HEAD -kutsun"
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kayttajatunnus salasana kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "head" otsikot parametrit kayttajatunnus salasana nil kasittele-vastaus-fn))
  ([integraatioloki integraatio jarjestelma url otsikot parametrit kasittele-vastaus-fn]
   (laheta-kutsu integraatioloki integraatio jarjestelma url "head" otsikot parametrit nil kasittele-vastaus-fn)))
