INSERT INTO lasku (viite, erapaiva, kokonaissumma, urakka, tyyppi, luotu, luoja, suorittaja)
VALUES ('2019080019', '2019-10-15', 666.66, (select id from urakka where nimi = 'Oulun MHU 2019-2024'), 'laskutettava', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), (select id from aliurakoitsija where nimi = 'Kaarinan Kadunkiillotus Oy'));
INSERT INTO lasku (viite, erapaiva, kokonaissumma, urakka, tyyppi, luotu, luoja, suorittaja)
VALUES ('2019080022', '2019-10-15', 6666.66, (select id from urakka where nimi = 'Oulun MHU 2019-2024'), 'laskutettava', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), (select id from aliurakoitsija where nimi = 'Alin Urakka Ky'));
INSERT INTO lasku (viite, erapaiva, kokonaissumma, urakka, tyyppi, luotu, luoja, suorittaja)
VALUES ('2019080035', '2019-09-15', 3666.66, (select id from urakka where nimi = 'Oulun MHU 2019-2024'), 'kiinteasti-hinnoiteltu', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), (select id from aliurakoitsija where nimi = 'Kaarinan Kadunkiillotus Oy'));

INSERT INTO lasku_kohdistus (lasku, rivi, toimenpideinstanssi, tehtavaryhma, tehtava, maksueratyyppi, summa, suoritus_alku, suoritus_loppu, luotu, luoja) VALUES ((select id from lasku where viite = '2019080019'), 1, (select id from toimenpideinstanssi where nimi = 'Oulu MHU Liikenneympäristön hoito TP'),(select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'), null, 'kokonaishintainen'::MAKSUERATYYPPI, 333.33, '2019-11-15', '2019-11-18', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));
INSERT INTO lasku_kohdistus (lasku, rivi, toimenpideinstanssi, tehtavaryhma, tehtava, maksueratyyppi, summa, suoritus_alku, suoritus_loppu, luotu, luoja) VALUES ((select id from lasku where viite = '2019080019'), 2, (select id from toimenpideinstanssi where nimi = 'Oulu MHU Liikenneympäristön hoito TP'),(select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'), null, 'kokonaishintainen'::MAKSUERATYYPPI, 222.22, '2019-11-22', '2019-11-25', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));
INSERT INTO lasku_kohdistus (lasku, rivi, toimenpideinstanssi, tehtavaryhma, tehtava, maksueratyyppi, summa, suoritus_alku, suoritus_loppu, luotu, luoja) VALUES ((select id from lasku where viite = '2019080019'), 3, (select id from toimenpideinstanssi where nimi = 'Oulu MHU Liikenneympäristön hoito TP'),(select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'), null, 'kokonaishintainen'::MAKSUERATYYPPI, 111.11, '2019-11-28', '2019-11-30', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));

INSERT INTO lasku_kohdistus (lasku, rivi, toimenpideinstanssi, tehtavaryhma, tehtava, maksueratyyppi, summa, suoritus_alku, suoritus_loppu, luotu, luoja) VALUES ((select id from lasku where viite = '2019080022'), 1, (select id from toimenpideinstanssi where nimi = 'Oulu MHU Liikenneympäristön hoito TP'), NULL, (select id from toimenpidekoodi where nimi = 'Rumpujen tarkastus' and tehtavaryhma is not null), 'kokonaishintainen'::MAKSUERATYYPPI, 2222.22, '2019-08-01', '2019-08-31', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));
INSERT INTO lasku_kohdistus (lasku, rivi, toimenpideinstanssi, tehtavaryhma, tehtava, maksueratyyppi, summa, suoritus_alku, suoritus_loppu, luotu, luoja) VALUES ((select id from lasku where viite = '2019080022'), 2, (select id from toimenpideinstanssi where nimi = 'Oulu MHU Liikenneympäristön hoito TP'), NULL, (select id from toimenpidekoodi where nimi = 'Äkillinen hoitotyö' and tehtavaryhma is not null and emo = 612), 'akillinen-hoitotyo'::MAKSUERATYYPPI, 4444.44, '2019-12-01T06:15:00.000000', '2019-12-01T09:45:00.000000', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));

INSERT INTO lasku_kohdistus (lasku, rivi, toimenpideinstanssi, tehtavaryhma, tehtava, maksueratyyppi, summa, suoritus_alku, suoritus_loppu, luotu, luoja) VALUES ((select id from lasku where viite = '2019080035'), 1, (select id from toimenpideinstanssi where nimi = 'Oulu MHU Talvihoito TP'), NULL, NULL, 'kokonaishintainen'::MAKSUERATYYPPI, 3666.66, '2020-07-01', '2020-07-31', current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));

INSERT INTO liite (nimi, tyyppi, lahde, urakka, luotu, luoja) VALUES ('pensas-2019080019.jpg', 'image/png', 'harja-ui'::lahde, (select id from urakka where nimi = 'Oulun MHU 2019-2024'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));
INSERT INTO lasku_liite (lasku, liite, luotu, luoja) VALUES ((select id from lasku where viite = '2019080019'), (select id from liite where nimi = 'pensas-2019080019.jpg'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'));



