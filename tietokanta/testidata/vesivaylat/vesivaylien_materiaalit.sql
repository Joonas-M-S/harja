INSERT INTO vv_materiaali(nimi, maara, pvm, "urakka-id", luoja, luotu) VALUES ('Bensakanisteri', 100, '2017-06-06', (SELECT id FROM urakka WHERE nimi ='Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'), NOW());
INSERT INTO vv_materiaali(nimi, maara, pvm, "urakka-id", luoja, luotu) VALUES ('Hiekkasäkki', 30, '2017-06-06', (SELECT id FROM urakka WHERE nimi ='Helsingin väyläyksikön väylänhoito ja -käyttö, Itäinen SL'), (SELECT id FROM kayttaja WHERE kayttajanimi = 'tero'), NOW());