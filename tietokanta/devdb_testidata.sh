#!/bin/sh

### HUOM: tämä ajetaan harjadb-kontin sisällä ###

if [ "$1" = "--localhost" ]; then
    pghost=localhost
    pgport=5432
else
    pghost=$POSTGRES_PORT_5432_TCP_ADDR
    pgport=$POSTGRES_PORT_5432_TCP_PORT
    cd /tietokanta
fi

set -e


echo "Ajetaan testidata harja-kantaan"
psql -h "$pghost" -p "$POSTGRES_PORT_5432_TCP_PORT" -U harja harja -X -q -a -v ON_ERROR_STOP=1 --pset pager=off -f testidata.sql > /dev/null

echo "Tapa porsaat ja vapauta olemassaolevat yhteydet"
psql -h "$pghost" -p "$POSTGRES_PORT_5432_TCP_PORT" -U harja harja -c "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'harja' AND pid <> pg_backend_pid();"

echo "Luodaan harjatest_template kanta harja-kannan pohjalta"
psql -h "$pghost" -p "$POSTGRES_PORT_5432_TCP_PORT" -U harja harja -c "CREATE DATABASE harjatest_template WITH TEMPLATE harja OWNER harjatest;"

echo "Luodaan harjatest kanta harjatest_template kannan pohjalta"
psql -h "$pghost" -p "$POSTGRES_PORT_5432_TCP_PORT" -U harja harja -c "CREATE DATABASE harjatest WITH TEMPLATE harjatest_template OWNER harjatest;"
