## APIs

This API can be used to create your own social media search engine using the public and open message API. Every servlet can be called with a POST request, all but push.json can be called with a PUT request.

### Client Authentication
You can access the API without any authentication. This service can be use without subscription, however, there is a user management to grant users administration rights. Excessive usage of APIs is restricted with DoS protection. Some servlets can only be called from localhost or administration rights to protect user data submitted to the server. There are three classes for access rights:

open: access without any restrictions for all clients from any IP
limited: localhost clients or administration users are granted more data than public clients
restricted: only localhost clients or administration users are granted access
### Cross-Origin Resource Sharing
Most servlets can be called with a callback=<function-name> property to call a jsonp result format. This is sufficient to allow a cross-origin access to the API. Servlets which do not provide jsonS content (i.e. all /vis/ servlets) implement CORS headers to enable embedding of their content (applies mostly to images).

### Configuration Dependencies
Some servlets read and provide configuration data from data/settings/customized_config.properties. To enable such services please edit that file and add your custom values. Example: these values are needed by loklak_webclient.

client.apiUrl: http://localhost:9000/aaa/ 
client.domain: http://localhost:3001 
client.twitterCallbackUrl: http://localhost:3000/auth/twitter/callback 

Properties prefixed with client. are exposed at the /aaa/settings.json API to clients outside. Some outside services may share authorization values for OAuth-protected services which are also used inside of loklak_server. Some services (like /aaa/account.json) also need access tokens, which shall never be exposed. You should configure the following attributes:

client.twitterConsumerKey: <KEY HERE> 
client.twitterConsumerSecret: <KEY HERE> 
twitterAccessToken: <KEY HERE> 
twitterAccessTokenSecret: <KEY HERE> 



#### /aaa/status.json
This API is open and can be accessed without any restrictions!
The status servlet shows the size of the internal Elasticsearch search index for messages and users. Furthermore, the servlet reflects the current browser clients settings in the client_info.

{
  "system": {
    "assigned_memory": 2138046464,
    "used_memory": 1483733632,
    "available_memory": 654312832,
    "cores": 8,
    "threads": 66,
    "runtime": 9771988,
    "time_to_restart": 76628012,
    "load_system_average": 3.41,
    "load_system_cpu": 0.15860576435689824,
    "load_process_cpu": 0.06938855268126196,
    "server_threads": 6
  },
  "index": {
    "mps": 145,
    "messages": {
      "size": 817015033,
      "size_local": 817015033,
      "size_backend": 0,
      "stats": {
        "name": "messages",
        "object_cache": {
          "update": 1681695,
          "hit": 68974,
          "miss": 5093840,
          "size": 10001,
          "maxsize": 10000
        },
        "exist_cache": {
          "update": 2047729,
          "hit": 633027,
          "miss": 4460813,
          "size": 2047695,
          "maxsize": 3000000
        },
        "index": {
          "get": 0,
          "write": 1681217,
          "exist": 2047772
        }
      },
      "queue": {
        "size": 98790,
        "maxSize": 100000,
        "clients": 0
      }
    },
    "users": {
      "size": 57390340,
      "size_local": 57390340,
      "size_backend": 0,
      "stats": {
        "name": "users",
        "object_cache": {
          "update": 1687190,
          "hit": 39660,
          "miss": 5495,
          "size": 10000,
          "maxsize": 10000
        },
        "exist_cache": {
          "update": 1732345,
          "hit": 0,
          "miss": 0,
          "size": 378658,
          "maxsize": 3000000
        },
        "index": {
          "get": 5495,
          "write": 1681420,
          "exist": 0
        }
      }
    },
    "queries": {
      "size": 3958,
      "stats": {
        "name": "queries",
        "object_cache": {
          "update": 4648,
          "hit": 3166,
          "miss": 3868,
          "size": 741,
          "maxsize": 10000
        },
        "exist_cache": {
          "update": 11514,
          "hit": 162,
          "miss": 2959,
          "size": 3096,
          "maxsize": 3000000
        },
        "index": {
          "get": 747,
          "write": 3907,
          "exist": 2959
        }
      }
    },
    "accounts": {"size": 94},
    "user": {"size": 688251},
    "followers": {"size": 141},
    "following": {"size": 129}
  },
  "client_info": {
    "RemoteHost": "14.139.85.203",
    "IsLocalhost": "false",
    "request_header": {
      "Upgrade-Insecure-Requests": "1",
      "Accept-Language": "en-US,en;q=0.8",
      "Host": "susi.ai",
      "Accept-Encoding": "gzip, deflate, sdch",
      "X-Forwarded-Proto": "http",
      "X-Forwarded-For": "172.30.107.104, 14.139.85.203",
      "X-Real-IP": "14.139.85.203",
      "Via": "1.1 proxy17.nitw (squid/3.1.8)",
      "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.112 Safari/537.36",
      "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
      "Connection": "close",
      "Cache-Control": "max-age=259200"
    }
  }
}


#### /aaa/search.json
This API is open and can be accessed without any restrictions!
Get a search result. There are two formats available: JSON and RSS. To get RSS data (which is OpenSearch-compliant) use the path /aaa/search.rss . The JSON return format is a subset of the Twitter API search result but has also some additional attributes. Request properties are:

q = <query term>// space replaced by '+', i.e. q=doctor+who

The query term has a special syntax and may contain the following term tokens:

term1 term2: term1 and term2 shall appear in the Tweet text
@user: the user must be mentioned in the message
from:user: only messages published by the user
#hashtag: the message must contain the given hashtag
near:<location>: messages shall be created near the given location
since:<date>: only messages after the date (including the date), <date>=yyyy-MM-dd or yyyy-MM-dd_HH:mm
until:<date>: only messages before the date (excluding the date), <date>=yyyy-MM-dd or yyyy-MM-dd_HH:mm

Furthermore the following GET-attributes can be used:

count = <result count>// default 100, i.e. count=100, the wanted number of results
source = <cache|backend|twitter|all>// the source for the search, default all, i.e. source=cache
fields = <field name list, separated by ','>// aggregation fields for search facets, like "created_at,mentions"
limit = <maximum number of facets for each field>// a limitation of number of facets for each aggregation
timezoneOffset = <offset in minutes>// offset applied on since:, until: and the date histogram
minified = <true|false>// minify the result, default false, i.e. minified=true

A search result in JSON format looks like:

{
  "readme_0" : "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!",
  "readme_1" : "susi.ai is the framework for a message search system, not the portal, read: http://susi.ai/about.html#notasearchportal",
  "readme_2" : "This is supposed to be the back-end of a search portal. For the api, see http://susi.ai/api.html",
  "readme_3" : "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)",
  "search_metadata" : {
    "itemsPerPage" : "1",
    "count" : "1",
    "count_twitter_all" : 0,
    "count_twitter_new" : 0,
    "count_backend" : 0,
    "count_cache" : 7336,
    "hits" : 7336,
    "period" : 9223372036854775807,
    "query" : "fossasia",
    "client" : "122.175.88.159",
    "time" : 1196,
    "servicereduction" : "false",
    "scraperInfo" : "local"
  },
  "statuses" : [ {
    "timestamp" : "2016-04-21T17:33:06.959Z",
    "created_at" : "2016-04-21T15:39:39.000Z",
    "screen_name" : "notrademark",
    "text" : "@fossasia @mariobehling sure, I am happy about every pull request on GitHub. What features do you plan?",
    "link" : "https://twitter.com/notrademark/status/723174364968005632",
    "id_str" : "723174364968005632",
    "source_type" : "USER",
    "provider_type" : "REMOTE",
    "provider_hash" : "1cadbfd3",
    "retweet_count" : 0,
    "favourites_count" : 0,
    "images" : [ ],
    "images_count" : 0,
    "audio" : [ ],
    "audio_count" : 0,
    "videos" : [ ],
    "videos_count" : 0,
    "place_name" : "Planá",
    "place_id" : "",
    "place_context" : "FROM",
    "location_point" : [ 12.743780125278874, 49.86816018348006 ],
    "location_radius" : 0,
    "location_mark" : [ 12.74180946741263, 49.86938948602838 ],
    "location_source" : "PLACE",
    "hosts" : [ ],
    "hosts_count" : 0,
    "links" : [ ],
    "links_count" : 0,
    "mentions" : [ "fossasia", "mariobehling" ],
    "mentions_count" : 2,
    "hashtags" : [ ],
    "hashtags_count" : 0,
    "classifier_language" : "english",
    "classifier_language_probability" : 2.7384035E-14,
    "without_l_len" : 103,
    "without_lu_len" : 79,
    "without_luh_len" : 79,
    "user" : {
      "screen_name" : "notrademark",
      "user_id" : "2374283574",
      "name" : "msquare",
      "profile_image_url_https" : "https://pbs.twimg.com/profile_images/514057179737772032/OPi1EgNA_bigger.png",
      "appearance_first" : "2015-08-17T02:42:20.874Z",
      "appearance_latest" : "2015-08-17T02:42:20.874Z"
    }
  } ],
  "aggregations" : { }
}
The text field is evaluated and all shortened links are extracted. Furthermore all hashtags and user screen names are extracted as well and written into special index fields, which can be used for statistical evaluation:

