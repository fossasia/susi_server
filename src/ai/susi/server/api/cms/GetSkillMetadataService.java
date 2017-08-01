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
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 This Servlet gives a API Endpoint to list meta for a  Skill. Given its model, group and language and skill.
 Can be tested on 127.0.0.1:4000/cms/getSkillMetadata.json
 */
public class GetSkillMetadataService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 3446536703362688060L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
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
        for (Map.Entry<String, String> entry : DAO.susi.getSkillDescriptions().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0)
                    && (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("descriptions", entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : DAO.susi.getSkillImage().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("image", entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : DAO.susi.getAuthors().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("author", entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : DAO.susi.getAuthorsUrl().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("author_url", entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : DAO.susi.getDeveloperPrivacyPolicies().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("developer_privacy_policy", entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : DAO.susi.getSkillNames().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("skill_name", entry.getValue());
            }
        }
        for (Map.Entry<String, String> entry : DAO.susi.getTermsOfUse().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("terms_of_use", entry.getValue());
            }
        }
        for (Map.Entry<String, Boolean> entry : DAO.susi.getDynamicContent().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                skillMetadata.put("dynamic_content", entry.getValue());
            }
        }
        JSONObject examples = new JSONObject(true);
        for (Map.Entry<String, Set<String>> entry: DAO.susi.getSkillExamples().entrySet()) {
            String path = entry.getKey();
            if ((path.indexOf("/" + model + "/") > 0) &&
                    (path.indexOf("/" + group + "/") > 0) &&
                    (path.indexOf("/" + language + "/") > 0) &&
                    (path.indexOf("/" + skill + ".txt") > 0)) {
                examples.put(path, entry.getValue());
            }
        }
        skillMetadata.put("examples", examples);
        JSONObject json = new JSONObject(true);
        json.put("skill_metadata", skillMetadata);
        json.put("accepted", true);
        json.put("message", "Success: Fetched Skill's Metadata");
        return new ServiceResponse(json);
    }

}
