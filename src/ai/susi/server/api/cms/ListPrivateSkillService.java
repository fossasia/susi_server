package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * Necessary parameters : access_token, example:
 * http://localhost:4000/cms/getPrivateSkillList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 * Other parameter, (not necessary) search:
 * http://localhost:4000/cms/getPrivateSkillList.json?access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3&search=test_bot
 */

public class ListPrivateSkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -5000658108778105134L;

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
        return "/cms/getPrivateSkillList.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization,
            final JsonObjectWithDefault permissions) throws APIException {

        if (authorization.getIdentity() == null) {
            throw new APIException(422, "Bad access token.");
        }

        JsonTray chatbot = DAO.chatbot;
        JSONObject botDetailsObject = chatbot.toJSON();
        JSONObject keysObject = new JSONObject();
        JSONObject groupObject = new JSONObject();
        JSONObject languageObject = new JSONObject();
        List<JSONObject> botList = new ArrayList<JSONObject>();
        JSONObject result = new JSONObject();

        Iterator Key = botDetailsObject.keys();
        List<String> keysList = new ArrayList<String>();

        while (Key.hasNext()) {
            String key = (String) Key.next();
            keysList.add(key);
        }

        for (String key_name : keysList) {
            keysObject = botDetailsObject.getJSONObject(key_name);
            Iterator groupNames = keysObject.keys();
            List<String> groupnameKeysList = new ArrayList<String>();

            while (groupNames.hasNext()) {
                String key = (String) groupNames.next();
                groupnameKeysList.add(key);
            }

            for (String group_name : groupnameKeysList) {
                groupObject = keysObject.getJSONObject(group_name);
                Iterator languageNames = groupObject.keys();
                List<String> languagenamesKeysList = new ArrayList<String>();

                while (languageNames.hasNext()) {
                    String key = (String) languageNames.next();
                    languagenamesKeysList.add(key);
                }

                for (String language_name : languagenamesKeysList) {
                    languageObject = groupObject.getJSONObject(language_name);
                    if (call.get("search", null) != null) {
                        String bot_name = call.get("search", null);
                        if(languageObject.has(bot_name)){
                            JSONObject botDetails = languageObject.getJSONObject(bot_name);
                            botDetails.put("name", bot_name);
                            botDetails.put("language", language_name);
                            botDetails.put("group", group_name);
                            botDetails.put("key", key_name);
                            botList.add(botDetails);
                        }
                    } else {
                        Iterator botNames = languageObject.keys();
                        List<String> botnamesKeysList = new ArrayList<String>();

                        while (botNames.hasNext()) {
                            String key = (String) botNames.next();
                            botnamesKeysList.add(key);
                        }

                        for (String bot_name : botnamesKeysList) {
                            JSONObject botDetails = languageObject.getJSONObject(bot_name);
                            botDetails.put("name", bot_name);
                            botDetails.put("language", language_name);
                            botDetails.put("group", group_name);
                            botDetails.put("key", key_name);
                            botList.add(botDetails);
                        }
                    }
                }
            }
        }

        try {
            result.put("chatbots", botList);
            result.put("accepted", true);
            result.put("message", "Success: Fetched all Private Skills");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed to fetch the requested list!");
        }

    }
}
