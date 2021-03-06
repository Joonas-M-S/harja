INSERT INTO tietyoilmoitus (paatietyoilmoitus,
                            luotu,
                            luoja,
                            "ilmoittaja-id",
                            ilmoittaja,
                            "urakka-id",
                            "urakan-nimi",
                            urakkatyyppi,
                            "urakoitsijayhteyshenkilo-id",
                            urakoitsijayhteyshenkilo,
                            "tilaaja-id",
                            "tilaajan-nimi",
                            "tilaajayhteyshenkilo-id",
                            tilaajayhteyshenkilo,
                            tyotyypit,
                            osoite,
                            "tien-nimi",
                            kunnat,
                            "alkusijainnin-kuvaus",
                            "loppusijainnin-kuvaus",
                            alku,
                            loppu,
                            tyoajat,
                            vaikutussuunta,
                            kaistajarjestelyt,
                            nopeusrajoitukset,
                            tienpinnat,
			    "kiertotien-pituus",
                            "kiertotien-mutkaisuus",
                            kiertotienpinnat,
                            liikenteenohjaus,
                            liikenteenohjaaja,
                            "viivastys-normaali-liikenteessa",
                            "viivastys-ruuhka-aikana",
                            ajoneuvorajoitukset,
                            huomautukset,
                            "ajoittaiset-pysaytykset",
                            "ajoittain-suljettu-tie",
                            "pysaytysten-alku",
                            "pysaytysten-loppu",
                            lisatietoja,
                            "urakoitsija-id",
                            "urakoitsijan-nimi")

