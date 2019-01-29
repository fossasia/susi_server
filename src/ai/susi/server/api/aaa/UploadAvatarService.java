package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authentication;
import ai.susi.server.Authorization;
import ai.susi.server.ClientCredential;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Query;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;

import ai.susi.server.api.cms.UploadImageService;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by @akshatnitd on 11/8/18.
 * This Service uploads an user avatar on the server
 * Can be tested on :-
 * http://127.0.0.1:4000/aaa/uploadAvatarImage.json
 * 3 parameters in post request: access_token : string, image : file
 */

@MultipartConfig(fileSizeThreshold=1024*1024*10,    // 10 MB
        maxFileSize=1024*1024*50,       // 50 MB
        maxRequestSize=1024*1024*100)       // 100 MB
public class UploadAvatarService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = -3378038477776664836L;

	@Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/aaa/uploadAvatar.json";
    }
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Access-Control-Allow-Origin", "*"); // enable CORS
        JSONObject result = new JSONObject();
        Part imagePart = req.getPart("image");
        if (req.getParameter("access_token") != null) {
            if (imagePart == null) {
                result.put("accepted", false);
                result.put("message", "Image file not received");
            } else {
                InputStream imagePartContent = imagePart.getInputStream();

                ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, req.getParameter("access_token"));
                Authentication authentication = DAO.getAuthentication(credential);
                ClientIdentity identity = authentication.getIdentity();
                String userEmail = identity.getName();
                String userId = identity.getUuid();
                    // check if access_token is valid
                if (authentication.getIdentity() != null) {

                    String imagePath = DAO.data_dir  + File.separator + "avatar_uploads";
                    // Reading content for image
                    Image image = ImageIO.read(imagePartContent);
                    BufferedImage bi = UploadImageService.toBufferedImage(image);
                    // Checks if images directory exists or not. If not then create one
                    if (!Files.exists(Paths.get(imagePath))) new File(imagePath).mkdirs();

                    String image_name = userId + ".jpg";
                    File p = new File(imagePath + File.separator + image_name);
                    if (p.exists()) p.delete();
                    ImageIO.write(bi, "jpg", new File(imagePath + File.separator + image_name));
                    result.put("accepted", true);
                    result.put("image_path", image_name);
                }
                else{
                    result.put("accepted", false);
                    result.put("message", "Access token is invalid");
                }
            }
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(result.toString());
        }
        else{
            result.put("message","Access token are not given");
            result.put("accepted",false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(result.toString());
        }
    }
    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) {

        return new ServiceResponse("");
    }
}
