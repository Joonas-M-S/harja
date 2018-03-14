#!/usr/bin/env bash

set -e
set -x
set -u
export TZ=EET
cd /tmp
cd harja
git pull origin develop
sed -i -e 's!jdbc:postgresql://localhost/!jdbc:postgresql://harjadb/!' src/clj/harja/kyselyt/specql_db.clj
sed -i -e 's!:palvelin "localhost"!:palvelin "harjadb"!' asetukset.edn
lein test
