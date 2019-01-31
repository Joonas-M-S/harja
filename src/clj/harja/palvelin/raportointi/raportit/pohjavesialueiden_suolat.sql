-- name: hae-urakan-pohjavesialueiden-suolatoteumat
SELECT pv.tunnus,
       pv.nimi,
       pv.tr_numero AS tie,
       sum(rp.maara) AS maara_t_per_km,
       sum(rp.maara)*sum(st_length(pv.alue))/1000 AS yhteensa,
       ts.talvisuolaraja AS kayttoraja
FROM suolatoteuma_reittipiste rp
  INNER JOIN toteuma tot ON tot.id = rp.toteuma
  INNER JOIN pohjavesialue pv ON pv.tunnus = rp.pohjavesialue
  LEFT JOIN pohjavesialue_talvisuola ts on ts.pohjavesialue = rp.pohjavesialue
WHERE tot.urakka=:urakkaid AND rp.aika BETWEEN :alkupvm AND :loppupvm
GROUP BY pv.tunnus, pv.tr_numero, pv.nimi, ts.talvisuolaraja;