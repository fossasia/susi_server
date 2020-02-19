/**
 *  ProfileDetailsService
 *  Copyright by @Akshat-Jain on 10/06/18.
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
import ai.susi.server.Authorization;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;

/**
 * This endpoint returns all the ratings by a User on different Skills, along with their timestamp
 * http://localhost:4000/cms/getProfileDetails.json?access_token=P8iKeYmAPaYDxHUKhSu2l9n6XEY6Dm
 */
public class ProfileDetailsService extends AbstractAPIHandler implements APIHandler {

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getProfileDetails.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

    JsonTray fiveStarSkillRating = DAO.fiveStarSkillRating;
    JSONObject result = new JSONObject();
    JSONObject modelObject = new JSONObject();
    JSONObject groupObject = new JSONObject();
    JSONObject languageObject = new JSONObject();
    JSONArray skillnameArray = new JSONArray();
    JSONArray skillRating = new JSONArray();

    if (authorization.getIdentity() == null) {
        throw new APIException(400, "Specified user data not found, ensure you are logged in");
    }

    String email = authorization.getIdentity().getName();

    for(String model_name : fiveStarSkillRating.keys())
    {
        modelObject = fiveStarSkillRating.getJSONObject(model_name);
        Iterator groupNames = modelObject.keys();
        List<String> groupnameKeysList = new ArrayList<String>();
        
        while(groupNames.hasNext()) {
            String key = (String) groupNames.next();
            groupnameKeysList.add(key);
        }
        
        for(String group_name : groupnameKeysList)
        {
            groupObject = modelObject.getJSONObject(group_name);
            Iterator languageNames = groupObject.keys();
            List<String> languagenameKeysList = new ArrayList<String>();
            
            while(languageNames.hasNext()) {
                String key = (String) languageNames.next();
                languagenameKeysList.add(key);
            }

            for(String language_name : languagenameKeysList)
            {
                languageObject = groupObject.getJSONObject(language_name);
                Iterator skillNames = languageObject.keys();
                List<String> skillnamesKeysList = new ArrayList<String>();
                
                while(skillNames.hasNext()) {
                    String key = (String) skillNames.next();
                    skillnamesKeysList.add(key);
                }

                for(String skill_name : skillnamesKeysList)
                {
                    skillnameArray = languageObject.getJSONArray(skill_name);

                    for(int i=0; i<skillnameArray.length(); i++) {
                        String jsonEmail = skillnameArray.getJSONObject(i).get("email").toString();
                        if(jsonEmail.equals(email)) {
                            JSONObject userSkillData = new JSONObject();
                            JSONObject userSkillRatings = new JSONObject();
                            int stars = Integer.parseInt(skillnameArray.getJSONObject(i).get("stars").toString());
                            if(stars > 0) {
                                String timestamp = skillnameArray.getJSONObject(i).get("timestamp").toString();
                                userSkillData.put("stars",stars);
                                userSkillData.put("timestamp",timestamp);
                                userSkillData.put("group", group_name);
                                userSkillData.put("language", language_name);
                                userSkillRatings.put(skill_name,userSkillData);
                                if(result.has(jsonEmail)) {
                                    skillRating = result.getJSONArray(jsonEmail);
                                }
                                skillRating.put(userSkillRatings);
                                result.put("rated_skills", skillRating);
                            }
                        }
                    }
                }
            }
        }
    }

    if(result.length()==0) {
        result.put("accepted", false);
        result.put("message", "User has not rated any Skills yet.");
        return new ServiceResponse(result);
    }

    result.put("accepted", true);
    result.put("message", "User ratings fetched.");
    return new ServiceResponse(result);

    }
}
