/**
 *  RSS Reader
 *  Copyright 23.06.2016 by Jigyasa Grover, @jig08
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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RSSReader extends HttpServlet {

	private static final long serialVersionUID = 1463185662941444503L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Query post = RemoteAccess.evaluate(request);

		// manage DoS
		if (post.isDoS_blackout()) {
			response.sendError(503, "your request frequency is too high");
			return;
		}

		String url = post.get("url", "");

		URL feedUrl = null;
		try {
			feedUrl = new URL(url);
		} catch (Exception e) {
			e.printStackTrace();
		}

		SyndFeedInput input = new SyndFeedInput();

		SyndFeed feed = null;
		try {
			feed = input.build(new XmlReader(feedUrl));
		} catch (Exception e) {
			e.printStackTrace();
		}

		@SuppressWarnings("unused")
		int totalEntries = 0;
		int i = 0;

		JSONArray jsonArray = new JSONArray();

		// Reading RSS Feed from the URL
		for (SyndEntry entry : (List<SyndEntry>) feed.getEntries()) {

			JSONObject jsonObject = new JSONObject();

			jsonObject.put("RSS Feed", url);
			jsonObject.put("Title", entry.getTitle().toString());
			jsonObject.put("Link", entry.getLink().toString());
			jsonObject.put("URI", entry.getUri().toString());
			jsonObject.put("Hash-Code", Integer.toString(entry.hashCode()));
			jsonObject.put("Published-Date", entry.getPublishedDate().toString());
			jsonObject.put("Updated-Date",
					(entry.getUpdatedDate() == null) ? ("null") : (entry.getUpdatedDate().toString()));
			jsonObject.put("Description", entry.getDescription().toString());

			jsonArray.put(i, jsonObject);

			i++;
		}

		totalEntries = i;

		JSONObject rssFeed = new JSONObject();
		rssFeed.put("RSS Feed", jsonArray);

		// print JSON
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(rssFeed.toString(2));
		sos.println();
	}
}
