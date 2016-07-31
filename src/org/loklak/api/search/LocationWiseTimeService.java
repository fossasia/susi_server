/**
 *  Location Wise Time
 *  timeanddate.com scraper
 *  Copyright 27.07.2016 by Jigyasa Grover, @jig08
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

import javax.servlet.http.HttpServletResponse;

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

public class LocationWiseTimeService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -1495493690406247295L;

	@Override
	public String getAPIPath() {
		return "/api/locationwisetime.json";
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
	public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights,
			JSONObjectWithDefault permissions) throws APIException {
		String query = call.get("query", "");
		return locationWiseTime(query);
	}

	public static SusiThought locationWiseTime(String query) {
		
		Document html = null;

		JSONArray arr = new JSONArray();

		try {
			html = Jsoup.connect("http://www.timeanddate.com/worldclock/results.html?query=" + query).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Elements locations = html.select("td");
		int i = 0;
		for (Element e : locations) {
			if (i % 2 == 0) {
				JSONObject obj = new JSONObject();
				String l = e.getElementsByTag("a").text();
				obj.put("location", l);
				String t = e.nextElementSibling().text();
				obj.put("time", t);
				arr.put(obj);
			}
			i++;
		}
		
		SusiThought json = new SusiThought();
		json.setData(arr);
		return json;
	}

}
