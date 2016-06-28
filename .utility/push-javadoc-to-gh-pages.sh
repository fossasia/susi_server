#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" = "loklak/loklak_server" ] && [ "$TRAVIS_JDK_VERSION" = "oraclejdk8" ] && [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then

  echo -e "Creating javadoc...\n"

  ant javadoc

  echo -e "Publishing javadoc...\n"

  cp -R html/javadoc $HOME/javadoc-latest

  cd $HOME
  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"
  git clone --quiet --branch=gh-pages https://${GH_TOKEN}@github.com/loklak/loklak_server gh-pages > /dev/null

  cd gh-pages
  git rm -rf ./*
  cp -Rf $HOME/javadoc-latest/* ./
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
  
fi