links : all links in the message. (almost) all shortlinks are expanded!
hosts : the host names from the extracted links
mentions : all user names denoted by a '@'-prefix from the message; listed without the leading '@'
hashtags : all hashtags appearing in the message, without the leading '#'

To identify the source of the Tweet, the Tweet system and the collection source is mentioned:

source_type : The source of the message, currently only 'TWITTER' is possible
provider_type : The type of the content provider, possible values are SCRAPED and REMOTE (for pushed content)

These extracted fields are especially useful for search aggregations, see below.

### Search Result Aggregations
A search result may also have field aggregations (aka 'facets') if they are requested in the search request. To get aggregations, the attribute 'source' MUST be set to 'cache' and the fields to be aggregated must be listed in the attribute 'fields'. Aggregations are only useful if the search query contains also since- and until-modifiers to define a time frame. A typical aggregation search request sets the count of search results to zero. Example:

url : http://localhost:9000/aaa/search.json?q=spacex%20since:2015-04-01%20until:2015-04-06&source=cache&count=0&fields=mentions,hashtags&limit=6

{
  "readme_0": "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!",
  "readme_1": "susi.ai is the framework for a message search system, not the portal, read: http://susi.ai/about.html#notasearchportal",
  "readme_2": "This is supposed to be the back-end of a search portal. For the api, see http://susi.ai/api.html",
  "readme_3": "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)",
  "search_metadata": {
    "itemsPerPage": "0",
    "count": "0",
    "count_twitter_all": 0,
    "count_twitter_new": 0,
    "count_backend": 0,
    "count_cache": 26,
    "hits": 26,
    "period": 9223372036854775807,
    "query": "spacex since:2015-04-01 until:2015-04-06",
    "client": "183.82.217.193",
    "time": 59658,
    "servicereduction": "false"
  },
  "statuses": [],
  "aggregations": {
    "hashtags": {
      "spacex": 4,
      "apple": 2,
      "kca": 2,
      "nasa": 2,
      "space": 2,
      "votejkt48id": 2
    },
    "mentions": {
      "AkulaEcho": 1,
      "Conduru": 1,
      "DaveDTC": 1,
      "David_Lark": 1,
      "JosieBaik": 1,
      "NanotronicsImag": 1
    }
  }
}
Loklak can also suggest what's trending and can give you the list of trending hashtags using the same search aggregations method. To find out the most trending hashtags on loklak since a particular date. Other filters like until can also be applied, here is a sample request

url  : http://susi.ai/aaa/search.json?q=since:2016-06-01&source=cache&count=0&fields=hashtags

{
  "readme_0": "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!",
  "readme_1": "susi.ai is the framework for a message search system, not the portal, read: http://susi.ai/about.html#notasearchportal",
  "readme_2": "This is supposed to be the back-end of a search portal. For the api, see http://susi.ai/api.html",
  "readme_3": "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)",
  "search_metadata": {
    "itemsPerPage": "0",
    "count": "0",
    "count_twitter_all": 0,
    "count_twitter_new": 0,
    "count_backend": 0,
    "count_cache": 33284,
    "hits": 33284,
    "period": 9223372036854775807,
    "query": "since:2016-06-01",
    "client": "1.39.62.88",
    "time": 4,
    "servicereduction": "false",
    "index": "messages_hour"
  },
  "statuses": [],
  "aggregations": {"hashtags": {
    "job": 433,
    "hiring": 346,
    "jobs": 183,
    "nowplaying": 165,
    "careerarc": 162,
    "6yearsof1d": 115,
    "6yearsofonedirection": 110,
    "dncleaks": 99,
    "pslmatchmadeinheaven": 97,
    "hungariangp": 92
  }}
} 
A special field is the "created_at" field which will create a date histogram if listed in the GET-attribute 'fields'. The date histogram resolution depends on the time frame as given in the query modifier with the 'since' and 'until' time limits: minutes if the time frame is below three hours, hours if the time frame is below seven days and days otherwise. Example:

url : http://localhost:9000/aaa/search.json?q=spacex%20since:2015-04-05_23:10%20until:2015-04-05_23:20&source=cache&count=0&fields=created_at&timezoneOffset=-120

{
  "readme_0": "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!",
  "readme_1": "susi.ai is the framework for a message search system, not the portal, read: http://susi.ai/about.html#notasearchportal",
  "readme_2": "This is supposed to be the back-end of a search portal. For the api, see http://susi.ai/api.html",
  "readme_3": "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)",
  "search_metadata": {
    "itemsPerPage": "0",
    "count": "0",
    "count_twitter_all": 0,
    "count_twitter_new": 0,
    "count_backend": 0,
    "count_cache": 20,
    "hits": 20,
    "period": 9223372036854775807,
    "query": "spacex since:2015-04-05_23:10 until:2015-04-06_23:20",
    "client": "183.82.217.193",
    "time": 1128,
    "servicereduction": "false"
  },
  "statuses": [],
  "aggregations": {"created_at": {
    "2015-04-06 00:00": 7,
    "2015-04-06 01:00": 12,
    "2015-04-06 02:00": 0,
    "2015-04-06 03:00": 0,
    "2015-04-06 04:00": 0,
    "2015-04-06 05:00": 0,
    "2015-04-06 06:00": 0,
    "2015-04-06 07:00": 0,
    "2015-04-06 08:00": 0,
    "2015-04-06 09:00": 0,
    "2015-04-06 10:00": 0,
    "2015-04-06 11:00": 0,
    "2015-04-06 12:00": 0,
    "2015-04-06 13:00": 0,
    "2015-04-06 14:00": 1
  }}
}
It is also possible to specify the order in descending order for the following filter options favourites_count, retweet_count and the default being created_at. Here are the examples of the different queries which make this happen.

