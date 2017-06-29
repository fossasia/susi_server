/**
 *  CreateGroups
 *  Copyright 22.06.2017 by saurabh
 *  Contributors: Saurabh Jain @saurabhjn76, Chetan Kaushik @dynamitechetan
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
 *
 */

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.BaseUserRole;
import ai.susi.server.ServiceResponse;
import ai.susi.server.Query;
import ai.susi.server.Authorization;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This Servlets creates a group with default baserole as anonymous.
 * It takes 1 parameter group and other optional parameter role.
 * Can be tested on
 * http://127.0.0.1:4000/aaa/createGroup.json?group=groupName&role=admin&permission=<comma separated values>
 *
 */
public class CreateGroupService extends AbstractAPIHandler implements APIHandler {


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
        return "/aaa/createGroup.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        JSONObject result = new JSONObject();
        String groupName = call.get("group", null);
        String grouppBaseRole = call.get("role","anonymous");
        String permission = call.get("permissions","");
        if( groupName!=null ) {
            if( DAO.group.has(groupName)) {
                result.put("message", "Group already exists");
                result.put("accepted", false);
            } else {
                JSONObject groupDetail = new JSONObject();
                groupDetail.put("group_members", new JSONObject());
                groupDetail.put("groupBaseRole", grouppBaseRole);
                ArrayList aList= new ArrayList(Arrays.asList(permission.split(",")));
                JSONArray permissionArray = new JSONArray();
                for(int i=0;i<aList.size();i++)
                {
                    permissionArray.put(i,aList.get(i));
                }
                groupDetail.put("permission", permissionArray);
                result.put("message", "Group created successfully");
                result.put("accepted", true);
                DAO.group.put(groupName,groupDetail,rights.getIdentity().isPersistent());
            }
            return new ServiceResponse(result);
        } else {
            result.put("message", "Bad call, group name parameter not specified");
            result.put("accepted", false);
        }
        return new ServiceResponse(result);
    }

}

