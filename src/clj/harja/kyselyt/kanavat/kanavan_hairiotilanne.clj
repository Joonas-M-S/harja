(ns harja.kyselyt.kanavat.kanavan-hairiotilanne
  (:require [specql.core :refer [fetch insert! update!]]
            [jeesql.core :refer [defqueries]]
            [specql.op :as op]
            [harja.pvm :as pvm]
            [harja.id :as id]
            [taoensso.timbre :as log]

            [harja.domain.kanavat.hairiotilanne :as hairiotilanne]
            [harja.domain.muokkaustiedot :as muokkaustiedot]
            [harja.domain.vesivaylat.materiaali :as materiaali]
            [clojure.set :as set]))

(defn hae-kanavatoimenpiteet [db hakuehdot]
  (fetch db
         ::hairiotilanne/hairiotilanne
         (set/union
           hairiotilanne/perustiedot
           hairiotilanne/viittaus-idt
           hairiotilanne/muokkaustiedot
           hairiotilanne/kuittaajan-tiedot
           hairiotilanne/kohteen-tiedot
           hairiotilanne/kohteenosan-tiedot)
         hakuehdot))

(defn hae-sopimuksen-hairiotilanteet-aikavalilta [db hakuehdot]
  (let [urakka-id (::hairiotilanne/urakka-id hakuehdot)
        sopimus-id (:haku-sopimus-id hakuehdot)
        vikaluokka (:haku-vikaluokka hakuehdot)
        korjauksen-tila (:haku-korjauksen-tila hakuehdot)
        [odotusaika-alku odotusaika-loppu] (:haku-odotusaika-h hakuehdot)
        [korjausaika-alku korjausaika-loppu] (:haku-korjausaika-h hakuehdot)
        paikallinen-kaytto? (:haku-paikallinen-kaytto? hakuehdot)
        [aikavali-alku aikavali-loppu] (:haku-aikavali hakuehdot)]

    (hae-kanavatoimenpiteet db (op/and
                                 (op/or {::muokkaustiedot/poistettu? op/null?}
                                        {::muokkaustiedot/poistettu? false})
                                 (merge
                                   {::hairiotilanne/urakka-id urakka-id}
                                   (when sopimus-id
                                     {::hairiotilanne/sopimus-id sopimus-id})
                                   (when vikaluokka
                                     {::hairiotilanne/vikaluokka vikaluokka})
                                   (when korjauksen-tila
                                     {::hairiotilanne/korjauksen-tila korjauksen-tila})
                                   (when (some? paikallinen-kaytto?)
                                     {::hairiotilanne/paikallinen-kaytto? paikallinen-kaytto?})
                                   (when (and odotusaika-alku odotusaika-loppu)
                                     {::hairiotilanne/odotusaika-h (op/between odotusaika-alku odotusaika-loppu)})
                                   (when (and korjausaika-alku korjausaika-loppu)
                                     {::hairiotilanne/korjausaika-h (op/between korjausaika-alku korjausaika-loppu)})
                                   (when (and aikavali-alku aikavali-loppu)
                                     {::hairiotilanne/havaintoaika (op/between aikavali-alku aikavali-loppu)}))))))

(defn tallenna-hairiotilanne [db kayttaja-id hairiotilanne]
  (if (id/id-olemassa? (::hairiotilanne/id hairiotilanne))
    (let [kanavatoimenpide (assoc hairiotilanne
                             ::muokkaustiedot/muokattu (pvm/nyt)
                             ::muokkaustiedot/muokkaaja-id kayttaja-id)]
      (update! db ::hairiotilanne/hairiotilanne kanavatoimenpide {::hairiotilanne/id (::hairiotilanne/id kanavatoimenpide)})
      ;; (log/debug "päivitetään, palautetaan id-tieto")
      (first (fetch db ::hairiotilanne/hairiotilanne #{::hairiotilanne/id} {::hairiotilanne/id (::hairiotilanne/id kanavatoimenpide)})))
    (let [kanavatoimenpide (assoc hairiotilanne
                             ::hairiotilanne/kuittaaja-id kayttaja-id
                             ::muokkaustiedot/luotu (pvm/nyt)
                             ::muokkaustiedot/luoja-id kayttaja-id)
          lisatty-rivi (insert! db ::hairiotilanne/hairiotilanne kanavatoimenpide)]
      ;; (log/debug "lisätty rivi: " (pr-str lisatty-rivi))
      lisatty-rivi)))
