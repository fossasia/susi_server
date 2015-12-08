#!/bin/bash
# This script needs jq
# If you are using OSX, then you can use brew install  jq
# And if you are using Ubuntu, then use apt-get install jq
cd "`dirname $0`"
./apicall.sh "api/search.json?q=$1" | jq -r '.statuses[].text'
