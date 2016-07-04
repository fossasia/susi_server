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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

		String startingTime = null;
		String endingTime = null;

		String ticketURL = null;

		Elements tagSection = null;
		Elements tagSpan = null;
		String[][] tags = new String[5][2];
		String topic = null; // By default

		String closingDateTime = null;
		String schedulePublishedOn = null;
		JSONObject creator = new JSONObject();
		String email = null;

		Float latitude = null;
		Float longitude = null;

		String privacy = "public"; // By Default
		String state = "completed"; // By Default
		String eventType = "";

		eventID = htmlPage.getElementsByTag("body").attr("data-event-id");
		eventName = htmlPage.getElementsByClass("listing-hero-body").text();
		eventDescription = htmlPage.select("div.js-xd-read-more-toggle-view.read-more__toggle-view").text();

		eventColor = null;

		imageLink = htmlPage.getElementsByTag("picture").attr("content");

		eventLocation = htmlPage.select("p.listing-map-card-street-address.text-default").text();
		startingTime = htmlPage.getElementsByAttributeValue("property", "event:start_time").attr("content").substring(0,
				19);
		endingTime = htmlPage.getElementsByAttributeValue("property", "event:end_time").attr("content").substring(0,
				19);

		ticketURL = url + "#tickets";

		// TODO Tags to be modified to fit in the format of Open Event "topic"
		tagSection = htmlPage.getElementsByAttributeValue("data-automation", "ListingsBreadcrumbs");
		tagSpan = tagSection.select("span");
		topic = "";

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

		creator.put("email", "");
		creator.put("id", "1"); // By Default

		latitude = Float
				.valueOf(htmlPage.getElementsByAttributeValue("property", "event:location:latitude").attr("content"));
		longitude = Float
				.valueOf(htmlPage.getElementsByAttributeValue("property", "event:location:longitude").attr("content"));

		// TODO This returns: "events.event" which is not supported by Open
		// Event Generator
		// eventType = htmlPage.getElementsByAttributeValue("property",
		// "og:type").attr("content");

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

		JSONArray socialLinks = new JSONArray();

		JSONObject fb = new JSONObject();
		fb.put("id", "1");
		fb.put("name", "Facebook");
		fb.put("link", organizerFacebookAccountLink);
		socialLinks.put(fb);

		JSONObject tw = new JSONObject();
		tw.put("id", "2");
		tw.put("name", "Twitter");
		tw.put("link", organizerTwitterAccountLink);
		socialLinks.put(tw);

		JSONArray jsonArray = new JSONArray();

		JSONObject event = new JSONObject();
		event.put("event_url", url);
		event.put("id", eventID);
		event.put("name", eventName);
		event.put("description", eventDescription);
		event.put("color", eventColor);
		event.put("background_url", imageLink);
		event.put("closing_datetime", closingDateTime);
		event.put("creator", creator);
		event.put("email", email);
		event.put("location_name", eventLocation);
		event.put("latitude", latitude);
		event.put("longitude", longitude);
		event.put("start_time", startingTime);
		event.put("end_time", endingTime);
		event.put("logo", imageLink);
		event.put("organizer_description", organizerDescription);
		event.put("organizer_name", organizerName);
		event.put("privacy", privacy);
		event.put("schedule_published_on", schedulePublishedOn);
		event.put("state", state);
		event.put("type", eventType);
		event.put("ticket_url", ticketURL);
		event.put("social_links", socialLinks);
		event.put("topic", topic);
		jsonArray.put(event);

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

		JSONArray microlocations = new JSONArray();
		jsonArray.put(microlocations);

		JSONArray customForms = new JSONArray();
		jsonArray.put(customForms);

		JSONArray sessionTypes = new JSONArray();
		jsonArray.put(sessionTypes);

		JSONArray sessions = new JSONArray();
		jsonArray.put(sessions);

		JSONArray sponsors = new JSONArray();
		jsonArray.put(sponsors);

		JSONArray speakers = new JSONArray();
		jsonArray.put(speakers);

		JSONArray tracks = new JSONArray();
		jsonArray.put(tracks);

		JSONObject eventBriteResult = new JSONObject();
		eventBriteResult.put("Event Brite Event Details", jsonArray);

		// print JSON
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(eventBriteResult.toString(2));
		sos.println();

		String userHome = System.getProperty("user.home");
		String path = userHome + "/Downloads/EventBriteInfo";

		new File(path).mkdir();

		try (FileWriter file = new FileWriter(path + "/event.json")) {
			file.write(event.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/org.json")) {
			file.write(org.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/social_links.json")) {
			file.write(socialLinks.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/microlocations.json")) {
			file.write(microlocations.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/custom_forms.json")) {
			file.write(customForms.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/session_types.json")) {
			file.write(sessionTypes.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/sessions.json")) {
			file.write(sessions.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/sponsors.json")) {
			file.write(sponsors.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/speakers.json")) {
			file.write(speakers.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try (FileWriter file = new FileWriter(path + "/tracks.json")) {
			file.write(tracks.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		try {
			zipFolder(path, userHome + "/Downloads");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

	}

	static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;
		fileWriter = new FileOutputStream(destZipFile);
		zip = new ZipOutputStream(fileWriter);
		addFolderToZip("", srcFolder, zip);
		zip.flush();
		zip.close();
	}

	static private void addFileToZip(String path, String srcFile, ZipOutputStream zip) throws Exception {
		File folder = new File(srcFile);
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream(srcFile);
			zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
			while ((len = in.read(buf)) > 0) {
				zip.write(buf, 0, len);
			}
			in.close();
		}
	}

	static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
		File folder = new File(srcFolder);

		for (String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
			} else {
				addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
			}
		}
	}

}
