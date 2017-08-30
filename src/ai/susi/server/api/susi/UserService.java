/**
 *  UserService
 *  Copyright 24.05.2017 by Michael Peter Christen, @0rb1t3r
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

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiCognition;
import ai.susi.server.*;
import ai.susi.tools.DateParser;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * get information about the user.
 * i.e. it will return the history of conversation with two memories starting from
 * given date. Also returns number of cognitions left to return.
 * http://127.0.0.1:4000/susi/memory.json?cognitions=2&date=2017-06-27T18:56:12.389Z
 */
public class UserService extends AbstractAPIHandler implements APIHandler {
   
    private static final long serialVersionUID = 8578478303098111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/memory.json";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        int cognitionsCount = Math.min(10, post.get("cognitions", 10));
        String date = post.get("date",null);
        boolean isValidDate = (date != null);

        String client = user.getIdentity().getClient();
        List<SusiCognition> cognitions = DAO.susi.getMemories().getCognitions(client);

        //Stores all dates in arraylist
        List<Date> dates = new ArrayList<>();
        for(int i=0 ; i<cognitions.size() ; i++) {
            dates.add(cognitions.get(i).getQueryDate());
        }

        Date keyDate = null;
        //Checks if date is valid
        if(isValidDate) {
            try {
                keyDate = DateParser.utcFormatter.parseDateTime(date).toDate();
                isValidDate = true;
            } catch (IllegalArgumentException e) {
                isValidDate = false;
            }
        } else {
            isValidDate = false;
        }

        int index = -1;

        if(isValidDate)
            index = dates.indexOf(keyDate);

        JSONArray coga = new JSONArray();
        int cognitionsRemaining = cognitions.size();
        if(index == -1) {
            for (SusiCognition cognition : cognitions) {
                coga.put(cognition.getJSON());
                cognitionsRemaining--;
                if (--cognitionsCount <= 0) break;
            }
        } else {
            cognitionsRemaining -= index;
            for(int i=index ; i<cognitions.size() ; i++) {
                coga.put(cognitions.get(i).getJSON());
                cognitionsRemaining--;
                if (--cognitionsCount <= 0) break;
            }
        }
        JSONObject json = new JSONObject(true);
        json.put("cognitions", coga);
        json.put("cognitions_remaining", cognitionsRemaining);
        return new ServiceResponse(json);
    }
}
