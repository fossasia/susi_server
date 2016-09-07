/**
 *  NOAA Alerts Servlet
 *  Copyright 02.07.2016 by Sudheesh Singanamalla, @sudheesh001
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.PrintWriter;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.XML;
import org.loklak.server.Query;
import org.loklak.http.RemoteAccess;
import org.loklak.data.DAO;

public class NOAAAlertServlet extends HttpServlet {

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Query post = RemoteAccess.evaluate(request);

    	// manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

        String content = new String(Files.readAllBytes(Paths.get(DAO.conf_dir + "/iot/scripts/counties.xml")));
        try {
            // Conversion of the XML Layers through PERL into the required JSON for well structured XML
            /*
            <resources>
                <string-array name="preference_county_entries_us">
                    <item>Entire Country</item>
                </string-array>
                <string-array name="preference_county_entryvalues_us">
                    <item>https://alerts.weather.gov/cap/us.php?x=0</item>
                </string-array>
                .
                .
                .
                Similarly every 2 DOM elements together in <resources> constitute a pair.
            </resources>
            */
        	JSONObject json = XML.toJSONObject(content);
        	PrintWriter sos = response.getWriter();
            JSONObject resourceObject = json.getJSONObject("resources");
            JSONArray stringArray = resourceObject.getJSONArray("string-array");
            JSONObject result = new JSONObject(true);

            // Extract and map the itemname and the url strings
            /*
            {
                "item": "Entire Country",
                "name": "preference_county_entries_us"
            },
            {
                "item": "https://alerts.weather.gov/cap/us.php?x=0",
                "name": "preference_county_entryvalues_us"
            }
            */
            for (int i = 0 ; i < stringArray.length(); i+=2) {
                JSONObject keyJSONObject = stringArray.getJSONObject(i);
                JSONObject valueJSONObject = stringArray.getJSONObject(i+1);
                Object kItemObj = keyJSONObject.get("item");
                Object vItemObj = valueJSONObject.get("item");

                // Since instances are variable, we need to check if they're Arrays or Strings
                // The processing for the Key : Value mappings will change for each type of instance
                if (kItemObj instanceof JSONArray) {
                    if (vItemObj instanceof JSONArray) {
                        JSONArray kArray = keyJSONObject.getJSONArray("item");
                        JSONArray vArray = valueJSONObject.getJSONArray("item");
                        for (int location = 0; location < kArray.length(); location++) {
                            String kValue = kArray.getString(location);
                            String vValue = vArray.getString(location);
                            result.put(kValue, vValue);
                        }
                    }
                }
                else {
                    // They are plain strings
                    String kItemValue = keyJSONObject.getString("item");
                    String vItemValue = valueJSONObject.getString("item");
                    result.put(kItemValue, vItemValue);
                }

            }
            // Sample response in result has to be something like
            /*
            {
                "Entire Country": "https://alerts.weather.gov/cap/us.php?x=0",
                "Entire State": "https://alerts.weather.gov/cap/wy.php?x=0",
                "Autauga": "https://alerts.weather.gov/cap/wwaatmget.php?x=ALC001&y=0",
                "Baldwin": "https://alerts.weather.gov/cap/wwaatmget.php?x=GAC009&y=0",
                "Barbour": "https://alerts.weather.gov/cap/wwaatmget.php?x=WVC001&y=0",
                "Bibb": "https://alerts.weather.gov/cap/wwaatmget.php?x=GAC021&y=0",
                .
                .
                .
                And so on.
            }
            */

        	sos.print(result.toString(2));
        	sos.println();
        }
        catch (IOException e) {
        	Log.getLog().warn(e);
        	JSONObject json = new JSONObject(true);
        	json.put("error", "Looks like there is an error in the conversion");
        	json.put("type", "Error");
        	PrintWriter sos = response.getWriter();
        	sos.print(json.toString(2));
        	sos.println();
        }

    }

}