retweet count:   : http://susi.ai/aaa/search.json?timezoneOffset=-120&q=fossasia&order=retweet_count&source=cache
favorites count:   : http://localhost:9000/aaa/search.json?timezoneOffset=-120&q=fossasia&order=favourites_count&source=cache
  {
  "readme_0": "THIS JSON IS THE RESULT OF YOUR SEARCH QUERY - THERE IS NO WEB PAGE WHICH SHOWS THE RESULT!",
  "readme_1": "susi.ai is the framework for a message search system, not the portal, read: http://susi.ai/about.html#notasearchportal",
  "readme_2": "This is supposed to be the back-end of a search portal. For the api, see http://susi.ai/api.html",
  "readme_3": "Parameters q=(query), source=(cache|backend|twitter|all), callback=p for jsonp, maximumRecords=(message count), minified=(true|false)",
  "search_metadata": {
    "itemsPerPage": "100",
    "count": "100",
    "count_twitter_all": 0,
    "count_twitter_new": 0,
    "count_backend": 0,
    "count_cache": 211,
    "hits": 211,
    "query": "fossasia",
    "client": "0:0:0:0:0:0:0:1",
    "time": 34,
    "servicereduction": "false",
    "index": "messages"
  },
  "statuses": [
    {
      "timestamp": "2016-07-23T13:25:33.795Z",
      "created_at": "2016-03-25T01:09:45.000Z",
      "screen_name": "fossasia",
      "text": "Become a #FOSSASIA #GSoC student! Application deadline ~20 hours: 25 March 19:00 UTC https://developers.google.com/open-source/gsoc/ @hpdang https://pic.twitter.com/IIGYXgpJ38",
      "link": "https://twitter.com/fossasia/status/713170978025504768",
      "id_str": "713170978025504768",
      "source_type": "TWITTER",
      "provider_type": "SCRAPED",
      "retweet_count": 27,
      "favourites_count": 6,
      "images": ["https://pic.twitter.com/IIGYXgpJ38"],
      "images_count": 1,
      "audio": [],
      "audio_count": 0,
      "videos": [],
      "videos_count": 0,
      "place_name": "March",
      "place_id": "",
      "place_context": "FROM",
      "place_country": "United Kingdom",
      "place_country_code": "GB",
      "place_country_center": [
        -4.696459893461537,
        30.077280001750808
      ],
      "location_point": [
        0.08827996444341579,
        52.55131132553856
      ],
      "location_radius": 0,
      "location_mark": [
        0.0831126359772881,
        52.54923050316976
      ],
      "location_source": "PLACE",
      "hosts": [
        "developers.google.com",
        "pic.twitter.com"
      ],
      "hosts_count": 2,
      "links": [
        "https://developers.google.com/open-source/gsoc/",
        "https://pic.twitter.com/IIGYXgpJ38"
      ],
      "links_count": 2,
      "mentions": ["hpdang"],
      "mentions_count": 1,
      "hashtags": [
        "fossasia",
        "gsoc"
      ],
      "hashtags_count": 2,
      "classifier_emotion": "joy",
      "classifier_emotion_probability": 2.4644330223466497E-22,
      "classifier_language": "english",
      "classifier_language_probability": 6.620603696388408E-20,
      "without_l_len": 92,
      "without_lu_len": 84,
      "without_luh_len": 68,
      "user": {
        "appearance_first": "2016-07-23T13:25:33.809Z",
        "profile_image_url_https": "https://pbs.twimg.com/profile_images/1141238022/fossasia-cubelogo_bigger.jpg",
        "screen_name": "fossasia",
        "user_id": "157702526",
        "name": "FOSSASIA",
        "appearance_latest": "2016-07-23T13:25:33.809Z"
      }
    },
    {
      "timestamp": "2016-06-19T09:37:37.365Z",
      "created_at": "2016-03-19T06:37:30.000Z",
      "screen_name": "fossasia",
      "text": "#FOSSASIA 2016's group photo is out! :D THANK YOU FOR JOINING! #opensource #redhat #linux @opensourceway #nosql https://pic.twitter.com/VmOfUkdzg8",
      "link": "https://twitter.com/fossasia/status/711079128221396993",
      "id_str": "711079128221396993",
      "source_type": "TWITTER",
      "provider_type": "SCRAPED",
      "retweet_count": 16,
      "favourites_count": 18,
      "images": ["https://pic.twitter.com/VmOfUkdzg8"],
      "images_count": 1,
      "audio": [],
      "audio_count": 0,
      "videos": [],
      "videos_count": 0,
      "place_name": "",
      "place_id": "",
      "place_context": "ABOUT",
      "hosts": ["pic.twitter.com"],
      "hosts_count": 1,
      "links": ["https://pic.twitter.com/VmOfUkdzg8"],
      "links_count": 1,
      "mentions": ["opensourceway"],
      "mentions_count": 1,
      "hashtags": [
        "fossasia",
        "opensource",
        "redhat",
        "linux",
        "nosql"
      ],
      "hashtags_count": 5,
      "classifier_emotion": "joy",
      "classifier_emotion_probability": 4.249905094787197E-18,
      "classifier_language": "english",
      "classifier_language_probability": 2.3849911989795255E-16,
      "without_l_len": 111,
      "without_lu_len": 96,
      "without_luh_len": 52,
      "user": {
        "appearance_first": "2016-07-23T13:25:33.809Z",
        "profile_image_url_https": "https://pbs.twimg.com/profile_images/1141238022/fossasia-cubelogo_bigger.jpg",
        "screen_name": "fossasia",
        "user_id": "157702526",
        "name": "FOSSASIA",
        "appearance_latest": "2016-07-23T13:25:33.809Z"
      }
    },
    {
      "timestamp": "2016-07-23T13:25:33.794Z",
      "created_at": "2016-06-23T07:43:31.000Z",
      "screen_name": "fossasia",
      "text": "We love the @redhatopen #Developer #Portal Thanks for sharing it at #FOSSASIA https://developers.redhat.com/auth/realms/rhd/protocol/openid-connect/registrations?client_id=web&redirect_uri=http%3a%2f%2fdevelopers.redhat.com%2fconfirmation&state=707feed6-2bd7-4cbd-8076-6da431b952ed&response_type=code&sc_cid=70160000000q5gAAAQ&elqTrackId=4dbc7819a1a74e59af32f4a534e5a9cd&elq=4999a526808d4ff0a7a6c73cfa6fe06c&elqaid=26078&elqat=1&elqCampaignId=107211 @harishpillay https://pic.twitter.com/YiV3NqQAIR",
      "link": "https://twitter.com/fossasia/status/745884978777587713",
      "id_str": "745884978777587713",
      "source_type": "TWITTER",
      "provider_type": "SCRAPED",
      "retweet_count": 15,
      "favourites_count": 5,
      "images": ["https://pic.twitter.com/YiV3NqQAIR"],
      "images_count": 1,
      "audio": [],
      "audio_count": 0,
      "videos": [],
      "videos_count": 0,
      "place_name": "",
      "place_id": "",
      "place_context": "ABOUT",
      "hosts": [
        "developers.redhat.com",
        "pic.twitter.com"
      ],
      "hosts_count": 2,
      "links": [
        "https://developers.redhat.com/auth/realms/rhd/protocol/openid-connect/registrations?client_id=web&redirect_uri=http%3a%2f%2fdevelopers.redhat.com%2fconfirmation&state=707feed6-2bd7-4cbd-8076-6da431b952ed&response_type=code&sc_cid=70160000000q5gAAAQ&elqTrackId=4dbc7819a1a74e59af32f4a534e5a9cd&elq=4999a526808d4ff0a7a6c73cfa6fe06c&elqaid=26078&elqat=1&elqCampaignId=107211",
        "https://pic.twitter.com/YiV3NqQAIR"
      ],
      "links_count": 2,
      "mentions": [
        "redhatopen",
        "harishpillay"
      ],
      "mentions_count": 2,
      "hashtags": [
        "developer",
        "portal",
        "fossasia"
      ],
      "hashtags_count": 3,
      "classifier_language": "english",
      "classifier_language_probability": 4.634340188089729E-28,
      "classifier_profanity": "sex",
      "classifier_profanity_probability": 9.00210576209998E-30,
      "without_l_len": 91,
      "without_lu_len": 65,
      "without_luh_len": 36,
      "user": {
        "appearance_first": "2016-07-23T13:25:33.809Z",
        "profile_image_url_https": "https://pbs.twimg.com/profile_images/1141238022/fossasia-cubelogo_bigger.jpg",
        "screen_name": "fossasia",
        "user_id": "157702526",
        "name": "FOSSASIA",
        "appearance_latest": "2016-07-23T13:25:33.809Z"
      }
    }
  ],
  "aggregations": {}
}
Please note that the times given here as since- und until modifiers as well as the histogram dates are shifted by -120 minutes as given in the timezoneOffset attribute. This makes it possible to pass that attribute from a browser which knows the actual time zone of the web front-end user and modify search attributes and result times according to the localized time of the user.



#### /aaa/suggest.json
This API has access limitations: localhost clients are granted more data than public clients.
All search queries are recorded to be able to provide a suggestion function based on previous search requests. No IP addresses are stored together with the query term. The suggest.json API is basically a search interface on query terms with special order options. Queries on search terms are applied on index terms using a fuzzy matching algorithm. The result can be ordered by time (actuality) or by the number of appearances of the same query (popularity).

A special search term is only allowed for localhost access: the empty search term. This will list all queries so far according to the given result order. It is possible to order by first query time, latest query time, number of times a user submitted a query, number of Tweets per day estimated by the latest query result and much more. Examples:

http://localhost:9000/aaa/suggest.json?q=soj&orderby=query_count
http://localhost:9000/aaa/suggest.json?count=20&order=asc
http://localhost:9000/aaa/suggest.json?count=20&order=desc
http://localhost:9000/aaa/suggest.json?count=20&orderby=messages_per_day&order=desc
http://localhost:9000/aaa/suggest.json?until=now&selectby=retrieval_next&orderby=retrieval_next&order=desc
http://localhost:9000/aaa/suggest.json?q=&timezoneOffset=-60&count=90&source=query&order=asc&orderby=retrieval_next&until=now&selectby=retrieval_next&random=3&minified=true&port.http=9000&port.https=9443&peername=anonymous

Delete operations are also possible for localhost clients by simply adding a delete=true to the call properties. This will produce a result set like without the delete property, but all data shown are deleted afterwards.

count = <int-value>// number of queries
q = <string>// to get a list of queries which match; to get all latest: leave q empty
source = <constant>// possible values: all,query,geo; default:all
order = <constant>// possible values: desc, asc; default: desc
orderby = <constant>// a field name of the query index schema, i.e. retrieval_next or query_count
timezoneOffset = <int>// the time zone offset in minutes
since = <date>// left bound for a query time
until = <date>// right bound for a query time
minified = <boolean>// minify the result
delete = <boolean>// delete everything which is in the result set

The result shows a list of queries in the given order. Additionally to the queries, 'artificial' queries, generated by the location database, are attached as well. This API is therefore also a tool to generate location name suggestions.

