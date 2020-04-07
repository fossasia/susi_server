#!/usr/bin/env sh
cd "`dirname $0`"

docker stop etherpad
docker rm etherpad
rm ../data/etherpad-lite/APIKEY.txt

