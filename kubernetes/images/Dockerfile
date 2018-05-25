FROM alpine:latest
LABEL maintainer="Ansgar Schmidt <ansgar.schmidt@gmx.net>"

ARG BRANCH
ARG COMMIT_HASH

ENV COMMIT_HASH ${COMMIT_HASH:-null}
ENV BRANCH ${BRANCH:-development}
# Install packages and clone from Github
RUN apk update && apk add git openjdk8 bash && \
    cd / && git clone https://github.com/fossasia/susi_skill_data.git && \
    git clone https://github.com/fossasia/susi_server.git /susi_server
WORKDIR /susi_server

RUN git checkout ${BRANCH} && \
    if [ -v COMMIT_HASH ] ; then git reset --hard ${COMMIT_HASH} ; fi && \
    git submodule update --recursive --remote && \
    git submodule update --init --recursive

# remove git history to reduce size of image, compile, change config file and hack until susi support no-daemon
RUN rm -rf dependencies/public-transport-enabler/.git && \
    rm -rf .git && \
    ./gradlew build -x test && \
    sed -i.bak 's/^\(port.http=\).*/\180/'        conf/config.properties && \
    sed -i.bak 's/^\(port.https=\).*/\1443/'      conf/config.properties && \
    sed -i.bak 's/^\(upgradeInterval=\).*/\186400000000/' conf/config.properties && \
  echo "while true; do sleep 10;done" >> bin/start.sh

# start susi
CMD ["bin/start.sh", "-Idn"]