{
  "search_metadata" : {
    "count" : "7",
    "hits" : 0,
    "query" : "soj",
    "order" : "ASC",
    "orderby" : "query_count",
    "client" : "0:0:0:0:0:0:0:1"
  },
  "queries" : [ {
    "query" : "Sojapango",
    "query_length" : 9,
    "source_type" : "IMPORT",
    "timezoneOffset" : 0,
    "query_first" : "2015-07-24T16:44:11.145Z",
    "retrieval_last" : "2015-07-24T16:44:11.145Z",
    "retrieval_next" : "2015-07-25T16:44:11.145Z",
    "expected_next" : "2015-07-25T04:44:11.145Z",
    "query_count" : 4,
    "retrieval_count" : 1,
    "message_period" : 86400000,
    "messages_per_day" : 1,
    "score_retrieval" : 0,
    "score_suggest" : 0
  }, {
    "query" : "Soju",
    "query_length" : 4,
    "source_type" : "IMPORT",
    "timezoneOffset" : 0,
    "query_first" : "2015-07-24T16:44:11.145Z",
    "retrieval_last" : "2015-07-24T16:44:11.145Z",
    "retrieval_next" : "2015-07-25T16:44:11.145Z",
    "expected_next" : "2015-07-25T04:44:11.145Z",
    "query_count" : 2,
    "retrieval_count" : 1,
    "message_period" : 86400000,
    "messages_per_day" : 1,
    "score_retrieval" : 0,
    "score_suggest" : 0
  }, {
    "query" : "Sojitra",
    "query_length" : 7,
    "source_type" : "IMPORT",
    "timezoneOffset" : 0,
    "query_first" : "2015-07-24T16:44:11.145Z",
    "retrieval_last" : "2015-07-24T16:44:11.145Z",
    "retrieval_next" : "2015-07-25T16:44:11.145Z",
    "expected_next" : "2015-07-25T04:44:11.145Z",
    "query_count" : 0,
    "retrieval_count" : 1,
    "message_period" : 86400000,
    "messages_per_day" : 1,
    "score_retrieval" : 0,
    "score_suggest" : 0
  } ]
}


#### /aaa/crawler.json
This API has access limitations: localhost clients are granted more data than public clients.
With the crawler servlet it is possible to retrieve mass-data from the search back-end, including the Twitter scraper. The crawler loads search results with a given set of query terms, extracts all the hashtags and user names from the result list and searches with those words again. Request properties are:

start = <terms, comma-separated>// i.e. start=fossasia,software
depth = <crawl depth>// default 0, non-localhost clients may only set a maximum of 1
hashtags = <true|false>// if true then hashtags from the results are used for the next search requests
users = <true|false>// if true then user names from the results are used for the next search requests

The crawler returns immediately with an object describing the index size (exactly the same as with the status servlets) and an object describing the crawler status:

{
  "index_sizes" : {
    "messages" : 127154,
    "users" : 56347
  },
  "crawler_status" : {
    "pending_size" : 68,
    "stacked_size" : 72,
    "processed_size" : 4,
    "pending" : [ "oVirt", "repeatedly", "FOSSAsia", "FOSSASIA", "RedHatJobs", "shwetank", "umnovnik", "12geeks", "TheTechScribe", "HerwonoWr", "ovirt", "opensource", "redhatopen", "Virtualization", "mishari", "doron_f", "penhleakchan", "dionyziz", "lilithlela", "CLT2015", "google", "sparhopper", "smokingwheels", "JetLeak", "ARQUEIRO_BR", "Emil_Blume", "catering", "appropedia", "connimark", "hanshafner", "free", "bugfix", "LordBexar", "coloofrm", "Technovelgy", "erikandgo", "onboard", "SpaceX", "TexasSpaceport", "lukealization", "space", "Tako3ka", "Mars", "UrHRGuru", "SpaceXNews", "defense", "_techstories", "defense_dp", "Scott_Allen", "rtgstrf", "canaldenoticias", "GileJudy", "NASA", "Soyuz", "justinrigodn", "ISS", "letsnurture", "BlerdTurd", "econdev", "PSBJ", "TheRealBuzz", "canalnews", "PappalardoJoe", "kdurhamevntse", "Patrick_S101", "Tesla", "EntreGulss", "US4USA" ],
    "processed" : [ "spacex", "yacy_search", "singapore", "fossasia" ]
  }
}


#### /aaa/hello.json
This API is open and can be accessed without any restrictions!
The hello servlet is part of the loklak peer-to-peer bootstrap process and shall be used to announce that a new client has been started up. The hello request is done automatically after a loklak startup against the loklak backend as configured in the settings in field backend. The back-end server then does not return any data, just an 'ok' string object.

callback = <string>// jsonp callback function name

The result is always:

{
  "status" : "ok"
}
loklak does not collect IP addresses on API interfaces where user data is collected. However, to make a peer-to-peer system possible, the collection on APIs where no user data is submitted, is necessary. The most simple API where no data is submitted at all would be most appropriate for the IP collection. That is why this API exists, because it does not take any request option and it does not deliver any specific data.

IPs, collected by this method is retrievable with the peers.json API. If a loklak user does not want that the IP addresses move along a p2p network, then simply the field backend in the configuration must be empty.



#### /aaa/geocode.json
This API is open and can be accessed without any restrictions!
This servlet provides geocoding of place names to location coordinates and also reverse geocoding of location coordinates to place names. Additionally to the added geocoding servlet: you can geocode place names into locations with this. To render markers, use the 'mark' location (they have an applied fuzziness). location coordinates are given as [lon,lat] Example usage:

http://susi.ai/aaa/geocode.json?data={%22places%22:[%22Frankfurt%20am%20Main%22,%22New%20York%22,%22Singapore%22]}
Other languages: 
http://susi.ai/aaa/geocode.json?data={%22places%22:[%22%E5%9C%A3%E8%83%A1%E5%88%A9%E5%A8%85%20%E5%BE%B7%E6%B4%9B%E9%87%8C%E4%BA%9A%22]}
Multiple Cities: 
http://susi.ai/aaa/geocode.json?minified=true&data={%22places%22:[%22Singapore%22,%20%22New%20York%22,%20%22Los%20Angeles%22]}
upgrade to geocode location detection: now recognizes also all alternative (different languages) place names. The geocode servlet shows all those alternative names in a string array. The minified version shortens this array to only one entry. The API also has reverse geocoding and fuzziness for marker computation.

callback = <string>// jsonp callback function name
minified = <boolean>// set true to minify
places = <comma-separated strings>// a list of place names
data = <json>// the json must contain a 'places' property with an array of place name strings. This is an alernative to the 'places' property.

Result-Description

{
  "locations" : {
    "Singapore" : {
      "place" : [ "Singapore", "SIN", "Sin-ka-po", "Singapore City", "Singapour", "Singapur", "Singapura", "Sinkapoure", "Sîn-kâ-po", "Tumasik", "cinkappur", "prathes singkhpor", "shingaporu", "sigapura", "sing-gapol", "sing-gapoleu", "singapura", "singkh por", "sngapwr", "snghafwrt", "syngpwr", "xin jia po", "xing jia po", "Σιγκαπούρη", "Сингапур", "Сінгапур", "סינגפור", "سنغافورة", "سنگاپور", "सिंगापुर", "सिंगापूर", "ਸਿੰਗਾਪੁਰ", "சிங்கப்பூர்", "ประเทศสิงคโปร์", "สิงค์โปร", "ປະເທດສງກະໂປ", "ປະເທດສິງກະໂປ", "စငကာပနငင", "စင်ကာပူနိုင်ငံ", "សងហបរ", "សិង្ហបុរី", "シンガポール", "新加坡", "星架坡", "싱가포르", "싱가폴" ],
      "population" : 3547809,
      "country_code" : "SG",
      "country" : "Singapore",
      "location" : [ 103.85006683126556, 1.2896698812440377 ],
      "mark" : [ 103.86065693202819, 1.2871885668277656 ]
    }
  }
}


#### /aaa/peers.json
This API is open and can be accessed without any restrictions!
This servlet combined the result of the hello calls from all peers and provides a list of addresses where the remote peers can be accessed.

{
  "peers" : [ {
    "class" : "SuggestServlet",
    "host" : "134.168.3.224",
    "port.http" : 80,
    "port.https" : 443,
    "lastSeen" : 1461655070398,
    "lastPath" : "/aaa/suggest.json",
    "peername" : "anonymous"
  }, {
    "class" : "SuggestServlet",
    "host" : "134.168.3.217",
    "port.http" : 80,
    "port.https" : 443,
    "lastSeen" : 1461654681535,
    "lastPath" : "/aaa/suggest.json",
    "peername" : "anonymous"
  }, {
    "class" : "SuggestServlet",
    "host" : "78.47.93.176",
    "port.http" : 9000,
    "port.https" : 9443,
    "lastSeen" : 1461655043101,
    "lastPath" : "/aaa/suggest.json",
    "peername" : "anonymous"
  }, {
    "class" : "SuggestServlet",
    "host" : "134.168.3.227",
    "port.http" : 80,
    "port.https" : 443,
    "lastSeen" : 1461654988168,
    "lastPath" : "/aaa/suggest.json",
    "peername" : "anonymous"
  } ],
  "count" : 4
}


#### /aaa/proxy.json
This API has access restrictions: only localhost clients are granted.
Either a URL or a screen_name can be passed to the API, or both. The proxy.json loads the URL and returns the content. If a screen_name is given, the url content is stored in a cache related to the given screen_name. If only the screen_name and no URL is given, the proxy returns the image of that user. If only a URL is given, the proxy.json tries to identify the url as user- related and loads the content. To retrieve an image, there are three possible paths: /aaa/proxy.gif, /aaa/proxy.png, or /aaa/proxy.jpg. This API supports both GET and POST requests. The HTTP GET or POST request has these attributes:

