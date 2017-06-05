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
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

/**
 * Servlet to load an expert from the exepert database
 * 
 */
public class ExpertGetTxtService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 18344224L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getexpert.txt";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        
        
        File models = new File(DAO.model_watch_dir, "models");
        
        String model_name = call.get("model", "general");
        File model = new File(models, model_name);
        String group_name = call.get("group", "knowledge");
        File group = new File(model, group_name);
        String language_name = call.get("language", "en");
        File language = new File(group, language_name);
        String expert_name = call.get("expert", "wikipedia");
        File expert = new File(language, expert_name + ".txt");
        
        try {
            String content = new String(Files.readAllBytes(expert.toPath()));
            return new ServiceResponse(content);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
}
