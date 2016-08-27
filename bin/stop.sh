#!/usr/bin/env sh
cd `dirname $0`/../data
echo "shut down of loklak"
# If you don't want to wait, just run this concurrently
kill $(cat loklak.pid 2>/dev/null) 2>/dev/null
if [ $? -eq 0 ]; then while [ -f "loklak.pid" ]; do sleep 1; done; fi;
rm -f loklak.pid 2>/dev/null
echo "loklak has been terminated"
