package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.DateParser;

import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 *  Necessary parameters : access_token, example:
 *  http://localhost:4000/cms/getPrivateDraftSkillList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
 
public class ListPrivateDraftSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 4985932190918215684L;
    
    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.OPERATOR;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getPrivateDraftSkillList.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {
        
        if (authorization.getIdentity() == null) {
          throw new APIException(422, "Bad access token.");
        }

        JSONObject result = new JSONObject();
        List<JSONObject> draftBotList = new ArrayList<JSONObject>();
        Collection<ClientIdentity> authorized = DAO.getAuthorizedClients();

        for (Client client : authorized) {
          String email = client.toString().substring(6);
          JSONObject json = client.toJSON();
          ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, client.getName());
          Authorization userAuthorization = DAO.getAuthorization(identity);
          Map<String, DAO.Draft> map = DAO.readDrafts(userAuthorization.getIdentity());
          JSONObject drafts = new JSONObject();

          for (Map.Entry<String, DAO.Draft> entry: map.entrySet()) {
            JSONObject val = new JSONObject();
            val.put("object", entry.getValue().getObject());
            val.put("created", DateParser.iso8601Format.format(entry.getValue().getCreated()));
            val.put("modified", DateParser.iso8601Format.format(entry.getValue().getModified()));
            drafts.put(entry.getKey(), val);
          }
          Iterator<?> keys = drafts.keySet().iterator();
          while(keys.hasNext()) {
            String key = (String)keys.next();
            if (drafts.get(key) instanceof JSONObject) {
              JSONObject draft = new JSONObject(drafts.get(key).toString());
              draft.put("id", key);
              draft.put("email", email);
              draftBotList.add(draft);
            }
          }
        }
        result.put("draftBots", draftBotList);

        return new ServiceResponse(result);
    }
}
