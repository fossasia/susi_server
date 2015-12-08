#!/bin/bash
curl "http://loklak.org/api/search.json?q=$1" | jq '.statuses[].text'
