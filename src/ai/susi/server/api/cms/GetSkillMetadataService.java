/**
 *  GetSkillMetadataService
 *  Copyright 1.08.2017 by Saurabh Jain , @saurabhjn76
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

package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import ai.susi.json.JsonTray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 This Servlet gives a API Endpoint to list meta for a Skill. Given its model, group and language and skill.
 Can be tested on http://127.0.0.1:4000/cms/getSkillMetadata.json?model=general&group=Knowledge&language=en&skill=creator_info
 For private skill send the userid instead of model
 http://127.0.0.1:4000/cms/getSkillMetadata.json?userid=17a70987d09c33e6f56fe05dca6e3d27&group=Knowledge&language=en&skill=testnew
 */
public class GetSkillMetadataService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 3446536703362688060L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillMetadata.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        DAO.observe(); // get a database update

        String model = call.get("model", "");
        String userid = call.get("userid", "");
        String group = call.get("group", "");
        String language = call.get("language", "");
        String skillname = call.get("skill", "");

        if ((model.length() == 0 && userid.length() == 0) || group.length() == 0 ||language.length() == 0 || skillname.length() == 0 ) {
            JSONObject json = new JSONObject(true);
            json.put("accepted", false);
            json.put("message", "Error: Bad parameter call");
            return new ServiceResponse(json);
        }

        if(userid.length() != 0) {
            // fetch private skill (chatbot) meta data
            HttpServletRequest request = call.getRequest();
            String referer = request.getHeader("Referer");
            JsonTray chatbot = DAO.chatbot;
            JSONObject json = new JSONObject(true);
            JSONObject userObject = chatbot.getJSONObject(userid);
            JSONObject groupObject = userObject.getJSONObject(group);
            JSONObject languageObject = groupObject.getJSONObject(language);
            JSONObject skillObject = languageObject.getJSONObject(skillname);
            JSONObject configureObject = skillObject.getJSONObject("configure");
            if (DAO.allowDomainForChatbot(configureObject, referer) == true) {
                json.put("skill_metadata", skillObject);
                json.put("accepted", true);
                json.put("message", "Success: Fetched Skill's Metadata");
            }
            else {
                json.put("accepted", false);
                json.put("message", "Not allowed to use on this domain");
            }
            return new ServiceResponse(json);
        }

        else {
            JSONObject skillMetadata = SusiSkill.getSkillMetadata(model, group, language, skillname);
            JSONObject json = new JSONObject(true);
            json.put("skill_metadata", skillMetadata);
            json.put("accepted", true);
            json.put("message", "Success: Fetched Skill's Metadata");
            return new ServiceResponse(json);
        }

        
    }

}
