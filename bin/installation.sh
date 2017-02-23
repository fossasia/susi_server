#!/usr/bin/env bash

# If you're looking for the variables, please go to bin/.preload.sh

# Make sure we're on project root
cd $(dirname $0)/..

# Execute preload script
source bin/.preload.sh

echo "starting loklak installation"
echo "startup" > $STARTUPFILE

cmdline="$cmdline -server -classpath $CLASSPATH -Dlog4j.configurationFile=$LOGCONFIG org.loklak.SusiInstallation >> data/loklak.log 2>&1 &";

eval $cmdline
PID=$!
echo $PID > $PIDFILE

while [ -f $STARTUPFILE ] && [ $(ps -p $PID -o pid=) ]; do
	if [ $(cat $STARTUPFILE) = 'done' ]; then
		break
	else
		sleep 1
	fi
done

if [ -f $STARTUPFILE ] && [ $(ps -p $PID -o pid=) ]; then
	CUSTOMPORT=$(grep -iw 'port.http' conf/config.properties | sed 's/^[^=]*=//' );
	LOCALHOST=$(grep -iw 'shortlink.urlstub' conf/config.properties | sed 's/^[^=]*=//');
	echo "susi installation started at port $CUSTOMPORT, open your browser at $LOCALHOST"
	rm -f $STARTUPFILE

    echo "waiting for installation to finish"
    wait "$PID"
    if [ $? -eq 0 ]; then
        echo "susi installation finished"
        echo 'done' > $INSTALLATIONCONFIG
    else
        echo "susi installation aborted"
    fi

	exit 0
else
	echo "susi installation failed to start. See data/loklag.log for details. Here are the last logs:"
    tail data/loklak.log
	rm -f $STARTUPFILE
	exit 1
fi
