
-- Luodaan Liikennevirasto
INSERT INTO organisaatio (tyyppi, nimi, lyhenne, ytunnus) VALUES ('liikennevirasto','Liikennevirasto','Livi', '1010547-1');

-- Luodaan hallintayksikot (ELY-keskukset)
\i testidata/elyt.sql

-- Urakoitsijoita, hoidon alueurakat
INSERT INTO organisaatio (tyyppi, ytunnus, nimi, katuosoite, postinumero, sampoid) VALUES ('urakoitsija', '1565583-5', 'YIT Rakennus Oy', 'Panuntie 11, PL 36', 00621, 'YITR-424342');
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '1765515-0', 'NCC Roads Oy');
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '2163026-3', 'Destia Oy');
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '0171337-9', 'Savon Kuljetus Oy');
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '2050797-6', 'TSE-Tienvieri Oy');
INSERT INTO organisaatio (tyyppi, ytunnus, nimi, sampoid) VALUES ('urakoitsija', '2138243-1', 'Lemminkäinen Infra Oy','TESTIURAKOITSIJA');
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '08851029', 'Carement Oy');

-- Urakoitsijoita, päällystys
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '0651792-4', 'Skanska Asfaltti Oy');

-- Urakoitsijoita, tiemerkintä
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '1234567-8', 'Tien Merkitsijät Oy');

-- Urakoitsijoita, valaistus
INSERT INTO organisaatio (tyyppi, ytunnus, nimi) VALUES ('urakoitsija', '2234567-8', 'Lampunvaihtajat Oy');

-- Urakoitsijoita, testi
INSERT INTO organisaatio (tyyppi, ytunnus, nimi, sampoid) VALUES ('urakoitsija', '6458856-1', 'Testi Oy', 'TESTIORGANISAATI');

-- Luodaan hoidon alueurakoita ja ylläpitourakoita
\i testidata/urakat.sql

