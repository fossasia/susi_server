#!/usr/bin/env sh
cd "`dirname $0`"

### THIS IS MADE FOR NODEJS
### Please install nodejs before use
### If you prefer, use start_etherpad_docker.sh instead (requires docker)

# make data path
cd ..
if [ ! -d data ]; then
    mkdir data
fi
cd data

# download the pad source
if [ ! -f etherpad-lite.tar.gz ]; then
    curl -L https://github.com/ether/etherpad-lite/archive/1.8.4.tar.gz --output etherpad-lite.tar.gz
fi

# extract package
if [ ! -d etherpad-lite ]; then
    mkdir -p etherpad-lite
    tar xfz etherpad-lite.tar.gz --strip-components=1 -C etherpad-lite
fi

# run package
cd etherpad-lite
bin/run.sh
