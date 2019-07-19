package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.EmailHandler;
import ai.susi.server.Authorization;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;

/**
 * This Servlet gives a API Endpoint to add, modify and delete Email Settings
 * details used by SUSI. It requires user role to be ADMIN or above ADMIN
 * example: For configuring settings, Necessary parameters, : access_token,
 * email, name, type, sendgrid_token, frontend_url, trustselfsignedcerts,
 * encryption, port, smtpUserName, smtpPassword
 * http://localhost:4000/aaa/emailSettings.json?access_token=tO063rvh13K58CsZPywwSu4qJqjUyL&frontend_url=https://susi.ai&displayname=shubham&type=SMTP&name=Admin&email=server@loklak.org&sendgrid_token=dfg
 * Optional - For sending a testing email, add parameter, receiver_email
 */

public class EmailSettingsService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 5000658108778105134L;

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ADMIN;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/emailSettings.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization authorization,
            final JsonObjectWithDefault permissions) throws APIException {

        String email = call.get("email", "");
        String name = call.get("name", "");
        String mailType = call.get("type", "");
        String sendgridToken = call.get("sendgrid_token", "");
        String frontendUrl = call.get("frontend_url", "");

        String trustselfsignedcerts = call.get("trustselfsignedcerts", "");
        String encryption = call.get("encryption", "");
        String port = call.get("port", "");
        String username = call.get("username", "");
        String password = call.get("password", "");

        // For testing email through admin
        String receiverEmail = call.get("receiver_email", null);
        String smtpHost = call.get("smtp_host", null);
        JSONObject result = new JSONObject();

        if (receiverEmail != null) {
            String adminEmail = authorization.getIdentity().getName();
            try {
                EmailHandler.sendEmail(email, name, receiverEmail, "Testing email through Admin",
                        "You have successfully sent a email");
                result.put("accepted", true);
                result.put("message", "You have successfully sent a email to your email id:" + adminEmail);
            } catch (Exception e) {
                result.put("message", e.getMessage() + receiverEmail);
            }
            return new ServiceResponse(result);
        }

        DAO.setConfig("smtp.sender.email", email);
        DAO.setConfig("smtp.sender.displayname", name);
        DAO.setConfig("mail.type", mailType);
        DAO.setConfig("mail.sendgridtoken", sendgridToken);
        DAO.setConfig("mail.frontendurl", frontendUrl);
        DAO.setConfig("smtp.trustselfsignedcerts", trustselfsignedcerts);
        DAO.setConfig("smtp.host.encryption", encryption);
        DAO.setConfig("smtp.host.port", port);
        DAO.setConfig("smtp.sender.username", username);
        DAO.setConfig("smtp.sender.password", password);
        DAO.setConfig("smtp.host.name", smtpHost);
        result.put("accepted", true);
        result.put("Successfully updated mail settings", true);
        return new ServiceResponse(result);
    }
}
