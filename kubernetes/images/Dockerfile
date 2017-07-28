FROM ubuntu:latest
MAINTAINER Ansgar Schmidt <ansgar.schmidt@gmx.net>
ENV DEBIAN_FRONTEND noninteractive

ARG BRANCH
ARG COMMIT_HASH

ENV COMMIT_HASH ${COMMIT_HASH:-null}
ENV BRANCH ${BRANCH:-development}

# update
RUN apt-get update
RUN apt-get upgrade -y

# add packages
RUN apt-get install -y git openjdk-8-jdk

# clone the github repo
RUN git clone https://github.com/fossasia/susi_server.git /susi_server
WORKDIR /susi_server

RUN git checkout ${BRANCH}
RUN if [ -v COMMIT_HASH ] ; then git reset --hard ${COMMIT_HASH} ; fi

RUN git submodule update --recursive --remote
RUN git submodule update --init --recursive

# remove git history to reduce size of image
RUN rm -rf dependencies/public-transport-enabler/.git
RUN rm -rf .git

# compile
RUN ./gradlew build -x test

# change config file
RUN sed -i.bak 's/^\(port.http=\).*/\180/'                conf/config.properties
RUN sed -i.bak 's/^\(port.https=\).*/\1443/'              conf/config.properties
RUN sed -i.bak 's/^\(upgradeInterval=\).*/\186400000000/' conf/config.properties

# hack until susi support no-daemon
RUN echo "while true; do sleep 10;done" >> bin/start.sh

# start susi
CMD ["bin/start.sh", "-Idn"]