url = <string>// the public url of the data source. This URL will be used to load the content.
screen_name = <string>// importer screen name
Example

{
  "screen_name" : "loklak_app";
  "url" : "https://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_bigger.png"
}
Retrieval of the image

http://localhost:9000/aaa/proxy.png?screen_name=loklak_app&url=https://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_bigger.png



#### /aaa/push.json
This API has access limitations: localhost clients are granted more data than public clients.
Whenever a peer acquires new Tweets, it reports these to the back-end for storage. Messages are first collected and bundled into HTTP POST requests to the back-end PUSH API. This servlet can only be requested with a POST request. The post must have only one attribute:

data = <a search result object>// push the same data as it is returned with search.json

The servlet returns with

{
  "status" : "ok",
  "records" : "100",
  "new" : "7",
  "known" : "93",
  "message" : "pushed"
}
When messages arrive the back-end peer, the value "source_type" of every message is replaced with "REMOTE"



#### /aaa/push/geojson.json
This API is open and can be accessed without any restrictions!
An alternative solution to push Tweets data to back-end, using GeoJSON format. Messages are collected from provided url, which contains a GeoJSON Feature Collection. Each message is a GeoJSON Feature. Example:

{
  {
    "type": "FeatureCollection",
    "features" : [
    {
      "type": "Feature",
      "geometry": {"type": "Point", "coordinates": [20.86, 106.68]},
      "properties" :
      {
        "screen_name": "Message 1",
        "user" : {
"screen_name" : "exanonym77s",
"name" : "Example User"
        }
      }
    },
    {
      "type": "Feature",
      "geometry": {"type": "Point", "coordinates": [43.56, 1.46]},
      "properties" :
      {
        "screen_name": "Message 2",
        "user" : {
"screen_name" : "exanonym77s",
"name" : "Example User"
        }
      }
    }
    ]}
}
In the back-end, "geometry" fields are transformed into Tweet "location_point". "properties" fields are pasted as Tweet fields.

This servlet allows users to specify some rules to map "properties" fields differently, with map_type parameter.The HTTP GET or POST request has these attributes:

url* = <string>// public url to retrieve geojson data. This url will be used to update the source periodically
screen_name* = <source type>// importer screen name.
source_type* = <source type>// "source_type" value of each message.
map_type = <mapping rule(s)>// field mapping rules, separated by commas

Along with some optional parameters to specify how loklak should treat the data source:

harvesting_freq = <number>// Update frequency (in minutes) of the data source
lifetime = <number>// expiration date (in epoch time) of the data source
public = <string>// set to `true` to make the data searchable by other users
It's mandatory to mention the SOURCE_TYPE as IMPORT and also it's mandatory to have a screen_name parameter while performing the push to the geojson. The GeoJSON should follow a valid geojson feature format.

The servlet returns with the same information as /aaa/push.json.

Here is an example:

http://localhost:9000/aaa/push/geojson.json?url=mysite.com/mygeojson.json&source_type=IMPORT&screen_name=test

Another example, with 3 field mapping rules:

http://localhost:9000/aaa/push/geojson.json?url=mysite.com/mygeojson.json&source_type=IMPORT&map_type=message_name:screen_name,username:user.name,user_shortname:user.screen_name



#### /aaa/push/*.*
This API is open and can be accessed without any restrictions!
Numerous data formats are internally supported by loklak push feature, other than geojson. The main difference is that these formats are hardwired and do not support custom mapping rules. This is a non-exhaustive list of active push endpoints:

/aaa/fossasia_api.json
/aaa/freifunk_node.json
/aaa/openwifimap.json
/aaa/nodelist.json
/aaa/netmon.xml
Required attributes are:

url* = <string>// public url of the data source
screen_name* = <string>// importer screen name
Along with some optional parameters to specify how loklak should treat the data source:

harvesting_freq = <number>// Update frequency (in minutes) of the data source
lifetime = <number>// expiration date (in epoch time) of the data source
public = <string>// set to `true` to make the data searchable by other users


