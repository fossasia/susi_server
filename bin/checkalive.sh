#!/usr/bin/env bash
cd "`dirname $0`"

# for a production environment with high-availability requirement,
# add the following line in /etc/crontab (replace with correct path!)
# 0 *	* * *	root    cd /usr/share/loklak/bin && ./checkalive.sh

FLAG=0
if [[ -n `./apicall.sh index.html | grep "loklak"` ]]; then
  FLAG=1
fi

if [[ $FLAG -eq '0' && -f ../data/loklak.log && `tail -1 ../data/loklak.log` == *"Waiting for elasticsearch yellow status"* ]]; then
  FLAG=1
fi

if [ $FLAG -eq '0' ]; then
  ./stop.sh & sleep 60; kill $!
  ./kill.sh
  ./start.sh
fi
exit
