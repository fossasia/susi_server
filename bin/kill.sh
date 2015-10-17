#!/usr/bin/env sh
cd `dirname $0`/../data
kill -9 `cat loklak.pid` 2>/dev/null
rm -f loklak.pid 2>/dev/null


