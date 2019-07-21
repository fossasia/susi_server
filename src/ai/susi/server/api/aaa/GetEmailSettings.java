package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;
import javax.servlet.http.HttpServletResponse;

import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

/**
 * This Servlet gives a API Endpoint to fetch Mail Settings used by SUSI
 * homepage. It requires user role to be ANONYMOUS or above ANONYMOUS example:
 * http://localhost:4000/aaa/getEmailSettings.json
 */

public class GetEmailSettings extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5000658108778105134L;

    @Override
    public String getAPIPath() {
        return "/aaa/getEmailSettings.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) throws APIException {

        String email = DAO.getConfig("smtp.sender.email", "");
        String name = DAO.getConfig("smtp.sender.displayname", "");
        String type = DAO.getConfig("mail.type", "");
        String sendgridToken = DAO.getConfig("mail.sendgridtoken", "");
        String frontendUrl = DAO.getConfig("mail.frontendurl", "");

        String trustselfsignedcerts = DAO.getConfig("smtp.trustselfsignedcerts", "");
        String encryption = DAO.getConfig("smtp.host.encryption", "");
        String port = DAO.getConfig("smtp.host.port", "");
        String smtpUserName = DAO.getConfig("smtp.sender.username", "");
        String smtpPassword = DAO.getConfig("smtp.sender.password", "");
        String smtpHost = DAO.getConfig("smtp.host.name", "");
        JSONObject result = new JSONObject();
        JSONObject settingsObj = new JSONObject();

        settingsObj.put("email", email);
        settingsObj.put("name", name);
        settingsObj.put("type", type);
        settingsObj.put("sendgrid_token", sendgridToken);
        settingsObj.put("frontend_url", frontendUrl);
        settingsObj.put("trustselfsignedcerts", trustselfsignedcerts);
        settingsObj.put("encryption", encryption);
        settingsObj.put("port", port);
        settingsObj.put("smtpUserName", smtpUserName);
        settingsObj.put("smtpPassword", smtpPassword);
        settingsObj.put("smtpHost", smtpHost);
        try {
            result.put("accepted", true);
            result.put("settings", settingsObj);
            result.put("message", "Success : Fetched mail settings!");
            return new ServiceResponse(result);
        } catch (Exception e) {
            throw new APIException(500, "Failed : Unable to fetch mail settings");
        }
    }
}