VALUES (NULL,
  '2017-01-01 01:01:01',
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: tietyon_henkilo,
  (SELECT id
   FROM urakka
   WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT nimi
   FROM urakka
   WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT tyyppi
   FROM urakka
   WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: tietyon_henkilo,
  (SELECT id
   FROM organisaatio
   WHERE lyhenne = 'POP'),
  (SELECT nimi
   FROM organisaatio
   WHERE lyhenne = 'POP'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com')) :: tietyon_henkilo,
  '{"(Tienrakennus,Rakennetaan tietä)"}',
  ROW (20, 1, 1, 5, 1,
  ST_GeomFromText(
      'MULTILINESTRING((430651.697487601 7212579.38600741,430667.369 7212589.979,430697.85 7212610.534,430797.842 7212680.199,430825.791 7212700.691,430831.32 7212704.529,430897.841 7212750.706,430928.359 7212773.044,430970.265 7212802.533,431004.767 7212826.58,431036.589 7212848.205,431081.499 7212879.362,431120.673 7212907.094,431176.235 7212944.988,431190.372 7212954.377,431212.991 7212970.698,431217.443 7212973.958,431227.922 7212981.794,431236.641 7212988.003,431279.488 7213019.255,431350.784 7213069.93,431397.511 7213102.948,431418.022 7213118.304,431426.405 7213124.78,431449.031 7213142.281,431486.141 7213172.539,431553.474 7213237.334,431607.221 7213289.507,431635.956 7213316.213,431669.835 7213343.386,431693.874 7213361.536,431733.887 7213390.148,431753.04 7213402.801,431762.396 7213408.575,431772.326 7213415.306,431789.594 7213425.862,431813.253 7213439.663,431851.74 7213459.513,431873.292 7213470.678,431884.732 7213476.398,431908.326 7213488.212,431941.407 7213504.965,431994.207 7213532.956,432023.093 7213549.134,432058.46 7213571.444,432079.672 7213584.434,432091.457 7213591.643,432122.832 7213610.355,432192.972 7213660.585,432263.565 7213710.363,432335.515 7213760.366,432381.219 7213793.853,432410.905 7213815.438,432427.905 7213827.798,432447.633 7213841.183,432497.064 7213874.629,432551.818 7213914.903,432622.411 7213965.36,432674.903 7214000.883,432817.307 7214101.146,432836.94 7214115.889,432859.94 7214132.173,432871.042 7214139.798,432909.88 7214167.229,432921.607 7214175.483,433104.142 7214303.024,433129.232 7214321.875,433148.142 7214335.547,433167.826 7214350.352,433211.796 7214381.221,433256.743 7214413.035,433296.003 7214440.18,433336.211 7214468.001,433425.744 7214531.781,433510.836 7214588.786,433514.481 7214591.228,433534.998 7214606.277,433544.311 7214613.133,433555.492 7214620.803,433564.971 7214627.401,433597.888 7214651.456,433673.219 7214705.867,433736.372 7214753.582,433765.25 7214775.182,433777.029 7214785.107,433867.398 7214857.158,433907.517 7214889.398,433929.686 7214907.278,433959.329 7214932.265,433991.308 7214956.166,434016.683 7214973.688,434038.098 7214989.478,434067.032 7215010.574,434087.56 7215024.542,434103.069 7215035.075,434115.06 7215043.587,434134.399 7215057.149,434146.494 7215064.844,434174.836 7215083.39,434212.271 7215107.757,434268.175 7215143.976,434328.583 7215183.239,434377.12 7215214.632,434408.777 7215234.947,434473.151 7215277.016,434517.023 7215305.215,434527.14 7215311.809,434542.767 7215321.988,434557.061 7215331.505,434593.051 7215355.736,434595.092 7215357.149,434640.866 7215388.847,434685.73 7215421.797,434687.589 7215423.176,434742.519 7215463.945,434764.109 7215480.302,434776.606 7215490.381,434784.224 7215501.513,434809.379 7215521.085,434838.862 7215543.856,434874.732 7215571.144,434887.912 7215581.155,434912.224 7215598.388,434930.88 7215612.225,434937.857 7215617.28,434951.95 7215627.667,434959.62 7215632.997,434975.32 7215644.318,434996.004 7215659.035,435041.619 7215692.904,435113.127 7215744.905,435177.465 7215791.14,435211.304 7215815.896,435234.269 7215832.152,435265.382 7215854.873,435283.704 7215867.728,435305.533 7215883.891,435342.579 7215910.822,435363.897 7215926.319,435402.991 7215954.8,435450.239 7215990.493,435497.814 7216027.27,435536.002 7216057.033,435591.586 7216100.312,435609.238 7216113.819,435622.811 7216123.637,435634.907 7216128.915,435652.899 7216140.833,435676.68 7216157.188,435699.523 7216174.213,435745.287 7216209.513,435760.104 7216221.192,435773.455 7216231.416,435788.098 7216242.903,435805.564 7216257.586,435810.622 7216261.513,435814.926 7216265.06),(435814.926 7216265.06,435831.514 7216278.067,435882.859 7216318.374,435913.494 7216343.096,435933.284 7216359.684,435955.88 7216379.433,435964.681 7216387.155,435971.711 7216393.265,435977.718 7216401.542,435988.686 7216410.81,436002.33 7216422.184,436016.666 7216433.672,436065.593 7216472.418,436104.501 7216502.842,436160.723 7216547.196,436209.97 7216586.139,436292.323 7216651.574,436337.442 7216687.563,436395.232 7216733.536,436432.823 7216763.106,436446.197 7216773.706,436458.223 7216783.135,436478.585 7216794.933,436498.412 7216809.444,436514.993 7216822.11,436536.865 7216839.524,436564.462 7216861.435,436582.744 7216875.971,436594.571 7216885.221,436598.84 7216888.665,436602.499 7216891.786,436614.265 7216901.032,436631.719 7216915.289,436639.454 7216921.416,436661.409 7216938.805,436694.632 7216965.811,436717.157 7216984.901,436737.352 7217003.218,436744.998 7217013.206,436755.827 7217022.713,436772.791 7217037.094,436782.526 7217046.045,436806.153 7217068.493,436823.251 7217085.785,436844.041 7217107.96,436871.24 7217138.923,436899.022 7217173.794,436925.432 7217210.012,436946.228 7217240.838,436963.35 7217268.069,436980.499 7217297.793,436997.529 7217330.354,437019.23 7217373.29,437034.825 7217405.35,437046.539 7217429.905,437060.972 7217461.04,437069.892 7217480.878,437116.31 7217579.035,437147.248 7217644.585,437174.693 7217702.54,437210.456 7217779.741,437234.712 7217831.348,437253.953 7217873.174,437275.063 7217918.109,437305.424 7217982.899,437335.755 7218046.909,437363.14 7218106.933,437392.187 7218166.988,437402.761 7218188.564,437403.866 7218190.791,437405.145 7218193.273,437443.018 7218273.705,437464.964 7218320.728,437491.989 7218377.087,437505.594 7218406.055,437516.655 7218430.491,437525.595 7218440.578,437531.65 7218451.989,437540.555 7218469.772,437562.925 7218516.988,437565.726 7218522.713,437568.802 7218528.371,437577.365 7218546.72,437595.734 7218586.244,437607.915 7218613.574,437614.594 7218630.456,437617.622 7218645.349,437628.479 7218667.837,437641.09 7218694.703,437656.535 7218728,437684.977 7218788.078,437710.757 7218843.145,437720.735 7218864.446,437744.745 7218915.966,437760.925 7218950.163,437770.926 7218963.409,437776.787 7218973.924,437783.987 7218987.984,437799.498 7219019.611,437805.561 7219031.908,437810.415 7219042.322,437818.47 7219059.296,437826.342 7219076.491,437829.919 7219084.287,437843.601 7219114.492,437863.715 7219159.727,437884.138 7219205.367,437898.462 7219236.113,437920.305 7219283.006,437935.089 7219314.789,437950.093 7219347.034,437972.926 7219395.522,438019.499 7219491.132,438054.911 7219563.811,438063.926 7219582.388,438066.406 7219587.238,438068.984 7219592.616,438070.088 7219595.047,438071.207 7219597.694,438074.848 7219605.072,438097.107 7219652.578,438104.557 7219668.511,438111.209 7219683.404,438117.627 7219698.443,438119.49 7219703.068,438120.738 7219713.614,438134.662 7219743.432,438149.997 7219776.159,438165.713 7219809.522,438186.205 7219852.675,438208.081 7219896.24,438223.778 7219925.253,438230.433 7219937.615,438250.464 7219971.477,438274.714 7220009.473,438308.037 7220058.078,438342.567 7220106.533,438380.278 7220158.124,438421.433 7220214.804,438457.854 7220264.845,438499.73 7220322.504,438519.159 7220348.763,438524.475 7220357.578,438525.309 7220358.796,438542.58 7220381.111,438582.177 7220435.373,438617.314 7220482.092,438651.842 7220524.726,438676.593 7220554.256,438706.971 7220585.281,438733.44 7220610.93,438772.4 7220646.153,438807.798 7220675.007,438838.419 7220697.892,438838.873 7220698.231,438862.547 7220714.203,438866.854 7220717.005,438867.249 7220717.267,438923.016 7220752.312,438972.468 7220780.374,439016.146 7220805.61,439082.66 7220843.312),(439082.66 7220843.312,439083.527890523 7220843.80875551))')) :: tr_osoite,
  'Kuusamontie',
  'Oulu, Kiiminki',
  'Kuusamontien alussa',
  'Jossain Kiimingissä',
  '2017-01-01 01:01:01',
  '2017-07-07 07:07:07',
  ARRAY[
    ROW('08:00:00'::TIME, '17:00:00'::TIME,
        ARRAY['maanantai','tiistai','keskiviikko']::viikonpaiva[])::tietyon_tyoaika,
    ROW('07:00:00'::TIME, '21:00:00'::TIME,
        ARRAY['lauantai','sunnuntai']::viikonpaiva[])::tietyon_tyoaika
  ]::tietyon_tyoaika[],
  'molemmat',
  ROW ('ajokaistaSuljettu', NULL) :: tietyon_kaistajarjestelyt,
  ARRAY ['(30, 100)'] :: tietyon_nopeusrajoitus [],
  ARRAY ['(paallystetty, 100)'] :: tietyon_tienpinta [],
  666,
  'loivatMutkat',
  ARRAY ['(murske, 100)'] :: tietyon_tienpinta [],
  'ohjataanVuorotellen',
  'liikennevalot',
  15,
  30,
  ROW (4, 3, 10, 4000) :: tietyon_ajoneuvorajoitukset,
  '{avotuli}',
  TRUE,
  TRUE,
  '2017-01-01 01:01:01',
  '2017-07-07 07:07:07',
  'Tämä on testi-ilmoitus',
  (SELECT id
   FROM organisaatio
   WHERE nimi ILIKE '% YIT Rakennus Oy %'),
  (SELECT nimi
   FROM organisaatio
   WHERE nimi ILIKE '%YIT Rakennus Oy%')),

   (NULL,
  NOW(),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: tietyon_henkilo,
  (SELECT id
   FROM urakka
   WHERE nimi = 'Rovaniemen MHU testiurakka (1. hoitovuosi)'),
  (SELECT nimi
   FROM urakka
   WHERE nimi = 'Rovaniemen MHU testiurakka (1. hoitovuosi)'),
  (SELECT tyyppi
   FROM urakka
   WHERE nimi = 'Rovaniemen MHU testiurakka (1. hoitovuosi)'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: tietyon_henkilo,
  (SELECT id
   FROM organisaatio
   WHERE lyhenne = 'LAP'),
  (SELECT nimi
   FROM organisaatio
   WHERE lyhenne = 'LAP'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com')) :: tietyon_henkilo,
  '{"(Tienrakennus,Rakennetaan tietä)"}',
  ROW (4, 501, 183, 501, 312,
  ST_GeomFromText(
      'MULTILINESTRING((443024.62635065464 7376285.663026805,443029.15299999993 7376291.609000001,443050.64499999955 7376318.767999999,443074.1830000002 7376347.986000001,443104.09300000034 7376385.761999998,443104.83108483226 7376386.695368251))')) :: tr_osoite,
  'Valtatie 4',
  'Rovaniemi, keskusta',
  'Keskustassa',
  'Keskustassa',
  NOW(),
  NOW() + interval '7 days',
  ARRAY[
    ROW('08:00:00'::TIME, '17:00:00'::TIME,
        ARRAY['maanantai','tiistai','keskiviikko']::viikonpaiva[])::tietyon_tyoaika,
    ROW('07:00:00'::TIME, '21:00:00'::TIME,
        ARRAY['lauantai','sunnuntai']::viikonpaiva[])::tietyon_tyoaika
  ]::tietyon_tyoaika[],
  'molemmat',
  ROW ('ajokaistaSuljettu', NULL) :: tietyon_kaistajarjestelyt,
  ARRAY ['(30, 100)'] :: tietyon_nopeusrajoitus [],
  ARRAY ['(paallystetty, 100)'] :: tietyon_tienpinta [],
  666,
  'loivatMutkat',
  ARRAY ['(murske, 100)'] :: tietyon_tienpinta [],
  'ohjataanVuorotellen',
  'liikennevalot',
  15,
  30,
  ROW (4, 3, 10, 4000) :: tietyon_ajoneuvorajoitukset,
  '{avotuli}',
  TRUE,
  TRUE,
  NOW(),
  NOW() + interval '7 days',
  'Tämä on testi-ilmoitus',
  (SELECT id
   FROM organisaatio
   WHERE nimi ILIKE '% YIT Rakennus Oy %'),
  (SELECT nimi
   FROM organisaatio
   WHERE nimi ILIKE '%YIT Rakennus Oy%'));

-- Luodaan lapsi-ilmoitus (työvaiheilmoitus) edelliselle
INSERT INTO tietyoilmoitus (paatietyoilmoitus,
                            luotu,
                            luoja,
                            "ilmoittaja-id",
                            ilmoittaja,
                            "urakka-id",
                            "urakan-nimi",
                            urakkatyyppi,
                            "urakoitsijayhteyshenkilo-id",
                            urakoitsijayhteyshenkilo,
                            "tilaaja-id",
                            "tilaajan-nimi",
                            "tilaajayhteyshenkilo-id",
                            tilaajayhteyshenkilo,
                            tyotyypit,
                            osoite,
                            "tien-nimi",
                            kunnat,
                            "alkusijainnin-kuvaus",
                            "loppusijainnin-kuvaus",
                            alku,
                            loppu,
                            tyoajat,
                            vaikutussuunta,
                            kaistajarjestelyt,
                            nopeusrajoitukset,
                            tienpinnat,
                            "kiertotien-pituus",
                            "kiertotien-mutkaisuus",
                            kiertotienpinnat,
                            liikenteenohjaus,
                            liikenteenohjaaja,
                            "viivastys-normaali-liikenteessa",
                            "viivastys-ruuhka-aikana",
                            ajoneuvorajoitukset,
                            huomautukset,
                            "ajoittaiset-pysaytykset",
                            "ajoittain-suljettu-tie",
                            "pysaytysten-alku",
                            "pysaytysten-loppu",
                            lisatietoja,
                            "urakoitsija-id",
                            "urakoitsijan-nimi",
                            "urakoitsijan-ytunnus")

VALUES (1,
  '2017-01-01 01:01:01',
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: TIETYON_HENKILO,
  (SELECT id
   FROM urakka
   WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT nimi
   FROM urakka
   WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT tyyppi
   FROM urakka
   WHERE nimi = 'Oulun alueurakka 2014-2019'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: TIETYON_HENKILO,
  (SELECT id
   FROM organisaatio
   WHERE lyhenne = 'POP'),
  (SELECT nimi
   FROM organisaatio
   WHERE lyhenne = 'POP'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com')) :: TIETYON_HENKILO,
  '{"(Tienrakennus,Rakennetaan tietä)"}',
  ROW (20, 3, 1, 4, 1,
  ST_GeomFromText(
      'MULTILINESTRING((430651.697487601 7212579.38600741,430667.369 7212589.979,430697.85 7212610.534,430797.842 7212680.199,430825.791 7212700.691,430831.32 7212704.529,430897.841 7212750.706,430928.359 7212773.044,430970.265 7212802.533,431004.767 7212826.58,431036.589 7212848.205,431081.499 7212879.362,431120.673 7212907.094,431176.235 7212944.988,431190.372 7212954.377,431212.991 7212970.698,431217.443 7212973.958,431227.922 7212981.794,431236.641 7212988.003,431279.488 7213019.255,431350.784 7213069.93,431397.511 7213102.948,431418.022 7213118.304,431426.405 7213124.78,431449.031 7213142.281,431486.141 7213172.539,431553.474 7213237.334,431607.221 7213289.507,431635.956 7213316.213,431669.835 7213343.386,431693.874 7213361.536,431733.887 7213390.148,431753.04 7213402.801,431762.396 7213408.575,431772.326 7213415.306,431789.594 7213425.862,431813.253 7213439.663,431851.74 7213459.513,431873.292 7213470.678,431884.732 7213476.398,431908.326 7213488.212,431941.407 7213504.965,431994.207 7213532.956,432023.093 7213549.134,432058.46 7213571.444,432079.672 7213584.434,432091.457 7213591.643,432122.832 7213610.355,432192.972 7213660.585,432263.565 7213710.363,432335.515 7213760.366,432381.219 7213793.853,432410.905 7213815.438,432427.905 7213827.798,432447.633 7213841.183,432497.064 7213874.629,432551.818 7213914.903,432622.411 7213965.36,432674.903 7214000.883,432817.307 7214101.146,432836.94 7214115.889,432859.94 7214132.173,432871.042 7214139.798,432909.88 7214167.229,432921.607 7214175.483,433104.142 7214303.024,433129.232 7214321.875,433148.142 7214335.547,433167.826 7214350.352,433211.796 7214381.221,433256.743 7214413.035,433296.003 7214440.18,433336.211 7214468.001,433425.744 7214531.781,433510.836 7214588.786,433514.481 7214591.228,433534.998 7214606.277,433544.311 7214613.133,433555.492 7214620.803,433564.971 7214627.401,433597.888 7214651.456,433673.219 7214705.867,433736.372 7214753.582,433765.25 7214775.182,433777.029 7214785.107,433867.398 7214857.158,433907.517 7214889.398,433929.686 7214907.278,433959.329 7214932.265,433991.308 7214956.166,434016.683 7214973.688,434038.098 7214989.478,434067.032 7215010.574,434087.56 7215024.542,434103.069 7215035.075,434115.06 7215043.587,434134.399 7215057.149,434146.494 7215064.844,434174.836 7215083.39,434212.271 7215107.757,434268.175 7215143.976,434328.583 7215183.239,434377.12 7215214.632,434408.777 7215234.947,434473.151 7215277.016,434517.023 7215305.215,434527.14 7215311.809,434542.767 7215321.988,434557.061 7215331.505,434593.051 7215355.736,434595.092 7215357.149,434640.866 7215388.847,434685.73 7215421.797,434687.589 7215423.176,434742.519 7215463.945,434764.109 7215480.302,434776.606 7215490.381,434784.224 7215501.513,434809.379 7215521.085,434838.862 7215543.856,434874.732 7215571.144,434887.912 7215581.155,434912.224 7215598.388,434930.88 7215612.225,434937.857 7215617.28,434951.95 7215627.667,434959.62 7215632.997,434975.32 7215644.318,434996.004 7215659.035,435041.619 7215692.904,435113.127 7215744.905,435177.465 7215791.14,435211.304 7215815.896,435234.269 7215832.152,435265.382 7215854.873,435283.704 7215867.728,435305.533 7215883.891,435342.579 7215910.822,435363.897 7215926.319,435402.991 7215954.8,435450.239 7215990.493,435497.814 7216027.27,435536.002 7216057.033,435591.586 7216100.312,435609.238 7216113.819,435622.811 7216123.637,435634.907 7216128.915,435652.899 7216140.833,435676.68 7216157.188,435699.523 7216174.213,435745.287 7216209.513,435760.104 7216221.192,435773.455 7216231.416,435788.098 7216242.903,435805.564 7216257.586,435810.622 7216261.513,435814.926 7216265.06),(435814.926 7216265.06,435831.514 7216278.067,435882.859 7216318.374,435913.494 7216343.096,435933.284 7216359.684,435955.88 7216379.433,435964.681 7216387.155,435971.711 7216393.265,435977.718 7216401.542,435988.686 7216410.81,436002.33 7216422.184,436016.666 7216433.672,436065.593 7216472.418,436104.501 7216502.842,436160.723 7216547.196,436209.97 7216586.139,436292.323 7216651.574,436337.442 7216687.563,436395.232 7216733.536,436432.823 7216763.106,436446.197 7216773.706,436458.223 7216783.135,436478.585 7216794.933,436498.412 7216809.444,436514.993 7216822.11,436536.865 7216839.524,436564.462 7216861.435,436582.744 7216875.971,436594.571 7216885.221,436598.84 7216888.665,436602.499 7216891.786,436614.265 7216901.032,436631.719 7216915.289,436639.454 7216921.416,436661.409 7216938.805,436694.632 7216965.811,436717.157 7216984.901,436737.352 7217003.218,436744.998 7217013.206,436755.827 7217022.713,436772.791 7217037.094,436782.526 7217046.045,436806.153 7217068.493,436823.251 7217085.785,436844.041 7217107.96,436871.24 7217138.923,436899.022 7217173.794,436925.432 7217210.012,436946.228 7217240.838,436963.35 7217268.069,436980.499 7217297.793,436997.529 7217330.354,437019.23 7217373.29,437034.825 7217405.35,437046.539 7217429.905,437060.972 7217461.04,437069.892 7217480.878,437116.31 7217579.035,437147.248 7217644.585,437174.693 7217702.54,437210.456 7217779.741,437234.712 7217831.348,437253.953 7217873.174,437275.063 7217918.109,437305.424 7217982.899,437335.755 7218046.909,437363.14 7218106.933,437392.187 7218166.988,437402.761 7218188.564,437403.866 7218190.791,437405.145 7218193.273,437443.018 7218273.705,437464.964 7218320.728,437491.989 7218377.087,437505.594 7218406.055,437516.655 7218430.491,437525.595 7218440.578,437531.65 7218451.989,437540.555 7218469.772,437562.925 7218516.988,437565.726 7218522.713,437568.802 7218528.371,437577.365 7218546.72,437595.734 7218586.244,437607.915 7218613.574,437614.594 7218630.456,437617.622 7218645.349,437628.479 7218667.837,437641.09 7218694.703,437656.535 7218728,437684.977 7218788.078,437710.757 7218843.145,437720.735 7218864.446,437744.745 7218915.966,437760.925 7218950.163,437770.926 7218963.409,437776.787 7218973.924,437783.987 7218987.984,437799.498 7219019.611,437805.561 7219031.908,437810.415 7219042.322,437818.47 7219059.296,437826.342 7219076.491,437829.919 7219084.287,437843.601 7219114.492,437863.715 7219159.727,437884.138 7219205.367,437898.462 7219236.113,437920.305 7219283.006,437935.089 7219314.789,437950.093 7219347.034,437972.926 7219395.522,438019.499 7219491.132,438054.911 7219563.811,438063.926 7219582.388,438066.406 7219587.238,438068.984 7219592.616,438070.088 7219595.047,438071.207 7219597.694,438074.848 7219605.072,438097.107 7219652.578,438104.557 7219668.511,438111.209 7219683.404,438117.627 7219698.443,438119.49 7219703.068,438120.738 7219713.614,438134.662 7219743.432,438149.997 7219776.159,438165.713 7219809.522,438186.205 7219852.675,438208.081 7219896.24,438223.778 7219925.253,438230.433 7219937.615,438250.464 7219971.477,438274.714 7220009.473,438308.037 7220058.078,438342.567 7220106.533,438380.278 7220158.124,438421.433 7220214.804,438457.854 7220264.845,438499.73 7220322.504,438519.159 7220348.763,438524.475 7220357.578,438525.309 7220358.796,438542.58 7220381.111,438582.177 7220435.373,438617.314 7220482.092,438651.842 7220524.726,438676.593 7220554.256,438706.971 7220585.281,438733.44 7220610.93,438772.4 7220646.153,438807.798 7220675.007,438838.419 7220697.892,438838.873 7220698.231,438862.547 7220714.203,438866.854 7220717.005,438867.249 7220717.267,438923.016 7220752.312,438972.468 7220780.374,439016.146 7220805.61,439082.66 7220843.312),(439082.66 7220843.312,439083.527890523 7220843.80875551))')) :: TR_OSOITE,
  'Kuusamontie',
  'Oulu, Kiiminki',
  'Vaalantien risteys',
  'Ylikiimingintien risteys',
  '2017-06-01 01:01:01',
  '2017-06-20 07:07:07',
  ARRAY [
    ROW ('06:00:00' :: TIME, '18:15:00' :: TIME,
    ARRAY ['maanantai', 'tiistai', 'keskiviikko'] :: VIIKONPAIVA []) :: TIETYON_TYOAIKA,
    ROW ('20:00:00' :: TIME, '23:00:00' :: TIME,
    ARRAY ['lauantai', 'sunnuntai'] :: VIIKONPAIVA []) :: TIETYON_TYOAIKA
  ] :: TIETYON_TYOAIKA [],
  'molemmat',
  ROW ('ajokaistaSuljettu', NULL) :: TIETYON_KAISTAJARJESTELYT,
  ARRAY ['(30, 100)'] :: TIETYON_NOPEUSRAJOITUS [],
  ARRAY ['(paallystetty, 100)'] :: TIETYON_TIENPINTA [],
  123,
  'jyrkatMutkat',
  ARRAY ['(murske, 100)'] :: TIETYON_TIENPINTA [],
  'ohjataanVuorotellen',
  'liikennevalot',
  15,
  30,
  ROW (4, 3, 10, 4000) :: TIETYON_AJONEUVORAJOITUKSET,
  '{avotuli}',
  TRUE,
  TRUE,
  '2017-01-01 01:01:01',
  '2017-07-07 07:07:07',
  'Tämä on testi-ilmoitus',
        (SELECT id
         FROM organisaatio
         WHERE nimi ILIKE '% YIT Rakennus Oy %'),
        (SELECT nimi
         FROM organisaatio
         WHERE nimi ILIKE '%YIT Rakennus Oy%'),
        (SELECT ytunnus
         FROM organisaatio
         WHERE nimi ILIKE '%YIT Rakennus Oy%')),
   (2,
  NOW(),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: TIETYON_HENKILO,
  (SELECT id
   FROM urakka
   WHERE nimi = 'Rovaniemen MHU testiurakka (1. hoitovuosi)'),
  (SELECT nimi
   FROM urakka
   WHERE nimi = 'Rovaniemen MHU testiurakka (1. hoitovuosi)'),
  (SELECT tyyppi
   FROM urakka
   WHERE nimi = 'Rovaniemen MHU testiurakka (1. hoitovuosi)'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'yit_pk2@example.org')) :: TIETYON_HENKILO,
  (SELECT id
   FROM organisaatio
   WHERE lyhenne = 'LAP'),
  (SELECT nimi
   FROM organisaatio
   WHERE lyhenne = 'LAP'),
  (SELECT id
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  ROW ((SELECT etunimi
        FROM kayttaja
        WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sukunimi
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT puhelin
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com'),
  (SELECT sahkoposti
   FROM kayttaja
   WHERE sahkoposti = 'tero.toripolliisi@example.com')) :: TIETYON_HENKILO,
  '{"(Tienrakennus,Rakennetaan tietä)"}',
  ROW (4, 501, 183, 501, 312,
  ST_GeomFromText(
      'MULTILINESTRING((443024.62635065464 7376285.663026805,443029.15299999993 7376291.609000001,443050.64499999955 7376318.767999999,443074.1830000002 7376347.986000001,443104.09300000034 7376385.761999998,443104.83108483226 7376386.695368251))')) :: tr_osoite,
  'Valtatie 4',
  'Rovaniemi, keskusta',
  'Keskusta',
  'Keskusta',
  NOW(),
  NOW() + interval '7 days',
  ARRAY [
    ROW ('06:00:00' :: TIME, '18:15:00' :: TIME,
    ARRAY ['maanantai', 'tiistai', 'keskiviikko'] :: VIIKONPAIVA []) :: TIETYON_TYOAIKA,
    ROW ('20:00:00' :: TIME, '23:00:00' :: TIME,
    ARRAY ['lauantai', 'sunnuntai'] :: VIIKONPAIVA []) :: TIETYON_TYOAIKA
  ] :: TIETYON_TYOAIKA [],
  'molemmat',
  ROW ('ajokaistaSuljettu', NULL) :: TIETYON_KAISTAJARJESTELYT,
  ARRAY ['(30, 100)'] :: TIETYON_NOPEUSRAJOITUS [],
  ARRAY ['(paallystetty, 100)'] :: TIETYON_TIENPINTA [],
  123,
  'jyrkatMutkat',
  ARRAY ['(murske, 100)'] :: TIETYON_TIENPINTA [],
  'ohjataanVuorotellen',
  'liikennevalot',
  15,
  30,
  ROW (4, 3, 10, 4000) :: TIETYON_AJONEUVORAJOITUKSET,
  '{avotuli}',
  TRUE,
  TRUE,
  NOW(),
  NOW() + interval '7 days',
  'Tämä on testi-ilmoitus',
        (SELECT id
         FROM organisaatio
         WHERE nimi ILIKE '% YIT Rakennus Oy %'),
        (SELECT nimi
         FROM organisaatio
         WHERE nimi ILIKE '%YIT Rakennus Oy%'),
        (SELECT ytunnus
         FROM organisaatio
         WHERE nimi ILIKE '%YIT Rakennus Oy%'));

INSERT INTO tietyoilmoituksen_email_lahetys
(tietyoilmoitus, tiedostonimi, lahetetty, lahetysid, lahettaja, kuitattu)
    VALUES
      ((SELECT id FROM tietyoilmoitus WHERE "alkusijainnin-kuvaus" = 'Kuusamontien alussa'),
      'Tietyöilmoitus-22.05.2018-26.05.2018-Kuusamontie',
        '2018-05-19 11:01:01', 'testi-jms-id-124321-523-523523-235-2353',
       (SELECT id FROM kayttaja WHERE kayttajanimi = 'jvh'),
       '2018-05-19 11:02:06'),
      ((SELECT id FROM tietyoilmoitus WHERE "alkusijainnin-kuvaus" = 'Kuusamontien alussa'),
       'Tietyöilmoitus-24.05.2018-28.05.2018-Kuusamontie',
       '2018-05-21 12:01:01', 'testi-jms-id-124321-523-523523-235-2354',
       (SELECT id FROM kayttaja WHERE kayttajanimi = 'jvh'),
       '2018-05-21 12:02:16');