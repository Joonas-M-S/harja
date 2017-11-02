-- Kanavien ja kohteiden testidata

INSERT INTO kan_kohde (nimi, tyyppi, "kanava-id", luoja)
VALUES
  (NULL, 'sulku' :: KOHTEEN_TYYPPI, (SELECT id
                                     FROM kan_kanava
                                     WHERE NIMI = 'Keiteleen kanava: Kapeenkosken kanava'), (SELECT id
                                                                                             FROM kayttaja
                                                                                             WHERE
                                                                                               kayttajanimi = 'tero')),
  (NULL, 'sulku' :: KOHTEEN_TYYPPI, (SELECT id
                                     FROM kan_kanava
                                     WHERE NIMI = 'Keiteleen kanava: Kuhankosken kanava'), (SELECT id
                                                                                            FROM kayttaja
                                                                                            WHERE
                                                                                              kayttajanimi = 'tero')),
  ('Tikkalansaaren avattava ratasilta', 'silta' :: KOHTEEN_TYYPPI, (SELECT id
                                                                    FROM kan_kanava
                                                                    WHERE NIMI = 'Taipaleen kanava'), (SELECT id
                                                                                                       FROM kayttaja
                                                                                                       WHERE
                                                                                                         kayttajanimi =
                                                                                                         'tero')),
  ('Taipaleen sulku', 'sulku' :: KOHTEEN_TYYPPI, (SELECT id
                                                  FROM kan_kanava
                                                  WHERE NIMI = 'Taipaleen kanava'), (SELECT id
                                                                                     FROM kayttaja
                                                                                     WHERE kayttajanimi = 'tero')),
  (NULL, 'sulku-ja-silta' :: KOHTEEN_TYYPPI, (SELECT id
                                              FROM kan_kanava
                                              WHERE NIMI = 'Nerkoon kanava'), (SELECT id
                                                                               FROM kayttaja
                                                                               WHERE kayttajanimi = 'tero'));

INSERT INTO kan_kohde_urakka ("kohde-id", "urakka-id", luoja)
VALUES
  ((SELECT id
    FROM kan_kohde
    WHERE nimi = 'Tikkalansaaren avattava ratasilta'),
   (SELECT id
    FROM urakka
    WHERE nimi = 'Saimaan kanava'),
   (SELECT id
    FROM kayttaja
    WHERE kayttajanimi = 'tero'));

INSERT INTO kan_toimenpide
(tyyppi,
 pvm,
 kohde,
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja)
VALUES ('kokonaishintainen' :: KAN_TOIMENPIDETYYPPI,
  now(),
  (SELECT id
   FROM kan_kohde
   WHERE nimi = 'Tikkalansaaren avattava ratasilta'),
  (SELECT id
   FROM kan_huoltokohde
   WHERE nimi = 'ASENNONMITTAUSLAITTEET'),
  (SELECT id
   FROM toimenpidekoodi
   WHERE emo = (SELECT id
                FROM toimenpidekoodi
                WHERE koodi = '24104') AND
         nimi = 'Ei yksilöity'),
  'Testitoimenpide',
  (SELECT id
   FROM kayttaja
   WHERE kayttajanimi = 'jvh'),
  (SELECT id
   FROM kayttaja
   WHERE kayttajanimi = 'jvh'),
  now(),
  (SELECT id
   FROM kayttaja
   WHERE kayttajanimi = 'jvh'),
  now(),
        (SELECT id
         FROM kayttaja
         WHERE kayttajanimi = 'jvh'),
        NULL,
        NULL);

INSERT INTO toimenpideinstanssi (urakka,
                                 toimenpide,
                                 nimi,
                                 alkupvm,
                                 loppupvm)
VALUES (31,
        597,
        'Testitoimenpideinstanssi',
        '2017-01-01',
        '2090-01-01');