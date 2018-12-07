package ai.susi.server.api.aaa;

import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.server.Authorization;
import io.swagger.annotations.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dravit on 29/8/17.
 */

@Path("/aaa/getAllUserroles.json")
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "GetAllUserroles", description = "This Endpoint fetches all user roles available.")
public class GetAllUserroles extends AbstractAPIHandler implements APIHandler {
    /**
     * 
     */
    private static final long serialVersionUID = -6147215063044889074L;

    @GET
    @ApiOperation(httpMethod = "GET", value = "Resource to fetch all user roles available")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "All user roles fetched successfully"),
    })
    @Override
    public String getAPIPath() {
        return "/aaa/getAllUserroles.json";
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
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

        JSONObject json = new JSONObject(true);
        json.put("accepted", false);
        List<String> userRoles = new ArrayList<String>();
        for(UserRole userRole : UserRole.values()){
            userRoles.add(userRole.getName());
        }
        json.put("userRoles", userRoles);
        json.put("accepted", true);
        json.put("accepted", "All user roles fetched successfully");
        return new ServiceResponse(json);
    }
}
