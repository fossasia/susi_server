/**
 *  Earthquake Servlet
 *  Copyright 15.08.2016 by Sudheesh Singanamalla, @sudheesh001
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

package org.loklak.api.iot;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.io.Reader;

import java.net.*;
import org.json.JSONObject;
import org.json.JSONException;
import org.loklak.server.Query;
import org.loklak.http.RemoteAccess;

/* Sample queries that can be constructed
===========================================
1. http://localhost:9000/api/earthquake.json?duration=month&magnitude=4.5
2. http://localhost:9000/api/earthquake.json?duration=week&magnitude=4.5
3. http://localhost:9000/api/earthquake.json?duration=day&magnitude=4.5
4. http://localhost:9000/api/earthquake.json?duration=hour&magnitude=4.5

The different sets of magnitudes supported by USGS GOV EarthQuake Data sets are
1. Magnitude : <4.5, 2.5, 1.0, significant>
2. Duration  : <hour, day, week, month>
*/

public class EarthquakeServlet extends HttpServlet {

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}


	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Query post = RemoteAccess.evaluate(request);

    	String eqBaseUrl = "http://earthquake.usgs.gov/earthquakes/feed/v1.0/";

    	// Earthquakes happening per hour.
    	String SIGNIFICANT_HOUR = "summary/significant_hour.geojson";
    	String MAGNITUDE_4_5_HOUR = "summary/4.5_hour.geojson";
    	String MAGNITUDE_2_5_HOUR = "summary/2.5_hour.geojson";
    	String MAGNITUDE_1_0_HOUR = "summary/1.0_hour.geojson";
    	String ALL_HOUR = "summary/all_hour.geojson";

    	// Earthquakes happening per day.
    	String SIGNIFICANT_DAY = "summary/significant_day.geojson";
    	String MAGNITUDE_4_5_DAY = "summary/4.5_day.geojson";
    	String MAGNITUDE_2_5_DAY = "summary/2.5_day.geojson";
    	String MAGNITUDE_1_0_DAY = "summary/1.0_day.geojson";
    	String ALL_DAY = "summary/all_day.geojson";

    	// Earthquakes in the last 7 days.
    	String SIGNIFICANT_WEEK = "summary/significant_week.geojson";
    	String MAGNITUDE_4_5_WEEK = "summary/4.5_week.geojson";
    	String MAGNITUDE_2_5_WEEK = "summary/2.5_week.geojson";
    	String MAGNITUDE_1_0_WEEK = "summary/1.0_week.geojson";
    	String ALL_WEEK = "summary/all_week.geojson";

    	// Earthquakes in the last 30 days.
    	String SIGNIFICANT_MONTH = "summary/significant_month.geojson";
    	String MAGNITUDE_4_5_MONTH = "summary/4.5_month.geojson";
    	String MAGNITUDE_2_5_MONTH = "summary/2.5_month.geojson";
    	String MAGNITUDE_1_0_MONTH = "summary/1.0_month.geojson";
    	String ALL_MONTH = "summary/all_month.geojson";

    	// manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        String duration = post.get("duration", "");
        String magnitude = post.get("magnitude", "");

        String earthquakeQueryURL = "";

        if ("hour".equals(duration)) {
            if ("4.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_4_5_HOUR;
            }
            else if ("2.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_2_5_HOUR;
            }
            else if ("1.0".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_1_0_HOUR;
            }
            else if ("significant".equals(magnitude)){
                earthquakeQueryURL = eqBaseUrl + SIGNIFICANT_HOUR;
            }
            else {
                earthquakeQueryURL = eqBaseUrl + ALL_HOUR;
            }
        }
        else if ("day".equals(duration)) {
            if ("4.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_4_5_DAY;
            }
            else if ("2.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_2_5_DAY;
            }
            else if ("1.0".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_1_0_DAY;
            }
            else if ("significant".equals(magnitude)){
                earthquakeQueryURL = eqBaseUrl + SIGNIFICANT_DAY;
            }
            else {
                earthquakeQueryURL = eqBaseUrl + ALL_DAY;
            }
        }
        else if ("week".equals(duration)) {
            if ("4.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_4_5_WEEK;
            }
            else if ("2.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_2_5_WEEK;
            }
            else if ("1.0".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_1_0_WEEK;
            }
            else if ("significant".equals(magnitude)){
                earthquakeQueryURL = eqBaseUrl + SIGNIFICANT_WEEK;
            }
            else {
                earthquakeQueryURL = eqBaseUrl + ALL_WEEK;
            }
        }
        else if ("month".equals(duration)) {
            if ("4.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_4_5_MONTH;
            }
            else if ("2.5".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_2_5_MONTH;
            }
            else if ("1.0".equals(magnitude)) {
                earthquakeQueryURL = eqBaseUrl + MAGNITUDE_1_0_MONTH;
            }
            else if ("significant".equals(magnitude)){
                earthquakeQueryURL = eqBaseUrl + SIGNIFICANT_MONTH;
            }
            else {
                earthquakeQueryURL = eqBaseUrl + ALL_MONTH;
            }
        }
        else {
            earthquakeQueryURL = eqBaseUrl + SIGNIFICANT_HOUR;
        }
        // earthquakeQueryURL = eqBaseUrl + MAGNITUDE_1_0_MONTH;

        JSONObject json = readJsonFromUrl(earthquakeQueryURL);
        json.put("query", earthquakeQueryURL);

        PrintWriter sos = response.getWriter();
        sos.print(json.toString(2));
        sos.println();
    }

}