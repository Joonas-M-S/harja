CREATE OR REPLACE FUNCTION validoi_hoitokauden_alkupvm(hk_alkupvm DATE)
    RETURNS VOID AS
$$
BEGIN
    IF (SELECT EXTRACT (DAY FROM hk_alkupvm) != 1) THEN
        RAISE EXCEPTION 'Kvartaalin alkupvm:n on oltava kuun ensimmäinen päivä.';
    END IF;
    IF (SELECT EXTRACT (MONTH FROM hk_alkupvm) NOT IN (1, 4, 7, 10)) THEN
        RAISE EXCEPTION 'Kvartaalin alkupvm:n kuukauden on oltava 1, 4, 7 tai 10.';
    END IF;
END
$$
    LANGUAGE plpgsql;



CREATE OR REPLACE FUNCTION luo_toteumataulun_partitio(alkupvm DATE, loppupvm DATE)
    RETURNS VOID AS
$$
DECLARE
    partitio text;
BEGIN
    IF (alkupvm = '0001-01-01') THEN  partitio := 'toteuma_ennen_20151001';
    ELSIF (loppupvm = '9999-12-31') THEN partitio :=  'toteuma_uudet';
    ELSE partitio :=  'toteuma_hk_' || TO_CHAR(alkupvm, 'YY') || '_' || TO_CHAR(loppupvm, 'YY');
    END IF;

    PERFORM validoi_hoitokauden_alkupvm(alkupvm);

    -- CREATE PARTITION TABLE, INHERITING FROM TOTEUMA
    EXECUTE 'CREATE TABLE IF NOT EXISTS ' || partitio ||
            ' ( CHECK( alkanut >= '''|| alkupvm || ''' AND alkanut < '''|| loppupvm ||''')) INHERITS (toteuma)';

    -- PRIMARY KEY SUBSTITUTE
    EXECUTE 'CREATE UNIQUE INDEX ' || partitio || '_id_idx ON ' || partitio || '(id)';

    -- OTHER INDEXES
    EXECUTE 'ALTER TABLE ' || partitio || ' ADD CONSTRAINT ' || partitio || '_uniikki_ulkoinen_id_luoja_urakka UNIQUE (ulkoinen_id, luoja, urakka)';
    EXECUTE 'CREATE INDEX ' || partitio || '_alkanut_idx ON ' || partitio || '(alkanut)';
    EXECUTE 'CREATE INDEX ' || partitio || '_urakka_idx ON ' || partitio || '(urakka)';
    EXECUTE 'CREATE INDEX ' || partitio || '_sopimus_idx ON ' || partitio || '(sopimus)';
    EXECUTE 'CREATE INDEX ' || partitio || '_tyyppi_urakka_alkanut_idx ON ' || partitio || '(tyyppi, urakka, alkanut)';
    EXECUTE 'CREATE INDEX ' || partitio || '_envelope_idx ON ' || partitio || ' USING GIST (envelope);';

    -- FOREIGN KEYS
    EXECUTE 'ALTER TABLE ' || partitio || ' ADD CONSTRAINT ' || partitio || '_luoja_fkey FOREIGN KEY (luoja) REFERENCES kayttaja (id);';
    EXECUTE 'ALTER TABLE ' || partitio || ' ADD CONSTRAINT ' || partitio || '_urakka_fkey FOREIGN KEY (urakka) REFERENCES urakka (id);';
    EXECUTE 'ALTER TABLE ' || partitio || ' ADD CONSTRAINT ' || partitio || '_sopimus_fkey FOREIGN KEY (sopimus) REFERENCES sopimus (id);';

END
$$
    LANGUAGE plpgsql;


-- Partitioi toteumataulu

ALTER TABLE toteuma rename to toteuma_vanha;

-- poistetaan FK rajoitteet osoittamasta toteuma_vanha tauluun, jotta voidaan tehdä datan siirto
ALTER TABLE toteuma_tehtava DROP CONSTRAINT toteuma_tehtava_toteuma_fkey;
ALTER TABLE toteuma_materiaali DROP CONSTRAINT toteuma_materiaali_toteuma_fkey;
ALTER TABLE varustetoteuma DROP CONSTRAINT varustetoteuma_toteuma_fkey;
ALTER TABLE toteuman_reittipisteet DROP CONSTRAINT toteuman_reittipisteet_toteuma_fkey;
ALTER TABLE toteuma_liite DROP CONSTRAINT toteuma_liite_toteuma_fkey;
ALTER TABLE paikkaustoteuma DROP CONSTRAINT "paikkaustoteuma_toteuma-id_fkey";

CREATE TABLE toteuma (LIKE toteuma_vanha INCLUDING ALL);

-- Lisätään uuteen toteumatauluun osoittavat FK:t ennen datan siirtoa
ALTER TABLE toteuma_tehtava
    ADD CONSTRAINT toteuma_tehtava_toteuma_fkey FOREIGN KEY (toteuma) REFERENCES toteuma(id) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE toteuma_materiaali
    ADD CONSTRAINT toteuma_materiaali_toteuma_fkey FOREIGN KEY (toteuma) REFERENCES toteuma(id) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE varustetoteuma
    ADD CONSTRAINT varustetoteuma_toteuma_fkey FOREIGN KEY (toteuma) REFERENCES toteuma(id) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE toteuman_reittipisteet
    ADD CONSTRAINT toteuman_reittipisteet_toteuma_fkey FOREIGN KEY (toteuma) REFERENCES toteuma(id) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE toteuma_liite
    ADD CONSTRAINT toteuma_liite_toteuma_fkey FOREIGN KEY (toteuma) REFERENCES toteuma(id) DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE paikkaustoteuma
    ADD CONSTRAINT "paikkaustoteuma_toteuma-id_fkey" FOREIGN KEY ("toteuma-id") REFERENCES toteuma(id) DEFERRABLE INITIALLY IMMEDIATE;

--SET CONSTRAINTS toteuma_tehtava_toteuma_fkey,
--    toteuma_materiaali_toteuma_fkey,
 --   varustetoteuma_toteuma_fkey,
  --  toteuman_reittipisteet_toteuma_fkey,
   -- toteuma_liite_toteuma_fkey,
    --"paikkaustoteuma_toteuma-id_fkey" DEFERRED;


-- ikivanhat, typotetut jne toteumat tänne
SELECT * FROM luo_toteumataulun_partitio( '0001-01-01'::DATE, '2015-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2015-10-01'::DATE, '2016-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2016-10-01'::DATE, '2017-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2017-10-01'::DATE, '2018-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2018-10-01'::DATE, '2019-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2019-10-01'::DATE, '2020-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2020-10-01'::DATE, '2021-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2021-10-01'::DATE, '2022-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2022-10-01'::DATE, '2023-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2023-10-01'::DATE, '2024-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2024-10-01'::DATE, '2025-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2025-10-01'::DATE, '2026-10-01'::DATE);
SELECT * FROM luo_toteumataulun_partitio( '2026-10-01'::DATE, '2027-10-01'::DATE);
-- tulevaisuuteen typotetut jne toteumat tänne
SELECT * FROM luo_toteumataulun_partitio( '2027-10-01'::DATE, '9999-12-31'::DATE);




  --WITH x AS (
  --    DELETE FROM toteuma_vanha WHERE alkanut BETWEEN '0001-01-01' AND ('2015-10-01') returning *
  --)
--INSERT INTO toteuma SELECT * FROM x;

  --WITH x AS (
  --    DELETE FROM toteuma_vanha WHERE alkanut BETWEEN '2016-10-01' AND '2017-10-01' returning *
 -- )
--INSERT INTO toteuma SELECT * FROM x;



-- Luo insert trigger
CREATE OR REPLACE FUNCTION toteuma_insert() RETURNS trigger AS $$
DECLARE
    alkanut date;
BEGIN
    alkanut := NEW.alkanut;
    IF alkanut < '2015-10-01'::date THEN
        INSERT INTO toteuma_ennen_20151001 VALUES (NEW.*);

    ELSIF alkanut >= '2015-10-01'::date AND alkanut < '2016-10-01'::date THEN
        INSERT INTO toteuma_hk_15_16 VALUES (NEW.*);
    ELSIF alkanut >= '2016-10-01'::date AND alkanut < '2017-10-01'::date THEN
        INSERT INTO toteuma_hk_16_17 VALUES (NEW.*);
    ELSIF alkanut >= '2017-10-01'::date AND alkanut < '2018-10-01'::date THEN
        INSERT INTO toteuma_hk_17_18 VALUES (NEW.*);
    ELSIF alkanut >= '2018-10-01'::date AND alkanut < '2019-10-01'::date THEN
        INSERT INTO toteuma_hk_18_19 VALUES (NEW.*);
    ELSIF alkanut >= '2019-10-01'::date AND alkanut < '2020-10-01'::date THEN
        INSERT INTO toteuma_hk_19_20 VALUES (NEW.*);
    ELSIF alkanut >= '2020-10-01'::date AND alkanut < '2021-10-01'::date THEN
        INSERT INTO toteuma_hk_20_21 VALUES (NEW.*);
    ELSIF alkanut >= '2021-10-01'::date AND alkanut < '2022-10-01'::date THEN
        INSERT INTO toteuma_hk_21_22 VALUES (NEW.*);
    ELSIF alkanut >= '2022-10-01'::date AND alkanut < '2023-10-01'::date THEN
        INSERT INTO toteuma_hk_22_23 VALUES (NEW.*);
    ELSIF alkanut >= '2023-10-01'::date AND alkanut < '2024-10-01'::date THEN
        INSERT INTO toteuma_hk_23_24 VALUES (NEW.*);
    ELSIF alkanut >= '2024-10-01'::date AND alkanut < '2025-10-01'::date THEN
        INSERT INTO toteuma_hk_24_25 VALUES (NEW.*);
    ELSIF alkanut >= '2025-10-01'::date AND alkanut < '2026-10-01'::date THEN
        INSERT INTO toteuma_hk_25_26 VALUES (NEW.*);
    ELSIF alkanut >= '2026-10-01'::date AND alkanut < '2027-10-01'::date THEN
        INSERT INTO toteuma_hk_26_27 VALUES (NEW.*);

    -- kaatissäkki kaikelle liian uudelle, typotetulle jne. Jos Harja elää 2027 pitempään, muuta
    -- tätä funktiota ja luo tarvittava määrä hoitokausipartitioita lisää
    ELSIF alkanut >= '2027-10-01'::date THEN
        INSERT INTO toteuma_uudet VALUES (NEW.*);  ELSE
        RAISE EXCEPTION 'Taululle toteuma ei löydy insert ehtoa, korjaa toteuma_insert() sproc!';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_toteuma_insert
    BEFORE INSERT ON toteuma
    FOR EACH ROW EXECUTE PROCEDURE toteuma_insert();

