#!/usr/bin/env sh
cd `dirname $0`/..
echo "Restarting SUSI"
bin/stop.sh
sleep 10
bin/start.sh
