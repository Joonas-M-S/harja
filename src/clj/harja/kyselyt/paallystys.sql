-- name: hae-urakan-paallystysilmoitukset
-- Hakee urakan kaikki päällystysilmoitukset
SELECT
  yllapitokohde.id            AS "paallystyskohde-id",
  pi.tila,
  nimi,
  kohdenumero,
  pi.paatos_tekninen_osa      AS "paatos-tekninen-osa",
  pi.paatos_taloudellinen_osa AS "paatos-taloudellinen-osa",
  sopimuksen_mukaiset_tyot    AS "sopimuksen-mukaiset-tyot",
  arvonvahennykset,
  bitumi_indeksi              AS "bitumi-indeksi",
  kaasuindeksi,
  lahetetty,
  lahetys_onnistunut          AS "lahetys-onnistunut",
  lahetysvirhe
FROM yllapitokohde
  LEFT JOIN paallystysilmoitus pi ON pi.paallystyskohde = yllapitokohde.id
                                     AND pi.poistettu IS NOT TRUE
WHERE urakka = :urakka
      AND sopimus = :sopimus
      AND yllapitokohdetyotyyppi = 'paallystys' :: yllapitokohdetyotyyppi
      AND yllapitokohde.poistettu IS NOT TRUE;

-- name: hae-urakan-paallystysilmoituksen-id-paallystyskohteella
SELECT id
FROM paallystysilmoitus
WHERE paallystyskohde = :paallystyskohde;

-- name: hae-urakan-paallystysilmoitus-paallystyskohteella
-- Hakee urakan päällystysilmoituksen päällystyskohteen id:llä
SELECT
  pi.id,
  pi.muutoshinta,
  tila,
  aloituspvm,
  valmispvm_kohde                 AS "valmispvm-kohde",
  valmispvm_paallystys            AS "valmispvm-paallystys",
  takuupvm,
  ypk.nimi                        AS kohdenimi,
  ypk.kohdenumero,
  ypk.sopimuksen_mukaiset_tyot    AS "sopimuksen-mukaiset-tyot",
  ypk.arvonvahennykset,
  ypk.bitumi_indeksi              AS "bitumi-indeksi",
  ypk.kaasuindeksi,
  ilmoitustiedot,
  paatos_tekninen_osa             AS "tekninen-osa_paatos",
  paatos_taloudellinen_osa        AS "taloudellinen-osa_paatos",
  perustelu_tekninen_osa          AS "tekninen-osa_perustelu",
  perustelu_taloudellinen_osa     AS "taloudellinen-osa_perustelu",
  kasittelyaika_tekninen_osa      AS "tekninen-osa_kasittelyaika",
  kasittelyaika_taloudellinen_osa AS "taloudellinen-osa_kasittelyaika",
  asiatarkastus_pvm               AS "asiatarkastus_tarkastusaika",
  asiatarkastus_tarkastaja        AS "asiatarkastus_tarkastaja",
  asiatarkastus_tekninen_osa      AS "asiatarkastus_tekninen-osa",
  asiatarkastus_taloudellinen_osa AS "asiatarkastus_taloudellinen-osa",
  asiatarkastus_lisatiedot        AS "asiatarkastus_lisatiedot",
  ypko.id                         AS kohdeosa_id,
  ypko.nimi                       AS kohdeosa_nimi,
  ypko.tunnus                     AS kohdeosa_tunnus,
  ypko.tr_numero                  AS "kohdeosa_tr-numero",
  ypko.tr_alkuosa                 AS "kohdeosa_tr-alkuosa",
  ypko.tr_alkuetaisyys            AS "kohdeosa_tr-alkuetaisyys",
  ypko.tr_loppuosa                AS "kohdeosa_tr-loppuosa",
  ypko.tr_loppuetaisyys           AS "kohdeosa_tr-loppuetaisyys",
  ypko.tr_ajorata                 AS "kohdeosa_tr-ajorata",
  ypko.tr_kaista                  AS "kohdeosa_tr-kaista",
  ypko.toimenpide                 AS "kohdeosa_toimenpide",
  ypk.tr_numero                       AS "tr-numero",
  ypk.tr_alkuosa                      AS "tr-alkuosa",
  ypk.tr_alkuetaisyys                 AS "tr-alkuetaisyys",
  ypk.tr_loppuosa                     AS "tr-loppuosa",
  ypk.tr_loppuetaisyys                AS "tr-loppuetaisyys"

FROM yllapitokohde ypk
  LEFT JOIN paallystysilmoitus pi ON pi.paallystyskohde = :paallystyskohde
                                     AND pi.poistettu IS NOT TRUE
  LEFT JOIN yllapitokohdeosa ypko ON ypko.yllapitokohde = :paallystyskohde
                                     AND ypko.poistettu IS NOT TRUE
WHERE ypk.id = :paallystyskohde
      AND ypk.poistettu IS NOT TRUE;

-- name: hae-paallystysilmoitus-paallystyskohteella
SELECT
  id,
  muutoshinta,
  tila,
  aloituspvm,
  valmispvm_kohde                 AS "valmispvm-kohde",
  valmispvm_paallystys            AS "valmispvm-paallystys",
  takuupvm,
  ilmoitustiedot,
  paatos_tekninen_osa             AS "tekninen-osa_paatos",
  paatos_taloudellinen_osa        AS "taloudellinen-osa_paatos",
  perustelu_tekninen_osa          AS "tekninen-osa_perustelu",
  perustelu_taloudellinen_osa     AS "taloudellinen-osa_perustelu",
  kasittelyaika_tekninen_osa      AS "tekninen-osa_kasittelyaika",
  kasittelyaika_taloudellinen_osa AS "taloudellinen-osa_kasittelyaika",
  asiatarkastus_pvm               AS "asiatarkastus_tarkastusaika",
  asiatarkastus_tarkastaja        AS "asiatarkastus_tarkastaja",
  asiatarkastus_tekninen_osa      AS "asiatarkastus_tekninen-osa",
  asiatarkastus_taloudellinen_osa AS "asiatarkastus_taloudellinen-osa",
  asiatarkastus_lisatiedot        AS "asiatarkastus_lisatiedot"
