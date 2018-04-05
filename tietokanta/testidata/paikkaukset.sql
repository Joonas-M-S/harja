INSERT INTO paikkauskohde ("luoja-id",
                           "ulkoinen-id",
                           nimi)
VALUES ((SELECT id
         FROM kayttaja
         WHERE kayttajanimi = 'destia'
         LIMIT 1),
        666,
        'Testikohde'),
        ((SELECT id
         FROM kayttaja
         WHERE kayttajanimi = 'destia'
         LIMIT 1),
        1337,
        'Testikohde 2'),
        ((SELECT id
         FROM kayttaja
         WHERE kayttajanimi = 'destia'
         LIMIT 1),
        221337,
        '22 testikohteet'),
        ((SELECT id
         FROM kayttaja
         WHERE kayttajanimi = 'skanska'
         LIMIT 1),
        7331,
        'Testikohde Muhoksen paallystysurakassa');

DO $$ DECLARE
  destia_kayttaja INTEGER := (SELECT id FROM kayttaja WHERE kayttajanimi = 'destia' LIMIT 1);
  skanska_kayttaja INTEGER := (SELECT id FROM kayttaja WHERE kayttajanimi = 'skanska' LIMIT 1);
  oulun_alueurakan_id INTEGER := (SELECT id FROM urakka WHERE sampoid='1242141-OULU2');
  muhoksen_paallystysurakan_id INTEGER := (SELECT id FROM urakka WHERE sampoid='4242523-TES2');
  hoito_paikkauskohde_id INTEGER := (SELECT id FROM paikkauskohde WHERE "ulkoinen-id"=666 LIMIT 1);
  hoito_paikkauskohde_2_id INTEGER := (SELECT id FROM paikkauskohde WHERE "ulkoinen-id"=1337 LIMIT 1);
  hoito_paikkauskohde_22_id INTEGER := (SELECT id FROM paikkauskohde WHERE "ulkoinen-id"=221337 LIMIT 1);
  paallystys_paikkauskohde_id INTEGER := (SELECT id FROM paikkauskohde WHERE "ulkoinen-id"=7331 LIMIT 1);
