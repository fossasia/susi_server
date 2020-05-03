/**
 * GetAllLanguages
 * Created by chetankaushik on 24/06/17.
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
import ai.susi.tools.IO;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Created by chetankaushik on 24/06/17.
 * Adds API Endpoint to get all languages for a group.
 * BASE ROLE Required is ANONYMOUS
 * Accepts 2 GET parameters, Model Name and Group Name
 * http://127.0.0.1:4000/cms/getAllLanguages.json?group=assistants
 */
public class GetAllLanguages  extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -7872551914189898030L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getAllLanguages.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        Path model = IO.resolvePath(DAO.model_watch_dir.toPath(), model_name);
        String group_name = call.get("group", null);
        if (group_name == null) {
            File group = IO.resolvePath(model, "Knowledge").toFile();
            JSONObject json = new JSONObject(true);
            json.put("accepted", false);
            String[] languages = group.list((current, name) -> new File(current, name).isDirectory());
            assert languages != null;
            JSONArray languagesArray = new JSONArray(languages);
            json.put("languagesArray", languagesArray);
            json.put("accepted", true);
            return new ServiceResponse(json);
        }
        else {
            if (group_name.equals("All"))
            {
                JSONObject json = new JSONObject(true);
                json.put("accepted", false);
                ArrayList<String> languages = new ArrayList<>();
                        String[] group_names = model.toFile().list((current, name) -> new File(current, name).isDirectory());
                assert group_names != null;
                for (String temp_group_name : group_names) {
                    File group = IO.resolvePath(model, temp_group_name).toFile();
                        group.list((file, s) -> {
                            try {
                                Boolean accepted = Objects.requireNonNull(new File(file, s).list()).length > 1;
                                if (accepted) {
                                    if (!languages.contains(s)) {
                                        languages.add(s);
                                    }
                                }
                                return accepted;
                            }
                            catch (Exception e)
                            {
                                return false;
                            }
                        });
                }
                JSONArray languagesArray = new JSONArray(languages);
                json.put("languagesArray", languagesArray);
                json.put("accepted", true);
                return new ServiceResponse(json);
            }
            else {
                File group = IO.resolvePath(model, group_name).toFile();
                JSONObject json = new JSONObject(true);
                json.put("accepted", false);
                String[] languages = group.list((file, s) -> {
                    try {
                        return Objects.requireNonNull(new File(file, s).list()).length > 1;
                    }
                    catch (Exception e){
                        return false;
                    }
                });
                assert languages != null;
                JSONArray languagesArray = new JSONArray(languages);
                json.put("languagesArray", languagesArray);
                json.put("accepted", true);
                return new ServiceResponse(json);
            }
        }
    }
}
