package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;


/**
 * Created by chetankaushik on 12/08/17.
 * API Endpoint to get all skills by an author.
 * http://127.0.0.1:4000/cms/getSkillsByAuthor.json?author=chetan%20kaushik
 */
public class GetSkillsByAuthor extends AbstractAPIHandler implements APIHandler {

    JSONObject result ;
    private static final long serialVersionUID = 3446536703362688060L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillsByAuthor.json";
    }


    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {
        result= new JSONObject();
        try {
            DAO.susi.observe(); // get a database update
        } catch (IOException e) {
            DAO.log(e.getMessage());
        }

        String author = call.get("author",null);

        if(author == null){
            JSONObject json =  new JSONObject();
            json.put("accepted",false);
            json.put("message","Author name not given");
            return new ServiceResponse(json);
        }

        for (Map.Entry<SusiSkill.ID, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
            SusiSkill.ID skill = entry.getKey();
            if ((entry.getValue().getAuthor()!=null) && entry.getValue().getAuthor().contains(author.toLowerCase())) {
                result.put(result.length()+"",skill);
            }
        }
        result.put("accepted",true);
        result.put("message","Success");
        return new ServiceResponse(result);

    }

}
