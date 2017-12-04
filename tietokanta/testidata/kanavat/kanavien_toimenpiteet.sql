DO $$
DECLARE
  urakka_id_saimaan_kanava INTEGER := (SELECT id FROM urakka WHERE nimi = 'Saimaan kanava');
  sopimus_id_saimaan_paahuolto INTEGER := (SELECT id FROM sopimus WHERE nimi = 'Saimaan huollon pääsopimus');
  sopimus_id_saimaan_lisahuolto INTEGER := (SELECT id FROM sopimus WHERE nimi = 'Saimaan huollon lisäsopimus');
  kohde_id_palli INTEGER := (SELECT id FROM kan_kohde WHERE nimi = 'Pälli');
  kohdeosa_id_palli INTEGER := (SELECT id FROM kan_kohteenosa WHERE "kohde-id" = kohde_id_palli LIMIT 1);
  huoltokohde_id_asennonmittauslaitteet INTEGER := (SELECT id FROM kan_huoltokohde WHERE nimi = 'ASENNONMITTAUSLAITTEET');
  kayttaja_id_jvh INTEGER := (SELECT id FROM kayttaja WHERE kayttajanimi = 'jvh');
  toimenpidekoodi_id_vv_laaja_yksiloimaton INTEGER := (SELECT id FROM toimenpidekoodi WHERE emo = (SELECT id FROM toimenpidekoodi WHERE koodi = '24104') AND nimi = 'Ei yksilöity');
  tpk_id_saimaan_kok_hint_tp INTEGER := (SELECT id FROM toimenpideinstanssi WHERE nimi = 'Saimaan kanava, sopimukseen kuuluvat työt, TP');
  tyonjohto_tpk_id INTEGER := (SELECT id FROM toimenpidekoodi WHERE nimi = 'Henkilöstö: Työnjohto' AND emo =  (SELECT id FROM toimenpidekoodi WHERE koodi = '24104'));
BEGIN
INSERT INTO kan_toimenpide
(tyyppi,
 urakka,
 sopimus,
 pvm,
 "kohde-id",
 "kohteenosa-id",
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja,
 toimenpideinstanssi)
VALUES ('kokonaishintainen' :: KAN_TOIMENPIDETYYPPI,
  urakka_id_saimaan_kanava,
  sopimus_id_saimaan_paahuolto,
  '2017-10-10',
  kohde_id_palli,
  kohdeosa_id_palli,
  huoltokohde_id_asennonmittauslaitteet,
  toimenpidekoodi_id_vv_laaja_yksiloimaton,
  'Testitoimenpide',
  kayttaja_id_jvh,
  kayttaja_id_jvh,
  '2017-10-10',
  kayttaja_id_jvh,
  '2017-10-10',
  kayttaja_id_jvh,
  FALSE,
  NULL,
  tpk_id_saimaan_kok_hint_tp);

INSERT INTO kan_toimenpide
(tyyppi,
 urakka,
 sopimus,
 pvm,
 "kohde-id",
 "kohteenosa-id",
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja,
 toimenpideinstanssi)
VALUES ('kokonaishintainen' :: KAN_TOIMENPIDETYYPPI,
  urakka_id_saimaan_kanava,
  sopimus_id_saimaan_paahuolto,
  '2016-11-07',
  (SELECT id
   FROM kan_kohde
   WHERE nimi = 'Soskua'),
  NULL,
  huoltokohde_id_asennonmittauslaitteet,
  toimenpidekoodi_id_vv_laaja_yksiloimaton,
  'Testitoimenpide',
  kayttaja_id_jvh,
  kayttaja_id_jvh,
  '2016-11-07',
  kayttaja_id_jvh,
  '2016-11-07',
  kayttaja_id_jvh,
  FALSE,
  NULL,
  tpk_id_saimaan_kok_hint_tp);

INSERT INTO kan_toimenpide
(tyyppi,
 urakka,
 sopimus,
 pvm,
 "kohde-id",
 "kohteenosa-id",
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja,
 toimenpideinstanssi)
