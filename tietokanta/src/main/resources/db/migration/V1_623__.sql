-- Toiukenna urakka-alus taulua
ALTER TABLE vv_alus_urakka ALTER COLUMN alus SET NOT NULL;
ALTER TABLE vv_alus_urakka ALTER COLUMN urakka SET NOT NULL;
ALTER TABLE vv_alus_urakka ALTER COLUMN lisatiedot TYPE VARCHAR(512);
