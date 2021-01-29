-- Lisää muut toiminta sarakkeet pot2_alusta tauluun

ALTER TABLE pot2_alusta ADD COLUMN lisatty_paksuus INTEGER;
ALTER TABLE pot2_alusta ADD COLUMN massamaara INTEGER;
ALTER TABLE pot2_alusta ADD COLUMN murske INTEGER REFERENCES pot2_mk_urakan_murske (id);
ALTER TABLE pot2_alusta ADD COLUMN kasittelysyvyys INTEGER;
ALTER TABLE pot2_alusta ADD COLUMN leveys INTEGER;
ALTER TABLE pot2_alusta ADD COLUMN pinta_ala INTEGER;
ALTER TABLE pot2_alusta ADD COLUMN kokonaismassamaara INTEGER;
ALTER TABLE pot2_alusta ADD COLUMN massa INTEGER REFERENCES pot2_mk_urakan_massa (id);
ALTER TABLE pot2_alusta ADD COLUMN sideaine INTEGER REFERENCES  pot2_mk_sideainetyyppi (koodi);
ALTER TABLE pot2_alusta ADD COLUMN sideainepitoisuus NUMERIC(10, 1);
ALTER TABLE pot2_alusta ADD COLUMN seosaine INTEGER; -- puuttuu koodisto taulukko

-- kaikissa alustariveissä ei välttämättä ole mursketta
ALTER TABLE pot2_alusta DROP COLUMN materiaali;

DELETE FROM pot2_mk_alusta_toimenpide
where nimi ilike 'Verkko%';

INSERT INTO pot2_mk_alusta_toimenpide (nimi, lyhenne, koodi)
VALUES ('Verkko', 'Verkko', 667);