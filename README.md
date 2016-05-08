# loklak
[![Build Status](https://travis-ci.org/loklak/loklak_server.svg?branch=master)](https://travis-ci.org/loklak/loklak_server)
[![Join the chat at https://gitter.im/loklak/loklak](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/loklak/loklak)
[![Twitter](https://img.shields.io/twitter/url/http/shields.io.svg?style=social)](https://twitter.com/intent/tweet?text=Wow Check Loklak on @gitHub @loklak_app @lklknt: https://github.com/loklak/loklak_server &url=%5Bobject%20Object%5D)

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
[![Deploy on Scalingo](https://cdn.scalingo.com/deploy/button.svg)](https://my.scalingo.com/deploy?source=https://github.com/loklak/loklak_server)
[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://github.com/loklak/loklak_server)
[![Deploy to Docker Cloud](https://files.cloud.docker.com/images/deploy-to-dockercloud.svg)](https://cloud.docker.com/stack/deploy/)

loklak is a server application which is able to collect messages from various sources, including twitter. The server contains a search index and a peer-to-peer index sharing interface. All messages are stored in an elasticsearch index.

'Lok Lak' is also a very tasty Cambodian stir-fry meat dish (usually beef) with a LOT of fresh black pepper. If you ever have the chance to eat Beef Lok Lak, please try it. I hope not to scare vegetarians with this name, currently I am one as well.


## Why should I use loklak?

If you like to be anonymous when searching things, want to archive tweets or messages about specific topics and if you are looking for a tool to create statistics about tweet topics, then you may consider loklak. With loklak you can do:

- collect and store a very, very large amount of tweets
- create your own search engine for tweets
- omit authentication enforcement for API requests on twitter
- share tweets and tweet archives with other loklak users
- search anonymously on your own search portal
- create your own tweet search portal or statistical evaluations
- use [Kibana](https://github.com/elastic/kibana) to analyze large amounts of tweets for statistical data.

## How do I install loklak: Download, Build, Run

At this time, loklak is not provided in compiled form, you easily build it yourself. It's not difficult and done in one minute! The source code is hosted at https://github.com/loklak/loklak_server, you can download it and run loklak with:

    > git clone https://github.com/loklak/loklak_server.git
    > cd loklak_server
    > ant
    > bin/start.sh

After all server processes are running, loklak tries to open a browser page itself. If that does not happen, just open http://localhost:9000; if you made the installation on a headless or remote server, then replace 'localhost' with your server name.

To stop loklak, run: (this will block until the server has actually terminated)

    > bin/stop.sh

A self-upgrading process is available which must be triggered by a shell command. Just run:

    > bin/upgrade.sh

### How do I install loklak with Docker?
To install loklak with Docker please refer to the [loklak Docker installation readme](installation_docker.md).

### How do I deploy loklak with Heroku?
You can easily deploy to Heroku by clicking the Deploy to Heroku button above.

To install loklak using Heroku Toolbelt, please refer to the [loklak Heroku installation readme](installation_heroku.md).

### How do I deploy loklak with cloud9?
To install loklak with cloud9 please refer to the [loklak cloud9 installation readme](installation_cloud9.md).

## How do I configure loklak?

The basis configuration file is in ```conf/config.properties```. To customize these settings place a file ```customized_config.properties``` to the path ```data/settings/```

## How do I run loklak?

- build loklak (you need to do this only once, see above)
- run `bin/start.sh`
- open `http://localhost:9000` in your browser
- to shut down loklak, run `bin/stop.sh`

## How do I analyze data acquired by loklak

loklak stores data into an elasticsearch index. There is a front-end
for the index available in elasticsearch-head. To install this, do:
- `sudo npm install -g grunt-cli`
- `cd` into the parent directly of loklak_server
- `git clone git://github.com/mobz/elasticsearch-head.git`
- `cd elasticsearch-head`
- `npm install`

Run elasticsearch-head with:
- `grunt server`
..which opens the administration page at `http://localhost:9100`

## Where can I find more information and documentation?

The application has built-in documentation web pages, you will see them when you opened the application web pages or you can simply open `html/index.html` or just use http://loklak.org as reference.

## How to compile using Gradle?
- To install Gradle on ubuntu:

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


## There should be XXX and YYY can be enhanced!!

This project is considered a community work. There is no company behind loklak. The development crew consist of YOU also. I am very thankful for pull request. So if you discovered that something can be enhanced, please do it yourself and send me a pull request. If you find a bug, please try to fix it. If you report a bug to me I will possibly consider it but at the very end of a giant, always growing heap of work. The best chance for you to get things done is to try it yourself.

## Where can I report bugs?

Please see above.


## Where can I download ready-built releases of loklak?

Nowhere, you must clone the git repository of loklak and built it yourself. That's easy, just do
- `git clone https://github.com/loklak/loklak_server.git`
- `cd loklak`
- then see above ("How do I run loklak")

## Where can I get the latest news about loklak?

Hey, this is the tool for that! Just put http://loklak.org/api/search.rss?q=%23loklak into your rss reader. Oh wait.. you will get a lot of information about tasty Cambodian food with that as well. Alternatively you may also read the authors timeline using http://loklak.org/api/search.rss?q=0rb1t3r or just follow @0rb1t3r (that's a zero after the at sign)

Have fun!
@0rb1t3r
