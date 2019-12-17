#!/usr/bin/env bash
curl http://localhost:9000/api/status.json
curl http://localhost:9000/api/search.json?q=spacex
curl "http://localhost:9000/api/search.json?q=spacex&source=cache"
curl "http://localhost:9000/api/search.json?q=spacex&source=twitter"
curl "http://localhost:9000/api/search.json?q=spacex&source=cache&count=0&fields=mentions,hashtags&limit=6"
curl "http://localhost:9000/api/suggest.json?q=soj&orderby=query_count"
curl "http://localhost:9000/api/suggest.json?count=20&order=asc"
curl "http://localhost:9000/api/suggest.json?q=&timezoneOffset=-60&count=90&source=query&order=asc&orderby=retrieval_next&until=now&selectby=retrieval_next&random=3&minified=true&port.http=9000&port.https=9443&peername=anonymous"
curl "localhost:9000/api/crawler.json?start=frankfurt&depth=2"
curl http://localhost:9000/api/hello.json
curl 'http://localhost:9000/api/geocode.json?data=%7B%22places%22:[%22Frankfurt%20am%20Main%22,%22New%20York%22,%22Singapore%22]%7D'
curl http://localhost:9000/api/peers.json
curl "http://localhost:9000/api/proxy.png?screen_name=loklak_app&url=https://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_bigger.png"
curl http://localhost:9000/api/import.json
curl http://localhost:9000/api/settings.json
curl "http://localhost:9000/api/account.json?action=update&data=%7B%22screen_name%22:%22test%22,%22oauth_token%22:%22abc%22,%22oauth_token_secret%22:%22def%22%7D"
curl "http://localhost:9000/api/account.json?screen_name=test"
curl "http://localhost:9000/api/account.json?action=update&data=%7B%22screen_name%22:%22test%22,%22apps%22:%7B%22wall%22:%7B%22type%22:%22horizontal%22%7D%7D%7D"
curl http://localhost:9000/api/user.json?screen_name=loklak_app
curl http://localhost:9000/api/threaddump.txt
curl http://localhost:9000/vis/map.png > /dev/null
curl "http://localhost:9000/vis/markdown.png?text=hello%20world%0Dhello%20universe&color_text=000000&color_background=ffffff&padding=3" > /dev/null
