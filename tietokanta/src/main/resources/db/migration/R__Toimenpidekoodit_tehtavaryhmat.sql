-- ÄLÄ AJA TEHTÄVÄRYHMIÄ enää R-migraatiossa.
-- RYHMIIN JA TOIMENPITEISIIN TEHDÄÄN MUUTOKSIA TAVALLISISSA MIGRAATIOISSA.
-- Jos tehtäväryhmät ja niihin liittyvät tiedot täytyy joskus palauttaa skripteistä, aja nämä lauseet ja sen jälkeen migraatiot, jotka




------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--- TEHTÄVÄRYHMÄT raportointia ja laskutusta varten. Ryhmittelee tehtävät ja lajittelee tehtäväryhmät. Käyttöliittymäjärjestystä vasten ui-taso ------------------------------------
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

-- Aja aina R_Toimenpidekoodit ennen näitä inserttejä

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Talvihoito',	NULL,	'ylataso',	1, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Välitaso Talvihoito',	(select id from tehtavaryhma where nimi = 'Talvihoito'),	'valitaso',	1, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Alataso Talvihoito',	(select id from tehtavaryhma where nimi = 'Välitaso Talvihoito'),	'alataso',	1, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Muut talvihoitotyöt',	(select id from tehtavaryhma where nimi = 'Talvihoito'),	'valitaso',	22, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Alataso Muut talvihoitotyöt',	(select id from tehtavaryhma where nimi = 'Muut talvihoitotyöt'),	'alataso',	22, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Liukkaudentorjunta',	(select id from tehtavaryhma where nimi = 'Talvihoito'),	'valitaso',	25, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'Alataso Liukkaudentorjunta',	(select id from tehtavaryhma where nimi = 'Liukkaudentorjunta'),	'alataso',	25, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen',	NULL,	'ylataso',	35, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Välitaso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen',	(select id from tehtavaryhma where nimi = 'Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	'valitaso',	35, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen',	(select id from tehtavaryhma where nimi = 'Välitaso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	'alataso',	35, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Vakiokokoiset liikennemerkit',	(select id from tehtavaryhma where nimi = 'Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	'valitaso',	39, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Vakiokokoiset liikennemerkit',	(select id from tehtavaryhma where nimi = 'Vakiokokoiset liikennemerkit'),	'alataso',	39, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Opastustaulut ja viitat',	(select id from tehtavaryhma where nimi = 'Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	'valitaso',	42, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.1 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Opastustaulut ja viitat',	(select id from tehtavaryhma where nimi = 'Opastustaulut ja viitat'),	'alataso',	42, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.2 LIIKENNEYMPÄRISTÖN HOITO',	'Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito',	NULL,	'ylataso',	48, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.2 LIIKENNEYMPÄRISTÖN HOITO',	'Välitaso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito',	(select id from tehtavaryhma where nimi = 'Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	'valitaso',	48, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.2 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito',	(select id from tehtavaryhma where nimi = 'Välitaso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	'alataso',	48, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Viheralueiden hoito',	NULL,	'ylataso',	62, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Vesakon raivaus ja runkopuun poisto',	(select id from tehtavaryhma where nimi = 'Viheralueiden hoito'),	'valitaso',	62, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Vesakon raivaus ja runkopuun poisto',	(select id from tehtavaryhma where nimi = 'Vesakon raivaus ja runkopuun poisto'),	'alataso',	62, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt',	(select id from tehtavaryhma where nimi = 'Viheralueiden hoito'),	'valitaso',	65, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt',	(select id from tehtavaryhma where nimi = 'Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt'),	'alataso',	65, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Nurmetus',	(select id from tehtavaryhma where nimi = 'Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt'),	'alataso',	65, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.3 LIIKENNEYMPÄRISTÖN HOITO',	'Puiden ja pensaiden hoito',	(select id from tehtavaryhma where nimi = 'Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt'),	'alataso',	70, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.4 LIIKENNEYMPÄRISTÖN HOITO',	'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito',	NULL,	'ylataso',	79, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.4 LIIKENNEYMPÄRISTÖN HOITO',	'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito',	(select id from tehtavaryhma where nimi = 'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito'),	'valitaso',	79, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.4 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito',	(select id from tehtavaryhma where nimi = 'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito'),	'alataso',	79, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito (rumpujen kunnossapito)',	(select id from tehtavaryhma where nimi = 'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito'),	'alataso',	84, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;


INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Rumpujen kunnossapito ja uusiminen',	NULL,	'ylataso',	86, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Rumpujen kunnossapito ja uusiminen (päällystetty tie)',	(select id from tehtavaryhma where nimi =  'Rumpujen kunnossapito ja uusiminen'),	'valitaso',	86, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Yksityiset rummut (päällystetty tie)',	(select id from tehtavaryhma where nimi =  'Rumpujen kunnossapito ja uusiminen (päällystetty tie)'),	'alataso',	86, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Ei-yksityiset rummut (päällystetty tie)',	(select id from tehtavaryhma where nimi =  'Rumpujen kunnossapito ja uusiminen (päällystetty tie)'),	'alataso',	88, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Rumpujen kunnossapito ja uusiminen (soratie)',	(select id from tehtavaryhma where nimi =  'Rumpujen kunnossapito ja uusiminen'),	'valitaso',	90, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Yksityiset rummut (soratie)',	(select id from tehtavaryhma where nimi =  'Rumpujen kunnossapito ja uusiminen (soratie)'),	'alataso',	90, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.5 LIIKENNEYMPÄRISTÖN HOITO',	'Ei-yksityiset rummut (soratie)',	(select id from tehtavaryhma where nimi =  'Rumpujen kunnossapito ja uusiminen (soratie)'), 'alataso',	92, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.6 LIIKENNEYMPÄRISTÖN HOITO',	'Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito',	NULL,	'ylataso',	97, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.6 LIIKENNEYMPÄRISTÖN HOITO',	'Välitaso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito',	(select id from tehtavaryhma where nimi =  'Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito'),	'valitaso',	97, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.6 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito',	(select id from tehtavaryhma where nimi =  'Välitaso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito'),	'alataso',	97, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO',	'Päällysteiden paikkaus',	NULL,	'ylataso',	102, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO',	'Välitaso Päällysteiden paikkaus',	(select id from tehtavaryhma where nimi =  'Päällysteiden paikkaus'),	'valitaso',	102, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Päällysteiden paikkaus',	(select id from tehtavaryhma where nimi =  'Välitaso Päällysteiden paikkaus'),	'alataso',	102, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO',	'Sillan päällysteen korjaus',	(select id from tehtavaryhma where nimi =  'Päällysteiden paikkaus'),	'valitaso',	109, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Sillan päällysteen korjaus',	(select id from tehtavaryhma where nimi =  'Sillan päällysteen korjaus'),	'alataso',	109, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.8 LIIKENNEYMPÄRISTÖN HOITO',	'Päällystettyjen teiden sorapientareen kunnossapito',	NULL,	'ylataso',	116, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.8 LIIKENNEYMPÄRISTÖN HOITO',	'Välitaso Päällystettyjen teiden sorapientareen kunnossapito',	(select id from tehtavaryhma where nimi =  'Päällystettyjen teiden sorapientareen kunnossapito'),	'valitaso',	116, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.8 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Päällystettyjen teiden sorapientareen kunnossapito',	(select id from tehtavaryhma where nimi =  'Välitaso Päällystettyjen teiden sorapientareen kunnossapito'),	'alataso',	116, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.9 LIIKENNEYMPÄRISTÖN HOITO',	'Siltojen ja laitureiden hoito',	NULL,	'ylataso',	121, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.9 LIIKENNEYMPÄRISTÖN HOITO',	'Välitaso Siltojen ja laitureiden hoito',	(select id from tehtavaryhma where nimi =  'Siltojen ja laitureiden hoito'),	'valitaso',	121, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.9 LIIKENNEYMPÄRISTÖN HOITO',	'Alataso Siltojen ja laitureiden hoito',	(select id from tehtavaryhma where nimi =  'Välitaso Siltojen ja laitureiden hoito'),	'alataso',	121, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Sorateiden hoito',	NULL,	'ylataso',	124, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Välitaso Sorateiden hoito',	(select id from tehtavaryhma where nimi =  'Sorateiden hoito'),	'valitaso',	124, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Alataso Sorateiden hoito',	(select id from tehtavaryhma where nimi =  'Välitaso Sorateiden hoito'),	'alataso',	128, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Sorateiden pinnan hoito',	(select id from tehtavaryhma where nimi =  'Sorateiden hoito'),	'valitaso',	124, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Alataso Sorateiden pinnan hoito',	(select id from tehtavaryhma where nimi =  'Sorateiden pinnan hoito'),	'alataso',	124, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Muut sorateiden hoidon tehtävät',	(select id from tehtavaryhma where nimi =  'Sorateiden hoito'),	'valitaso',	129, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'Alataso Muut sorateiden hoidon tehtävät',	(select id from tehtavaryhma where nimi =  'Muut sorateiden hoidon tehtävät'),	'alataso',	129, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'MHU Ylläpito',	NULL,	'ylataso',	139, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Välitaso MHU Ylläpito',	(select id from tehtavaryhma where nimi =  'MHU Ylläpito'),	'valitaso',	139, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Alataso MHU Ylläpito',	(select id from tehtavaryhma where nimi =  'Välitaso MHU Ylläpito'),	'alataso',	139, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Ojitus',	(select id from tehtavaryhma where nimi =  'MHU Ylläpito'),	'valitaso',	139, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Alataso Ojitus',	(select id from tehtavaryhma where nimi =  'Ojitus'),	'alataso',	139, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Alataso Ojitus (päällystetyt tiet)',	(select id from tehtavaryhma where nimi =  'Ojitus'),	'alataso',	139, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Alataso Ojitus (soratiet)',	(select id from tehtavaryhma where nimi =  'Ojitus'),	'alataso',	142, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Pohjavesisuojaukset',	NULL,	'ylataso',	152, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Välitaso Pohjavesisuojaukset',	(select id from tehtavaryhma where nimi =  'Pohjavesisuojaukset'),	'valitaso',	152, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '5 KORJAAMINEN',	'Alataso Pohjavesisuojaukset',	(select id from tehtavaryhma where nimi =  'Välitaso Pohjavesisuojaukset'),	'alataso',	152, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Muut liik.ymp.hoitosasiat',	NULL,	'ylataso',	155, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Välitaso Muut liik.ymp.hoitosasiat',	(select id from tehtavaryhma where nimi =  'Muut liik.ymp.hoitosasiat'),	'valitaso',	155, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Alataso Muut liik.ymp.hoitosasiat',	(select id from tehtavaryhma where nimi =  'Välitaso Muut liik.ymp.hoitosasiat'),	'alataso',	155, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Johto- ja hallintokorvaukseen sisältyvät tehtävät',	NULL,	'ylataso',	161, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Välitaso Johto- ja hallintokorvaukseen sisältyvät tehtävät',	(select id from tehtavaryhma where nimi =  'Johto- ja hallintokorvaukseen sisältyvät tehtävät'),	'valitaso',	161, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Alataso Johto- ja hallintokorvaukseen sisältyvät tehtävät',	(select id from tehtavaryhma where nimi =  'Välitaso Johto- ja hallintokorvaukseen sisältyvät tehtävät'),	'alataso',	161, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Erillishankinnat erillishinnoin',	NULL,	'valitaso',	165, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), TRUE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Välitaso Erillishankinnat erillishinnoin',	(select id from tehtavaryhma where nimi =  'Erillishankinnat erillishinnoin'),	'valitaso',	165, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'Alataso Erillishankinnat erillishinnoin',	(select id from tehtavaryhma where nimi =  'Välitaso Erillishankinnat erillishinnoin'),	'alataso',	165, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE) ON CONFLICT DO NOTHING;



------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--- MHU: Uudet tehtävät. Näille ei löydynyt vastaavuutta vanhoista tehtävistä. -----------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

-- Aja toimenpidekoodit ja tehtäväryhmät ennen näitä inserttejä
-- Tehtävät on järjestetty (toimenpidekoodi.jarjestys) vuoden 2019 tehtävä- ja määräluettelon mukaan (sopimuksen liite). Siksi saman toimenpiteen alle kuuluvia tehtäviä on useassa osiossa.
-- Toivottavasti jatkossa sopimuksen liite on toimenpidekoodi- ja tehtäväryhmätaulujen todellisen hierarkian mukainen.

-- Talvihoito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ise 2-ajorat.', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'tiekm',	1, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ise 1-ajorat.', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'tiekm',	2, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ise ohituskaistat', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'kaistakm',	3, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ise rampit', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'kaistakm',	4, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ic 2-ajorat', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'tiekm',	13, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ic 1-ajorat', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'tiekm',	14, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ic ohituskaistat', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'kaistakm',	15, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ic rampit', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'kaistakm',	16, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kävely- ja pyöräilyväylien laatukäytävät', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'tiekm',	19, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Levähdys- ja pysäköimisalueet', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'kpl',	22, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muiden alueiden talvihoito', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'Mitä seurataan?',	23, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Talvihoidon kohotettu laatu', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'tiekm',	24, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Ennalta arvaamattomien kuljetusten avustaminen', (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	'tonnia',	28, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Pysäkkikatosten puhdistus', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'kpl',	29, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Hiekkalaatikoiden täyttö ja hiekkalaatikoiden edustojen lumityöt', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'kpl',	30, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Portaiden talvihoito', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'kpl',	31, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Lisäkalustovalmius/-käyttö', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	'kpl',	32, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- Liikenneympäristön hoito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Liikennemerkkien ja opasteiden kunnossapito (oikominen, pesu yms.)', (select id from tehtavaryhma where nimi = 'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	'',	35, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoitotyöt', (select id from tehtavaryhma where nimi = 'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	NULL,	47, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Tie- ja levähdysalueiden kalusteiden kunnossapito ja hoito', (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	NULL,	49, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Pysäkkikatosten siisteydestä huolehtiminen (oikaisu, huoltomaalaus jne.) ja jätehuolto sekä pienet vaurioiden korjaukset', (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	'kpl',	50, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Hiekoitushiekan ja irtoainesten poisto', (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'	),	NULL,	52, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut tie- levähdys- ja liitännäisalueiden puhtaanpitoon ja kalusteiden hoitoon liittyvät työt', (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	'',	57, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Erillisten hoito-ohjeiden mukaiset vihertyöt', (select id from tehtavaryhma where nimi = 'Alataso Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt'),	NULL,	72, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Erillisten hoito-ohjeiden mukaiset vihertyöt, uudet alueet', (select id from tehtavaryhma where nimi = 'Alataso Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt'),	NULL,	73, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Vesistöpenkereiden hoito', (select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'),	NULL,	75, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Tiekohtaiset maisemanhoitoprojektit', (select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'),	NULL,	76, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kaivojen ja putkistojen tarkastus', (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito'),	NULL,	80, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kaivojen ja putkistojen sulatus', (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito'),	NULL,	81, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kuivatusjärjestelmän pumppaamoiden hoito ja tarkkailu', (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito'),	'kpl',	82, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Rumpujen sulatus, aukaisu ja toiminnan varmistaminen', (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito (rumpujen kunnossapito)'),	'',	84, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Rumpujen tarkastus', (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito (rumpujen kunnossapito)'),	NULL,	85, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- MHU Ylläpito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Yksityisten rumpujen korjaus ja uusiminen  Ø ≤ 400 mm, päällystetyt tiet', (select id from tehtavaryhma where nimi = 'Yksityiset rummut (päällystetty tie)'),	'jm',	86, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Yksityisten rumpujen korjaus ja uusiminen  Ø > 400 mm ≤ 600 mm, päällystetyt tiet', (select id from tehtavaryhma where nimi = 'Yksityiset rummut (päällystetty tie)'),	'jm',	87, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Yksityisten rumpujen korjaus ja uusiminen  Ø ≤ 400 mm, soratiet', (select id from tehtavaryhma where nimi = 'Yksityiset rummut (soratie)'),	'jm',	90, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Yksityisten rumpujen korjaus ja uusiminen  Ø > 400 mm ≤ 600 mm, soratiet', (select id from tehtavaryhma where nimi = 'Yksityiset rummut (soratie)'),	'jm',	91, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Päällystetyn tien rumpujen korjaus ja uusiminen Ø <= 600 mm' , (select id from tehtavaryhma where nimi = 'Ei-yksityiset rummut (päällystetty tie)'), 'jm', 88, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Päällystetyn tien rumpujen korjaus ja uusiminen  Ø> 600  <= 800 mm' , (select id from tehtavaryhma where nimi = 'Ei-yksityiset rummut (päällystetty tie)'), 'jm', 89, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Soratien rumpujen korjaus ja uusiminen  Ø <= 600 mm' , (select id from tehtavaryhma where nimi = 'Ei-yksityiset rummut (soratie)'), 'jm', 92, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Soratien rumpujen korjaus ja uusiminen  Ø> 600  <=800 mm' , (select id from tehtavaryhma where nimi = 'Ei-yksityiset rummut (soratie)'), 'jm', 93, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- Liikenneympäristön hoito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut rumpujen kunnossapitoon liittyvät työt', (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito (rumpujen kunnossapito)'),	NULL,	96, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kaiteiden ja aitojen tarkastaminen ja vaurioiden korjaukset', (select id from tehtavaryhma where nimi = 'Alataso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito'	),	'',	97, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Reunakivivaurioiden korjaukset', (select id from tehtavaryhma where nimi = 'Alataso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito'),	'',	98, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapitoon liittyvät työt', (select id from tehtavaryhma where nimi = 'Alataso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito'),	NULL,	101, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- Päällysteiden paikkaus
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut päällysteiden paikkaukseen liittyvät työt', (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	NULL,	115, NULL,  (select id from toimenpidekoodi where koodi = '20107'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut päällystettyjen teiden sorapientareiden kunnossapitoon liittyvät työt', (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'),	NULL,	120, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- Liikenneympäristön hoito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Siltojen hoito (kevätpuhdistus, puhtaanapito, kasvuston poisto ja pienet kunnostustoimet sekä vuositarkastukset)', (select id from tehtavaryhma where nimi = 'Alataso Siltojen ja laitureiden hoito'),	'kpl',	121, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Laitureiden hoito (puhtaanapito, pienet kunnostustoimet, turvavarusteiden kunnon varmistaminen sekä vuositarkastukset)', (select id from tehtavaryhma where nimi = 'Alataso Siltojen ja laitureiden hoito'),	'kpl',	122, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Muut siltojen ja laitureiden hoitoon liittyvät työt', (select id from tehtavaryhma where nimi = 'Alataso Siltojen ja laitureiden hoito'),	NULL,	123, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- Sorateiden hoito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Sorateiden pinnan hoito, hoitoluokka II', (select id from tehtavaryhma where nimi = 'Alataso Sorateiden pinnan hoito'),	'tiekm',	125, NULL, (select id from toimenpidekoodi where koodi = '23124'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Sorateiden pinnan hoito, hoitoluokka III', (select id from tehtavaryhma where nimi = 'Alataso Sorateiden pinnan hoito'),	'tiekm',	126, NULL, (select id from toimenpidekoodi where koodi = '23124'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Sorapintaisten kävely- ja pyöräilyväylienhoito', (select id from tehtavaryhma where nimi = 'Alataso Sorateiden pinnan hoito'),	'tiekm',	127, NULL, (select id from toimenpidekoodi where koodi = '23124'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Maakivien (>1m3) poisto', (select id from tehtavaryhma where nimi = 'Alataso Sorateiden pinnan hoito'),	'jm',	130, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- MHU Ylläpito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Avo-ojitus/päällystetyt tiet' , (select id from tehtavaryhma where nimi = 'Alataso Ojitus (päällystetyt tiet)'), 'jm', 139, (select id from toimenpidekoodi where nimi = 'Avo-ojitus / päällystetyt tiet' and emo = (select id from toimenpidekoodi where koodi = '20112')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Avo-ojitus/päällystetyt tiet (kaapeli kaivualueella)' , (select id from tehtavaryhma where nimi = 'Alataso Ojitus (päällystetyt tiet)'), 'jm', 140, (select id from toimenpidekoodi where nimi = 'Avo-ojitus/päällystetyt tiet (kaapeli kaivualueella)' and emo = (select id from toimenpidekoodi where koodi = '20112')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Laskuojat/päällystetyt tiet' , (select id from tehtavaryhma where nimi = 'Alataso Ojitus (päällystetyt tiet)'), 'jm', 141, (select id from toimenpidekoodi where nimi = 'Laskuojat/päällystetyt tiet' and emo = (select id from toimenpidekoodi where koodi = '20112')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Avo-ojitus/soratiet' , (select id from tehtavaryhma where nimi = 'Alataso Ojitus (soratiet)'), 'jm', 142, (select id from toimenpidekoodi where nimi = 'Sorateiden avo-ojitus' and emo = (select id from toimenpidekoodi where koodi = '20143')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Avo-ojitus/soratiet (kaapeli kaivualueella)' , (select id from tehtavaryhma where nimi = 'Alataso Ojitus (soratiet)'), 'jm', 143, (select id from toimenpidekoodi where nimi = 'Sorateiden avo-ojitus (kaapeli kaivualueella)' and emo = (select id from toimenpidekoodi where koodi = '20143')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Laskuojat/soratiet' , (select id from tehtavaryhma where nimi = 'Alataso Ojitus (soratiet)'), 'jm', 144, (select id from toimenpidekoodi where nimi = 'Laskuojat/soratiet' and emo = (select id from toimenpidekoodi where koodi = '20143')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kalliokynsien louhinta ojituksen yhteydessä', (select id from tehtavaryhma where nimi = 'Alataso Ojitus'),	'm2',	145, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Pohjavesisuojaukset', (select id from tehtavaryhma where nimi = 'Alataso Pohjavesisuojaukset'),	NULL,	152, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

-- Liikenneympäristön hoito
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Nopeusnäyttötaulun hankinta', (select id from tehtavaryhma where nimi = 'Alataso Muut liik.ymp.hoitosasiat'),	'kpl/1. hoitovuosi',	155, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
-- TODO: Onko oikea toimenpidekoodi?

INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Pysäkkikatoksen uusiminen' , (select id from tehtavaryhma where nimi = 'Alataso Muut liik.ymp.hoitosasiat'), 'kpl', 157, (select id from toimenpidekoodi where nimi = 'Pysäkkikatoksen uusiminen' and emo = (select id from toimenpidekoodi where koodi = '14301')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES ('Pysäkkikatoksen poistaminen' , (select id from tehtavaryhma where nimi = 'Alataso Muut liik.ymp.hoitosasiat'), 'kpl', 158, (select id from toimenpidekoodi where nimi = 'Pysäkkikatoksen poistaminen' and emo = (select id from toimenpidekoodi where koodi = '14301')), (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Tilaajan rahavaraus lupaukseen 1', (select id from tehtavaryhma where nimi = 'Alataso Muut liik.ymp.hoitosasiat'),	'euroa',	159, NULL, (select id from toimenpidekoodi where koodi = '20191'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
-- TODO: Onko oikea toimenpidekoodi?

--  MHU
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Hoitourakan työnjohto', (select id from tehtavaryhma where nimi = 'Alataso Johto- ja hallintokorvaukseen sisältyvät tehtävät'),	NULL,	161, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Toimistotarvike- ja ICT-kulut, tiedotus, opastus, kokousten järjestäminen jne.', (select id from tehtavaryhma where nimi = 'Alataso Johto- ja hallintokorvaukseen sisältyvät tehtävät'),	NULL,	162, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Hoito- ja korjaustöiden pientarvikevarasto', (select id from tehtavaryhma where nimi = 'Alataso Johto- ja hallintokorvaukseen sisältyvät tehtävät'),	NULL,	163, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Osallistuminen tilaajalle kuuluvien viranomaistehtävien hoitoon', (select id from tehtavaryhma where nimi = 'Alataso Johto- ja hallintokorvaukseen sisältyvät tehtävät'),	NULL,	164, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Toimitilat sähkö-, lämmitys-, vesi-, jäte-, siivous-, huolto-, korjaus- ja vakuutus- yms. kuluineen', (select id from tehtavaryhma where nimi = 'Alataso Erillishankinnat erillishinnoin'),	NULL,	165, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Hoitourakan tarvitsemat kelikeskus- ja keliennustepalvelut', (select id from tehtavaryhma where nimi = 'Alataso Erillishankinnat erillishinnoin'),	NULL,	166, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Seurantajärjestelmät (mm. ajantasainen seuranta, suolan automaattinen seuranta)', (select id from tehtavaryhma where nimi = 'Alataso Erillishankinnat erillishinnoin'),	NULL,	167, NULL, (select id from toimenpidekoodi where koodi = '23151'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;


------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--- MHU: Uudet tehtävät. Näille löytyi useita vastaavuuksia vanhoista tehtävistä ja täytyy lisätä ensisijainen tehtävä määrien suunnittelua varten. --------------------------------
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
-- Jos löytyy oikean niminen vanha tehtävä, niputtavaa tehtävää ei ole luotu vaan käytetään niputtajana olemassa olevaa tehtävää.
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Liukkaudentorjunta hiekoituksella', (select id from tehtavaryhma where nimi = 'Alataso Liukkaudentorjunta'),	'jkm',27, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Opastustaulun/-viitan uusiminen', (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'), 'm2',	42  , NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kuumapäällyste', (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	'tonni', 102, NULL, (select id from toimenpidekoodi where koodi = '20107'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Puhallus-SIP', (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	'tonni', 104, NULL, (select id from toimenpidekoodi where koodi = '20107'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Massasaumaus', (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	'tonni', 106, NULL, (select id from toimenpidekoodi where koodi = '20107'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Valuasfaltti', (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	'tonni', 108, NULL, (select id from toimenpidekoodi where koodi = '20107'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Reunantäyttö', (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'), 'tonni',	116, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Liikenteen varmistaminen kelirikkokohteissa', (select id from tehtavaryhma where nimi = 'Alataso Muut sorateiden hoidon tehtävät'),	'tonni', 132, NULL, (select id from toimenpidekoodi where koodi = '23124'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Soratien runkokelirikkokorjaukset', (select id from tehtavaryhma where nimi = 'Alataso MHU Ylläpito'),	'tiem', 146, NULL, (select id from toimenpidekoodi where koodi = '14301'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;



------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--- MHU: Uudet tehtävät. Äkillinen hoitotyö ja kolmansien osapuolten aiheuttamien vahinkojen korjaaminen                                            --------------------------------
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Äkillinen hoitotyö', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	NULL,	135, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Äkillinen hoitotyö', (select id from tehtavaryhma where nimi = 'Muut liik.ymp.hoitosasiat'),	NULL,	135, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Äkillinen hoitotyö', (select id from tehtavaryhma where nimi = 'Alataso Sorateiden hoito'),	NULL,	135, NULL, (select id from toimenpidekoodi where koodi = '23124'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;

INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen', (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'), NULL, 136, NULL, (select id from toimenpidekoodi where koodi = '23104'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen', (select id from tehtavaryhma where nimi = 'Muut liik.ymp.hoitosasiat'),	NULL,	136, NULL, (select id from toimenpidekoodi where koodi = '23116'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;
INSERT into toimenpidekoodi (nimi, tehtavaryhma, yksikko, jarjestys, api_tunnus, emo, luotu, luoja, taso, ensisijainen) VALUES (	'Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen', (select id from tehtavaryhma where nimi = 'Alataso Sorateiden hoito'),	NULL,	136, NULL, (select id from toimenpidekoodi where koodi = '23124'), current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), 4, TRUE) ON CONFLICT DO NOTHING;



------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
--- MHU: Olemassa olleiden tehtävien tehtäväryhmämäppäykset     --------------------------------------------------------------------------------------------------------------------
------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

----------------
-- TALVIHOITO --
----------------
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 5, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Is 2-ajorat.' AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 6, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Is 1-ajorat.'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 7, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Is ohituskaistat' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 8, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Is rampit'	AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 9, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Ib 2-ajorat.'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 10, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Ib 1-ajorat.'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 11, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Ib ohituskaistat'	AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 12, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Ib rampit'	AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 17, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'II'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 18, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'III'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 20, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'K1'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Talvihoito'),	jarjestys = 21, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'K2'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Liukkaudentorjunta'),	jarjestys = 25, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Suolaus' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
-- Suolaus-tehtävän yksikkö on jkm, materiaalit raportoidaan erikseen (tonneina)

-- Liukkauden torjunta hiekoittamalla
---------------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
-- Yksikkö on jkm, materiaalit tonneina erikseen raportointuna
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Liukkaudentorjunta'),	jarjestys = 27, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Pistehiekoitus' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Liukkaudentorjunta'),	jarjestys = 27, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Linjahiekoitus' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND poistettu is not true AND piilota is not true;

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt'),	jarjestys = 34, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Ei yksilöity' AND emo = (select id from toimenpidekoodi where koodi = '23104') AND yksikko = '-' AND poistettu is not true AND piilota is not true;


------------------------------
-- LIIKENNEYMPÄRISTÖN HOITO --
------------------------------
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	jarjestys = 36, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Valvontakameratolppien puhdistus/tarkistus keväisin' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	jarjestys = 37, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Reunapaalujen kp (uusien)'	AND yksikko = 'tiekm' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen'),	jarjestys = 38, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Porttaalien tarkastus ja huolto' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vakiokokoiset liikennemerkit'),	jarjestys = 39, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Vakiokokoisten liikennemerkkien uusiminen,  pelkkä merkki'	AND yksikko = 'kpl' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vakiokokoiset liikennemerkit'),	jarjestys = 40, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Vakiokokoisten liikennemerkkien uusiminen ja lisääminen merkki tukirakenteineen (60 mm varsi)' AND yksikko = 'kpl' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vakiokokoiset liikennemerkit'),	jarjestys = 41, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Vakiokokoisten liikennemerkkien uusiminen ja lisääminen merkki tukirakenteineen (90 mm varsi)' AND yksikko = 'kpl' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;


-- Opastustaulun/-viitan uusiminen
-----------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'),	jarjestys = 42, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Opastinviitan tai -taulun uusiminen ja lisääminen -ajoradan yläpuoliset opasteet'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'),	jarjestys = 42, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Opastustaulujen ja opastusviittojen uusiminen -vanhan viitan/opastetaulun uusiminen'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'),	jarjestys = 43, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Opastustaulujen ja liikennemerkkien rakentaminen tukirakenteineen (sis. liikennemerkkien poistamisia)'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;


-- Opastustaulun/viitan uusiminen porttaalissa
-----------------------------------
-- Kolme vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'),	jarjestys = 44, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Opastustaulujen ja opastusviittojen uusiminen portaaliin'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'),	jarjestys = 44, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Opastustaulujen ja opastusviittojen uusiminen -portaalissa olevan viitan/opastetaulun uusiminen'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat'),	jarjestys = 44, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Opastustaulujen ja opastusviittojen uusiminen -porttaalissa olevan viitan/opastetaulun uusiminen'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	jarjestys = 48, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Levähdysalueen puhtaanapito' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	jarjestys = 51, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Meluesteiden pesu' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;


-- Töherrysten poisto
-----------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	jarjestys = 53, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Töherrysten poisto'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	jarjestys = 53, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Graffitien poisto'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;


UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	jarjestys = 54, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Töherrysten estokäsittely'	AND yksikko = 'm2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito'),	jarjestys = 55, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Katupölynsidonta' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vesakon raivaus ja runkopuun poisto'),	jarjestys = 62, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Vesakonraivaus N1'	AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vesakon raivaus ja runkopuun poisto'),	jarjestys = 63, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Vesakonraivaus N2'AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vesakon raivaus ja runkopuun poisto'),	jarjestys = 64, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Vesakonraivaus N3'	AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Nurmetus'),	jarjestys = 65, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Nurmetuksen hoito / niitto N1' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Nurmetus'),	jarjestys = 66, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Nurmetuksen hoito / niitto N2' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Nurmetus'),	jarjestys = 67, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Nurmetuksen hoito / niitto N3' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Nurmetus'),	jarjestys = 68, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Nurmetuksen hoito / niitto T1/E1'	AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Nurmetus'),	jarjestys = 69, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Nurmetuksen hoito / niitto T2/E2'	AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'),	jarjestys = 70, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Puiden ja pensaiden hoito T1/E1'	AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito'),	jarjestys = 71, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Puiden ja pensaiden hoito T2/E2/N1' AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vesakon raivaus ja runkopuun poisto'),	jarjestys = 74, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Runkopuiden poisto'	AND yksikko = 'kpl' AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt'),	jarjestys = 78, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Muut viheralueiden hoitoon liittyvät työt'	AND yksikko = '-'  AND emo = (select id from toimenpidekoodi where koodi = '23116') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito'),	jarjestys = 79, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito'	AND (yksikko = '-' OR yksikko is NULL) AND emo = (select id from toimenpidekoodi where koodi = '23116');



------------------------------------
-- KORJAUS I                      --
------------------------------------

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito'),	jarjestys = 97, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Kaiteiden ja aitojen tarkastaminen ja vaurioiden korjaukset'	AND (yksikko = '' OR yksikko is NULL) AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23116');


------------------------------------
-- PÄÄLLYSTEIDEN PAIKKAUS         --
------------------------------------

-- Kuumapäällyste
-----------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 102, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Kuumapäällyste, ab käsityönä'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 102, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - kuumapäällyste'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

-- Kylmäpäällyste ml. SOP
-----------------------------------
-- Kolme vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 103, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus, kylmäpäällyste'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 103, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus -kylmäpäällyste ml. SOP'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 103, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -kylmäpäällyste ml. SOP'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

-- Puhallus-SIP
-----------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 104, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -puhallus SIP'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 104, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'SIP paikkaus (kesto+kylmä)' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 105, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus -saumojen juottaminen bitumilla'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

-- Massasaumaus
-----------------------------------
-- Viisi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 106, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Konetiivistetty massasaumaus 10 cm leveä' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 106, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Konetiivistetty massasaumaus 20 cm leveä' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 106, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus - Konetiivistetty massasaumaus 20 cm leveä'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 106, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus - massasaumaus'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 106, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - massasaumaus'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

-- Konetiivistetty valuasfaltti
-----------------------------------
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 107, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -konetivistetty valuasvaltti'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

-- Valuasfaltti
-----------------------------------
-- Kolme vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 108, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Kuumapäällyste, valuasfaltti'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 108, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - valuasvaltti'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällysteiden paikkaus'),	jarjestys = 108, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällysteiden paikkaus - valuasfaltti'	AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sillan päällysteen korjaus'),	jarjestys = 109, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Sillan päällysteen halkeaman avarrussaumaus'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sillan päällysteen korjaus'),	jarjestys = 110, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Sillan kannen päällysteen päätysauman korjaukset'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sillan päällysteen korjaus'),	jarjestys = 111, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Reunapalkin ja päällysteen väl. sauman tiivistäminen'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sillan päällysteen korjaus'),	jarjestys = 112, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Reunapalkin liikuntasauman tiivistäminen'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '20107');


------------------------------------
-- LIIKENNEYMPÄRISTÖN HOITO --
------------------------------------

-- Reunantäyttö
-----------------------------------
-- Kolme vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'),	jarjestys = 116, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällystettyjen teiden pientareiden täyttö'	AND (yksikko = 'tn' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23116');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'),	jarjestys = 116, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällystettyjen teiden sorapientareen täyttö'	AND (yksikko = 'tn' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23116');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'),	jarjestys = 116, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällystettyjen teiden sr-pientareen täyttö'	AND (yksikko = 'tn' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23116');


UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'),	jarjestys = 117, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Päällystettyjen teiden palteiden poisto'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23116');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito'),	jarjestys = 118, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Reunapalteen poisto kaiteen alta'	AND yksikko = 'jm' AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23116');


------------------------------------
-- SORATEIDEN HOITO --
------------------------------------
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sorateiden pinnan hoito'),	jarjestys = 124, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Sorateiden hoito, hoitoluokka I'	AND yksikko = 'tiekm' AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23124');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sorateiden hoito'), jarjestys = 128, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Sorateiden pölynsidonta' AND poistettu is not true AND piilota is not true AND emo = (select id from toimenpidekoodi where koodi = '23124');
-- Tehtävän yksikkö on jkm, materiaalit erikseen (t)

-- Sorastus
-----------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen (ensimmäinen ensisijainen)
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut sorateiden hoidon tehtävät'),	jarjestys = 129, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Sorastus' AND (yksikko = 't' or yksikko = 'tonni') AND poistettu is not true AND piilota is not true;
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut sorateiden hoidon tehtävät'),	jarjestys = 129, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Sorastus km' AND poistettu is not true AND piilota is not true;


UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut sorateiden hoidon tehtävät'),	jarjestys = 131, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Oja- ja luiskameteriaalin käyttö kulutuskerrokseeen' AND emo = (select id from toimenpidekoodi where koodi = '23124');

-- Liikenteen varmistaminen kelirikkokohteessa
--------------------------------------
-- Kaksi vanhaa tehtävää mäpätty yhteen
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut sorateiden hoidon tehtävät'),	jarjestys = 132, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Liikenteen varmistaminen kelirikkokohteissa' AND emo = (select id from toimenpidekoodi where koodi = '23124');
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut sorateiden hoidon tehtävät'),	jarjestys = 132, ensisijainen = FALSE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Liikenteen varmistaminen kelirikkokohteissa (tonni)'	AND yksikko = 'tonni' AND emo = (select id from toimenpidekoodi where koodi = '23124');

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Sorateiden pinnan hoito'),	jarjestys = 134, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Ei yksilöity' AND emo = (select id from toimenpidekoodi where koodi = '23124')	AND (yksikko = '-' OR yksikko is NULL) AND poistettu is not true AND piilota is not true;


------------------------------------
-- LIIKENNEYMPÄRISTÖN HOITO --
------------------------------------
UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut liik.ymp.hoitosasiat'),	jarjestys = 156, ensisijainen = TRUE, muokattu = current_timestamp, muokkaaja = (select id from kayttaja where kayttajanimi = 'Integraatio') WHERE taso = 4 and nimi = 'Nopeusnäyttötaulujen ylläpito, käyttö ja siirto'	AND emo = (select id from toimenpidekoodi where koodi = '23116');-- AND yksikko = 'kpl'; yksikkö on erä eikä kappal AND poistettu is not true AND piilota is not truee


-- Päivitä vielä nimet
UPDATE toimenpidekoodi set nimi = 'Muut talvihoitotyöt' WHERE emo = (select id from toimenpidekoodi where koodi = '23104') and nimi = 'Ei yksilöity';
UPDATE toimenpidekoodi set nimi = 'Muut sorateiden hoitoon liittyvät tehtävät' WHERE emo = (select id from toimenpidekoodi where koodi = '23124') and nimi = 'Ei yksilöity';

-- Päivitetään api-tunnus tehtävähierarkian tehtäville, joilla sitä ei entuudestaan ole. Sama tunnus kaikkiin ympäristöihin (prod, stg, test, local). Huom. osa tehtävistä puuttuu kehitysympäristöstä.
-- Api-tunnuksen olemassa olo ei tarkoita sitä, että tehtävälle kirjataan apin kautta toteumia. Api-käyttöä määrittää seurataan-apin-kautta-sarake.
UPDATE toimenpidekoodi
set api_tunnus = (115 * jarjestys)
where tehtavaryhma is not null and api_tunnus is null;




-- Muokkaukset uuden Tehtävä- ja määräluetteloversion johdosta


UPDATE tehtavaryhma SET nimi = 'TALVIHOITO' where nimi = 'Alataso Talvihoito';
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'TALVIHOITO' and tyyppi = 'alataso') WHERE  tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Muut talvihoitotyöt' and tyyppi = 'alataso');
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Alataso Muut talvihoitotyöt', 'Muut talvihoitotyöt');

UPDATE tehtavaryhma SET nimi = 'TALVISUOLA' where nimi = 'Alataso Liukkaudentorjunta';

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'KFO,NAFO',	(select id from tehtavaryhma where nimi = 'Liukkaudentorjunta'),	'alataso',	26, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '1.0 TALVIHOITO',	'HIEKOITUS',	(select id from tehtavaryhma where nimi = 'Liukkaudentorjunta'),	'alataso',	27, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);-- Siirrä tehtävät uuteen tehtäväryhmään
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'HIEKOITUS' and tyyppi = 'alataso') WHERE  nimi in ('Linjahiekoitus', 'Pistehiekoitus', 'Liukkaudentorjunta hiekoituksella') and tehtavaryhma IS NOT NULL;

UPDATE tehtavaryhma SET nimi = 'LIIKENNEMERKIT JA LIIKENTEENOHJAUSLAITTEET' where nimi = 'Alataso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen';
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'LIIKENNEMERKIT JA LIIKENTEENOHJAUSLAITTEET' and tyyppi = 'alataso') WHERE
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Vakiokokoiset liikennemerkit' and tyyppi = 'alataso') OR
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Opastustaulut ja viitat' and tyyppi = 'alataso');
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Alataso Vakiokokoiset liikennemerkit', 'Alataso Opastustaulut ja viitat', 'Vakiokokoiset liikennemerkit', 'Opastustaulut ja viitat');

UPDATE tehtavaryhma SET nimi = 'PUHTAANAPITO' where nimi = 'Alataso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito';

UPDATE tehtavaryhma SET nimi = 'VESAKONRAIVAUKSET JA PUUN POISTO' where nimi = 'Alataso Vesakon raivaus ja runkopuun poisto';

UPDATE tehtavaryhma SET nimi = 'NURMETUKSET JA MUUT VIHERTYÖT' where nimi = 'Nurmetus';

UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'NURMETUKSET JA MUUT VIHERTYÖT' and tyyppi = 'alataso') WHERE
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Puiden ja pensaiden hoito' and tyyppi = 'alataso') OR
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Nurmi, puut ja pensaat sekä muut virheraluehommat' and tyyppi = 'alataso') OR
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Puut ja pensaat' and tyyppi = 'alataso') ;
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Puiden ja pensaiden hoito', 'Alataso Nurmi, puut ja pensaat sekä muut virheraluehommat', 'Puut ja pensaat', 'Alataso Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt', 'Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt');

UPDATE tehtavaryhma SET nimi = 'KUIVATUSJÄRJESTELMÄT' where nimi = 'Alataso Sade, kaivot ja rummut';

UPDATE tehtavaryhma SET nimi = 'RUMMUT (PÄÄLLYSTETIET)' where nimi = 'Yksityiset rummut (päällystetty tie)';
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'RUMMUT (PÄÄLLYSTETIET)' and tyyppi = 'alataso') WHERE
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Ei-yksityiset rummut (päällystetty tie)' and tyyppi = 'alataso');
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Ei-yksityiset rummut (päällystetty tie)');

UPDATE tehtavaryhma SET nimi = 'RUMMUT (SORATIET)' where nimi = 'Yksityiset rummut (soratie)';
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'RUMMUT (PÄÄLLYSTETIET)' and tyyppi = 'alataso') WHERE
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Ei-yksityiset rummut (soratie)' and tyyppi = 'alataso');
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Ei-yksityiset rummut (soratie)');

