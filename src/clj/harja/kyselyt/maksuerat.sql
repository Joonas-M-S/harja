-- name: hae-urakan-maksuerat
-- Hakee id:n perusteella maksueran lähettämiseen tarvittavat tiedot
-- FIXME: miten haetaan summat?
SELECT
  m.numero,
  m.tyyppi,
  m.nimi,
  m.tila,
  m.lahetetty,
  tpi.id                                      AS toimenpideinstanssi_id,
  tpi.nimi                                    AS toimenpideinstanssi_nimi,
  tpi.alkupvm                                 AS toimenpideinstanssi_alkupvm,
  tpi.loppupvm                                AS toimenpideinstanssi_loppupvm,
  s.sampoid                                   AS sopimus_sampoid,
  k.tila                                      AS kustannussuunnitelma_tila,
  k.lahetetty                                 AS kustannussuunnitelma_lahetetty,
  (SELECT SUM(yht.maara * yht.yksikkohinta)
   FROM yksikkohintainen_tyo yht
   WHERE yht.tehtava IN (SELECT id
                         FROM toimenpidekoodi
                         WHERE emo = tpk.id)) AS yksikkohintaisettyot_summa,
  (SELECT SUM(kht.summa)
   FROM kokonaishintainen_tyo kht
   WHERE kht.toimenpideinstanssi = tpi.id)    AS kokonaishintaisettyot_summa
FROM maksuera m
  JOIN toimenpideinstanssi tpi ON tpi.id = m.toimenpideinstanssi
  JOIN urakka u ON u.id = tpi.urakka
  JOIN sopimus s ON s.urakka = u.id AND s.paasopimus IS NULL
  JOIN kustannussuunnitelma k ON m.numero = k.maksuera
  JOIN toimenpidekoodi tpk ON tpi.toimenpide = tpk.id
WHERE tpi.urakka = :urakkaid;


-- name: hae-lahetettava-maksuera
-- Hakee numeron perusteella maksueran lähettämiseen tarvittavat tiedot
SELECT
  m.numero,
  m.tyyppi,
  m.nimi,
  m.tila,
  m.lahetetty,
  tpi.id                                      AS toimenpideinstanssi_id,
  tpi.nimi                                    AS toimenpideinstanssi_nimi,
  tpi.alkupvm                                 AS toimenpideinstanssi_alkupvm,
  tpi.loppupvm                                AS toimenpideinstanssi_loppupvm,
  tpi.tuotepolku                              AS toimenpideinstanssi_tuotepolku,
  s.sampoid                                   AS sopimus_sampoid,
  u.sampoid                                   AS urakka_sampoid,
  k.tila                                      AS kustannussuunnitelma_tila,
  k.lahetetty                                 AS kustannussuunnitelma_lahetetty,
  (SELECT SUM(yht.maara * yht.yksikkohinta)
   FROM yksikkohintainen_tyo yht
   WHERE yht.tehtava IN (SELECT id
                         FROM toimenpidekoodi
                         WHERE emo = tpk.id)) AS yksikkohintaisettyot_summa,
  (SELECT SUM(kht.summa)
   FROM kokonaishintainen_tyo kht
   WHERE kht.toimenpideinstanssi = tpi.id)    AS kokonaishintaisettyot_summa,
  (SELECT emo.tuotenumero
   FROM toimenpidekoodi emo
   WHERE emo.id = tpk.emo)                    AS tuotenumero
FROM maksuera m
  JOIN toimenpideinstanssi tpi ON tpi.id = m.toimenpideinstanssi
  JOIN urakka u ON u.id = tpi.urakka
  JOIN sopimus s ON s.urakka = u.id AND s.paasopimus IS NULL
  JOIN kustannussuunnitelma k ON m.numero = k.maksuera
  JOIN toimenpidekoodi tpk ON tpi.toimenpide = tpk.id
WHERE m.numero = :numero;

-- name: lukitse-maksuera!
-- Lukitsee maksuerän lähetyksen ajaksi
UPDATE maksuera
SET lukko = :lukko, lukittu = current_timestamp
WHERE numero = :numero AND (lukko IS NULL OR
                            (EXTRACT(EPOCH FROM (current_timestamp - lukittu)) > 300));

-- name: hae-maksueranumero-lahetys-idlla
-- Hakee maksueranumeron lahetys-id:llä
SELECT numero
FROM maksuera
WHERE lahetysid = :lahetysid;

-- name: merkitse-maksuera-odottamaan-vastausta!
-- Merkitsee maksuerän lähetetyksi, kirjaa lähetyksen id:n ja avaa lukon
UPDATE maksuera
SET lahetysid = :lahetysid, lukko = NULL, tila = 'odottaa_vastausta'
WHERE numero = :numero;

-- name: merkitse-maksuera-lahetetyksi!
-- Merkitsee maksuerän lähetetyksi, kirjaa lähetyksen id:n ja avaa lukon
UPDATE maksuera
SET lahetetty = current_timestamp, tila = 'lahetetty'
WHERE numero = :numero;

-- name: merkitse-maksueralle-lahetysvirhe!
-- Merkitsee maksuerän lähetetyksi, kirjaa lähetyksen id:n ja avaa lukon
UPDATE maksuera
SET tila = 'virhe'
WHERE numero = :numero;
