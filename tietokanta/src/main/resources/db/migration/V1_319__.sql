-- Turpon muutokset

-- Ammattityypit:

ALTER TABLE turvallisuuspoikkeama ADD COLUMN tyontekijanammatti_muu VARCHAR(512);
UPDATE turvallisuuspoikkeama SET tyontekijanammatti_muu = turvallisuuspoikkeama.tyontekijanammatti;

CREATE TYPE tyontekijanammatti AS ENUM (
  'aluksen_paallikko',
  'asentaja',
  'asfalttityontekija',
  'harjoittelija',
  'hitsaaja',
  'kunnossapitotyontekija',
  'kansimies',
  'kiskoilla_liikkuvan_tyokoneen_kuljettaja',
  'konemies',
  'kuorma-autonkuljettaja',
  'liikenteenohjaaja',
  'mittamies',
  'panostaja',
  'peramies',
  'porari',
  'rakennustyontekija',
  'ratatyontekija',
  'ratatyosta_vastaava',
  'sukeltaja',
  'sahkotoiden_ammattihenkilo',
  'tilaajan_edustaja',
  'turvalaiteasentaja',
  'turvamies',
  'tyokoneen_kuljettaja',
  'tyonjohtaja',
  'valvoja',
  'veneenkuljettaja',
  'vaylanhoitaja',
  'muu_tyontekija',
  'tyomaan_ulkopuolinen'
);

ALTER TABLE turvallisuuspoikkeama DROP COLUMN tyontekijanammatti;
ALTER TABLE turvallisuuspoikkeama ADD COLUMN tyontekijanammatti tyontekijanammatti;
UPDATE turvallisuuspoikkeama SET tyontekijanammatti = 'muu_tyontekija'::tyontekijanammatti WHERE tyontekijanammatti_muu IS NOT NULL;

-- Turpon seuraukset
ALTER TABLE turvallisuuspoikkeama ADD COLUMN aiheutuneet_seuraukset VARCHAR(2048);

-- Turpolle uusia vammoihin liittyviä kenttiä
CREATE TYPE turvallisuuspoikkeama_aiheutuneet_vammat AS ENUM (
  'haavat_ja_pinnalliset_vammat',
  'luunmurtumat',
  'sijoiltaan_menot_nyrjahdykset_ja_venahdykset',
  'amputoitumiset_ja_irti_repeamiset',
  'tarahdykset_ja_sisaiset_vammat_ruhjevammat',
  'palovammat_syopymat_ja_paleltumat',
  'myrkytykset_ja_tulehdukset',
  'hukkuminen_ja_tukehtuminen',
  'aanen_ja_varahtelyn_vaikutukset',
  'aarilampotilojen_valon_ja_sateilyn_vaikutukset',
  'sokki',
  'useita_samantasoisia_vammoja',
  'muut',
  'ei_tietoa'
);

ALTER TABLE turvallisuuspoikkeama DROP COLUMN vammat;
ALTER TABLE turvallisuuspoikkeama ADD COLUMN vammat turvallisuuspoikkeama_aiheutuneet_vammat[];

CREATE TYPE turvallisuuspoikkeama_vahingoittunut_ruumiinosa AS ENUM (
  'paan_alue',
  'silmat',
  'niska_ja_kaula',
  'selka',
  'vartalo',
  'sormi_kammen',
  'ranne',
  'muu_kasi',
  'nilkka',
  'jalkatera_ja_varvas',
  'muu_jalka',
  'koko_keho',
  'ei_tietoa'
);

ALTER TABLE turvallisuuspoikkeama ADD COLUMN vahingoittuneet_ruumiinosat turvallisuuspoikkeama_vahingoittunut_ruumiinosa[];
ALTER TABLE turvallisuuspoikkeama ADD COLUMN sairauspoissaolo_jatkuu BOOLEAN;

--- - Turpolle lisää sarakkeita

CREATE TYPE vaylamuoto AS ENUM(
  'tie',
  'vesi',
  'rautatie'
);

ALTER TABLE turvallisuuspoikkeama ADD COLUMN ilmoittaja_etunimi VARCHAR(1024);
ALTER TABLE turvallisuuspoikkeama ADD COLUMN ilmoittaja_sukunimi VARCHAR(1024);
ALTER TABLE turvallisuuspoikkeama ADD COLUMN vaylamuoto vaylamuoto;