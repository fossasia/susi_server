/**
 *  GenericScraper
 *  Copyright 16.06.2016 by Damini Satya, @daminisatya
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

import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;

import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import java.io.PrintWriter;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class GenericScraper extends HttpServlet {

	private static final long serialVersionUID = 4653635987712691127L;

	/**
     * PrintJSON
     * @param response
     * @param JSONObject genericScraperData
     */
	public void printJSON(HttpServletResponse response, JSONObject genericScraperData) throws ServletException, IOException {
		response.setCharacterEncoding("UTF-8");
		PrintWriter sos = response.getWriter();
		sos.print(genericScraperData.toString(2));
		sos.println();
	}

	/**
     * Article API
     * @param URL
     * @param JSONObject genericScraperData
     * @return genericScraperData
     */
	public JSONObject articleAPI (String url, JSONObject genericScraperData) throws MalformedURLException{
        URL qurl = new URL(url);
        String data = "";

        try {
            data = ArticleExtractor.INSTANCE.getText(qurl);
            genericScraperData.put("query", qurl);
            genericScraperData.put("data", data);
            genericScraperData.put("NLP", "true");
        }
        catch (Exception e) {
            if ("".equals(data)) {
                try 
                {
                    Document htmlPage = Jsoup.connect(url).get();
                    data = htmlPage.text();
                    genericScraperData.put("query", qurl);
                    genericScraperData.put("data", data);
                    genericScraperData.put("NLP", "false");
                }
                catch (Exception ex) {

                }
            }
        }

        return genericScraperData;
    }

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        Query post = RemoteAccess.evaluate(request);

        String url = post.get("url", "");
        String type = post.get("type", "");

        URL qurl = new URL(url);

        // This can also be done in one line:
        JSONObject genericScraperData = new JSONObject(true);
        if ("article".equals(type)) {
            genericScraperData = articleAPI(url, genericScraperData);
            genericScraperData.put("type", type);
            printJSON(response, genericScraperData);
        } else {
        	genericScraperData.put("error", "Please mentione type of scraper: <article>");
        	printJSON(response, genericScraperData);
        }
        // Also try other extractors!
//        System.out.println(DefaultExtractor.INSTANCE.getText(url));
//       System.out.println(CommonExtractors.CANOLA_EXTRACTOR.getText(url));
    }
}