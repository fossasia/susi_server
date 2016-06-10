#!/usr/bin/env sh
cd `dirname $0`/..
mkdir -p data
#to not allow process to overwrite the already running one.
x=`cat data/loklak.pid 2>/dev/null` && ps -A | egrep "^\ $x" >/dev/null 2>/dev/null
if [ "$x" != "" ]; then
	echo "Server is already running, please stop it and then start"
	exit 0
fi

DFAULTCONFIG="conf/config.properties"
CUSTOMCONFIG="data/settings/customized_config.properties"
LOGCONFIG="conf/logs/log-to-file.properties"
STARTUPFILE="data/startup.tmp"
DFAULTXmx="-Xmx800m";
CUSTOMXmx=""
if [ -f $DFAULTCONFIG ]; then
 j="`grep Xmx $DFAULTCONFIG | sed 's/^[^=]*=//'`";
 if [ -n $j ]; then DFAULTXmx="$j"; fi;
fi
if [ -f $CUSTOMCONFIG ]; then
 j="`grep Xmx $CUSTOMCONFIG | sed 's/^[^=]*=//'`";
 if [ -n $j ]; then CUSTOMXmx="$j"; fi;
fi

#echo "DFAULTXmx: !$DFAULTXmx!"
#echo "CUSTOMXmx: !$CUSTOMXmx!"

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

cmdline="$cmdline -server -classpath $CLASSPATH -Dlog4j.configurationFile=$LOGCONFIG org.loklak.LoklakServer >> /dev/null 2>&1 & echo \$! > data/loklak.pid &";

eval $cmdline
while [ -f $STARTUPFILE ]; do 
	if [ `cat $STARTUPFILE` = 'done' ]; then
		break
	else
		sleep 1
	fi
done

if [ -f $STARTUPFILE ]; then
	CUSTOMPORT=$(grep -iw 'port.http' conf/config.properties | sed 's/^[^=]*=//' );
	LOCALHOST=$(grep -iw 'shortlink.urlstub' conf/config.properties | sed 's/^[^=]*=//');
	echo "loklak server started at port $CUSTOMPORT, open your browser at $LOCALHOST"
	rm $STARTUPFILE
	exit 0
else
	echo "loklak server failed to start. See data/loklag.log for details"
	exit 1
fi
