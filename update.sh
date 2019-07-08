#!/bin/bash
set -e
wget http://download.susi.ai/susi_server/susi_server_binary_latest.tar.gz
tar -xzf susi_server_binary_latest.tar.gz
rm -rf bin ssi build conf html system-integration
mv susi_server_binary_latest/{bin,ssi,build,conf,html,system-integration} .
rmdir susi_server_binary_latest/release
rmdir susi_server_binary_latest/
rm susi_server_binary_latest.tar.gz

# now do
# - check for new files that **should** be included and do
#     git add .....
#   for them
# - then do git commit --amend 
# - and git push --force origin dev-dist  (or stable-dist)

