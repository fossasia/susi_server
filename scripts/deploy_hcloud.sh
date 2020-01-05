#!/bin/bash

if [ -z "$USER_NAME" ] || [ -z "$IP" ] || [ -z "$encrypted_0139fbcb2566_key" ] || [ -z "$encrypted_0139fbcb2566_iv" ]; then
    echo "Skipping VPS deployment. Environment variables not set."
    exit 0
fi

openssl aes-256-cbc -K $encrypted_0139fbcb2566_key -iv $encrypted_0139fbcb2566_iv -in deploy_rsa.enc -out deploy_rsa -d
eval "$(ssh-agent -s)"
cp deploy_rsa ~/.ssh/deploy_rsa
chmod 600 ~/.ssh/deploy_rsa
ssh-add ~/.ssh/deploy_rsa

set -e
git config --global push.default simple # we only want to push one branch — master
git fetch --unshallow origin
ssh-keyscan -H $IP >> ~/.ssh/known_hosts
# add repo on vps as a repote
git remote add production ssh://$USER_NAME@$IP/home/$USER_NAME/susi_server
# push updates
git push -f production HEAD:master
# build and start susi server
ssh $USER_NAME@$IP <<EOF
  cd susi_server
  bin/stop.sh
  git submodule update --recursive --remote
  git submodule update --init --recursive
  mkdir -p data/generic_skills/
  touch data/generic_skills/media_discovery
  ./gradlew build

EOF
ssh $USER_NAME@$IP <<EOF
  cd susi_server
  bin/start.sh
EOF
