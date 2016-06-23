/**
 *  GenericScraper
 *  Copyright 22.02.2015 by Damini Satya, @daminisatya
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
import java.net.URLEncoder;
import java.util.*;
import java.io.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.server.Query;
import org.loklak.tools.CharacterCoding;
import org.loklak.tools.UTF8;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GenericScraper extends HttpServlet {

	private static final long serialVersionUID = 4653635987712691127L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Query post = RemoteAccess.evaluate(request);

		// manage DoS
		if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

		// evaluate get parameters
		String url = post.get("url", "");
		JSONObject obj = new JSONObject();
		//loading the data from the URL
		Document page = Jsoup.connect(url).get();
		String title = page.title();

		List<String> linkHref = new ArrayList<String>();
		List<String> linkText = new ArrayList<String>();
		List<String> articleTags = new ArrayList<String>();
		List<String> preTags = new ArrayList<String>();
		List<String> codeTags = new ArrayList<String>();
		List<String> image = new ArrayList<String>();
		List<String> src = new ArrayList<String>();
		List<String> audio = new ArrayList<String>();
		List<String> video = new ArrayList<String>();

		Elements links = page.getElementsByTag("a");
		Elements links2 = page.getElementsByTag("link");
		Elements srciptLinks = page.getElementsByTag("script");
		Elements imageLinks = page.getElementsByTag("img");
		Elements taglang = page.getElementsByTag("html");
		Elements articles = page.getElementsByTag("article");
		Elements pres = page.getElementsByTag("pre");
		Elements codes = page.getElementsByTag("code");
		Elements audios = page.getElementsByTag("audio");
		Elements videos = page.getElementsByTag("video");
		Elements videos2 = page.getElementsByTag("iframe");
		Elements videos3 = page.getElementsByTag("object");
		Elements videos4 = page.getElementsByTag("embed");

		String language = taglang.attr("lang");
		for (Element link : links) {
			if(link.attr("href") != null && link.attr("href").length() != 0){
				linkHref.add(link.attr("href"));
			}
			if(link.text() != null && link.text().length() != 0){
				linkText.add(link.text());
			}
		}
		for (Element link : links2) {
			if(link.attr("href") != null && link.attr("href").length() != 0){
				linkHref.add(link.attr("href"));
			}
		}
		for (Element link : srciptLinks) {
			if(link.attr("src") != null && link.attr("src").length() != 0){
				src.add(link.attr("src"));
			}
		}
		for (Element link : imageLinks) {
			if(link.attr("src") != null && link.attr("src").length() != 0){
				image.add(link.attr("src"));
			}
		}
		for (Element article : articles){
			if(article.text() != null && article.text().length() != 0){
				articleTags.add(article.text());
			}
		}
		for (Element pre : pres){
			if(pre.text() != null && pre.text().length() != 0){
				preTags.add(pre.text());
			}
		}
		for (Element code : codes){
			if(code.text() != null && code.text().length() != 0){
				codeTags.add(code.text());
			}
		}
		for (Element link : audios) {
			Elements audioSources = link.getElementsByTag("source");
			if(!(audioSources.isEmpty())){
				for(Element audioLink : audioSources){
					if(audioLink.attr("src") != null && audioLink.attr("src").length() != 0){
						audio.add(audioLink.attr("src"));
					}
				}
			}
			if(link.attr("src") != null && link.attr("src").length() != 0){
				audio.add(link.attr("src"));
			}
		}
		for (Element link : videos) {
			Elements videoSources = link.getElementsByTag("source");
			if(!(videoSources.isEmpty())){
				for(Element videoLink : videoSources){
					if(videoLink.attr("src") != null && videoLink.attr("src").length() != 0){
						video.add(videoLink.attr("src"));
					}
				}
			}
			if(link.attr("src") != null && link.attr("src").length() != 0){
				video.add(link.attr("src"));
			}
		}
		for (Element link : videos2) {
			if(link.attr("src") != null && link.attr("src").length() != 0){
				video.add(link.attr("src"));
			}
		}
		for (Element link : videos3) {
			if(link.attr("src") != null && link.attr("src").length() != 0){
				video.add(link.attr("src"));
			}
		}
		for (Element link : videos4) {
			if(link.attr("src") != null && link.attr("src").length() != 0){
				video.add(link.attr("src"));
			}
		}
		obj.put("title", title);
		obj.put("language", language);
		obj.put("Links", new JSONArray(linkHref));
		obj.put("Text in Links", new JSONArray(linkText));
		obj.put("source files", new JSONArray(src));
		obj.put("Image files", new JSONArray(image));
		obj.put("Articles", new JSONArray(articleTags));
		obj.put("Preformatted Text", new JSONArray(preTags));
		obj.put("Code", new JSONArray(codeTags));
		obj.put("Audio", new JSONArray(audio));
		obj.put("Video", new JSONArray(video));

		//print JSON 
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(obj.toString(2));
		sos.println();
	}
}
