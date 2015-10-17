#!/usr/bin/env sh
cd "`dirname $0`"

# for a production environment with high-availability requirement,
# add the following line in /etc/crontab (replace with correct path!)
# 0 *	* * *	root    cd /usr/share/loklak/bin && ./checkalive.sh

FLAG=0
if [ `./apicall.sh /index.html | grep "</html>"` ]; then
  FLAG=1
fi

if [ $FLAG -eq '0' ]; then
    ./stop.sh
    ./kill.sh
    ./start.sh
fi
exit