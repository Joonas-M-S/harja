-- vv_materiaali-tauluun poistettu ja poistaja
ALTER TABLE vv_materiaali ADD COLUMN poistettu BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE vv_materiaali ADD COLUMN poistaja INTEGER REFERENCES kayttaja (id);
ALTER TABLE vv_materiaali ADD CONSTRAINT poistaja_olemassa CHECK (poistettu IS NOT TRUE OR (poistettu IS TRUE AND poistaja IS NOT NULL));

-- Päivitä materiaalilistaus-view, älä listaa poistettuja kirjauksia

ALTER TYPE vv_materiaali_muutos ADD ATTRIBUTE id INTEGER;

CREATE OR REPLACE VIEW vv_materiaalilistaus AS
  SELECT DISTINCT ON (m1."urakka-id", m1.nimi)
    m1."urakka-id", m1.nimi,
    -- Haetaan alkuperäinen määrä (ajallisesti ensimmäinen kirjaus)
    (SELECT maara FROM vv_materiaali m2
    WHERE m2."urakka-id" = m1."urakka-id"
          AND m2.nimi = m1.nimi
          AND m2.poistettu IS NOT TRUE
     ORDER BY pvm ASC LIMIT 1) AS "alkuperainen-maara",
    -- Haetaan "nykyinen" määrä: kaikkien kirjausten summa
    (SELECT SUM(maara) FROM vv_materiaali m3
    WHERE m3."urakka-id" = m1."urakka-id"
          AND m3.nimi = m1.nimi
          AND m3.poistettu IS NOT TRUE) AS "maara-nyt",
    -- Kerätään kaikki muutokset omaan taulukkoon
    (SELECT array_agg(ROW(l.pvm, l.maara, l.lisatieto, l.id)::vv_materiaali_muutos)
     FROM vv_materiaali l
     WHERE l."urakka-id" = m1."urakka-id"
           AND m1.poistettu IS NOT TRUE
           AND m1.nimi = l.nimi) AS muutokset
  FROM vv_materiaali m1
  WHERE m1.poistettu IS NOT TRUE
  ORDER BY nimi ASC;