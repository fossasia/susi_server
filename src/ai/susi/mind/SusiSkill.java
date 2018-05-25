/**
 *  SusiSkill
 *  Copyright 06.06.2017 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package ai.susi.mind;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jfree.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.DAO;
import ai.susi.SusiServer;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.mind.SusiInference.Type;

/**
 * A susi skill is a set of intents.
 * This class provides parser methods for such sets, given as text files
 */
public class SusiSkill {

    private String skillName;
    private String description;
    private String author;
    private String authorURL;
    private String image;
    private String termsOfUse;
    private Set<String> examples;
    private String developerPrivacyPolicy;
    private Boolean dynamicContent;
    private Set<String> tags;

    public static class ID implements Comparable<ID> {
        private String skillpath;
        
        /**
         * compute the skill path from the origin file
         * @param origin
         * @return a relative path to the skill location, based on the git repository
         */
        public ID(File origin) throws UnsupportedOperationException {
            this.skillpath = origin.getAbsolutePath();
            // The skillpath must start with the root path of either the susi_skill_data git repository or of susi_server git repository.
            // In both cases the path must start with a "/".
            int i = this.skillpath.indexOf("/susi");
            if (i < 0) {
                i = this.skillpath.indexOf("\\susi");
                if(i < 0)
                    throw new UnsupportedOperationException("the file path does not point to a susi skill model repository: " + origin.getAbsolutePath());
            }
            this.skillpath = this.skillpath.substring(i);
            if (this.skillpath.startsWith("/susi/") || this.skillpath.startsWith("\\susi\\")) this.skillpath = this.skillpath.substring(5);
        }
        
        public String toString() {
            return this.skillpath;
        }
        
        public String getPath() {
            return this.skillpath;
        }
        
        @Override
        public int compareTo(ID o) {
            return this.skillpath.compareTo(o.skillpath);
        }
        
        @Override
        public int hashCode() {
            return this.skillpath.hashCode();
        }

        /**
         * compute the language from the skillpath
         * @param skillpath
         * @return
         */
        public SusiLanguage language() {
            if (this.skillpath.startsWith("/susi_server/conf/susi/")) {
                SusiLanguage language = SusiLanguage.parse(this.skillpath.substring(23, 25));
                if (language != SusiLanguage.unknown) return language;
            } else if (this.skillpath.startsWith("/susi_skill_data")) {
                SusiLanguage language = SusiLanguage.unknown;
                String[] paths = this.skillpath.split("/");
                if (paths.length > 5) language = SusiLanguage.parse(paths[5]);
                if (language != SusiLanguage.unknown) return language;
                return language;
            }
            
            return SusiLanguage.unknown;
        }

        public boolean hasModel(String model) {
            return model.length() == 0 || this.skillpath.indexOf("/" + model + "/") > 0;
        }

        public boolean hasGroup(String group) {
            return group.length() == 0 || this.skillpath.indexOf("/" + group + "/") > 0;
        }

        public boolean hasLanguage(String language) {
            return language.length() == 0 || this.skillpath.indexOf("/" + language + "/") > 0;
        }

        public boolean hasName(String skillname) {
            return skillname.length() == 0 || this.skillpath.indexOf("/" + skillname + ".txt") > 0;
        }

    }
    
    /**
     * read an "EzD" ('Easy Dialog') file: this is just a text file. Read the docs/susi_skill_development_tutorial.md for an explanation
     * @param br
     * @return
     * @throws JSONException
     * @throws FileNotFoundException
     */

    public SusiSkill() {
        this.author = null;
        this.authorURL = null;
        this.description = null;
        this.examples = new LinkedHashSet<>();
        this.image = null;
        this.skillName = null;
        this.termsOfUse = null;
        this.developerPrivacyPolicy = null;
        this.dynamicContent = false;
        this.tags = new LinkedHashSet<>();
    }
    
