#!/usr/bin/env bash
export DEPLOY_BRANCH=${DEPLOY_BRANCH:-development}

if [ "$TRAVIS_REPO_SLUG" != "chiragw15/susi_server" -o  "$TRAVIS_BRANCH" != "$DEPLOY_BRANCH" ]; then
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
openssl aes-256-cbc -K $encrypted_f47ba411af0b_key -iv $encrypted_f47ba411af0b_iv -in ./kubernetes/travis/Saga-874fa83917a8.json.enc -out Saga-874fa83917a8.json -d
gcloud auth activate-service-account --key-file Saga-874fa83917a8.json
export GOOGLE_APPLICATION_CREDENTIALS=$(pwd)/Saga-874fa83917a8.json
#saga-39285 is gcloud project id
gcloud config set project saga-39285
gcloud container clusters get-credentials susic

echo ">>> Building Docker image"
cd kubernetes/images

docker build --build-arg BRANCH=$DEPLOY_BRANCH --no-cache -t chiragw15/susi_server2:$TRAVIS_COMMIT .
docker login -u="$DOCKER_USERNAME" -p="$DOCKER_PASSWORD"
docker tag chiragw15/susi_server2:$TRAVIS_COMMIT chiragw15/susi_server2:latest
echo ">>> Pushing docker image"
docker push chiragw15/susi_server2

echo ">>> Updating deployment"
kubectl set image deployment/susi-server --namespace=web susi-server=chiragw15/susi_server2:$TRAVIS_COMMIT