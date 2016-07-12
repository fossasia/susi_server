/**
 *  Meetups.com Crawler
 *  Copyright 12.07.2016 by Jigyasa Grover, @jig08
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.api.search;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.susi.SusiThought;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class MeetupsCrawlerService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -8463958440848132447L;

	@Override
	public String getAPIPath() {
		return "/api/meetupscrawler.json";
	}

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		return null;
	}

	@Override
	public JSONObject serviceImpl(Query call, Authorization rights, JSONObjectWithDefault permissions)
			throws APIException {
		String url = call.get("url", "");
		return crawlMeetups(url);
	}

	public static SusiThought crawlMeetups(String url) {
		
		Document meetupHTML = null;
		String meetupGroupName = null;
		String meetupType = null;
		String groupDescription = null;
		String groupLocality = null;
		String groupCountry = null;
		String latitude = null;
		String longitude = null;
		String imageLink = null;
		Elements topicList = null;
		String[] topicListArray = new String[100];
		Integer numberOfTopics = 0;
		Elements recentMeetupsSection = null;
		Integer numberOfRecentMeetupsShown = 0;
		Integer i = 0, j = 0;
		String recentMeetupsResult[][] = new String[100][3];
		// recentMeetupsResult[i][0] == date && time
		// recentMeetupsResult[i][1] == Attendance && Review
		// recentMeetupsResult[i][2] == Information

		JSONObject result = new JSONObject();

		try {
			meetupHTML = Jsoup.connect(url).userAgent("Mozilla)").get();

		} catch (Exception e) {
			e.printStackTrace();
		}

		meetupGroupName = meetupHTML.getElementsByAttributeValue("property", "og:title").attr("content");
		result.put("group_name", meetupGroupName);

		meetupType = meetupHTML.getElementsByAttributeValue("property", "og:type").attr("content");
		result.put("meetup_type", meetupType);

		groupDescription = meetupHTML.getElementById("groupDesc").text();
		result.put("group_description", groupDescription);

		groupLocality = meetupHTML.getElementsByAttributeValue("property", "og:locality").attr("content");
		result.put("group_locality", groupLocality);

		groupCountry = meetupHTML.getElementsByAttributeValue("property", "og:country-name").attr("content");
		result.put("group_country_code", groupCountry);

		latitude = meetupHTML.getElementsByAttributeValue("property", "og:latitude").attr("content");
		result.put("group_latitude", latitude);

		longitude = meetupHTML.getElementsByAttributeValue("property", "og:longitude").attr("content");
		result.put("group_longitude", longitude);

		imageLink = meetupHTML.getElementsByAttributeValue("property", "og:image").attr("content");
		result.put("group_imageLink", imageLink);

		topicList = meetupHTML.getElementById("topic-box-2012").getElementsByTag("a");

		int p = 0;
		for (Element topicListStringsIterator : topicList) {
			topicListArray[p] = topicListStringsIterator.text().toString();
			p++;
		}
		numberOfTopics = p;

		JSONArray groupTopics = new JSONArray();
		for (int l = 0; l < numberOfTopics; l++) {
			groupTopics.put(l, topicListArray[l]);
		}
		result.put("group_topics", groupTopics);

		recentMeetupsSection = meetupHTML.getElementById("recentMeetups").getElementsByTag("p");

		i = 0;
		j = 0;

		for (Element recentMeetups : recentMeetupsSection) {
			if (j % 3 == 0) {
				j = 0;
				i++;
			}

			recentMeetupsResult[i][j] = recentMeetups.text().toString();
			j++;

		}

		numberOfRecentMeetupsShown = i;

		JSONArray recentMeetups = new JSONArray();
		for (int k = 1; k < numberOfRecentMeetupsShown; k++) {
			JSONObject obj = new JSONObject();
			obj.put("recent_meetup_number", k);
			obj.put("date_time", recentMeetupsResult[k][0]);
			obj.put("attendance", recentMeetupsResult[k][1]);
			obj.put("information", recentMeetupsResult[k][2]);
			recentMeetups.put(obj);
		}

		result.put("recent_meetups", recentMeetups);

		JSONArray meetupsCrawlerResultArray = new JSONArray();
		meetupsCrawlerResultArray.put(result);

		SusiThought json = new SusiThought();
		json.setData(meetupsCrawlerResultArray);
		return json;
	}

}
