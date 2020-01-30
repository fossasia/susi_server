/**
 *  GetSkillJsonService
 *  Copyright 28.05.2017 by Michael Peter Christen, @0rb1t3r
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
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Endpoint to return text of a skill in JSON format from the skill database
 * i.e.
 * http://localhost:4000/cms/getSkill.json
 * This accepts 4 parameters: - Model, Group, Language and Skill Name
 * http://localhost:4000/cms/getSkill.json?model=general&group=Knowledge&language=en&skill=wikipedia
 * To get a private skill also send access_token and private=1 parameters
 * http://localhost:4000/cms/getSkill.json?group=Communication&language=en&skill=testD&private=1&access_token=3idhpIajP4oPAe8862fElL8c9hxCdP
 */

public class GetSkillJsonService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 18344223L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        JSONObject json = new JSONObject(true);
        // modify caching
        json.put("$EXPIRES", 0);
        json.put("accepted", false);
        String userId = null;
        String access_token = call.get("access_token", null);
        String email = call.get("email", null);
        ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, access_token);
        Authentication authentication = DAO.getAuthentication(credential);
        if (authentication.getIdentity() != null) {
            ClientIdentity identity = authentication.getIdentity();
            Authorization authorization = DAO.getAuthorization(identity);
            UserRole userRole = authorization.getUserRole();
            if(email != null && (userRole.getName().equals("admin") || userRole.getName().equals("superadmin"))){
                ClientIdentity userIdentity = new ClientIdentity(ClientIdentity.Type.email, email);
                userId = userIdentity.getUuid();
            } else {
                userId = identity.getUuid();
            }
        }
        // if client sends private=1 then it is a private skill
        String privateSkill = call.get("private", null);
        File private_skill_dir = null;
        if(privateSkill != null){
            private_skill_dir = new File(DAO.private_skill_watch_dir,userId);
        }
        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        if(privateSkill != null){
            model = private_skill_dir;
        }
        String group_name = call.get("group", "Knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String skill_name = call.get("skill", "wikipedia");
        File skill = DAO.getSkillFileInLanguage(language, skill_name, false);

        try {
            String content = new String(Files.readAllBytes(skill.toPath()));
            json.put("text",content);
            json.put("accepted",true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResponse(json);
    }
}
