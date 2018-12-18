#!/usr/bin/env sh
cd "`dirname $0`"
cd ..

githash=`git log -n 1 --pretty=format:"%h"`
release_path=release
release=susi_server_`date "+%Y%m%d"`_$githash
jar=build/libs/susi_server-all.jar
rm -Rf $release_path/$release
mkdir $release_path/$release
cp -R conf $release_path/$release/
cp -R html $release_path/$release/
mkdir $release_path/$release/bin
cp bin/*.sh $release_path/$release/bin/
cp $jar $release_path/$release/
#gradle assemble

