-- name: hae-pohjavesialueet
-- Hakee pohjavesialueet annetulle hallintayksikölle
SELECT
  id,
  nimi,
  alue,
  tunnus
FROM pohjavesialueet_hallintayksikoittain
WHERE hallintayksikko = :hallintayksikko AND suolarajoitus IS TRUE;

-- name: hae-urakan-pohjavesialueet
-- Hakee hoidon alueurakan alueella olevat pohjavesialueet
SELECT
  p.nimi,
  p.tunnus,
  p.alue
FROM pohjavesialueet_urakoittain p
WHERE p.urakka = :urakka-id AND suolarajoitus IS TRUE;

-- name: hae-urakan-pohjavesialueet-teittain
-- Hakee hoidon alueurakan alueella olevat pohjavesialueet teittain
SELECT
  p.nimi,
  p.tunnus,
  pa.tr_numero AS tie,
  p.alue
FROM pohjavesialueet_urakoittain p
  LEFT JOIN pohjavesialue pa ON pa.tunnus = p.tunnus
WHERE p.urakka = :urakka-id AND p.suolarajoitus IS TRUE ORDER BY p.nimi;

-- name: poista-pohjavesialueet!
-- Poistaa kaikki pohjavesialueet
DELETE FROM pohjavesialue;

-- name: luo-pohjavesialue!
INSERT INTO pohjavesialue
    (nimi,
     tunnus,
     alue,
     suolarajoitus,
     tr_numero,
     tr_alkuosa,
     tr_alkuetaisyys,
     tr_loppuosa,
     tr_loppuetaisyys,
     tr_ajorata,
     luotu,
     luoja,
     aineisto_id) VALUES
    (:nimi,
     :tunnus,
     ST_GeomFromText(:geometria) :: GEOMETRY,
     :suolarajoitus,
     :tr_numero,
     :tr_alkuosa,
     :tr_alkuetaisyys,
     :tr_loppuosa,
     :tr_loppuetaisyys,
     :tr_ajorata,
     current_timestamp,
     (select id from kayttaja where kayttajanimi = 'Integraatio'),
     :aineisto_id);

-- name: paivita-pohjavesialueet
SELECT paivita_pohjavesialueet();

-- name: paivita-pohjavesialue-kooste!
SELECT paivita_pohjavesialue_kooste();
