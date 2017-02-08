# How do I install Susi: Download, Build, Run

[![Deploy](https://www.herokucdn.com/deploy/button.svg)](https://heroku.com/deploy)
[![Deploy on Scalingo](https://cdn.scalingo.com/deploy/button.svg)](https://my.scalingo.com/deploy?source=https://github.com/fossasia/susi_server)
[![Deploy to Bluemix](https://bluemix.net/deploy/button.png)](https://bluemix.net/deploy?repository=https://github.com/fossasia/susi_server)
[![Deploy to Docker Cloud](https://files.cloud.docker.com/images/deploy-to-dockercloud.svg)](https://cloud.docker.com/stack/deploy/)

At this time, Susi is not provided in compiled form, you easily build it yourself. It's not difficult and done in one minute! The source code is hosted at https://github.com/fossasia/susi_server, you can download it and run Susi with:

    > git clone https://github.com/fossasia/susi_server.git
    > cd susi_server
    > ant
    > bin/start.sh

For Windows Users (who are using GitBash/Cygwin or any terminal):
    > git clone https://github.com/fossasia/susi_server.git
    > cd susi_server
    > git checkout master
    > ant jar
    > java -jar dist/susiserver.jar
    > git checkout development
    > ant jar
    > java -jar dist/susiserver.jar
    
    To stop:
    > Press Ctrl+C


After all server processes are running, Susi tries to open a browser page itself. If that does not happen, just open http://localhost:4000; if you made the installation on a headless or remote server, then replace 'localhost' with your server name.

To stop Susi, run: (this will block until the server has actually terminated)

    > bin/stop.sh

A self-upgrading process is available which must be triggered by a shell command. Just run:

    > bin/upgrade.sh

## Where can I download ready-built releases of Susi?

No-where, you must clone the git repository of Susi and built it yourself. That's easy, just do
- `git clone https://github.com/fossasia/susi_server.git`
- `cd susi`
- then see below ("How do I run Susi")

## How do I install Susi with Docker?
To install Susi with Docker please refer to the [Susi Docker installation readme](/docs/installation_docker.md).

## How do I deploy Susi with Heroku?
You can easily deploy to Heroku by clicking the Deploy to Heroku button above. To install Susi using Heroku Toolbelt, please refer to the [Susi Heroku installation readme](/docs/installation_heroku.md).

## How do I deploy Susi with cloud9?
To install Susi with cloud9 please refer to the [Susi cloud9 installation readme](/docs/installation_cloud9.md).

## How do I setup Susi on Eclipse?

To install Susi on Eclipes, please refer to the [Susi Eclipse readme](/docs/eclipseSetup.md).

## How do I run Susi?

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

