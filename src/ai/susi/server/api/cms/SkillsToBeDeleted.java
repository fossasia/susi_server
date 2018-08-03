/**
 *  SkillsToBeDeleted
 *  Copyright 19.08.2017 by Chetan Kaushik , @dynamitechetan
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.Collection;

/**
 * Created by chetankaushik on 19/08/17.
 * This API endpoint returns the list of all the skills which are scheduled to be deleted.
 * http://127.0.0.1:4000/cms/skillsToBeDeleted.json
 */
public class SkillsToBeDeleted extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -8691003678852307876L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ADMIN; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/skillsToBeDeleted.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.deleted_skill_dir, model_name);
        JSONObject json = new JSONObject();

        Collection files = FileUtils.listFiles(
                new File(DAO.deleted_skill_dir.getPath()),
                TrueFileFilter.INSTANCE,
                TrueFileFilter.TRUE
        );

        JSONArray jsArray = new JSONArray(files);

        json.put("skills", jsArray);
        json.put("accepted", true);
        json.put("message","Success: Fetched skill list");
        return new ServiceResponse(json);

    }


}
