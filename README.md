# Liikenneviraston Harja järjestelmä #

Projekti on client/server, jossa serveri on Clojure sovellus (http-kit) ja
client on ClojureScript sovellus, joka käyttää Reagentia, OpenLayersiä ja Bootstrap CSSää.

Tietokantana PostgreSQL PostGIS laajennoksella. Hostaus Solitan infrassa.

Autentikointiin käytetään KOKAa.

## Hakemistorakenne ##

Alustava 
Harja repon hakemistorakenne:

- README                    (yleinen readme)

- src/                      (kaikki lähdekoodit)
  - cljc/                   (palvelimen ja asiakkaan jaettu koodi)
  - cljs/                   (asiakaspuolen ClojureScript koodi)
    - harja/asiakas/
      - ui/                 (yleisiä UI komponentteja ja koodia)
      - nakymat/            (UI näkymät)
      - kommunikointi.cljs  (serverikutsujen pääpiste)
      - main.cljs           (aloituspiste, jota sivun latauduttua kutsutaan)
  - cljs-{dev|prod}/        (tuotanto/kehitysbuildeille spesifinen cljs source)
      - harja/asiakas/
        - lokitus.cljs      (lokitus, tuotantoversiossa no-op, kehitysversiossa console.log tms)
  - clj/                    (palvelimen koodi)
    - harja/palvelin/
      - komponentit/        (Yleiset komponentit: tietokanta, todennus, HTTP-palvelin, jne)
      - lokitus/            (Logitukseen liittyvää koodia)
      - integraatiot/       (Integraatioiden toteutukset)
      - api/                (Harja API endpointit ja tukikoodi)
      - palvelut/           (Harja asiakkaalle palveluja tarjoavat EDN endpointit)
      - main.cljs           (palvelimen aloituspiste)

- (dev-)resources/          (web-puolen resurssit)
  - css/                    (ulkoiset css tiedostot)
  - js/                     (ulkoiset javascript tiedostot)


## Integraatiot

MULEsta on luovuttu, integraatiot suoraan backendistä.

## Tietokanta

Tietokannan määrittely ja migraatio (SQL tiedostot ja flyway taskit) ovat omassa repossaan: harja-tietokanta 

Ohjeet kehitysympäristön tietokannan pystytykseen Vagrantilla löytyvät tiedostosta `vagrant/README.md`


## Staging tietokannan sisällön muokkaus
* Lisää itsellesi tiedosto ~/.ssh/config johon sisällöksi:
Host harja-*-test
  ProxyCommand ssh harja-jenkins.solitaservices.fi -W %h:%p

Host harja-*-stg
  ProxyCommand ssh harja-jenkins.solitaservices.fi -W %h:%p

* Sourceta uusi config tai avaa uusi terminaali-ikkuna. 

* Avaa VPN putki.

* Luo itsellesi SSH-avainpari ja pyydä tuttuja laittamaan julkinen avain palvelimelle.

ssh -L7777:localhost:5432 harja-dfb1-stg
 * Luo yhteys esim. käyttämäsi IDE:n avulla,
    * tietokanta: harja, username: flyway salasana: kysy tutuilta

## Testipalvelimen tietokannan päivitys, vanha ja huono tapa, mutta säilyköön ohje jälkipolville:
 * Avaa VPN putki <br/>
 <code>
    ssh harja-jenkins.solitaservices.fi
    [jarnova@harja-jenkins ~]$ sudo bash <br/>
    [root@harja-jenkins jarnova]# su jenkins <br/>
    bash-4.2$ ssh harja-db1-test <br/>
    Last login: Mon Mar 16 15:23:22 2015 from 172.17.238.100 <br/>
    [jenkins@harja-db1-test ~]$ sudo bash <br/>
    [root@harja-db1-test jenkins]# su postgres <br/>
    bash-4.2$ psql harja <br/>
</code>
 * Tee temput


## Autogeneroi nuolikuvat SVG:nä

(def varit {"punainen" "rgb(255,0,0)"
            "oranssi" "rgb(255,128,0)"
            "keltainen" "rgb(255,255,0)"
            "lime" "rgb(128,255,0)"
	    "vihrea" "rgb(0,255,0)"
 	    "turkoosi" "rgb(0,255,128)"
 	    "syaani" "rgb(0,255,255)"
 	    "sininen" "rgb(0,128,255)"
 	    "tummansininen" "rgb(0,0,255)"
 	    "violetti" "rgb(128,0,255)"
 	    "magenta" "rgb(255,0,255)"
 	    "pinkki" "rgb(255,0,128)"})

(for [[vari rgb] varit]
  (spit (str "resources/public/images/nuoli-" vari ".svg")
  	(str "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 6 9\">
   <polygon points=\"5.5,5 0,9 0,7 3,5 0,2 0,0 5.5,5\" style=\"fill:" rgb ";\" />
</svg>")))