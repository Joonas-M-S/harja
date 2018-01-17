-- Päivitetään toteuman luovan transaktion lopuksi materiaalit
CREATE OR REPLACE FUNCTION paivita_urakan_materiaalin_kaytto_hoitoluokittain ()
  RETURNS TRIGGER AS $$
DECLARE
  rivi RECORD;
  rivi2 RECORD;
  u INTEGER;
  p RECORD;
BEGIN
  -- Jos toteuma on luotu tässä transaktiossa, ei käsitellä uudelleen päivitystä
  IF TG_OP = 'UPDATE' AND NEW.luotu = current_timestamp THEN
    RETURN NEW;
  END IF;
  --
  u := NEW.urakka;
  FOR rivi IN SELECT SUM(rm.maara) AS summa,
                     rm.materiaalikoodi,
		     rp.aika::DATE,
		     COALESCE(rp.talvihoitoluokka, 0) AS talvihoitoluokka
                FROM toteuman_reittipisteet tr
		     JOIN LATERAL unnest(tr.reittipisteet) rp ON true
		     JOIN LATERAL unnest(rp.materiaalit) rm ON true
	       WHERE tr.toteuma = NEW.id
	       GROUP BY rm.materiaalikoodi, rp.aika::DATE, rp.talvihoitoluokka
  LOOP
    IF NEW.poistettu IS TRUE THEN
      RAISE NOTICE 'poistetaan toteuma, joten vähennettään materiaalia % määrä %', rivi.materiaalikoodi, rivi.summa;
      -- Toteuma on merkitty poistetuksi, vähennetään määrää
      UPDATE urakan_materiaalin_kaytto_hoitoluokittain
         SET maara = maara - rivi.summa
       WHERE pvm = rivi.aika AND
             materiaalikoodi = rivi.materiaalikoodi AND
 	     talvihoitoluokka = rivi.talvihoitoluokka AND
	     urakka = u;
    END IF;
  END LOOP;


  -- Poista hoitoluokittainen materiaalicache kaikille reittipisteiden pvm:ille tässä urakassa
    DELETE FROM urakan_materiaalin_kaytto_hoitoluokittain
    WHERE pvm IN (SELECT DISTINCT (rp.aika::DATE)
                  FROM toteuman_reittipisteet tr
                    JOIN LATERAL unnest(tr.reittipisteet) rp ON true
                    JOIN LATERAL unnest(rp.materiaalit) rm ON true
                  WHERE tr.toteuma = NEW.id)
          AND urakka = u;

    -- Päivitä materiaalin käyttö ko. pvm:lle ja urakalle
    FOR rivi2 IN SELECT t.urakka, rp.talvihoitoluokka, mat.materiaalikoodi,
                   sum(mat.maara) as maara,
                   rp.aika::DATE
                 FROM toteuma t
                   JOIN toteuman_reittipisteet tr ON tr.toteuma = t.id
                   JOIN LATERAL unnest(tr.reittipisteet) rp ON true
                   JOIN LATERAL unnest(rp.materiaalit) mat ON true
                 WHERE t.alkanut::date in (SELECT DISTINCT (rp.aika::DATE)
                                           FROM toteuman_reittipisteet tr
                                             JOIN LATERAL unnest(tr.reittipisteet) rp ON true
                                             JOIN LATERAL unnest(rp.materiaalit) rm ON true
                                           WHERE tr.toteuma = NEW.id)
                       AND t.urakka = u
                 GROUP BY t.urakka, rp.talvihoitoluokka, mat.materiaalikoodi, rp.aika
    LOOP
      RAISE NOTICE 'INSERT INTO urakan_materiaalin_kaytto_hoitoluokittain  rivi2: %', rivi2;
      INSERT INTO urakan_materiaalin_kaytto_hoitoluokittain
      (pvm, materiaalikoodi, talvihoitoluokka, urakka, maara)
      VALUES (rivi2.aika,
              rivi2.materiaalikoodi,
              COALESCE(rivi2.talvihoitoluokka, 0),
              rivi2.urakka,
              rivi2.maara);
    END LOOP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Päivitä kaikki materiaalin käyttö päivämäärälle
CREATE OR REPLACE FUNCTION paivita_materiaalin_kaytto_hoitoluokittain_paivalle(pvm_ DATE)
  RETURNS VOID AS $$
DECLARE
  rivi RECORD;
BEGIN
  RAISE WARNING 'paivita_materiaalin_kaytto_hoitoluokittain_paivalle %', pvm_;
  DELETE FROM urakan_materiaalin_kaytto_hoitoluokittain WHERE pvm = pvm_;
  FOR rivi IN SELECT t.urakka, rp.talvihoitoluokka, mat.materiaalikoodi,
                     sum(mat.maara) as maara
      	        FROM toteuma t
                     JOIN toteuman_reittipisteet tr ON tr.toteuma = t.id
                     JOIN LATERAL unnest(tr.reittipisteet) rp ON true
                     JOIN LATERAL unnest(rp.materiaalit) mat ON true
               WHERE t.alkanut::date = pvm_
            GROUP BY t.urakka, rp.talvihoitoluokka, mat.materiaalikoodi
  LOOP
    RAISE NOTICE 'INSERT INTO urakan_materiaalin_kaytto_hoitoluokittain pvm: %', pvm_;
    INSERT INTO urakan_materiaalin_kaytto_hoitoluokittain
                (pvm, materiaalikoodi, talvihoitoluokka, urakka, maara)
         VALUES (pvm_,
	         rivi.materiaalikoodi,
		 COALESCE(rivi.talvihoitoluokka, 0),
		 rivi.urakka,
		 rivi.maara);
  END LOOP;
  RETURN;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION paivita_materiaalin_kaytto_hoitoluokittain_aikavalille(alku DATE, loppu DATE)
  RETURNS VOID AS $$
DECLARE
  pvm DATE;
BEGIN
  pvm := alku;
  LOOP
    IF pvm > loppu THEN
      EXIT;
    END IF;
    RAISE NOTICE 'Päivitetään materiaalin käyttö hoitoluokittain: %', pvm;
    PERFORM paivita_materiaalin_kaytto_hoitoluokittain_paivalle(pvm);
    pvm := pvm + 1;
 END LOOP;
 RETURN;
END;
$$ LANGUAGE plpgsql;
