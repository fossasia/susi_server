#!/usr/bin/env sh
cd `dirname $0`/..
echo "Loading latest code changes"
git pull origin master
echo "Cleaning up"
ant clean
echo "Building SUSI"
ant
bin/restart.sh
