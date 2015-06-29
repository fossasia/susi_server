#!/usr/bin/env sh
cd `dirname $0`/../data

kill `cat loklak.pid`

# wait until the yacy.running file disappears which means that YaCy has terminated
# If you don't want to wait, just run this concurrently
while [ -f "loklak.pid" ]
do
sleep 1
done
#rm loklak.pid