UPDATE tehtavaryhma SET nimi = 'KAITEET, AIDAT JA KIVEYKSET' where nimi = 'Alataso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito';

UPDATE toimenpidekoodi SET tehtavaryhma = (select id from tehtavaryhma where nimi = 'LIIKENNEMERKIT JA LIIKENTEENOHJAUSLAITTEET') WHERE nimi = 'Muut päällysteiden paikkaukseen liittyvät työt' AND tehtavaryhma IS NOT NULL;

UPDATE tehtavaryhma SET nimi = 'KUUMAPÄÄLLYSTE' where nimi = 'Alataso Päällysteiden paikkaus';

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO / Päällysteiden paikkaus',	'KYLMÄPÄÄLLYSTE',	(select id from tehtavaryhma where nimi = 'Välitaso Päällysteiden paikkaus'),	'alataso',	103, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'KYLMÄPÄÄLLYSTE' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN(
                                       'Päällysteiden paikkaus -kylmäpäällyste ml. SOP',
                                       'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -kylmäpäällyste ml. SOP',
                                       'Päällysteiden paikkaus, kylmäpäällyste');

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO / Päällysteiden paikkaus',	'PUHALLUS-SIP',	(select id from tehtavaryhma where nimi = 'Välitaso Päällysteiden paikkaus'),	'alataso',	104, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'PUHALLUS-SIP' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN(
                                       'Puhallus-SIP',
                                       'SIP paikkaus (kesto+kylmä)',
                                       'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -puhallus SIP');

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO / Päällysteiden paikkaus',	'SAUMOJEN JUOTTAMINEN BITUMILLA',	(select id from tehtavaryhma where nimi = 'Välitaso Päällysteiden paikkaus'),	'alataso',	105, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'SAUMOJEN JUOTTAMINEN BITUMILLA' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN('Päällysteiden paikkaus -saumojen juottaminen bitumilla');

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO / Päällysteiden paikkaus',	'MASSASAUMAUS',	(select id from tehtavaryhma where nimi = 'Välitaso Päällysteiden paikkaus'),	'alataso',	105, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'MASSASAUMAUS' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN
                               ('Päällysteiden paikkaus - massasaumaus',
                                'Konetiivistetty massasaumaus 10 cm leveä',
                                'Konetiivistetty massasaumaus 20 cm leveä',
                                'Massasaumaus',
                                'Päällysteiden paikkaus - Konetiivistetty massasaumaus 20 cm leveä',
                                'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - massasaumaus');

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO / Päällysteiden paikkaus',	'KT-VALU',	(select id from tehtavaryhma where nimi = 'Välitaso Päällysteiden paikkaus'),	'alataso',	105, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'KT-VALU' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN
                               ('Päällysteiden paikkaus (ml. sillat ja siltapaikat) -konetivistetty valuasvaltti');

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '2.7 LIIKENNEYMPÄRISTÖN HOITO / Päällysteiden paikkaus',	'VALU',	(select id from tehtavaryhma where nimi = 'Välitaso Päällysteiden paikkaus'),	'alataso',	105, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'VALU' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN
                               ('Kuumapäällyste, valuasfaltti',
                                'Päällysteiden paikkaus - valuasfaltti',
                                'Valuasfaltti',
                                'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - valuasvaltti');

