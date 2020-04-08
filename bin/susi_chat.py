#!/usr/bin/env python3

import sys
import requests

response = requests.get("http://127.0.0.1:4000/susi/chat.json?timezoneOffset=-120&q=" + sys.argv[1])

print(response.json()['answers'][0]['actions'][0]['expression'])
