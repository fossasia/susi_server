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

if [ -n "$CUSTOMXmx" ]; then cmdline="$cmdline -Xmx$CUSTOMXmx";
elif [ -n "$DFAULTXmx" ]; then cmdline="$cmdline -Xmx$DFAULTXmx";
fi


#jdk7

JAVA_DIST="openjdk1.7.0_21"
JDK7_URL="https://s3.amazonaws.com/heroku-jdk/${JAVA_DIST}.tar.gz"

if [ -d .jdk7 ]; then
  echo "-----> .jdk7 folder found, moving along."

else
  echo -n "-----> .jdk7 folder not found! "
  if [[ -d "$PWD/.jdk7" ]]; then
    echo -n "Copying jdk from cache to app... "
    cp -r "$PWD/.jdk7" "$PWD"
    echo "Done!"

  else
    echo -n "-----> Installing ${JAVA_DIST} build (to .jdk7)....."
    mkdir "$PWD/.jdk7"
    cd "$PWD/.jdk7"
    curl --max-time 180 --location "$JDK7_URL" | tar xz
    cd "$PWD"
    echo "Done!"
  fi
fi

cd $PWD

export JAVA_HOME="$PWD/.jdk7"
export PATH="$JAVA_HOME/bin:$PATH"


echo "starting loklak"

cmdline="$cmdline -server -classpath $CLASSPATH org.loklak.LoklakServer";

eval $cmdline
#echo $cmdline;

echo "loklak server started at port 9000, open your browser at http://localhost:9000"
