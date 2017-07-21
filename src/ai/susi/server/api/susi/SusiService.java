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

package ai.susi.server.api.susi;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiArgument;
import ai.susi.mind.SusiCognition;
import ai.susi.mind.SusiSkill;
import ai.susi.mind.SusiMind;
import ai.susi.mind.SusiThought;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        // parameters
        String q = post.get("q", "").trim();
        int count = post.get("count", 1);
        int timezoneOffset = post.get("timezoneOffset", 0); // minutes, i.e. -60
        double latitude = post.get("latitude", Double.NaN); // i.e. 8.68 
        double longitude = post.get("longitude", Double.NaN); // i.e. 50.11
        String language = post.get("language", "en"); // ISO 639-1
        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }
        
        // compute a recall
        SusiArgument observation_argument = new SusiArgument();
        List<SusiCognition> cognitions = DAO.susi.getMemories().getCognitions(user.getIdentity().getClient());
        cognitions.forEach(cognition -> observation_argument.think(cognition.recallDispute()));
        SusiThought recall = observation_argument.mindmeld(false);
        
        // find out if we are dreaming
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
                SusiMind dream = new SusiMind(DAO.susi_memory_dir); // we need the memory directory here to get a share on the memory of previous dialoges, otherwise we cannot test call-back questions
                JSONObject rules = SusiSkill.readEzDSkill(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)));
                dream.learn(rules, new File("file://" + etherpad_dream));
                // susi is now dreaming.. Try to find an answer out of the dream
                SusiCognition cognition = new SusiCognition(dream, q, timezoneOffset, latitude, longitude, language, count, user.getIdentity());
                if (cognition.getAnswers().size() > 0) {
                    DAO.susi.getMemories().addCognition(user.getIdentity().getClient(), cognition);
                    return new ServiceResponse(cognition.getJSON());
                }
            } catch (JSONException | IOException e) {
                e.printStackTrace();
            }
        }
        
        // answer with built-in intents
        SusiCognition cognition = new SusiCognition(DAO.susi, q, timezoneOffset, latitude, longitude, language, count, user.getIdentity());
        DAO.susi.getMemories().addCognition(user.getIdentity().getClient(), cognition);
        JSONObject json = cognition.getJSON();
        return new ServiceResponse(json);
    }
    
}
