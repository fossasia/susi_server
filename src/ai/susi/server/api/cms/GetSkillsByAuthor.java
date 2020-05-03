package ai.susi.server.api.cms;

import ai.susi.DAO;
import ai.susi.EmailHandler;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import ai.susi.tools.IO;
import ai.susi.tools.TimeoutMatcher;
import ai.susi.tools.skillqueryparser.SkillQuery;
import ai.susi.tools.skillqueryparser.SkillQueryParser;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by chetankaushik on 12/08/17.
 * API Endpoint to get all skills by an author.
 * http://127.0.0.1:4000/cms/getSkillsByAuthor.json?author=chetan%20kaushik
 *
 * Modified by @Akshat-Jain on 21/06/18.
 * API Endpoint to get all skills by an author using his email.
 * http://127.0.0.1:4000/cms/getSkillsByAuthor.json?author_email=akjn11@gmail.com
 */
public class GetSkillsByAuthor extends AbstractAPIHandler implements APIHandler {

    JSONObject result ;
    private static final long serialVersionUID = 3446536703362688060L;

    @Override
    public UserRole getMinimalUserRole() { return UserRole.ANONYMOUS; }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public String getAPIPath() {
        return "/cms/getSkillsByAuthor.json";
    }


    @Override
    public ServiceResponse serviceImpl(Query call, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException {
        result= new JSONObject();
        DAO.observe(); // get a database update

        String author = call.get("author",null);
        String author_email = call.get("author_email",null);
        String author_avatar = "";

        if(author == null && author_email == null){
            throw new APIException(400, "Bad service call, missing arguments.");
        }

        if(author_email == null) {
            for (Map.Entry<SusiSkill.ID, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
                SusiSkill.ID skill = entry.getKey();
                if ((entry.getValue().getAuthor()!=null) && entry.getValue().getAuthor().contains(author)) {
                    result.put(result.length()+"",skill);
                }
            }
            result.put("accepted",true);
            result.put("message","Success");
            return new ServiceResponse(result);
        }
        else {
            // check email pattern
            Pattern pattern = Pattern.compile(EmailHandler.EMAIL_PATTERN);
            if (!new TimeoutMatcher(pattern.matcher(author_email)).matches()) {
                throw new APIException(400, "Invalid email address.");
            }

            JSONObject result = new JSONObject();

            SkillQuery skillQuery = SkillQueryParser.Builder.getInstance().group("All").build().parse(call);
            String model_name = skillQuery.getModel();
            String group_name = skillQuery.getGroup();
            String language_name = skillQuery.getLanguage();
            JSONArray authorSkills = new JSONArray();
            JSONObject json = new JSONObject();

            // Returns all the Skills made by the author
            if (group_name.equals("All")) {
                File allGroup = skillQuery.getModelPath().toFile();
                ArrayList<String> folderList = new ArrayList<String>();
                listFoldersForFolder(allGroup, folderList);
                json.put("accepted", false);

                for (String temp_group_name : folderList){
                    Path group = IO.resolvePath(skillQuery.getModelPath(), temp_group_name);
                    File language = IO.resolvePath(group, language_name).toFile();
                    ArrayList<String> fileList = new ArrayList<String>();
                    listFilesForFolder(language, fileList);

                    for (String skill_name : fileList) {
                        skill_name = skill_name.replace(".txt", "");
                        JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, temp_group_name, language_name, skill_name);

                        if(skillMetadata.get("author_email").equals(author_email)) {
                            skillMetadata.put("skill_name", skill_name);
                            authorSkills.put(skillMetadata);
                        }
                    }
                }
            }

            // Returns all the Skills made by the author belonging to a particular group
            else {
                Path group = IO.resolvePath(skillQuery.getModelPath(), group_name);
                File language = IO.resolvePath(group, language_name).toFile();
                json.put("accepted", false);
                ArrayList<String> fileList = new ArrayList<String>();
                listFilesForFolder(language, fileList);

                for (String skill_name : fileList) {
                    skill_name = skill_name.replace(".txt", "");
                    JSONObject skillMetadata = DAO.susi.getSkillMetadata(model_name, group_name, language_name, skill_name);

                    if(skillMetadata.get("author_email").equals(author_email)) {
                        skillMetadata.put("skill_name", skill_name);
                        authorSkills.put(skillMetadata);
                    }
                }
            }

            if(authorSkills.length() == 0){
                result.put("accepted",true);
                result.put("message","Author hasn't created any Skill yet.");
                return new ServiceResponse(result);
            }
            
            try {
                author_avatar = getUserAvatar(author_email);
            } catch (IOException e) {}

            result.put("author_avatar", author_avatar);
            result.put("author_skills", authorSkills);
            result.put("accepted",true);
            result.put("message","Successfully fetched all Skills created by author.");
            return new ServiceResponse(result);
        }
    }

 // Method to get the avatar image of the user
    public static String getUserAvatar(String email) throws IOException {
        ClientIdentity identity = new ClientIdentity(ClientIdentity.Type.email, email);
        Accounting accounting = DAO.getAccounting(identity);
        String userId = identity.getUuid();
        String avatarType = "";
        String file = "";
        String avatar_string = "";
        File imageFile = null;

        JSONObject accountingObj = accounting.getJSON();
        if (accountingObj.has("settings") &&
            accountingObj.getJSONObject("settings").has("avatarType")) {
            avatarType = accountingObj.getJSONObject("settings").getString("avatarType");
            file = userId + ".jpg";
        } else {
            avatarType = "default";
        }

        InputStream is = null;
        byte[] b = new byte[2048];
        ByteArrayOutputStream data = new ByteArrayOutputStream();

        switch (avatarType) { 
            case "gravatar":
                String gravatarUrl = "https://www.gravatar.com/avatar/" + file;
                URL url = new URL(gravatarUrl);
                is = url.openStream();
                break;
            case "server":
                imageFile = GetImageServlet.getAvatar(file);
                is = new BufferedInputStream(new FileInputStream(imageFile));
                break;
            default:
                imageFile = GetImageServlet.getDefaultImage();
                is = new BufferedInputStream(new FileInputStream(imageFile));
                break;
        }

        int c;
        try {
            while ((c = is.read(b)) >  0) {data.write(b, 0, c);}
        } catch (IOException e) {
            data.reset();
            e.printStackTrace();
        }
        
        avatar_string = new String(data.toByteArray());
        
        return avatar_string;
    }
    
    private void listFilesForFolder(final File folder, ArrayList<String> fileList) {
        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            Arrays.stream(filesInFolder)
                    .filter(fileEntry -> !fileEntry.isDirectory() && !fileEntry.getName().startsWith("."))
                    .forEach(fileEntry -> fileList.add(fileEntry.getName() + ""));
        }
    }

    private void listFoldersForFolder(final File folder, ArrayList<String> fileList) {
        File[] filesInFolder = folder.listFiles();
        if (filesInFolder != null) {
            Arrays.stream(filesInFolder)
                    .filter(fileEntry -> fileEntry.isDirectory() && !fileEntry.getName().startsWith("."))
                    .forEach(fileEntry -> fileList.add(fileEntry.getName() + ""));
        }
    }
}
