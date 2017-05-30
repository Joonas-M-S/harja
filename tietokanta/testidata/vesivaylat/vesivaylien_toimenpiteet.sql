-- ***********************************************
-- VIKA ILMAN KORJAUSTA
-- ***********************************************

INSERT INTO vv_vikailmoitus
("reimari-id", kuvaus, pvm, "turvalaite-id")
VALUES
  ('1234', 'Akonniemen kyltti on lähtenyt irti myrskyn takia', '2017-04-02', (SELECT id FROM vv_turvalaite WHERE nimi = 'Akonniemen kyltti'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    12,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren poiju'),
    '',
    FALSE,
    '2017-05-05T23:23Z',
    '2017-05-05',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-05',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    22,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren poiju'),
    '',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542049',
    '1022541802',
    '1022541903',
    '(123, Akonniemen väylät, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Akonniemen väylät'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    32,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren poiju'),
    '',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542049',
    '1022541803',
    '1022541903',
    '(123, Akonniemen väylät, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Akonniemen väylät'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    42,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren viitta, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren viitta'),
    'TESTITOIMENPIDE 2',
    FALSE,
    '2017-04-04T23:23Z',
    '2017-04-04',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-04-04',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

INSERT INTO vv_vikailmoitus
("reimari-id", kuvaus, pvm, "turvalaite-id", "toimenpide-id")
VALUES
  ('123', 'Hietasaaren viitta on kaatunut', '2017-04-02', (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren viitta'),
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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    52,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren poiju'),
    '',
    TRUE,
    '2017-05-03T23:23Z',
    '2017-05-03',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-03',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    62,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren poiju'),
    '',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));


-- ***********************************************
-- TODO: YKSIKKÖHINTAISIIN SIIRRETTY TYÖ, HINTAERITTELY MATERIAALEIHIN/KOMPONENTTEIHIN/TÖIHIN TEHTY
-- ***********************************************


-- ***********************************************
-- TODO: REIMARISTA YKSIKKÖHINTAISENA RAPORTOITU TYÖ
-- ***********************************************

-- ***********************************************
-- TODO: REIMARISTA YKSIKKÖHINTAISENA RAPORTOITU, KÖNTTÄSUMMALLA HINNOITELTU TOIMENPIDE

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    72,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren pienempi poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Poijujen korjausta kuten on sovittu',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    72,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren pienempi poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Poijujen korjausta taas',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Hietasaaren läntinen rinnakkaisväylä'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    72,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren pienempi poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Poijujen korjausta jossain ihan muualla',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Muu väylä'));

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
 lisatyo,
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
    (SELECT id FROM urakka WHERE nimi ILIKE 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'),
    72,
    '(23, Pohjanmeren venepojat)',
    (SELECT id
     FROM organisaatio
     WHERE nimi = 'Pohjanmeren venepojat'),
    '(-5, 1022542301, Helsingin väyläyksikön pääsopimus)',
    (SELECT id FROM sopimus WHERE nimi = 'Helsingin väyläyksikön pääsopimus'),
    '(62, Hietasaaren pienempi poiju, 555)',
    (SELECT id FROM vv_turvalaite WHERE nimi = 'Hietasaaren pienempi poiju'),
    'Yksittäisen poljun korjaus',
    FALSE,
    '2017-05-08T23:23Z',
    '2017-05-08',
    (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'),
    '2017-05-08',
    '(MBKE24524, MS Piggy)',
    '1022541202',
    '1022542001',
    '1022541802',
    '1022541905',
    '(123, Hietasaaren läntinen rinnakkaisväylä, 55)',
   (SELECT id FROM vv_vayla WHERE nimi = 'Muu väylä'));

INSERT INTO vv_hinnoittelu
(nimi, "urakka-id", hintaryhma, luoja)
VALUES
  ('Hietasaaren poijujen korjaus hintaryhmä', (SELECT id FROM urakka WHERE nimi = 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), true, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu
(nimi, "urakka-id", hintaryhma, luoja)
VALUES
  ('Hietasaaren poijujen korjaus', (SELECT id FROM urakka WHERE nimi = 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), false, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu
(nimi, "urakka-id", hintaryhma, luoja)
VALUES
  ('Muun väylän korjaus hintaryhmä', (SELECT id FROM urakka WHERE nimi = 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), true, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu
(nimi, "urakka-id", hintaryhma, luoja)
VALUES
  ('Muun väylän korjaus', (SELECT id FROM urakka WHERE nimi = 'Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), false, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, maara, luoja)
VALUES
  ((SELECT id FROM vv_hinnoittelu WHERE nimi = 'Hietasaaren poijujen korjaus hintaryhmä'),
   'Tilaus', 300, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, maara, luoja)
VALUES
  ((SELECT id FROM vv_hinnoittelu WHERE nimi = 'Hietasaaren poijujen korjaus'),
   'Tilaus', 60000, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, maara, luoja)
VALUES
  ((SELECT id FROM vv_hinnoittelu WHERE nimi = 'Muun väylän korjaus hintaryhmä'),
   'Tilaus', 100, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinta
("hinnoittelu-id", otsikko, maara, luoja)
VALUES
  ((SELECT id FROM vv_hinnoittelu WHERE nimi = 'Muun väylän korjaus'),
   'Tilaus', 200, (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id")
VALUES
  ((SELECT id FROM reimari_toimenpide WHERE lisatieto = 'Poijujen korjausta kuten on sovittu'),
   (SELECT id FROM vv_hinnoittelu WHERE nimi = 'Hietasaaren poijujen korjaus'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id")
VALUES
  ((SELECT id FROM reimari_toimenpide WHERE lisatieto = 'Poijujen korjausta kuten on sovittu'),
   (SELECT id FROM vv_hinnoittelu WHERE nimi = 'Hietasaaren poijujen korjaus hintaryhmä'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id")
VALUES
  ((SELECT id FROM reimari_toimenpide WHERE lisatieto = 'Poijujen korjausta taas'),
   (SELECT id FROM vv_hinnoittelu WHERE nimi = 'Hietasaaren poijujen korjaus hintaryhmä'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id")
VALUES
  ((SELECT id FROM reimari_toimenpide WHERE lisatieto = 'Poijujen korjausta jossain ihan muualla'),
   (SELECT id FROM vv_hinnoittelu WHERE nimi = 'Hietasaaren poijujen korjaus hintaryhmä'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id")
VALUES
  ((SELECT id FROM reimari_toimenpide WHERE lisatieto = 'Yksittäisen poljun korjaus'),
   (SELECT id FROM vv_hinnoittelu WHERE nimi = 'Muun väylän korjaus hintaryhmä'));

INSERT INTO vv_hinnoittelu_toimenpide
("toimenpide-id", "hinnoittelu-id")
VALUES
  ((SELECT id FROM reimari_toimenpide WHERE lisatieto = 'Yksittäisen poljun korjaus'),
   (SELECT id FROM vv_hinnoittelu WHERE nimi = 'Muun väylän korjaus'));
-- ***********************************************


-- ***********************************************
-- TODO: ERIKSEEN TILATTU YKSIKKÖHINTAINEN TYÖ?
-- ***********************************************