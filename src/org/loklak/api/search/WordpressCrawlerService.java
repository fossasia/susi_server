/**
 *  Wordpress Crawler
 *  Copyright 08.06.2016 by Jigyasa Grover, @jig08
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

import javax.servlet.http.HttpServletResponse;

public class WordpressCrawlerService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -5357182691897402354L;

	@Override
	public String getAPIPath() {
		return "/api/wordpresscrawler.json";
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
	public JSONObject serviceImpl(Query call, HttpServletResponse response, Authorization rights, JSONObjectWithDefault permissions)
			throws APIException {
		String url = call.get("url", "");
		return crawlWordpress(url);
	}
	
	public static SusiThought crawlWordpress(String blogURL) {
		Document blogHTML = null;

		Elements articles = null;
		Elements articleList_title = null;
		Elements articleList_content = null;
		Elements articleList_dateTime = null;
		Elements articleList_author = null;

		String[][] blogPosts = new String[100][4];

		// blogPosts[][0] = Blog Title
		// blogPosts[][1] = Posted On
		// blogPosts[][2] = Author
		// blogPosts[][3] = Blog Content

		Integer numberOfBlogs = 0;
		Integer iterator = 0;

		try {
			blogHTML = Jsoup.connect(blogURL).get();
		} catch (IOException e) {
			e.printStackTrace();
		}

		articles = blogHTML.getElementsByTag("article");

		iterator = 0;
		for (Element article : articles) {

			articleList_title = article.getElementsByClass("entry-title");
			for (Element blogs : articleList_title) {
				blogPosts[iterator][0] = blogs.text().toString();
			}

			articleList_dateTime = article.getElementsByClass("posted-on");
			for (Element blogs : articleList_dateTime) {
				blogPosts[iterator][1] = blogs.text().toString();
			}

			articleList_author = article.getElementsByClass("byline");
			for (Element blogs : articleList_author) {
				blogPosts[iterator][2] = blogs.text().toString();
			}

			articleList_content = article.getElementsByClass("entry-content");
			for (Element blogs : articleList_content) {
				blogPosts[iterator][3] = blogs.text().toString();
			}

			iterator++;

		}

		numberOfBlogs = iterator;

		JSONArray blog = new JSONArray();

		for (int k = 0; k < numberOfBlogs; k++) {
			JSONObject blogpost = new JSONObject();
			blogpost.put("blog_url", blogURL);
			blogpost.put("title", blogPosts[k][0]);
			blogpost.put("posted_on", blogPosts[k][1]);
			blogpost.put("author", blogPosts[k][2]);
			blogpost.put("content", blogPosts[k][3]);
			blog.put(blogpost);
		}
		
		SusiThought json = new SusiThought();
		json.setData(blog);
		return json;
	
	}

}
