/**
 *  DeleteGroupService
 *  Copyright 27/6/17 by Dravit Lochan, @DravitLochan
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

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by dravit on 27/6/17.
 * http://127.0.0.1:4000/aaa/deleteGroup.json?group=groupName
 */
public class DeleteGroupService extends AbstractAPIHandler implements APIHandler{

    private static final long serialVersionUID = -6460356959547369940L;

    @Override
    public String getAPIPath() {
        return "/aaa/deleteGroup.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {
        JSONObject result = new JSONObject();
        String groupName = post.get("group", null);

        if( groupName!=null ) {
            if( DAO.group.has(groupName)) {
                DAO.group.remove(groupName);
                result.put("accepted", true);
		result.put("message","Group deleted successfully");
            } else {
                result.put("accepted", false);
		result.put("message", "Group does not exists");
            }
            return new ServiceResponse(result);
        } else {
	    result.put("accepted", false);
	    result.put("message", "Bad call, group name parameter not specified");
        }
        return new ServiceResponse(result);
    }
}
