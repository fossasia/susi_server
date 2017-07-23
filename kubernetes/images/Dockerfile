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

RUN bash setup.sh

WORKDIR /susi_server

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
