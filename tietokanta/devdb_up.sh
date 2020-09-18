#!/usr/bin/env bash

set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
IMAGE=solita/harjadb:centos-12

if ! docker image list --filter=reference=${IMAGE} | tail -n +2 >> /dev/null; then
    echo "Imagea" $IMAGE "ei löydetty. Yritetään pullata."
    if ! docker pull $IMAGE; then
        echo $IMAGE "ei ole docker hubissa. Buildataan."
        docker build -t $IMAGE . ;
    fi
    echo ""
fi

docker run -p 127.0.0.1:5432:5432 --name "${POSTGRESQL_NAME:-harjadb}" -dit -v "$DIR":/var/lib/pgsql/harja/tietokanta \
       ${IMAGE} /bin/bash -c \
       "sudo -iu postgres /usr/pgsql-${POSTGRESQL_VERSION:-12}/bin/pg_ctl start -D /var/lib/pgsql/${POSTGRESQL_VERSION:-12}/data; /bin/bash";
       1> /dev/null

echo "Käynnistetään Docker-image" $IMAGE
echo ""
docker images | head -n1
docker images | grep $(echo $IMAGE | sed "s/:.*//") | grep $(echo $IMAGE | sed "s/.*://")

echo ""
echo "Odotetaan, että PostgreSQL on käynnissä ja vastaa yhteyksiin portissa 5432"
while ! nc -z localhost 5432; do
    echo "nukutaan..."
    sleep 0.5;
done;

docker exec --user postgres ${POSTGRESQL_NAME:-harjadb} /bin/bash -c "~/aja-migraatiot.sh"
docker exec --user postgres ${POSTGRESQL_NAME:-harjadb} /bin/bash -c "~/aja-testidata.sh"

echo ""
echo "Harjan tietokanta käynnissä! Imagen tiedot:"
echo ""

docker image list --filter=reference=${IMAGE}