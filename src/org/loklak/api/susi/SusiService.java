/**
 *  SusiService
 *  Copyright 29.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.susi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.loklak.data.DAO;
import org.loklak.server.APIException;
import org.loklak.server.APIHandler;
import org.loklak.server.BaseUserRole;
import org.loklak.server.AbstractAPIHandler;
import org.loklak.server.Authorization;
import org.loklak.server.Query;
import org.loklak.susi.SusiArgument;
import org.loklak.susi.SusiAwareness;
import org.loklak.susi.SusiCognition;
import org.loklak.susi.SusiMind;
import org.loklak.susi.SusiThought;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.servlet.http.HttpServletResponse;

public class SusiService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/chat.json";
    }
    
    @Override
    public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization user, final JSONObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "").trim();
        int count = post.get("count", 1);
        int timezoneOffset = post.get("timezoneOffset", 0); // minutes, i.e. -60
        double latitude = post.get("latitude", Double.NaN); // i.e. 8.68 
        double longitude = post.get("longitude", Double.NaN); // i.e. 50.11
        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }
        
        // find out if we are dreaming
        SusiArgument observation_argument = new SusiArgument();
        List<SusiCognition> cognitions = DAO.susi.getMemories().getCognitions(user.getIdentity().getClient());
        cognitions.forEach(cognition -> observation_argument.think(cognition.recallDispute()));
        SusiThought recall = observation_argument.mindmeld(false);
        String etherpad_dream = recall.getObservation("_etherpad_dream");
        if (etherpad_dream != null && etherpad_dream.length() != 0) {
            // we are dreaming!
            // read the pad
            String etherpadApikey = DAO.getConfig("etherpad.apikey", "");
            String etherpadUrlstub = DAO.getConfig("etherpad.urlstub", "");
            String padurl = etherpadUrlstub + "/api/1/getText?apikey=" + etherpadApikey + "&padID=$query$";
            try {
                JSONTokener serviceResponse = new JSONTokener(new ByteArrayInputStream(ConsoleService.loadData(padurl, etherpad_dream)));
                JSONObject json = new JSONObject(serviceResponse);
                String text = json.getJSONObject("data").getString("text");
                // fill an empty mind with the dream
                SusiMind dream = new SusiMind(null, DAO.susi_watch_dir); // an empty mind!
                JSONObject rules = dream.readEzDLesson(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)));
                dream.learn(rules);
                // susi is now dreaming.. Try to find an answer out of the dream
                SusiCognition cognition = new SusiCognition(dream, q, timezoneOffset, latitude, longitude, count, user.getIdentity());
                if (cognition.getAnswers().size() > 0) {
                    DAO.susi.getMemories().addCognition(user.getIdentity().getClient(), cognition);
                    return cognition.getJSON();
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        
        // answer normally
        SusiCognition cognition = new SusiCognition(DAO.susi, q, timezoneOffset, latitude, longitude, count, user.getIdentity());
        DAO.susi.getMemories().addCognition(user.getIdentity().getClient(), cognition);
        JSONObject json = cognition.getJSON();
        return json;
    }
    
}
