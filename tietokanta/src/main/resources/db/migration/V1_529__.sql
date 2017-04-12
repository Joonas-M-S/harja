-- Vesiväylien Kickoff-migraatio

-- Lisää urakkatyypit
ALTER TYPE urakkatyyppi ADD VALUE 'vesivayla-hoito';
ALTER TYPE urakkatyyppi ADD VALUE 'vesivayla-ruoppaus';
ALTER TYPE urakkatyyppi ADD VALUE 'vesivayla-turvalaitteiden-korjaus';
ALTER TYPE urakkatyyppi ADD VALUE 'vesivayla-kanavien-hoito';
ALTER TYPE urakkatyyppi ADD VALUE 'vesivayla-kanavien-korjaus';

-- harjassa_luotu
ALTER TABLE hanke ADD COLUMN harjassa_luotu BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE hanke SET harjassa_luotu = FALSE where harjassa_luotu IS NULL;

ALTER TABLE organisaatio ADD COLUMN harjassa_luotu BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE organisaatio SET harjassa_luotu = FALSE where harjassa_luotu IS NULL;

ALTER TABLE urakka ADD COLUMN harjassa_luotu BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE urakka SET harjassa_luotu = FALSE where harjassa_luotu IS NULL;

ALTER TABLE sopimus ADD COLUMN harjassa_luotu BOOLEAN DEFAULT FALSE;
UPDATE sopimus SET harjassa_luotu = FALSE where harjassa_luotu IS NULL;

-- Urakalta ja sopimukselta pois pakollinen sampoid (ei ole pakollinen vesiväyläurakoissa)
ALTER TABLE urakka ALTER COLUMN sampoid DROP NOT NULL;
ALTER TABLE sopimus ALTER COLUMN sampoid DROP NOT NULL;

-- Luotu, muokattu, muokkaaja jne. tiedot
ALTER TABLE urakka ADD COLUMN luotu timestamp;
ALTER TABLE urakka ADD COLUMN muokattu timestamp;
ALTER TABLE urakka ADD COLUMN luoja integer REFERENCES kayttaja (id);
ALTER TABLE urakka ADD COLUMN muokkaaja integer REFERENCES kayttaja (id);
ALTER TABLE urakka ADD COLUMN poistettu boolean default false;

ALTER TABLE sopimus ADD COLUMN luotu timestamp;
ALTER TABLE sopimus ADD COLUMN muokattu timestamp;
ALTER TABLE sopimus ADD COLUMN luoja integer REFERENCES kayttaja (id);
ALTER TABLE sopimus ADD COLUMN muokkaaja integer REFERENCES kayttaja (id);
ALTER TABLE sopimus ADD COLUMN poistettu boolean default false;

ALTER TABLE hanke ADD COLUMN luotu timestamp;
ALTER TABLE hanke ADD COLUMN muokattu timestamp;
ALTER TABLE hanke ADD COLUMN luoja integer REFERENCES kayttaja (id);
ALTER TABLE hanke ADD COLUMN muokkaaja integer REFERENCES kayttaja (id);
ALTER TABLE hanke ADD COLUMN poistettu boolean default false;

ALTER TABLE organisaatio ADD COLUMN luotu timestamp;
ALTER TABLE organisaatio ADD COLUMN muokattu timestamp;
-- ALTER TABLE organisaatio ADD COLUMN luoja integer REFERENCES kayttaja (id);
ALTER TABLE organisaatio ADD COLUMN muokkaaja integer REFERENCES kayttaja (id);
ALTER TABLE organisaatio ADD COLUMN poistettu boolean default false;