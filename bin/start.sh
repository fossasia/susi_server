#!/usr/bin/env sh
cd `dirname $0`/..
mkdir -p data
CLASSPATH=""
for N in lib/*.jar; do CLASSPATH="$CLASSPATH$N:"; done
CLASSPATH=".:./classes/:$CLASSPATH"
echo "starting loklak"
java -Xmx2G -Xms2G -server -XX:+AggressiveOpts -XX:NewSize=512M -classpath $CLASSPATH org.loklak.LoklakServer >> data/loklak.log 2>&1 & echo $! > data/loklak.pid &

echo "loklak server started at port 9000, open your browser at http://localhost:9000"
