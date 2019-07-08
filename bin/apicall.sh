#!/usr/bin/env sh
cd "`dirname $0`"
port=$(grep ^port.http= ../data/settings/customized_config.properties |cut -d= -f2)
if [ -z "$port" ]; then port=4000; fi

if which curl &>/dev/null; then
  curl -m 60 -s "http://127.0.0.1:$port/$1"
elif which wget &>/dev/null; then
  wget -q -t 1 --timeout=120 "http://127.0.0.1:$port/$1" -O -
else
  exit 1
fi

