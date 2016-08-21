/**
 *  Quora Profile Scraper
 *  Copyright 16.08.2016 by Jigyasa Grover, @jig08
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

public class QuoraProfileScraper extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -3398701925784347310L;

	@Override
	public String getAPIPath() {
		return "/api/quoraprofilescraper";
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
		return scrapeQuora(profile);
	}

	public static SusiThought  scrapeQuora(String profile) {

		JSONObject quoraProfile = new JSONObject();

		Document userHTML = null;
		String url = "https://www.quora.com/profile/" + profile;

		try {
			userHTML = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		String bio = userHTML.getElementsByClass("qtext_para").text();
		quoraProfile.put("bio", bio);
		
		String profileImage = userHTML.select("img.profile_photo_img").attr("data-src");
		quoraProfile.put("profileImage", profileImage);

		String userName = userHTML.select("img.profile_photo_img").attr("alt");
		quoraProfile.put("user", userName);
		
		String rssFeedLink = url + "/rss";
		quoraProfile.put("rss_feed_link", rssFeedLink);
		
		JSONArray jsonArray = new JSONArray();
		jsonArray.put(quoraProfile);

		SusiThought json = new SusiThought();
		json.setData(jsonArray);
		return json;
	}

}
