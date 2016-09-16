#!/usr/bin/env sh
cd `dirname $0`/..
echo "re-starting susi"
bin/stop.sh
bin/start.sh
