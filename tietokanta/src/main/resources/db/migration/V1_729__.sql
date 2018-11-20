ALTER TYPE reittipistedata ADD ATTRIBUTE pohjavesialue INT CASCADE;

CREATE OR REPLACE FUNCTION paivita_pohjavesialue_reittipistedataan(d reittipistedata[], threshold INT) RETURNS reittipistedata[] AS $$
DECLARE
    i INT;
    tmp reittipistedata;
BEGIN
    FOR i IN 1 .. array_upper(d,1) LOOP
    	tmp := d[i];
	SELECT kandidaatit.id FROM
	   (SELECT id, ST_Distance(alue, tmp.sijainti::geometry) AS etaisyys
	      FROM pohjavesialue
	      WHERE suolarajoitus IS TRUE
	      ORDER BY 2
	      LIMIT 1) AS kandidaatit
	   WHERE kandidaatit.etaisyys<threshold
	   INTO tmp.pohjavesialue;
	d[i] := tmp;
    END LOOP;
    RETURN d;
END;
$$ LANGUAGE plpgsql;

CREATE INDEX pohjavesialue_alue_idx ON pohjavesialue USING GIST (alue);
CREATE INDEX pohjavesialue_suolarajoitettu_idx ON pohjavesialue (suolarajoitus);

CREATE TABLE suolatoteuma_reittipiste (
       toteuma INTEGER REFERENCES toteuma(id),
       aika TIMESTAMP DEFAULT NOW(),
       pohjavesialue INTEGER REFERENCES pohjavesialue(id),
       sijainti POINT,
       materiaalikoodi INTEGER,
       maara NUMERIC
);

CREATE INDEX suolatoteuma_reittipiste_idx ON suolatoteuma_reittipiste USING GIST(sijainti);

CREATE OR REPLACE FUNCTION pisteen_pohjavesialue(piste POINT, threshold INTEGER) RETURNS INTEGER AS $$
DECLARE
  pohjavesialue INTEGER;
BEGIN
  SELECT id FROM pohjavesialue
   WHERE ST_DWithin(alue, piste::geometry, threshold)
   ORDER BY ST_Distance(alue, piste::geometry)
   LIMIT 1
   INTO pohjavesialue;
  RETURN pohjavesialue;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION konvertoi_urakan_suolatoteumat(urakkaid INTEGER, threshold INTEGER) RETURNS VOID AS $$
DECLARE
  cur CURSOR FOR SELECT rp.toteuma,
  	     	 	rp.luotu,
			(unnest(rp.reittipisteet)).*
		FROM toteuman_reittipisteet rp
  		LEFT JOIN toteuma tot ON tot.id = rp.toteuma
  		WHERE tot.urakka = urakkaid;
  tmp INTEGER[];
  suolamateriaalikoodit INTEGER[];
  m reittipiste_materiaali;
BEGIN
  SELECT array_agg(id) FROM materiaalikoodi
   WHERE materiaalityyppi='talvisuola' INTO suolamateriaalikoodit;
  
  FOR r IN cur LOOP
    FOREACH m IN ARRAY r.materiaalit LOOP
      IF suolamateriaalikoodit @> ARRAY[m.materiaalikoodi] THEN
        INSERT INTO suolatoteuma_reittipiste (toteuma, aika, sijainti, materiaalikoodi, maara, pohjavesialue) 
             VALUES (r.toteuma,
	     	    r.aika,
		    r.sijainti,
		    m.materiaalikoodi,
		    m.maara,
		    pisteen_pohjavesialue(r.sijainti, threshold));
      END IF;
    END LOOP;
  END LOOP;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tr_valin_suolatoteumat(tie_ INTEGER, aosa_ INTEGER, aet_ INTEGER, losa_ INTEGER, let_ INTEGER, threshold INTEGER) RETURNS TABLE (
       materiaalikoodi INTEGER,
       maara NUMERIC) AS $$
DECLARE
  g geometry;
BEGIN
  SELECT tierekisteriosoitteelle_viiva(tie_, aosa_, aet_, losa_, let_) INTO g;
  
  RETURN QUERY SELECT rp.materiaalikoodi, SUM(rp.maara)
    FROM suolatoteuma_reittipiste AS rp
    WHERE ST_DWithin(g, rp.sijainti::geometry, threshold)
    GROUP BY rp.materiaalikoodi;
END;
$$ LANGUAGE plpgsql;
