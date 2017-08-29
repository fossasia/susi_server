package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Created by chetankaushik on 07/06/17.
 * changelog is the commit message that you want to set for the versioning system.
 * before modifying an skill the skill must exist in the directory.
 * send a POST request to http://127.0.0.1:4000/cms/modifySkill.json
 * Parameters in AJAX:
 form.append("OldModel", "general");
 form.append("OldGroup", "Knowledge");
 form.append("OldLanguage", "de");
 form.append("OldSkill", "github");
 form.append("NewModel", "general");
 form.append("NewGroup", "Knowledge");
 form.append("NewLanguage", "en");
 form.append("NewSkill", "githuba");
 form.append("changelog", "change group");
 form.append("content", "skill content");
 form.append("imageChanged", "true");
 form.append("old_image_name", "github.png");
 form.append("new_image_name", "githubModified.png");
 form.append("image_name_changed", "true");
 form.append("image", "");
 *
 */
@MultipartConfig(fileSizeThreshold=1024*1024*10, 	// 10 MB
        maxFileSize=1024*1024*50,      	// 50 MB
        maxRequestSize=1024*1024*100)   	// 100 MB
public class ModifySkillService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = -1834363513093189312L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.USER; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {
        return new ServiceResponse("");
    }

    @Override
    public String getAPIPath() {
        return "/cms/modifySkill.json";
    }

    @Override
    protected void doPost(HttpServletRequest call, HttpServletResponse resp) throws ServletException, IOException {
        String userEmail=null;
        // CORS Header
        resp.setHeader("Access-Control-Allow-Origin", "*");
        if (call.getParameter("access_token") != null) {
            // GET OLD VALUES HERE
            String model_name = call.getParameter("OldModel");
            if (model_name == null) {
                model_name = "general";
            }
            File model = new File(DAO.model_watch_dir, model_name);
            String group_name = call.getParameter("OldGroup");
            if (group_name == null) {
                group_name = "Knowledge";
            }
            File group = new File(model, group_name);
            String language_name = call.getParameter("OldLanguage");
            if (language_name == null) {
                language_name = "en";
            }
            File language = new File(group, language_name);
            String skill_name = call.getParameter("OldSkill");
            if (skill_name == null) {
                skill_name = "";
            }
            File skill = new File(language, skill_name + ".txt");
            // GET MODIFIED VALUES HERE
            String modified_model_name = call.getParameter("NewModel");
            if (modified_model_name == null) {
                modified_model_name = "general";
            }
            File modified_model = new File(DAO.model_watch_dir, modified_model_name);
            String modified_group_name = call.getParameter("NewGroup");
            if (modified_group_name == null) {
                modified_group_name = "Knowledge";
            }
            File modified_group = new File(modified_model, modified_group_name);
            String modified_language_name = call.getParameter("NewLanguage");
            if (modified_language_name == null) {
                modified_language_name = "en";
            }
            File modified_language = new File(modified_group, modified_language_name);
            String modified_skill_name = call.getParameter("NewSkill");
            if (modified_skill_name == null) {
                modified_skill_name = skill_name;
            }
            File modified_skill = new File(modified_language, modified_skill_name + ".txt");
            // GET CHANGELOG MESSAGE HERE
            String commit_message = call.getParameter("changelog");
            if (commit_message == null) {
                commit_message = "Modified " + skill_name;
            }
            String content = call.getParameter("content");
            if (skill.exists() && content != null) {
                JSONObject json = new JSONObject();
                // CHECK IF SKILL PATH AND NAME IS SAME. IF IT IS SAME THEN MAKE CHANGES IN OLD FILE ONLY
                if (Objects.equals(model_name, modified_model_name) &&
                        Objects.equals(group_name, modified_group_name) &&
                        Objects.equals(language_name, modified_language_name) &&
                        Objects.equals(skill_name, modified_skill_name)) {
                    // Writing to File
                    try (FileWriter file = new FileWriter(skill)) {
                        file.write(content);
                        json.put("message", "Skill updated");
                        json.put("accepted", true);

                    } catch (IOException e) {
                        e.printStackTrace();
                        json.put("message", "error: " + e.getMessage());
                    }
                    // CHECK IF IMAGE WAS CHANGED
                    // PARAMETER FOR GETTING IF IMAGE WAS CHANGED
                    String image_changed = call.getParameter("imageChanged");
                    if (image_changed == null) {
                        image_changed = "false";
                    }

                    if (image_changed.equals("true")) {
                        Part file = call.getPart("image");
                        if (file != null) {
                            InputStream filecontent = file.getInputStream();
                            String new_image_name = call.getParameter("new_image_name");
                            String old_image_name = call.getParameter("old_image_name");
                            Path new_path = Paths.get(language + File.separator + "images/" + new_image_name);
                            Path old_path = Paths.get(language + File.separator + "images/" + old_image_name);
                            if (Files.exists(old_path)) {
                                File old_image = old_path.toFile();
                                old_image.delete();
                            }
                            if (!Files.exists(new_path)) {
                                Image image = ImageIO.read(filecontent);
                                BufferedImage bi = this.createResizedCopy(image, 512, 512, true);
                                // Checks if images directory exists or not. If not then create one
                                if (!Files.exists(Paths.get(language.getPath() + File.separator + "images"))) {
                                    new File(language.getPath() + File.separator + "images").mkdirs();
                                }
                                ImageIO.write(bi, "jpg", new File(language.getPath() + File.separator + "images/" + new_image_name));
                                json.put("message", "Skill updated");
                                json.put("accepted", true);
                            } else {
                                // Checking if same file is present or not
                                json.put("accepted", false);
                                json.put("message", "Image with same name is already present ");
                            }
                        } else {
                            // Checking if file is null or not
                            json.put("accepted", false);
                            json.put("message", "Image not sent in request");
                        }

                    }

                    String image_name_changed = call.getParameter("image_name_changed");
                    if (image_name_changed == null) {
                        image_name_changed = "false";
                    }

                    if (image_name_changed.equals("true") && image_changed.equals("false")) {
                        String new_image_name = call.getParameter("new_image_name");
                        String old_image_name = call.getParameter("old_image_name");
                        Path new_path = Paths.get(language + File.separator + "images/" + new_image_name);
                        Path old_path = Paths.get(language + File.separator + "images/" + old_image_name);
                        if (!Files.exists(old_path)) {
                            json.put("accepted", false);
                            json.put("message", "Image requested to rename is not present");
                        }
                        if (!Files.exists(new_path)) {
                            old_path.toFile().renameTo(new_path.toFile());
                            json.put("message", "Skill updated");
                            json.put("accepted", true);
                        } else {
                            json.put("accepted", false);
                            json.put("message", "Image with same name already present");
                        }
                    }

                }
                // IF SKILL AND IMAGE PATH IS CHANGED
                else {
                    // if skill is moved to a new location then delete the previous skill and create a new skill at a new location.
                    String new_image_name = call.getParameter("new_image_name");
                    Path new_path = Paths.get(modified_language + File.separator + "images/" + new_image_name);
                    if (!Files.exists(Paths.get(modified_language.getPath() + File.separator + "images"))) {
                        new File(modified_language.getPath() + File.separator + "images").mkdirs();
                    }
                    // write new file here
                    if (!modified_skill.exists() && !Files.exists(new_path)) {
                        skill.delete();
                        try (FileWriter newSkillFile = new FileWriter(modified_skill)) {
                            newSkillFile.write(content);
                            json.put("message", "Skill updated");
                            json.put("accepted", true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            json.put("message", "error: " + e.getMessage());
                        }

                        // CHECK IF IMAGE WAS CHANGED
                        // PARAMETER FOR GETTING IF IMAGE WAS CHANGED
                        String image_changed = call.getParameter("imageChanged");
                        if (image_changed == null) {
                            image_changed = "false";
                        }

                        if (image_changed.equals("true")) {
                            Part file = call.getPart("image");
                            if (file != null) {
                                InputStream filecontent = file.getInputStream();
                                String old_image_name = call.getParameter("old_image_name");
                                Path old_path = Paths.get(language + File.separator + "images/" + old_image_name);
                                if (Files.exists(old_path)) {
                                    File old_image = old_path.toFile();
                                    old_image.delete();
                                }
                                if (!Files.exists(new_path)) {
                                    Image image = ImageIO.read(filecontent);
                                    BufferedImage bi = this.createResizedCopy(image, 512, 512, true);
                                    // Checks if images directory exists or not. If not then create one
                                    if (!Files.exists(Paths.get(modified_language.getPath() + File.separator + "images"))) {
                                        new File(modified_language.getPath() + File.separator + "images").mkdirs();
                                    }
                                    ImageIO.write(bi, "jpg", new File(modified_language.getPath() + File.separator + "images/" + new_image_name));
                                    json.put("message", "Skill updated");
                                    json.put("accepted", true);
                                } else {
                                    json.put("accepted", false);
                                    json.put("message", "The Image name not given or Image with same name is already present ");
                                }
                            }
                        }
                        // else just move the image from old path to new path
                        else {
                            String old_image_name = call.getParameter("old_image_name");
                            Path old_path = Paths.get(language + File.separator + "images/" + old_image_name);
                            if (!Files.exists(new_path)) {
                                old_path.toFile().renameTo(new_path.toFile());
                                json.put("message", "Skill updated");
                                json.put("accepted", true);
                            }

                        }

                        String image_name_changed = call.getParameter("image_name_changed");
                        if (image_name_changed == null) {
                            image_name_changed = "false";
                        }

                        if (image_name_changed.equals("true") && image_changed.equals("false")) {
                            String old_image_name = call.getParameter("old_image_name");
                            Path old_path = Paths.get(modified_language + File.separator + "images/" + old_image_name);
                            if (!Files.exists(new_path)) {
                                old_path.toFile().renameTo(new_path.toFile());
                                json.put("message", "Skill updated");
                                json.put("accepted", true);
                            }
                        }
                    }

                }

                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(json.toString());
            } else {
                JSONObject json = new JSONObject();
                json.put("message", "Bad parameter call");
                json.put("accepted", false);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(json.toString());
            }
            if (call.getParameter("access_token") != null) { // access tokens can be used by api calls, somehow the stateless equivalent of sessions for browsers
                ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, call.getParameter("access_token"));
                Authentication authentication = DAO.getAuthentication(credential);

                // check if access_token is valid
                if (authentication.getIdentity() != null) {
                    ClientIdentity identity = authentication.getIdentity();
                    userEmail = identity.getName();
                }
            }

            try (Git git = DAO.getGit()) {
                git.add().setUpdate(true).addFilepattern(".").call();
                git.add().addFilepattern(".").call();

                // commit the changes
                DAO.pushCommit(git, commit_message, userEmail);
            } catch (IOException | GitAPIException e) {
                e.printStackTrace();

            }
        }
        else{
            JSONObject json = new JSONObject();
            json.put("message", "Bad Access Token not given");
            json.put("accepted", false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
    }

    BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha) {
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }

}
