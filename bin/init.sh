#!/usr/bin/env sh
cd `dirname $0`/..
mkdir -p data
mkdir -p data/settings
cp conf/config.properties data/settings/customized_config.properties