BEGIN
INSERT INTO paikkaus("luoja-id", luotu, "muokkaaja-id", muokattu, "poistaja-id", poistettu, "urakka-id", "paikkauskohde-id",
                     "ulkoinen-id", alkuaika, loppuaika, tierekisteriosoite, tyomenetelma, massatyyppi, leveys, massamenekki,
                     raekoko, kuulamylly)
  VALUES -- 20 tien paikkaukset
          (destia_kayttaja, NOW(), NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_id,
          6661, NOW() + interval '1 day', NOW() + interval '10 day', ROW (20, 1, 1, 1, 100, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.3, 2, 1, '2'),

          (destia_kayttaja, NOW() + time '00:01:00', NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_id,
          6662, NOW() + interval '5 day', NOW() + interval '15 day', ROW (20, 1, 50, 1, 150, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.4, 3, 1, '2'),

          (destia_kayttaja, NOW() + time '00:02:00', NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_id,
          6663, NOW() + interval '10 day', NOW() + interval '20 day', ROW (20, 3, 1, 3, 200, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.2, 4, 1, '2'),

          (destia_kayttaja, NOW() + time '00:03:00', NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_2_id,
          133, NOW() + interval '10 day', NOW() + interval '20 day', ROW (20, 3, 200, 3, 300, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.2, 4, 1, '2'),

          (skanska_kayttaja, NOW(), NULL, NULL, NULL, FALSE, muhoksen_paallystysurakan_id, paallystys_paikkauskohde_id,
          733, NOW(), NOW() + interval '20 day', ROW (20, 19, 1, 19, 50, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.2, 4, 1, '2'),
          -- 22 tien paikkaukset
          (destia_kayttaja, NOW(), NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_22_id,
          221, NOW() - interval '1 day', NOW() + interval '9 day', ROW (22, 3, 1, 3, 100, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.3, 2, 1, '2'),
          (destia_kayttaja, NOW(), NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_22_id,
          222, NOW() - interval '1 day', NOW() + interval '9 day', ROW (22, 3, 200, 3, 300, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.3, 2, 1, '2'),
          (destia_kayttaja, NOW(), NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_22_id,
          223, NOW() - interval '1 day', NOW() + interval '9 day', ROW (22, 3, 400, 3, 450, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.3, 2, 1, '2'),
          (destia_kayttaja, NOW(), NULL, NULL, NULL, FALSE, oulun_alueurakan_id, hoito_paikkauskohde_22_id,
          224, NOW() - interval '1 day', NOW() + interval '9 day', ROW (22, 4, 1, 5, 1, NULL) :: TR_OSOITE,
          'massapintaus', 'asfalttibetoni', 1.3, 2, 1, '2');

 INSERT INTO paikkauksen_materiaali ("paikkaus-id", esiintyma, "kuulamylly-arvo", muotoarvo, sideainetyyppi, pitoisuus,
                                     "lisa-aineet")
 VALUES -- 20 tien paikkaukset
        ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6661 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.2,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6661 LIMIT 1), 'Testikivi', '1', 'Muotoarvo2', 'Sideaine2', 3.2,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6662 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.3,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6663 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.1,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 133 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.1,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 733 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.1,
         'Lisäaineet'),
         -- 22 tien paikkaukset
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 221 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.2,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 222 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.2,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 223 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.2,
         'Lisäaineet'),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 224 LIMIT 1), 'Testikivi', '1', 'Muotoarvo', 'Sideaine', 3.2,
         'Lisäaineet');

 INSERT INTO paikkauksen_tienkohta ("paikkaus-id", ajorata, reunat, ajourat, ajouravalit, keskisaumat)
 VALUES  -- 20 tien paikkaukset
        ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6661 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], NULL),
        ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6662 LIMIT 1), 2, ARRAY [0], ARRAY [1, 2], ARRAY [1], NULL),
        ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 6663 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], ARRAY [1]),
        ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 133 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], ARRAY [1]),
        ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 733 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], ARRAY [1]),
         -- 22 tien paikkaukset
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 221 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], NULL),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 222 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], NULL),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 223 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], NULL),
         ((SELECT id FROM paikkaus WHERE "ulkoinen-id" = 224 LIMIT 1), 1, ARRAY [0], ARRAY [1, 2], ARRAY [1], NULL);
END $$;

INSERT INTO paikkaustoteuma ("ulkoinen-id",
                             "urakka-id",
                             "paikkauskohde-id",
                             "toteuma-id",
                             "luoja-id",
                             tyyppi,
                             selite,
                             hinta)
VALUES
  (6661,
   (SELECT id
    FROM urakka
    WHERE sampoid = '1242141-OULU2'
    LIMIT 1),
   (SELECT id
    FROM paikkauskohde
    WHERE "ulkoinen-id" = 666
    LIMIT 1),
   NULL,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'destia'
    LIMIT 1),
   'kokonaishintainen',
   'Liikennejärjestelyt',
   3500);

INSERT INTO paikkaustoteuma ("ulkoinen-id",
                             "urakka-id",
                             "paikkauskohde-id",
                             "toteuma-id",
                             "luoja-id",
                             tyyppi,
                             selite,
                             yksikko,
                             yksikkohinta,
                             maara)
VALUES
  (6662,
   (SELECT id
    FROM urakka
    WHERE sampoid = '1242141-OULU2'
    LIMIT 1),
   (SELECT id
    FROM paikkauskohde
    WHERE "ulkoinen-id" = 666
    LIMIT 1),
   NULL,
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'destia'
    LIMIT 1),
   'yksikkohintainen',
   'Asfaltti',
   'tonnia/€',
   200,
   13.2)