    /**
     * read a text skill file (once called "easy dialog - EzD")
     * @param br a buffered reader
     * @return a skill object as JSON
     * @throws JSONException
     */
    public static JSONObject readLoTSkill(final BufferedReader br, final SusiLanguage language, final String skillid) throws JSONException {
        // read the text file and turn it into a intent json; then learn that
        JSONObject json = new JSONObject();
        JSONArray intents = new JSONArray();
        json.put("intents", intents);
        String lastLine = "", line = "";
        String bang_answers = "", bang_type = "", bang_term = ""; StringBuilder bang_bag = new StringBuilder();
        String example = "", tags = "", expect = "", description="", image="", skillName="", authorName= "",
                authorURL = "", developerPrivacyPolicy = "", termsOfUse="";
        boolean prior = false, dynamicContent = false;
        int indentStep = 4; // like in python
        try {readloop: while ((line = br.readLine()) != null) {
            String linebeforetrim = line;
            line = line.trim();
            int indent = linebeforetrim.indexOf(line) % indentStep;            
            
            // connect lines
            if (lastLine.endsWith("\\")) {
                lastLine = lastLine.substring(0,lastLine.length() - 1).trim() + " " + line;
                continue readloop;
            }
            if (line.startsWith("\\")) {
                lastLine = lastLine + " " + line.substring(1).trim();
                continue readloop;
            }
            
            // collect bang expressions
            if (bang_type.length() > 0) {
                // collect a bang
                if (line.toLowerCase().equals("eol")) {
                    // stop collection
                    if (bang_type.equals("javascript")) {
                        // create a javascript intent
                        JSONObject intent = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        intent.put("phrases", phrases);
                        for (String phrase: bang_answers.split("\\|")) phrases.put(SusiUtterance.simplePhrase(phrase.trim(), prior));
                        
                        // javascript process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.javascript.name());
                        try {
                            process.put("expression", bang_bag.toString());
                        } catch (JSONException e) {
                            throw new JSONException(e.getMessage() + " \"" + bang_bag.toString() + "\"");
                        }
                        intent.put("process", new JSONArray().put(process));
                        
                        // answers; must contain $!$
                        intent.put("actions", new JSONArray().put(SusiAction.answerAction(language, bang_term.split("\\|"))));
                        if (example.length() > 0) intent.put("example", example);
                        intents.put(intent);
                    }
                    else if (bang_type.equals("console")) {
                        // create a console intent
                        JSONObject intent = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        intent.put("phrases", phrases);
                        for (String phrase: bang_answers.split("\\|")) phrases.put(SusiUtterance.simplePhrase(phrase.trim(), prior));
                        
                        // console process
                        JSONObject process = new JSONObject();
                        process.put("type", Type.console.name());
                        JSONObject definition = null;
                        try {
                            definition = new JSONObject(new JSONTokener(bang_bag.toString()));
                            process.put("definition", definition);
                        } catch (JSONException e) {
                            throw new JSONException(e.getMessage() + " \"" + bang_bag.toString() + "\"");
                        }
                        intent.put("process", new JSONArray().put(process));
                        
                        // actions; we may have several actions here
                        JSONArray actions = new JSONArray();
                        intent.put("actions", actions);

                        // verify actions
                        if (definition.has("actions")) {
                            JSONArray bo_actions = definition.getJSONArray("actions");
                            bo_actions.forEach(action -> {
                                try {
                                    // looks silly, but this is for verification that the action is valid
									SusiAction protoAction = new SusiAction((JSONObject) action);
									actions.put(protoAction.toJSONClone());
								} catch (SusiActionException e) {
									Log.error(e.getMessage());
								}
                                
                            });
                        }
                        
                        // answers; must contain names from the console result array
                        if (bang_term.length() > 0) {
                            actions.put(SusiAction.answerAction(language, bang_term.split("\\|")));
                        }
                        if (example.length() > 0) intent.put("example", example);
                        if (expect.length() > 0) intent.put("expect", expect);
                        intents.put(intent);
                    }
                    bang_answers = "";
                    bang_type = "";
                    bang_term = "";
                    bang_bag.setLength(0);
                }
                bang_bag.append(line).append('\n');
                continue readloop;
            }
            
            // read metadata
            if (line.startsWith("::")) {
                int thenpos=-1;
//                line = line.toLowerCase();
                if (line.startsWith("::minor")) prior = false;
                if (line.startsWith("::prior")) prior = true;
                if (line.startsWith("::description") && (thenpos = line.indexOf(' ')) > 0) {
                    description = line.substring(thenpos + 1).trim();
                    if(description.length() > 0)
                        json.put("description",description);
                   // System.out.println(description);
                }
                if (line.startsWith("::image") && (thenpos = line.indexOf(' ')) > 0) {
                    image = line.substring(thenpos + 1).trim();
                    if(image.length() > 0)
                        json.put("image",image);
                }
                if (line.startsWith("::name") && (thenpos = line.indexOf(' ')) > 0) {
                    skillName = line.substring(thenpos + 1).trim();
                    if(skillName.length() > 0)
                        json.put("skill_name",skillName);
                }
                if (line.startsWith("::author") && (!line.startsWith("::author_url")) && (thenpos = line.indexOf(' ')) > 0) {
                   authorName = line.substring(thenpos + 1).trim();
                    if(authorName.length() > 0)
                        json.put("author",authorName);
                }
                if (line.startsWith("::author_url") && (thenpos = line.indexOf(' ')) > 0) {
                    authorURL = line.substring(thenpos + 1).trim();
                    if(authorURL.length() > 0)
                        json.put("author_url",authorURL);
                }
                if (line.startsWith("::developer_privacy_policy") && (thenpos = line.indexOf(' ')) > 0) {
                    developerPrivacyPolicy = line.substring(thenpos + 1).trim();
                    if(developerPrivacyPolicy.length() > 0)
                        json.put("developer_privacy_policy",developerPrivacyPolicy);
                }
                if (line.startsWith("::terms_of_use") && (thenpos = line.indexOf(' ')) > 0) {
                    termsOfUse = line.substring(thenpos + 1).trim();
                    if(termsOfUse.length() > 0)
                        json.put("terms_of_use",termsOfUse);
                }
                if (line.startsWith("::dynamic_content") && (thenpos = line.indexOf(' ')) > 0) {
                    if (line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) dynamicContent=true;
                    json.put("dynamic_content",dynamicContent);
                }

                if(line.startsWith("::tags") && (thenpos = line.indexOf(' ')) > 0) {
                    tags = line.substring(thenpos + 1).trim();
                    if(tags.length() > 0)
                        json.put("tags", tags);
                }

                lastLine = ""; example = ""; expect = "";
                continue readloop;
            }
            
            if (line.startsWith("#")) {
                // a comment line; ignore the line and consider it as whitespace
                lastLine = ""; example = ""; expect = "";
                continue readloop;
            }
            
            // read content
            if (line.length() > 0 && lastLine.length() > 0) {
                // mid of conversation (last answer is query for next intent)
                String[] phrases = lastLine.split("\\|");
                String condition = null;
                int thenpos = -1;
                if (line.startsWith("?") && (thenpos = line.indexOf(':')) > 0) {
                    int elsepos = line.substring(thenpos + 1).indexOf(':') + thenpos + 1;
                    condition = line.substring(1, thenpos).trim();
                    if (elsepos <= thenpos) {
                        // only if, no else
                        String ifsubstring = line.substring(thenpos + 1).trim();
                        if (ifsubstring.length() > 0) {
                            String[] answers = ifsubstring.split("\\|");
                            JSONObject intent = SusiIntent.answerIntent(phrases, "IF " + condition, answers, prior, example, expect, language);
                            intents.put(intent);
                        }
                    } else {
                        String ifsubstring = line.substring(thenpos + 1, elsepos).trim();
                        if (ifsubstring.length() > 0) {
                            String[] ifanswers = ifsubstring.split("\\|");
                            JSONObject intentif = SusiIntent.answerIntent(phrases, "IF " + condition, ifanswers, prior, example, expect, language);
                            intents.put(intentif);
                        }
                        String elsesubstring = line.substring(elsepos + 1).trim();
                        if (elsesubstring.length() > 0) {
                            String[] elseanswers = elsesubstring.split("\\|");
                            JSONObject intentelse = SusiIntent.answerIntent(phrases, "NOT " + condition, elseanswers, prior, example, expect, language);
                            intents.put(intentelse);
                        }
                    }
                } else if (line.startsWith("!" /*bang!*/) && (thenpos = line.indexOf(':')) > 0) {
                    String head = line.substring(1, thenpos).trim().toLowerCase();
                    String tail = line.substring(thenpos + 1).trim();
                    // test bang type
                    if (head.equals("example")) {
                        example = tail;
                    } else if (head.equals("expect")) {
                        expect = tail;
                    }
                    else if (head.equals("image")) {
                        image =tail;
                    } else {
                        // start multi-line bang
                        bang_answers = lastLine;
                        bang_type = head;
                        bang_term = tail;
                        bang_bag.setLength(0);
                    }
                    continue readloop;
                } else {
                    String[] answers = line.split("\\|");
                    JSONObject intent = SusiIntent.answerIntent(phrases, condition, answers, prior, example, expect, language);
                    //System.out.println(intent.toString());
                    intents.put(intent);
                }
            }
            lastLine = line;
        }} catch (IOException e) {
            DAO.log(e.getMessage());
        }
        return json;
    }
    
