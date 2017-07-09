/**
 *  ListSettingsService
 *  Copyright 14.06.2017 by Dravit Lochan, @DravitLochan
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
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Created by dravit on 14/6/17.
 * Servlet to get list of all the files present in data/settings directory
 * test locally at http://127.0.0.1:4000/aaa/getAllFiles
 */
public class ListSettingsService extends AbstractAPIHandler implements APIHandler{
    @Override
    public String getAPIPath() {
        return "/aaa/listSettings.json";
    }

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        String path = DAO.data_dir.getPath()+"/settings/";
        File settings = new File(path);
        String[] files = settings.list();
        JSONObject result = new JSONObject(true);
        JSONArray fileArray = new JSONArray(files);
        result.put("accepted", true);
        result.put("message", "Success: Fetched all files");
        result.put("files", fileArray);
        return new ServiceResponse(result);
    }
}
