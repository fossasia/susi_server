package ai.susi.server.api.cms;

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
 * Created by @hkedia321 on 8/6/18. This Service uploads an image on the server
 * Can be tested on :- http://127.0.0.1:4000/cms/uploadImage.json 3 parameters
 * in post request, Necessary parameters: access_token, image, image_name
 */

@MultipartConfig(fileSizeThreshold = 1024 * 1024 * 10, // 10 MB
        maxFileSize = 1024 * 1024 * 50, // 50 MB
        maxRequestSize = 1024 * 1024 * 100) // 100 MB
public class UploadImageService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 2461878195432910492L;

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
        return "/cms/uploadImage.json";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        resp.setHeader("Access-Control-Allow-Origin", "*"); // enable CORS
        JSONObject json = new JSONObject();
        Part imagePart = req.getPart("image");
        if (req.getParameter("access_token") != null) {
            if (imagePart == null) {
                json.put("accepted", false);
                json.put("message", "Image not given");
            } else {
                InputStream imagePartContent = imagePart.getInputStream();

                String image_name = req.getParameter("image_name");

                ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token,
                        req.getParameter("access_token"));
                Authentication authentication = DAO.getAuthentication(credential);
                ClientIdentity identity = authentication.getIdentity();
                String userId = identity.getUuid();
                // check if access_token is valid
                if (authentication.getIdentity() != null) {

                    String imagePath = DAO.data_dir + File.separator + "image_uploads" + File.separator + userId;
                    if (image_name == null) {
                        // Checking for
                        json.put("accepted", false);
                        json.put("message", "The Image name is not given");

                    } else {

                        // Reading content for image
                        Image image = ImageIO.read(imagePartContent);
                        BufferedImage bi = this.toBufferedImage(image);
                        // Checks if images directory exists or not. If not then create one
                        if (!Files.exists(Paths.get(imagePath)))
                            new File(imagePath).mkdirs();
                        String new_image_name = System.currentTimeMillis() + "_" + image_name;
                        File p = new File(imagePath + File.separator + new_image_name);
                        if (p.exists())
                            p.delete();
                        ImageIO.write(bi, "jpg", new File(imagePath + File.separator + new_image_name));
                        json.put("accepted", true);
                        json.put("image_path", userId + "/" + new_image_name);
                    }
                } else {
                    json.put("accepted", false);
                    json.put("message", "Access token is invalid");
                }
            }
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        } else {
            json.put("message", "Access token are not given");
            json.put("accepted", false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights,
            final JsonObjectWithDefault permissions) {

        return new ServiceResponse("");
    }
}
