package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonFile;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
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

    private static final long serialVersionUID = -8691003678852307876L;

    @Override
    public BaseUserRole getMinimalBaseUserRole() { return BaseUserRole.ANONYMOUS; }

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
        JSONObject success = new JSONObject();
        success.put("success",false);
        JSONObject allUsers ;
        allUsers = DAO.group;
        String model_name = call.get("group", null);
        if(model_name==null){
            return new ServiceResponse(success);
        }

        else{
            Set<?> s =  allUsers.keySet();

            Iterator<?> i = s.iterator();
            do{
                String k = i.next().toString();

                if(!model_name.equals(k)){
                    return new ServiceResponse(success);
                }

            }while(i.hasNext());


            return new ServiceResponse(allUsers);

        }
            
    }

}
