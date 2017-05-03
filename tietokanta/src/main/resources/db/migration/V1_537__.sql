CREATE TABLE tyotunnit (
  id             SERIAL PRIMARY KEY,
  urakka         INTEGER REFERENCES urakka (id)      NOT NULL,
  vuosi          INTEGER                             NOT NULL,
  vuosikolmannes INTEGER                             NOT NULL,
  tyotunnit      INTEGER                             NOT NULL,
  UNIQUE (urakka, vuosi, vuosikolmannes),
  CONSTRAINT validi_vuosikolmannes CHECK (vuosikolmannes >= 1 AND vuosikolmannes <= 3)
);