/**
 *  GeocodeServlet
 *  Copyright 03.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.loklak.data.DAO;
import org.loklak.geo.GeoMark;
import org.loklak.http.RemoteAccess;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * geocoding of places into locations
 * test:
 * http://localhost:9000/api/geocode.json?data={%22places%22:[%22Frankfurt%20am%20Main%22,%22New%20York%22,%22Singapore%22]}
 * for reverse geocoding, try
 * http://localhost:9000/api/geocode.json?data={%22places%22:[%22iPhone:%2037.313690,-122.022911%22,%22%C3%9CT:%2019.109458,72.825842%22]}
 */
public class GeocodeServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
     
        // manage DoS
        if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}
        
        // parameters
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        boolean minified = post.get("minified", false);
        String data = post.get("data", "");
        String places = post.get("places", "");
        if (places.length() == 0 && data.length() == 0) {response.sendError(503, "you must submit a data attribut with a json containing the property 'places' with a list of place names"); return;}
        
        String[] place = new String[0];
        if (places.length() > 0) {
            place = places.split(",");
        } else {
            // parse the json data
            try {
                Map<String, Object> map = DAO.jsonMapper.readValue(data, DAO.jsonTypeRef);
                Object places_obj = map.get("places");
                if (places_obj instanceof List<?>) {
                    List<Object> p = (List<Object>) places_obj;
                    place = new String[p.size()];
                    int i = 0; for (Object o: p) place[i++] = (String) o; 
                } else {
                    response.sendError(400, "submitted data is not well-formed: expected a list of strings");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // find locations for places
        Map<String, Object> locations = new LinkedHashMap<>();
        for (String p: place) {
            GeoMark loc = DAO.geoNames.analyse(p, null, 5, Long.toString(System.currentTimeMillis()));
            Map<String, Object> location = new LinkedHashMap<>();
            if (loc != null) {
                location.put("place", minified ? new String[]{loc.getNames().iterator().next()} : loc.getNames());
                location.put("population", loc.getPopulation());
                location.put("country_code", loc.getISO3166cc());
                location.put("country", DAO.geoNames.getCountryName(loc.getISO3166cc()));
                location.put("location", new double[]{loc.lon(), loc.lat()});
                location.put("mark", new double[]{loc.mlon(), loc.mlat()});
            }
            locations.put(p, location);
        }
        
        post.setResponse(response, "application/javascript");

        // generate json
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("locations", locations);
        
        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print((minified ? new ObjectMapper().writer() : new ObjectMapper().writerWithDefaultPrettyPrinter()).writeValueAsString(m));
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
    
}