#### /aaa/import.json
This API is open and can be accessed without any restrictions!
While pushing custom data using /aaa/push/*.json, loklak saves metadata of the push: collection frequency, lifetime, privacy status, etc.. in an object structure called ImportProfile. This API enables Read-Update-Delete operations on those objects. The type of action to performed is defined by `action` attribute:

action = <action type>// accepted values are update|delete. Leave blank for read operation.
Request attributes are different depending on action type. For read action:

screen_name = <string>// unique screen_name of the importer
source_type = <string>// source type data
For update action:

data* = <json>// the data to update. Must contain an `id_str` field to identify the import profile to update.
For delete action:

source_url* = <string>// url of the source
screen_name* = <string>// importer screen_name. Along with source_url this attribute is required to identify the import profile.


#### /aaa/validate.json
This API is open and can be accessed without any restrictions!
In loklak, before pushing custom data, the data format is validated against its corresponding JSON schema. This step ensures the data format is correct and helps users check their data even before pushing it. Required attributes are:

url* = <string>// public url of the data
source_type* = <string>// indicate the data format to validate against. Some valid values are : FOSSASIA_API, FREIFUNK_NODE, OPENWIFIMAP, etc..


#### /aaa/settings.json
This API has access restrictions: only localhost clients are granted.
This servlet provides a method to provide the settings API for the loklak front-end connections. This servlet works for requests that are running the application from the localhost.

File to edit = /data/settings/customized_config.properties

To serve the configuration, the config file should read the parameters as follows with a client. prefix

client.test = abc

Which renders the output as

{
  "test" : "abc"
}
Here is an example :

http://localhost:9000/aaa/settings.json



#### /aaa/account.json
This API has access restrictions: only localhost clients are granted.
This servlet provides the storage and retrieval of user account data. This includes Twitter account information (OAuth tokens). This servlet can only be called from localhost.

screen_name = <user nickname>// the identifier of the account record
action = <an action>// proper values are either <empty> or 'update'
minified = <true|false>// minify the result, default false, i.e. minified=true
callback = <a jsonp function name>// use this to call the servlet with jsonp result
data = <an object to store>// if you set action=update, you must submit this data object

If you want to retrieve data from the API, just submit the screen_name and you get the stored data object returned. If you want to store one or more account objects, submit that object (these objects) inside the data value. Hint: the screen_name is not used for storage of the objects, the screen_name must be inside the data object!

The data object must have the following form:

{
  "screen_name"           : "test",        // primary key for the user
  "source_type"           : "TWITTER",     // the application which created the token, by default "TWITTER"
  "oauth_token"           : "abc",         // the oauth token
  "oauth_token_secret"    : "def",         // the oauth token_secret
  "authentication_first"  : "2015-06-07T09:39:22.341Z",        // optional
  "authentication_latest" : "2015-06-07T09:39:22.341Z",        // optional
  "apps"                  : {"wall" : {"type" : "horizontal"}} // any json
}
You can set such a record i.e. with the call

http://localhost:9000/aaa/account.json?action=update&data={"screen_name":"test","oauth_token":"abc","oauth_token_secret":"def"}

It can then be retrieved again with

http://localhost:9000/aaa/account.json?screen_name=test

To add or update a record with apps settings, simply omit the OAuth details and just set apps

http://localhost:9000/aaa/account.json?action=update&data={"screen_name":"test","apps":{"wall":{"type":"horizontal"}}}



#### /aaa/user.json
This API is open and can be accessed without any restrictions!
This servlet provides the retrieval of user followers and the accounts which the user is following. Just submit the screen_name as GET http attribute. Example:

http://susi.ai/aaa/user.json?screen_name=loklak_app

{
  "search_metadata" : {
    "client" : "122.175.88.165"
  },
  "user" : {
    "retrieval_date" : "2016-04-24T09:18:18.699Z",
    "location" : "",
    "default_profile" : true,
    "statuses_count" : 174,
    "profile_background_tile" : false,
    "lang" : "en",
    "profile_link_color" : "0084B4",
    "id" : 3090229939,
    "following" : false,
    "favourites_count" : 321,
    "profile_location" : null,
    "protected" : false,
    "profile_text_color" : "333333",
    "description" : "Large-scale distibuted tweet harvesting and analysis with free and open source software from http://t.co/D8XmZwuU2Y; maybe next-gen twitter alternative",
    "verified" : false,
    "contributors_enabled" : false,
    "profile_sidebar_border_color" : "C0DEED",
    "name" : "loklak_app",
    "profile_background_color" : "C0DEED",
    "created_at" : "Mon Mar 16 16:48:17 +0000 2015",
    "is_translation_enabled" : false,
    "default_profile_image" : false,
    "followers_count" : 46,
    "geo_enabled" : false,
    "has_extended_profile" : false,
    "profile_image_url_https" : "https://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_normal.png",
    "profile_background_image_url" : "http://abs.twimg.com/images/themes/theme1/bg.png",
    "profile_background_image_url_https" : "https://abs.twimg.com/images/themes/theme1/bg.png",
    "needs_phone_verification" : false,
    "entities" : {
      "description" : {
        "urls" : [ {
          "expanded_url" : "http://susi.ai",
          "indices" : [ 93, 115 ],
          "display_url" : "susi.ai",
          "url" : "http://t.co/D8XmZwuU2Y"
        } ]
      },
      "url" : {
        "urls" : [ {
          "expanded_url" : "http://susi.ai",
          "indices" : [ 0, 23 ],
          "display_url" : "susi.ai",
          "url" : "https://t.co/D8XmZwdjbq"
        } ]
      }
    },
    "follow_request_sent" : false,
    "url" : "https://t.co/D8XmZwdjbq",
    "suspended" : false,
    "utc_offset" : -39600,
    "time_zone" : "International Date Line West",
    "notifications" : false,
    "friends_count" : 14,
    "profile_use_background_image" : true,
    "profile_sidebar_fill_color" : "DDEEF6",
    "screen_name" : "loklak_app",
    "id_str" : "3090229939",
    "profile_image_url" : "http://pbs.twimg.com/profile_images/577512240640733184/fizL4YIn_normal.png",
    "is_translator" : false,
    "listed_count" : 9
  },
  "topology" : {
    "following" : [ ],
    "followers" : [ ],
    "unfollowing_count" : 0,
    "unfollowers" : [ ],
    "unfollowers_count" : 0,
    "following_count" : 0,
    "unfollowing" : [ ],
    "followers_count" : 0
  }
}
It is also possible to get the followers of an account all at once. To trigger this, just add the attribute followers=<maxcount> to the request. The same applies to the account which the user is following, just add following=<maxcount>. This will produce a list of <maxcount> user entries in a 'topology' object. Example :

http://susi.ai/aaa/user.json?screen_name=loklak_app&followers=10000&following=10000

{
  "search_metadata" : {
    (client) 
  }
  "user" : {
    (user data)
  },
  "topology" : {
    "following" : [{
      (user's friend's data)
    }, {
      (user's friend's data)
    },
    ...
    ],
    "followers" : [ {
      (user's follower data)
    }, {
      (user's follower data)
    },
    ...
    ],
    "retrieval_date" : "2016-04-24T09:21:22.060Z",
    "unfollowers" : [ ],
    "unfollowing_count" : 0,
    "complete" : true,
    "unfollowers_count" : 0,
    "following_count" : 10,
    "unfollowing" : [ ],
    "followers_count" : 33,
    "$P" : "I"
  }
}


#### /cms/apps.json
This API is open and can be accessed without any restrictions!
the json of /cms/apps.json now contains two more objects:

"categories" with a list of category names

"category" with an object which holds a list of the application names for each category.


Furthermore, the servlet can now be called with an 'category' property, like:

/cms/apps.json?category=Demo

This will reduce the app list to the sub-list which contains only apps from that category.

  {
    "@context": "http://schema.org",
    "@type": "SoftwareApplication",
    "name": "boilerplate",
    "headline": "Boilerplate for loklak apps",
    "alternativeHeadline": "the 'hello world' app that you want to copy-paste for your own app",
    "applicationCategory": "Demo",
    "applicationSubCategory": "Hello World",
    "operatingSystem": "http://susi.ai",
    "author": {
      "@type": "Person",
      "name": "Michael Christen",
      "url": "http://yacy.net",
      "sameAs": "https://github.com/orbiter"
    }
  }


#### /aaa/asset
This API has access limitations: localhost clients are granted more data than public clients.
Storage of data must be done using POST http commands to servlet /aaa/asset and retrieval of data must be done using GET http commands to the same servlet /aaa/asset.

POST request:

screen_name = <user nickname>// the identifier of the account record
id_str = <number>// message id
file = <file name>// loklak stores new file by this name
data = <binary>// binary data of image
For example,

http://localhost:9000/aaa/asset?id_str=608991531941425153&screen_name=loklak_messages&file=image0.png

... with data in POST parameter stores the file from /Users/admin/Desktop/wall0.png as attachment for a message with id 608991531941425153 assigned to user loklak_messages under the name image0.png

Retrieval of this file with the browser you can do with GET request to:

http://localhost:9000/aaa/asset?id_str=608991531941425153&screen_name=loklak_messages&file=image0.png



#### /aaa/threaddump.txt
This API has access limitations: localhost clients are granted more data than public clients.
Simply download http://localhost:9000/aaa/threaddump.txt file. Either GET or POST request is valid for thread dump api. No parameter is needed in this api.

http://localhost:9000/aaa/threaddump.txt contains the real-time information about loklak in JVM, including:

Memory usage
Total running time
Time to restart
Status of individual threads
Threads that have been terminated
Thread dump can be used to track the memory use of loklak, and it helps developers to discover potential problems like memory leaks. There are six states that has been assigned to a thread, see further reading: Enum Thread.State

At the end of the thread dump is a list of running thread. This list may indicate the load of the server.

Here is some example of threaddump.txt(The following example has been delibrately cut to fit in the document):

  ************* Start Thread Dump Thu Dec 31 07:51:47 CET 2015 *******************

  Assigned   Memory = 9544663040
  Used       Memory = 4937137296
  Available  Memory = 4607525744
  Runtime           = 16h 57m 16s
  Time To Restart   = 7h 2m 43s


  THREADS WITH STATES: BLOCKED


  THREADS WITH STATES: RUNNABLE

  Thread= elasticsearch[Tag][http_server_worker][T#3]{New I/O worker #37} daemon id=236 RUNNABLE
  at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:98)


  Thread= pool-1-thread-5-acceptor-0@5927ff67-httpd:9000@785dd85d{HTTP/1.1}{0.0.0.0:9000}  id=278 RUNNABLE
  at sun.nio.ch.ServerSocketChannelImpl.accept(ServerSocketChannelImpl.java:250)
  at org.eclipse.jetty.server.ServerConnector.accept(ServerConnector.java:377)


  Thread= Thread-12680  id=14172 RUNNABLE
  at java.net.Socket.connect(Socket.java:579)
  at org.loklak.api.server.SearchServlet$1.run(SearchServlet.java:113)


  Thread= pool-1-thread-2-selector-ServerConnectorManager@3242d75a/1  id=275 RUNNABLE
  at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:102)


  Thread= elasticsearch[Tag][transport_client_boss][T#1]{New I/O boss #17} daemon id=210 RUNNABLE
  at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:98)
  at org.jboss.netty.channel.socket.nio.SelectorUtil.select(SelectorUtil.java:68)actNioSelector.run(AbstractNioSelector.java:212)
  at java.lang.Thread.run(Thread.java:745)


  Thread= elasticsearch[Tag][http_server_boss][T#1]{New I/O server boss #51} daemon id=250 RUNNABLE
  at sun.nio.ch.SelectorImpl.select(SelectorImpl.java:102)
  at org.jboss.netty.channel.socket.nio.NioServerBoss.select(NioServerBoss.java:163)
  at java.lang.Thread.run(Thread.java:745)


  Thread= pool-1-thread-10  id=290 RUNNABLE
  at java.lang.Thread.getAllStackTraces(Thread.java:1640)
  at org.eclipse.jetty.servlet.ServletHandler$CachedChain.doFilter(ServletHandler.java:1652)


  THREADS WITH STATES: TIMED_WAITING

  Thread= elasticsearch[Tag][transport_client_timer][T#1]{Hashed wheel timer #1} daemon id=209 TIMED_WAITING
  at java.lang.Thread.sleep(Native Method)
  at java.lang.Thread.run(Thread.java:745)


  Thread= elasticsearch[Tag][[ttl_expire]] daemon id=192 TIMED_WAITING
  at java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject.await(AbstractQueuedSynchronizer.java:2176)
  at org.elasticsearch.indices.ttl.IndicesTTLService$Notifier.await(IndicesTTLService.java:341)
  at org.elasticsearch.indices.ttl.IndicesTTLService$PurgerThread.run(IndicesTTLService.java:147)


  Thread= elasticsearch[Tag][[timer]] daemon id=189 TIMED_WAITING
  at java.lang.Thread.sleep(Native Method)
  at org.elasticsearch.threadpool.ThreadPool$EstimatedTimeThread.run(ThreadPool.java:703)


  Thread= elasticsearch[Tag][generic][T#4] daemon id=257 TIMED_WAITING
  at java.lang.Thread.run(Thread.java:745)


  Thread= Thread-180  id=188 TIMED_WAITING
  at java.lang.Thread.sleep(Native Method)
  at org.loklak.http.AccessTracker.run(AccessTracker.java:103)


  Thread= main  id=1 TIMED_WAITING
  at org.eclipse.jetty.server.Server.join(Server.java:560)
  at org.loklak.LoklakServer.main(LoklakServer.java:324)



  THREADS WITH STATES: WAITING

  Thread= Thread-184  id=279 WAITING
  at java.util.ArrayList.add(ArrayList.java:440)
  at org.loklak.data.MessageEntry.extract(MessageEntry.java:556)
  at org.loklak.Caretaker.run(Caretaker.java:96)


  Thread= elasticsearch[Tag][search][T#12] daemon id=299 WAITING
  at java.util.concurrent.LinkedTransferQueue.take(LinkedTransferQueue.java:1137)
  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
  at java.lang.Thread.run(Thread.java:745)


  Thread= elasticsearch[Tag][listener][T#3] daemon id=259 WAITING
  at java.lang.Thread.run(Thread.java:745)


  Thread= Reference Handler daemon id=2 WAITING
  at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:133)


  Thread= Finalizer daemon id=3 WAITING
  at java.lang.ref.Finalizer$FinalizerThread.run(Finalizer.java:209)



  THREADS WITH STATES: NEW


  THREADS WITH STATES: TERMINATED


  ************* End Thread Dump Thu Dec 31 07:51:47 CET 2015 *******************

  Thread list from ThreadMXBean, 165 threads:
  Thread-12680
  Keep-Alive-SocketCleaner
  Finalizer
  Reference Handler
  main


#### /vis/map.png
This API is open and can be accessed without any restrictions!
Usage-Description

text = <text>// Value of text like Hello
mlat = <latitude coordinates>// Latitude Value
mlon = <longitude coordinates>// Longitude Value
zoom = <zoom value>// Zoom Value
width = <width>// Width Value
height = <height>// Height Value
bbox = <maplongWest,maplatSouth..>// BBOX parameter

Result-Description

{
  json
}


#### /vis/markdown.png
This API is open and can be accessed without any restrictions!
This servlet provides an image with text on it. It shall be used to attach large texts to Tweets. The servlet can be called with POST and GET html requests, however it is recommended to call this using POST to be able to attach large texts. The request properties are:

text = <text to be printed, markdown possible>// the image size adopts automatically
color_text = <text color>// 6-character hex code for the color
color_background = <background color>// 6-character hex code for the color
padding = <space around text>// this is an integer number of the pixels
uppercase = <true|false> by default true// if true the text is printed UPPERCASE

The servlet can produce also gif and jpg images, just change the extension of the servlet path.

Here is an example :

http://susi.ai/vis/markdown.png?text=hello%20world%0Dhello%20universe&color_text=000000&color_background=ffffff&padding=3

#### /vis/piechart.png
This API is open and can be accessed without any restrictions!
This servlet provides the visualization of any given JSON into a piechart which can be consumed by the clients where interactive charts cannot be sent. These images denote the piechart visualizations of a json key:value pair object. This API can be called over GET/POST to the api endpoint /vis/piechart.png with the data parameter

Here is the request for the given data:

{
  "ford": "17.272992",
  "toyota": "27.272992",
  "renault": "47.272992"
}
  
http://susi.ai/vis/piechart.png?data={%22ford%22:%2217.272992%22,%22toyota%22:%2227.272992%22,%22renault%22:%2247.272992%22}&width=1000&height=1000

data = <text>// Value of JSON Key:Value pairs to visualize
width = <text>// Width of the image needed
height = <text>// Height of the image needed

## Existing API libraries for using loklak API
If you want to use loklak within your application, There are libraries that are available for you to install directly from the library installation tools. The libraries are currently available in the following languages

Contribute to this library by opening an issue in case you find a bug or any missing functionality.

#### Python
pip install python_loklak_api
#### Node.js
npm install loklak
#### C#.NET (nuget - Windows)
Install-Package LoklakDotNet
#### Ruby
gem install loklak


#### /aaa/xml2json.json
Any well formed XML data source can be converted to JSON using the loklak's `xml2json.json` API endpoint. This servlet provides the ability to send data to loklak's API as XML and receive a JSON response.

data = The XML Data to be converted to JSON // This is the field which takes data=<XML>
You send this this information as a GET / POST request. Here's a sample XML

<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<project basedir="." default="build" name="loklak">
    <property environment="env"/>
    <property name="target" value="1.8"/>
    <property name="source" value="1.8"/>
    <path id="loklak.classpath">
        <pathelement location="classes"/>
        <pathelement location="lib/antlr-runtime-3.5.jar"/>
        <pathelement location="lib/apache-log4j-extras-1.2.17.jar"/>
        <pathelement location="lib/asm-4.1.jar"/>
        <pathelement location="lib/asm-commons-4.1.jar"/>
        <pathelement location="lib/commons-cli-1.3.1.jar"/>
        <pathelement location="lib/commons-logging-1.2.jar"/>
        <pathelement location="lib/compiler-0.8.13.jar"/>
        <pathelement location="lib/compress-lzf-1.0.2.jar"/>
        <pathelement location="lib/elasticsearch-2.3.2.jar"/>
        <pathelement location="lib/groovy-all-2.4.4.jar"/>
        <pathelement location="lib/guava-18.0.jar"/>
        <pathelement location="lib/hamcrest-core-1.3.jar"/>
        <pathelement location="lib/HdrHistogram-2.1.6.jar"/>
        <pathelement location="lib/hppc-0.7.1.jar"/>
        <pathelement location="lib/httpclient-4.5.1.jar"/>
        <pathelement location="lib/httpcore-4.4.3.jar"/>
        <pathelement location="lib/httpmime-4.5.1.jar"/>
        <pathelement location="lib/jackson-core-2.6.2.jar"/>
        <pathelement location="lib/jackson-dataformat-cbor-2.6.2.jar"/>
        <pathelement location="lib/jackson-dataformat-smile-2.6.2.jar"/>
        <pathelement location="lib/jackson-dataformat-yaml-2.6.2.jar"/>
        <pathelement location="lib/javax.servlet-api-3.1.0.jar"/>
        <pathelement location="lib/javax.mail-1.5.5.jar"/>
        <pathelement location="lib/jetty-http-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-io-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-rewrite-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-security-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-server-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-servlet-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-servlets-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-util-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jetty-webapp-9.3.9.v20160517.jar"/>
        <pathelement location="lib/jna-4.1.0.jar"/>
        <pathelement location="lib/joda-convert-1.2.jar"/>
        <pathelement location="lib/joda-time-2.8.2.jar"/>
        <pathelement location="lib/json-schema-validator-2.2.6-lib.jar"/>
        <pathelement location="lib/jsr166e-1.1.0.jar"/>
        <pathelement location="lib/jts-1.13.jar"/>
        <pathelement location="lib/junit-4.12.jar"/>
        <pathelement location="lib/log4j-1.2.17.jar"/>
        <pathelement location="lib/lucene-analyzers-common-5.5.0.jar"/>
        <pathelement location="lib/lucene-backward-codecs-5.5.0.jar"/>
        <pathelement location="lib/lucene-core-5.5.0.jar"/>
        <pathelement location="lib/lucene-grouping-5.5.0.jar"/>
        <pathelement location="lib/lucene-highlighter-5.5.0.jar"/>
        <pathelement location="lib/lucene-join-5.5.0.jar"/>
        <pathelement location="lib/lucene-memory-5.5.0.jar"/>
        <pathelement location="lib/lucene-misc-5.5.0.jar"/>
        <pathelement location="lib/lucene-queries-5.5.0.jar"/>
        <pathelement location="lib/lucene-queryparser-5.5.0.jar"/>
        <pathelement location="lib/lucene-sandbox-5.5.0.jar"/>
        <pathelement location="lib/lucene-spatial-5.5.0.jar"/>
        <pathelement location="lib/lucene-spatial3d-5.5.0.jar"/>
        <pathelement location="lib/lucene-suggest-5.5.0.jar"/>
        <pathelement location="lib/netty-3.10.5.Final.jar"/>
        <pathelement location="lib/securesm-1.0.jar"/>
        <pathelement location="lib/snakeyaml-1.15.jar"/>
        <pathelement location="lib/spatial4j-0.5.jar"/>
        <pathelement location="lib/t-digest-3.0.jar"/>
        <pathelement location="lib/twitter4j-core-4.0.2.jar"/>
        <pathelement location="lib/twitter4j-stream-4.0.2.jar"/>
    </path>

    <target name="init">
        <copy includeemptydirs="false" todir="classes">
            <fileset dir="src">
                <exclude name="**/*.launch"/>
                <exclude name="**/*.java"/>
            </fileset>
        </copy>
    </target>

    <target name="clean">
        <delete dir="classes"/>
        <delete dir="html/javadoc"/>
    </target>

    <target depends="init" name="build">
        <delete dir="classes"/>
        <mkdir dir="classes"/>
        <echo message="${ant.project.name}: ${ant.file}"/>
        <javac debug="true" destdir="classes" includeantruntime="false" source="${source}" target="${target}">
            <src path="src"/>
            <classpath refid="loklak.classpath"/>
        </javac>
    </target>

    <target name="javadoc" depends="init" description="make javadoc">
        <delete dir="html/javadoc"/>
        <javadoc destdir="html/javadoc" windowtitle="loklak javadoc" encoding="UTF-8" charset="UTF-8" access="private">
            <classpath refid="loklak.classpath"/>
            <fileset dir="src">
                <include name="**/*.java"/>
            </fileset>
        </javadoc>
    </target>

    <target depends="build,javadoc" name="all"/>

    <target name="start">
        <java classname="org.loklak.Main" failonerror="true" fork="yes">
            <jvmarg line="-ea"/>
            <classpath refid="loklak.classpath"/>
        </java>
    </target>

    <target name="jar" depends="build">
        <mkdir dir="dist"/>
        <manifestclasspath property="jar.classpath" jarfile="dist/loklak.jar">
            <classpath refid="loklak.classpath" />
        </manifestclasspath>
        <jar destfile="dist/loklak.jar" basedir="classes/">
            <manifest>
                <attribute name="Class-Path" value="${jar.classpath}" />
                <attribute name="Main-Class" value="org.loklak.LoklakServer" />
            </manifest>
        </jar>
    </target>
