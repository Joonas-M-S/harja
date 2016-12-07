(ns harja-laadunseuranta.tiedot.asetukset.kuvat)

;; Käytä SVG-spriteä aina kun mahdollista
(defn svg-sprite
  ([nimi] (svg-sprite nimi nil))
  ([nimi luokka]
  [:svg (when luokka {:class luokka})
   [:use {:href (str "#" nimi)}]]))

(defn- ikoni-uri [nimi]
  (str "img/" nimi))

(defn havainto-ikoni-uri [nimi]
  (str "img/havainnot/" nimi ".svg"))

(def +autonuoli+ (ikoni-uri "autonuoli.svg"))
(def +harja-logo+ (ikoni-uri "harja_logo_soft.svg"))
(def +harja-logo-ilman-tekstia+ (ikoni-uri "harja_logo_soft_ilman_tekstia.svg"))
(def +check+ (ikoni-uri "check.svg"))
(def +cross+ (ikoni-uri "cross.svg"))
(def +spinner+ (ikoni-uri "ajax-loader.gif"))
(def +hampurilaisvalikko+ (ikoni-uri "hampurilaisvalikko.svg"))
(def +havaintopiste+ (ikoni-uri "havaintopiste.png"))
(def +avausnuoli+ (ikoni-uri "avausnuoli.svg"))