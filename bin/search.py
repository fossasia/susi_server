import argparse
import requests
import json

parser = argparse.ArgumentParser(description='Process the query input')
parser.add_argument('query', type=str, help='Query to search for')
args = parser.parse_args()
query = args.query

SEARCH_URL = 'http://loklak.org/api/search.json'
params = dict(q=query)


resp = requests.get(url=SEARCH_URL, params=params)
data = json.loads(resp.text)
statuses = data['statuses']
for status in statuses:
    print(status['text'])
