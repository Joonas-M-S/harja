CREATE OR REPLACE FUNCTION tarkista_hinnoittelu()
  RETURNS TRIGGER AS $$
DECLARE uusi_hintaryhma BOOLEAN;
BEGIN

  SELECT hintaryhma
  FROM vv_hinnoittelu
  WHERE id = new."hinnoittelu-id"
  INTO uusi_hintaryhma;

  IF (SELECT EXISTS(
      SELECT *
      FROM vv_hinnoittelu_toimenpide ht
        JOIN
        vv_hinnoittelu h ON ht."hinnoittelu-id" = h.id
      WHERE
        ht."toimenpide-id" = new."toimenpide-id" AND
        h.hintaryhma = uusi_hintaryhma AND
        ht.poistettu IS NOT TRUE))
  THEN
    RAISE EXCEPTION 'Toimenpiteelle % on jo olemassa hinnoittelu', new."toimenpide-id";
  END IF;
  RETURN new;

END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_tarkista_vv_toimenpiteiden_hinnoittelut
BEFORE INSERT
  ON vv_hinnoittelu_toimenpide
FOR EACH ROW
EXECUTE PROCEDURE tarkista_hinnoittelu();
