ALTER TABLE toteuma ADD COLUMN tyokoneen_lisatieto TEXT;

ALTER TYPE tyokone ADD VALUE 'Ei ajoneuvoa';
ALTER TYPE tyokone ADD VALUE 'Huoltoauto';
ALTER TYPE tyokone ADD VALUE 'Imulakaisukone';
ALTER TYPE tyokone ADD VALUE 'Jalkautuneena';
ALTER TYPE tyokone ADD VALUE 'Kiinteistötraktori';
ALTER TYPE tyokone ADD VALUE 'Maalausajoneuvo';
ALTER TYPE tyokone ADD VALUE 'Muu';
ALTER TYPE tyokone ADD VALUE 'Niittolaite';
ALTER TYPE tyokone ADD VALUE 'Nostokoriauto';
ALTER TYPE tyokone ADD VALUE 'Tasoleikkuri';
ALTER TYPE tyokone ADD VALUE 'Turva-auto';
ALTER TYPE tyokone ADD VALUE 'Työkon';