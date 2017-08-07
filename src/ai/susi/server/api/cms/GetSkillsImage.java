/**
 *  GetSkillImage
 *  Copyright 20.07.2017 by Chetan Kaushik , @dynamitechetan
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
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 This Servlet gives a API Endpoint to list images for all the Skills given its model, group and language.
 Can be tested on 127.0.0.1:4000/cms/getSkillImage.json
 */
public class GetSkillsImage extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 692253797031953182L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
            return "/cms/getSkillImage.json";
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

        JSONObject images = new JSONObject(true);
        for (Map.Entry<String, String> entry: DAO.susi.getSkillImage().entrySet()) {
            String path = entry.getKey();
            if ((model.length() == 0 || path.indexOf("/" + model + "/") > 0) &&
                    (group.length() == 0 || path.indexOf("/" + group + "/") > 0) &&
                    (language.length() == 0 || path.indexOf("/" + language + "/") > 0)) {
                images.put(path, entry.getValue());
            }
        }

        JSONObject json = new JSONObject(true)
                .put("model", model)
                .put("group", group)
                .put("language", language)
                .put("image",images);
        json.put("accepted", true);
        json.put("message", "Success: Fetched Image urls");
        return new ServiceResponse(json);
    }

}
