/**
 *  TwitterAnalysis
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
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;
import org.loklak.tools.UTF8;

public class TwitterAnalysis extends HttpServlet {

	private static final long serialVersionUID = -3753965521858525803L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Query post = RemoteAccess.evaluate(request);

		// manage DoS
		if (post.isDoS_blackout()) {
			response.sendError(503, "your request frequency is too high");
			return;
		}

		JSONObject finalresult = new JSONObject();
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		String username = post.get("screen_name", "");
		String count = post.get("count", "");
		String siteurl = request.getRequestURL().toString();
		String baseurl = siteurl.substring(0, siteurl.length() - request.getRequestURI().length())
				+ request.getContextPath();

		String searchurl = baseurl + "/api/search.json?q=from%3A" + username + (count != "" ? ("&count=" + count) : "");
		// String userurl = baseurl + "/api/user.json?screen_name=" + username;
		byte[] searchbyte = ClientConnection.download(searchurl);
		// byte[] userbyte = ClientConnection.download(userurl);
		String searchstr = UTF8.String(searchbyte);
		// String userstr = UTF8.String(userbyte);
		JSONObject searchresult = new JSONObject(searchstr);
		// JSONObject userresult = new JSONObject(userstr);

		JSONArray tweets = searchresult.getJSONArray("statuses");
		if (tweets.length() == 0) {
			// TODO: add similar check for userbyte in the if statement
			finalresult.put("data collected", "empty");
			finalresult.put("status", "invalid username or no tweets");
			finalresult.put("username", username);
			sos.print(finalresult.toString(2));
			sos.println();
			post.finalize();
			return;
		}

		// activity frequency statistics
		List<String> tweetDate = new ArrayList<>();
		List<String> tweetHour = new ArrayList<>();
		List<String> tweetDay = new ArrayList<>();
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
		}

		JSONObject yearlyact = new JSONObject();
		JSONObject hourlyact = new JSONObject();
		JSONObject dailyact = new JSONObject();

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

		finalresult.put("yearwise_activity_frequency", yearlyact);
		finalresult.put("hourwise_activity_frequency", hourlyact);
		finalresult.put("daywise_activity_frequency", dailyact);
		finalresult.put("username", username);
		finalresult.put("tweets_analysed", searchresult.getJSONObject("search_metadata").get("count"));
		sos.print(finalresult.toString(2));
		sos.println();
		post.finalize();
		return;
	}

}