    /**
     * For some strange reason the skill name is requested here in lowercase, while the name may also be uppercase
     * this should be fixed in the front-end, however we implement a patch here to circumvent the problem if possible
     * Another strange effect is, that some file systems do match lowercase with uppercase (like in windows),
     * so testing skill.exists() would return true even if the name does not exist exactly as given in the file system.
     * @param languagepath
     * @param skill_name
     * @return the actual skill file if one exist or a skill file that is constructed from language and skill_name
     */
    public static File getSkillFileInLanguage(File languagepath, String skill_name, boolean null_if_not_found) {

        String fn = skill_name + ".txt";
        String[] list = languagepath.list();
        
        // first try: the skill name may be same or similar to the skill file name
        for (String n: list) {
            if (n.equals(fn) || n.toLowerCase().equals(fn)) {
                return new File(languagepath, n);
            }
        }
        
        // second try: the skill name may be same or similar to the skill name within the skill description
        // this is costly: we must parse the whole skill file
        for (String n: list) {
            if (!n.endsWith(".txt") && !n.endsWith(".ezd")) continue;
            File f = new File(languagepath, n);
            try {
                SusiSkill.ID skillid = new SusiSkill.ID(f);
                SusiLanguage language = skillid.language();
                JSONObject json = SusiSkill.readLoTSkill(new BufferedReader(new FileReader(f)), language, Integer.toString(skillid.hashCode()));
                String sn = json.optString("skill_name");
                if (sn.equals(skill_name) || sn.toLowerCase().equals(skill_name) || sn.toLowerCase().replace(' ', '_').equals(skill_name)) {
                    return new File(languagepath, n);
                }
            } catch (JSONException | FileNotFoundException e) {
                continue;
            }
        }
        
        // the final attempt is bad and may not succeed, but it's the only last thing left we could do.
        return null_if_not_found ? null : new File(languagepath, fn);
    }
    
