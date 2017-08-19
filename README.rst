Susi
====

|Join the chat at https://gitter.im/fossasia/susi_server| |Build
Status| |Percentage of issues still open| |Average time
to resolve an issue| |Twitter| |Twitter Follow|

Susi AI is an intelligent Open Source personal assistant. It is capable of chat and voice interaction by using APIS to perform actions such as music playback, making to-do lists, setting alarms, streaming podcasts, playing audiobooks, and providing weather, traffic, and other real time information. Additional functionalities can be added as console services using external APIs. Susi AI is able to answer questions and depending on the context will ask for additional information in order to perform the desired outcome. The core of the assistant is the Susi AI server that holds the "intelligence" and "personality" of Susi AI. The Android and web applications make use of the APIs to access information from a hosted server.

An automatic deployment from the development branch at GitHub is available for tests here https://susi-server.herokuapp.com

Communication
-------------

Please join our mailing list to discuss questions regarding the project: https://groups.google.com/forum/#!forum/opntec-dev/

Our chat channel is on gitter here: https://gitter.im/fossasia/susi_server

How do I install Susi: Download, Build, Run
===========================================

.. note::

    - You must be logged in to Docker Cloud for the button to work correctly. If you are not logged in, you'll see a 404 error instead.
    

|Deploy| |Deploy on Scalingo| |Deploy to Bluemix| |Deploy to Docker
Cloud|

At this time, Susi AI is not provided in compiled form, you easily build it yourself. It's not difficult and done in one minute! The source code is
hosted at https://github.com/fossasia/susi_server, you can download it and run Susi AI with:

::

    > git clone https://github.com/fossasia/susi_server.git
    > cd susi_server
    > git submodule update --recursive --remote
    > git submodule update --init --recursive
    > ./gradlew build
    > bin/start.sh

For Windows Users (who are using GitBash/Cygwin or any terminal):

::

    > git clone https://github.com/fossasia/susi_server.git
    > cd susi_server
    > git checkout master
    > ant jar
    > java -jar dist/susiserver.jar
    > git checkout development
    > ant jar
    > java -jar dist/susiserver.jar

::

    To stop:
    > Press Ctrl+C

After all server processes are running, Susi AI tries to open a browser page itself. If that does not happen, just open http://localhost:4000; if you made the installation on a headless or remote server, then replace 'localhost' with your server name.

To stop Susi AI, run: (this will block until the server has actually terminated)

::

    > bin/stop.sh

A self-upgrading process is available which must be triggered by a shell command. Just run:

::

    > bin/upgrade.sh

Where can I download ready-built releases of Susi AI?
--------------------------------------------------

No-where, you must clone the git repository of Susi AI and built it yourself. That's easy, just do

-  ``git clone https://github.com/fossasia/susi_server.git``
-  ``cd susi``
-  then see below ("How do I run Susi AI")

How do I install Susi AI with Docker on Google Cloud?
----------------------------------

To install Susi AI with Docker on Google Cloud please refer to the `Susi Docker installation readme </docs/installation/installation_docker_gcloud.md>`__.

How do I install Susi AI with Docker on AWS?
----------------------------------

To install Susi AI with Docker on AWS please refer to the `Susi Docker installation readme </docs/installation/installation_docker_aws.md>`__.

How do I install Susi AI with Docker on Bluemix ?
----------------------------------

To install Susi AI with Docker on Bluemix please refer to the `Susi Docker installation readme </docs/installation/installation_docker_bluemix.md>`__.

How do I install Susi AI with Docker on Digital Ocean ?
----------------------------------

To install Susi AI with Docker on Digital Ocean please refer to the `Susi Docker installation readme </docs/installation/installation_docker_digitalocean.md>`__.

How do I deploy Susi AI with Heroku?
---------------------------------

You can easily deploy to Heroku by clicking the Deploy to Heroku button above. To install Susi AI using Heroku Toolbelt, please refer to the `Susi Heroku installation readme </docs/installation/installation_heroku.md>`__.

How do I deploy Susi AI with cloud9?
---------------------------------

To install Susi AI with cloud9 please refer to the `Susi cloud9 installation readme </docs/installation/installation_cloud9.md>`__.

How do I setup Susi AI on Eclipse?
-------------------------------

To install Susi AI on Eclipes, please refer to the `Susi Eclipse
readme </docs/installation/eclipseSetup.md>`__.

How do I run Susi AI?
------------------

-  build Susi (you need to do this only once, see above)
-  run ``bin/start.sh``
-  open ``http://localhost:4000`` in your browser
-  to shut down Susi, run ``bin/stop.sh``

How do I configure Susi AI?
------------------------

The basis configuration file is in ``conf/config.properties``. To
customize these settings place a file ``customized_config.properties``
to the path ``data/settings/``

How to compile using Gradle?
----------------------------

-  To install Gradle on Ubuntu:
   ::

       $ sudo add-apt-repository ppa:cwchien/gradle
       $ sudo apt-get update
       $ sudo apt-get install gradle
    
-  To install Gradle on Mac OS X with homebrew
   ::
   
       brew install gradle

-  To compile, first, create dir necessary for Gradle
   ::
   
       ./gradle_init.sh

   Compile the source to classes and a jar file
   ::

       gradle assemble

   Compiled file can be found in build dir Last, clean up so that we can
   still build the project using Ant
   ::
       ./gradle_clean.sh

How do I develop Skills (AI Conversation Rules) for Susi AI?
---------------------------------------------------------

The Susi AI skill language is described in the `Skill Development
Tutorial </docs/skills/susi_skill_development_tutorial.md>`__.

Why should I use Susi AI?
----------------------

