#!/usr/bin/env sh
cd `dirname $0`/../data

kill `cat loklak.pid`
rm loklak.pid
