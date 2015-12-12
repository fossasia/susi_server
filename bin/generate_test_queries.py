#!/usr/bin/env python
import os
import sys
import time
try:
    import wikipedia
except ImportError:
    print('Queries generation requires wikipedia package. \n'
          'You can install it by running "pip install wikipedia"')
    exit()

# If user in bin directory set correct path to queries
if os.getcwd().endswith('bin'):
    save_folder = os.getcwd().rstrip('bin') + 'test/queries'
else:
    save_folder = os.getcwd() + '/test/queries'

if len(sys.argv) != 3:
    print('Please run script by format: python bin/generate_test_queries.py en 100')
    exit()

language = sys.argv[1]
queries_num = int(sys.argv[2])
wikipedia.set_lang(language)
queries = wikipedia.random(queries_num)

with open('{}/{}_{}.txt'.format(save_folder, language, int(time.time())), 'w') as file:
    for query in queries:
        if sys.version[0] == '3':
            file.write('%s\n' % query)
        else:
            file.write('%s\n' % query.encode('utf-8'))

print('Done.')
