FROM openjdk:8
LABEL maintainer="Ansgar Schmidt <ansgar.schmidt@gmx.net>"
ENV DEBIAN_FRONTEND noninteractive
CMD ["bin/start.sh", "-Idn"]
# Expose the web interface ports
EXPOSE 80 443

RUN apt-get update && \
apt-get upgrade -y && \
rm -rf /var/lib/apt/lists/*



# clone the github repo
RUN git clone --recursive https://github.com/fossasia/susi_server.git && \
cd susi_server && \
git submodule update --init --recursive
WORKDIR susi_server

# compile
RUN ./gradlew assemble

# change config file
RUN sed -i.bak 's/^\(port.http=\).*/\180/'            conf/config.properties && \
sed -i.bak 's/^\(port.https=\).*/\1443/'              conf/config.properties && \
sed -i.bak 's/^\(upgradeInterval=\).*/\186400000000/' conf/config.properties && \
echo "while true; do sleep 10;done" >> bin/start.sh
