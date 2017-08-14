package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.SusiServer;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Constructor;

/**
 * Created by chetankaushik on 30/05/17.
 * This is a servlet that is used to change the role of any user
 * Anyone with role admin or above admin can access this servlet
 *
 * * Sample request :
 * http://127.0.0.1:4000/aaa/changeRoles?user=example@example.com&user=admin
 * two post/get parameters-
 * 1. user    --> The username of the user whose role is to be modified
 * 2. role    --> the new role of the user
 */

public class ChangeUserRoles extends AbstractAPIHandler implements APIHandler {
    private static final long serialVersionUID = -1432553481906185711L;

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
        return "/aaa/changeRoles.json";
    }


    @Override
    public ServiceResponse serviceImpl(Query query, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions) throws APIException {

        JSONObject result = new JSONObject();
        result.put("accepted", false);
        result.put("message", "Error: Unable to process your request");
        if (query.get("getServicePermissions", null) != null) {
            String serviceString = query.get("getServicePermissions", null);

            Class<?> serviceClass;
            try {
                serviceClass = Class.forName(serviceString);
            } catch (ClassNotFoundException e) {
                throw new APIException(400, "Bad service name (no class)");
            }
            Constructor<?> constructor;
            try {
                constructor = serviceClass.getConstructors()[0];
            } catch (Throwable e) {
                throw new APIException(400, "Bad service name (no constructor)");
            }
            Object service;
            try {
                service = constructor.newInstance();
            } catch (Throwable e) {
                throw new APIException(400, "Bad service name (no instance possible)");
            }

            if (service instanceof AbstractAPIHandler) {
                result.put("servicePermissions", authorization.getPermission());
                result.put("accepted", true);
                result.put("message", "Successfully processed request");
                return new ServiceResponse(result);
            } else {
                throw new APIException(400, "Bad service name (no instance of AbstractAPIHandler)");
            }
        } else if (query.get("getServiceList", false)) {
            JSONArray serviceList = new JSONArray();
            for (Class<? extends Servlet> service : SusiServer.services) {
                serviceList.put(service.getCanonicalName());
            }
            result.put("serviceList", serviceList);
            result.put("accepted", true);
            result.put("message", "Successfully processed request");
            return new ServiceResponse(result);
        } else if (query.get("getUserRolePermission", false)) {
            result.put("userRolePermissions", authorization.getPermission());
            result.put("accepted", true);
            result.put("message", "Successfully processed request");
            return new ServiceResponse(result);
        } else {
            // Accept POST/GET params
            String userTobeUpgraded = query.get("user", null);
            String upgradedRole = query.get("role", "user");
            upgradedRole = upgradedRole.toLowerCase();
            UserRole userRole;

            switch (upgradedRole) {
                case "bot":
                    userRole = UserRole.BOT;
                    break;
                case "anonymous":
                    userRole = UserRole.ANONYMOUS;
                    break;
                case "user":
                    userRole = UserRole.USER;
                    break;
                case "reviewer":
                    userRole = UserRole.REVIEWER;
                    break;
                case "accountcreator":
                    userRole = UserRole.ACCOUNTCREATOR;
                    break;
                case "admin":
                    userRole = UserRole.ADMIN;
                    break;
                case "bureaucrat":
                    userRole = UserRole.BUREAUCRAT;
                    break;
                default:
                    userRole = null;
            }
            // Validation
            if (userTobeUpgraded == null) {
                throw new APIException(400, "Bad username");
            }

            //generate client
            ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, userTobeUpgraded);
            ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, credential.getName());
            if (!DAO.hasAuthorization(identity)) {
                throw new APIException(400, "Username not found");
            }

            // Update the local database
            Authorization auth = DAO.getAuthorization(identity);
            try {
                auth.setUserRole(userRole);
            } catch (IllegalArgumentException e) {
                throw new APIException(400, "role not found");
            }

            // Print Response
            result.put("newDetails", auth.getJSON());
            result.put("accepted", true);
            result.put("message", "User role changed successfully!!");
            return new ServiceResponse(result);
        }
    }
}