-- Luodaan sopimuksia urakoille, kaikilla urakoilla on oltava ainakin yksi sopimus
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Oulun alueurakka pääsopimus', '2005-10-01','2010-09-30','1H05228/01', (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Oulun alueurakka pääsopimus','2014-10-01','2019-09-30','2H16339/01', (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka, paasopimus) VALUES ('Oulun alueurakka lisäsopimus', '2005-10-01','2010-09-30','2H05228/10', (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE sampoid='1H05228/01'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Pudasjärvi pääsopimus', '2007-10-01','2012-09-30','3H05228/40', (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Porin pääsopimus', '2007-10-01','2012-09-30','4H05111/22', (SELECT id FROM urakka WHERE nimi='Porin alueurakka 2007-2012'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Muhoksen pääsopimus', '2007-06-01','2012-09-30','5H05228/10', (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Muhoksen pääsopimus', '2007-06-01','2012-09-30','5H05229/10', (SELECT id FROM urakka WHERE nimi='Muhoksen paikkausurakka'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Porintien pääsopimus', '2007-05-01','2007-08-22','8605228/10', (SELECT id FROM urakka WHERE nimi='Porintien päällystysurakka'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Oulun tiemerkinnän palvelusopimuksen pääsopimus 2013-2018', '2013-01-01','2018-12-31','7H05228/10', (SELECT id FROM urakka WHERE nimi='Oulun tiemerkinnän palvelusopimus 2013-2018'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Oulun valaistuksen palvelusopimuksen pääsopimus 2013-2018', '2013-01-01','2018-12-31','5A05228/10', (SELECT id FROM urakka WHERE nimi='Oulun valaistuksen palvelusopimus 2013-2018'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Pirkanmaan tiemerkinnän palvelusopimuksen pääsopimus 2013-2018', '2013-01-01','2018-12-31','2A05228/10', (SELECT id FROM urakka WHERE nimi='Pirkanmaan tiemerkinnän palvelusopimus 2013-2018'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Lapin tiemerkinnän palvelusopimuksen pääsopimus 2013-2018', '2013-01-01','2018-12-31','2A06228/10', (SELECT id FROM urakka WHERE nimi='Lapin tiemerkinnän palvelusopimus 2013-2018'));
INSERT INTO sopimus (nimi, alkupvm, loppupvm, sampoid, urakka) VALUES ('Kempeleen valaistuksen pääsopimus', '2007-03-01','2012-05-30','9H05224/01', (SELECT id FROM urakka WHERE nimi='Kempeleen valaistusurakka'));

-- Luodaan toimenpidekoodit
\i testidata/toimenpidekoodit.sql

-- Luodaan sanktiotyypit
\i testidata/sanktiot.sql

-- Testikäyttäjiä
INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin, organisaatio) VALUES ('tero','Tero','Toripolliisi','tero.toripolliisi@example.com','0405127232', (SELECT id FROM organisaatio WHERE lyhenne='POP'));

INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin, organisaatio) VALUES ('jvh','Jalmari','Järjestelmävastuuhenkilö','jalmari@example.com', '040123456789', (SELECT id FROM organisaatio WHERE lyhenne='Livi'));
INSERT INTO kayttaja_rooli (kayttaja, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='jvh'), 'jarjestelmavastuuhenkilo');

INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio) VALUES ('antero','Antero','Asfalttimies','antero@example.com','0401111111', (SELECT id FROM organisaatio WHERE nimi='Destia Oy'));

INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio) values ('ulle', 'Ulle', 'Urakoitsija', 'ulle@example.org', 123123123, (SELECT id FROM organisaatio WHERE nimi='Destia Oy'));
INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio) values ('yit_pk','Yitin', 'Pääkäyttäjä', 'yit_pk@example.org', 43223123, (SELECT id FROM organisaatio WHERE nimi='YIT Rakennus Oy'));
INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio) values ('yit_pk2','Uuno', 'Urakoitsija', 'yit_pk2@example.org', 43223123, (SELECT id FROM organisaatio WHERE lyhenne='POP'));
INSERT INTO kayttaja_rooli (kayttaja, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='tero'), 'urakanvalvoja');
INSERT INTO kayttaja_rooli (kayttaja, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_pk2'), 'urakoitsijan paakayttaja');
INSERT INTO kayttaja_rooli (kayttaja, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_pk'), 'urakoitsijan paakayttaja');
INSERT INTO kayttaja_urakka_rooli (kayttaja, urakka, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_pk'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 'urakoitsijan paakayttaja');
INSERT INTO kayttaja_urakka_rooli (kayttaja, urakka, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_pk'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'urakoitsijan paakayttaja');
INSERT INTO kayttaja_urakka_rooli (kayttaja, urakka, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='tero'), (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'urakanvalvoja');
INSERT INTO kayttaja_urakka_rooli (kayttaja, urakka, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_pk2'), (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), 'urakoitsijan paakayttaja');

INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio) values ('yit_uuvh','Yitin', 'Urakkavastaava', 'yit_uuvh@example.org', 43363123, (SELECT id FROM organisaatio WHERE nimi='YIT Rakennus Oy'));
INSERT INTO kayttaja_rooli (kayttaja, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_uuvh'), 'urakoitsijan urakan vastuuhenkilo');
INSERT INTO kayttaja_urakka_rooli (kayttaja, urakka, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_uuvh'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 'urakoitsijan urakan vastuuhenkilo');
INSERT INTO kayttaja_urakka_rooli (kayttaja, urakka, rooli) VALUES ((SELECT id FROM kayttaja WHERE kayttajanimi='yit_uuvh'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'urakoitsijan urakan vastuuhenkilo');
INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio, jarjestelma) values ('fastroi', null, null, null, null, (SELECT id FROM organisaatio WHERE nimi='Destia Oy'), true);
INSERT INTO kayttaja (kayttajanimi,etunimi,sukunimi,sahkoposti,puhelin,organisaatio, jarjestelma) values ('yit-rakennus', null, null, null, null, (SELECT id  FROM organisaatio  WHERE nimi = 'YIT Rakennus Oy'), true);

-- Luodaan yhteyshenkilöpooliin henkilöitä
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jouko','Kasslin','JoukoKasslin@gustr.com','046 248 7808','Domed1942');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Marcus','Kilpeläinen','MarcusKilpelainen@einrot.com','044 416 9420','Andecone');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Taimi','Mallat','TaimiMallat@gustr.com','046 854 6654','Whapin');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Anna-Liisa','Harju','Anna-LiisaHarju@superrito.com','041 275 3698','Flar1949');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Tuomas','Vesisaari','TuomasVesisaari@gustr.com','042 864 3468','Sawite');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Eemeli','Perunka','EemeliPerunka@cuvox.de','046 176 5938','Betteramer');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Aija','Lindman','AijaLindman@rhyta.com','041 625 9992','Muchis');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Lassi','Repo','LassiRepo@armyspy.com','042 500 7369','Wifyin');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Armas','Virta','ArmasVirta@superrito.com','046 285 0546','Yoursou');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Akseli','Jauho','AkseliJauho@dayrep.com','046 060 7355','Tarm1975');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Virpi','Rissanen','VirpiRissanen@jourrapide.com','040 285 2285','Achat1950');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Maunu','Kyllönen','MaunuKyllonen@einrot.com','041 048 8974','Therequisels');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jani','Laatikainen','JaniLaatikainen@gustr.com','050 318 3084','Whearclas');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Oskar','Harju','OskarHarju@einrot.com','040 543 0883','Theithe');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Ilse','Kovanen','IlseKovanen@rhyta.com','044 113 8378','Hisolinsts');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Reijo','Olamo','ReijoOlamo@einrot.com','046 202 7285','Thavite');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Maarit','Holkeri','MaaritHolkeri@teleworm.us','041 653 6307','Agartherm');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Veli-Pekka','Petäjä','Veli-PekkaPetaja@teleworm.us','041 564 7682','Brated1964');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Manu','Möttölä','ManuMottola@gustr.com','044 599 7237','Prewn1982');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Seija','Männikkö','SeijaMannikko@superrito.com','040 109 7087','Begivaing90');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Reijo','Liukko','ReijoLiukko@teleworm.us','041 765 1868','Oldisher');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Iines','Romppanen','IinesRomppanen@armyspy.com','044 216 5351','Sarlizies');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jean','Lakanen','JeanLakanen@teleworm.us','044 629 6721','Weavent61');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Tuula','Mattila','TuulaMattila@fleckens.hu','042 023 5901','Emberought');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Eeva','Haapalainen','EevaHaapalainen@dayrep.com','050 397 4171','Thenecolasty');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Markku','Laiho','MarkkuLaiho@rhyta.com','044 109 4847','Argoor');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Salla','Pekkanen','SallaPekkanen@cuvox.de','040 248 3646','Alcull');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Minna','Tuulola','MinnaTuulola@rhyta.com','041 075 6043','Hingall');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Elisa','Varis','ElisaVaris@fleckens.hu','042 152 2474','Phatted');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Meeri','Takko','MeeriTakko@cuvox.de','040 823 0724','Aunder');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Salla','Suominen','SallaSuominen@armyspy.com','050 556 7471','Shink1976');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Pontus','Pallasmaa','PontusPallasmaa@einrot.com','041 463 9814','Foremary');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Sanna-Leen','Kantola','Sanna-LeenKantola@superrito.com','040 449 8739','Befulaust');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Merja','Kiiskinen','MerjaKiiskinen@fleckens.hu','042 802 3382','Allizenalice');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Mattiesko','Järventaus','MattieskoJarventaus@jourrapide.com','044 557 4701','Fambireett1989');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Atso','Kottila','AtsoKottila@cuvox.de','044 451 3518','Oncents');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Salla','Seikola','SallaSeikola@fleckens.hu','041 670 2222','Inglan');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Onni','Niinimaa','OnniNiinimaa@teleworm.us','046 779 4752','Whis1995');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Lassi','Uusipaikka','LassiUusipaikka@dayrep.com','044 728 3849','Mingh1993');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Eija','Okkonen','EijaOkkonen@jourrapide.com','042 314 1543','Yazzle');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Reijo','Oramo','ReijoOramo@armyspy.com','041 776 9931','Hentle');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Lalli','Eriksson','LalliEriksson@armyspy.com','044 701 2342','Buttly');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jukka','Linnasalo','JukkaLinnasalo@superrito.com','046 303 1577','Uning1961');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Meeri','Soini','MeeriSoini@jourrapide.com','050 629 3177','Palwas');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Robin','Kent','RobinKent@jourrapide.com','050 538 3869','Andued1966');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Anssi','Sukki','AnssiSukki@superrito.com','044 153 9141','Jused1958');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Marcus','Ikonen','MarcusIkonen@dayrep.com','044 853 9404','Miltrared');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Laura','Hyvönen','LauraHyvonen@teleworm.us','040 791 3393','Supolnester50');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Harry','Ojala','HarryOjala@fleckens.hu','042 249 7133','Covic1982');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Marcus','Uusipaikka','MarcusUusipaikka@teleworm.us','042 529 3135','Bery1983');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Elias','Tuulola','EliasTuulola@einrot.com','046 404 0939','Cambee');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Eemeli','Kuismanen','EemeliKuismanen@cuvox.de','042 038 9602','Precand1952');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jonatan','Hanski','JonatanHanski@armyspy.com','042 209 1696','Camenly45');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Maija','Johanson','MaijaJohanson@gustr.com','050 400 0399','Throking');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Roosa','Numminen','RoosaNumminen@armyspy.com','041 226 3326','Plat1941');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Armi','Varvio','ArmiVarvio@gustr.com','046 274 1784','Prideaught');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Anni','Lilja','AnniLilja@armyspy.com','040 270 5519','Mortherat1944');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Tom','Lindman','TomLindman@jourrapide.com','040 523 7094','Aticeyound');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Suoma','Koivuniemi','SuomaKoivuniemi@rhyta.com','046 826 5981','Andifteek');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Helvi','Tuikka','HelviTuikka@gustr.com','041 390 7975','Hentireacted');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Essi','Ruoho','EssiRuoho@einrot.com','050 575 8010','Wherruccops1978');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Veijo','Manninen','VeijoManninen@jourrapide.com','046 134 2875','Therroys');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Tuomo','Peltola','TuomoPeltola@teleworm.us','050 803 9928','Hatian1964');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Ilkka','Penttilä','IlkkaPenttila@einrot.com','040 330 4515','Spither33');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jaakko','Kangas','JaakkoKangas@cuvox.de','040 304 7081','Eity1966');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Mattiesko','Kuitunen','MattieskoKuitunen@gustr.com','041 449 5308','Colestook1948');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Nea','Kauppinen','NeaKauppinen@superrito.com','044 642 4542','Frouleem34');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Raija','Wuori','RaijaWuori@dayrep.com','040 438 5255','Nongthe');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Joel','Larivaara','JoelLarivaara@superrito.com','040 228 0834','Vourpontow');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jesse','Petäjä','JessePetaja@gustr.com','042 074 0530','Hismandent93');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Raino','Haataja','RainoHaataja@rhyta.com','040 860 2860','Mersed');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Toni','Keskitalo','ToniKeskitalo@jourrapide.com','041 357 2334','Hasters');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jyri','Grönholm','JyriGronholm@armyspy.com','046 127 9052','Whers1994');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Annukka','Kankkunen','AnnukkaKankkunen@fleckens.hu','050 082 5715','Casigh');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jouko','Muukkonen','JoukoMuukkonen@superrito.com','050 272 1563','Appermak');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Kristiina','Suutari-Jääskö','KristiinaSuutari-Jaasko@fleckens.hu','044 774 3961','Cattat');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Raija','Weckström','RaijaWeckstrom@fleckens.hu','046 794 0935','Youltaides53');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Henriikka','Matinsalo','HenriikkaMatinsalo@jourrapide.com','041 304 1165','Yountered');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Riia','Mönkkönen','RiiaMonkkonen@teleworm.us','046 600 3377','Tarromend1949');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Henriikka','Sasi','HenriikkaSasi@einrot.com','044 356 8030','Peaced');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Kalevi','Vesisaari','KaleviVesisaari@jourrapide.com','044 368 8439','Thopherch');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Harri','Saarijärvi','HarriSaarijarvi@rhyta.com','040 656 5674','Mispeas');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Kauko','Aunola','KaukoAunola@cuvox.de','050 159 8340','Arrierld81');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Paula','Sasi','PaulaSasi@einrot.com','046 371 8878','Hicieven');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Reija','Liukko','ReijaLiukko@gustr.com','042 082 7150','Gifforn');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Terttu','Niva','TerttuNiva@armyspy.com','044 026 5844','Prelf1950');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jere','Hurme','JereHurme@gustr.com','040 354 2868','Pereadesen');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Åsa','Linnasalo','AsaLinnasalo@cuvox.de','044 261 2773','Mucall');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Vihtori','Ollila','VihtoriOllila@einrot.com','042 220 6892','Blad1936');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Reijo','Vänskä','ReijoVanska@gustr.com','042 805 1911','Clorge69');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Julia','Vanhanen','JuliaVanhanen@teleworm.us','050 858 0966','Whin1952');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Teija','Hasu','TeijaHasu@dayrep.com','050 387 2734','Suplusentep');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Jarmo','Selänne','JarmoSelanne@dayrep.com','040 695 1749','Givernevends');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Reeta','Järventaus','ReetaJarventaus@cuvox.de','040 024 2851','Mame1981');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Ari-Pekka','Wiljakainen','Ari-PekkaWiljakainen@einrot.com','041 836 1478','Frace1947');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Tiina','Frösén','TiinaFrosen@teleworm.us','042 869 5938','Ager1962');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Tommy','Ikonen','TommyIkonen@gustr.com','046 347 0815','Coputere');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Toini','Eskelinen','ToiniEskelinen@superrito.com','040 458 8262','Askinkin');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Leila','Myller','LeilaMyller@gustr.com','050 415 5623','Notat1982');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus) values('Pirkka','Holmen','PirkkaHolmen@teleworm.us','041 606 7533','Fularks');
insert into yhteyshenkilo (etunimi,sukunimi,sahkoposti,matkapuhelin,kayttajatunnus, sampoid) values('Sampo','Testi','sampo.testi@foo.bar','041 606 7533','sampotesti', 'sampotesti');

