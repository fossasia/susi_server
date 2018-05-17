#!/usr/bin/env sh
cd `dirname $0`/..
echo "Restarting SUSI"
bin/stop.sh
bin/start.sh
