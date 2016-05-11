-- Oulun alueurakka 2005-2012
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2005-10-11 06:06.37', '2005-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), 'Testihavainto 1', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Tämä ei ollut asiallinen havainto', 123, 1, NOW(), '2005-10-12 06:06.37', '2005-10-12 06:06.37', true, true, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), 'Testihavainto 2', 1, 2, 3, 4, point(418437, 7204744)::GEOMETRY, 5);

-- Oulun alueurakka 2014-2019
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2015-10-11 06:06.37', '2015-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Testihavainto 3', 1, 2, 3, 4, point(418237, 7207744)::GEOMETRY, 5);
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'tilaaja'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Tämä ei ollut asiallinen havainto', 123, 1, NOW(), '2015-10-12 06:06.37', '2015-10-12 06:06.37', true, true, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Testihavainto 4', 1, 2, 3, 4, point(418437, 7204744)::GEOMETRY, 5);
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'urakoitsija'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'hylatty'::laatupoikkeaman_paatostyyppi, 'Tämä ei ollut nyt asiallinen havainto', 123, 1, NOW(), '2015-10-14 06:06.37', '2015-10-15 06:06.37', true, true, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Testihavainto 5', 1, 2, 3, 4, point(418417, 7204444)::GEOMETRY, 5);
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'urakoitsija'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'ei_sanktiota'::laatupoikkeaman_paatostyyppi, 'Yksityinen aita', 123, 1, NOW(), '2015-10-15 06:06.37', '2015-10-16 06:06.37', true, true, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Aidassa reikä', 1, 2, 3, 4, point(418617, 7204344)::GEOMETRY, 5);
INSERT INTO laatupoikkeama (lahde, kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES ('harja-ui'::lahde, 'Testikohde', 'urakoitsija'::osapuoli, 'puhelin'::laatupoikkeaman_kasittelytapa, '', 'sanktio'::laatupoikkeaman_paatostyyppi, 'Ei saisi jättää näitä tälleen', 123, 1, NOW(), '2015-10-17 06:06.37', '2015-10-18 06:06.37', true, true, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Palava autonromu jätetty keskelle tietä!!', 1, 2, 3, 4, point(418817, 7204144)::GEOMETRY, 5);
INSERT INTO laatupoikkeama
(lahde, kohde, tekija, tarkastuspiste, luoja, luotu, aika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys) VALUES
('harja-ui'::lahde, 'Testikohde', 'urakoitsija'::osapuoli, 123, 1, NOW(), '2015-10-16 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'Roskakori kaatunut', 1, 2, 3, 4, point(418717, 7204244)::GEOMETRY, 5);

