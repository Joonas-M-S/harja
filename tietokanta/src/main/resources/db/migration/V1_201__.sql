−− Kuvaus: tierekisteriosoitteelle viiva

CREATE OR REPLACE FUNCTION tierekisteriosoitteelle_viiva(
  tie_ INTEGER, aosa_ INTEGER, aet_ INTEGER, losa_ INTEGER, let_ INTEGER)
  RETURNS geometry
AS $$
DECLARE
   rval geometry;
BEGIN
   SELECT ST_LineMerge(ST_Union((CASE WHEN osa=aosa_ THEN ST_Line_Substring(geom, LEAST(1, aet_/ST_Length(geom)), 1)
                                      WHEN osa=losa_ THEN ST_Line_Substring(geom, 0, LEAST(1,let_/ST_Length(geom)))
				      ELSE geom END) ORDER BY osa, path)) FROM tieverkko_paloina
    WHERE tie = tie_
      AND osa >= aosa_
      AND osa <= losa_
   INTO rval;

   RETURN rval;
END;
$$ LANGUAGE plpgsql;