-- Liitetään urakoihin pari yhteyshenkilöä, FIXME: enum eri rooleista
INSERT INTO yhteyshenkilo_urakka (yhteyshenkilo, urakka, rooli) VALUES (1, 1, 'Urakanvastuuhenkilö');
INSERT INTO yhteyshenkilo_urakka (yhteyshenkilo, urakka, rooli) VALUES (2, 1, 'Tilaajan edustaja');

UPDATE yhteyshenkilo SET organisaatio=(SELECT id FROM organisaatio WHERE ytunnus='1565583-5');

INSERT INTO yhteyshenkilo_urakka (yhteyshenkilo, urakka, rooli) values (3, 1, 'Työmaapäällikkö');

-- Tehdään pari hanketta
INSERT INTO hanke (nimi,alkupvm,loppupvm,alueurakkanro, sampoid) values ('Oulun alueurakka','2010-10-01', '2015-09-30', '1238', 'oulu1');
INSERT INTO hanke (nimi,alkupvm,loppupvm,alueurakkanro, sampoid) values ('Pudasjärven alueurakka','2007-10-01', '2012-09-30', '1240', 'pudis2');
INSERT INTO hanke (nimi,alkupvm,loppupvm,alueurakkanro, sampoid) values ('Oulun alueurakka','2014-10-01', '2019-09-30', '1238', 'oulu2');

-- Liitetään urakat niihin
UPDATE urakka SET hanke=(SELECT id FROM hanke WHERE sampoid='oulu1') WHERE tyyppi='hoito' AND nimi LIKE 'Oulun%2005%';
UPDATE urakka SET hanke=(SELECT id FROM hanke WHERE sampoid='pudis2') WHERE tyyppi='hoito' AND nimi LIKE 'Pudas%';
UPDATE urakka SET hanke=(SELECT id FROM hanke WHERE sampoid='oulu2') WHERE tyyppi='hoito' AND nimi LIKE 'Oulun%2014%';



-- Ladataan alueurakoiden geometriat
\i testidata/alueurakat.sql


-- Lisätään ELY numerot hallintayksiköille

UPDATE organisaatio SET elynumero=1 WHERE lyhenne='UUD';
UPDATE organisaatio SET elynumero=2 WHERE lyhenne='VAR';
UPDATE organisaatio SET elynumero=3 WHERE lyhenne='KAS';
UPDATE organisaatio SET elynumero=4 WHERE lyhenne='PIR';
UPDATE organisaatio SET elynumero=8 WHERE lyhenne='POS';
UPDATE organisaatio SET elynumero=9 WHERE lyhenne='KES';
UPDATE organisaatio SET elynumero=10 WHERE lyhenne='EPO';
UPDATE organisaatio SET elynumero=12 WHERE lyhenne='POP';
UPDATE organisaatio SET elynumero=14 WHERE lyhenne='LAP';

-- Lisätään indeksejä
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 1, 101.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 2, 103.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 3, 107.6);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 4, 101.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 5, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 6, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 7, 108.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 8, 104.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 9, 107.4);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 10, 102.0);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 11, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2013, 12, 105.2);

INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 1, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 2, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 3, 107.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 4, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 5, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 6, 107.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 7, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 8, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 9, 107.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 10, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 11, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2014, 12, 107.2);

INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 1, 103.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 2, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 3, 104.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 4, 103.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 5, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 6, 104.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 7, 105.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 8, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 9, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 10, 104.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 11, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2005', 2015, 12, 106.2);

INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 1, 101.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 2, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 3, 102.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 4, 104.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 5, 102.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 6, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 7, 107.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 8, 122.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 9, 127.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 10, 132.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 11, 145.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2013, 12, 167.2);

INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 1, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 2, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 3, 107.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 4, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 5, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 6, 107.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 7, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 8, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 9, 107.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 10, 102.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 11, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2014, 12, 107.2);

INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 1, 103.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 2, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 3, 104.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 4, 103.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 5, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 6, 104.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 7, 105.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 8, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 9, 106.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 10, 104.9);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 11, 105.2);
INSERT INTO indeksi (nimi, vuosi, kuukausi, arvo) VALUES ('MAKU 2010', 2015, 12, 106.2);


INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2005-12-31', 3, 'vrk', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 1-ajorat. KVL >15000'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2006-01-01', '2006-09-30', 9, 'vrk', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 1-ajorat. KVL >15000'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2005-12-31', 525.73, 'km', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is ohituskaistat KVL >15000'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2006-01-01', '2006-09-30', 1525.321, 'km', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is ohituskaistat KVL >15000'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));

INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2013-10-01', '2013-12-31', 525.73, 'km', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is ohituskaistat KVL >15000'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));

INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2013-10-01', '2013-12-31', 3, 'vrk', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 1-ajorat. KVL >15000'), (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012') AND paasopimus IS null));
INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2014-01-01', '2014-09-30', 9, 'vrk', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 1-ajorat. KVL >15000'), (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012') AND paasopimus IS null));
INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2013-10-01', '2013-12-31', 866.0, 'km', 525.50, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is ohituskaistat KVL >15000'), (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012') AND paasopimus IS null));

-- talvihoidon laaja toimenpide Oulun ja Pudasjärven urakoille
-- talvihoidon  laaja toimenpide 23104
-- soratien hoidon laaja toimenpide 23124
-- hoitokausi 2005-2006
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku, sampoid, talousosasto_id, talousosastopolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM toimenpidekoodi WHERE koodi='23104'), 'Oulu Talvihoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT loppupvm FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 'tuotepolku', 'sampoid', 'talousosastoid', 'talousosastopolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku, sampoid, talousosasto_id, talousosastopolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM toimenpidekoodi WHERE koodi='23116'), 'Oulu Liikenneympäristön hoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT loppupvm FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 'tuotepolku', 'sampoid', 'talousosastoid', 'talousosastopolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku, sampoid, talousosasto_id, talousosastopolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM toimenpidekoodi WHERE koodi='23124'), 'Oulu Sorateiden hoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT loppupvm FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 'tuotepolku', 'sampoid', 'talousosastoid', 'talousosastopolku');

INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku, sampoid, talousosasto_id, talousosastopolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM toimenpidekoodi WHERE koodi='23104'), 'Oulu Talvihoito TP 2014-2019', (SELECT alkupvm FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT loppupvm FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'tuotepolku', 'sampoid', 'talousosastoid', 'talousosastopolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM toimenpidekoodi WHERE koodi='23116'), 'Oulu Liikenneympäristön hoito TP 2014-2019', (SELECT alkupvm FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT loppupvm FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT id FROM toimenpidekoodi WHERE koodi='23124'), 'Oulu Sorateiden hoito TP 2014-2019', (SELECT alkupvm FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), (SELECT loppupvm FROM urakka WHERE nimi='Oulun alueurakka 2014-2019'), 'tuotepolku');

INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT id FROM toimenpidekoodi WHERE koodi='23104'), 'Pudasjärvi Talvihoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT loppupvm FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT id FROM toimenpidekoodi WHERE koodi='23124'), 'Pudasjärvi Sorateiden hoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT loppupvm FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT id FROM toimenpidekoodi WHERE koodi='23116'), 'Pudasjärvi Liikenneympäristön hoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), (SELECT loppupvm FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 'tuotepolku');

INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), (SELECT id FROM toimenpidekoodi WHERE koodi='23104'), 'Pori Talvihoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), (SELECT loppupvm FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), (SELECT id FROM toimenpidekoodi WHERE koodi='23124'), 'Pori Sorateiden hoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), (SELECT loppupvm FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka, toimenpide, nimi, alkupvm, loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), (SELECT id FROM toimenpidekoodi WHERE koodi='23116'), 'Pori Liikenneympäristön hoito TP', (SELECT alkupvm FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), (SELECT loppupvm FROM urakka WHERE nimi='Porin alueurakka 2007-2012'), 'tuotepolku');

INSERT INTO toimenpideinstanssi (urakka,toimenpide,nimi,alkupvm,loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun tiemerkinnän palvelusopimus 2013-2018'), (SELECT id FROM toimenpidekoodi WHERE taso=3 AND koodi='20123'), 'Tiemerkinnän TP', '2013-10-01','2018-12-31', 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka,toimenpide,nimi,alkupvm,loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Pirkanmaan tiemerkinnän palvelusopimus 2013-2018'), (SELECT id FROM toimenpidekoodi WHERE taso=3 AND koodi='20123'), 'Pirkanmaan Tiemerkinnän TP', '2013-01-01','2018-12-31', 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka,toimenpide,nimi,alkupvm,loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka'), (SELECT id FROM toimenpidekoodi WHERE taso=3 AND koodi='20101'), 'Muhos Ajoradan päällyste TP', '2007-01-01','2012-12-31', 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka,toimenpide,nimi,alkupvm,loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Porintien päällystysurakka'), (SELECT id FROM toimenpidekoodi WHERE taso=3 AND koodi='20101'), 'Porintien Ajoradan päällyste TP', '2007-01-01','2012-12-31', 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka,toimenpide,nimi,alkupvm,loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun valaistuksen palvelusopimus 2013-2018'), (SELECT id FROM toimenpidekoodi WHERE taso=3 AND koodi='20172'), 'Oulu Valaistuksen korjaus TP', '2013-01-01','2018-12-31', 'tuotepolku');
INSERT INTO toimenpideinstanssi (urakka,toimenpide,nimi,alkupvm,loppupvm, tuotepolku) VALUES ((SELECT id FROM urakka WHERE nimi='Kempeleen valaistusurakka'), (SELECT id FROM toimenpidekoodi WHERE taso=3 AND koodi='20172'), 'Kempele Valaistuksen korjaus TP', '2007-10-01','2012-09-30', 'tuotepolku');

INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2005, 10, 3500, '2005-10-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2005, 11, 3500, '2005-11-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2005, 12, 3500, '2005-12-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 1, 3500, '2006-01-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 2, 3500, '2006-02-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 3, 3500, '2006-03-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 4, 3500, '2006-04-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 5, 3500, '2006-05-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 6, 3500, '2006-06-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 7, 3500, '2006-07-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 8, 3500, '2006-08-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 9, 3500, '2006-09-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));

-- hoitokausi 2006-2007
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 10, 3500, '2006-10-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 11, 3500, '2006-11-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 12, 3500, '2006-12-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 1, 3500, '2007-01-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 2, 3500, '2007-02-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 3, 3500, '2007-03-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 4, 3500, '2007-04-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 5, 3500, '2007-05-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 6, 3500, '2007-06-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 7, 3500, '2007-07-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 8, 3500, '2007-08-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2007, 9, 3500, '2007-09-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));

-- toisella sopimusnumerolla kiusaksi yksi työ
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 9, 9999, '2006-09-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS NOT null));


INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2005, 10, 1500, '2005-10-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2005, 11, 1500, '2005-11-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2005, 12, 1500, '2005-12-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 1, 1500, '2006-01-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 2, 1500, '2006-02-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 3, 1500, '2006-03-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 4, 1500, '2006-04-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 5, 1500, '2006-05-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 6, 1500, '2006-06-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 7, 1500, '2006-07-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 8, 1500, '2006-08-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO kokonaishintainen_tyo (vuosi,kuukausi,summa,maksupvm,toimenpideinstanssi,sopimus) VALUES (2006, 9, 1500, '2006-09-15', (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Sorateiden hoito TP'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));

\i testidata/pohjavesialueet.sql

-- Luodaan matskut
INSERT INTO materiaalikoodi (nimi, yksikko, urakkatyyppi, kohdistettava, materiaalityyppi) VALUES ('Talvisuolaliuos NaCl', 't', 'hoito'::urakkatyyppi, false, 'talvisuola'::materiaalityyppi);
INSERT INTO materiaalikoodi (nimi, yksikko, urakkatyyppi, kohdistettava, materiaalityyppi) VALUES ('Talvisuolaliuos CaCl2', 't', 'hoito'::urakkatyyppi, false, 'talvisuola'::materiaalityyppi);
INSERT INTO materiaalikoodi (nimi, yksikko, urakkatyyppi, kohdistettava, materiaalityyppi) VALUES ('Erityisalueet NaCl', 't', 'hoito'::urakkatyyppi, true, 'talvisuola'::materiaalityyppi);
INSERT INTO materiaalikoodi (nimi, yksikko, urakkatyyppi, kohdistettava, materiaalityyppi) VALUES ('Erityisalueet NaCl-liuos', 't', 'hoito'::urakkatyyppi, true, 'talvisuola'::materiaalityyppi);
INSERT INTO materiaalikoodi (nimi, yksikko, urakkatyyppi, kohdistettava, materiaalityyppi) VALUES ('Hiekoitushiekka', 't', 'hoito'::urakkatyyppi, false, 'muu'::materiaalityyppi);
INSERT INTO materiaalikoodi (nimi, yksikko, urakkatyyppi, kohdistettava, materiaalityyppi) VALUES ('Kaliumformiaatti', 't', 'hoito'::urakkatyyppi, false, 'talvisuola'::materiaalityyppi);

-- Materiaalin käytöt
INSERT INTO materiaalin_kaytto (alkupvm, loppupvm, maara, materiaali, urakka, sopimus, pohjavesialue, luotu, muokattu, luoja, muokkaaja, poistettu) VALUES ('20051001', '20100930', 15, 1, 1, 1, null, '2004-10-19 10:23:54+02', '2004-10-19 10:23:54+02', 1, 1, false);