UPDATE tehtavaryhma SET nimi = 'SILTAPÄÄLLYSTEET' where nimi = 'Alataso Sillan päällysteen korjaus';

UPDATE tehtavaryhma SET nimi = 'SORAPIENTAREET' where nimi = 'Alataso Päällystettyjen teiden sorapientareen kunnossapito';

UPDATE tehtavaryhma SET nimi = 'SILLAT JA LAITURIT' where nimi = 'Alataso Siltojen ja laitureiden hoito';

UPDATE tehtavaryhma SET nimi = 'SORATEIDEN HOITO' where nimi = 'Alataso Sorateiden pinnan hoito';
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'SORATEIDEN HOITO' and tyyppi = 'alataso') WHERE
    tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Hoitoluokat, kevarit ja kivet sekä muut' and tyyppi = 'alataso');
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Alataso Hoitoluokat, kevarit ja kivet sekä muut', 'Hoitoluokat, kevarit ja kivet sekä muut');

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '3 SORATEIDEN HOITO',	'KESÄSUOLA (MATERIAALI)',	(select id from tehtavaryhma where nimi = 'Välitaso Sorateiden hoito'),	'alataso',	129, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'KESÄSUOLA (MATERIAALI)' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi = 'Sorateiden pölynsidonta';

UPDATE tehtavaryhma SET nimi = 'SORASTUS' where nimi = 'Alataso Muut sorateiden hoidon tehtävät';

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '4 LIIKENTEEN VARMISTAMINEN ERIKOISTILANTEESSA',	'ÄKILLISET HOITOTYÖT (TALVIHOITO)',	(select id from tehtavaryhma where nimi = 'Välitaso Talvihoito'),	'alataso',	135, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '4 LIIKENTEEN VARMISTAMINEN ERIKOISTILANTEESSA',	'ÄKILLISET HOITOTYÖT (LIIKENNEYMPÄRISTÖN HOITO)',	(select id from tehtavaryhma where nimi = 'Välitaso Muut liik.ymp.hoitosasiat'),	'alataso',	135, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '4 LIIKENTEEN VARMISTAMINEN ERIKOISTILANTEESSA',	'ÄKILLISET HOITOTYÖT (SORATIET)',	(select id from tehtavaryhma where nimi = 'Alataso Sorateiden hoito'),	'alataso',	135, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'ÄKILLISET HOITOTYÖT (TALVIHOITO)') WHERE emo = (select id from toimenpidekoodi where koodi = '23104');
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'ÄKILLISET HOITOTYÖT (LIIKENNEYMPÄRISTÖN HOITO)') WHERE emo = (select id from toimenpidekoodi where koodi = '23116');
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'ÄKILLISET HOITOTYÖT (SORATIET)') WHERE emo = (select id from toimenpidekoodi where koodi = '23124');

