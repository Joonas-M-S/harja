INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 2, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 1-ajorat.'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 2.5, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 2-ajorat.'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 3.5, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='I ohituskaistat'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 4.5, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='I rampit'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
