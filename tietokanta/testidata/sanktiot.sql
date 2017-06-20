-----------------------------------------
-- Oulun alueurakka 2014-2019
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 1', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 1000, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 1'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 2', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 666.666, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 2'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Liikenneympäristön hoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 666', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 100, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 666'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 667', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 10, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 667'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Muu tuote

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 4', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 1, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 4'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-- Ryhmä C

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 5', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('C'::sanktiolaji, 123, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 5'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Määräpäivän ylitys'), false, 2);

-- Muistutus Talvihoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 6', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('muistutus'::sanktiolaji, null, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 6'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Muistutus Muu

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 7', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('muistutus'::sanktiolaji, null, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 7'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Liikenneympäristön hoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-----------------------------------------
-- Pudasjärven alueurakka 2007-2012
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 100', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 10000, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 100'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 200', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 6660, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 200'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Liikenneympäristön hoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 66600', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 1000, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 66600'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Liikenneympäristön hoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 66700', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 100, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 66700'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Liikenneympäristön hoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Liikenneympäristön hoito'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Muu tuote

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 400', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 10, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 400'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Liikenneympäristön hoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-- Ryhmä C

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 500', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('C'::sanktiolaji, 1230, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 500'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Liikenneympäristön hoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Määräpäivän ylitys'), false, 2);

-- Muistutus Talvihoito

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 600', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('muistutus'::sanktiolaji, null, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 600'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Talvihoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Muistutus Muu

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2011-10-11 06:06.37', '2011-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'Sanktion sisältävä laatupoikkeama 700', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('muistutus'::sanktiolaji, null, '2011-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 700'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Pudasjärvi Liikenneympäristön hoito TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Muu tuote'), false, 2);

-----------------------------------------
-- Vantaan alueurakka 2009-2019
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Vantaan alueurakka 2009-2019'), 'Sanktion sisältävä laatupoikkeama 999', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 2.5, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 999'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Vantaa Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Vantaan alueurakka 2009-2019'), 'Sanktion sisältävä laatupoikkeama 9990', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 2, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 9990'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Vantaa Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-----------------------------------------
-- Espoon alueurakka 2014-2019
-----------------------------------------

-- Sakkoryhmä A, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Espoon alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 6767', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 1, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 6767'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Espoo Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-- Sakkoryhmä B, Sanktiotyyppi Talvihoito, päätiet

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Espoon alueurakka 2014-2019'), 'Sanktion sisältävä laatupoikkeama 3424', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('B'::sanktiolaji, 1.5, '2016-10-12 06:06.37', 'MAKU 2010', (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Sanktion sisältävä laatupoikkeama 3424'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Espoo Talvihoito TP 2014-2019'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Talvihoito, päätiet (talvihoitoluokat Is ja I)'), false, 2);

-----------------------------------------
-- Ylläpito
-----------------------------------------

-- Ylläpitourakkaan kohteeseen liitetty sanktio
INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, (SELECT id FROM yllapitokohde WHERE nimi = 'Oulaisten ohitusramppi'), 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-2 06:06.37', '2017-01-04 16:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'Ylläpitokohteeseen linkattu suorasanktio');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_sakko'::sanktiolaji, 1500, '2017-01-4 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseen linkattu suorasanktio'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Muhos Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon sakko'), true, 2);

INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, (SELECT id FROM yllapitokohde WHERE nimi = 'Oulaisten ohitusramppi'), 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-3 12:06.37', '2017-01-05 13:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'Ylläpitokohteeseen linkattu suorasanktio 2');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_sakko'::sanktiolaji, 1500, '2017-01-5 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseen linkattu suorasanktio 2'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Muhos Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon sakko'), true, 2);

INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, (SELECT id FROM yllapitokohde WHERE nimi = 'Leppäjärven ramppi'), 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-3 12:06.37', '2017-01-05 13:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'Ylläpitokohteeseen linkattu suorasanktio 3');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_bonus'::sanktiolaji, -2000, '2017-01-6 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseen linkattu suorasanktio 3'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Muhos Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon bonus'), true, 2);

INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, (SELECT id FROM yllapitokohde WHERE nimi = 'Nakkilan ramppi'), 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-12 16:06.37', '2017-01-14 16:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'Ylläpitokohteeseen linkattu suorasanktio muistutus 1');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_muistutus'::sanktiolaji, null, '2017-01-15 03:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseen linkattu suorasanktio muistutus 1'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Muhos Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon muistutus'), true, 2);

INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, (SELECT id FROM yllapitokohde WHERE nimi = 'Oulaisten ohitusramppi'), 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-12 16:06.37', '2017-01-14 16:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'Ylläpitokohteeseen linkattu suorasanktio muistutus 2');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_muistutus'::sanktiolaji, null, '2017-01-16 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseen linkattu suorasanktio muistutus 2'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Muhos Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon muistutus'), true, 2);

-- Ylläpitourakan sanktioita ilman ylläpitokohdetta
INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, null, 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-2 06:06.37', '2017-01-04 16:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'Ylläpitokohteeton suorasanktio');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_sakko'::sanktiolaji, 1500, '2017-01-4 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeton suorasanktio'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Muhos Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon sakko'), true, 2);


INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, null, 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-3 12:06.37', '2017-01-05 13:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Porintien päällystysurakka'), 'Ylläpitokohteeseeton suorasanktio 4');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_sakko'::sanktiolaji, 1500, '2017-01-5 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseeton suorasanktio 4'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Porintien Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon sakko'), true, 2);

INSERT INTO laatupoikkeama (lahde, yllapitokohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, null, 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2017-01-3 12:06.37', '2017-01-05 13:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Porintien päällystysurakka'), 'Ylläpitokohteeseeton suorasanktio 5');
INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, laatupoikkeama, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('yllapidon_sakko'::sanktiolaji, 1500, '2017-01-5 06:06.37', null, (SELECT id FROM laatupoikkeama WHERE kuvaus = 'Ylläpitokohteeseeton suorasanktio 5'), (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Porintien Ajoradan päällyste TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Ylläpidon sakko'), true, 2);

-----------------------------------------
-- Vesiväylät
-----------------------------------------

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2017-06-20 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), 'Virtuaalinen vesiväylien laatupoikkeama 1, koska tietomalli');
INSERT INTO sanktio (laatupoikkeama, sakkoryhma, maara, perintapvm, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ((SELECT id FROM laatupoikkeama WHERE kuvaus = 'Virtuaalinen vesiväylien laatupoikkeama 1, koska tietomalli'), 'vesivayla_sakko'::sanktiolaji, 30, '2017-6-20 06:06.37', (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Väylänhoito, Itäinen Suomenlahti, sopimuksen kok.hintaiset työt, rann kmrk, TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Vesiväylän sakko'), true, 2);

INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, >, urakka, kuvaus) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2017-06-20 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), 'Virtuaalinen vesiväylien laatupoikkeama 2, koska tietomalli');
INSERT INTO sanktio (laatupoikkeama, sakkoryhma, maara, perintapvm, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ((SELECT id FROM laatupoikkeama WHERE kuvaus = 'Virtuaalinen vesiväylien laatupoikkeama 2, koska tietomalli'), 'vesivayla_sakko'::sanktiolaji, 30, '2017-6-20 01:06.37', (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Väylänhoito, Itäinen Suomenlahti, sopimuksen kok.hintaiset työt, rann kmrk, TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Vesiväylän sakko'), true, 2);
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2017-06-20 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), 'Virtuaalinen vesiväylien laatupoikkeama 3, koska tietomalli');
INSERT INTO sanktio (laatupoikkeama, sakkoryhma, maara, perintapvm, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ((SELECT id FROM laatupoikkeama WHERE kuvaus = 'Virtuaalinen vesiväylien laatupoikkeama 3, koska tietomalli'), 'vesivayla_sakko'::sanktiolaji, 666, '2017-6-20 06:06.37', (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Väylänhoito, Itäinen Suomenlahti, sopimuksen kok.hintaiset työt, rann kmrk, TP'), (SELECT id FROM sanktiotyyppi WHERE nimi = 'Vesiväylän sakko'), true, 2);


