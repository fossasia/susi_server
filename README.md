# Susi

[![Join the chat at https://gitter.im/fossasia/susi_server](https://badges.gitter.im/fossasia/susi_server.svg)](https://gitter.im/fossasia/susi_server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/fossasia/susi_server.svg?branch=development)](https://travis-ci.org/fossasia/susi_server)
[![Join the chat at https://gitter.im/loklak/loklak](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/loklak/loklak)
[![Docker Pulls](https://img.shields.io/docker/pulls/mariobehling/loklak.svg?maxAge=2592000?style=flat-square)](https://hub.docker.com/r/mariobehling/loklak/)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/fossasia/susi_server.svg)](http://isitmaintained.com/project/fossasia/susi_server "Percentage of issues still open")
[![Average time to resolve an issue](http://isitmaintained.com/badge/resolution/fossasia/susi_server.svg)](http://isitmaintained.com/project/fossasia/susi_server "Average time to resolve an issue")
[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Wow Check Susi on @gitHub @asksusi: https://github.com/fossasia/susi_server &url=%5Bobject%20Object%5D)
[![Twitter Follow](https://img.shields.io/twitter/follow/lklknt.svg?style=social&label=Follow&maxAge=2592000?style=flat-square)](https://twitter.com/lklknt)

Susi is a server application which is able to collect messages from various sources, including twitter. The server contains a search index and a peer-to-peer index sharing interface. All messages are stored in an elasticsearch index. An automatic deployment from the development branch at GitHub is available for tests here https://susi-server.herokuapp.com

## Communication

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/forum/#!forum/loklak

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_server

## Why should I use Susi?

If you like to be anonymous when searching things, want to archive tweets or messages about specific topics and if you are looking for a tool to create statistics about tweet topics, then you may consider Susi. With Susi you can:

- collect and store a very, very large amount of tweets
- create your own search engine for tweets
- omit authentication enforcement for API requests on twitter
- share tweets and tweet archives with other loklak users
- search anonymously on your own search portal
- create your own tweet search portal or statistical evaluations
- use [Kibana](https://github.com/elastic/kibana) to analyze large amounts of tweets for statistical data.

## How do I install Susi: Download, Build, Run

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
[![Deploy on Scalingo](https://cdn.scalingo.com/deploy/button.svg)](https://my.scalingo.com/deploy?source=https://github.com/fossasia/susi_server)
[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://github.com/fossasia/susi_server)
[![Deploy to Docker Cloud](https://files.cloud.docker.com/images/deploy-to-dockercloud.svg)](https://cloud.docker.com/stack/deploy/)

At this time, Susi is not provided in compiled form, you easily build it yourself. It's not difficult and done in one minute! The source code is hosted at https://github.com/fossasia/susi_server, you can download it and run Susi with:

    > git clone https://github.com/fossasia/susi_server.git
    > cd susi_server
    > gradle run

For Windows Users
    > git clone https://github.com/fossasia/susi_server.git
    > cd susi_server
    > gradlew run
   
    To stop:
    > Press Ctrl+C


After all server processes are running, Susi tries to open a browser page itself. If that does not happen, just open http://localhost:4000; if you made the installation on a headless or remote server, then replace 'localhost' with your server name.

To stop Susi, run: (this will block until the server has actually terminated)

    > bin/stop.sh

A self-upgrading process is available which must be triggered by a shell command. Just run:

    > bin/upgrade.sh

### Where can I download ready-built releases of Susi?

No-where, you must clone the git repository of Susi and built it yourself. That's easy, just do
- `git clone https://github.com/fossasia/susi_server.git`
- `cd susi`
- then see above ("How do I run Susi")

### How do I install Susi with Docker?
To install Susi with Docker please refer to the [Susi Docker installation readme](/docs/installation_docker.md).

### How do I deploy Susi with Heroku?
You can easily deploy to Heroku by clicking the Deploy to Heroku button above. To install Susi using Heroku Toolbelt, please refer to the [Susi Heroku installation readme](/docs/installation_heroku.md).

### How do I deploy Susi with cloud9?
To install Susi with cloud9 please refer to the [Susi cloud9 installation readme](/docs/installation_cloud9.md).

### How do I setup Susi on Eclipse?

To install Susi on Eclipes, please refer to the [Susi Eclipse readme](/docs/eclipseSetup.md).

### How do I run Susi?

- build Susi (you need to do this only once, see above)
- run `bin/start.sh`
- open `http://localhost:4000` in your browser
- to shut down Susi, run `bin/stop.sh`

## How do I analyze data acquired by Susi

Susi stores data into an elasticsearch index. There is a front-end
for the index available in elasticsearch-head. To install this, do:
- `sudo npm install -g grunt-cli`
- `cd` into the parent directly of Susi_server
- `git clone git://github.com/mobz/elasticsearch-head.git`
- `cd elasticsearch-head`
- `npm install`

Run elasticsearch-head with:
- `grunt server`
..which opens the administration page at `http://localhost:9100`

## How do I configure Susi?

The basis configuration file is in ```conf/config.properties```. To customize these settings place a file ```customized_config.properties``` to the path ```data/settings/```

## Where can I find documentation?

The application has built-in documentation web pages, you will see them when you opened the application web pages or you can simply open `html/index.html` or just use http://api.asksusi.com as reference. 

### Where can I find showcases and tutorials?

Articles and tutorials are also on our blog at http://blog.loklak.net.

### Where do I find the javadocs?

At http://susi.github.io/susi_server/ or by building them via 'ant javadoc'

### Where can I get the latest news about Susi?

Hey, this is the tool for that! Just put http://loklak.org/api/search.rss?q=%23susi into your rss reader. Oh wait.. you will get a lot of information about tasty Cambodian food with that as well. Alternatively you may also read the authors timeline using http://loklak.org/api/search.rss?q=0rb1t3r or just follow @0rb1t3r (that's a zero after the at sign)

## How to compile using Gradle?
- To install Gradle on Ubuntu:

  ```
  $ sudo add-apt-repository ppa:cwchien/gradle

  $ sudo apt-get update

  $ sudo apt-get install gradle
  ```
- To install Gradle on Mac OS X with homebrew

  ```
  brew install gradle
  ```
- To compile, first, create dir necessary for Gradle

  ```
  ./gradle_init.sh
  ```

  Compile the source to classes and a jar file

  ```
  gradle assemble
  ```

  Compiled file can be found in build dir
  Last, clean up so that we can still build the project using Ant

  ```
  ./gradle_clean.sh
  ```


## What is the software license?

LGPL 2.1


## Where can I report bugs and make feature requests?

This project is considered a community work. The development crew consist of YOU too. I am very thankful for pull request. So if you discovered that something can be enhanced, please do it yourself and make a pull request. If you find a bug, please try to fix it. If you report a bug to me I will possibly consider it but at the very end of a giant, always growing heap of work. The best chance for you to get things done is to try it yourself. Our [issue tracker is here](https://github.com/fossasia/susi_server/issues).


Have fun!
@0rb1t3r
