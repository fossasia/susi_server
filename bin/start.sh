#!/usr/bin/env sh
cd `dirname $0`/..
mkdir -p data
CLASSPATH=""
for N in lib/*.jar; do CLASSPATH="$CLASSPATH$N:"; done
CLASSPATH=".:./classes/:$CLASSPATH"

java -classpath $CLASSPATH org.loklak.Main >> data/loklak.log 2>&1 & echo $! > data/loklak.pid &

echo "loklak server started at port 9100, open your browser at http://localhost:9100"
