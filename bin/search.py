#!/usr/bin/env python

import urllib
import json
import sys

searchTerm = sys.argv[1]
query = "http://127.0.0.1:9000/api/search.json?q={}".format(searchTerm)

try:
	fetchData = urllib.urlopen(query).read()
except Exception, e:
	print "! Sorry, something went wrong:"
	print "! Error: %s"%e
	sys.exit(1)

statuses = json.loads(fetchData).get("statuses")
texts = [tweet.get("text") for tweet in statuses]

for text in texts:
	print text

