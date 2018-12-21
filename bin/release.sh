#!/usr/bin/env bash

cd $(dirname $0)/..
source bin/preload.sh

while getopts ":pb" opt; do
    case $opt in
        p)
	    git reset --hard
            git pull origin master
            ;;
        b)
	    gradle assemble
            ;;
        \?)
            echo "Usage: $0 [options...]"
            echo -e " -p\tpull latest commit and overwrite all local changes"
            echo -e " -b\trun the gradle build script"
            exit 1
            ;;
    esac
done

# variables used from preload.sh
# JARFILE="build/libs/susi_server-all.jar"

# make release file name
GITHASH=`git log -n 1 --pretty=format:"%h"`
RELEASE_PATH=release
RELEASE_FILE=susi_server_binary_`date "+%Y%m%d"`_$GITHASH
LATEST_FILE=susi_server_binary_latest.tar.gz

# clean up
rm -Rf $RELEASE_PATH/$RELEASE_FILE
rm -f $RELEASE_PATH/${RELEASE_FILE}.tar.gz

# make release file structure
mkdir $RELEASE_PATH/$RELEASE_FILE
mkdir $RELEASE_PATH/$RELEASE_FILE/bin
mkdir $RELEASE_PATH/$RELEASE_FILE/build
mkdir $RELEASE_PATH/$RELEASE_FILE/build/libs

# copy files
cp -R conf $RELEASE_PATH/$RELEASE_FILE/
cp -R html $RELEASE_PATH/$RELEASE_FILE/
cp bin/*.sh $RELEASE_PATH/$RELEASE_FILE/bin/
cp $JARFILE $RELEASE_PATH/$RELEASE_FILE/$JARFILE

# compress folder
cd $RELEASE_PATH
tar cf ${RELEASE_FILE}.tar $RELEASE_FILE
gzip -9 ${RELEASE_FILE}.tar
rm -Rf $RELEASE_FILE
cd ..

# link latest file
rm -f $RELEASE_PATH/$LATEST_FILE
ln -s $RELEASE_PATH/${RELEASE_FILE}.tar.gz $RELEASE_PATH/$LATEST_FILE

# finish!
echo "wrote ${RELEASE_FILE}.tar.gz"
