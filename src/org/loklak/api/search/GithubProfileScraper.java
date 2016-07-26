/**
 *  Github Profile Crawler
 *  Copyright 22.07.2016 by Jigyasa Grover, @jig08
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
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
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

public abstract class GithubProfileScraper extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -4166800345379685201L;

	@Override
	public String getAPIPath() {
		return "/api/githubprofilescraper.json";
	}

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		// TODO Auto-generated method stub
		return null;
	}

	public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights, JSONObjectWithDefault permissions)
			throws APIException {
		String profile = call.get("profile", "");
		return scrapeGithub(profile);
	}

	public static SusiThought scrapeGithub(String profile) {

		Document html = null;

		JSONObject githubProfile = new JSONObject();

		try {
			html = Jsoup.connect("https://github.com/" + profile).get();
		} catch (IOException e) {
			
			URI uri = null;
			try {
				uri = new URI("https://api.github.com/search/users?q=" + profile);
			} catch (URISyntaxException e1) {
				e1.printStackTrace();
			}
			
			JSONTokener tokener = null;
			try {
				tokener = new JSONTokener(uri.toURL().openStream());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			
			JSONObject obj = new JSONObject(tokener);
			
			JSONArray arr = new JSONArray();
			arr.put(obj);
			
			SusiThought json = new SusiThought();
			json.setData(arr);
			return json;
		}

		String avatarUrl = html.getElementsByAttributeValue("class", "avatar rounded-2").attr("src");
		githubProfile.put("avatar_url", avatarUrl);		
		
		String fullName = html.getElementsByAttributeValue("class", "vcard-fullname").text();
		githubProfile.put("full_name", fullName);		
		
		String userName = html.getElementsByAttributeValue("class", "vcard-username").text();
		githubProfile.put("user_name", userName);
		
		String bio = html.getElementsByAttributeValue("class", "user-profile-bio").text();
		githubProfile.put("bio", bio);

		String atomFeedLink = html.getElementsByAttributeValue("type", "application/atom+xml").attr("href");
		githubProfile.put("atom_feed_link", "https://github.com" + atomFeedLink);

		String worksFor = html.getElementsByAttributeValue("itemprop", "worksFor").text();
		githubProfile.put("works_for", worksFor);

		String homeLocation = html.getElementsByAttributeValue("itemprop", "homeLocation").attr("title");
		githubProfile.put("home_location", homeLocation);

		String email = html.getElementsByAttributeValue("itemprop", "email").text();
		githubProfile.put("email", email);

		String specialLink = html.getElementsByAttributeValue("itemprop", "url").text();
		githubProfile.put("special_link", specialLink);

		String joiningDate = html.getElementsByAttributeValue("class", "join-date").attr("datetime");
		githubProfile.put("joining_date", joiningDate);

		/* If Individual User */
		if (html.getElementsByAttributeValue("class", "vcard-stat").size() != 0) {
			
			String followersUrl = html.getElementsByAttributeValue("class", "vcard-stat").get(0).attr("href");
			githubProfile.put("followers_url", "https://github.com" + followersUrl);

			String followers = html.getElementsByAttributeValue("class", "vcard-stat").get(0).tagName("strong").text();
			githubProfile.put("followers", followers);

			String starredUrl = html.getElementsByAttributeValue("class", "vcard-stat").get(1).attr("href");
			githubProfile.put("starred_url", "https://github.com" + starredUrl);

			String starred = html.getElementsByAttributeValue("class", "vcard-stat").get(1).tagName("strong").text();
			githubProfile.put("starred", starred);

			String followingUrl = html.getElementsByAttributeValue("class", "vcard-stat").get(2).attr("href");
			githubProfile.put("following_url", "https://github.com" + followingUrl);

			String following = html.getElementsByAttributeValue("class", "vcard-stat").get(2).tagName("strong").text();
			githubProfile.put("following", following);
		}
		
		String gistsUrl ="https://api.github.com/users/" + profile + "/gists";
		githubProfile.put("gists_url", gistsUrl);
		
		String subscriptionsUrl ="https://api.github.com/users/" + profile + "/subscriptions";
		githubProfile.put("subscriptions_url", subscriptionsUrl);
		
		String reposUrl ="https://api.github.com/users/" + profile + "/repos";
		githubProfile.put("repos_url", reposUrl);
		
		String eventsUrl ="https://api.github.com/users/" + profile + "/events";
		githubProfile.put("events_url", eventsUrl);
		
		String receivedEventsUrl ="https://api.github.com/users/" + profile + "/received_events";
		githubProfile.put("received_events_url", receivedEventsUrl);
		

		JSONArray organizations = new JSONArray();
		Elements orgs = html.getElementsByAttributeValue("itemprop", "follows");
		for (Element e : orgs) {
			JSONObject obj = new JSONObject();

			String label = e.attr("aria-label");
			obj.put("label", label);

			String link = e.attr("href");
			obj.put("link", "https://github.com" + link);

			String imgLink = e.children().attr("src");
			obj.put("img_link", imgLink);

			String imgAlt = e.children().attr("alt");
			obj.put("img_Alt", imgAlt);

			organizations.put(obj);
		}
		githubProfile.put("organizations", organizations);

		/* If Organization */
		Elements navigation = html.getElementsByAttributeValue("class", "orgnav");
		for (Element e : navigation) {
			String orgRepositoriesLink = e.child(0).tagName("a").attr("href");
			githubProfile.put("organization_respositories_link", "https://github.com" + orgRepositoriesLink);

			String orgPeopleLink = e.child(1).tagName("a").attr("href");
			githubProfile.put("organization_people_link", "https://github.com" + orgPeopleLink);

			String orgPeopleNumber = e.child(1).tagName("a").child(1).text();
			githubProfile.put("organization_people_number", orgPeopleNumber);
		}

		JSONArray jsonArray = new JSONArray();
		jsonArray.put(githubProfile);

		SusiThought json = new SusiThought();
		json.setData(jsonArray);
		return json;
	}

}
