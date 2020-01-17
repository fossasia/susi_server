#!/usr/bin/env bash

JARFILE="build/libs/susi_server.jar"
INSTALLATIONCONFIG="data/settings/installation.txt"
PIDFILE="data/susi.pid"
DFAULTCONFIG="conf/config.properties"
CUSTOMCONFIG="data/settings/customized_config.properties"
LOGCONFIG="conf/logs/log-to-file.properties"
STARTUPFILE="data/startup.tmp"
DFAULTXmx="-Xmx800m";
CUSTOMXmx=""

mkdir -p data/settings

#to not allow process to overwrite the already running one.
if [ -f $PIDFILE ]; then
    PID=$(cat $PIDFILE 2>/dev/null)
    if [ -n $PID ]; then
        # check whether this process is actually running or a leftover
        # from a hard shutdown
        if kill -0 $PID 2>/dev/null ; then
            echo "Server is already running. Please stop it and then start."
            exit 1
        else
            echo "Removing left over $PIDFILE"
            rm $PIDFILE
        fi
    else
        rm $PIDFILE
    fi
fi

if [ -f $DFAULTCONFIG ]; then
    j="$(grep Xmx $DFAULTCONFIG | sed 's/^[^=]*=//')";
    if [ -n $j ]; then DFAULTXmx="$j"; fi;
fi
if [ -f $CUSTOMCONFIG ]; then
    j="$(grep Xmx $CUSTOMCONFIG | sed 's/^[^=]*=//')";
    if [ -n $j ]; then CUSTOMXmx="$j"; fi;
fi

if [ -f $JARFILE ]; then
    CLASSPATH="$JARFILE"
else
    echo "It seems you haven't compiled SUSI"
    echo "To build SUSI,"
    echo "$ ./gradlew build"
    exit 1
fi

cmdline="java -Xverify:none";

if [ -n "$ENVXmx" ] ; then cmdline="$cmdline -Xmx$ENVXmx";
elif [ -n "$CUSTOMXmx" ]; then cmdline="$cmdline -Xmx$CUSTOMXmx";
elif [ -n "$DFAULTXmx" ]; then cmdline="$cmdline -Xmx$DFAULTXmx";
fi

export INSTALLATIONCONFIG
export LOGCONFIG
export STARTUPFILE
export CLASSPATH
