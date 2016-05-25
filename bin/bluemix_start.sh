#!/usr/bin/env sh
cd `dirname $0`/..
mkdir -p data

DFAULTCONFIG="conf/config.properties"
CUSTOMCONFIG="data/settings/customized_config.properties"
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


#jdk8

JAVA_DIST="openjdk-1.8.0_51"
JDK8_URL="https://download.run.pivotal.io/openjdk/lucid/x86_64/${JAVA_DIST}.tar.gz"

if [ -d .jdk8 ]; then
  echo "-----> .jdk8 folder found, moving along."

else
  echo -n "-----> .jdk8 folder not found! "
  if [[ -d "$PWD/.jdk8" ]]; then
    echo -n "Copying jdk from cache to app... "
    cp -r "$PWD/.jdk8" "$PWD"
    echo "Done!"

  else
    echo -n "-----> Installing ${JAVA_DIST} build (to .jdk8)....."
    mkdir "$PWD/.jdk8"
    cd "$PWD/.jdk8"
    curl --max-time 180 --location "$JDK8_URL" | tar xz
    cd "$PWD"
    echo "Done!"
  fi
fi

cd $PWD

export JAVA_HOME="$PWD/.jdk8"
export PATH="$JAVA_HOME/bin:$PATH"


echo "starting loklak"

cmdline="$cmdline -server -classpath $CLASSPATH org.loklak.LoklakServer";

eval $cmdline
#echo $cmdline;

echo "loklak server started at port 9000, open your browser at http://localhost:9000"
