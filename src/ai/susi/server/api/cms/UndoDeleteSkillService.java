/**
 *  UndoDeleteSkillService
 *  Copyright 23/08/17 by Chetan Kaushik, @dynamitechetan
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
import ai.susi.SkillTransactions;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.skillqueryparser.SkillQuery;
import ai.susi.tools.skillqueryparser.SkillQueryParser;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * Created by chetankaushik on 23/08/17.
 * 127.0.0.1:4000/cms/undoDeleteSkill.json?skill=wikipedia
 * When someone deletes a skill then it will move a folder delete_skills_dir.
 * This API Endpoint moves the deleted skill from the  delete_skills_dir to susi_skill_data repository.
 */
public class UndoDeleteSkillService  extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1755374387315534691L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/undoDeleteSkill.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        SkillQuery skillQuery = SkillQueryParser.Builder.getInstance()
                .modelPath(DAO.deleted_skill_dir.toPath())
                .build()
                .parse(call);
        String skill_name = skillQuery.getSkill();
        File skill = skillQuery.getSkillFile();
        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        String path = skill.getPath();
        path = path.replace(DAO.deleted_skill_dir.getPath(),"");

        if (skill.exists()) {
            File file = new File(DAO.model_watch_dir.getPath()+path);
            file.getParentFile().mkdirs();
            if(skill.renameTo(file)){
                Boolean changed = file.setLastModified(System.currentTimeMillis());
                System.out.print(changed);
                System.out.println("Skill moved successfully!");
            }else{
                System.out.println("Skill failed to move!");
            }

            json.put("message","Restored "+ skill_name + " successfully!");

            //Add to git
            SkillTransactions.addAndPushCommit(false, "Undo Delete " + skill_name, !rights.getIdentity().isAnonymous() ? rights.getIdentity().getName() : "anonymous@");
            json.put("accepted", true);
            json.put("message", "Undo Deletion of " + skill_name + " aborted!");

        } else {
            json.put("message", "Cannot find '" + skill + "' ('" + skill.getAbsolutePath() + "')");
        }
        return new ServiceResponse(json);
    }

}
