/**
 * GetSkillDataUrl
 * Created by chetankaushik on 17/09/17.
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
import org.eclipse.jgit.lib.Repository;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class GetSkillDataUrl extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -5686523277755750923L;


    @Override
    public String getAPIPath() {
        return "/cms/getSkillDataUrl.json";
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
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        JSONObject json = new JSONObject();
        try {
            //get the repository
            Repository repository = DAO.getRepository();
            //get the repository path
            String url = repository.getConfig().getString("remote", "origin", "url");
            json.put("url", url);
            //set the githubrawusercontent path
            String rawUserContenturl = url.replace("github.com", "raw.githubusercontent.com");
            rawUserContenturl = rawUserContenturl.replace(".git", "");
            //get current branch name
            String currentBranch = repository.getBranch();
            //append branch name in rawUserContenturl
            rawUserContenturl = rawUserContenturl + "/" + currentBranch;
            json.put("githubusercontent", rawUserContenturl);
            json.put("accepted",true);
        } catch (IOException e) {
            e.printStackTrace();
            json.put("accepted",false);
        }

        return new ServiceResponse(json);
    }

}
