#Returns all tweet text lines related to fossasia
import urllib
from urllib import request
import html
import json
import sys


search_term = sys.argv[1]
query = "http://loklak.org/api/search.json?q={}".format(search_term)

#get file and decode
try:
        json_data = urllib.request.Request(query)
        with urllib.request.urlopen(json_data) as data:
            data1 = str(data.read().decode('utf-8'))
        non_bmp_map = dict.fromkeys(range(0x10000, sys.maxunicode + 1), 0xfffd)
        data1 = data1.translate(non_bmp_map)
except Exception as e:
	print ("! Sorry, something went wrong:")
	print ("! Error: %s"%e)
	sys.exit(1)

#output tweets
statuses = json.loads(data1).get("statuses")
texts = [tweet.get("text") for tweet in statuses]

for text in texts:
	print (text)
