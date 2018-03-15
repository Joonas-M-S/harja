ALTER TABLE vatu_turvalaite ALTER COLUMN turvalaitenro TYPE text USING turvalaitenro::text;
ALTER TABLE vatu_turvalaite ALTER COLUMN koordinaatit TYPE geometry USING koordinaatit::geometry;
UPDATE vatu_turvalaite SET luoja = NULL WHERE luoja = 'Integraatio';
ALTER TABLE vatu_turvalaite ALTER COLUMN luoja TYPE integer USING luoja::integer;
UPDATE vatu_turvalaite SET muokkaaja = NULL WHERE muokkaaja = 'Integraatio';
ALTER TABLE vatu_turvalaite ALTER COLUMN muokkaaja TYPE integer USING muokkaaja::integer;
ALTER TABLE vatu_turvalaite ADD COLUMN kiintea BOOL;
update vatu_turvalaite set kiintea = true where tarkenne = 'KIINTEÄ';
update vatu_turvalaite set kiintea = false where tarkenne = 'KELLUVA';
ALTER TABLE vatu_turvalaite DROP COLUMN tarkenne;
UPDATE vv_vikailmoitus SET "reimari-turvalaitenro" = (SELECT turvalaitenro FROM vv_turvalaite WHERE id = "turvalaite-id");
ALTER TABLE vv_vikailmoitus DROP COLUMN "turvalaite-id";
DROP TRIGGER IF EXISTS vv_vikailmoituksen_turvalaite_id_trigger ON vv_vikailmoitus;
DROP FUNCTION IF EXISTS vv_vikailmoituksen_turvalaite_id_trigger_proc();
ALTER TABLE reimari_toimenpide ADD COLUMN turvalaitenro TEXT;