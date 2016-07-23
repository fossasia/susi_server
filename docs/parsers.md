#Parsers & Console Services

* [RSS Feed Reader](#rss-feed-reader)
* [Generic Scraper](#generic-scraper)
* [Event Brite Crawler](#event-brite-crawler)
* [Meetups Scraper](#meetups-scraper)
* [Wordpress Crawler](#wordpress-crawler)

###RSS Feed Reader
RSS Feed Reader helps you read the RSS Feeds from Loklak and also get specific fields when asked from _SUSI_ (example query: Susi, Please read the RSS feed from https://news.ycombinator.com/rss)
<br>
It is implemented by making use of `ROME` Java framework.

* Implementation: [loklak/loklak_server/src/org/loklak/api/search/RSSReaderService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/RSSReaderService.java)

* Data format of `SusiThought` (extension of JSONObject) returned:
```
JSONObject jsonObject = new JSONObject();
jsonObject.put("title", entry.getTitle().toString());
jsonObject.put("link", entry.getLink().toString());
jsonObject.put("uri", entry.getUri().toString());
jsonObject.put("guid", Integer.toString(entry.hashCode()));
if (entry.getPublishedDate() != null) 
    jsonObject.put("pubDate", entry.getPublishedDate().toString());
if (entry.getUpdatedDate() != null) 
    jsonObject.put("updateDate", entry.getUpdatedDate().toString());
if (entry.getDescription() != null) 
    jsonObject.put("description", entry.getDescription().getValue().toString());
```

* Console Service: [loklak/loklak_server/src/org/loklak/api/search/ConsoleService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/ConsoleService.java)<br>
```
dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?rss\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = RSSReaderService.readRSS(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
```

* Sample Link: [http://localhost:9000/api/console.json?q=SELECT * FROM rss WHERE url= 'https://www.reddit.com/search.rss?q=loklak';](http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20rss%20WHERE%20url=%27https://www.reddit.com/search.rss?q=loklak%27;
)
* Sample Output: 
```
{
  "data": [
    {
      "updateDate": "Tue Feb 28 12:19:29 CST 2006",
      "link": "https://www.reddit.com/r/programming/",
      "guid": "-2067538461",
      "title": "programming",
      "uri": "t5_2fwo"
    },
    {
      "updateDate": "Thu Oct 08 10:56:05 CDT 2009",
      "link": "https://www.reddit.com/r/datasets/",
      "guid": "-1256311214",
      "title": "Datasets Archive",
      "uri": "t5_2r97t"
    },
    {
      "updateDate": "Wed Jun 08 00:59:26 CDT 2016",
      "link": "https://www.reddit.com/r/datasets/comments/4n3ljv/we_collected_13_billion_tweets_with_a_distributed/?ref=search_posts",
      "guid": "-762215657",
      "title": "We collected 1.3 billion tweets with a distributed, peer-to-peer based free, open source twitter scraper that has a nice API for your self-made apps to evaluate the data: loklak.org",
      "uri": "t3_4n3ljv"
    },
    {
      "updateDate": "Mon May 04 18:53:19 CDT 2015",
      "link": "https://www.reddit.com/r/programming/comments/34vtya/loklak_web_client_test_a_twitter_search_engine/?ref=search_posts",
      "guid": "1528051145",
      "title": "#Loklak Web client Test A Twitter search engine loklak.org",
      "uri": "t3_34vtya"
    },
    {
      "updateDate": "Fri May 08 23:53:19 CDT 2015",
      "link": "https://www.reddit.com/r/programming/comments/35d94z/a_look_at_loklakorg_and_the_webclient_progress/?ref=search_posts",
      "guid": "-599295987",
      "title": "A look at Loklak.org and the Webclient progress",
      "uri": "t3_35d94z"
    }
  ],
  "metadata": {
    "count": 5
  },
  "session": {
    "identity": {
      "type": "host",
      "name": "10.67.93.57",
      "anonymous": true
    }
  }
}
```
***

###Generic Scraper
This is a very generic web page scraper implemented using `JSoup` library which yields the title of the page, texts, links, images, audio, video, code blocks etc.

* Implementation: [loklak/loklak_server/src/org/loklak/api/search/GenericScraper.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/GenericScraper.java)

* Data format of JSONObject returned:
```
	obj.put("title", title);
	obj.put("language", language);
	obj.put("Links", new JSONArray(linkHref));
	obj.put("Text in Links", new JSONArray(linkText));
	obj.put("source files", new JSONArray(src));
	obj.put("Image files", new JSONArray(image));
	obj.put("Articles", new JSONArray(articleTags));
	obj.put("Preformatted Text", new JSONArray(preTags));
	obj.put("Code", new JSONArray(codeTags));
	obj.put("Audio", new JSONArray(audio));
	obj.put("Video", new JSONArray(video));
```
* Usage: [http://loklak.org/api/genericscraper.json?url=<-link-to-the-HTML-page->](http://loklak.org/api/genericscraper.json?url=https://github.com/loklak)

* Sample Link: [http://loklak.org/api/genericscraper.json?url=https://github.com/loklak](http://loklak.org/api/genericscraper.json?url=https://github.com/loklak)

* Sample Output: 
```
{
  "Text in Links": [
    "Skip to content",
    "Personal",
    "Open source",
    "Business",
    "Explore",
    "Sign up",
    "Sign in",
    "Pricing",
    "Blog",
    "Support",
    "Search GitHub",
    "http://loklak.org",
    "Repositories",
    "Next",
    "12 People",
    "Contact GitHub",
    "API",
    "Training",
    "Shop",
    "Blog",
    "About",
    "Terms",
    "Privacy",
    "Security",
    "Status",
    "Help",
    "Reload"
  ],
  "Image files": [
    "https://avatars1.githubusercontent.com/u/11370631?v=3&s=200",
    "https://assets-cdn.github.com/images/spinners/octocat-spinner-32.gif",
    "https://avatars2.githubusercontent.com/u/5436027?v=3&s=120",
    "https://avatars2.githubusercontent.com/u/1632181?v=3&s=120",
    "https://avatars0.githubusercontent.com/u/6227784?v=3&s=120",
  ],
  "source files": [
    "https://assets-cdn.github.com/assets/compat-7db58f8b7b91111107fac755dd8b178fe7db0f209ced51fc339c446ad3f8da2b.js",
    "https://assets-cdn.github.com/assets/frameworks-404cdd1add1f710db016a02e5e31fff8a9089d14ff0c227df862b780886db7d5.js",
    "https://assets-cdn.github.com/assets/github-8951b8889d701a8bda73fa44510fb2d70dd306d2251977e1172a78d7459cd594.js"
  ],
  "Articles": [
    
  ],
  "Video": [
    
  ],
  "Links": [
    "#start-of-content",
    "https://github.com/",
    "/personal",
    "/open-source",
    "/business",
    "/explore",
    "/join?source=header",
    "/login?return_to=%2Floklak",
    "/pricing",
    "/blog",
    "https://help.github.com",
    "https://github.com/search",
    "http://loklak.org",
    "/loklak",
    "/orgs/loklak/people",
    "/loklak/loklak_server/stargazers",
    "/loklak/loklak_server/network",
    "/loklak/loklak_server",
    "/loklak/asksusi_messenger/stargazers",
    "/loklak/asksusi_messenger/network",
    "/loklak/asksusi_messenger",
    "/loklak/loklak_webclient/stargazers",
    "/loklak/loklak_webclient/network",
    "/loklak/loklak_webclient",
    "/loklak/wp-ai-twitter-feeds/stargazers",
    "/loklak/wp-ai-twitter-feeds/network",
    "/loklak/wp-ai-twitter-feeds",
    "/loklak/wp-rotatingtweets/stargazers",
    "/loklak/wp-rotatingtweets/network",
    "/loklak/wp-rotatingtweets",
    "/loklak/wp-twidget/stargazers",
    "/loklak/wp-twidget/network",
    "/loklak/wp-twidget",
    "/loklak/wp-dev-buddy/stargazers",
    "/loklak/wp-dev-buddy/network",
    "/loklak/wp-dev-buddy",
    "/loklak/wp-juiz-last-tweet-widget/stargazers",
    "/loklak/wp-juiz-last-tweet-widget/network",
    "/loklak/wp-juiz-last-tweet-widget",
    "/loklak/wp-tweeple/stargazers",
    "/loklak/wp-tweeple/network",
    "/loklak/wp-tweeple",
    "/loklak/wp-simple-twitter-feeds/stargazers",
    "/loklak/wp-simple-twitter-feeds/network",
    "/loklak/wp-simple-twitter-feeds",
    "/loklak/wp-tweetscroll-widget/stargazers",
    "/loklak/wp-tweetscroll-widget/network",
    "/loklak/wp-tweetscroll-widget",
    "/loklak/asksusi_com/stargazers",
    "/loklak/asksusi_com/network",
    "/loklak/asksusi_com",
    "/loklak/asksusi_android/stargazers",
    "/loklak/asksusi_android/network",
    "/loklak/asksusi_android",
    "/loklak/loklak_php_api/stargazers",
    "/loklak/loklak_php_api/network",
    "/loklak/loklak_php_api",
    "/loklak/loklak_walls/stargazers",
    "/loklak/loklak_walls/network",
    "/loklak/loklak_walls",
    "/loklak/loklak_python_api/stargazers",
    "/loklak/loklak_python_api/network",
    "/loklak/loklak_python_api",
    "/sudheesh001/loklak_api_python",
    "https://github.com/contact",
    "https://developer.github.com",
    "https://training.github.com",
    "https://shop.github.com",
    "https://github.com/blog",
    "https://github.com/about",
    "https://github.com",
    "https://github.com/site/terms",
    "https://github.com/site/privacy",
    "https://github.com/security",
    "https://status.github.com/",
    "https://help.github.com",
    "https://assets-cdn.github.com/assets/frameworks-e53fc1ddbde2a9e5645df620f65c80ef723c741b33293b6f22a2b7f2c8145fcf.css",
    "https://assets-cdn.github.com/assets/github-8fd3f509ca0995390abdb9f0dfe5b79d1af6ed1ac09c75ba2d68b29b82cadf8d.css",
    "https://assets-cdn.github.com/assets/site-f4b3d32cffc56de06873b8a6d88ae6139de92dc0fc31574232803d68729f6fac.css",
    "https://assets-cdn.github.com/assets/frameworks-404cdd1add1f710db016a02e5e31fff8a9089d14ff0c227df862b780886db7d5.js",
    "https://assets-cdn.github.com/assets/github-8951b8889d701a8bda73fa44510fb2d70dd306d2251977e1172a78d7459cd594.js",
    "/opensearch.xml",
    "https://github.com/fluidicon.png",
    "https://assets-cdn.github.com/",
    "https://assets-cdn.github.com/pinned-octocat.svg",
    "https://assets-cdn.github.com/favicon.ico"
  ],
  "language": "en",
  "title": "loklak Â· GitHub",
  "Preformatted Text": [
    
  ],
  "Audio": [
    
  ],
  "Code": [
    
  ]
}
```


***

###Event Brite Crawler
This crawler parses the event details (name, description, date, time, location, organizer's information, etc.) from [www.eventbrite.com](https://www.eventbrite.com/).
<br> It uses `JSoup`, the Java library for parsing HTML Pages.
<br>The event page on the website is scraped when a call with format `/api/eventbritecrawler.json?url=<event-name-as-on-eventbrite.com>` is made.

* Implementation: [loklak/loklak_server/src/org/loklak/api/search/EventBriteCrawlerService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/EventBriteCrawlerService.java)

* Data Format of `SusiThought` (extension of JSONObject) returned:
```
        JSONArray jsonArray = new JSONArray();

		JSONObject event = new JSONObject();
		event.put("event_url", url);
		event.put("id", eventID);
		event.put("name", eventName);
		event.put("description", eventDescription);
		event.put("color", eventColor);
		event.put("background_url", imageLink);
		event.put("closing_datetime", closingDateTime);
		event.put("creator", creator);
		event.put("email", email);
		event.put("location_name", eventLocation);
		event.put("latitude", latitude);
		event.put("longitude", longitude);
		event.put("start_time", startingTime);
		event.put("end_time", endingTime);
		event.put("logo", imageLink);
		event.put("organizer_description", organizerDescription);
		event.put("organizer_name", organizerName);
		event.put("privacy", privacy);
		event.put("schedule_published_on", schedulePublishedOn);
		event.put("state", state);
		event.put("type", eventType);
		event.put("ticket_url", ticketURL);
		event.put("social_links", socialLinks);
		event.put("topic", topic);
		jsonArray.put(event);

		JSONObject org = new JSONObject();
		org.put("organizer_name", organizerName);
		org.put("organizer_link", organizerLink);
		org.put("organizer_profile_link", organizerProfileLink);
		org.put("organizer_website", organizerWebsite);
		org.put("organizer_contact_info", organizerContactInfo);
		org.put("organizer_description", organizerDescription);
		org.put("organizer_facebook_feed_link", organizerFacebookFeedLink);
		org.put("organizer_twitter_feed_link", organizerTwitterFeedLink);
		org.put("organizer_facebook_account_link", organizerFacebookAccountLink);
		org.put("organizer_twitter_account_link", organizerTwitterAccountLink);
		jsonArray.put(org);
		
		SusiThought json = new SusiThought();
		json.setData(jsonArray);
```

* Console Service: [loklak/loklak_server/src/org/loklak/api/search/ConsoleService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/ConsoleService.java)
```
 dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?eventbrite\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = EventBriteCrawlerService.crawlEventBrite(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
```
* Usage: [http://localhost:9000/api/console.json?q=SELECT * FROM eventbrite WHERE url='<-link-to-event-listing-on-eventbrite.com->';
](http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20eventbrite%20WHERE%20url=%27https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153?aff=es2%27;
)

* Sample Link: [http://loklak.org/api/eventbritecrawler.json?url=https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153](http://loklak.org/api/eventbritecrawler.json?url=https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153)

* Sample Output: 
```
{
  "data": [
    {
      "creator": {
        "id": "1",
        "email": ""
      },
      "background_url": "https://img.evbuc.com/https%3A%2F%2Fcdn.evbuc.com%2Fimages%2F21620618%2F176404209748%2F1%2Foriginal.jpg?w=1000&rect=0%2C0%2C1600%2C800&s=0596c1ac359823293d255b7b41639e6b",
      "social_links": [
        {
          "name": "Facebook",
          "link": "https://www.facebook.com/europeadenamur",
          "id": "1"
        },
        {
          "name": "Twitter",
          "link": "",
          "id": "2"
        }
      ],
      "latitude": 50.46685,
      "end_time": "2016-07-22T21:00:00",
      "description": "Concert de chorales le vendredi 22 juillet 2016   GRATUIT  -  FREE ADMISSION  -  VRIJE TOEGANG  -  EINTRITT FREI La réservation permet un accès prioritaire au spectacle.  The reservation provides priority access to the show. De boeking geeft prioritaire toegang tot de show. Die Reservierung gibt einen bevorzugten Zugang zum Schauspiel. www.europeade2016.be ",
      "privacy": "public",
      "type": "",
      "ticket_url": "https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153#tickets",
      "event_url": "https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153",
      "start_time": "2016-07-22T19:00:00",
      "location_name": "2 Avenue Sergent Vrithoff, 5000 Namur, Belgium",
      "name": "EUROPEADE 2016 - Concert de musique vocale",
      "logo": "https://img.evbuc.com/https%3A%2F%2Fcdn.evbuc.com%2Fimages%2F21620618%2F176404209748%2F1%2Foriginal.jpg?w=1000&rect=0%2C0%2C1600%2C800&s=0596c1ac359823293d255b7b41639e6b",
      "organizer_description": "None",
      "topic": "",
      "id": "25592599153",
      "organizer_name": ": VILLE DE NAMUR",
      "state": "completed",
      "longitude": 4.8482485
    },
    {
      "organizer_contact_info": "https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153#lightbox_contact",
      "organizer_link": "https://www.eventbrite.fr/e/billets-europeade-2016-concert-de-musique-vocale-25592599153#listing-organizer",
      "organizer_profile_link": "http://www.eventbrite.fr/o/ville-de-namur-10801060853",
      "organizer_facebook_feed_link": "http://www.eventbrite.fr/o/ville-de-namur-10801060853#facebook_feed",
      "organizer_description": "None",
      "organizer_twitter_account_link": "",
      "organizer_name": ": VILLE DE NAMUR",
      "organizer_website": "http://www.europeade2016.be",
      "organizer_facebook_account_link": "https://www.facebook.com/europeadenamur",
      "organizer_twitter_feed_link": "http://www.eventbrite.fr/o/ville-de-namur-10801060853#twitter_feed"
    }
  ],
  "metadata": {
    "count": 9
  },
  "session": {
    "identity": {
      "type": "host",
      "name": "10.67.93.57",
      "anonymous": true
    }
  }
}
```

***

###Meetups Scraper
This scraper is intended to provide with all the information like name, agenda, description, location, recent meetup, reviews atc.
<br> It uses `JSoup`, the Java library for parsing HTML Pages.
<br>The group page on [meetup.com](https://www.meetup.com/) is scraped when a call with format `/api/meetupscrawler.json?url=<link-to-group-page>` is made.

* Implementation: [loklak/loklak_server/src/org/loklak/api/search/MeetupsCrawlerService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/MeetupsCrawlerService.java)

* Data Format of `SusiThought` (extension of JSONObject) returned:
```
	JSONObject result = new JSONObject();
	result.put("group_name", meetupGroupName);
	result.put("meetup_type", meetupType);
	result.put("group_description", groupDescription);
	result.put("group_locality", groupLocality);
	result.put("group_country_code", groupCountry);
	result.put("group_latitude", latitude);
	result.put("group_longitude", longitude);
	result.put("group_imageLink", imageLink);
	result.put("group_topics", groupTopics);
	result.put("recent_meetups", recentMeetups);
	
	JSONArray meetupsCrawlerResultArray = new JSONArray();
	meetupsCrawlerResultArray.put(result);
	
	SusiThought json = new SusiThought();
	json.setData(meetupsCrawlerResultArray);
```

* Console Service: [loklak/loklak_server/src/org/loklak/api/search/ConsoleService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/ConsoleService.java)
```
	dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?meetup\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
            SusiThought json = MeetupsCrawlerService.crawlMeetups(matcher.group(2));
            SusiTransfer transfer = new SusiTransfer(matcher.group(1));
            json.setData(transfer.conclude(json.getData()));
            return json;
        });
```

* Sample Link: [http://loklak.org/api/console.json?q=SELECT * FROM meetup WHERE url='http://www.meetup.com/Women-Who-Code-Delhi';](http://loklak.org/api/console.json?q=SELECT%20*%20FROM%20meetup%20WHERE%20url=%27http://www.meetup.com/Women-Who-Code-Delhi%27;)

* Sample Output: 
```
{
  "data": [{
    "group_topics": [
      "Django",
      "Web Design",
      "Ruby",
      "HTML5",
      "Women Programmers",
      "JavaScript",
      "Python",
      "Women in Technology",
      "Android Development",
      "Mobile Technology",
      "iOS Development",
      "Women Who Code",
      "Ruby On Rails",
      "Computer programming",
      "WWC"
    ],
    "group_longitude": "77.21",
    "recent_meetups": [
      {
        "date_time": "April 2 · 10:30 AM",
        "recent_meetup_number": 1,
        "information": "Brought to you in collaboration with Women Techmakers Delhi.According to a survey, only 11% of open source participants are women. People find it intimidating to get... Learn more",
        "attendance": "13 Women Who Code-rs | 5.001"
      },
      {
        "date_time": "March 3 · 3:00 PM",
        "recent_meetup_number": 2,
        "information": "\u201cBehold, the number five is at hand. Grab it and shake and harness the power of networking.\u201d Women Who Code Delhi is proud to present Social Hack Eve, a networking... Learn more",
        "attendance": "21 Women Who Code-rs | 5.001"
      },
      {
        "date_time": "Oct 18, 2015 · 9:00 AM",
        "recent_meetup_number": 3,
        "information": "Hello Ladies :) Google Women Techmakers is looking for women techies to present a talk in one of the segments of Google DevFest Delhi 2015 planned for October 18, 2015... Learn more",
        "attendance": "20 Women Who Code-rs | 4.502"
      },
      {
        "date_time": "Jul 5, 2015 · 12:00 PM",
        "recent_meetup_number": 4,
        "information": "Agenda: Learning how to use and develop open source software, and contribute to huge existing open source projects.A series of talks by some of this year\u2019s GSoC... Learn more",
        "attendance": "24 Women Who Code-rs | 4.001 | 1 Photo"
      }
    ],
    "group_name": "Women Who Code Delhi",
    "group_locality": "Delhi",
    "group_country_code": "in",
    "group_imageLink": "http://photos1.meetupstatic.com/photos/event/b/7/7/3/highres_431686963.jpeg",
    "meetup_type": "activity",
    "group_description": "Mission: Women Who Code is a global nonprofit organization dedicated to inspiring women to excel in technology careers by creating a global, connected community of women in technology. The organization tripled in 2013 and has grown to be one of the largest communities of women engineers in the world. Empowerment: Women Who code is a professional community for women in tech. We provide an avenue for women to pursue a career in technology, help them gain new skills and hone existing skills for professional advancement, and foster environments where networking and mentorship are valued. Key Initiatives: - Free technical study groups - Events featuring influential tech industry experts and investors - Hack events - Career and leadership development Current and aspiring coders are welcome.  Bring your laptop and a friend!  Support Women Who Code: Donating to Women Who Code, Inc. (#46-4218859) directly impacts our ability to efficiently run this growing organization, helps us produce new programs that will increase our reach, and enables us to expand into new cities around the world ensuring that women and girls everywhere have the opportunity to pursue a career in technology. Women Who Code (WWCode) is dedicated to providing an empowering experience for everyone who participates in or supports our community, regardless of gender, gender identity and expression, sexual orientation, ability, physical appearance, body size, race, ethnicity, age, religion, or socioeconomic status. Because we value the safety and security of our members and strive to have an inclusive community, we do not tolerate harassment of members or event participants in any form. Our Code of Conduct applies to all events run by Women Who Code, Inc. If you would like to report an incident or contact our leadership team, please submit an incident report form. WomenWhoCode.com",
    "group_latitude": "28.67"
  }],
  "metadata": {"count": 1},
  "session": {"identity": {
    "type": "host",
    "name": "10.67.93.57",
    "anonymous": true
  }}
}
```

***

###Wordpress Crawler
* Implementation: [loklak/loklak_server/src/org/loklak/api/search/WordpressCrawlerService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/WordpressCrawlerService.java)

* Data Format of `SusiThought` (extension of JSONObject) returned:
```
	JSONArray blog = new JSONArray();
	
	for (int k = 0; k < numberOfBlogs; k++) {
		JSONObject blogpost = new JSONObject();
		blogpost.put("blog_url", blogURL);
		blogpost.put("title", blogPosts[k][0]);
		blogpost.put("posted_on", blogPosts[k][1]);
		blogpost.put("author", blogPosts[k][2]);
		blogpost.put("content", blogPosts[k][3]);
		blog.put(blogpost);
	}
	
	SusiThought json = new SusiThought();
	json.setData(blog);
```

* Console Service: [loklak/loklak_server/src/org/loklak/api/search/ConsoleService.java](https://github.com/loklak/loklak_server/blob/development/src/org/loklak/api/search/ConsoleService.java)
```
	dbAccess.put(Pattern.compile("SELECT\\h+?(.*?)\\h+?FROM\\h+?wordpress\\h+?WHERE\\h+?url\\h??=\\h??'(.*?)'\\h??;"), (flow, matcher) -> {
		SusiThought json = WordpressCrawlerService.crawlWordpress(matcher.group(2));
		SusiTransfer transfer = new SusiTransfer(matcher.group(1));
		json.setData(transfer.conclude(json.getData()));
		return json;
	});
```

* Sample Link: [http://loklak.org/api/wordpresscrawler.json?url=https://jigyasagrover.wordpress.com/](http://loklak.org/api/wordpresscrawler.json?url=https://jigyasagrover.wordpress.com/)

* Sample Output: 
```
{
  "data": [{
    "blog_url": "https://jigyasagrover.wordpress.com/",
    "title": "A niche for my kaleidoscopic explorations :)",
    "content": "Check out this space for posts spanning colossal themes ! #pharo #smalltalk #morphic #spec #java #android #c #c++ #git #php #asp.net #python Catch up with updates on Google Summer of Code project apprises, Google Code-In mentoring experiences, Conference reports, Google Developers Group events, Women Who Code MeetUps , Google Women Techmakers Get Togethers , contributions to organizations like FOSSASIA and Square Bracket Associates, sundry Startup experiences, Enactus undertakings, Social Welfare escapades and many more \u2026. Keep the feedback pouring in \u2026 Follow/like/comment for more. Post any query, Would be happy to help😀 Go to: RECENT POSTS   Share this: Twitter Facebook Google Like this: Like Loading..."
  }],
  "metadata": {"count": 1},
  "session": {"identity": {
    "type": "host",
    "name": "10.67.93.57",
    "anonymous": true
  }}
}
```
* Usage: [http://localhost:9000/api/console.json?q=SELECT * FROM wordpress WHERE url='<-link-to-wordpress-blogspot->';](http://localhost:9000/api/console.json?q=SELECT%20*%20FROM%20wordpress%20WHERE%20url=%27https://jigyasagrover.wordpress.com/%27;)



