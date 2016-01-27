#Uses Python3
import urllib.request, json, sys

#Get JSON file
data = urllib.request.urlopen("127.0.0.1:9000/api/search.json?q=%s" % (sys.argv[1]))

#Read and parse JSON
data = data.read().decode('utf-8')
data = json.loads(data)

#Print out tweets
for tweet in data["statuses"]: 
	print(tweet["text"])
