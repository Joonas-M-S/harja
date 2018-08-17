-- name: hae-ymparistoraportti-tiedot
-- Haetaan kuinka paljon jokaista materiaalia on käytetty. Tämä on "summarivi" hoitoluokittaisille riveille,
-- lisäksi tälle riville otetaan mukaan frontin kautta raportoidut käytöt, jolle ei ole hoitoluokkatietoa.
SELECT
  u.id AS urakka_id,
  u.nimi AS urakka_nimi,
  NULL AS luokka,
  mk.id AS materiaali_id,
  mk.nimi AS materiaali_nimi,
  mk.yksikko AS materiaali_yksikko,
  mk.materiaalityyppi AS materiaali_tyyppi,
  date_trunc('month', skm.alkupvm) AS kk,
  SUM(skm.maara) AS maara
FROM sopimuksen_kaytetty_materiaali skm
  JOIN sopimus s ON skm.sopimus = s.id
  JOIN urakka u ON s.urakka = u.id AND u.urakkanro IS NOT NULL
  JOIN materiaalikoodi mk ON skm.materiaalikoodi = mk.id
WHERE (:urakka::INTEGER IS NULL OR u.id = :urakka)
      AND (:hallintayksikko::INTEGER IS NULL OR u.hallintayksikko = :hallintayksikko)
      AND (skm.alkupvm::DATE BETWEEN :alkupvm AND :loppupvm)
      AND (:urakkatyyppi::urakkatyyppi IS NULL OR u.tyyppi = :urakkatyyppi::urakkatyyppi)
GROUP BY u.id, u.nimi, mk.id, mk.nimi, mk.materiaalityyppi, mk.yksikko, date_trunc('month', skm.alkupvm)
UNION
-- Haetaan hoitoluokittaiset käytöt urakan_materiaalin_kaytto_hoitoluokittain taulusta.
SELECT
  u.id AS urakka_id,
  u.nimi AS urakka_nimi,
  COALESCE(umkh.talvihoitoluokka, 100) AS luokka, -- Jos hoitoluokkaa ei ole, asetetaan 100 = Ei tiedossa
  mk.id AS materiaali_id,
  mk.nimi AS materiaali_nimi,
  mk.yksikko AS materiaali_yksikko,
  mk.materiaalityyppi AS materiaali_tyyppi,
  date_trunc('month', umkh.pvm) AS kk,
  SUM(umkh.maara) AS maara
FROM urakka u
  JOIN urakan_materiaalin_kaytto_hoitoluokittain umkh ON u.id = umkh.urakka
  JOIN materiaalikoodi mk ON mk.id = umkh.materiaalikoodi
WHERE (:urakka::INTEGER IS NULL OR u.id = :urakka)
      AND (:hallintayksikko::INTEGER IS NULL OR u.hallintayksikko = :hallintayksikko)
      AND (umkh.pvm::DATE BETWEEN :alkupvm AND :loppupvm)
      AND (:urakkatyyppi::urakkatyyppi IS NULL OR u.tyyppi = :urakkatyyppi::urakkatyyppi)
GROUP BY u.id, u.nimi, mk.id, mk.nimi, mk.materiaalityyppi, date_trunc('month', umkh.pvm), umkh.talvihoitoluokka
UNION
-- Liitä lopuksi mukaan suunnittelutiedot. Kuukausi on null, josta myöhemmin
-- rivi tunnistetaan suunnittelutiedoksi.
SELECT
  u.id as urakka_id, u.nimi as urakka_nimi,
  NULL as luokka,
  mk.id as materiaali_id, mk.nimi as materiaali_nimi,
  mk.yksikko AS materiaali_yksikko,
  mk.materiaalityyppi AS materiaali_tyyppi,
  NULL as kk,
  SUM(s.maara) as maara
FROM materiaalin_kaytto s
  JOIN materiaalikoodi mk ON s.materiaali = mk.id
  JOIN urakka u ON s.urakka = u.id AND u.urakkanro IS NOT NULL
WHERE s.poistettu IS NOT TRUE
      AND (s.alkupvm, s.loppupvm) OVERLAPS (:alkupvm, :loppupvm)
      AND (:urakka::integer IS NULL OR s.urakka = :urakka)
      AND (:hallintayksikko::integer IS NULL OR u.hallintayksikko = :hallintayksikko)
      AND (:urakkatyyppi::urakkatyyppi IS NULL OR u.tyyppi = :urakkatyyppi::urakkatyyppi)
GROUP BY u.id, u.nimi, mk.id, mk.nimi, mk.yksikko, mk.materiaalityyppi;

-- name: hae-materiaalit
-- Hakee materiaali id:t ja nimet
-- Huomaa, että tämän kyselyn pitää palauttaa samat sarakkeet, kuin mitkä ympäristöraportissa haetaan
-- "materiaali_*" nimeen
SELECT id,nimi, yksikko, materiaalityyppi as tyyppi FROM materiaalikoodi;