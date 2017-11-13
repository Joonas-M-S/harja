INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', -20000, 'MAKU 2005', 'Urakoitsija maksaa tilaajalle', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', 5200, 'MAKU 2005', 'Vahingot on nyt korjattu, lasku tulossa.', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-11-18', -65200, 'MAKU 2005', 'Urakoitsija maksaa tilaajalle.', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('asiakastyytyvaisyysbonus', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', 10000, 'MAKU 2005', 'Asiakkaat erittäin tyytyväisiä, tyytyväisyysindeksi 0,92.', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', 20000, 'MAKU 2005', 'Muun erilliskustannuksen lisätieto', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));

INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('asiakastyytyvaisyysbonus', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP 2014-2019'), '2016-01-15', 20000, 'MAKU 2005', 'As.tyyt. bonuksen lisätieto', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP 2014-2019'), '2016-01-17', 10000, 'MAKU 2005', 'Muun erilliskustannuksen lisätieto', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP 2014-2019'), '2016-01-19', -2000, 'MAKU 2005', 'Tilaaja maksaa urakoitsijalle korvausta 2ke', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));

INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('asiakastyytyvaisyysbonus', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Pudasjärvi Talvihoito TP'), '2012-01-15', 20000, 'MAKU 2005', 'As.tyyt. bonuksen lisätieto', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'jvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Pudasjärvi Talvihoito TP'), '2012-01-19', 10000, 'MAKU 2005', 'Muun erilliskustannuksen lisätieto', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'jvh'));

INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Saimaan kanava') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Saimaan kanava'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Saimaan kanava, sopimukseen kuuluvat työt, TP'), '2017-11-11', 9000, 'MAKU 2005', 'Muun erilliskustannuksen lisätieto kanavat', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'jvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,urakka,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Saimaan kanava') AND paasopimus IS null), (SELECT id FROM urakka WHERE nimi='Saimaan kanava'), (SELECT id FROM toimenpideinstanssi WHERE nimi='Testitoimenpideinstanssi'), '2017-11-11', 1000, 'MAKU 2005', 'Muun erilliskustannuksen lisätieto kanavat', NOW(), (SELECT ID FROM kayttaja WHERE kayttajanimi = 'jvh'));
