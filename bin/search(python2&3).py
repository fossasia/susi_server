"""
loklak Tweeet Searcher

Works with Python 2 & 3
"""

import sys
import argparse
import json
try:
    from urllib.request import urlopen  # Imports for Python 3
except ImportError:
    from urllib import urlopen          # Imports for Python 2

# Gets command line arguments
parser = argparse.ArgumentParser()
parser.add_argument("query", type=str, help="Query to search for")
args = parser.parse_args()
query = args.query

# Crafts url
SEARCH_URL = "http://loklak.org/api/search.json?q="
full_url = SEARCH_URL + query

# Gets page
page = urlopen(full_url).read().decode('utf-8')

# Parses json
data = json.loads(page)
statuses = data['statuses']

# Prints tweets
for status in statuses:
    # Note: encode is used to account for special characters that cannot be displayed in some command lines
    print(status['text'].encode('utf-8'))
    # Note: if using Python 3, they are printed as a byte
    print("")