If you like to create your own AI, then you may consider Susi AI.

Where can I get the latest news about Susi AI?
-------------------------------------------

Hey, this is the tool for that! Just put
http://loklak.org/api/search.rss?q=%23susi into your rss reader. Oh
wait.. you will get a lot of information about tasty Cambodian food with
that as well. Alternatively you may also read the authors timeline using
http://loklak.org/api/search.rss?q=0rb1t3r or just follow @0rb1t3r
(that's a zero after the at sign)

Where can I find documentation?
-------------------------------

The application has built-in documentation web pages, you will see them
when you opened the application web pages or you can simply open
``html/index.html`` or just use http://api.susi.ai as reference.


Where do I find the javadocs?
-----------------------------
You can build them via 'ant
javadoc'

Where can I report bugs and make feature requests?
--------------------------------------------------

This project is considered a community work. The development crew
consist of you too. I am very thankful for pull request. So if you
discovered that something can be enhanced, please do it yourself and
make a pull request. If you find a bug, please try to fix it. If you
report a bug to me I will possibly consider it but at the very end of a
giant, always growing heap of work. The best chance for you to get
things done is to try it yourself. Our `issue tracker is
here <https://github.com/fossasia/susi_server/issues>`__.

What is the software license?
-----------------------------

LGPL 2.1

Development Workflow
====================

Fixing issues
-------------

Step 1: Pick an issue to fix
~~~~~~~~~~~~~~~~~~~~~~~~~~~~

After selecting the issue

1.Comment on the issue saying you are working on the issue.

2.We expect you to discuss the approach either by commenting or in the
gitter.

3.Updates or progress on the issue would be nice.

Step 2: Branch policy
~~~~~~~~~~~~~~~~~~~~~

Start off from your ``development`` branch and make sure it is
up-to-date with the latest version of the committer repo's
``development`` branch. Make sure you are working in development branch
only. ``git pull upstream development``

If you have not added upstream follow the steps given
`here <https://help.github.com/articles/configuring-a-remote-for-a-fork/>`__.

Step 3: Coding Policy
~~~~~~~~~~~~~~~~~~~~~

-  Please help us follow the best practice to make it easy for the
   reviewer as well as the contributor. We want to focus on the code
   quality more than on managing pull request ethics.

-  Single commit per pull request


-  For writing commit messages please adhere to the `Commit style guidelines <docs/commitStyle.md>`__.


-  Follow uniform design practices. The design language must be
   consistent throughout the app.

-  The pull request will not get merged until and unless the commits are
   squashed. In case there are multiple commits on the PR, the commit
   author needs to squash them and not the maintainers cherrypicking and
   merging squashes.

-  If you don't know what does squashing of commits is read from
   `here <http://stackoverflow.com/a/35704829/6181189>`__.

-  If the PR is related to any front end change, please attach relevant
   screenshots in the pull request description

Step 4: Submitting a PR
~~~~~~~~~~~~~~~~~~~~~~~

Once a PR is opened, try and complete it within 2 weeks, or at least
stay actively working on it. Inactivity for a long period may
necessitate a closure of the PR. As mentioned earlier updates would be
nice.

Step 5: Code Review
~~~~~~~~~~~~~~~~~~~

Your code will be reviewed, in this sequence, by:

-  Travis CI: by building and running tests. If there are failed tests,
   the build will be marked as a failure. You can consult the CI log to
   find which tests. Ensure that all tests pass before triggering
   another build.
-  The CI log will also contain the command that will enable running the
   failed tests locally.
-  Reviewer: A core team member will be assigned to the PR as its
   reviewer, who will approve your PR or he will suggest changes.

Have fun! @0rb1t3r


.. |Join the chat at https://gitter.im/fossasia/susi_server| image:: https://badges.gitter.im/fossasia/susi_server.svg
   :target: https://gitter.im/fossasia/susi_server?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
.. |Build Status| image:: https://travis-ci.org/fossasia/susi_server.svg?branch=development
   :target: https://travis-ci.org/fossasia/susi_server
.. |Percentage of issues still open| image:: http://isitmaintained.com/badge/open/fossasia/susi_server.svg
   :target: http://isitmaintained.com/project/fossasia/susi_server
.. |Average time to resolve an issue| image:: http://isitmaintained.com/badge/resolution/fossasia/susi_server.svg
   :target: http://isitmaintained.com/project/fossasia/susi_server
.. |Twitter| image:: https://img.shields.io/twitter/url/http/shields.io.svg?style=social
   :target: https://twitter.com/intent/tweet?text=Wow%20Check%20Susi%20on%20@gitHub%20@asksusi:%20https://github.com/fossasia/susi_server%20&url=%5Bobject%20Object%5D
.. |Twitter Follow| image:: https://img.shields.io/twitter/follow/lklknt.svg?style=social&label=Follow&maxAge=2592000?style=flat-square
   :target: https://twitter.com/lklknt
.. |Deploy| image:: https://www.herokucdn.com/deploy/button.svg
   :target: https://heroku.com/deploy?template=https://github.com/fossasia/susi_server/tree/development
.. |Deploy on Scalingo| image:: https://cdn.scalingo.com/deploy/button.svg
   :target: https://my.scalingo.com/deploy?source=https://github.com/fossasia/susi_server
.. |Deploy to Bluemix| image:: https://bluemix.net/deploy/button.png
   :target: https://bluemix.net/deploy?repository=https://github.com/fossasia/susi_server
.. |Deploy to Docker Cloud| image:: https://files.cloud.docker.com/images/deploy-to-dockercloud.svg
   :target: https://cloud.docker.com/stack/deploy/
