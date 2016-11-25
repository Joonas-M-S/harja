-- Lisää työkoneelle reitti

ALTER TABLE tyokonehavainto
  ADD COLUMN reitti geometry,
  ADD COLUMN alkanut timestamp without time zone DEFAULT NOW();

CREATE OR REPLACE FUNCTION paivita_tyokoneen_reitti() RETURNS trigger AS $$
BEGIN
  IF NEW.reitti IS NULL THEN
    NEW.reitti = ST_MakeLine(ARRAY[NEW.sijainti]::GEOMETRY[]);
  ELSE
    NEW.reitti = ST_AddPoint(NEW.reitti, NEW.sijainti::GEOMETRY);
 END IF;
 RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_paivita_tyokoneen_reitti
BEFORE INSERT OR UPDATE ON tyokonehavainto
FOR EACH ROW
EXECUTE PROCEDURE paivita_tyokoneen_reitti();