    /**
     * the following method scans a given model for all files to see if it matches the skill name
     * @param model a path to a model directory
     * @param skill_name
     * @return
     */
    public static File getSkillFileInModel(File model, String skill_name) {
        String[] groups = model.list();
        for (String group: groups) {
            if (group.startsWith(".")) continue;
            File gf = new File(model, group);
            if (!gf.isDirectory()) continue;
            String[] languages = gf.list();
            for (String language: languages) {
                if (language.startsWith(".")) continue;
                File l = new File(gf, language);
                if (!l.isDirectory()) continue;
                File skill = getSkillFileInLanguage(l, skill_name, true);
                if (skill != null) return skill;
            }
        }
        return null;
    }
    
    public static JSONObject getSkillMetadata(String model, String group, String language, String skillname) {

        JSONObject skillMetadata = new JSONObject(true)
                .put("model", model)
                .put("group", group)
                .put("language", language);
        File modelpath = new File(DAO.model_watch_dir, model);
        File grouppath = new File(modelpath, group);
        File languagepath = new File(grouppath, language);
        File skillpath = getSkillFileInLanguage(languagepath, skillname, false);
        skillname = skillpath.getName().replaceAll(".txt", ""); // fixes the bad name (lowercased) to the actual right name
        
        // default values
        skillMetadata.put("developer_privacy_policy", JSONObject.NULL);
        skillMetadata.put("descriptions",JSONObject.NULL);
        skillMetadata.put("image", JSONObject.NULL);
        skillMetadata.put("author", JSONObject.NULL);
        skillMetadata.put("author_url", JSONObject.NULL);
        skillMetadata.put("skill_name", JSONObject.NULL);
        skillMetadata.put("terms_of_use", JSONObject.NULL);
        skillMetadata.put("dynamic_content", false);
        skillMetadata.put("examples", JSONObject.NULL);
        skillMetadata.put("skill_rating", JSONObject.NULL);
        
        // metadata
        for (Map.Entry<SusiSkill.ID, SusiSkill> entry : DAO.susi.getSkillMetadata().entrySet()) {
            SusiSkill skill = entry.getValue();
            SusiSkill.ID skillid = entry.getKey();
            if (skillid.hasModel(model) &&
                skillid.hasGroup(group) &&
                skillid.hasLanguage(language) &&
                skillid.hasName(skillname)) {

                skillMetadata.put("skill_name", skill.getSkillName() ==null ? JSONObject.NULL: skill.getSkillName());
                skillMetadata.put("developer_privacy_policy", skill.getDeveloperPrivacyPolicy() ==null ? JSONObject.NULL:skill.getDeveloperPrivacyPolicy());
                skillMetadata.put("descriptions", skill.getDescription() ==null ? JSONObject.NULL:skill.getDescription());
                skillMetadata.put("image", skill.getImage() ==null ? JSONObject.NULL: skill.getImage());
                skillMetadata.put("author", skill.getAuthor()  ==null ? JSONObject.NULL:skill.getAuthor());
                skillMetadata.put("author_url", skill.getAuthorURL() ==null ? JSONObject.NULL:skill.getAuthorURL());
                skillMetadata.put("terms_of_use", skill.getTermsOfUse() ==null ? JSONObject.NULL:skill.getTermsOfUse());
                skillMetadata.put("dynamic_content", skill.getDynamicContent());
                skillMetadata.put("examples", skill.getExamples() ==null ? JSONObject.NULL: skill.getExamples());
                
            }
        }
        
        // rating
        JsonTray skillRating = DAO.skillRating;
        if (skillRating.has(model)) {
            JSONObject modelName = skillRating.getJSONObject(model);
            if (modelName.has(group)) {
                JSONObject groupName = modelName.getJSONObject(group);
                if (groupName.has(language)) {
                    JSONObject languageName = groupName.getJSONObject(language);
                    if (languageName.has(skillname)) {
                        JSONObject skillName = languageName.getJSONObject(skillname);
                        skillMetadata.put("skill_rating", skillName);
                    }
                }
            }
        }
        
        // file attributes
        BasicFileAttributes attr = null;
        Path p = Paths.get(skillpath.getPath());
        try {
            attr = Files.readAttributes(p, BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(attr!=null){
            skillMetadata.put("creationTime: " , attr.creationTime());
            skillMetadata.put("lastAccessTime: " , attr.lastAccessTime());
            skillMetadata.put("lastModifiedTime: " , attr.lastModifiedTime());
        }
        return skillMetadata;
    }

    public static JSONObject readJsonSkill(File file) throws JSONException, FileNotFoundException {
        JSONObject json = new JSONObject(new JSONTokener(new FileReader(file)));
        //System.out.println(json.toString(2)); // debug
        return json;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }

    public void setAuthorURL(String authorURL) {
        this.authorURL = authorURL;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void setDeveloperPrivacyPolicy(String developerPrivacyPolicy) {
        this.developerPrivacyPolicy = developerPrivacyPolicy;
    }

    public void setExamples(Set<String> examples) {
        this.examples = examples;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public void setDynamicContent(Boolean dynamicContent) {
        this.dynamicContent = dynamicContent;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public void setTermsOfUse(String termsOfUse) {
        this.termsOfUse = termsOfUse;
    }

    public String getDescription() {
        return description;
    }

    public Set<String> getTags() {
        return tags;
    }

    public String getAuthor() {
        if (author!=null) {
            return author.toLowerCase();
        }
        else {
            return author;
        }
    }

    public String getAuthorURL() {
        return authorURL;
    }

    public String getImage() {
        return image;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public Set<String> getExamples() {
        return examples;
    }

    public String getDeveloperPrivacyPolicy() {
        return developerPrivacyPolicy;
    }

    public Boolean getDynamicContent() {
        return dynamicContent;
    }
    
    public JSONObject toJSON() {
        JSONObject json = new JSONObject(true);
        if (this.description != null) json.put("description", this.description);
        if (this.image != null) json.put("image", this.image);
        if (this.skillName != null) json.put("skill_name", this.skillName);
        if (this.author != null) json.put("author", this.author);
        if (this.authorURL != null) json.put("author_url", this.authorURL);
        if (this.developerPrivacyPolicy != null) json.put("developer_privacy_policy", this.developerPrivacyPolicy);
        if (this.termsOfUse != null) json.put("terms_of_use", this.termsOfUse);
        if (this.dynamicContent != null) json.put("dynamic_content", this.dynamicContent);
        if (this.tags != null && this.tags.size() > 0) json.put("tags", this.tags);
        return json;
    }
    
    public static void main(String[] args) {
        Path data = FileSystems.getDefault().getPath("data");
        Map<String, String> config;
        try {config = SusiServer.readConfig(data);DAO.init(config, data);} catch (Exception e) {e.printStackTrace();}
        File model = new File(DAO.model_watch_dir, "persona");
        File skill = SusiSkill.getSkillFileInModel(model, "nefertiti");
        System.out.println(skill);
        System.exit(0);
    }
}