-- Toteumat
INSERT INTO toteuma (urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2004-10-19 10:23:54+02', '2005-10-01 00:00:00+02', '2006-09-30 00:00:00+02', 'yksikkohintainen'::toteumatyyppi, 'Seppo Suorittaja', 'Y123', 'Tällä toteumalla on reitti.');
INSERT INTO toteuma (urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2004-10-19 10:23:54+02', '2005-10-02 00:00:00+02', '2006-09-30 00:00:00+02', 'yksikkohintainen'::toteumatyyppi, 'Antti Ahertaja', 'Y124', 'Sateinen sää haittasi.');
INSERT INTO toteuma (urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2004-10-19 10:23:54+02', '2005-10-03 00:00:00+02', '2006-09-30 00:00:00+02', 'kokonaishintainen'::toteumatyyppi, 'Teppo Tienraivaaja', 'Y125', 'Tehtävä oli vaikea');
INSERT INTO toteuma (urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2004-10-19 10:23:54+02', '2005-10-02 00:00:00+02', '2006-09-30 00:00:00+02', 'yksikkohintainen'::toteumatyyppi, 'Antti Ahertaja', 'Y124', 'Sateinen sää haittasi.');
INSERT INTO toteuma (urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2004-10-19 10:23:54+02', '2005-10-03 00:00:00+02', '2006-09-30 00:00:00+02', 'kokonaishintainen'::toteumatyyppi, 'Teppo Tienraivaaja', 'Y125', 'Tehtävä oli vaikea :(');
INSERT INTO toteuma (urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2004-10-19 10:23:54+02', '2005-10-03 00:00:00+02', '2006-09-30 00:00:00+02', 'yksikkohintainen'::toteumatyyppi, 'Pekan Kone OY', 'Y125', 'Automaattisesti lisätty fastroi toteuma', (SELECT id FROM kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES (1, '2005-10-01 00:00.00', 1350, 10);
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES (2, '2005-10-01 00:00.00', 1350, 7);
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES (1, '2005-10-01 00:00.00', 1351, 5);
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES (3, '2005-10-01 00:00.00', 1350, 15);
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES (3, '2005-10-01 00:00.00', 1351, 150);
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Automaattisesti lisätty fastroi toteuma'), '2005-10-01 00:00.00', 1350, 28);
INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Automaattisesti lisätty fastroi toteuma'), '2005-10-01 00:00.00', 1351, 123);
INSERT INTO toteuma_materiaali (toteuma, luotu, materiaalikoodi, maara) VALUES (1, '2005-10-01 00:00.00', 1, 7);
INSERT INTO toteuma_materiaali (toteuma, luotu, materiaalikoodi, maara) VALUES (1, '2005-10-01 00:00.00', 2, 4);
INSERT INTO toteuma_materiaali (toteuma, luotu, materiaalikoodi, maara) VALUES (2, '2005-10-01 00:00.00', 3, 3);
INSERT INTO toteuma_materiaali (toteuma, luotu, materiaalikoodi, maara) VALUES (3, '2005-10-01 00:00.00', 4, 9);
INSERT INTO toteuma_materiaali (toteuma, luotu, materiaalikoodi, maara) VALUES ((SELECT id FROM toteuma WHERE luoja = 7), '2005-10-01 00:00.00', 5, 25);

-- Muutos-, lisä- ja äkillistä hoitotytöätoteumatyyppi: 'akillinen-hoitotyo', 'lisatyo', 'muutostyo'
INSERT INTO toteuma (urakka, sopimus, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto)
    VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2005-11-11 00:00:00+02', '2005-11-11 00:00:00+02', 'akillinen-hoitotyo'::toteumatyyppi, 'Teppo Tienraivaaja', 'Y125', 'Äkillinen1'),
           ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2005-11-12 00:00:00+02', '2005-11-12 00:00:00+02', 'lisatyo'::toteumatyyppi, 'Teppo Tienraivaaja', 'Y125', 'Lisätyö1'),
           ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2005-11-13 00:00:00+02', '2005-11-13 00:00:00+02', 'muutostyo'::toteumatyyppi, 'Teppo Tienraivaaja', 'Y125', 'Muutostyö1'),
           ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2005-11-14 00:00:00+02', '2005-11-14 00:00:00+02', 'muutostyo'::toteumatyyppi, 'Teppo Tienraivaaja', 'Y125', 'Muutostyö2');
INSERT INTO toteuma (urakka, sopimus, alkanut, paattynyt, tyyppi, suorittajan_nimi, suorittajan_ytunnus, lisatieto, luoja) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), '2005-12-22 10:23:54+02', '2005-12-22 12:23:54+02', 'muutostyo'::toteumatyyppi, 'Pekan Kone OY', 'Y125', 'Koneen muutostyö1', (SELECT id FROM kayttaja WHERE kayttajanimi = 'fastroi'));
INSERT INTO toteuma_tehtava (toteuma, toimenpidekoodi, maara) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Äkillinen1'), (SELECT id FROM toimenpidekoodi WHERE nimi = 'Is 1-ajorat.'), 43);
INSERT INTO toteuma_tehtava (toteuma, toimenpidekoodi, maara) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Lisätyö1'), (SELECT id FROM toimenpidekoodi WHERE nimi = 'Is 2-ajorat.'), 4);
INSERT INTO toteuma_tehtava (toteuma, toimenpidekoodi, maara) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Muutostyö1'), (SELECT id FROM toimenpidekoodi WHERE nimi = 'I ohituskaistat'), 2);
INSERT INTO toteuma_tehtava (toteuma, toimenpidekoodi, maara, paivan_hinta) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Muutostyö2'), (SELECT id FROM toimenpidekoodi WHERE nimi = 'Ib ohituskaistat'), 3, 2000);
INSERT INTO toteuma_tehtava (toteuma, toimenpidekoodi, maara) VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Koneen muutostyö1'), (SELECT id FROM toimenpidekoodi WHERE nimi = 'I ohituskaistat'), 35.4);

-- Sillat
\i testidata/sillat.sql

-- Maksuerät Oulun alueurakalle
\i testidata/maksuerat.sql

INSERT INTO erilliskustannus (tyyppi,sopimus,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('tilaajan_maa-aines', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', -20000, 'MAKU 2005', 'Urakoitsija maksaa tilaajalle', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('vahinkojen_korjaukset', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', 5200, 'MAKU 2005', 'Vahingot on nyt korjattu, lasku tulossa.', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('vahinkojen_korjaukset', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-11-18', -65200, 'MAKU 2005', 'Urakoitsija maksaa tilaajalle.', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('asiakastyytyvaisyysbonus', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', 10000, 'MAKU 2005', 'Asiakkaat erittäin tyytyväisiä, tyytyväisyysindeksi 0,92.', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));
INSERT INTO erilliskustannus (tyyppi,sopimus,toimenpideinstanssi,pvm,rahasumma,indeksin_nimi,lisatieto,luotu,luoja) VALUES ('muu', (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null), (SELECT id FROM toimenpideinstanssi WHERE nimi='Oulu Talvihoito TP'), '2005-10-15', 20000, 'MAKU 2005', 'Muun erilliskustannuksen lisätieto', '2005-10-13', (SELECT ID FROM kayttaja WHERE kayttajanimi = 'yit_uuvh'));

INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 2, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 1-ajorat.'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 2.5, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='Is 2-ajorat.'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 3.5, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='I ohituskaistat'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));
INSERT INTO muutoshintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus) VALUES ('2005-10-01', '2010-09-30', 'tiekm', 4.5, (SELECT id FROM toimenpidekoodi WHERE taso=4 AND nimi='I rampit'), (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (select id from sopimus where urakka = (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010') AND paasopimus IS null));

-- Päällystyskohteet

-- Päällystys
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 'L03', 'Leppäjärven ramppi', 400, true, 100, 4543.95, 0);
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 308, 'Mt 2855 Viisari - Renko', 9000, false, 200, 565, 100);
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 'L010', 'Tie 357', 500, true, 3457, 5, 6);
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen päällystysurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen päällystysurakka') AND paasopimus IS null), 310, 'Oulaisten ohitusramppi', 500, false, 3457, 5, 6);

INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide,sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi 2'), 'Laivaniemi 1', 19520, 3, 5, 10, 15, 2, 1, 'PAB-B 16/80 MPKJ', ST_GeomFromText('MULTILINESTRING((426888 7212758,427081 7212739),(434777 7215499,436899 7217174,438212 7219910,438676 7220554,440102 7221432,441584 7222729,442255 7223162,443128 7223398,443750 7223713,448682 7225293,451886 7226708,456379 7228018,459945 7229222,461039 7229509))'));
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide,sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'Laivaniemi 1', 19521, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ', ST_GeomFromText('MULTILINESTRING((426888 7212758,427081 7212739),(434777 7215499,436899 7217174,438212 7219910,438676 7220554,440102 7221432,441584 7222729,442255 7223162,443128 7223398,443750 7223713,448682 7225293,451886 7226708,456379 7228018,459945 7229222,461039 7229509))'));
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide,sijainti) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'Laivaniemi 45', 19521, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ', ST_GeomFromText('MULTILINESTRING((384276 6674532,384269 6674528,383563 6674582,383518 6674607,383350 6674736,383244 6674822,383201 6674859,383028 6675028,382959 6675071,382825 6675131,382737 6675175,382737 6675213,382730 6675251,382615 6675745,382569 6675961,382555 6675978,382529 6675975,382519 6675967))'));

-- Paikkaus
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen paikkausurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen paikkausurakka') AND paasopimus IS null), 311, 'Oulaisten ohitusramppi 2', 0, false, 3457, 5, 6);
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen paikkausurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen paikkausurakka') AND paasopimus IS null), 312, 'Tie 358', 0, false, 3457, 5, 6);
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen paikkausurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen paikkausurakka') AND paasopimus IS null), 313, 'Leppäjärven ramppi 2', 0, false, 3457, 5, 6);
INSERT INTO paallystyskohde (urakka, sopimus, kohdenumero, nimi, sopimuksen_mukaiset_tyot, muu_tyo, arvonvahennykset, bitumi_indeksi, kaasuindeksi) VALUES ((SELECT id FROM urakka WHERE  nimi = 'Muhoksen paikkausurakka'), (SELECT id FROM sopimus WHERE urakka = (SELECT id FROM urakka WHERE nimi='Muhoksen paikkausurakka') AND paasopimus IS null), 314, 'Leppäjärven ramppi 3', 0, false, 3457, 5, 6);

INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi 2'), 'Laivaniemi 1', 19522, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ');
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Tie 358'), 'Laivaniemi 2', 19525, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ');
INSERT INTO paallystyskohdeosa (paallystyskohde, nimi, tr_numero, tr_alkuosa, tr_alkuetaisyys, tr_loppuosa, tr_loppuetaisyys, kvl, nykyinen_paallyste, toimenpide) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Leppäjärven ramppi 2'), 'Laivaniemi 3', 19526, 10, 5, 10, 15, 2, 2, 'PAB-B 16/80 MPKJ');

-- Päällystysilmoitukset
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Leppäjärven ramppi'), 'aloitettu'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Tie 357'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}');
INSERT INTO paallystysilmoitus (paallystyskohde, tila, aloituspvm, valmispvm_kohde, valmispvm_paallystys, takuupvm, muutoshinta, ilmoitustiedot, paatos_tekninen_osa, paatos_taloudellinen_osa, perustelu_tekninen_osa, perustelu_taloudellinen_osa, kasittelyaika_tekninen_osa, kasittelyaika_taloudellinen_osa) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi'), 'valmis'::paallystystila, '2005-11-14 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-19 00:00:00+02', '2005-12-20 00:00:00+02', 2000, '{"osoitteet":[{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":0,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":16,"massa":100,"rc%":0,"tyomenetelma":12,"leveys":6.5,"massamaara":1781,"edellinen-paallystetyyppi":12,"pinta-ala":15},{"tie":2846,"aosa":5,"aet":22,"losa":5,"let":9377,"ajorata":1,"suunta":0,"kaista":1,"paallystetyyppi":21,"raekoko":10,"massa":512,"rc%":0,"tyomenetelma":12,"leveys":4,"massamaara":1345,"edellinen-paallystetyyppi":11,"pinta-ala":9}],"kiviaines":[{"esiintyma":"KAMLeppäsenoja","km-arvo":"An14","muotoarvo":"Fi20","sideainetyyppi":"B650/900","pitoisuus":4.3,"lisaaineet":"Tartuke"}],"alustatoimet":[{"aosa":22,"aet":3,"losa":5,"let":4785,"kasittelymenetelma":13,"paksuus":30,"verkkotyyppi":1,"tekninen-toimenpide":2}],"tyot":[{"tyyppi":"ajoradan-paallyste","tyo":"AB 16/100 LTA","tilattu-maara":10000,"toteutunut-maara":10100,"yksikkohinta":20, "yksikko": "km"}]}', 'hylatty'::paallystysilmoituksen_paatostyyppi, 'hylatty'::paallystysilmoituksen_paatostyyppi, 'Ei tässä ole mitään järkeä', 'Ei tässä ole mitään järkeä', '2005-12-20 00:00:00+02', '2005-12-20 00:00:00+02');

-- Paikkausilmoitukset
INSERT INTO paikkausilmoitus (paikkauskohde, ilmoitustiedot, toteutunut_hinta, aloituspvm, luotu, paatos, perustelu, kasittelyaika, tila) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Leppäjärven ramppi 2'), '{"osoitteet":[{"tie":1,"aosa":1,"aet":1,"losa":5,"let": 7,"paallysteen_leveys":2,"paikkausneliot":2}],"toteumat":[{"suorite":1,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":2,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":3,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":4,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":5,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":6,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":7,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"}]}', 7, '2005-11-14 00:00:00+02', NOW(), null, null, '2005-12-20 00:00:00+02', 'aloitettu'::paikkausilmoituksen_tila);
INSERT INTO paikkausilmoitus (paikkauskohde, ilmoitustiedot, toteutunut_hinta, aloituspvm, valmispvm_paikkaus, valmispvm_kohde, luotu, paatos, perustelu, kasittelyaika, tila) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Tie 358'), '{"osoitteet":[{"tie":1,"aosa":1,"aet":1,"losa":5,"let": 7,"paallysteen_leveys":2,"paikkausneliot":2}],"toteumat":[{"suorite":1,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":2,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":3,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":4,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":5,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":6,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":7,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"}]}', 7, '2005-11-15 00:00:00+02', '2005-11-16 00:00:00+02', '2005-11-17 00:00:00+02', NOW(), 'hylatty'::paikkausilmoituksen_paatostyyppi, 'Tämä ei nyt ole oikein hyvä', '2005-12-20 00:00:00+02', 'valmis'::paikkausilmoituksen_tila);
INSERT INTO paikkausilmoitus (paikkauskohde, ilmoitustiedot, toteutunut_hinta, aloituspvm, valmispvm_paikkaus, valmispvm_kohde, luotu, paatos, perustelu, kasittelyaika, tila) VALUES ((SELECT id FROM paallystyskohde WHERE nimi ='Oulaisten ohitusramppi 2'), '{"osoitteet":[{"tie":1,"aosa":1,"aet":1,"losa":5,"let": 7,"paallysteen_leveys":2,"paikkausneliot":2}],"toteumat":[{"suorite":1,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":2,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":3,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":4,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":5,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":6,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"},{"suorite":7,"yksikko":"km","maara":1,"yks_hint_alv_0":1,"takuupvm":"2005-11-13T22:00:00+02"}]}', 7, '2005-11-14 00:00:00+02', '2005-11-15 00:00:00+02', '2005-11-16 00:00:00+02', NOW(), null, null, '2005-12-20 00:00:00+02', 'valmis'::paikkausilmoituksen_tila);

-- Ilmoitukset ja kuittaukset
-- Ensimmäinen ilmoitus: Oulun alueella, kysely
INSERT INTO ilmoitus 
(urakka, ilmoitusid, ilmoitettu, valitetty, yhteydenottopyynto, vapaateksti, sijainti, 
tr_numero, tr_alkuosa, tr_loppuosa, tr_alkuetaisyys, tr_loppuetaisyys, ilmoitustyyppi, selitteet, urakkatyyppi,
ilmoittaja_etunimi, ilmoittaja_sukunimi, ilmoittaja_tyopuhelin, ilmoittaja_matkapuhelin, ilmoittaja_sahkoposti, ilmoittaja_tyyppi,
lahettaja_etunimi, lahettaja_sukunimi, lahettaja_puhelinnumero, lahettaja_sahkoposti)
VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 12345, '2005-10-01 10:00.00', '2005-10-01 10:05.13', true, 'Voisko joku soittaa?',
ST_MakePoint(452935, 7186873)::POINT, 6, 6, 6, 6, 6, 'kysely'::ilmoitustyyppi, ARRAY['saveaTiella', 'vettaTiella']::ilmoituksenselite[],
(SELECT tyyppi FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'),
'Seppo', 'Savela', '0441231234', '0441231234', 'seppo.savela@eiole.fi', 'asukas'::ilmoittajatyyppi,
'Mari', 'Marttala', '085674567', 'mmarttala@isoveli.com');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12345), 12345, '2005-10-01 10:07.03', 'vastaanotto'::kuittaustyyppi,
'Mikael', 'Pöytä', '04428671283', '0509288383', 'mikael.poyta@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_henkilo_etunimi, kasittelija_henkilo_sukunimi, kasittelija_henkilo_matkapuhelin, kasittelija_henkilo_tyopuhelin, kasittelija_henkilo_sahkoposti,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12345), 12345, '2005-10-01 10:34.50', 'Soitan kunhan kerkeän', 'vastaus'::kuittaustyyppi,
'Usko', 'Untamo', '04428121283', '0509288383', 'usko.untamo@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234',
'Usko', 'Untamo', '04428121283', '0509288383', 'usko.untamo@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_henkilo_etunimi, kasittelija_henkilo_sukunimi, kasittelija_henkilo_matkapuhelin, kasittelija_henkilo_tyopuhelin, kasittelija_henkilo_sahkoposti,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12345), 12345, '2005-10-02 11:28.50', 'Soitan lounaan jälkeen!', 'aloitus'::kuittaustyyppi,
'Usko', 'Untamo', '04428121283', '0509288383', 'usko.untamo@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234',
'Usko', 'Untamo', '04428121283', '0509288383', 'usko.untamo@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_henkilo_etunimi, kasittelija_henkilo_sukunimi, kasittelija_henkilo_matkapuhelin, kasittelija_henkilo_tyopuhelin, kasittelija_henkilo_sahkoposti,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12345), 12345, '2005-10-02 12:08.02',
'Homma on hoidettu. Ei siellä oikeastaan mitään tähdellistä asiaa ollutkaan..', 'lopetus'::kuittaustyyppi,
'Usko', 'Untamo', '04428121283', '0509288383', 'usko.untamo@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234',
'Usko', 'Untamo', '04428121283', '0509288383', 'usko.untamo@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234');

-- Toinen ilmoitus: Oulun alueella, toimenpidepyynto
INSERT INTO ilmoitus
(urakka, ilmoitusid, ilmoitettu, valitetty, yhteydenottopyynto, vapaateksti, sijainti,
tr_numero, tr_alkuosa, tr_loppuosa, tr_alkuetaisyys, tr_loppuetaisyys, ilmoitustyyppi, selitteet, urakkatyyppi,
ilmoittaja_etunimi, ilmoittaja_sukunimi, ilmoittaja_tyopuhelin, ilmoittaja_matkapuhelin, ilmoittaja_sahkoposti, ilmoittaja_tyyppi,
lahettaja_etunimi, lahettaja_sukunimi, lahettaja_puhelinnumero, lahettaja_sahkoposti)
VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 12346, '2005-10-10 06:05.32', '2005-10-11 06:06.37', true, 'Taas täällä joku mättää!',
ST_MakePoint(435847, 7216217)::POINT, 6, 6, 6, 6, 6, 'toimenpidepyynto'::ilmoitustyyppi,
ARRAY['kaivonKansiRikki', 'vettaTiella']::ilmoituksenselite[],
(SELECT tyyppi FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'),
'Yrjö', 'Mestari', '0441271234', '0441233424', 'tyonvalvonta@isoveli.com', 'muu'::ilmoittajatyyppi,
'Mari', 'Marttala', '085674567', 'mmarttala@isoveli.com');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12346), 12346, '2005-10-11 06:10.07', 'vastaanotto'::kuittaustyyppi,
'Mikael', 'Pöytä', '04428671283', '0509288383', 'mikael.poyta@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12346), 12346, '2005-10-11 14:02.57', 'Siirretty aliurakoitsijalle', 'muutos'::kuittaustyyppi,
'Mikael', 'Pöytä', '04428671283', '0509288383', 'mikael.poyta@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234',
'Veljekset Ukkola Huoltoyritys', 'Y8172');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12346), 12346, '2005-10-11 19:20.57', 'Ukkolat korjasi tilanteen', 'lopetus'::kuittaustyyppi,
'Mikael', 'Pöytä', '04428671283', '0509288383', 'mikael.poyta@valittavaurakoitsija.fi',
'Välittävä Urakoitsija', 'Y1234',
'Veljekset Ukkola Huoltoyritys', 'Y8172');


-- Kolmas ilmoitus: Pudasjärvi, toimenpidepyynto, avoin
INSERT INTO ilmoitus
(urakka, ilmoitusid, ilmoitettu, valitetty, yhteydenottopyynto, vapaateksti, sijainti,
tr_numero, tr_alkuosa, tr_loppuosa, tr_alkuetaisyys, tr_loppuetaisyys, ilmoitustyyppi, selitteet, urakkatyyppi,
ilmoittaja_etunimi, ilmoittaja_sukunimi, ilmoittaja_tyopuhelin, ilmoittaja_matkapuhelin, ilmoittaja_sahkoposti, ilmoittaja_tyyppi,
lahettaja_etunimi, lahettaja_sukunimi, lahettaja_puhelinnumero, lahettaja_sahkoposti)
VALUES ((SELECT id FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'), 12347, '2007-12-01 20:01.20', '2007-12-07 08:07.50',
false, 'Kauhea kuoppa tiessä',
ST_MakePoint(499687, 7248153)::POINT, 6, 6, 6, 6, 6, 'toimenpidepyynto'::ilmoitustyyppi,
ARRAY['kuoppiaTiessa', 'vettaTiella']::ilmoituksenselite[],
(SELECT tyyppi FROM urakka WHERE nimi='Pudasjärven alueurakka 2007-2012'),
'Paavo', 'Poliisimies', '086727461', '0448261234', 'paavo.poliisimies@poliisi.fi', 'viranomainen'::ilmoittajatyyppi,
'Mika', 'Vaihdemies', '085612567', 'vaihde@valituspalvelu.fi');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12347), 12347, '2007-12-07 08:47.50', 'vastaanotto'::kuittaustyyppi,
'Merituuli', 'Salmela', '04020671222', '081234512', 'merituuli.salmela@vainamoinen.fi',
'Väinämöinen', 'Y72787');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12347), 12347, '2007-12-07 08:48.05', 'Anteeksi kauheasti olin kahvilla!',
'vastaus'::kuittaustyyppi, 'Merituuli', 'Salmela', '04020671222', '081234512', 'merituuli.salmela@vainamoinen.fi',
'Väinämöinen', 'Y72787');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12347), 12347, '2007-12-07 11:27.07', 'Aliurakoitsija käy katsomassa',
'muutos'::kuittaustyyppi,
'Merituuli', 'Salmela', '04020671222', '081234512', 'merituuli.salmela@vainamoinen.fi',
'Väinämöinen', 'Y72787',
'Veljekset Ukkola Huoltoyritys', 'Y8172');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12347), 12347, '2007-12-07 15:07.30', 'Ukkolat aloitti työt',
'aloitus'::kuittaustyyppi,
'Merituuli', 'Salmela', '04020671222', '081234512', 'merituuli.salmela@vainamoinen.fi',
'Väinämöinen', 'Y72787',
'Veljekset Ukkola Huoltoyritys', 'Y8172');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12347), 12347, '2007-12-17 09:17.30', 'Työt ei edisty, hoidetaan itse.',
'muutos'::kuittaustyyppi,
'Merituuli', 'Salmela', '04020671222', '081234512', 'merituuli.salmela@vainamoinen.fi',
'Väinämöinen', 'Y72787',
'Väinämöinen', 'Y72787');