UPDATE tehtavaryhma SET nimi = 'AVO-OJITUS (PÄÄLLYSTETIET)' where nimi = 'Alataso Ojat (päällystetyt tiet)';

UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'Alataso Ojat (soratiet)' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN ('Kalliokynsien louhinta ojituksen yhteydessä');

UPDATE tehtavaryhma SET nimi = 'AVO-OJITUS (SORATIET)' where nimi IN ('Alataso Ojat (soratiet)');
UPDATE tehtavaryhma set nimi = concat('VOID ' || nimi) where nimi in ('Alataso Ojat');

UPDATE tehtavaryhma SET nimi = 'RKR-KORJAUS' where nimi = 'Alataso MHU Ylläpito';

INSERT into tehtavaryhma (otsikko, nimi, emo, tyyppi, jarjestys, luotu, luoja, nakyva) VALUES ( '6 MUUTA',	'TILAAJAN RAHAVARAUS',	(select id from tehtavaryhma where nimi = 'Välitaso Muut liik.ymp.hoitosasiat'),	'alataso',	159, current_timestamp, (select id from kayttaja where kayttajanimi = 'Integraatio'), FALSE);
UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'TILAAJAN RAHAVARAUS' and tyyppi = 'alataso') WHERE
  tehtavaryhma IS NOT NULL and nimi IN ('Tilaajan rahavaraus lupaukseen 1');

