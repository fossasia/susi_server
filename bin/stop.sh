#!/usr/bin/env sh
cd `dirname $0`/../data
echo "shut down of loklak"
kill `cat loklak.pid`

# wait until the yacy.running file disappears which means that YaCy has terminated
# If you don't want to wait, just run this concurrently
echo "waiting on termination of loklak"
while [ -f "loklak.pid" ]
do
sleep 1
done
echo "loklak has been terminated"
#rm loklak.pid
