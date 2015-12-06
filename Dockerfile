FROM ubuntu:latest
MAINTAINER Ansgar Schmidt <ansgar.schmidt@gmx.net>
ENV DEBIAN_FRONTEND noninteractive

# update
RUN apt-get update
RUN apt-get upgrade -y

# loklak
RUN apt-get install -y git ant openjdk-7-jdk
RUN git clone https://github.com/loklak/loklak_server.git
WORKDIR loklak_server
RUN ant
RUN sed -i.bak 's/^\(port.http=\).*/\180/' conf/config.properties
RUN sed -i.bak 's/^\(port.https=\).*/\1443/' conf/config.properties
RUN echo "while true; do sleep 10;done" >> bin/start.sh
CMD ["bin/start.sh"]

EXPOSE 80 443
