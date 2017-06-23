/**
 *  CreateGroups
 *  Copyright 22.06.2017 by saurabh
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

/**
 * This Servlets returns all the group details of all the groups created.
 * It takes one parameter group
 * Can be tested on
 * http://127.0.0.1:4000/cms/createGroups.json?group=groupName
 *
 */
public class CreateGroups extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -742269505564698987L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/createGroups.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        JSONObject result = new JSONObject();
        String group_name = call.get("group", null);
        if( DAO.group.has(group_name)) {
            result.put("success", false);
            result.put("message", "Group already exists");
            return new ServiceResponse(result);
        } else {
            DAO.group.put(group_name,new JSONObject(),rights.getIdentity().isPersistent());
            result.put("success", true);
            result.put("message", "Group created successfully");
            return new ServiceResponse(result);
        }
    }

}

