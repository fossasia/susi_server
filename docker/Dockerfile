FROM openjdk:11
LABEL maintainer="Norbert Preining <norbert@preining.info>"
ENV DEBIAN_FRONTEND noninteractive

ARG default_branch=master

CMD ["bin/start.sh", "-Idn"]
# Expose the web interface ports
EXPOSE 4000 4443

RUN apt-get update && \
apt-get upgrade -y && \
rm -rf /var/lib/apt/lists/*

# clone the github repo
RUN git clone --recursive https://github.com/fossasia/susi_server.git && \
cd susi_server && \
git submodule update --init --recursive && \
git checkout $default_branch
WORKDIR susi_server

# compile
RUN ./gradlew assemble

# change config file
#RUN \
#sed -i.bak 's/^\(port.http=\).*/\18080/'            conf/config.properties && \
#sed -i.bak 's/^\(port.https=\).*/\18443/'              conf/config.properties && \

# don't do any updates from loklak
RUN \
  sed -i.bak 's/^\(upgradeInterval=\).*/\186400000000/' conf/config.properties && \
  sed -i.bak 's/^\(skill_repo\.enable=\).*/\1false/' conf/config.properties


