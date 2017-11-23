(ns harja.kyselyt.kanavat.kanavan-hairiotilanne
  (:require [specql.core :refer [fetch insert! update!]]
            [harja.domain.kanavat.hairiotilanne :as hairiotilanne]
            [harja.domain.muokkaustiedot :as muokkaustiedot]
            [jeesql.core :refer [defqueries]]
            [specql.op :as op]
            [harja.pvm :as pvm]
            [harja.id :as id]))

(defn hae-kanavatoimenpiteet [db hakuehdot]
  (fetch db ::hairiotilanne/hairiotilanne hairiotilanne/perustiedot+kanava+kohde hakuehdot))

(defn hae-sopimuksen-hairiotilanteet-aikavalilta [db hakuehdot]
  (let  [urakka-id (::hairiotilanne/urakka-id hakuehdot)
         sopimus-id (:haku-sopimus-id hakuehdot)
         vikaluokka (:haku-vikaluokka hakuehdot)
         korjauksen-tila (:haku-korjauksen-tila hakuehdot)
         [odotusaika-alku odotusaika-loppu] (:haku-odotusaika-h hakuehdot)
         [korjausaika-alku korjausaika-loppu] (:haku-korjausaika-h hakuehdot)
         paikallinen-kaytto? (:haku-paikallinen-kaytto? hakuehdot)
         [aikavali-alku aikavali-loppu] (:haku-aikavali hakuehdot)]
    (hae-kanavatoimenpiteet db (merge
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
                                  {::hairiotilanne/pvm (op/between aikavali-alku aikavali-loppu)})))))

(defn tallenna-hairiotilanne [db kayttaja-id kanavatoimenpide]
  (if (id/id-olemassa? (::hairiotilanne/id kanavatoimenpide))
    (let [kanavatoimenpide (assoc kanavatoimenpide
                             ::muokkaustiedot/muokattu (pvm/nyt)
                             ::muokkaustiedot/muokkaaja-id kayttaja-id)]
      (update! db ::hairiotilanne/hairiotilanne kanavatoimenpide {::hairiotilanne/id (::hairiotilanne/id kanavatoimenpide)}))
    (let [kanavatoimenpide (assoc kanavatoimenpide
                             ::muokkaustiedot/luotu (pvm/nyt)
                             ::muokkaustiedot/luoja-id kayttaja-id)]
      (insert! db ::hairiotilanne/hairiotilanne kanavatoimenpide))))