INSERT INTO kuittaus
(ilmoitus, ilmoitusid, kuitattu, vapaateksti, kuittaustyyppi,
kuittaaja_henkilo_etunimi, kuittaaja_henkilo_sukunimi, kuittaaja_henkilo_matkapuhelin, kuittaaja_henkilo_tyopuhelin, kuittaaja_henkilo_sahkoposti,
kuittaaja_organisaatio_nimi, kuittaaja_organisaatio_ytunnus,
kasittelija_organisaatio_nimi, kasittelija_organisaatio_ytunnus)
VALUES ((SELECT id FROM ilmoitus WHERE ilmoitusid=12347), 12347, '2007-12-18 19:17.30', 'Normaalia kiperämpi kuoppa.',
'vastaus'::kuittaustyyppi,
'Merituuli', 'Salmela', '04020671222', '081234512', 'merituuli.salmela@vainamoinen.fi',
'Väinämöinen', 'Y72787',
'Väinämöinen', 'Y72787');


-- Neljäs ilmoitus: Turun alueella, tiedoitus. Ei kuittauksia!
INSERT INTO ilmoitus
(ilmoitusid, ilmoitettu, valitetty, yhteydenottopyynto, vapaateksti, sijainti,
tr_numero, tr_alkuosa, tr_loppuosa, tr_alkuetaisyys, tr_loppuetaisyys, ilmoitustyyppi, selitteet,
ilmoittaja_etunimi, ilmoittaja_sukunimi, ilmoittaja_tyopuhelin, ilmoittaja_matkapuhelin, ilmoittaja_sahkoposti, ilmoittaja_tyyppi,
lahettaja_etunimi, lahettaja_sukunimi, lahettaja_puhelinnumero, lahettaja_sahkoposti)
VALUES (12348, '2006-02-13 00:00.00', '2005-02-13 00:00.00', false, 'Täällä joku pommi räjähti!!',
ST_MakePoint(249863, 6723867)::POINT, 6, 6, 6, 6, 6, 'tiedoitus'::ilmoitustyyppi, ARRAY['virkaApupyynto']::ilmoituksenselite[],
'George', 'Doe', '05079163872', '05079163872', '', 'tienkayttaja'::ilmoittajatyyppi,
'Mika', 'Vaihdemies', '085612567', 'vaihde@valityspalvelu.fi');


