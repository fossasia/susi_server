#!/usr/bin/env bash

# If you're looking for the variables, please go to bin/preload.sh

# Make sure we're on project root
cd $(dirname $0)/..

# Execute preload script
source bin/preload.sh

echo "Starting SUSI installation"
echo "Startup" > $STARTUPFILE

cmdline="$cmdline -server -classpath $CLASSPATH -Dlog4j.configurationFile=$LOGCONFIG ai.susi.SusiInstallation >> data/susi.log 2>&1 &";

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

    echo "Waiting for installation to finish"
    wait "$PID"
    if [ $? -eq 0 ]; then
        echo "SUSI installation finished"
        echo 'Done' > $INSTALLATIONCONFIG
    else
        echo "SUSI installation aborted"
    fi

	exit 0
else
	echo "SUSI installation failed to start. See data/loklak.log for details. Here are the last logs:"
    tail data/susi.log
	rm -f $STARTUPFILE
	exit 1
fi
