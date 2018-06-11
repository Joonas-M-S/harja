-- name: hae-yllapitokohteen-tiedot-tietyoilmoitukselle
SELECT
  ypk.id                 AS "yllapitokohde-id",
  ypk.tr_numero          AS "tr-numero",
  ypk.tr_alkuosa         AS "tr-alkuosa",
  ypk.tr_alkuetaisyys    AS "tr-alkuetaisyys",
  ypk.tr_loppuosa        AS "tr-loppuosa",
  ypk.tr_loppuetaisyys   AS "tr-loppuetaisyys",
  ypkat.kohde_alku       AS alku,
  ypkat.paallystys_loppu AS loppu,
  u.id                   AS "urakka-id",
  u.nimi                 AS "urakka-nimi",
  u.tyyppi               AS "urakkatyyppi",
  u.sampoid              AS "urakka-sampo-id",
  urk.id                 AS "urakoitsija-id",
  urk.nimi               AS "urakoitsija-nimi",
  ely.id                 AS "tilaaja-id",
  ely.nimi               AS "tilaaja-nimi"
FROM yllapitokohde ypk
  JOIN yllapitokohteen_aikataulu ypkat ON ypk.id = ypkat.yllapitokohde
  JOIN urakka u ON ypk.urakka = u.id
  LEFT JOIN organisaatio urk ON u.urakoitsija = urk.id
  LEFT JOIN organisaatio ely ON u.hallintayksikko = ely.id
WHERE ypk.id = :kohdeid;

-- name: hae-urakan-tiedot-tietyoilmoitukselle
SELECT
  u.id        AS "urakka-id",
  u.nimi      AS "urakka-nimi",
  u.tyyppi    AS "urakkatyyppi",
  u.sampoid   AS "urakka-sampo-id",
  urk.id      AS "urakoitsija-id",
  urk.nimi    AS "urakoitsija-nimi",
  urk.ytunnus AS "urakoitsija-ytunnus",
  ely.id      AS "tilaaja-id",
  ely.nimi    AS "tilaaja-nimi"
FROM urakka u
  JOIN organisaatio urk ON u.urakoitsija = urk.id
  JOIN organisaatio ely ON u.hallintayksikko = ely.id
WHERE u.id = :urakkaid;

-- name: lisaa-tietyoilmoituksen-email-lahetys!
INSERT INTO tietyoilmoituksen_email_lahetys (tietyoilmoitus, lahetysid, lahetetty)
     VALUES (:id, :lahetysid, current_timestamp);

-- name: merkitse-tietyoilmoituksen-sahkopostilahetys-kuitatuksi!
UPDATE tietyoilmoituksen_email_lahetys
SET kuitattu = current_timestamp
WHERE lahetysid = :lahetysid;

-- name: merkitse-tietyoilmoitukselle-lahetysvirhe!
UPDATE tietyoilmoituksen_email_lahetys
SET lahetysvirhe = current_timestamp
WHERE tietyoilmoitus = :id;

-- name: merkitse-tietyoilmoitukselle-lahetysvirhe-lahetysidlla!
UPDATE tietyoilmoituksen_email_lahetys
SET lahetysvirhe = current_timestamp
WHERE lahetysid = :lahetysid;