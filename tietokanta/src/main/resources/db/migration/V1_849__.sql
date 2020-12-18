-- toteutunut_tyo tauluun siirretään mhurakoiden suunnitellut kustannukset aina kuukauden viimeinen päivä.

CREATE TABLE toteutunut_tyo
(
    id                  serial primary key, -- sisäinen ID
    vuosi               smallint not null CHECK (1900 < vuosi AND vuosi < 2200),
    kuukausi            smallint not null CHECK (13 > kuukausi AND kuukausi > 0),
    summa               numeric,
    tyyppi              toteumatyyppi,
    tehtava             integer REFERENCES toimenpidekoodi (id),
    tehtavaryhma        integer REFERENCES tehtavaryhma (id),
    toimenpideinstanssi integer  not null REFERENCES toimenpideinstanssi (id),
    sopimus_id          integer REFERENCES sopimus (id),
    urakka_id           integer REFERENCES urakka (id),
    luotu               timestamp,
    muokattu            timestamp,
    muokkaaja           integer references kayttaja (id),
    unique (toimenpideinstanssi, tehtava, sopimus_id, urakka_id, vuosi, kuukausi)
);

create index toteutunut_tyo_urakka_index
    on toteutunut_tyo (urakka_id, kuukausi, vuosi);


COMMENT ON table toteutunut_tyo IS
    E'Kustannusarvioitu_tyo taulusta siirretään kuukauden viimeisenä päivänä (teiden-hoito, eli mh-urakoiden) tiedot
      tähän toteutunut_tyo tauluun, josta voidaan hakea toteutuneen työn kustannukset raportteihin ja kustannuksiin.
      Toteutumia tulee neljälle erityyppiselle kululle:
      - laskutettava-tyo
      - akillinen-hoitotyo
      - muutostyo
      - muut-rahavaraukset';

-- Siirretään kaikki olemassaolevat rivit kustannusarvoidut_tyot taulusta toteutunut_tyo tauluun, mikäli kuukausi on eri kuin kuluva kuukausi.
-- Kuluvan kuukauden kustannusarvoidut_tyot siirretään vasta kuukauden viimeisenä päivänä

INSERT INTO toteutunut_tyo (vuosi, kuukausi, summa, tyyppi, tehtava, tehtavaryhma, toimenpideinstanssi, sopimus_id,
                            urakka_id, luotu)
SELECT k.vuosi,
       k.kuukausi,
       k.summa,
       k.tyyppi,
       k.tehtava,
       k.tehtavaryhma,
       k.toimenpideinstanssi,
       k.sopimus,
       (select s.urakka FROM sopimus s where s.id = k.sopimus) as "urakka-id",
       NOW()
FROM kustannusarvioitu_tyo k
WHERE (SELECT (date_trunc('MONTH', format('%s-%s-%s', k.vuosi, k.kuukausi, 1)::DATE))) <
      date_trunc('month', current_date);

do
$$
    declare
        tehtavaryhma_id integer := (SELECT id
                                    FROM tehtavaryhma
                                    WHERE nimi = 'Johto- ja hallintokorvaus (J)');


    BEGIN

        -- Siirretään kaikki olemassaolevat rivit johto_ja_hallintakorvaus  taulustatoteutunut_tyo tauluun, mikäli kuukausi on eri kuin kuluva kuukausi.
        -- Kuluvan kuukauden kustannusarvoidut_tyot siirretään vasta kuukauden viimeisenä päivänä
        INSERT INTO toteutunut_tyo (vuosi, kuukausi, summa, tyyppi, tehtava, tehtavaryhma, toimenpideinstanssi,
                                    sopimus_id, urakka_id, luotu)
        SELECT j.vuosi,
               j.kuukausi,
               (j.tunnit * j.tuntipalkka) AS summa,
               'laskutettava-tyo' AS tyyppi,
               null as tehtava,
               tehtavaryhma_id,
               (SELECT tpi.id AS id
                FROM toimenpideinstanssi tpi
                         JOIN toimenpidekoodi tpk3 ON tpk3.id = tpi.toimenpide
                         JOIN toimenpidekoodi tpk2 ON tpk3.emo = tpk2.id,
                     maksuera m
                WHERE tpi.urakka = j."urakka-id"
                  AND m.toimenpideinstanssi = tpi.id
                  AND tpk2.koodi = '23150'),
               (SELECT id
                FROM sopimus s
                WHERE s.urakka = j."urakka-id"
                  AND s.poistettu IS NOT TRUE
                ORDER BY s.loppupvm DESC limit 1),
               j."urakka-id",
               NOW()
        FROM johto_ja_hallintokorvaus j
        WHERE (SELECT (date_trunc('MONTH', format('%s-%s-%s', j.vuosi, j.kuukausi, 1)::DATE))) <
              date_trunc('month', current_date);
    END
$$;