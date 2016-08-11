/**
 *  Wiki Articles based on Location
 *  Copyright 09.08.2016 by Jigyasa Grover, @jig08
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

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.susi.SusiThought;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class WikiGeoData extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 4012622482310525730L;

	@Override
	public String getAPIPath() {
		return "/api/wikigeodata.json";
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
		String place = call.get("place", "");
		return wikiGeoData(place);
	}

	public static SusiThought wikiGeoData(String place) {

		URL getCoordURL = null;

		String path = "data={\"places\":[\"" + place + "\"]}";

		try {
			getCoordURL = new URL("http://loklak.org/api/geocode.json?" + path);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		JSONTokener tokener = null;
		try {
			tokener = new JSONTokener(getCoordURL.openStream());
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		JSONObject obj = new JSONObject(tokener);

		String longitude = obj.getJSONObject("locations").getJSONObject(place).getJSONArray("location").get(0)
				.toString();
		String lattitude = obj.getJSONObject("locations").getJSONObject(place).getJSONArray("location").get(1)
				.toString();

		URL getWikiURL = null;

		try {
			getWikiURL = new URL(
					"https://en.wikipedia.org/w/api.php?action=query&list=geosearch&gsradius=10000&gscoord=" + lattitude
							+ "|" + longitude + "&format=json");
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		JSONTokener wikiTokener = null;
		try {
			wikiTokener = new JSONTokener(getWikiURL.openStream());
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		JSONObject wikiGeoResult = new JSONObject(wikiTokener);

		JSONArray jsonArray = new JSONArray();
		jsonArray.put(wikiGeoResult);

		SusiThought json = new SusiThought();
		json.setData(jsonArray);
		return json;
	}

}
