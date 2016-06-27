/**
 *  Eventbrite.com Crawler v2.0
 *  Copyright 19.06.2016 by Jigyasa Grover, @jig08
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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;

public class EventbriteCrawler extends HttpServlet {

	private static final long serialVersionUID = 5216519528576842483L;

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

		String url = post.get("url", "");

		Document htmlPage = null;

		try {
			htmlPage = Jsoup.connect(url).get();
		} catch (Exception e) {
			e.printStackTrace();
		}

		String eventID = null;
		String eventName = null;
		String eventDescription = null;

		// TODO Fetch Event Color
		String eventColor = null;

		String imageLink = null;

		String eventLocation = null;
		String eventTime = null;

		String ticketURL = null;

		Elements tagSection = null;
		Elements tagSpan = null;
		String[][] tags = new String[5][2];

		eventID = htmlPage.getElementsByTag("body").attr("data-event-id");
		eventName = htmlPage.getElementsByClass("listing-hero-body").text();
		eventDescription = htmlPage.select("div.js-xd-read-more-toggle-view.read-more__toggle-view").text();

		eventColor = "";

		imageLink = htmlPage.getElementsByTag("picture").attr("content");

		eventLocation = htmlPage.select("p.listing-map-card-street-address.text-default").text();
		eventTime = htmlPage.select("time.clrfix").text();

		ticketURL = url + "#tickets";

		tagSection = htmlPage.getElementsByAttributeValue("data-automation", "ListingsBreadcrumbs");
		tagSpan = tagSection.select("span");

		int iterator = 0, k = 0;
		for (Element e : tagSpan) {
			if (iterator % 2 == 0) {
				tags[k][1] = "www.eventbrite.com"
						+ e.select("a.js-d-track-link.badge.badge--tag.l-mar-top-2").attr("href");
			} else {
				tags[k][0] = e.text();
				k++;
			}
			iterator++;
		}

		JSONArray jsonArray = new JSONArray();

		JSONObject event = new JSONObject();
		event.put("event_id", eventID);
		event.put("name", eventName);
		event.put("description", eventDescription);
		event.put("color", eventColor);
		event.put("image_link", imageLink);
		event.put("location", eventLocation);
		event.put("time", eventTime);
		event.put("ticket_url", ticketURL);
		event.put("tags", tags);
		jsonArray.put(event);

		String organizerName = null;
		String organizerLink = null;
		String organizerProfileLink = null;
		String organizerWebsite = null;
		String organizerContactInfo = null;
		String organizerDescription = null;
		String organizerFacebookFeedLink = null;
		String organizerTwitterFeedLink = null;
		String organizerFacebookAccountLink = null;
		String organizerTwitterAccountLink = null;

		organizerName = htmlPage.select("a.js-d-scroll-to.listing-organizer-name.text-default").text().substring(4);
		organizerLink = url + "#listing-organizer";
		organizerProfileLink = htmlPage
				.getElementsByAttributeValue("class", "js-follow js-follow-target follow-me fx--fade-in is-hidden")
				.attr("href");
		organizerContactInfo = url + "#lightbox_contact";

		Document orgProfilePage = null;

		try {
			orgProfilePage = Jsoup.connect(organizerProfileLink).get();
		} catch (Exception e) {
			e.printStackTrace();
		}

		organizerWebsite = orgProfilePage.getElementsByAttributeValue("class", "l-pad-vert-1 organizer-website").text();
		organizerDescription = orgProfilePage.select("div.js-long-text.organizer-description").text();
		organizerFacebookFeedLink = organizerProfileLink + "#facebook_feed";
		organizerTwitterFeedLink = organizerProfileLink + "#twitter_feed";
		organizerFacebookAccountLink = orgProfilePage.getElementsByAttributeValue("class", "fb-page").attr("data-href");
		organizerTwitterAccountLink = orgProfilePage.getElementsByAttributeValue("class", "twitter-timeline")
				.attr("href");

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

		JSONObject eventBriteResult = new JSONObject();
		eventBriteResult.put("Event Brite Event Details", jsonArray);

		// print JSON
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(eventBriteResult.toString(2));
		sos.println();
	}
}
