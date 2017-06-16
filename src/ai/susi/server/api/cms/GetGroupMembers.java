package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonFile;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import com.sun.org.apache.xpath.internal.operations.Bool;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by chetankaushik on 15/06/17.
 */
public class GetGroupMembers extends AbstractAPIHandler implements APIHandler {


    private static final long serialVersionUID = 5747506850176916431L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() {
        return BaseUserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getGroupMembers.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        Boolean foundUser;
        JSONObject success = new JSONObject();
        success.put("success", false);
        JSONObject allUsers;
        allUsers = DAO.group;
        String model_name = call.get("group", null);
        foundUser = false;
        if (model_name == null) {
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
                details.put("success", true);
                return new ServiceResponse(details);

            } else
                return new ServiceResponse(success);


        }

    }

}