VALUES ('kokonaishintainen' :: KAN_TOIMENPIDETYYPPI,
  urakka_id_saimaan_kanava,
  sopimus_id_saimaan_lisahuolto,
  '2017-01-07',
  kohde_id_palli,
  NULL,
  huoltokohde_id_asennonmittauslaitteet,
  toimenpidekoodi_id_vv_laaja_yksiloimaton,
  'Testitoimenpide',
  kayttaja_id_jvh,
  kayttaja_id_jvh,
  '2017-01-07',
  kayttaja_id_jvh,
  '2017-01-07',
  kayttaja_id_jvh,
  FALSE,
  NULL,
  tpk_id_saimaan_kok_hint_tp);

INSERT INTO kan_toimenpide
(tyyppi,
 urakka,
 sopimus,
 pvm,
 "kohde-id",
 "kohteenosa-id",
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja,
 toimenpideinstanssi)
VALUES ('muutos-lisatyo' :: KAN_TOIMENPIDETYYPPI,
  urakka_id_saimaan_kanava,
  sopimus_id_saimaan_lisahuolto,
  '2017-01-07',
  kohde_id_palli,
  NULL,
  huoltokohde_id_asennonmittauslaitteet,
  toimenpidekoodi_id_vv_laaja_yksiloimaton,
  'Testitoimenpide',
  kayttaja_id_jvh,
  kayttaja_id_jvh,
  '2017-01-07',
   kayttaja_id_jvh,
   '2017-01-07',
   kayttaja_id_jvh,
   FALSE,
   NULL,
   tpk_id_saimaan_kok_hint_tp);

INSERT INTO kan_toimenpide
(tyyppi,
 urakka,
 sopimus,
 pvm,
 "kohde-id",
 "kohteenosa-id",
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja,
 toimenpideinstanssi)
VALUES ('muutos-lisatyo' :: KAN_TOIMENPIDETYYPPI,
  urakka_id_saimaan_kanava,
  sopimus_id_saimaan_paahuolto,
  '2017-01-11',
  kohde_id_palli,
  NULL,
  huoltokohde_id_asennonmittauslaitteet,
  toimenpidekoodi_id_vv_laaja_yksiloimaton,
  'Testitoimenpide',
  kayttaja_id_jvh,
  kayttaja_id_jvh,
  '2017-01-07',
  kayttaja_id_jvh,
  '2017-01-07',
  kayttaja_id_jvh,
  FALSE,
  NULL,
  tpk_id_saimaan_kok_hint_tp);

INSERT INTO kan_toimenpide
(tyyppi,
 urakka,
 sopimus,
 pvm,
 "kohde-id",
 "kohteenosa-id",
 huoltokohde,
 toimenpidekoodi,
 lisatieto,
 suorittaja,
 kuittaaja,
 luotu,
 luoja,
 muokattu,
 muokkaaja,
 poistettu,
 poistaja,
 toimenpideinstanssi)
