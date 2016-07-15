/**
 *  TwitterAnalysisService
 *  Copyright 04.07.2016 by Shiven Mian, @shivenmian
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

package org.loklak.api.cms;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.http.ClientConnection;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.susi.SusiThought;
import org.loklak.tools.UTF8;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class TwitterAnalysisService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -3753965521858525803L;

	@Override
	public String getAPIPath() {
		return "/api/twitanalysis.json";
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
		String username = call.get("screen_name", "");
		String count = call.get("count", "");
		return showAnalysis(username, count, call.request);
	}

	public static SusiThought showAnalysis(String username, String count, HttpServletRequest request) {

		SusiThought json = new SusiThought();
		JSONArray finalresultarray = new JSONArray();
		JSONObject finalresult = new JSONObject(true);
		String siteurl = request.getRequestURL().toString();
		String baseurl = siteurl.substring(0, siteurl.length() - request.getRequestURI().length())
				+ request.getContextPath();

		String searchurl = baseurl + "/api/search.json?q=from%3A" + username + (count != "" ? ("&count=" + count) : "");
		byte[] searchbyte;
		try {
			searchbyte = ClientConnection.download(searchurl);
		} catch (IOException e) {
			return json.setData(new JSONArray().put(new JSONObject().put("Error", "Can't contact server")));
		}
		String searchstr = UTF8.String(searchbyte);
		JSONObject searchresult = new JSONObject(searchstr);

		JSONArray tweets = searchresult.getJSONArray("statuses");
		if (tweets.length() == 0) {
			finalresult.put("error", "Invalid username " + username + " or no tweets");
			finalresultarray.put(finalresult);
			json.setData(finalresultarray);
			return json;
		}
		finalresult.put("username", username);
		finalresult.put("items_per_page", searchresult.getJSONObject("search_metadata").getString("itemsPerPage"));
		finalresult.put("tweets_analysed", searchresult.getJSONObject("search_metadata").getString("count"));

		// main loop
		JSONObject activityFreq = new JSONObject(true);
		JSONObject activityType = new JSONObject(true);
		int imgCount = 0, audioCount = 0, videoCount = 0, linksCount = 0, likesCount = 0, retweetCount = 0,
				hashtagCount = 0;
		int maxLikes = 0, maxRetweets = 0, maxHashtags = 0;
		String maxLikeslink, maxRetweetslink, maxHashtagslink;
		maxLikeslink = maxRetweetslink = maxHashtagslink = tweets.getJSONObject(0).getString("link");
		List<String> tweetDate = new ArrayList<>();
		List<String> tweetHour = new ArrayList<>();
		List<String> tweetDay = new ArrayList<>();
		List<Integer> likesList = new ArrayList<>();
		List<Integer> retweetsList = new ArrayList<>();
		List<Integer> hashtagsList = new ArrayList<>();
		List<String> languageList = new ArrayList<>();
		List<String> sentimentList = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();

		for (int i = 0; i < tweets.length(); i++) {
			JSONObject status = tweets.getJSONObject(i);
			String[] datearr = status.getString("created_at").split("T")[0].split("-");
			calendar.set(Integer.parseInt(datearr[0]), Integer.parseInt(datearr[1]) - 1, Integer.parseInt(datearr[2]));
			Date date = new Date(calendar.getTimeInMillis());
			tweetDate.add(new SimpleDateFormat("MMM yyyy").format(date));
			tweetDay.add(new SimpleDateFormat("EEEE", Locale.ENGLISH).format(date)); // day
			String times = status.getString("created_at").split("T")[1];
			String hour = times.substring(0, times.length() - 5).split(":")[0];
			tweetHour.add(hour); // hour
			imgCount += status.getInt("images_count");
			audioCount += status.getInt("audio_count");
			videoCount += status.getInt("videos_count");
			linksCount += status.getInt("links_count");
			likesList.add(status.getInt("favourites_count"));
			retweetsList.add(status.getInt("retweet_count"));
			hashtagsList.add(status.getInt("hashtags_count"));
			if (status.has("classifier_emotion")) {
				sentimentList.add(status.getString("classifier_emotion"));
			} else {
				sentimentList.add("neutral");
			}
			if (status.has("classifier_language")) {
				languageList.add(status.getString("classifier_language"));
			} else {
				languageList.add("no_text");
			}
			if (maxLikes < status.getInt("favourites_count")) {
				maxLikes = status.getInt("favourites_count");
				maxLikeslink = status.getString("link");
			}
			if (maxRetweets < status.getInt("retweet_count")) {
				maxRetweets = status.getInt("retweet_count");
				maxRetweetslink = status.getString("link");
			}
			if (maxHashtags < status.getInt("hashtags_count")) {
				maxHashtags = status.getInt("hashtags_count");
				maxHashtagslink = status.getString("link");
			}
			likesCount += status.getInt("favourites_count");
			retweetCount += status.getInt("retweet_count");
			hashtagCount += status.getInt("hashtags_count");
		}
		activityType.put("posted_image", imgCount);
		activityType.put("posted_audio", audioCount);
		activityType.put("posted_video", videoCount);
		activityType.put("posted_link", linksCount);
		activityType.put("posted_story",
				Integer.parseInt(searchresult.getJSONObject("search_metadata").getString("count"))
						- (imgCount + audioCount + videoCount + linksCount));

		JSONObject yearlyact = new JSONObject(true);
		JSONObject hourlyact = new JSONObject(true);
		JSONObject dailyact = new JSONObject(true);
		Set<String> yearset = new HashSet<String>(tweetDate);
		Set<String> hourset = new HashSet<String>(tweetHour);
		Set<String> dayset = new HashSet<String>(tweetDay);

		for (String s : yearset) {
			yearlyact.put(s, Collections.frequency(tweetDate, s));
		}

		for (String s : hourset) {
			hourlyact.put(s, Collections.frequency(tweetHour, s));
		}

		for (String s : dayset) {
			dailyact.put(s, Collections.frequency(tweetDay, s));
		}

		activityFreq.put("yearwise", yearlyact);
		activityFreq.put("hourwise", hourlyact);
		activityFreq.put("daywise", dailyact);
		finalresult.put("tweet_frequency", activityFreq);
		finalresult.put("tweet_type", activityType);

		// activity on my tweets

		JSONObject activityOnTweets = new JSONObject(true);
		JSONObject activityCharts = new JSONObject(true);
		JSONObject likesChart = new JSONObject(true);
		JSONObject retweetChart = new JSONObject(true);
		JSONObject hashtagsChart = new JSONObject(true);

		Set<Integer> likesSet = new HashSet<Integer>(likesList);
		Set<Integer> retweetSet = new HashSet<Integer>(retweetsList);
		Set<Integer> hashtagSet = new HashSet<Integer>(hashtagsList);

		for (Integer i : likesSet) {
			likesChart.put(i.toString(), Collections.frequency(likesList, i));
		}

		for (Integer i : retweetSet) {
			retweetChart.put(i.toString(), Collections.frequency(retweetsList, i));
		}

		for (Integer i : hashtagSet) {
			hashtagsChart.put(i.toString(), Collections.frequency(hashtagsList, i));
		}

		activityOnTweets.put("likes_count", likesCount);
		activityOnTweets.put("max_likes",
				new JSONObject(true).put("number", maxLikes).put("link_to_tweet", maxLikeslink));
		activityOnTweets.put("average_number_of_likes",
				(likesCount / (Integer.parseInt(searchresult.getJSONObject("search_metadata").getString("count")))));

		activityOnTweets.put("retweets_count", retweetCount);
		activityOnTweets.put("max_retweets",
				new JSONObject(true).put("number", maxRetweets).put("link_to_tweet", maxRetweetslink));
		activityOnTweets.put("average_number_of_retweets",
				(retweetCount / (Integer.parseInt(searchresult.getJSONObject("search_metadata").getString("count")))));

		activityOnTweets.put("hashtags_used_count", hashtagCount);
		activityOnTweets.put("max_hashtags",
				new JSONObject(true).put("number", maxHashtags).put("link_to_tweet", maxHashtagslink));
		activityOnTweets.put("average_number_of_hashtags_used",
				(hashtagCount / (Integer.parseInt(searchresult.getJSONObject("search_metadata").getString("count")))));

		activityCharts.put("likes", likesChart);
		activityCharts.put("retweets", retweetChart);
		activityCharts.put("hashtags", hashtagsChart);
		activityOnTweets.put("frequency_charts", activityCharts);
		finalresult.put("activity_on_my_tweets", activityOnTweets);

		// content analysis
		JSONObject contentAnalysis = new JSONObject(true);
		JSONObject languageAnalysis = new JSONObject(true);
		JSONObject sentimentAnalysis = new JSONObject(true);
		Set<String> languageSet = new HashSet<String>(languageList), sentimentSet = new HashSet<String>(sentimentList);

		for (String s : languageSet) {
			languageAnalysis.put(s, Collections.frequency(languageList, s));
		}

		for (String s : sentimentSet) {
			sentimentAnalysis.put(s, Collections.frequency(sentimentList, s));
		}
		contentAnalysis.put("language_analysis", languageAnalysis);
		contentAnalysis.put("sentiment_analysis", sentimentAnalysis);
		finalresult.put("content_analysis", contentAnalysis);

		finalresultarray.put(finalresult);
		json.setData(finalresultarray);
		return json;
	}
}
