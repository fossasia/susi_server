/**
 *  APIKeysService
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
import ai.susi.server.Authorization;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * This Servlet gives a API Endpoint to add, modify and delete Skill Slider
 * details used by SUSI. It requires user role to be ADMIN or above ADMIN
 * example: For adding/uploading new image: Necessary parameters : access_token,
 * redirect_link(for redirect on click), image_name, Optional parameter: info
 * http://localhost:4000/cms/skillSlideshow.json?access_token=go2ijgk5ijkmViAac2bifng3uthdZ&redirect_link=https://susi.ai&info=Metadata&image_name=NAME_OF_IMAGE
 * For deleting, Necessary parameters: redirect_link -> string, deleteSlide=true
 * http://localhost:4000/aaa/apiKeys.json?redirect_link=https://susi.ai&deleteSlide=true
 */

public class SkillSlideshowService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5000658108778105134L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/skillSlideshow.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization,
            final JsonObjectWithDefault permissions) throws APIException {
        if (call.get("redirect_link", null) == null || call.get("image_name", null) == null) {
            throw new APIException(400, "Bad Request. No enough parameter present");
        }

        String redirectLink = call.get("redirect_link", null);
        String imageName = call.get("image_name", null);
        String info = call.get("info", null);
        boolean deleteSlide = call.get("deleteSlide", false);
        JsonTray skillSlideshow = DAO.skillSlideshow;
        JSONObject result = new JSONObject();
        JSONObject skillSlideshowObj = new JSONObject();
        if (skillSlideshow.has("slideshow")) {
            skillSlideshowObj = skillSlideshow.getJSONObject("slideshow");
        }
        if (!deleteSlide) {
            try {
                JSONObject slideObj = new JSONObject();
                slideObj.put("image_name", imageName);
                slideObj.put("info", info);
                skillSlideshowObj.put(redirectLink, slideObj);
                skillSlideshow.put("slideshow", skillSlideshowObj, true);
                result.put("accepted", true);
                result.put("message", "Added new Slide " + call.get("redirect_link") + " successfully !");
                return new ServiceResponse(result);
            } catch (Exception e) {
                throw new APIException(500,
                        "Failed : Unable to add slide with path " + call.get("redirect_link") + " !");
            }
        } else {
            try {
                skillSlideshowObj.remove(redirectLink);
                skillSlideshow.put("slideshow", skillSlideshowObj, true);
                result.put("accepted", true);
                result.put("message", "Removed Slide with path " + call.get("redirect_link") + " successfully !");
                return new ServiceResponse(result);
            } catch (Exception e) {
                throw new APIException(501,
                        "Failed to remove Slide: " + call.get("redirect_link") + " doesn't exists!");
            }
        }
    }
}
