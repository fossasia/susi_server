---

layout: default

title: Susi - Docs

---
# Susi

[![Join the chat at https://gitter.im/fossasia/susi_server](https://badges.gitter.im/fossasia/susi_server.svg)](https://gitter.im/fossasia/susi_server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/fossasia/susi_server.svg?branch=development)](https://travis-ci.org/fossasia/susi_server)
[![Docker Pulls](https://img.shields.io/docker/pulls/mariobehling/loklak.svg?maxAge=2592000?style=flat-square)](https://hub.docker.com/r/mariobehling/loklak/)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/fossasia/susi_server.svg)](http://isitmaintained.com/project/fossasia/susi_server "Percentage of issues still open")
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/fossasia/susi_server.svg)](http://isitmaintained.com/project/fossasia/susi_server "Average time to resolve an issue")
[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Wow Check Susi on @gitHub @asksusi: https://github.com/fossasia/susi_server &url=%5Bobject%20Object%5D)
[![Twitter Follow](https://img.shields.io/twitter/follow/lklknt.svg?style=social&label=Follow&maxAge=2592000?style=flat-square)](https://twitter.com/lklknt)

SUSI AI is an intelligent Open Source personal assistant. It is capable of chat and voice interaction and by using APIS to perform actions such as music playback, making to-do lists, setting alarms, streaming podcasts, playing audiobooks, and providing weather, traffic, and other real time information. Additional functionalities can be added as console services using external APIs. Susi AI is able to answer questions and depending on the context will ask for additional information in order to perform a desired outcome. The core of the assistant is the SUSI AI server that holds the "ingelligence" and "personality" of SUSI. The Android and web applications make use of the APIs to access information from a hosted server.

An automatic deployment from the development branch at GitHub is available for tests here https://susi-server.herokuapp.com

## Communication

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/forum/#!forum/loklak

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_server

[**Contributior Guide**](/docs/contributor-guide.md)|
[**Documentation for Developers** :book:](/docs/documentation-guide.md)|
[**Development Workflow**](/docs/workflow.md)

## Why should I use Susi?

If you like to create your own AI, then you may consider Susi. 


## Where can I get the latest news about Susi?

Hey, this is the tool for that! Just put http://loklak.org/api/search.rss?q=%23susi into your rss reader. Oh wait.. you will get a lot of information about tasty Cambodian food with that as well. Alternatively you may also read the authors timeline using http://loklak.org/api/search.rss?q=0rb1t3r or just follow @0rb1t3r (that's a zero after the at sign)

## What is the software license?

LGPL 2.1

# Development Workflow

## Fixing issues


### Step 1: Pick an issue to fix
 
 After selecting the issue
 
  1.Comment on the issue saying you are working on the issue.

  2.We except you to discuuss the approach either by commenting or in the gitter.
  
  3.Updates or progress on the issue would be nice.

### Step 2: Branch policy<br>

 Start off from your `development` branch and make sure it is up-to-date with the latest version of the committer repo's
 `development` branch. Make sure you are working in development branch only.<br>
 `git pull upstream development`
    
  If you have not added upstream follow the steps given [here](https://help.github.com/articles/configuring-a-remote-for-a-fork/).
  
### Step 3: Coding Policy

  * Please help us follow the best practice to make it easy for the reviewer as well as the contributor. 
    We want to focus on the code quality more than on managing pull request ethics.
    
  * Single commit per pull request

  * Reference the issue numbers in the commit message. Follow the pattern Fixes #

  * Follow uniform design practices. The design language must be consistent throughout the app.

  * The pull request will not get merged until and unless the commits are squashed. 
    In case there are multiple commits on the PR,  the commit author needs to squash them and 
    not the maintainers cherrypicking and merging squashes.
  
  * If you don't know what does squashing of commits is read from [here](http://stackoverflow.com/a/35704829/6181189).

  * If the PR is related to any front end change, please attach relevant screenshots in the pull request description

### Step 4: Submitting a PR

 Once a PR is opened, try and complete it within 2 weeks, or at least stay actively working on it.
 Inactivity for a long period may necessitate a closure of the PR. As mentioned earlier updates would be nice.

### Step 5: Code Review
 Your code will be reviewed, in this sequence, by:

 * Travis CI: by building and running tests.<br>
   If there are failed tests, the build will be marked as a failure.
   You can consult the CI log to find which tests.<br>
   Ensure that all tests pass before triggering another build.
 * The CI log will also contain the command that will enable running the failed tests locally.<br>
 * Reviewer: A core team member will be assigned to the PR as its reviewer, who will approve your PR or he will suggest changes.


Have fun!
@0rb1t3r
