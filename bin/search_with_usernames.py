#!/usr/bin/python2
import json
import urllib
import sys

if __name__ == "__main__":
    args = sys.argv[1:]
    if args:
        query = args[0]
    else:
        print("Usage: ./search.py QUERY")
        sys.exit(1)
    res = urllib.urlopen("http://127.0.0.1:9000/api/search.json?q={}".format(query)).read()
    data = json.loads(res)
    for tweet in data["statuses"]:
        print "@{}: {}".format(tweet["screen_name"].encode("utf-8"), tweet["text"].encode("utf-8"))
