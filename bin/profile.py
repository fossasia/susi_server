#!/usr/bin/env python
"""
Requirements:

requests (installation: pip install requests)
"""
import requests
import json
import os

SEARCH_URL = 'http://localhost:9000/api/search.json'
_ALL_FILES_ = []

for (d, f, filenames) in os.walk(os.path.join(os.getcwd(),
                                              '../test/queries/')):
    if filenames == "README.txt":
        continue
    else:
        _ALL_FILES_ = filenames

for single_file in _ALL_FILES_:
    path_to_file = os.path.join(os.path.abspath('../test/queries'),
                                single_file)
    with open(path_to_file, 'rb') as f:
        queries = f.readlines()

    for query in queries:
        resp = requests.get(url=SEARCH_URL, params={'source': 'cache',
                                                    'q': query.strip()})
        data = resp.json()
        statuses = data['statuses']
        for status in statuses:
            print status['text']
