CREATE TABLE kiinteahintainen_tyo (
                                     id serial primary key,       -- sisäinen ID
                                     vuosi smallint not null,
                                     kuukausi smallint not null,
                                     summa numeric,
                                     toimenpideinstanssi integer not null REFERENCES toimenpideinstanssi (id),
                                     tehtavaryhma integer REFERENCES tehtavaryhma (id),
                                     tehtava integer REFERENCES toimenpidekoodi (id),
                                     sopimus integer REFERENCES sopimus(id),
                                     luotu timestamp,
                                     luoja integer references kayttaja(id),
                                     muokattu timestamp,
                                     muokkaaja integer references kayttaja(id),
                                     unique (toimenpideinstanssi, sopimus, vuosi, kuukausi));


COMMENT ON table kiinteahintainen_tyo IS
  E'Kiinteähintaista työtä suunnitellaan urakkatyypissä teiden-hoito (MHU).
   Työlle suunniteltu kustannus lasketaan mukaan Sampoon lähetettävään kokonaishintaiseen kustannussuunnitelmaan. Suunniteltu summa myös kasvattaa Sampoon lähetettävää maksuerää (samaan tapaan kuin kokonaishintainen_tyo urakkatyypissä hoito).
    Kustannussuunnitelma tehdään kuukausi- ja toimenpidetasolla.' ;


CREATE TABLE kustannusarvioitu_tyo (
                                     id serial primary key,       -- sisäinen ID
                                     vuosi smallint not null,
                                     kuukausi smallint not null,
                                     summa numeric,
                                     tyyppi toteumatyyppi,
                                     tehtava integer REFERENCES toimenpidekoodi (id),
                                     tehtavaryhma integer REFERENCES tehtavaryhma (id),
                                     toimenpideinstanssi integer not null REFERENCES toimenpideinstanssi (id),
                                     sopimus integer REFERENCES sopimus(id),
                                     luotu timestamp,
                                     luoja integer references kayttaja(id),
                                     muokattu timestamp,
                                     muokkaaja integer references kayttaja(id),
                                     unique (toimenpideinstanssi, tehtava, sopimus, vuosi, kuukausi));


ALTER TABLE yksikkohintainen_tyo
ADD COLUMN luotu TIMESTAMP,
ADD COLUMN kuukausi integer CHECK (13 > kuukausi AND kuukausi > 0),
ADD COLUMN vuosi integer CHECK (2015 < vuosi AND vuosi < 2100);


COMMENT ON table kustannusarvioitu_tyo IS
  E'Kustannusarvioitua työtä suunnitellaan urakkatyypissä teiden-hoito (MHU).
   Työlle suunniteltu kustannus lasketaan mukaan Sampoon lähetettävään kokonaishintaiseen kustannussuunnitelmaan, mutta suunniteltu summa ei kasvata Sampoon lähetettävää maksuerää (toisin kuin kiinteahintainen_tyo ja kokonaishintainen_tyo urakkatyypissä hoito).
    Kustannusarvioita tehdään neljälle erityyppiselle kululle:
    - työ
    - äkillinen hoitotyö
    - kolmansien osapuolten aiheuttamat muutokset
    - muut urakan rahavaraukset' ;

COMMENT ON table kokonaishintainen_tyo IS
  E'Kokonaishintaista työtä suunnitellaan urakkatyypissä hoito. Kokonaishintaisella työllä on kiinteä kuukausittainen summa, joka kasvattaa myös Sampoon lähetettävän maksuerän summaa kuukausittain.
   Urakkatyypissä teiden-hoito (MHU) tauluun summataan kustannussuunnitelman Sampoon lähettämistä varten kaikki sellaiset suunnitellut ja arvoidut kustannukset, jotka lähetetään kokonaishintaisessa maksuerässä.
   Varsinaiset budjetoinnit on tallennettu tauluihin kiinteahintainen_tyo, kustannusarvioitu_tyo ja yksikkohintainen_tyo [sic].' ;

COMMENT ON table yksikkohintainen_tyo IS
  E'Yksikkohintaista työtä suunnitellaan urakkatyypeissä hoito ja teiden-hoito (MHU).
   Hoitourakassa suunniteltu yksikköhintainen työ lähetetään Sampoon yksikköhintaisessa kustannussuunnitelmassa. Teiden hoidon urakassa (MHU), suunniteltu yksikköhintainen työ summataan kiinteän ja kustannusarvioidun työn kanssa tauluun kokonaishintainen_tyo
   ja lähetetään Sampoon kokonaishintaisessa kustannussuunnitelmassa [sic].
   Kummassakaan urakkatyypissä yksikköhintainen työ ei automaattisesti kasvata maksuerää vaan maksuerän summa perustuu toteumille (hoito) ja laskutukselle (teiden-hoito).';
