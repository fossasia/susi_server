#!/usr/bin/env bash
cd "`dirname $0`"

# for a production environment with high-availability requirement,
# add the following line in /etc/crontab (replace with correct path!)
# 0 *	* * *	root    cd /home/susi/susi_server/bin && ./checkalive.sh

FLAG=0
if [[ -n `./apicall.sh index.html | grep "susi"` ]]; then
  FLAG=1
fi

if [ $FLAG -eq '0' ]; then
  ./stop.sh & sleep 10; kill $!
  ./kill.sh
  ./start.sh
fi
exit