-- Turvallisuuspoikkeama
INSERT INTO turvallisuuspoikkeama
(urakka, tapahtunut, paattynyt, kasitelty, tyontekijanammatti, tyotehtava, kuvaus, vammat, sairauspoissaolopaivat,
sairaalavuorokaudet, luotu, luoja, sijainti, tr_numero, tr_alkuosa, tr_loppuosa, tr_alkuetaisyys, tr_loppuetaisyys, tyyppi)
VALUES
((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), '2005-10-01 10:00.00', '2005-10-01 12:20.00', '2005-10-06 09:00.00',
'Trukkikuski', 'Lastaus', 'Sepolla oli kiire lastata laatikot, ja torni kaatui päälle. Ehti onneksi pois alta niin ei henki lähtenyt.',
'Murtunut peukalo', 7, 1, NOW(), (SELECT id FROM kayttaja WHERE kayttajanimi='jvh'), ST_MakePoint(435847, 7216217)::POINT, 6, 6, 6, 6, 6,
ARRAY['turvallisuuspoikkeama']::turvallisuuspoikkeamatyyppi[]);

INSERT INTO korjaavatoimenpide
(turvallisuuspoikkeama, kuvaus, vastaavahenkilo)
VALUES
((SELECT id FROM turvallisuuspoikkeama WHERE tyontekijanammatti='Trukkikuski'), 'Pidetään huoli että ei kenenkään tarvi liikaa kiirehtiä',
'Tomi Työnjohtaja');

