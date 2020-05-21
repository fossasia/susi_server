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

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiCognition;
import ai.susi.mind.SusiFace;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import ai.susi.tools.EtherpadClient;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// http://127.0.0.1:4000/susi/chat.json?q=wootr&instant=wootr%0d!example:x%0d!expect:y%0dyee
// http://127.0.0.1:4000/susi/chat.json?q=wootr&instant=wootr%0dyee

public class SusiService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 857847830309879111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/chat.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {
        String q = post.get("q", "").trim();
        boolean debug = "true".equals(post.get("debug", ""));
        EtherpadClient etherpad = null; try {etherpad = new EtherpadClient();} catch (IOException e) {}
        if (!etherpad.isPrivate()) etherpad = null; // do not log into a public etherpad
        String username = user.getIdentity().getName();
        int p = username.lastIndexOf('_'); if (p >= 0) username = username.substring(p + 1); // extract the salt from the identity
        DAO.log("CHAT susi is asked by " + username + ": " + q);

        SusiFace face = new SusiFace(q).setPost(post).setAuthorization(user).setDebug(debug);
        try {
            long start = System.currentTimeMillis();
            SusiCognition cognition = face.call();
            long time = (System.currentTimeMillis() - start);
            LinkedHashMap<String, List<String>> answers = cognition.getAnswers();
            for (Map.Entry<String, List<String>> answer: answers.entrySet()) {
                if (etherpad != null) try {
                    etherpad.appendChatMessage("susi", "@susi " + q, username); // do not move this line to the time before the question is answered, this conflicts with the caretaker task
                    etherpad.appendChatMessage("susi", "@" + username + " " + answer.getKey(), "susi");
                } catch (IOException e) {}
                DAO.log("CHAT susi answered to " + username + ": " + answer.getKey() + " (" + (((double) time) / 1000.0d) + "s)");
            }
            JSONObject json = cognition.getJSON();
            return new ServiceResponse(json);
        } catch (Exception e) {
            if (e instanceof APIException) throw (APIException) e;
            throw new APIException(500, e.getMessage());
        }
    }

}
