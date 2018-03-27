#!/usr/bin/env bash

set -e
set -x
set -u
export TZ=EET
cd /tmp
cd harja
# git pull origin develop
function cmd_phantom
{
    lein doo phantom test once
}

function cmd_test
{
    lein clean
    lein test
}
function cmd_test+phantom
{
    lein clean
    lein test
    lein doo phantom test once
}

function cmd_back
{
    lein tuotanto-notest
    java -jar target/harja-0.0.1-SNAPSHOT-standalone.jar
}

function cmd_uberjar
{
    lein tuotanto-notest
}

function cmd_help {
    echo komennot: phantom, test, test+phantom, back, help
}

SUBCMD="$1"
shift
BRANCH="$1"
shift
ARGS="$@"

git fetch origin
pwd
rm -vf asetukset.edn
git checkout -b "t_$BRANCH" "origin/$BRANCH"
git checkout asetukset.edn

mkdir -p ../.harja
echo aaaa > ../.harja/anti-csrf-token
touch ../.harja/{mml,google-static-maps-key,turi-salasana,ava-salasana,yha-salasana,labyrintti-salasana}
# todo: kokeile lein trampoline "$@"
cd tietokanta
mvn flyway:migrate
psql -h localhost -U harja harja -X -q -a -v ON_ERROR_STOP=1 --pset pager=off -f testidata.sql > /dev/null
psql -h localhost -U harja harja -c "CREATE DATABASE harjatest_template WITH TEMPLATE harja OWNER harjatest;"
psql -h localhost -U harja harja -c "CREATE DATABASE harjatest WITH TEMPLATE harjatest_template OWNER harjatest;"
cd ..
eval "cmd_$SUBCMD"
