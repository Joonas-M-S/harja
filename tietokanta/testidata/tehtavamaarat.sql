INSERT INTO urakka_tehtavamaara (urakka, "hoitokauden-alkuvuosi", tehtava, maara) VALUES ((select id from urakka where nimi = 'Oulun MHU 2019-2024'), 2020, (select id from toimenpidekoodi where api_tunnus = 1430), 200);
INSERT INTO urakka_tehtavamaara (urakka, "hoitokauden-alkuvuosi", tehtava, maara) VALUES ((select id from urakka where nimi = 'Oulun MHU 2019-2024'), 2020, (select id from toimenpidekoodi where api_tunnus = 1414), 33.4);
INSERT INTO urakka_tehtavamaara (urakka, "hoitokauden-alkuvuosi", tehtava, maara) VALUES ((select id from urakka where nimi = 'Oulun MHU 2019-2024'), 2020, (select id from toimenpidekoodi where api_tunnus = 9775), 32.6);
INSERT INTO urakka_tehtavamaara (urakka, "hoitokauden-alkuvuosi", tehtava, maara) VALUES ((select id from urakka where nimi = 'Oulun MHU 2019-2024'), 2020, (select id from toimenpidekoodi where api_tunnus = 18055), 400);
INSERT INTO urakka_tehtavamaara (urakka, "hoitokauden-alkuvuosi", tehtava, maara) VALUES ((select id from urakka where nimi = 'Oulun MHU 2019-2024'), 2021, (select id from toimenpidekoodi where api_tunnus = 18055), 666);






