-- name:tallenna-budjettitavoite<!
INSERT INTO urakka_tavoite (urakka, hoitokausi, tavoitehinta, tavoitehinta_siirretty, kattohinta, luotu, luoja) VALUES
(:urakka, :hoitokausi, :tavoitehinta, :tavoitehinta_siirretty, :kattohinta, current_timestamp, :kayttaja);

-- name:paivita-budjettitavoite<!
UPDATE urakka_tavoite
SET tavoitehinta = :tavoitehinta,
tavoitehinta_siirretty = :tavoitehinta_siirretty,
kattohinta = :kattohinta,
muokattu = current_timestamp,
muokkaaja = :kayttaja WHERE
urakka = :urakka AND hoitokausi = :hoitokausi;

-- name:hae-budjettitavoite
SELECT * from urakka_tavoite
WHERE urakka = :urakka;


