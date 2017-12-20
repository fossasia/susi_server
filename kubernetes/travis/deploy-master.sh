#!/usr/bin/env bash
export DEPLOY_BRANCH=${DEPLOY_BRANCH:-master}

if [ "$TRAVIS_PULL_REQUEST" != "false" -o "$TRAVIS_REPO_SLUG" != "fossasia/susi_server" -o  "$TRAVIS_BRANCH" != "$DEPLOY_BRANCH" ]; then
    echo "Skip production deployment for a very good reason."
    exit 0
fi

echo ">>> Removing obsolete gcoud files"
sudo rm -f /usr/bin/git-credential-gcloud.sh
sudo rm -f /usr/bin/bq
sudo rm -f /usr/bin/gsutil
sudo rm -f /usr/bin/gcloud

echo ">>> Installing new files"
curl https://sdk.cloud.google.com | bash;
source ~/.bashrc
gcloud components install kubectl

gcloud config set compute/zone us-central1-c

echo ">>> Decrypting credentials and authenticating gcloud account"
# Decrypt the credentials we added to the repo using the key we added with the Travis command line tool
openssl aes-256-cbc -K $encrypted_fa9beebb0291_key -iv $encrypted_fa9beebb0291_iv -in ./kubernetes/travis/susi-server-d6ee8400eacd.json.enc -out susi-server-d6ee8400eacd.json -d
gcloud auth activate-service-account --key-file susi-server-d6ee8400eacd.json
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/susi-server-d6ee8400eacd.json
#susi-server-test is gcloud project id
gcloud config set project susi-server-test
gcloud container clusters get-credentials susic-master

echo ">>> Building Docker image"
cd kubernetes/images

docker build --build-arg BRANCH=$DEPLOY_BRANCH --build-arg COMMIT_HASH=$TRAVIS_COMMIT --no-cache -t fossasia/susi_server:$TRAVIS_COMMIT .
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker tag fossasia/susi_server:$TRAVIS_COMMIT fossasia/susi_server:latest-$DEPLOY_BRANCH
echo ">>> Pushing docker image"
docker push fossasia/susi_server

echo ">>> Updating deployment"
kubectl set image deployment/susi-server --namespace=web susi-server=fossasia/susi_server:$TRAVIS_COMMIT
