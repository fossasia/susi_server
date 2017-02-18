#!/usr/bin/env bash

JARFILE="build/libs/susi_server-all.jar"
INSTALLATIONCONFIG="data/settings/installation.txt"
PIDFILE="data/loklak.pid"
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
    if kill $PID > /dev/null 2>&1; then
        echo "Server is already running, please stop it and then start"
        exit 1
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
    echo "It seems you haven't compile susi"
    echo "To build susi,"
    echo "$ ./gradlew build"
    exit 1
fi

cmdline="java";

if [ -n "$ENVXmx" ] ; then cmdline="$cmdline -Xmx$ENVXmx";
elif [ -n "$CUSTOMXmx" ]; then cmdline="$cmdline -Xmx$CUSTOMXmx";
elif [ -n "$DFAULTXmx" ]; then cmdline="$cmdline -Xmx$DFAULTXmx";
fi

export INSTALLATIONCONFIG
export LOGCONFIG
export STARTUPFILE
export CLASSPATH