FROM paallystysilmoitus pi
WHERE paallystyskohde = :paallystyskohde;

-- name: paivita-paallystysilmoitus<!
-- Päivittää päällystysilmoituksen tiedot (ei käsittelyä tai asiatarkastusta, päivitetään erikseen)
UPDATE paallystysilmoitus
SET
  tila                 = :tila :: paallystystila,
  ilmoitustiedot       = :ilmoitustiedot :: JSONB,
  aloituspvm           = :aloituspvm,
  valmispvm_kohde      = :valmispvm_kohde,
  valmispvm_paallystys = :valmispvm_paallystys,
  takuupvm             = :takuupvm,
  muutoshinta          = :muutoshinta,
  muokattu             = NOW(),
  muokkaaja            = :muokkaaja,
  poistettu            = FALSE
WHERE paallystyskohde = :id
      AND paallystyskohde IN (SELECT id
                              FROM yllapitokohde
                              WHERE urakka = :urakka);

-- name: paivita-paallystysilmoituksen-kasittelytiedot<!
-- Päivittää päällystysilmoituksen käsittelytiedot
UPDATE paallystysilmoitus
SET
  paatos_tekninen_osa             = :paatos_tekninen_osa :: paallystysilmoituksen_paatostyyppi,
  paatos_taloudellinen_osa        = :paatos_taloudellinen_osa :: paallystysilmoituksen_paatostyyppi,
  perustelu_tekninen_osa          = :perustelu_tekninen_osa,
  perustelu_taloudellinen_osa     = :perustelu_taloudellinen_osa,
  kasittelyaika_tekninen_osa      = :kasittelyaika_tekninen_osa,
  kasittelyaika_taloudellinen_osa = :kasittelyaika_taloudellinen_osa,
  muokattu                        = NOW(),
  muokkaaja                       = :muokkaaja
  WHERE paallystyskohde = :id
        AND paallystyskohde IN (SELECT id
                                FROM yllapitokohde
                                WHERE urakka = :urakka);

-- name: paivita-paallystysilmoituksen-asiatarkastus<!
-- Päivittää päällystysilmoituksen asiatarkastuksen
UPDATE paallystysilmoitus
SET
  asiatarkastus_pvm               = :asiatarkastus_pvm,
  asiatarkastus_tarkastaja        = :asiatarkastus_tarkastaja,
  asiatarkastus_tekninen_osa      = :asiatarkastus_tekninen_osa,
  asiatarkastus_taloudellinen_osa = :asiatarkastus_taloudellinen_osa,
  asiatarkastus_lisatiedot        = :asiatarkastus_lisatiedot,
  muokattu                        = NOW(),
  muokkaaja                       = :muokkaaja
  WHERE paallystyskohde = :id
        AND paallystyskohde IN (SELECT id
                                FROM yllapitokohde
                                WHERE urakka = :urakka);

-- name: luo-paallystysilmoitus<!
-- Luo uuden päällystysilmoituksen
INSERT INTO paallystysilmoitus (paallystyskohde, tila, ilmoitustiedot, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, luotu, luoja, poistettu)
VALUES (:paallystyskohde,
  :tila :: paallystystila,
  :ilmoitustiedot :: JSONB,
  :aloituspvm,
  :valmispvm_kohde,
  :valmispvm_paallystys,
  :takuupvm,
  :muutoshinta,
  NOW(),
  :kayttaja, FALSE);

-- name: hae-paallystysilmoituksen-kommentit
-- Hakee annetun päällystysilmoituksen kaikki kommentit (joita ei ole poistettu) sekä
-- kommentin mahdollisen liitteen tiedot. Kommentteja on vaikea hakea
-- array aggregoimalla itse havainnon hakukyselyssä.
SELECT
  k.id,
  k.tekija,
  k.kommentti,
  k.luoja,
  k.luotu                              AS aika,
  CONCAT(ka.etunimi, ' ', ka.sukunimi) AS tekijanimi,
  l.id                                 AS liite_id,
  l.tyyppi                             AS liite_tyyppi,
  l.koko                               AS liite_koko,
  l.nimi                               AS liite_nimi,
  l.liite_oid                          AS liite_oid
FROM kommentti k
  JOIN kayttaja ka ON k.luoja = ka.id
  LEFT JOIN liite l ON l.id = k.liite
WHERE k.poistettu = FALSE
      AND k.id IN (SELECT pk.kommentti
                   FROM paallystysilmoitus_kommentti pk
                   WHERE pk.paallystysilmoitus = :id)
ORDER BY k.luotu ASC;

-- name: liita-kommentti<!
-- Liittää päällystysilmoitukseen uuden kommentin
INSERT INTO paallystysilmoitus_kommentti (paallystysilmoitus, kommentti) VALUES (:paallystysilmoitus, :kommentti);

-- name: onko-paallystysilmoitus-olemassa-kohteelle?
-- single?: true
SELECT exists(SELECT *
              FROM paallystysilmoitus
              WHERE paallystyskohde = :id);

-- name: paivita-paallystysilmoituksen-ilmoitustiedot<!
-- Päivittää päällystysilmoituksen ilmoitustiedot
UPDATE paallystysilmoitus
SET
  ilmoitustiedot       = :ilmoitustiedot :: JSONB,
  muokattu             = NOW(),
  muokkaaja            = :muokkaaja
WHERE paallystyskohde = :id;
