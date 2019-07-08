#!/usr/bin/env bash

# Make sure we're on project root
cd $(dirname $0)/..

#jdk8

JAVA_DIST="openjdk-1.8.0_51"
JDK8_URL="https://download.run.pivotal.io/openjdk/lucid/x86_64/${JAVA_DIST}.tar.gz"

if [ -d .jdk8 ]; then
  echo "-----> .jdk8 folder found, moving along."

else
  echo -n "-----> .jdk8 folder not found! "
  if [[ -d "$PWD/.jdk8" ]]; then
    echo -n "Copying JDK from cache to app... "
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

exec ./bin/start.sh -Idn
