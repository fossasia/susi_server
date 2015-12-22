#!/usr/bin/env python
"""
    Requirements:

    requests (installation: pip install requests)
"""

from __future__ import print_function
import requests
import sys

if len(sys.argv) < 2:
    print('Please pass in the query which you want to search for')
    sys.exit(-1)

query = sys.argv[-1]
url = 'http://loklak.org/api/search.json'
data = requests.get(url, params={'query': query})
for status in data.json()['statuses']:
    print(status['text'])
