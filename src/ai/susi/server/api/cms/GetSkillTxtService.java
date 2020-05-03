/**
 *  GetSkillTxtService
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

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Servlet to load an skill from the skill database
 * i.e.
 * http://localhost:4000/cms/getSkill.txt
 * http://localhost:4000/cms/getSkill.txt?model=general&group=Knowledge&language=en&skill=wikipedia
 */
public class GetSkillTxtService extends AbstractAPIHandler implements APIHandler {
    
    private static final long serialVersionUID = 18344224L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkill.txt";
    }
    
    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        File skill = SkillQuery.getParser("wikipedia").parse(call).getSkillFile();

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        
        try {
            assert skill != null;
            String content = new String(Files.readAllBytes(skill.toPath()));
            json.put("content", content);
            json.put("accepted", true);
            return new ServiceResponse(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ServiceResponse(json);
    }
}