VALUES ('muutos-lisatyo' :: KAN_TOIMENPIDETYYPPI,
  urakka_id_saimaan_kanava,
  sopimus_id_saimaan_paahuolto,
  '2017-01-12',
  kohde_id_palli,
  NULL,
  huoltokohde_id_asennonmittauslaitteet,
  toimenpidekoodi_id_vv_laaja_yksiloimaton,
  'Testitoimenpide',
  kayttaja_id_jvh,
  kayttaja_id_jvh,
  '2017-01-07',
  kayttaja_id_jvh,
  '2017-01-07',
  kayttaja_id_jvh,
  FALSE,
  NULL,
  tpk_id_saimaan_kok_hint_tp);

  INSERT INTO kan_toimenpide
  (tyyppi,
   urakka,
   sopimus,
   pvm,
   "kohde-id",
   "kohteenosa-id",
   huoltokohde,
   toimenpidekoodi,
   lisatieto,
   suorittaja,
   kuittaaja,
   luotu,
   luoja,
   muokattu,
   muokkaaja,
   poistettu,
   poistaja,
   toimenpideinstanssi)
  VALUES ('muutos-lisatyo' :: KAN_TOIMENPIDETYYPPI,
    urakka_id_saimaan_kanava,
    sopimus_id_saimaan_paahuolto,
    '2017-11-12',
    kohde_id_palli,
    NULL,
    huoltokohde_id_asennonmittauslaitteet,
    toimenpidekoodi_id_vv_laaja_yksiloimaton,
    'Testitoimenpide 20171112',
    kayttaja_id_jvh,
    kayttaja_id_jvh,
          '2017-01-07',
          kayttaja_id_jvh,
          '2017-01-07',
          kayttaja_id_jvh,
          FALSE,
          NULL,
          tpk_id_saimaan_kok_hint_tp);

  INSERT INTO kan_tyo (toimenpide, "toimenpidekoodi-id", maara, luoja)
  VALUES ((SELECT MAX(id) FROM kan_toimenpide),
      toimenpidekoodi_id_vv_laaja_yksiloimaton, 4, kayttaja_id_jvh);

  INSERT INTO toimenpidekoodi (nimi, taso, luotu, yksikko, suoritettavatehtava, hinnoittelu, emo)
    VALUES ('Henkilöstö: muutosaskare', 4, now(), 'h', NULL,
    '{yksikkohintainen}', (SELECT id FROM toimenpidekoodi WHERE koodi='24104'));
  INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, yksikko, yksikkohinta, tehtava, urakka, sopimus)
  VALUES ('2016-08-01', '2017-07-30', 'h', 41, (SELECT id FROM toimenpidekoodi WHERE nimi = 'Henkilöstö: muutosaskare'), urakka_id_saimaan_kanava, sopimus_id_saimaan_paahuolto),
         ('2017-08-01', '2018-07-30', 'h', 41, (SELECT id FROM toimenpidekoodi WHERE nimi = 'Henkilöstö: muutosaskare'), urakka_id_saimaan_kanava, sopimus_id_saimaan_paahuolto);

  INSERT INTO kan_tyo (toimenpide, "toimenpidekoodi-id", maara, luoja)
  VALUES ((SELECT id FROM kan_toimenpide WHERE lisatieto = 'Testitoimenpide 20171112'),
          (SELECT id FROM toimenpidekoodi WHERE nimi = 'Henkilöstö: muutosaskare'), 10, kayttaja_id_jvh);

  INSERT INTO kan_hinta (toimenpide, otsikko, yksikko, yksikkohinta, maara, luoja)
  VALUES ((SELECT id FROM kan_toimenpide WHERE lisatieto = 'Testitoimenpide 20171112'), 'Automies ja konevuokra', 'h', 200, 3, kayttaja_id_jvh);

  INSERT INTO kan_hinta (toimenpide, otsikko, summa, luoja)
  VALUES ((SELECT id FROM kan_toimenpide WHERE lisatieto = 'Testitoimenpide 20171112'), 'Muuta könttäsummasälää', 400, kayttaja_id_jvh);
  INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus)
  VALUES ('2017-08-01', '2018-07-31', 1, 'h', 45, tyonjohto_tpk_id, urakka_id_saimaan_kanava, sopimus_id_saimaan_paahuolto);
  INSERT INTO yksikkohintainen_tyo (alkupvm, loppupvm, maara, yksikko, yksikkohinta, tehtava, urakka, sopimus)
  VALUES ('2016-08-01', '2017-07-31', 1, 'h', 41, tyonjohto_tpk_id, urakka_id_saimaan_kanava, sopimus_id_saimaan_paahuolto);
  INSERT INTO kan_tyo (toimenpide, "toimenpidekoodi-id", maara, luoja) VALUES ((SELECT MAX(id) FROM kan_toimenpide),
    tyonjohto_tpk_id, 3, kayttaja_id_jvh);
END $$;

select * from kan_toimenpide ktp
left join kan_hinta h ON h.toimenpide = ktp.id
where ktp.id = 7;
