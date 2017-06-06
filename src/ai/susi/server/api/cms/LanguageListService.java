/**
 *  GetExpertTxtService
 *  Copyright 28.05.2017 by Michael Peter Christen, @0rb1t3r
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

import org.json.JSONArray;
import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.BaseUserRole;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to load an expert from the expert database
 * i.e.
 * http://localhost:4000/cms/getLanguage.txt
 * http://localhost:4000/cms/getexpert.txt?model=general&group=knowledge&expert=wikipedia
 */
public class LanguageListService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 18344224L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getLanguage.txt";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        String model_name = call.get("model", "general");
        File model = new File(DAO.model_watch_dir, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String expert_name = call.get("expert", "wikipedia");

        String[] languages = group.list((current, name) -> new File(current, name).isDirectory());
        ArrayList<String> languageList = new ArrayList<>();
        for(String languageName: languages) {
            File language = new File(group, languageName);
            File[] listOfExperts = language.listFiles();
            for( File expert: listOfExperts){
                if(expert.getName().equals(expert_name+".txt")){
                    languageList.add(language.getName());
                    break;
                }
            }
        }
        JSONArray languageJsonArray = new JSONArray(languageList);
        return new ServiceResponse(languageJsonArray);
    }
}
