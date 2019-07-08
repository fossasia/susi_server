#!/bin/bash
set -e
wget http://download.susi.ai/susi_server/susi_server_binary_latest.tar.gz
tar -xzf susi_server_binary_latest.tar.gz
rm -rf bin build conf html system-integration
mv susi_server_binary_latest/{bin,build,conf,html,system-integration} .
rmdir susi_server_binary_latest/release
rmdir susi_server_binary_latest/
rm susi_server_binary_latest.tar.gz