</project>
Query String: http://localhost:9000/aaa/xml2json.json?data=<xml>
#### Resulting JSON

  {"project": {
  "basedir": ".",
  "path": {
    "pathelement": [
      {"location": "classes"},
      {"location": "lib/antlr-runtime-3.5.jar"},
      {"location": "lib/apache-log4j-extras-1.2.17.jar"},
      {"location": "lib/asm-4.1.jar"},
      {"location": "lib/asm-commons-4.1.jar"},
      {"location": "lib/commons-cli-1.3.1.jar"},
      {"location": "lib/commons-logging-1.2.jar"},
      {"location": "lib/compiler-0.8.13.jar"},
      {"location": "lib/compress-lzf-1.0.2.jar"},
      {"location": "lib/elasticsearch-2.3.2.jar"},
      {"location": "lib/groovy-all-2.4.4.jar"},
      {"location": "lib/guava-18.0.jar"},
      {"location": "lib/hamcrest-core-1.3.jar"},
      {"location": "lib/HdrHistogram-2.1.6.jar"},
      {"location": "lib/hppc-0.7.1.jar"},
      {"location": "lib/httpclient-4.5.1.jar"},
      {"location": "lib/httpcore-4.4.3.jar"},
      {"location": "lib/httpmime-4.5.1.jar"},
      {"location": "lib/jackson-core-2.6.2.jar"},
      {"location": "lib/jackson-dataformat-cbor-2.6.2.jar"},
      {"location": "lib/jackson-dataformat-smile-2.6.2.jar"},
      {"location": "lib/jackson-dataformat-yaml-2.6.2.jar"},
      {"location": "lib/javax.servlet-api-3.1.0.jar"},
      {"location": "lib/javax.mail-1.5.5.jar"},
      {"location": "lib/jetty-http-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-io-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-rewrite-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-security-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-server-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-servlet-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-servlets-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-util-9.3.9.v20160517.jar"},
      {"location": "lib/jetty-webapp-9.3.9.v20160517.jar"},
      {"location": "lib/jna-4.1.0.jar"},
      {"location": "lib/joda-convert-1.2.jar"},
      {"location": "lib/joda-time-2.8.2.jar"},
      {"location": "lib/json-schema-validator-2.2.6-lib.jar"},
      {"location": "lib/jsr166e-1.1.0.jar"},
      {"location": "lib/jts-1.13.jar"},
      {"location": "lib/junit-4.12.jar"},
      {"location": "lib/log4j-1.2.17.jar"},
      {"location": "lib/lucene-analyzers-common-5.5.0.jar"},
      {"location": "lib/lucene-backward-codecs-5.5.0.jar"},
      {"location": "lib/lucene-core-5.5.0.jar"},
      {"location": "lib/lucene-grouping-5.5.0.jar"},
      {"location": "lib/lucene-highlighter-5.5.0.jar"},
      {"location": "lib/lucene-join-5.5.0.jar"},
      {"location": "lib/lucene-memory-5.5.0.jar"},
      {"location": "lib/lucene-misc-5.5.0.jar"},
      {"location": "lib/lucene-queries-5.5.0.jar"},
      {"location": "lib/lucene-queryparser-5.5.0.jar"},
      {"location": "lib/lucene-sandbox-5.5.0.jar"},
      {"location": "lib/lucene-spatial-5.5.0.jar"},
      {"location": "lib/lucene-spatial3d-5.5.0.jar"},
      {"location": "lib/lucene-suggest-5.5.0.jar"},
      {"location": "lib/netty-3.10.5.Final.jar"},
      {"location": "lib/securesm-1.0.jar"},
      {"location": "lib/snakeyaml-1.15.jar"},
      {"location": "lib/spatial4j-0.5.jar"},
      {"location": "lib/t-digest-3.0.jar"},
      {"location": "lib/twitter4j-core-4.0.2.jar"},
      {"location": "lib/twitter4j-stream-4.0.2.jar"}
    ],
    "id": "loklak.classpath"
  },
  "default": "build",
  "name": "loklak",
  "property": [
    {"environment": "env"},
    {
      "name": "target",
      "value": 1.8
    },
    {
      "name": "source",
      "value": 1.8
    }
  ],
  "target": [
    {
      "name": "init",
      "copy": {
        "includeemptydirs": false,
        "todir": "classes",
        "fileset": {
          "exclude": [
            {"name": "**/*.launch"},
            {"name": "**/*.java"}
          ],
          "dir": "src"
        }
      }
    },
    {
      "name": "clean",
      "delete": [
        {"dir": "classes"},
        {"dir": "html/javadoc"}
      ]
    },
    {
      "javac": {
        "includeantruntime": false,
        "debug": true,
        "src": {"path": "src"},
        "classpath": {"refid": "loklak.classpath"},
        "destdir": "classes",
        "source": "${source}",
        "target": "${target}"
      },
      "depends": "init",
      "name": "build",
      "echo": {"message": "${ant.project.name}: ${ant.file}"},
      "delete": {"dir": "classes"},
      "mkdir": {"dir": "classes"}
    },
    {
      "javadoc": {
        "charset": "UTF-8",
        "access": "private",
        "classpath": {"refid": "loklak.classpath"},
        "destdir": "html/javadoc",
        "windowtitle": "loklak javadoc",
        "encoding": "UTF-8",
        "fileset": {
          "include": {"name": "**/*.java"},
          "dir": "src"
        }
      },
      "depends": "init",
      "name": "javadoc",
      "description": "make javadoc",
      "delete": {"dir": "html/javadoc"}
    },
    {
      "depends": "build,javadoc",
      "name": "all"
    },
    {
      "java": {
        "fork": "yes",
        "classname": "org.loklak.Main",
        "classpath": {"refid": "loklak.classpath"},
        "jvmarg": {"line": "-ea"},
        "failonerror": true
      },
      "name": "start"
    },
    {
      "manifestclasspath": {
        "classpath": {"refid": "loklak.classpath"},
        "jarfile": "dist/loklak.jar",
        "property": "jar.classpath"
      },
      "depends": "build",
      "name": "jar",
      "jar": {
        "basedir": "classes/",
        "destfile": "dist/loklak.jar",
        "manifest": {"attribute": [
          {
            "name": "Class-Path",
            "value": "${jar.classpath}"
          },
          {
            "name": "Main-Class",
            "value": "org.loklak.LoklakServer"
          }
        ]}
      },
      "mkdir": {"dir": "dist"}
    }
  ]
}}
#### /aaa/csv2json.json
Any well formed CSV data source can be converted to JSON using the loklak's `csv2json.json` API endpoint. This servlet provides the ability to send data to loklak's API as CSV and receive a JSON response. To do this, every CSV line ending with \n needs to be encoded as %0A. Then pass this information as a value to data and you'll get a response.

data = The CSV Data to be converted to JSON // This is the field which takes data=<CSV,CSV,CSV%0ACSV,CSV,CSV>
Query String: http://localhost:9000/aaa/xml2json.json?data=<csv>
  twittername,name,org
  0rb1t3r, Michael Peter Christen, FOSSASIA
  sudheesh001, Sudheesh Singanamalla, FOSSASIA
  
Sample Query: http://localhost:9000/aaa/csv2json.json?data=twittername,name,org%0A0rb1t3r,Michael%20Peter%20Christen,FOSSASIA%0Asudheesh001,Sudheesh%20Singanamalla,FOSSASIA%0A

#### Output

[
  {
    "org": "FOSSASIA",
    "name": "Michael Peter Christen",
    "twittername": "0rb1t3r"
  },
  {
    "org": "FOSSASIA",
    "name": "Sudheesh Singanamalla",
    "twittername": "sudheesh001"
  }
]
  
