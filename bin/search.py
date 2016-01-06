#!/usr/bin/env python
"""
Requirements:

requests (installation: pip install requests)
"""

import argparse
import requests
import json

parser = argparse.ArgumentParser(description='Process the query input')
parser.add_argument('query', type=str, help='Query to search for')
args = parser.parse_args()
query = args.query

SEARCH_URL = 'http://127.0.0.1:9000/api/search.json'
params = dict(q=query)


resp = requests.get(url=SEARCH_URL, params=params)
data = json.loads(resp.text)
statuses = data['statuses']
for status in statuses:
    print status['text']
