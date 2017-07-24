#!/bin/bash

# clone the github repo
git clone https://github.com/fossasia/susi_server.git /susi_server
cd /susi_server

git checkout ${BRANCH}

if [ -v COMMIT_HASH ]; then
    git reset --hard ${COMMIT_HASH}
fi

git submodule update --recursive --remote
git submodule update --init --recursive

rm -rf dependencies/public-transport-enabler/.git
rm -rf .git