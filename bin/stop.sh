#!/usr/bin/env sh
cd `dirname $0`/../data
echo "Shutting down SUSI."
# If you don't want to wait, just run this concurrently
kill $(cat susi.pid 2>/dev/null) 2>/dev/null
if [ $? -eq 0 ]; then while [ -f "susi.pid" ]; do sleep 1; done; fi;
rm -f susi.pid 2>/dev/null
echo "SUSI has been terminated."
