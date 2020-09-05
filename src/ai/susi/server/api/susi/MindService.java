/**
 *  SusiService
 *  Copyright 29.06.2015 by Michael Peter Christen, @0rb1t3r
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

package ai.susi.server.api.susi;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiFace;
import ai.susi.mind.SusiLanguage;
import ai.susi.mind.SusiMind;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authorization;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import ai.susi.tools.EtherpadClient;

import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

public class MindService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303098111L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    public String getAPIPath() {
        return "/susi/mind.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization user, final JsonObjectWithDefault permissions) throws APIException {

        DAO.observe(); // get a database update
        String dream = post.get("dream", null);
        JSONObject json = null;

        if (dream != null && dream.length() > 0) {
            if ("susi".equals(dream) && EtherpadClient.localEtherpadExists()) {
                SusiMind dreamMind = SusiFace.getLocalSusiDream();
                if (dreamMind != null) json = dreamMind.getMind();
            } else {
                SusiMind dreamMind = SusiFace.getDream(dream, SusiLanguage.unknown, true);
                if (dreamMind != null) json = dreamMind.getMind();
            }
        }

        if (json == null) json = DAO.susi.getMind();
        return new ServiceResponse(json);
    }

}
