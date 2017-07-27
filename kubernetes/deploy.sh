#!/bin/bash
export DIR=${BASH_SOURCE%/*}

if [ "$1" = "create" ]; then
    echo "Deploying the project to kubernetes cluster"
    kubectl create -R -f ${DIR}/yamls/susi-server
    echo "Waiting for server to start up. ~30s."
    sleep 30
    echo "Done. The project was deployed to kubernetes. :)"
fi