UPDATE tehtavaryhma SET nimi = 'JOHTO- JA HALLINTOKORVAUS' where nimi = 'Alataso Johto- ja hallintokorvaukseen sisältyvät tehtävät';

UPDATE tehtavaryhma SET nimi = 'ERILLISHANKINNAT' where nimi = 'Alataso Erillishankinnat erillishinnoin';

UPDATE toimenpidekoodi set tehtavaryhma = (select id from tehtavaryhma where nimi = 'AVO-OJITUS (SORATIET)') WHERE nimi = 'Kalliokynsien louhinta ojituksen yhteydessä';


-- Päivitä toimenpidekoodien järjestys uuden tehtävä- ja määräluettelon mukaiseksi (MHU)
UPDATE toimenpidekoodi SET jarjestys = 1	WHERE nimi = 'Ise 2-ajorat.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 2	WHERE nimi = 'Ise 1-ajorat.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 3	WHERE nimi = 'Ise ohituskaistat' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 4	WHERE nimi = 'Ise rampit' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 5	WHERE nimi = 'Is 2-ajorat.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 6	WHERE nimi = 'Is 1-ajorat.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 7	WHERE nimi = 'Is ohituskaistat' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 8	WHERE nimi = 'Is rampit' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 9	WHERE nimi = 'Ib 2-ajorat.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 10	WHERE nimi = 'Ib 1-ajorat.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 11	WHERE nimi = 'Ib ohituskaistat' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 12	WHERE nimi = 'Ib rampit' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 13	WHERE nimi = 'Ic 2-ajorat' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 14	WHERE nimi = 'Ic 1-ajorat' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 15	WHERE nimi = 'Ic ohituskaistat' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 16	WHERE nimi = 'Ic rampit' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 17	WHERE nimi = 'II' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 18	WHERE nimi = 'III' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 19	WHERE nimi = 'Kävely- ja pyöräilyväylien laatukäytävät' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 20	WHERE nimi = 'K1' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 21	WHERE nimi = 'K2' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 22	WHERE nimi = 'Levähdys- ja pysäköimisalueet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 23	WHERE nimi = 'Muiden alueiden talvihoito' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 24	WHERE nimi = 'Talvihoidon kohotettu laatu' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 25	WHERE nimi = 'Suolaus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 26	WHERE nimi = 'Suolauksessa on vain yksi tehtävä. XXXXXXXXX' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 27	WHERE nimi = 'Linjahiekoitus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 27	WHERE nimi = 'Pistehiekoitus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 27	WHERE nimi = 'Liukkaudentorjunta hiekoituksella' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 28	WHERE nimi = 'Ennalta arvaamattomien kuljetusten avustaminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 29	WHERE nimi = 'Pysäkkikatosten puhdistus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 30	WHERE nimi = 'Hiekkalaatikoiden täyttö ja hiekkalaatikoiden edustojen lumityöt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 31	WHERE nimi = 'Portaiden talvihoito' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 32	WHERE nimi = 'Lisäkalustovalmius/-käyttö' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 34	WHERE nimi = 'Muut talvihoitotyöt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 35	WHERE nimi = 'Liikennemerkkien ja opasteiden kunnossapito (oikominen, pesu yms.)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 36	WHERE nimi = 'Valvontakameratolppien puhdistus/tarkistus keväisin' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 37	WHERE nimi = 'Reunapaalujen kp (uusien)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 38	WHERE nimi = 'Porttaalien tarkastus ja huolto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 39	WHERE nimi = 'Vakiokokoisten liikennemerkkien uusiminen,  pelkkä merkki' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 40	WHERE nimi = 'Vakiokokoisten liikennemerkkien uusiminen ja lisääminen merkki tukirakenteineen (60 mm varsi)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 41	WHERE nimi = 'Vakiokokoisten liikennemerkkien uusiminen ja lisääminen merkki tukirakenteineen (90 mm varsi)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 42	WHERE nimi = 'Opastustaulujen ja opastusviittojen uusiminen -vanhan viitan/opastetaulun uusiminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 42	WHERE nimi = 'Opastinviitan tai -taulun uusiminen ja lisääminen -ajoradan yläpuoliset opasteet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 42	WHERE nimi = 'Opastustaulun/-viitan uusiminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 43	WHERE nimi = 'Opastustaulujen ja liikennemerkkien rakentaminen tukirakenteineen (sis. liikennemerkkien poistamisia)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 44	WHERE nimi = 'Opastustaulujen ja opastusviittojen uusiminen portaaliin' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 44	WHERE nimi = 'Opastustaulujen ja opastusviittojen uusiminen -porttaalissa olevan viitan/opastetaulun uusiminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 44	WHERE nimi = 'Opastustaulujen ja opastusviittojen uusiminen -portaalissa olevan viitan/opastetaulun uusiminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 47	WHERE nimi = 'Muut liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoitotyöt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 48	WHERE nimi = 'Levähdysalueen puhtaanapito' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 49	WHERE nimi = 'Tie- ja levähdysalueiden kalusteiden kunnossapito ja hoito' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 50	WHERE nimi = 'Pysäkkikatosten siisteydestä huolehtiminen (oikaisu, huoltomaalaus jne.) ja jätehuolto sekä pienet vaurioiden korjaukset' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 51	WHERE nimi = 'Meluesteiden pesu' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 52	WHERE nimi = 'Hiekoitushiekan ja irtoainesten poisto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 53	WHERE nimi = 'Graffitien poisto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 53	WHERE nimi = 'Töherrysten poisto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 54	WHERE nimi = 'Töherrysten estokäsittely' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 55	WHERE nimi = 'Katupölynsidonta' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 57	WHERE nimi = 'Muut tie- levähdys- ja liitännäisalueiden puhtaanpitoon ja kalusteiden hoitoon liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 63	WHERE nimi = 'Vesakonraivaus N2' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 64	WHERE nimi = 'Vesakonraivaus N3' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 65	WHERE nimi = 'Runkopuiden poisto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 66	WHERE nimi = 'Nurmetuksen hoito / niitto N1' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 67	WHERE nimi = 'Nurmetuksen hoito / niitto N2' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 68	WHERE nimi = 'Nurmetuksen hoito / niitto N3' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 69	WHERE nimi = 'Nurmetuksen hoito / niitto T1/E1' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 70	WHERE nimi = 'Nurmetuksen hoito / niitto T2/E2' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 71	WHERE nimi = 'Puiden ja pensaiden hoito T1/E1' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 72	WHERE nimi = 'Puiden ja pensaiden hoito T2/E2/N1' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 73	WHERE nimi = 'Erillisten hoito-ohjeiden mukaiset vihertyöt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 74	WHERE nimi = 'Erillisten hoito-ohjeiden mukaiset vihertyöt, uudet alueet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 75	WHERE nimi = 'Vesistöpenkereiden hoito' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 76	WHERE nimi = 'Tiekohtaiset maisemanhoitoprojektit' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 78	WHERE nimi = 'Muut viheralueiden hoitoon liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 79	WHERE nimi = 'PUUTTUU' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 80	WHERE nimi = 'Kaivojen ja putkistojen tarkastus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 81	WHERE nimi = 'Kaivojen ja putkistojen sulatus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 82	WHERE nimi = 'Kuivatusjärjestelmän pumppaamoiden hoito ja tarkkailu' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 84	WHERE nimi = 'Rumpujen sulatus, aukaisu ja toiminnan varmistaminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 85	WHERE nimi = 'Rumpujen tarkastus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 86	WHERE nimi = 'Yksityisten rumpujen korjaus ja uusiminen  Ø ≤ 400 mm, päällystetyt tiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 87	WHERE nimi = 'Yksityisten rumpujen korjaus ja uusiminen  Ø > 400 mm ≤ 600 mm, päällystetyt tiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 88	WHERE nimi = 'Päällystetyn tien rumpujen korjaus ja uusiminen Ø <= 600 mm' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 89	WHERE nimi = 'Päällystetyn tien rumpujen korjaus ja uusiminen  Ø> 600  <= 800 mm' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 90	WHERE nimi = 'Yksityisten rumpujen korjaus ja uusiminen  Ø ≤ 400 mm, soratiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 91	WHERE nimi = 'Yksityisten rumpujen korjaus ja uusiminen  Ø > 400 mm ≤ 600 mm, soratiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 92	WHERE nimi = 'Soratien rumpujen korjaus ja uusiminen  Ø <= 600 mm' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 93	WHERE nimi = 'Soratien rumpujen korjaus ja uusiminen  Ø> 600  <=800 mm' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 96	WHERE nimi = 'Muut rumpujen kunnossapitoon liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 97	WHERE nimi = 'Kaiteiden ja aitojen tarkastaminen ja vaurioiden korjaukset' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 98	WHERE nimi = 'Reunakivivaurioiden korjaukset' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 101	WHERE nimi = 'Muut kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapitoon liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 102	WHERE nimi = 'Kuumapäällyste, ab käsityönä' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 102	WHERE nimi = 'Kuumapäällyste' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 102	WHERE nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - kuumapäällyste' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 103	WHERE nimi = 'Päällysteiden paikkaus -kylmäpäällyste ml. SOP' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 103	WHERE nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -kylmäpäällyste ml. SOP' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 103	WHERE nimi = 'Päällysteiden paikkaus, kylmäpäällyste' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 104	WHERE nimi = 'Puhallus-SIP' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 104	WHERE nimi = 'SIP paikkaus (kesto+kylmä)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 104	WHERE nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -puhallus SIP' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 105	WHERE nimi = 'Päällysteiden paikkaus -saumojen juottaminen bitumilla' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 106	WHERE nimi = 'Päällysteiden paikkaus - massasaumaus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 106	WHERE nimi = 'Konetiivistetty massasaumaus 10 cm leveä' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 106	WHERE nimi = 'Konetiivistetty massasaumaus 20 cm leveä' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 106	WHERE nimi = 'Massasaumaus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 106	WHERE nimi = 'Päällysteiden paikkaus - Konetiivistetty massasaumaus 20 cm leveä' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 106	WHERE nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - massasaumaus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 107	WHERE nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) -konetivistetty valuasvaltti' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 108	WHERE nimi = 'Kuumapäällyste, valuasfaltti' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 108	WHERE nimi = 'Päällysteiden paikkaus - valuasfaltti' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 108	WHERE nimi = 'Valuasfaltti' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 108	WHERE nimi = 'Päällysteiden paikkaus (ml. sillat ja siltapaikat) - valuasvaltti' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 109	WHERE nimi = 'Sillan päällysteen halkeaman avarrussaumaus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 110	WHERE nimi = 'Sillan kannen päällysteen päätysauman korjaukset' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 111	WHERE nimi = 'Reunapalkin ja päällysteen väl. sauman tiivistäminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 112	WHERE nimi = 'Reunapalkin liikuntasauman tiivistäminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 115	WHERE nimi = 'Muut päällysteiden paikkaukseen liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 116	WHERE nimi = 'Päällystettyjen teiden sr-pientareen täyttö' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 116	WHERE nimi = 'Päällystettyjen teiden pientareiden täyttö' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 116	WHERE nimi = 'Reunantäyttö' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 116	WHERE nimi = 'Päällystettyjen teiden sorapientareen täyttö' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 117	WHERE nimi = 'Päällystettyjen teiden palteiden poisto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 118	WHERE nimi = 'Reunapalteen poisto kaiteen alta' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 120	WHERE nimi = 'Muut päällystettyjen teiden sorapientareiden kunnossapitoon liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 121	WHERE nimi = 'Siltojen hoito (kevätpuhdistus, puhtaanapito, kasvuston poisto ja pienet kunnostustoimet sekä vuositarkastukset)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 122	WHERE nimi = 'Laitureiden hoito (puhtaanapito, pienet kunnostustoimet, turvavarusteiden kunnon varmistaminen sekä vuositarkastukset)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 124	WHERE nimi = 'Muut siltojen ja laitureiden hoitoon liittyvät työt' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 124	WHERE nimi = 'Sorateiden hoito, hoitoluokka I' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 125	WHERE nimi = 'Sorateiden pinnan hoito, hoitoluokka II' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 126	WHERE nimi = 'Sorateiden pinnan hoito, hoitoluokka III' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 127	WHERE nimi = 'Sorapintaisten kävely- ja pyöräilyväylienhoito' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 128	WHERE nimi = 'Maakivien (>1m3) poisto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 129	WHERE nimi = 'Sorateiden pölynsidonta' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 130	WHERE nimi = 'Sorastus' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 130	WHERE nimi = 'Sorastus km' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 131	WHERE nimi = 'Oja- ja luiskameteriaalin käyttö kulutuskerrokseeen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 132	WHERE nimi = 'Liikenteen varmistaminen kelirikkokohteissa' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 134	WHERE nimi = 'Muut sorateiden hoitoon liittyvät tehtävät' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 135	WHERE nimi = 'Äkillinen hoitotyö' AND tehtavaryhma IS NOT NULL; -- 3 riviä
UPDATE toimenpidekoodi SET jarjestys = 137	WHERE nimi = 'Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 138	WHERE nimi = 'Kolmansien osapuolten aiheuttamien vahinkojen korjaaminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 139	WHERE nimi = 'Avo-ojitus/päällystetyt tiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 140	WHERE nimi = 'Avo-ojitus/päällystetyt tiet (kaapeli kaivualueella)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 141	WHERE nimi = 'Laskuojat/päällystetyt tiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 142	WHERE nimi = 'Avo-ojitus/soratiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 143	WHERE nimi = 'Avo-ojitus/soratiet (kaapeli kaivualueella)' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 144	WHERE nimi = 'Laskuojat/soratiet' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 145	WHERE nimi = 'Kalliokynsien louhinta ojituksen yhteydessä' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 146	WHERE nimi = 'Soratien runkokelirikkokorjaukset' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 152	WHERE nimi = 'Pohjavesisuojaukset' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 155	WHERE nimi = 'Nopeusnäyttötaulun hankinta' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 156	WHERE nimi = 'Nopeusnäyttötaulujen ylläpito, käyttö ja siirto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 157	WHERE nimi = 'Pysäkkikatoksen uusiminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 158	WHERE nimi = 'Pysäkkikatoksen poistaminen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 159	WHERE nimi = 'Tilaajan rahavaraus lupaukseen 1' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 161	WHERE nimi = 'Hoitourakan työnjohto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 162	WHERE nimi = 'Toimistotarvike- ja ICT-kulut, tiedotus, opastus, kokousten järjestäminen jne.' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 163	WHERE nimi = 'Hoito- ja korjaustöiden pientarvikevarasto' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 164	WHERE nimi = 'Osallistuminen tilaajalle kuuluvien viranomaistehtävien hoitoon' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 165	WHERE nimi = 'Toimitilat sähkö-, lämmitys-, vesi-, jäte-, siivous-, huolto-, korjaus- ja vakuutus- yms. kuluineen' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 166	WHERE nimi = 'Hoitourakan tarvitsemat kelikeskus- ja keliennustepalvelut' AND tehtavaryhma IS NOT NULL;
UPDATE toimenpidekoodi SET jarjestys = 167	WHERE nimi = 'Seurantajärjestelmät (mm. ajantasainen seuranta, suolan automaattinen seuranta)' AND tehtavaryhma IS NOT NULL;



