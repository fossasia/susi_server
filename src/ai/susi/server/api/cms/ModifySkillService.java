package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.SkillTransactions;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;

import org.json.JSONObject;
import ai.susi.server.APIException;
import ai.susi.server.APIHandler;
import ai.susi.server.AbstractAPIHandler;
import ai.susi.server.Authentication;
import ai.susi.server.Authorization;
import ai.susi.server.ClientCredential;
import ai.susi.server.ClientIdentity;
import ai.susi.server.Query;
import ai.susi.server.RemoteAccess;
import ai.susi.server.ServiceResponse;
import ai.susi.server.UserRole;
import ai.susi.tools.DateParser;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.nio.file.Path;
import java.nio.file.Paths;

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
    	// debug the call
    	Query query = RemoteAccess.evaluate(call);
        query.initPOST(RemoteAccess.getPostMap(call));
        logClient(System.currentTimeMillis(), query, null, 0, "init post");

        String access_token = call.getParameter("access_token");
        String email = call.getParameter("email");
        String userEmail=null;
        String userId = null;
        ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, access_token);
        Authentication authentication = DAO.getAuthentication(credential);

        if (authentication.getIdentity() != null) {
            ClientIdentity identity = authentication.getIdentity();
            Authorization authorization = DAO.getAuthorization(identity);
            UserRole userRole = authorization.getUserRole();

            if (email != null && (userRole.getName().equals("admin") || userRole.getName().equals("superadmin"))) {
                ClientIdentity userIdentity = new ClientIdentity(ClientIdentity.Type.email, email);
                userId = userIdentity.getUuid();
                userEmail = userIdentity.getName();
            } else {
                userEmail = identity.getName();
                userId = identity.getUuid();
            }
        }
        // CORS Header
        resp.setHeader("Access-Control-Allow-Origin", "*");
        if (call.getParameter("access_token") != null) {
            // if client sends private=1 then it is a private skill
            File private_skill_dir = null;
            String privateSkill = call.getParameter("private");
            if(privateSkill != null){
                private_skill_dir = new File(DAO.private_skill_watch_dir,userId);
            }
            // GET OLD VALUES HERE
            String model_name = call.getParameter("OldModel");
            if (model_name == null) {
                model_name = "general";
            }
            File model = new File(DAO.model_watch_dir, model_name);
            if(privateSkill != null){
                    model = private_skill_dir;
            }
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
            File skill = DAO.getSkillFileInLanguage(language, skill_name, false);
            skill_name = skill.getName().replaceAll("\\.txt", "");
            // GET MODIFIED VALUES HERE
            String modified_model_name = call.getParameter("NewModel");
            if (modified_model_name == null) {
                modified_model_name = "general";
            }
            File modified_model = new File(DAO.model_watch_dir, modified_model_name);
            if(privateSkill != null){
                    modified_model = private_skill_dir;
            }
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
                if (model_name.equals(modified_model_name) &&
                    group_name.equals(modified_group_name) &&
                    language_name.equals(modified_language_name) &&
                    skill_name.equals(modified_skill_name)) {
                    // Writing to File
                    try (FileWriter file = new FileWriter(skill)) {
                        file.write(content);
                        json.put("message", "Skill updated");
                        json.put("accepted", true);

                    } catch (IOException e) {
                        e.printStackTrace();
                        json.put("message", "error: " + e.getMessage());
                    }

                    // Update the modified time in the skillInfo.json file
                    updateModifiedTime(model_name, group_name, language_name, skill_name);

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
                            if (Files.exists(new_path)) {
                                File new_image = new_path.toFile();
                                new_image.delete();
                            }
                            Image image = ImageIO.read(filecontent);
                            BufferedImage bi = CreateSkillService.createResizedCopy(image, 512, 512, true);
                            // Checks if images directory exists or not. If not then create one
                            if (!Files.exists(Paths.get(language.getPath() + File.separator + "images"))) {
                                new File(language.getPath() + File.separator + "images").mkdirs();
                            }
                            ImageIO.write(bi, "jpg", new File(language.getPath() + File.separator + "images/" + new_image_name));
                            json.put("message", "Skill updated");
                            json.put("accepted", true);
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
                    if (!modified_skill.exists()) {
                        skill.delete();
                        try (FileWriter newSkillFile = new FileWriter(modified_skill)) {
                            newSkillFile.write(content);
                            json.put("message", "Skill updated");
                            json.put("accepted", true);
                        } catch (IOException e) {
                            e.printStackTrace();
                            json.put("message", "error: " + e.getMessage());
                        }

                        // Update the modified time in the skillInfo.json file
                        updateModifiedTime(modified_model_name, modified_group_name, modified_language_name, modified_skill_name);

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
                                    BufferedImage bi = CreateSkillService.createResizedCopy(image, 512, 512, true);
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
                json.put("message", "Skill doesn't exists");
                json.put("accepted", false);
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(json.toString());
            }
            if (privateSkill != null) {
                modifyChatbot(modified_skill, userId, group_name, language_name, skill_name, modified_group_name, modified_language_name, modified_skill_name);
                SkillTransactions.addAndPushCommit(true, commit_message, userEmail);
            } else {
                SkillTransactions.addAndPushCommit(false, commit_message, userEmail);
            }
        } else {
            JSONObject json = new JSONObject();
            json.put("message", "Bad Access Token not given");
            json.put("accepted", false);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json.toString());
        }
    }

    /**
    * Helper method to update the private skill bot of the user in chatbot.json file
    */
    private static void modifyChatbot(File modified_skill, String userId, String group_name, String language_name, String skill_name, String modified_group_name, String modified_language_name, String modified_skill_name) {
        JsonTray chatbot = DAO.chatbot;
        JSONObject userName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject designObject = new JSONObject();
        JSONObject configObject = new JSONObject();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        if (chatbot.has(userId)) {
            userName = chatbot.getJSONObject(userId);
            if (userName.has(modified_group_name)) {
                groupName = userName.getJSONObject(modified_group_name);
                if (groupName.has(modified_language_name)) {
                    languageName = groupName.getJSONObject(modified_language_name);
                }
            }
        }

        // read design and configuration settings
        try {
          BufferedReader br = new BufferedReader(new FileReader(modified_skill));
          String line = "";
          readloop: while ((line = br.readLine()) != null) {
            String linebeforetrim = line;
            line = line.trim();

            if (line.startsWith("::")) {
              int thenpos=-1;
              if (line.startsWith("::bodyBackgroundImage") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("bodyBackgroundImage",value);
              }
              else if (line.startsWith("::bodyBackground") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("bodyBackground",value);
              }
              else if (line.startsWith("::userMessageBoxBackground") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("userMessageBoxBackground",value);
              }
              else if (line.startsWith("::userMessageTextColor") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("userMessageTextColor",value);
              }
              else if (line.startsWith("::botMessageBoxBackground") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("botMessageBoxBackground",value);
              }
              else if (line.startsWith("::botMessageTextColor") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("botMessageTextColor",value);
              }
              else if (line.startsWith("::botIconColor") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("botIconColor",value);
              }
              else if (line.startsWith("::botIconImage") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                designObject.put("botIconImage",value);
              }
              else if (line.startsWith("::allow_bot_only_on_own_sites") && (thenpos = line.indexOf(' ')) > 0) {
                Boolean value = false;
                if(line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) value = true;
                configObject.put("allow_bot_only_on_own_sites",value);
              }
              else if (line.startsWith("::allowed_sites") && (thenpos = line.indexOf(' ')) > 0) {
                String value = line.substring(thenpos + 1).trim();
                if(value.length() > 0)
                configObject.put("allowed_sites",value);
              }
              else if (line.startsWith("::enable_default_skills") && (thenpos = line.indexOf(' ')) > 0) {
                Boolean value = true;
                if(line.substring(thenpos + 1).trim().equalsIgnoreCase("no")) value = false;
                configObject.put("enable_default_skills",value);
              }
              else if (line.startsWith("::enable_bot_in_my_devices") && (thenpos = line.indexOf(' ')) > 0) {
                Boolean value = false;
                if(line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) value = true;
                configObject.put("enable_bot_in_my_devices",value);
              }
              else if (line.startsWith("::enable_bot_for_other_users") && (thenpos = line.indexOf(' ')) > 0) {
                Boolean value = false;
                if(line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) value = true;
                configObject.put("enable_bot_for_other_users",value);
              }
            }
          }
        } catch (IOException e) {
          DAO.log(e.getMessage());
        }
        // delete the previous chatbot
        DAO.deleteChatbot(userId, group_name, language_name, skill_name);
        // save a new bot
        JSONObject botObject = new JSONObject();
        botObject.put("design",designObject);
        botObject.put("configure",configObject);
        botObject.put("timestamp", timestamp.toString());
        languageName.put(modified_skill_name, botObject);
        groupName.put(modified_language_name, languageName);
        userName.put(modified_group_name, groupName);
        chatbot.put(userId, userName, true);
    }

    private static void updateModifiedTime(String model_name, String group_name, String language_name, String skill_name ) {
        JsonTray skillInfo = DAO.skillInfo;
        JSONObject modelName = new JSONObject();
        JSONObject groupName = new JSONObject();
        JSONObject languageName = new JSONObject();
        JSONObject skillName = new JSONObject();
        DateFormat dateFormatType = DateParser.iso8601Format;
        String skillModifiedTime = dateFormatType.format(new Date());

        if (skillInfo.has(model_name)) {
            modelName = skillInfo.getJSONObject(model_name);
            if (modelName.has(group_name)) {
                groupName = modelName.getJSONObject(group_name);
                if (groupName.has(language_name)) {
                    languageName = groupName.getJSONObject(language_name);
                    if (languageName.has(skill_name)) {
                        skillName = languageName.getJSONObject(skill_name);
                        skillName.put("lastModifiedTime",skillModifiedTime);
                        return;
                    }
                }
            }
        }

        skillName.put("lastModifiedTime",skillModifiedTime);
        languageName.put(skill_name, skillName);
        groupName.put(language_name, languageName);
        modelName.put(group_name, groupName);
        skillInfo.put(model_name, modelName, true);
        return;
    }
}
