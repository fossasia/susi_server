#!/usr/bin/env sh
cd `dirname $0`/..
echo "re-starting loklak"
bin/stop.sh
bin/start.sh
