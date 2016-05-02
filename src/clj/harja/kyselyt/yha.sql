-- name: poista-urakan-yha-tiedot!
-- Poistaa urakan yha-tiedot
DELETE FROM yhatiedot
WHERE urakka = :urakka;

-- name: lisaa-urakalle-yha-tiedot<!
-- Lisää urakalle YHA-tiedot
INSERT INTO yhatiedot
(urakka, yhatunnus, yhaid, yhanimi, elyt, vuodet, kohdeluettelo_paivitetty, luotu, linkittaja, muokattu)
VALUES (:urakka, :yhatunnus, :yhaid, :yhanimi, :elyt :: TEXT [], :vuodet :: INTEGER [], NULL, NOW(), :kayttaja, NOW());

-- name: paivita-yhatietojen-kohdeluettelon-paivitysaika<!
-- Päivittää urakan YHA-tietoihin kohdeluettelon uudeksi päivitysajaksi nykyhetken
UPDATE yhatiedot
SET
  kohdeluettelo_paivitetty = NOW(),
  muokattu                 = NOW()
WHERE urakka = :urakka;

-- name: hae-urakan-yhatiedot
SELECT
  yhatunnus,
  yhaid,
  yhanimi,
  elyt,
  vuodet
FROM yhatiedot
WHERE urakka = :urakka;

-- name: poista-urakan-yllapitokohteet!
DELETE FROM yllapitokohde
WHERE urakka = :urakka;

-- name: poista-urakan-yllapitokohdeosat!
DELETE FROM yllapitokohdeosa
WHERE yllapitokohde IN
      (SELECT id
       FROM yllapitokohde
       WHERE urakka = :urakka);

-- name: hae-urakoiden-sidontatiedot
SELECT
  yt.yhaid,
  u.nimi AS "sidottu-urakkaan"
FROM yhatiedot yt
  JOIN urakka u ON yt.urakka = u.id
WHERE yt.yhaid IN (:yhaidt);

-- name: luo-yllapitokohde<!
INSERT INTO yllapitokohde
(urakka, sopimus, kohdenumero, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys,
 yhatunnus, yhaid)
VALUES (:urakka,
        :sopimus,
        :kohdenumero,
        :nimi,
        :tr_numero,
        :tr_alkuosa,
        :tr_alkuetaisyys,
        :tr_loppuosa,
        :tr_loppuetaisyys,
        :yhatunnus,
        :yhaid)
ON CONFLICT ON CONSTRAINT yllapitokohde_uniikki_yhaid DO NOTHING;