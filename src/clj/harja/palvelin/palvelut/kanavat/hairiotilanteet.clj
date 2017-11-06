(ns harja.palvelin.palvelut.kanavat.hairiotilanteet
  (:require [clojure.java.jdbc :as jdbc]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [harja.palvelin.komponentit.http-palvelin :refer [julkaise-palvelu poista-palvelut]]
            [harja.domain.oikeudet :as oikeudet]
            [harja.domain.kanavat.hairiotilanne :as hairio]
            [harja.kyselyt.konversio :as konv]
            [harja.kyselyt.kanavat.kanavat :as q]
            [specql.core :as specql]
            [specql.op :as op]
            [harja.domain.kanavat.kanava :as kan]
            [clojure.set :as set]))

(defn hae-hairiotilanteet [db user tiedot]
  ;; TODO Oikeustarkistus + testi
  ;; TODO Testi haulle
  (let [urakka-id (::hairio/urakka-id tiedot)
        sopimus-id (:haku-sopimus-id tiedot)
        vikaluokka (:haku-vikaluokka tiedot)
        korjauksen-tila (:haku-korjauksen-tila tiedot)
        [odotusaika-alku odotusaika-loppu] (:haku-odotusaika-h tiedot)
        [korjausaika-alku korjausaika-loppu] (:haku-korjausaika-h tiedot)
        paikallinen-kaytto? (:haku-paikallinen-kaytto? tiedot)
        [aikavali-alku aikavali-loppu] (:haku-aikavali tiedot)]
    (reverse (sort-by
               ::hairio/pvm
               (specql/fetch
                 db
                 ::hairio/hairiotilanne
                 hairio/perustiedot+kanava+kohde
                 (merge
                   {::hairio/urakka-id urakka-id}
                   (when sopimus-id
                     {::hairio/sopimus-id sopimus-id})
                   (when vikaluokka
                     {::hairio/vikaluokka vikaluokka})
                   (when korjauksen-tila
                     {::hairio/korjauksen-tila korjauksen-tila})
                   (when (some? paikallinen-kaytto?)
                     {::hairio/paikallinen-kaytto? paikallinen-kaytto?})
                   ;; TODO op/between ei toimi?
                   #_(when (and odotusaika-alku odotusaika-loppu)
                     {::hairio/odotusaika-h (op/between odotusaika-alku odotusaika-loppu)})
                   ;; TODO op/between ei toimi?
                   #_(when (and korjausaika-alku korjausaika-loppu)
                     {::hairio/korjausaika-h (op/between korjausaika-alku korjausaika-loppu)})
                   ;; TODO ei toimi oikein!?
                   #_(when (and aikavali-alku aikavali-loppu)
                       {::hairio/pvm (op/between aikavali-alku aikavali-loppu)})))))))

(defrecord Hairiotilanteet []
  component/Lifecycle
  (start [{http :http-palvelin
           db :db :as this}]
    (julkaise-palvelu
      http
      :hae-hairiotilanteet
      (fn [user tiedot]
        (hae-hairiotilanteet db user tiedot))
      {:kysely-spec ::hairio/hae-hairiotilanteet-kysely
       :vastaus-spec ::hairio/hae-hairiotilanteet-vastaus})
    this)

  (stop [this]
    (poista-palvelut
      (:http-palvelin this)
      :hae-hairiotilanteet)
    this))
