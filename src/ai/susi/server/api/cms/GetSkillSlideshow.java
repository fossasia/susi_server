/**
 *  GetApiKeys
 *  Copyright by Shubham Gupta @fragm3
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
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;

import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

/**
 * This Servlet gives a API Endpoint to fetch Skill Slideshow used by SUSI
 * homepage. It requires user role to be ANONYMOUS or above ANONYMOUS example:
 * http://localhost:4000/cms/getSkillSlideshow.json
 */

public class GetSkillSlideshow extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5000658108778105134L;

    @Override
    public String getAPIPath() {
        return "/cms/getSkillSlideshow.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {

        JsonTray skillSlideshow = DAO.skillSlideshow;
        JSONObject skillSlideshowObj = skillSlideshow.getJSONObject("slideshow");
        JSONObject result = new JSONObject();

        try {
            result.put("accepted", true);
            result.put("slideshow", skillSlideshowObj);
            result.put("message", "Success : Fetched all Skills Slides!");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed : Unable to fetch Skills Slides!");
        }
    }
}
