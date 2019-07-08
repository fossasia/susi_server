#!/usr/bin/env sh
cd `dirname $0`/../data
kill -9 `cat susi.pid` 2>/dev/null
rm -f susi.pid 2>/dev/null


