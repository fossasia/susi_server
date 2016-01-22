#!/usr/bin/env bash
curl 'loklak.org/api/search.json?q='$1 | python -c "
import json
import sys
raw_data = sys.stdin
json_data = json.load(raw_data)
for t in json_data['statuses']:
	print(t['text']+'\n')" 