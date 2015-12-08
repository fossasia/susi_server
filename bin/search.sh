#!/bin/bash
# You should install jq and curl
# if you are using OSX, then you can use brew install curl jq
# And if you are using Ubuntu, then use apt-get install curl jq
curl "http://loklak.org/api/search.json?q=$1" | jq '.statuses[].text'
