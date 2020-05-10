#!/usr/bin/env bash

# This starts the susi_server
# The default port after start is 4000, sou you can reach the server
# at http://localhost:4000 after startup

# If you're looking for the variables, please go to bin/preload.sh

# Make sure we're on project root
cd $(dirname $0)/..

# Execute preload script
source bin/preload.sh

#
# default log to file, but take env var into account
SS_LOG_TO_STDOUT=${SS_LOG_TO_STDOUT:-0}

while getopts ":Idno" opt; do
    case $opt in
        d)
            SKIP_WAITING=1
            ;;
        n)
            DO_NOT_DAEMONIZE=1
            ;;
        o)
            SS_LOG_TO_STDOUT=1
            ;;
        \?)
            echo "Usage: $0 [options...]"
            echo -e " -d\tSkip waiting for SUSI"
            echo -e " -n\tDo not Daemonize"
            exit 1
            ;;
    esac
done


# If DO_NOT_DAEMONIZE is declared, use the log4j config that outputs
# to stdout/stderr
if [[ $DO_NOT_DAEMONIZE -eq 1 ]]; then
    SS_LOG_TO_STDOUT=1
fi

if [[ $SS_LOG_TO_STDOUT -eq 1 ]]; then
    LOGCONFIG="conf/logs/log4j2.properties"
fi



echo "starting SUSI"
echo "Startup" > $STARTUPFILE

cmdline="$cmdline -server -classpath $CLASSPATH -Dlog4j.configurationFile=$LOGCONFIG ai.susi.SusiServer";

# If DO_NOT_DAEMONIZE, pass it to the java command, end of this script.
if [[ $DO_NOT_DAEMONIZE -eq 1 ]]; then
    exec $cmdline
fi

if [[ $SS_LOG_TO_STDOUT -eq 1 ]]; then
    cmdline="$cmdline &"
else
    cmdline="$cmdline >> data/susi.log 2>&1 &"
fi

eval $cmdline
PID=$!
echo $PID > $PIDFILE

if [[ $SKIP_WAITING -eq 0 ]]; then
    while [ -f $STARTUPFILE ] && [ $(ps -p $PID -o pid=) ]; do
        if [ $(cat $STARTUPFILE) = 'done' ]; then
            break
        else
            sleep 1
        fi
    done
fi

if [ -f $STARTUPFILE ] && [ $(ps -p $PID -o pid=) ]; then
	CUSTOMPORT=$(grep -iw 'port.http' conf/config.properties | sed 's/^[^=]*=//' );
	LOCALHOST=$(grep -iw 'host.url' conf/config.properties | sed 's/^[^=]*=//');
	echo "susi server started at port $CUSTOMPORT, open your browser at $LOCALHOST"
	rm -f $STARTUPFILE
	exit 0
else
	echo "SUSI server failed to start. See data/susi.log for details. Here are the last logs:"
    tail data/susi.log
	rm -f $STARTUPFILE
	exit 1
fi
