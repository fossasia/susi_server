package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by chetankaushik on 15/06/17.
 * This servlet returns details of the group whose name is passed in the URL Get parameters.
 * This accepts one GET parameter, which is the group name you want to search for.
 * This can be tested on http://127.0.0.1:4000/cms/getGroupDetails.json?group={groupName}
 *
 */
public class GetGroupDetails extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 5747506850176916431L;

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
        return "/cms/getGroupDetails.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        Boolean foundUser;
        JSONObject success = new JSONObject();
        success.put("accepted", false);
        success.put("message", "Error: Unable to process request");
        JSONObject allUsers;
        allUsers = DAO.group.toJSON();
        String model_name = call.get("group", null);
        foundUser = false;
        if (model_name == null) {
            success.put("message", "Error: Bad call group parameter not specified");
            return new ServiceResponse(success);
        } else {
            //Searching for keys in groups.json
            Set<?> s = allUsers.keySet();

            Iterator<?> i = s.iterator();
            do {
                String k = i.next().toString();

                if (model_name.equals(k)) {
                    foundUser = true;
                    break;
                }


            } while (i.hasNext());

            if (foundUser) {
                JSONObject details = new JSONObject();
                details = allUsers.getJSONObject(model_name);
                details.put("accepted", true);
                details.put("message", "Success: Request processed");
                return new ServiceResponse(details);

            } else{
                success.put("message", "user not found");
                return new ServiceResponse(success);
            }


        }

    }

}
