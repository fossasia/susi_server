/**
 *  Instagram Profile Scraper
 *  Copyright 08.08.2016 by Jigyasa Grover, @jig08
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
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.BaseUserRole;
import org.loklak.server.Query;
import org.loklak.susi.SusiThought;
import org.loklak.tools.storage.JSONObjectWithDefault;

public class InstagramProfileScraper extends AbstractAPIHandler implements APIHandler{

	private static final long serialVersionUID = -3360416757176406602L;

	@Override
	public String getAPIPath() {
		return "/api/instagramprofilescraper.json";
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
		String profile = call.get("profile", "");
		return scrapeInstagram(profile);
	}

	public static SusiThought scrapeInstagram(String profile) {
		
		Document htmlPage = null;

		try {
			htmlPage = Jsoup.connect("https://www.instagram.com/" + profile).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String script = htmlPage.getElementsByTag("script").get(6).html().substring(21);
		JSONObject obj = new JSONObject(script);
		
		JSONArray instaProfile = new JSONArray();
		instaProfile.put(obj.get("entry_data"));
		
		SusiThought json = new SusiThought();
		json.setData(instaProfile);
		return json;
		
	}

}
