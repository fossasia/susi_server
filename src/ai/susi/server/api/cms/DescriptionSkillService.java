/**
 *  DescriptionSkillService
 *  Copyright 18.06.2016 by Saurabh Jain , @saurabhjn76
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
 This Servlet gives a API Endpoint to list descriptions for all the Skills given its model, group and language.
 Can be tested on 127.0.0.1:4000/cms/getDescriptionSkill.json
 */
public class DescriptionSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 4175356383695207511L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getDescriptionSkill.json";
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

        JSONObject descriptions = new JSONObject(true);
            for (Map.Entry<String, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
                String path = entry.getKey();
                if ((model.length() == 0 || path.indexOf("/" + model + "/") > 0) &&
                        (group.length() == 0 || path.indexOf("/" + group + "/") > 0) &&
                        (language.length() == 0 || path.indexOf("/" + language + "/") > 0) &&
                        (skill.length() == 0 || path.indexOf("/" + skill + ".txt") > 0)) {
                    descriptions.put(path, entry.getValue().getDescription());
                }
            }

            JSONObject json = new JSONObject(true)
                    .put("model", model)
                    .put("group", group)
                    .put("language", language)
                    .put("descriptions", descriptions);
            if (descriptions.length() != 0) {
                json.put("accepted", true);
                json.put("message", "Sucess: Fetched descriptions");
            } else {
                json.put("accepted", false);
                json.put("message", "Error: Can't find description");
            }
        return new ServiceResponse(json);
    }

}
