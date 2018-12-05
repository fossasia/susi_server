package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Created by dravit on 10/8/17.
 * To show or not to show admin option to the user who is logged in,
 * this servlet will return a boolean flag on attribute showAdmin
 */
@Path("/aaa/showAdminService.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "ShowAdminService",
        description = "This Endpoint returns the flag of a user account to show or not to show admin option.")
public class ShowAdminService extends AbstractAPIHandler implements APIHandler{

    private static final long serialVersionUID = 8597701313383109201L;

    @Override
    public String getAPIPath() {
        return "/aaa/showAdminService.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource  To show or not to show admin option to the user who is logged in")
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                    message = "showAdmin : true") })
    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        UserRole userRole = rights.getUserRole();

        switch (userRole) {
            case SUPERADMIN:
            case ADMIN:
            case OPERATOR:
                json.put("accepted", true);
                json.put("showAdmin", true);
                break;
            case ANONYMOUS:
            case USER:
            case REVIEWER:
            default:
                json.put("accepted", true);
                json.put("showAdmin", false);
        }

        return new ServiceResponse(json);
    }
}