UPDATE tehtavaryhma SET jarjestys = 1 WHERE nimi = 'Talvihoito';
UPDATE tehtavaryhma SET jarjestys = 1 WHERE nimi = 'Välitaso Talvihoito';
UPDATE tehtavaryhma SET jarjestys = 25 WHERE nimi = 'Liukkaudentorjunta';
UPDATE tehtavaryhma SET jarjestys = 1 WHERE nimi = 'TALVIHOITO';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Muut talvihoitotyöt';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Muut talvihoitotyöt';
UPDATE tehtavaryhma SET jarjestys = 25 WHERE nimi = 'TALVISUOLA';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Alataso Sillat';
UPDATE tehtavaryhma SET jarjestys = 124 WHERE nimi = 'Sorateiden hoito';
UPDATE tehtavaryhma SET jarjestys = 129 WHERE nimi = 'Välitaso Sorateiden hoito';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Hoitoluokat, kevarit ja kivet sekä muut';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Sorastus, luiska ja varmistus';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Alataso Sorastus, luiska ja varmistus';
UPDATE tehtavaryhma SET jarjestys = 139 WHERE nimi = 'MHU Ylläpito';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Hoitoluokat, kevarit ja kivet sekä muut';
UPDATE tehtavaryhma SET jarjestys = 135 WHERE nimi = 'ÄKILLISET HOITOTYÖT (SORATIET)';
UPDATE tehtavaryhma SET jarjestys = 146 WHERE nimi = 'Välitaso MHU Ylläpito';
UPDATE tehtavaryhma SET jarjestys = 139 WHERE nimi = 'Ojat';
UPDATE tehtavaryhma SET jarjestys = 152 WHERE nimi = 'Pohjavesisuojaukset';
UPDATE tehtavaryhma SET jarjestys = 152 WHERE nimi = 'Välitaso Pohjavesisuojaukset';
UPDATE tehtavaryhma SET jarjestys = 152 WHERE nimi = 'Alataso Pohjavesisuojaukset';
UPDATE tehtavaryhma SET jarjestys = 159 WHERE nimi = 'Muut liik.ymp.hoitosasiat';
UPDATE tehtavaryhma SET jarjestys = 159 WHERE nimi = 'Välitaso Muut liik.ymp.hoitosasiat';
UPDATE tehtavaryhma SET jarjestys = 155 WHERE nimi = 'Alataso Muut liik.ymp.hoitosasiat';
UPDATE tehtavaryhma SET jarjestys = 161 WHERE nimi = 'Johto- ja hallintokorvaukseen sisältyvät tehtävät';
UPDATE tehtavaryhma SET jarjestys = 161 WHERE nimi = 'Välitaso Johto- ja hallintokorvaukseen sisältyvät tehtävät';
UPDATE tehtavaryhma SET jarjestys = 165 WHERE nimi = 'Erillishankinnat erillishinnoin';
UPDATE tehtavaryhma SET jarjestys = 139 WHERE nimi = 'AVO-OJITUS (PÄÄLLYSTETIET)';
UPDATE tehtavaryhma SET jarjestys = 142 WHERE nimi = 'AVO-OJITUS (SORATIET)';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Ojat';
UPDATE tehtavaryhma SET jarjestys = 146 WHERE nimi = 'RKR-KORJAUS';
UPDATE tehtavaryhma SET jarjestys = 161 WHERE nimi = 'JOHTO- JA HALLINTOKORVAUS';
UPDATE tehtavaryhma SET jarjestys = 165 WHERE nimi = 'Välitaso Erillishankinnat erillishinnoin';
UPDATE tehtavaryhma SET jarjestys = 124 WHERE nimi = 'Sorateiden pinnan hoito';
UPDATE tehtavaryhma SET jarjestys = 124 WHERE nimi = 'SORATEIDEN HOITO';
UPDATE tehtavaryhma SET jarjestys = 165 WHERE nimi = 'ERILLISHANKINNAT';
UPDATE tehtavaryhma SET jarjestys = 130 WHERE nimi = 'Muut sorateiden hoidon tehtävät';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Ojitus';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Alataso Ojitus';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Alataso Ojitus (päällystetyt tiet)';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Alataso Ojitus (soratiet)';
UPDATE tehtavaryhma SET jarjestys = 35 WHERE nimi = 'Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen';
UPDATE tehtavaryhma SET jarjestys = 35 WHERE nimi = 'Välitaso Liikennemerkkien, liikenteen ohjauslaitteiden ja reunapaalujen hoito sekä uusiminen';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Vakiokokoiset liikennemerkit';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Opastustaulut ja viitat';
UPDATE tehtavaryhma SET jarjestys = 48 WHERE nimi = 'Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito';
UPDATE tehtavaryhma SET jarjestys = 48 WHERE nimi = 'Välitaso Tie-, levähdys- ja liitännäisalueiden puhtaanapito ja kalusteiden hoito';
UPDATE tehtavaryhma SET jarjestys = 62 WHERE nimi = 'Viheralueiden hoito';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Vesakko ja runkopuu';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Alataso Vesakko ja runkopuu';
UPDATE tehtavaryhma SET jarjestys = 66 WHERE nimi = 'Nurmi, puut ja pensaat sekä muut virheraluehommat';
UPDATE tehtavaryhma SET jarjestys = 62 WHERE nimi = 'Vesakon raivaus ja runkopuun poisto';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Nurmetus, puiden ja pensaiden hoito, muut viheralueiden hoidon työt';
UPDATE tehtavaryhma SET jarjestys = 79 WHERE nimi = 'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito';
UPDATE tehtavaryhma SET jarjestys = 79 WHERE nimi = 'Sade, kaivot ja rummut';
UPDATE tehtavaryhma SET jarjestys = 666666 WHERE nimi = 'Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito';
UPDATE tehtavaryhma SET jarjestys = 666666 WHERE nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito';
UPDATE tehtavaryhma SET jarjestys = 84 WHERE nimi = 'Alataso Sade, kaivot ja rummut (rumpujen kunnossapito)'; -- KUIVATUSJÄRJESTELMÄT ALALAHKU
UPDATE tehtavaryhma SET jarjestys = 86 WHERE nimi = 'Rumpujen kunnossapito ja uusiminen';
UPDATE tehtavaryhma SET jarjestys = 86 WHERE nimi = 'Rumpujen kunnossapito ja uusiminen (päällystetty tie)';
UPDATE tehtavaryhma SET jarjestys = 90 WHERE nimi = 'Rumpujen kunnossapito ja uusiminen (soratie)';
UPDATE tehtavaryhma SET jarjestys = 35 WHERE nimi = 'LIIKENNEMERKIT JA LIIKENTEENOHJAUSLAITTEET';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Vakiokokoiset liikennemerkit';
UPDATE tehtavaryhma SET jarjestys = 48 WHERE nimi = 'PUHTAANAPITO';
UPDATE tehtavaryhma SET jarjestys = 62 WHERE nimi = 'VESAKONRAIVAUKSET JA PUUN POISTO';
UPDATE tehtavaryhma SET jarjestys = 66 WHERE nimi = 'NURMETUKSET JA MUUT VIHERTYÖT';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Nurmi, puut ja pensaat sekä muut virheraluehommat';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Puut ja pensaat';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Puiden ja pensaiden hoito';
UPDATE tehtavaryhma SET jarjestys = 79 WHERE nimi = 'KUIVATUSJÄRJESTELMÄT';
UPDATE tehtavaryhma SET jarjestys = 86 WHERE nimi = 'RUMMUT (PÄÄLLYSTETIET)';
UPDATE tehtavaryhma SET jarjestys = 1000  WHERE nimi = 'VOID Ei-yksityiset rummut (päällystetty tie)';
UPDATE tehtavaryhma SET jarjestys = 90 WHERE nimi = 'RUMMUT (SORATIET)';
UPDATE tehtavaryhma SET jarjestys = 10000 WHERE nimi = 'VOID Ei-yksityiset rummut (soratie)';
UPDATE tehtavaryhma SET jarjestys = 120 WHERE nimi = 'SORASTUS';
UPDATE tehtavaryhma SET jarjestys = 79 WHERE nimi = 'Alataso Kuivatusjärjestelmän kaivojen, putkistojen ja pumppaamoiden hoito, rumpujen tarkastus ja hoito (rumpujen kunnossapito)';
UPDATE tehtavaryhma SET jarjestys = 97 WHERE nimi = 'Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito';
UPDATE tehtavaryhma SET jarjestys = 97 WHERE nimi = 'Välitaso Kaiteiden, riista- ja suoja-aitojen sekä kiveysten kunnossapito';
UPDATE tehtavaryhma SET jarjestys = 102 WHERE nimi = 'Päällysteiden paikkaus';
UPDATE tehtavaryhma SET jarjestys = 102 WHERE nimi = 'Välitaso Päällysteiden paikkaus';
UPDATE tehtavaryhma SET jarjestys = 666 WHERE nimi = 'Sillat'; ---
UPDATE tehtavaryhma SET jarjestys = 109 WHERE nimi = 'Sillan päällysteen korjaus';
UPDATE tehtavaryhma SET jarjestys = 116 WHERE nimi = 'Päällystettyjen teiden sorapientareen kunnossapito';
UPDATE tehtavaryhma SET jarjestys = 116 WHERE nimi = 'Välitaso Päällystettyjen teiden sorapientareen kunnossapito';
UPDATE tehtavaryhma SET jarjestys = 121 WHERE nimi = 'Siltojen ja laitureiden hoito';
UPDATE tehtavaryhma SET jarjestys = 121 WHERE nimi = 'Välitaso Siltojen ja laitureiden hoito';
UPDATE tehtavaryhma SET jarjestys = 26 WHERE nimi = 'KFO,NAFO';
UPDATE tehtavaryhma SET jarjestys = 27 WHERE nimi = 'HIEKOITUS';
UPDATE tehtavaryhma SET jarjestys = 1000 WHERE nimi = 'VOID Alataso Opastustaulut ja viitat';
UPDATE tehtavaryhma SET jarjestys = 97 WHERE nimi = 'KAITEET, AIDAT JA KIVEYKSET';
UPDATE tehtavaryhma SET jarjestys = 102 WHERE nimi = 'KUUMAPÄÄLLYSTE';
UPDATE tehtavaryhma SET jarjestys = 103 WHERE nimi = 'KYLMÄPÄÄLLYSTE';
UPDATE tehtavaryhma SET jarjestys = 104 WHERE nimi = 'PUHALLUS-SIP';
UPDATE tehtavaryhma SET jarjestys = 105 WHERE nimi = 'SAUMOJEN JUOTTAMINEN BITUMILLA';
UPDATE tehtavaryhma SET jarjestys = 106 WHERE nimi = 'MASSASAUMAUS';
UPDATE tehtavaryhma SET jarjestys = 107 WHERE nimi = 'KT-VALU';
UPDATE tehtavaryhma SET jarjestys = 108 WHERE nimi = 'VALU';
UPDATE tehtavaryhma SET jarjestys = 109 WHERE nimi = 'SILTAPÄÄLLYSTEET';
UPDATE tehtavaryhma SET jarjestys = 116 WHERE nimi = 'SORAPIENTAREET';
UPDATE tehtavaryhma SET jarjestys = 121 WHERE nimi = 'SILLAT JA LAITURIT';
UPDATE tehtavaryhma SET jarjestys = 129 WHERE nimi = 'KESÄSUOLA (MATERIAALI)';
UPDATE tehtavaryhma SET jarjestys = 159 WHERE nimi = 'TILAAJAN RAHAVARAUS';




