ALTER TABLE lukko
  ALTER COLUMN tunniste TYPE TEXT;

DROP FUNCTION aseta_lukko(tarkistettava_tunniste VARCHAR(30), uusi_lukko CHAR(36), aikaraja BIGINT );
DROP FUNCTION avaa_lukko(tarkistettava_tunniste VARCHAR(30) );

CREATE OR REPLACE FUNCTION aseta_lukko(tarkistettava_tunniste TEXT, uusi_lukko CHAR(36), aikaraja BIGINT)
  RETURNS BOOLEAN LANGUAGE plpgsql AS $$
DECLARE
  loytynyt_tunniste TEXT;
  asetettu_lukko    CHAR(36);
  lukko_asetettu    TIMESTAMP;
BEGIN
  SELECT
    tunniste,
    lukko,
    lukittu
  INTO loytynyt_tunniste, asetettu_lukko, lukko_asetettu
  FROM lukko
  WHERE tunniste = tarkistettava_tunniste;

  IF loytynyt_tunniste IS NULL
  THEN
    INSERT INTO lukko (tunniste, lukko, lukittu) VALUES (tarkistettava_tunniste, uusi_lukko, now());
    RETURN TRUE;
  ELSE
    IF asetettu_lukko IS NULL OR
       (aikaraja IS NOT NULL AND (EXTRACT(EPOCH FROM (current_timestamp - lukko_asetettu)) > aikaraja))
    THEN
      UPDATE lukko
      SET lukko = uusi_lukko, lukittu = now()
      WHERE tunniste = tarkistettava_tunniste;
      RETURN TRUE;
    ELSE
      RETURN FALSE;
    END IF;
  END IF;
  RETURN FALSE;
END
$$;

-- Lukon avaus
CREATE OR REPLACE FUNCTION avaa_lukko(tarkistettava_tunniste TEXT)
  RETURNS BOOLEAN LANGUAGE plpgsql AS $$
DECLARE
  loytynyt_tunniste TEXT;
BEGIN
  SELECT tunniste
  INTO loytynyt_tunniste
  FROM lukko
  WHERE tunniste = tarkistettava_tunniste;

  IF loytynyt_tunniste IS NULL
  THEN
    RETURN FALSE;
  ELSE
    UPDATE lukko
    SET lukittu = NULL, lukko = NULL
    WHERE tunniste = tarkistettava_tunniste;
    RETURN TRUE;
  END IF;
  RETURN FALSE;
END
$$;
