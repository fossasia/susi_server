/**
 * HistorySkillService
 * Created by chetankaushik on 09/06/17.
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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;


/**
 * This Service Adds a API Endpoint to return history of an skill
 * This accepts 4 parameters: - Model, Group, Language and Skill Name
 * Can be tested on : -
 * http://127.0.0.1:4000/cms/getSkillHistory.json?model=general&group=Knowledge&language=en&skill=bitcoin
 */
public class HistorySkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 6976713190365750955L;
    JSONObject commit;
    Boolean success=false;

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
        return "/cms/getSkillHistory.json";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doGet(request, response);
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        File skill = SkillQuery.getParser("wikipedia").parse(call).getSkillFile();
        JSONArray commitsArray;
        commitsArray = new JSONArray();
        String path = skill.getPath().replace(DAO.model_watch_dir.toString(), "models");
        //Add to git
        try {
            Iterable<RevCommit> logs = SkillTransactions.getGitLog(false, path);
            int i = 0;
            for (RevCommit rev : logs) {
                commit = new JSONObject();
                commit.put("commitRev", rev);
                commit.put("commitName", rev.getName());
                commit.put("commitID", rev.getId().getName());
                commit.put("commit_message", rev.getShortMessage());
                commit.put("author",rev.getAuthorIdent().getName());
                commit.put("commitDate",rev.getAuthorIdent().getWhen());
                commit.put("author_mail",rev.getAuthorIdent().getEmailAddress());
                commitsArray.put(i, commit);
                i++;
            }
            success=true;

        } catch (IOException | GitAPIException e) {
            e.printStackTrace();
            success=false;

        }
        if(commitsArray.length()==0){
            success=false;
        }
        JSONObject result = new JSONObject();
        result.put("commits",commitsArray);
        result.put("accepted",success);
        return new ServiceResponse(result);
    }
}
