(ns harja-laadunseuranta.core
  (:require [taoensso.timbre :as log]
            [gelfino.timbre :as gt]
            [org.httpkit.server :as server]
            [compojure.api.sweet :refer :all]
            [compojure.api.core :as compojure-api]
            [ring.util.http-response :refer :all]
            [ring.util.response :refer [redirect]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [harja-laadunseuranta.tietokanta :as tietokanta]
            [harja-laadunseuranta.kyselyt :as q]
            [harja-laadunseuranta.tarkastukset :as tarkastukset]
            [harja-laadunseuranta.schemas :as schemas]
            [harja-laadunseuranta.utils :as utils :refer [respond]]
            [harja-laadunseuranta.config :as c]
            [schema.core :as s]
            [clojure.core.match :refer [match]]
            [clojure.java.jdbc :as jdbc]
            [compojure.route :as route]
            [compojure.api.exception :as ex]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [com.stuartsierra.component :as component])
  (:import (org.postgis PGgeometry))
  (:gen-class))

(def db tietokanta/db)

(defn- tallenna-merkinta! [tx vakiohavainto-idt merkinta]
  (q/tallenna-reittimerkinta! tx {:id            (:id merkinta)
                                  :tarkastusajo  (:tarkastusajo merkinta)
                                  :aikaleima     (:aikaleima merkinta)
                                  :x             (:lon (:sijainti merkinta))
                                  :y             (:lat (:sijainti merkinta))
                                  :lampotila     (get-in merkinta [:mittaukset :lampotila])
                                  :lumisuus      (get-in merkinta [:mittaukset :lumisuus])
                                  :tasaisuus     (get-in merkinta [:mittaukset :tasaisuus])
                                  :kitkamittaus  (get-in merkinta [:mittaukset :kitkamittaus])
                                  :kiinteys      (get-in merkinta [:mittaukset :kiinteys])
                                  :polyavyys     (get-in merkinta [:mittaukset :polyavyys])
                                  :sivukaltevuus (get-in merkinta [:mittaukset :sivukaltevuus])
                                  :havainnot     (mapv vakiohavainto-idt (:havainnot merkinta))
                                  :kuvaus        (get-in merkinta [:kuvaus])
                                  :laadunalitus  (get-in merkinta [:laadunalitus])
                                  :kuva          (get-in merkinta [:kuva])}))

(defn- tallenna-kuva! [tx {:keys [data mime-type]} kayttaja-id]
  (let [decoded-data (b64/decode (.getBytes data "UTF-8"))
        oid (tietokanta/tallenna-lob (io/input-stream decoded-data))]
    (:id (q/tallenna-kuva<! tx {:lahde "harja-ls-mobiili"
                                :tyyppi mime-type
                                :koko (count decoded-data)
                                :pikkukuva (tietokanta/tee-thumbnail decoded-data)
                                :oid oid
                                :luoja kayttaja-id}))))

(defn- tallenna-multipart-kuva! [tx {:keys [tempfile content-type size]} kayttaja-id]
  (let [oid (tietokanta/tallenna-lob (io/input-stream tempfile))]
    (:id (q/tallenna-kuva<! tx {:lahde "harja-ls-mobiili"
                                :tyyppi content-type
                                :koko size
                                :pikkukuva (tietokanta/tee-thumbnail tempfile)
                                :oid oid
                                :luoja kayttaja-id}))))

(defn- tallenna-merkinnat! [kirjaukset kayttaja-id]
  (jdbc/with-db-transaction [tx @db]
    (let [vakiohavainto-idt (q/hae-vakiohavaintoavaimet tx)]
      (doseq [merkinta (:kirjaukset kirjaukset)]
        (tallenna-merkinta! tx vakiohavainto-idt merkinta)))))

(defn merkitse-ajo-paattyneeksi! [tx tarkastusajo-id kayttaja]
  (q/paata-tarkastusajo! tx {:id tarkastusajo-id
                             :kayttaja (:id kayttaja)}))

(defn- paata-tarkastusajo! [tarkastusajo kayttaja]
  (jdbc/with-db-transaction [tx @db]
    (let [tarkastusajo-id (-> tarkastusajo :tarkastusajo :id)
          urakka-id (:id (first (q/paattele-urakka tx {:tarkastusajo tarkastusajo-id})))
          merkinnat (q/hae-reitin-merkinnat tx {:tarkastusajo tarkastusajo-id
                                                :treshold 100})
          merkinnat-tr-osoitteilla (tarkastukset/lisaa-reittimerkinnoille-tieosoite merkinnat)
          tarkastukset (-> (tarkastukset/reittimerkinnat-tarkastuksiksi merkinnat-tr-osoitteilla)
                           (tarkastukset/lisaa-tarkastuksille-urakka-id urakka-id))]
      (tarkastukset/tallenna-tarkastukset! tarkastukset kayttaja)
      (merkitse-ajo-paattyneeksi! tx tarkastusajo-id kayttaja))))

(defn- tarkastustyypiksi [tyyppi]
  (condp = tyyppi
    :kelitarkastus 1
    :soratietarkastus 2
    :paallystys 3
    :tiemerkinta 4
    0))

(defn- luo-uusi-tarkastusajo! [tiedot kayttaja]
  (q/luo-uusi-tarkastusajo<! @db {:ulkoinen_id 0
                                  :kayttaja (:id kayttaja)
                                  :tyyppi (tarkastustyypiksi (-> tiedot :tyyppi))}))

(defn- hae-tr-osoite [lat lon treshold]
  (try
    (first (q/hae-tr-osoite @db {:y lat
                                 :x lon
                                 :treshold treshold}))
    (catch Exception e
      nil)))

(defn- hae-tr-tiedot [lat lon treshold]
  (let [pos {:y lat
             :x lon
             :treshold treshold}
        talvihoitoluokka (q/hae-pisteen-hoitoluokka @db (assoc pos :tietolaji "talvihoito")
                                                    )
        soratiehoitoluokka (q/hae-pisteen-hoitoluokka @db (assoc pos :tietolaji "soratie")
                                                      )]
    {:talvihoitoluokka (:hoitoluokka_pisteelle (first talvihoitoluokka))
     :soratiehoitoluokka (:hoitoluokka_pisteelle (first soratiehoitoluokka))
     :tr-osoite (hae-tr-osoite lat lon treshold)}))

(defn- hae-urakkatyypin-urakat [urakkatyyppi kayttaja]
  (println "hae ur tyyypin urakat, kayttaja " kayttaja)
  (let [urakat (q/hae-urakkatyypin-urakat @db {:tyyppi urakkatyyppi})]
    urakat))

(defapi laadunseuranta-api
  {:format {:formats [:transit-json]}
   :exceptions {:handlers {::ex/default utils/poikkeuskasittelija}}}

  (POST "/reittimerkinta" []
        :body [kirjaukset schemas/Havaintokirjaukset]
        :summary "Tallentaa reittimerkinnat"
        :kayttaja kayttaja
        :return {:ok s/Str}
        (respond (tallenna-merkinnat! kirjaukset (:id kayttaja))
                 "Reittimerkinta tallennettu"))

  (POST "/paata-tarkastusajo" []
        :body [tarkastusajo schemas/TarkastuksenPaattaminen]
        :kayttaja kayttaja
        :summary "Päättää tarkastusajon"
        :return {:ok s/Str}
        (respond (log/debug "Päätetään tarkastusajo " tarkastusajo)
                 (paata-tarkastusajo! tarkastusajo kayttaja)
                 "Tarkastusajo päätetty"))

  (POST "/uusi-tarkastusajo" []
        :body [tiedot s/Any]
        :kayttaja kayttaja
        :summary "Luo uuden tarkastusajon"
        :return {:ok s/Any}
        (respond (log/debug "Luodaan uusi tarkastusajo " tiedot)
                 (luo-uusi-tarkastusajo! tiedot kayttaja)))

  (POST "/hae-tr-tiedot" []
        :body [koordinaatit s/Any]
        :summary "Hakee tierekisterin tiedot annetulle pisteelle"
        :return {:ok s/Any}
        (respond (log/debug "Haetaan tierekisteritietoja pisteelle " koordinaatit)
                 (let [{:keys [lat lon treshold]} koordinaatit]
                   (hae-tr-tiedot lat lon treshold))))

  (POST "/urakkatyypin-urakat" []
        :body [urakkatyyppi s/Str]
        :kayttaja kayttaja
        :summary "Hakee urakkatyypin urakat"
        :return {:ok s/Any}
        (respond (log/debug "Haetaan urakkatyypin urakat " urakkatyyppi)
                 (hae-urakkatyypin-urakat urakkatyyppi kayttaja)))

  (GET "/hae-kayttajatiedot" []
       :summary "Hakee käyttäjän tiedot"
       :kayttaja kayttaja
       :return {:ok s/Any}
       (respond (log/debug "Käyttäjän tietojen haku")
                {:kayttajanimi (:kayttajanimi kayttaja)
                 :nimi (str (:etunimi kayttaja) " " (:sukunimi kayttaja))
                 :vakiohavaintojen-kuvaukset (q/hae-vakiohavaintojen-kuvaukset @db)})))

(defn- tallenna-liite [req]
  (jdbc/with-db-transaction [tx @db]
    (let [id (tallenna-multipart-kuva! tx (get-in req [:multipart-params "liite"]) (get-in req [:kayttaja :id]))]
      {:status 200
       :headers {"Content-Type" "text/plain"}
       :body (str id)})))

(defn luo-routet [todennus]
  (compojure-api/routes
   (GET "/" [] (redirect (utils/polku "/index.html")))
   (middleware [(partial utils/wrap-kayttajatarkistus todennus)]
               (context "/api" [] laadunseuranta-api))
   (middleware [(partial utils/wrap-kayttajatarkistus todennus)
                wrap-multipart-params]
               (POST "/tallenna-liite" req tallenna-liite))
   (route/resources "/" {:root "public/laadunseuranta"})
   (route/not-found "Page not found")))


(defn start-server [todennus]
  (log/info "Harja-laadunseuranta käynnistyy")
  (server/run-server (luo-routet todennus) (:http-palvelin @c/config)))

(defrecord Laadunseuranta [asetukset]
  component/Lifecycle
  (start [{db :db
           todennus :todennus
           :as this}]
    (c/aseta-config! asetukset)
    (tietokanta/aseta-tietokanta! db)
    (assoc this ::sulje-palvelin (start-server todennus)))

  (stop [{sulje ::sulje-palvelin :as this}]
    (sulje)
    (dissoc this ::sulje-palvelin)))
