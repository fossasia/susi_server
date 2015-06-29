#!/usr/bin/env sh
cd `dirname $0`/..
echo "loading latest code changes"
git pull origin master
echo "building loklak"
ant
bin/stop.sh
bin/start.sh
