#!/bin/bash

echo -e "Creating javadoc...\n"

./gradlew javadoc

echo -e "Publishing javadoc...\n"

 cp -R build/docs/javadoc $HOME/javadoc-latest

cd $HOME
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"
git clone --quiet --branch=gh-pages git@github.com:fossasia/susi_server.git gh-pages

cd gh-pages
git rm -rf ./javadoc
cp -Rf $HOME/javadoc-latest ./javadoc
git add -f .
git commit -m "Latest javadoc on successful travis build $TRAVIS_BUILD_NUMBER auto-pushed to gh-pages"
git push -fq origin gh-pages > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "Published Javadoc to gh-pages.\n"
    exit 0
else
    echo -e "Publishing failed. Maybe the access-token was invalid or had insufficient permissions.\n"
    exit 1
fi
