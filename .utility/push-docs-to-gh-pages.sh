#!/bin/bash

echo -e "Copying CNAME ...\n"

cp .utility/CNAME $HOME

echo -e "Creating javadoc...\n"

./gradlew javadoc

echo -e "Publishing javadoc...\n"

cp -R build/docs/javadoc $HOME/javadoc-latest

echo -e "Installing requirements...\n"

cd docs
pip3 install -r requirements.txt

echo -e "Including README.rst from root to docs directory"

python3 include.py README.rst

echo -e "Generating static HTML pages for documentation...\n"

make html

rm README.rst

echo -e "Publishing documentation...\n"

cp -Rf _build/html $HOME/docs

cd $HOME
git config --global user.email "travis@travis-ci.org"
git config --global user.name "travis-ci"
git clone --quiet --branch=gh-pages git@github.com:fossasia/susi_server.git gh-pages

cd gh-pages
git rm -rf ./*
cp -Rf $HOME/docs/* .
cp -Rf $HOME/javadoc-latest ./javadoc
cp -f $HOME/CNAME .
touch .nojekyll
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
