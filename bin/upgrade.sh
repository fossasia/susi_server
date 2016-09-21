#!/usr/bin/env sh
cd `dirname $0`/..
echo "loading latest code changes"
git pull origin master
echo "clean up"
ant clean
echo "building susi"
ant
bin/restart.sh
