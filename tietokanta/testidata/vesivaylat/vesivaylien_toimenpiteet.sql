-- ***********************************************
-- VIKA ILMAN KORJAUSTA
-- ***********************************************

INSERT INTO vv_vikailmoitus
("reimari-id", kuvaus, pvm, "turvalaite-id")
VALUES
  ('1234', 'Akonniemen kyltti on lähtenyt irti myrskyn takia', '2017-04-02', (SELECT id
                                                                              FROM vv_turvalaite
                                                                              WHERE nimi = 'Akonniemen kyltti'));

-- ***********************************************
-- KOKONAISHINTAINEN TOIMENPIDE KIINTIÖSSÄ
-- ***********************************************

INSERT INTO vv_kiintio
("urakka-id", "sopimus-id", nimi, koko, luoja)
VALUES
  ((SELECT id
    FROM urakka
    WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
   (SELECT id
    FROM sopimus
    WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
   'Siirtyneiden poijujen siirto',
   30,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO vv_kiintio
("urakka-id", "sopimus-id", nimi, koko, luoja)
VALUES
  ((SELECT id
    FROM urakka
    WHERE nimi ILIKE 'Vantaan väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
   (SELECT id
    FROM sopimus
    WHERE nimi = 'Vantaan väyläyksikön pääsopimus'),
   'Joku kiintiö Vantaalla',
   30,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO vv_kiintio
("urakka-id", "sopimus-id", nimi, koko, luoja, poistettu)
VALUES
  ((SELECT id
    FROM urakka
    WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
   (SELECT id
    FROM sopimus
    WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
   'POISTETTU KIINTIÖ EI SAA NÄKYÄ',
   999999,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   TRUE);

-- Tyhjä kiintiö
INSERT INTO vv_kiintio
("urakka-id", "sopimus-id", nimi, koko, luoja)
VALUES
  ((SELECT id
    FROM urakka
    WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
   (SELECT id
    FROM sopimus
    WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
   'Aurinkopaneelien akkujen vaihto',
   5,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "kiintio-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('kokonaishintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    2,
    (SELECT id
     FROM vv_kiintio
     WHERE nimi = 'Siirtyneiden poijujen siirto'),
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus, NULL)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8881, Poiju 1, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren poiju'),
    'Kiintiöön kuuluva jutska',
    '2017-08-05T23:23Z',
    '2017-08-05',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-05',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
   (SELECT id
    FROM vv_vayla
    WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

-- ***********************************************
-- KOKONAISHINTAISET TOIMENPITEET ILMAN VIKAA
-- ***********************************************

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('kokonaishintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    12,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8882, Poiju 2, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren poiju'),
    NULL,
    '2017-08-05T23:23Z',
    '2017-08-05',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-05',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('kokonaishintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    22,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(12345, Poiju 3, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren poiju'),
    NULL,
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542049',
    '1022541802',
    '1022541903',
    '(123,Akonniemen väylät,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Akonniemen väylät'));

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "reimari-henkilo-lkm",
 "vayla-id")
VALUES
  ('kokonaishintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    32,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8884, Poiju 4, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren poiju'),
    NULL,
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542049',
    '1022541803',
    '1022541903',
    '(123,Akonniemen väylät,55)',
    30,
   (SELECT id
    FROM vv_vayla
    WHERE nimi = 'Akonniemen väylät'));

-- ***********************************************
-- KOKONAISHINTAISET TOIMENPITEET VIALLA
-- ***********************************************

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('kokonaishintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    42,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(1234, Poiju 6, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren viitta'),
    'TESTITOIMENPIDE 2',
    '2017-04-04T23:23Z',
    '2017-04-04',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-04-04',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

INSERT INTO liite (nimi, tyyppi, lahde) VALUES ('vv_liite.jpg', 'image/jpeg', 'harja-ui' :: LAHDE);
INSERT INTO liite (nimi, tyyppi, lahde) VALUES ('vv_liite2.jpg', 'image/jpeg', 'harja-ui' :: LAHDE);
INSERT INTO liite (nimi, tyyppi, lahde) VALUES ('POISTETTU LIITE EI SAA NÄKYÄ', 'image/jpeg', 'harja-ui' :: LAHDE);
INSERT INTO reimari_toimenpide_liite ("toimenpide-id", "liite-id") VALUES ((SELECT id
                                                                            FROM reimari_toimenpide
                                                                            WHERE lisatieto = 'TESTITOIMENPIDE 2'),
                                                                           (SELECT id
                                                                            FROM liite
                                                                            WHERE nimi = 'vv_liite.jpg'));
INSERT INTO reimari_toimenpide_liite ("toimenpide-id", "liite-id") VALUES ((SELECT id
                                                                            FROM reimari_toimenpide
                                                                            WHERE lisatieto = 'TESTITOIMENPIDE 2'),
                                                                           (SELECT id
                                                                            FROM liite
                                                                            WHERE nimi = 'vv_liite2.jpg'));
INSERT INTO reimari_toimenpide_liite ("toimenpide-id", "liite-id", poistettu) VALUES ((SELECT id
                                                                                       FROM reimari_toimenpide
                                                                                       WHERE lisatieto =
                                                                                             'TESTITOIMENPIDE 2'),
                                                                                      (SELECT id
                                                                                       FROM liite
                                                                                       WHERE nimi =
                                                                                             'POISTETTU LIITE EI SAA NÄKYÄ'),
                                                                                      TRUE);

INSERT INTO vv_vikailmoitus
("reimari-id", kuvaus, pvm, "turvalaite-id", "toimenpide-id")
VALUES
  ('123', 'Hietasaaren viitta on kaatunut', '2017-04-02', (SELECT id
                                                           FROM vv_turvalaite
                                                           WHERE nimi = 'Hietasaaren viitta'),
   (SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'TESTITOIMENPIDE 2'));

-- ***********************************************
-- KOKONAISHINTAISIIN SIIRRETYT, REIMARISTA YKSIKKÖHINTAISENA RAPORTOIDUT TYÖT
-- ***********************************************

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")

VALUES
  ('kokonaishintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    52,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8890, Poiju 10, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren poiju'),
    NULL,
    '2017-08-03T23:23Z',
    '2017-08-03',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-03',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

-- ***********************************************
-- YKSIKKÖHINTAISIIN SIIRRETYT TYÖT, ILMAN HINTAERITTELYÄ
-- ***********************************************

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('yksikkohintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    62,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8891, Poiju 11, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren poiju'),
    NULL,
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

-- ***********************************************
-- TODO: YKSIKKÖHINTAISIIN SIIRRETTY TYÖ, HINTAERITTELY MATERIAALEIHIN/KOMPONENTTEIHIN/TÖIHIN TEHTY
-- ***********************************************


-- ***********************************************
-- TODO: REIMARISTA YKSIKKÖHINTAISENA RAPORTOITU TYÖ
-- ***********************************************

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('yksikkohintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    82,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8881, Poiju 1, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Poijujen korjausta kuten on sovittu otos 2',
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

-- ***********************************************
-- REIMARISTA YKSIKKÖHINTAISENA RAPORTOITU, KÖNTTÄSUMMALLA
-- SEKÄ TARKENNETULLA HINNALLA HINNOITELTU TOIMENPIDE
-- ***********************************************

INSERT INTO reimari_toimenpide
(hintatyyppi,
 "urakka-id",
 "reimari-id",
 "reimari-urakoitsija",
 "urakoitsija-id",
 "reimari-sopimus",
 "sopimus-id",
 "reimari-turvalaite",
 "turvalaite-id",
 lisatieto,
 suoritettu,
 luotu,
 luoja,
 "reimari-luotu",
 "reimari-alus",
 "reimari-tila",
 "reimari-toimenpidetyyppi",
 "reimari-tyolaji",
 "reimari-tyoluokka",
 "reimari-vayla",
 "vayla-id")
VALUES
  ('yksikkohintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    72,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8881, Poiju 1, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Poijujen korjausta kuten on sovittu',
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä')),
  ('yksikkohintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    73,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(8881, Poiju 1, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Lisää poijujen korjausta',
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä')),
  ('yksikkohintainen',
    (SELECT id
     FROM urakka
     WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    74,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus,)',
    (SELECT id
     FROM sopimus
     WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(666, Poiju 666, 555)',
    (SELECT id
     FROM vv_turvalaite
     WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Oulaisten poijujen korjaus',
    '2017-08-08T23:23Z',
    '2017-08-08',
    (SELECT id
     FROM kayttaja
     WHERE kayttajanimi = 'tero'),
    '2017-08-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123,Hietasaaren läntinen rinnakkaisväylä,55)',
    (SELECT id
     FROM vv_vayla
     WHERE nimi = 'Oulaisten meriväylä'));

-- Hintaryhmät

INSERT INTO vv_hinnoittelu
(nimi, hintaryhma, luoja, "urakka-id")
VALUES
  ('Hietasaaren poijujen korjaus', TRUE,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT "urakka-id"
    FROM reimari_toimenpide
    WHERE lisatieto = 'Poijujen korjausta kuten on sovittu'));

INSERT INTO vv_hinnoittelu
(nimi, hintaryhma, luoja, "urakka-id")
VALUES
  ('Oulaisten meriväylän kunnostus', TRUE,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT "urakka-id"
    FROM reimari_toimenpide
    WHERE lisatieto = 'Oulaisten poijujen korjaus'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Hietasaaren poijujen korjaus'),
   'Ryhmähinta', 60000, (SELECT id
                         FROM kayttaja
                         WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Oulaisten meriväylän kunnostus'),
   'Ryhmähinta', 30, (SELECT id
                      FROM kayttaja
                      WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id", luoja)
VALUES
  ((SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'Poijujen korjausta kuten on sovittu'),
   (SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Hietasaaren poijujen korjaus'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id", luoja)
VALUES
  ((SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'Lisää poijujen korjausta'),
   (SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Hietasaaren poijujen korjaus'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id", luoja)
VALUES
  ((SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'Oulaisten poijujen korjaus'),
   (SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Oulaisten meriväylän kunnostus'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

-- Toimenpiteen hintatiedot

INSERT INTO vv_hinnoittelu
(nimi, hintaryhma, luoja, "urakka-id")
VALUES
  ('Toimenpiteen oma hinnoittelu', FALSE,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT "urakka-id"
    FROM reimari_toimenpide
    WHERE lisatieto = 'Poijujen korjausta kuten on sovittu'));

INSERT INTO vv_hinnoittelu
(nimi, hintaryhma, luoja, "urakka-id", poistettu)
VALUES
  ('POISTETTU HINNOITTELU EI SAISI NÄKYÄ MISSÄÄN', FALSE,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT "urakka-id"
    FROM reimari_toimenpide
    WHERE lisatieto = 'TESTITOIMENPIDE 2'), TRUE);

INSERT INTO vv_hinnoittelu
(nimi, hintaryhma, luoja, "urakka-id")
VALUES
  ('Vanhaan urakan testihinnoittelu', TRUE,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT id
    FROM urakka
    WHERE nimi = 'Vantaan väyläyksikön väylänhoito ja -käyttö, Itäinen SL'));

INSERT INTO vv_hinnoittelu
(nimi, hintaryhma, luoja, "urakka-id")
VALUES
  ('Hietasaaren poijujen korjausta otos 2', FALSE,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT id
    FROM urakka
    WHERE nimi = 'Vantaan väyläyksikön väylänhoito ja -käyttö, Itäinen SL'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Toimenpiteen oma hinnoittelu'),
   'Työ', 600, (SELECT id
                FROM kayttaja
                WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja, poistettu)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Toimenpiteen oma hinnoittelu'),
   'POISTETTU HINTA EI SAA NÄKYÄ', 99999999, (SELECT id
                                              FROM kayttaja
                                              WHERE kayttajanimi = 'tero'), TRUE);

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja, poistettu)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Hietasaaren poijujen korjaus'),
   'HINTARYHMÄÄN LIITTYVÄ POISTETTU HINTA EI SAA NÄKYÄ', 99999999, (SELECT id
                                                                    FROM kayttaja
                                                                    WHERE kayttajanimi = 'tero'), TRUE);

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja, poistettu)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'POISTETTU HINNOITTELU EI SAISI NÄKYÄ MISSÄÄN'),
   'POISTETTUUN HINNOITTELUUN KUULUVA HINTA JOKA EI OLE POISTETTU EI SAA NÄKYÄ MISSÄÄN', 99999999, (SELECT id
                                                                                                    FROM kayttaja
                                                                                                    WHERE kayttajanimi =
                                                                                                          'tero'),
   TRUE);

INSERT INTO vv_hinta
("hinnoittelu-id", summa, luoja, toimenpidekoodi)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Toimenpiteen oma hinnoittelu'),
   30,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT id
    FROM toimenpidekoodi
   WHERE nimi = 'Henkilöstö: Ammattimies'));

INSERT INTO vv_hinta
("hinnoittelu-id", summa, luoja, toimenpidekoodi)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Toimenpiteen oma hinnoittelu'),
   15,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'),
   (SELECT id
    FROM toimenpidekoodi
    WHERE nimi = 'Henkilöstö: Työnjohto'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Vanhaan urakan testihinnoittelu'),
   'Työ', 5, (SELECT id
              FROM kayttaja
              WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, summa, luoja)
VALUES
  ((SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Hietasaaren poijujen korjausta otos 2'),
   'Työ', 70, (SELECT id
               FROM kayttaja
               WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id", luoja)
VALUES
  ((SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'Poijujen korjausta kuten on sovittu'),
   (SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Toimenpiteen oma hinnoittelu'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id", luoja)
VALUES
  ((SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'TESTITOIMENPIDE 2'),
   (SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'POISTETTU HINNOITTELU EI SAISI NÄKYÄ MISSÄÄN'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id", luoja)
VALUES
  ((SELECT id
    FROM reimari_toimenpide
    WHERE lisatieto = 'Poijujen korjausta kuten on sovittu otos 2'),
   (SELECT id
    FROM vv_hinnoittelu
    WHERE nimi = 'Hietasaaren poijujen korjausta otos 2'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

-- ***********************************************
-- TODO: ERIKSEEN TILATTU YKSIKKÖHINTAINEN TYÖ?
-- ***********************************************
