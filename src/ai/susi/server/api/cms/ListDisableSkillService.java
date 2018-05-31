/**
 *  ListDisableSkillService
 *  Copyright by saurabh on 21/8/17.
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
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Accounting;
import ai.susi.server.Authorization;
import ai.susi.server.UserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to list all the disable skill  for a user in cms
 * test locally at http://127.0.0.1:4000/cms/listDisableSkill.json
 */
public class ListDisableSkillService extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = -702759446567881858L;

    @Override
    public String getAPIPath() {
        return "/cms/listDisableSkill.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        if (authorization.getIdentity() == null) {
            throw new APIException(400, "Specified User disabled skill not found, ensure you are logged in");
        } else {
            Accounting accounting = DAO.getAccounting(authorization.getIdentity());
            JSONObject result = new JSONObject();
            if (accounting.getJSON().has("disabledSkills")) {
                result.put("disabledSkills",accounting.getJSON().get("disabledSkills"));
            } else {
                result.put("disabledSkills", new JSONObject());
            }
            result.put("accepted", true);
            result.put("message", "Success: Showing User disabled skills");
            return new ServiceResponse(result);
        }

    }
}
