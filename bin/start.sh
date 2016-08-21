#!/usr/bin/env bash

PIDFILE="data/loklak.pid"
DFAULTCONFIG="conf/config.properties"
CUSTOMCONFIG="data/settings/customized_config.properties"
LOGCONFIG="conf/logs/log-to-file.properties"
STARTUPFILE="data/startup.tmp"
DFAULTXmx="-Xmx800m";
CUSTOMXmx=""

cd $(dirname $0)/..
mkdir -p data/settings

#to not allow process to overwrite the already running one.
if [ -f $PIDFILE ]; then
	PID=$(cat $PIDFILE 2>/dev/null)
	if [ $(ps -p $PID -o pid=) ]; then
		echo "Server is already running, please stop it and then start"
		exit 1
	else
		rm $PIDFILE
	fi
fi


# installation
INSTALLATIONCONFIG="data/settings/installation.txt"
if [ ! -f $INSTALLATIONCONFIG ]; then
    echo "Loklak detected that you did not yet run the installation wizard."
    echo "It let's you setup an administrator account and a number of settings, but is not mandatory."
    echo "You can manually start it by running bin/installation.sh"

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
fi

if [ -f $DFAULTCONFIG ]; then
    j="$(grep Xmx $DFAULTCONFIG | sed 's/^[^=]*=//')";
    if [ -n $j ]; then DFAULTXmx="$j"; fi;
fi
if [ -f $CUSTOMCONFIG ]; then
    j="$(grep Xmx $CUSTOMCONFIG | sed 's/^[^=]*=//')";
    if [ -n $j ]; then CUSTOMXmx="$j"; fi;
fi

CLASSPATH=""
for N in lib/*.jar; do CLASSPATH="$CLASSPATH$N:"; done
CLASSPATH=".:./classes/:$CLASSPATH"

cmdline="java";

if [ -n "$ENVXmx" ] ; then cmdline="$cmdline -Xmx$ENVXmx";
elif [ -n "$CUSTOMXmx" ]; then cmdline="$cmdline -Xmx$CUSTOMXmx";
elif [ -n "$DFAULTXmx" ]; then cmdline="$cmdline -Xmx$DFAULTXmx";
fi

echo "starting loklak"
echo "startup" > $STARTUPFILE

cmdline="$cmdline -server -classpath $CLASSPATH -Dlog4j.configurationFile=$LOGCONFIG org.loklak.LoklakServer >> data/loklak.log 2>&1 &";

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
	echo "loklak server started at port $CUSTOMPORT, open your browser at $LOCALHOST"
	rm -f $STARTUPFILE
	exit 0
else
	echo "loklak server failed to start. See data/loklag.log for details. Here are the last logs:"
    tail data/loklak.log
	rm -f $STARTUPFILE
	exit 1
fi
