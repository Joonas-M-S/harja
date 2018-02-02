-- Lisää taulu, johon tullaan tallentamaan odottavien sähköpostien tietoja (aluksi tiemerkinnän valmistuminen)
CREATE TYPE ODOTTAVA_SAHKOPOSTI_TYYPPI AS ENUM ('tiemerkinta_valmistunut');

CREATE TABLE odottava_sahkoposti (
  id                 SERIAL PRIMARY KEY,
  tyyppi             ODOTTAVA_SAHKOPOSTI_TYYPPI            NOT NULL,
  yllapitokohde_id   INTEGER REFERENCES yllapitokohde (id) NOT NULL,
  vastaanottajat     TEXT []                               NOT NULL,
  viesti             TEXT                                  NOT NULL,
  kopio_lahettajalle TEXT -- Mailin aikaansaaneen käyttäjän s-posti, johon lähetetään kopio viestistä (tai NULL)
);

-- Vain yksi samantyyppinen rivi per ylläpitokohde
ALTER TABLE odottava_sahkoposti ADD CONSTRAINT uniikki_yllapitokohde UNIQUE (yllapitokohde_id, tyyppi);