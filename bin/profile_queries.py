#!/usr/bin/env python
import os
import time
import json
try:
    import requests
except ImportError:
    print('Profiling script requires requests package. \n'
          'You can install it by running "pip install requests"')
    exit()

API_BASE_URL = 'http://127.0.0.1:9000/api/'

# If user in bin directory set correct path to quires
if os.getcwd().endswith('bin'):
    queries_folder = os.getcwd().rstrip('bin') + 'test/queries'
else:
    queries_folder = os.getcwd() + '/test/queries'

results = []
test_queries = []
for file_name in os.listdir(queries_folder):
    if file_name.endswith('.txt') and file_name != 'README.txt':
        for query in open('{}/{}'.format(queries_folder, file_name)).readlines():
            test_queries.append(query.rstrip())

print('Start profiling {} queries'.format(len(test_queries)))
for query in test_queries:
    time_start = time.time()

    url = API_BASE_URL + 'search.json?source=cache&q={}'.format(query)
    result = json.loads(requests.get(url).text)
    assert 'statuses' in result

    time_finish = time.time()
    results.append(time_finish - time_start)

print('Profiling finished.')
print('Average time: {} seconds'.format(sum(results) / len(results)))
print('Minimal time: {} seconds'.format(min(results)))
print('Maximal time: {} seconds'.format(max(results)))
