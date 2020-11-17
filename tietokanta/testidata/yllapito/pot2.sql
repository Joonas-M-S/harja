-- Replikoidaan olemassa oleva potti vanhasta tietomallista
-- Kenties päästään tekemään niiden pohjalta aikanaan myös
-- hyviä testejä, esim. että YHA:an lähtevä hyötykuorma on identtinen jne.

INSERT INTO pot2(
    yllapitokohde,
    takuupvm,
    tila,
    paatos_tekninen_osa,
    lahetetty_yhaan,
    poistettu,
    muokkaaja,
    muokattu,
    luoja,
    luotu)
VALUES
((SELECT id FROM yllapitokohde WHERE nimi = 'Ouluntie'),
 '2022-12-31',
 'aloitettu' ::paallystystila,
 NULL,
 FALSE,
 FALSE,
 NULL,
 NULL,
 (SELECT id FROM kayttaja WHERE nimi = 'Skanska Asfaltti Oy'));