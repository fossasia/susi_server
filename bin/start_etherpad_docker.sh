#!/usr/bin/env sh
cd "`dirname $0`"

### THIS IS MADE FOR DOCKER
### Please install docker before use
### If you prefer, use start_etherpad_nodejs.sh instead (requires nodejs)

# run or start the container
if [ "$(docker ps -q -f name=etherpad)" ]; then
    echo "etherpad is already running at http://localhost:9001"
else
    if [ "$(docker ps -aq -f status=exited -f name=etherpad)" ]; then
        echo "restarting existing container"
	docker start etherpad
    else
	echo "running etherpad image"
        docker pull etherpad/etherpad
        docker run -d --restart unless-stopped -p 9001:9001 --name etherpad etherpad/etherpad
    fi

    # get the api key of etherpad out of the container
    mkdir -p ../data/etherpad-lite
    until [ "`docker inspect -f {{.State.Running}} etherpad`"=="true" ]; do
        sleep 0.1;
    done;
    sleep 3
    until docker cp etherpad:/opt/etherpad-lite/APIKEY.txt ../data/etherpad-lite/
    do
        sleep 1
    done

    # done
    echo "etherpad is running at http://localhost:9001"
fi
