/**
 *  Freifunk Node Fetch Servlet
 *  Copyright 16.08.2016 by Sudheesh Singanamalla, @sudheesh001
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

/*
Requests the freifunk router network and fetches the information from the routers.
==================================================================================

"communities": {
    "aachen": {
      "name": "Freifunk Aachen",
      "url": "http://www.Freifunk-Aachen.de",
      "meta": "Freifunk Regio Aachen"
    }...,
}

"allTheRouters": [
    {
      "id": "60e327366bfe",
      "lat": "50.564485",
      "long": "6.359705",
      "name": "ffac-FeWo-Zum-Sonnenschein-2",
      "community": "aachen",
      "status": "online",
      "clients": 1
    }...,
}
*/

public class FreifunkNodeFetchServlet extends HttpServlet {

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

    	String freifunkDataQuery = "http://www.freifunk-karte.de/data.php";

    	// manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        JSONObject json = readJsonFromUrl(freifunkDataQuery);

        PrintWriter sos = response.getWriter();
        sos.print(json.toString(2));
        sos.println();

    }

}