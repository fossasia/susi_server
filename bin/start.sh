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
        I)
            SKIP_INSTALL_CHECK=1
            ;;
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
            echo -e " -I\tIgnore installation config"
            echo -e " -d\tSkip waiting for SUSI"
            echo -e " -n\tDo not Daemonize"
            exit 1
            ;;
    esac
done

# installation
if [ ! -f $INSTALLATIONCONFIG ] && [[ $SKIP_INSTALL_CHECK -eq 0 ]]; then
    echo "SUSI detected that you did not yet run the installation wizard."
    echo "It lets you setup an administrator account and a number of settings, but is not mandatory."
    echo "You can manually start it by running bin/installation.sh"

:<<'OPTIONAL'
    while [ true ]; do
        echo "Would you like to start the installation now? (y)es, (n)o, (r)emind me next time"
        read -n 1 -s -t 20 input
        if  [ $? = 0 ]; then
            if [ "$input" = "y" ]; then
                bin/installation.sh
                if [ $? -ne 0 ]; then
                    exit 1
                fi
                break
            elif [ "$input" = "n" ]; then
                echo "Installation wizard skipped."
                echo 'skipped' > $INSTALLATIONCONFIG
                break
            elif [ "$input" = "r" ]; then
                break
            fi
        else
            echo "Timeout, skipping installation wizard."
            echo 'skipped' > $INSTALLATIONCONFIG
            break
        fi
    done
OPTIONAL
fi


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
	LOCALHOST=$(grep -iw 'shortlink.urlstub' conf/config.properties | sed 's/^[^=]*=//');
	echo "susi server started at port $CUSTOMPORT, open your browser at $LOCALHOST"
	rm -f $STARTUPFILE
	exit 0
else
	echo "SUSI server failed to start. See data/susi.log for details. Here are the last logs:"
    tail data/susi.log
	rm -f $STARTUPFILE
	exit 1
fi