-- Havainnot

INSERT INTO havainto (kohde, tekija, kasittelytapa, muu_kasittelytapa, paatos, perustelu, tarkastuspiste, luoja, luotu, aika, kasittelyaika, selvitys_pyydetty, selvitys_annettu, urakka, kuvaus, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tr_alkuetaisyys)
VALUES ('Testikohde', 'tilaaja'::osapuoli, 'puhelin'::havainnon_kasittelytapa, '', 'hylatty'::havainnon_paatostyyppi, 'Ei tässä ole mitään järkeä', 123, 1, NOW(), '2005-10-11 06:06.37', '2005-10-11 06:06.37', false, false, (SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), 'Täysin turha testihavainto', 1, 2, 3, 4, point(1, 2), 5);

-- Sanktiot

INSERT INTO sanktio (sakkoryhma, maara, perintapvm, indeksi, havainto, toimenpideinstanssi, tyyppi, suorasanktio, luoja) VALUES ('A'::sanktiolaji, 1000, '2005-10-12 06:06.37', 'Testi-indeksi', 1, (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Oulu Talvihoito TP'), 1, true, 2);

-- Tarkastukset

INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, mittaaja, tyyppi, havainto, luotu, luoja, tr_alkuetaisyys) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE nimi = 'Oulun alueurakka pääsopimus' AND urakka = 1), '2005-10-01 10:00.00', 1 ,2, 3, 4, point(429293, 7209214), 'Ismo', 'Seppo', 'pistokoe'::tarkastustyyppi, 1, NOW(), 1, 3);
INSERT INTO tarkastus (urakka, sopimus, aika, tr_numero, tr_alkuosa, tr_loppuosa, tr_loppuetaisyys, sijainti, tarkastaja, mittaaja, tyyppi, havainto, luotu, luoja, tr_alkuetaisyys) VALUES ((SELECT id FROM urakka WHERE nimi='Oulun alueurakka 2005-2010'), (SELECT id FROM sopimus WHERE nimi = 'Oulun alueurakka pääsopimus' AND urakka = 1), '2005-10-01 10:00.00', 1 ,2, 3, 4, point(429000, 7202314), 'Matti', 'Pentti', 'pistokoe'::tarkastustyyppi, 1, NOW(), 1, 3);

-- Tyokoneseurannan havainnot

INSERT INTO tyokonehavainto (jarjestelma, organisaatio, viestitunniste, lahetysaika, tyokoneid, tyokonetyyppi, sijainti, suunta,
urakkaid, sopimusid, tehtavat) VALUES (
  'Urakoitsijan järjestelmä 1',
  (SELECT id FROM organisaatio WHERE nimi='Destia Oy'),
  123,
  current_timestamp,
  31337,
  'aura-auto',
  ST_MakePoint(7207739,429493)::POINT,
  45,
  (SELECT id FROM urakka WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT id FROM sopimus WHERE nimi = 'Muhoksen pääsopimus' AND urakka=12),
  ARRAY['harjaus', 'suolaus']::suoritettavatehtava[]
);

INSERT INTO tyokonehavainto (jarjestelma, organisaatio, viestitunniste, lahetysaika, tyokoneid, tyokonetyyppi, sijainti, suunta,
urakkaid, sopimusid, tehtavat) VALUES (
  'Urakoitsijan järjestelmä 1',
  (SELECT id FROM organisaatio WHERE nimi='NCC Roads Oy'),
  123,
  current_timestamp,
  31338,
  'aura-auto',
  ST_MakePoint(7211247,427861)::POINT,
  45,
  (SELECT id FROM urakka WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT id FROM sopimus WHERE nimi = 'Muhoksen pääsopimus' AND urakka=12),
  ARRAY['pistehiekoitus']::suoritettavatehtava[]
);

INSERT INTO tyokonehavainto (jarjestelma, organisaatio, viestitunniste, lahetysaika, tyokoneid, tyokonetyyppi, sijainti, suunta,
urakkaid, sopimusid, tehtavat) VALUES (
  'Urakoitsijan järjestelmä 1',
  (SELECT id FROM organisaatio WHERE nimi='Destia Oy'),
  123,
  current_timestamp,
  31339,
  'aura-auto',
  ST_MakePoint(7249077,499399)::POINT,
  45,
  (SELECT id FROM urakka WHERE nimi = 'Pudasjärven alueurakka 2007-2012'),
  (SELECT id FROM sopimus WHERE nimi = 'Pudasjärvi pääsopimus'),
  ARRAY['muu']::suoritettavatehtava[]
);

-- Toteumia joilla on reittipisteitä
INSERT INTO toteuma
(urakka, sopimus, luotu, alkanut, paattynyt, tyyppi, lisatieto, suorittajan_ytunnus, suorittajan_nimi, luoja)
VALUES
((SELECT id FROM urakka WHERE nimi = 'Pudasjärven alueurakka 2007-2012'),
(SELECT id FROM sopimus WHERE nimi = 'Pudasjärvi pääsopimus'),
NOW(),
'2008-09-09 10:00.00',
'2008-09-09 10:09.00',
'kokonaishintainen'::toteumatyyppi,
'Tämä on käsin tekaistu juttu',
'Y1234',
'Antti Aurakuski',
(SELECT id FROM kayttaja WHERE kayttajanimi='jvh'));

INSERT INTO toteuma_tehtava (toteuma, luotu, toimenpidekoodi, maara, luoja, paivan_hinta, lisatieto)
VALUES
((SELECT id FROM toteuma WHERE lisatieto = 'Tämä on käsin tekaistu juttu'),
NOW(), 1350, 10, (SELECT id FROM kayttaja WHERE kayttajanimi='jvh'), 40, 'Tämä on tekaistu tehtävä');

-- Reittipisteet kokonaishintaiselle työlle

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tämä on käsin tekaistu juttu'),
'2008-09-09 10:00.00',
NOW(),
st_makepoint(498919, 7247099) :: POINT);

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tämä on käsin tekaistu juttu'),
'2008-09-09 10:03.00',
NOW(),
st_makepoint(499271, 7248395) :: POINT);

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tämä on käsin tekaistu juttu'),
'2008-09-09 10:06.00',
NOW(),
st_makepoint(499399, 7249019) :: POINT);

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tämä on käsin tekaistu juttu'),
'2008-09-09 10:09.00',
NOW(),
st_makepoint(499820, 7249885) :: POINT);

-- Reittipisteet yksikköhintaiselle työlle

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tällä toteumalla on reitti.'),
'2005-10-10 10:00.00',
NOW(),
st_makepoint(498919, 7247099) :: POINT);

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tällä toteumalla on reitti.'),
'2005-10-10 10:00.00',
NOW(),
st_makepoint(499271, 7248395) :: POINT);

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tällä toteumalla on reitti.'),
'2005-10-10 10:00.00',
NOW(),
st_makepoint(499399, 7249019) :: POINT);

INSERT INTO reittipiste (toteuma, aika, luotu, sijainti)
VALUES ((SELECT id FROM toteuma WHERE lisatieto = 'Tällä toteumalla on reitti.'),
'2005-10-10 10:00.00',
NOW(),
st_makepoint(499820, 7249885) :: POINT);

INSERT INTO reitti_tehtava (reittipiste, luotu, toimenpidekoodi, maara)
VALUES ((SELECT id FROM reittipiste WHERE aika = '2008-09-09 10:00.00' :: TIMESTAMP ),
NOW(), 1350, 10);

INSERT INTO reitti_tehtava (reittipiste, luotu, toimenpidekoodi, maara)
VALUES
((SELECT id FROM reittipiste WHERE aika = '2008-09-09 10:03.00' :: TIMESTAMP ),
NOW(), 1350, 10);

INSERT INTO reitti_tehtava (reittipiste, luotu, toimenpidekoodi, maara)
VALUES ((SELECT id FROM reittipiste WHERE aika = '2008-09-09 10:06.00' :: TIMESTAMP ),
NOW(), 1350, 10);

INSERT INTO reitti_tehtava (reittipiste, luotu, toimenpidekoodi, maara)
VALUES ((SELECT id FROM reittipiste WHERE aika = '2008-09-09 10:09.00' :: TIMESTAMP ),
NOW(), 1350, 10);

-- Refreshaa Viewit. Nämä kannattanee pitää viimeisenä just in case

SELECT paivita_urakoiden_alueet();

-- Luodaan testidataa laskutusyhteenvetoraporttia varten
\i testidata/laskutusyhteenveto.sql
-- ****
