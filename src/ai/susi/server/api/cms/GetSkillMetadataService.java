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
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 This Servlet gives a API Endpoint to list meta for a  Skill. Given its model, group and language and skill.
 Can be tested on 127.0.0.1:4000/cms/getSkillMetadata.json
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

        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }

        String model = call.get("model", "");
        String group = call.get("group", "");
        String language = call.get("language", "");
        String skill = call.get("skill", "");

        if (model.length() == 0 || group.length() == 0 ||language.length() == 0 || skill.length() == 0 ) {
            JSONObject json = new JSONObject(true);
            json.put("accepted", false);
            json.put("message", "Error: Bad parameter call");
            return new ServiceResponse(json);
        }

        JSONObject skillMetadata = new JSONObject(true)
                .put("model", model)
                .put("group", group)
                .put("language", language);
        skillMetadata.put("developer_privacy_policy", JSONObject.NULL);
        skillMetadata.put("descriptions",JSONObject.NULL);
        skillMetadata.put("image", JSONObject.NULL);
        skillMetadata.put("author", JSONObject.NULL);
        skillMetadata.put("author_url", JSONObject.NULL);
        skillMetadata.put("skill_name", JSONObject.NULL);
        skillMetadata.put("terms_of_use", JSONObject.NULL);
        skillMetadata.put("dynamic_content", false);
        skillMetadata.put("examples", JSONObject.NULL);
        for (Map.Entry<String, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0)
                    && (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("skill_name", entry.getValue().getSkillName() ==null ? JSONObject.NULL: entry.getValue().getSkillName());
                skillMetadata.put("developer_privacy_policy", entry.getValue().getDeveloperPrivacyPolicy() ==null ? JSONObject.NULL:entry.getValue().getDeveloperPrivacyPolicy());
                skillMetadata.put("descriptions", entry.getValue().getDescription() ==null ? JSONObject.NULL:entry.getValue().getDescription());
                skillMetadata.put("image", entry.getValue().getImage() ==null ? JSONObject.NULL: entry.getValue().getImage());
                skillMetadata.put("author", entry.getValue().getAuthor()  ==null ? JSONObject.NULL:entry.getValue().getAuthor());
                skillMetadata.put("author_url", entry.getValue().getAuthorURL() ==null ? JSONObject.NULL:entry.getValue().getAuthorURL());
                skillMetadata.put("terms_of_use", entry.getValue().getTermsOfUse() ==null ? JSONObject.NULL:entry.getValue().getTermsOfUse());
                skillMetadata.put("dynamic_content", entry.getValue().getDynamicContent());
                skillMetadata.put("examples", entry.getValue().getExamples() ==null ? JSONObject.NULL: entry.getValue().getExamples());
            }
        }
        JSONObject json = new JSONObject(true);
        json.put("skill_metadata", skillMetadata);
        json.put("accepted", true);
        json.put("message", "Success: Fetched Skill's Metadata");
        return new ServiceResponse(json);
    }

}
