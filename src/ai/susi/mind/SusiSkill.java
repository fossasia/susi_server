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
import java.util.LinkedHashSet;
import java.util.Set;

import org.jfree.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import ai.susi.DAO;
import ai.susi.mind.SusiAction.SusiActionException;
import ai.susi.mind.SusiInference.Type;

/**
 * A susi skill is a set of intents.
 * This class provides parser methods for such sets, given as text files
 */
public class SusiSkill {

    private String skillName;
    private Boolean protectedSkill;
    private String description;
    private String author;
    private String authorURL;
    private String authorEmail;
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
        public boolean equals(Object o) {
            return o instanceof ID && ((ID) o).skillpath.equals(this.skillpath);
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
        this.authorEmail = null;
        this.description = null;
        this.examples = new LinkedHashSet<>();
        this.image = null;
        this.skillName = null;
        this.protectedSkill = false;
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
    public static JSONObject readLoTSkill(final BufferedReader br, final SusiLanguage language, final String skillidname, boolean acceptWildcardIntent) throws JSONException {
        // read the text file and turn it into a intent json; then learn that
        JSONObject json = new JSONObject(true);
        JSONArray intents = new JSONArray();
        String lastLine = "", line = "";
        String bang_answers = "", bang_type = "", bang_term = "", example = "", expect = "", label = "", implication = ""; 
        StringBuilder bang_bag = new StringBuilder();
        boolean prior = false, dynamicContent = false, protectedSkill = false;
        int indentStep = 4; // like in python
        try {readloop: while ((line = br.readLine()) != null) {
            String linebeforetrim = line;
            line = line.trim();
            int depth = linebeforetrim.indexOf(line) / indentStep; // the last line of an intent therefore declares the depth

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
                        for (String phrase: bang_answers.split("\\|")) {
                            JSONObject simplePhrase = SusiUtterance.simplePhrase(phrase.trim(), prior);
                            if (!acceptWildcardIntent && SusiUtterance.isCatchallPhrase(simplePhrase)) {
                                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                                continue readloop;
                            } else {
                                phrases.put(simplePhrase);
                            }
                        }

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
                        if (expect.length() > 0) intent.put("expect", expect);
                        if (label.length() > 0) intent.put("label", label);
                        if (implication.length() > 0) intent.put("implication", implication);
                        intent.put("depth", depth);
                        extendParentWithAnswer(intents, intent);
                        intents.put(intent);
                    }
                    else if (bang_type.equals("console")) {
                        // create a console intent
                        JSONObject intent = new JSONObject(true);
                        JSONArray phrases = new JSONArray();
                        intent.put("phrases", phrases);
                        for (String phrase: bang_answers.split("\\|")) {
                            JSONObject simplePhrase = SusiUtterance.simplePhrase(phrase.trim(), prior);
                            if (!acceptWildcardIntent && SusiUtterance.isCatchallPhrase(simplePhrase)) {
                                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                                continue readloop;
                            } else {
                                phrases.put(simplePhrase);
                            }
                        }

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
                        
                        // validate additional data object: it must be an array
                        if (definition.has("data")) {
                            Object o = definition.get("data");
                            if (!(o instanceof JSONArray)) definition.remove("data");
                        }

                        // answers; must contain names from the console result array
                        if (bang_term.length() > 0) {
                            actions.put(SusiAction.answerAction(language, bang_term.split("\\|")));
                        }
                        if (example.length() > 0) intent.put("example", example);
                        if (expect.length() > 0) intent.put("expect", expect);
                        if (label.length() > 0) intent.put("label", label);
                        if (implication.length() > 0) intent.put("implication", implication);
                        intent.put("depth", depth);
                        extendParentWithAnswer(intents, intent);
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
                int thenpos = -1;
                if (line.startsWith("::minor")) prior = false;
                if (line.startsWith("::prior")) prior = true;
                if (line.startsWith("::description") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("description", meta);
                }
                if (line.startsWith("::image") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("image", meta);
                }
                if (line.startsWith("::name") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("skill_name", meta);
                }
                if (line.startsWith("::protected") && (thenpos = line.indexOf(' ')) > 0) {
                    if (line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) protectedSkill=true;
                    json.put("protected", protectedSkill);
                }
                if (line.startsWith("::author") && (!line.startsWith("::author_url")) && (!line.startsWith("::author_email")) && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("author", meta);
                }
                if (line.startsWith("::author_email") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("author_email", meta);
                }
                if (line.startsWith("::author_url") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("author_url", meta);
                }
                if (line.startsWith("::developer_privacy_policy") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("developer_privacy_policy", meta);
                }
                if (line.startsWith("::terms_of_use") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("terms_of_use", meta);
                }
                if (line.startsWith("::dynamic_content") && (thenpos = line.indexOf(' ')) > 0) {
                    if (line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) dynamicContent = true;
                    json.put("dynamic_content", dynamicContent);
                }
                if (line.startsWith("::tags") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("tags", meta);
                }
                if (line.startsWith("::kickoff") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    if (meta.length() > 0) json.put("kickoff", meta);
                }

                lastLine = ""; example = ""; expect = ""; label = ""; implication = "";
                continue readloop;
            }

            if (line.startsWith("#")) {
                // a comment line; ignore the line and consider it as whitespace
                lastLine = ""; example = ""; expect = ""; label = ""; implication = "";
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
                            JSONObject intent = SusiIntent.answerIntent(phrases, "IF " + condition, answers, prior, depth, example, expect, label, implication, language);
                            if (!acceptWildcardIntent && SusiIntent.isCatchallIntent(intent)) {
                                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                                continue readloop;
                            } else {
                                extendParentWithAnswer(intents, intent);
                                intents.put(intent);
                            }
                        }
                    } else {
                        String ifsubstring = line.substring(thenpos + 1, elsepos).trim();
                        if (ifsubstring.length() > 0) {
                            String[] ifanswers = ifsubstring.split("\\|");
                            JSONObject intentif = SusiIntent.answerIntent(phrases, "IF " + condition, ifanswers, prior, depth, example, expect, label, implication, language);
                            if (!acceptWildcardIntent && SusiIntent.isCatchallIntent(intentif)) {
                                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                                continue readloop;
                            } else {
                                extendParentWithAnswer(intents, intentif);
                                intents.put(intentif);
                            }
                        }
                        String elsesubstring = line.substring(elsepos + 1).trim();
                        if (elsesubstring.length() > 0) {
                            String[] elseanswers = elsesubstring.split("\\|");
                            JSONObject intentelse = SusiIntent.answerIntent(phrases, "NOT " + condition, elseanswers, prior, depth, example, expect, label, implication, language);
                            if (!acceptWildcardIntent && SusiIntent.isCatchallIntent(intentelse)) {
                                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                                continue readloop;
                            } else {
                                extendParentWithAnswer(intents, intentelse);
                                intents.put(intentelse);
                            }
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
                    } else if (head.equals("label")) {
                        label = tail;
                    } else if (head.equals("implication")) {
                        implication = tail;
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
                    JSONObject intent = SusiIntent.answerIntent(phrases, condition, answers, prior, depth, example, expect, label, implication, language);
                    if (!acceptWildcardIntent && SusiIntent.isCatchallIntent(intent)) {
                        DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                        continue readloop;
                    } else {
                        extendParentWithAnswer(intents, intent);
                        //System.out.println(intent.toString());
                        intents.put(intent);
                    }
                    example = ""; expect = ""; label = ""; implication = "";
                }
            }
            lastLine = line;
        }} catch (IOException e) {
            DAO.log(e.getMessage());
        }
        json.put("intents", intents);
        return json;
    }

    private static JSONObject lastIntentWithDepth(JSONArray intents, int depth) {
        for (int i = intents.length() - 1; i >= 0; i--) {
            JSONObject intent = intents.getJSONObject(i);
            int d = intent.optInt("depth", -1);
            if (d > depth) continue;
            if (d == depth) return intent;
            if (d < depth) return null;
        }
        return null;
    }
    
    private static void extendParentWithAnswer(JSONArray intents, JSONObject intent) {
        JSONArray utterances = intent.getJSONArray("phrases");
        if (utterances == null || utterances.length() != 1) return;
        String utterance = utterances.getJSONObject(0).getString("expression");
        if (utterance.indexOf('*') >= 0) return;
        int depth = intent.optInt("depth", -1);
        if (depth <= 0) return;
        JSONObject parent = lastIntentWithDepth(intents, depth - 1);
        if (parent == null) return;
        
        // we have found a parent and we want to add the utterance as cue
        JSONArray cues = parent.optJSONArray("cues");
        if (cues == null) {
            cues = new JSONArray();
            parent.put("cues", cues);
        }
        cues.put(utterance);
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setAuthorURL(String authorURL) {
        this.authorURL = authorURL;
    }

    public void setAuthorEmail(String authorEmail) {
        this.authorEmail = authorEmail;
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
/*
    public void setExamples(Set<String> examples) {
        this.examples = examples;
    }
*/

    public void setImage(String image) {
        this.image = image;
    }

    public void setDynamicContent(Boolean dynamicContent) {
        this.dynamicContent = dynamicContent;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public void setProtectedSkill(Boolean protectedSkill) {
        this.protectedSkill = protectedSkill;
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
        return author;
    }

    public String getAuthorURL() {
        return authorURL;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public String getImage() {
        return image;
    }

    public String getSkillName() {
        return skillName;
    }

    public Boolean getProtectedSkill() {
        return protectedSkill;
    }

    public String getTermsOfUse() {
        return termsOfUse;
    }

    public Set<String> getExamples() {
        return examples;
    }

    public void addExample(String s) {
        if (this.examples == null) this.examples = new LinkedHashSet<>();
        this.examples.add(s);
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
        if (this.protectedSkill != null) json.put("protected", this.protectedSkill);
        if (this.author != null) json.put("author", this.author);
        if (this.authorURL != null) json.put("author_url", this.authorURL);
        if (this.authorEmail != null) json.put("author_email", this.authorEmail);
        if (this.developerPrivacyPolicy != null) json.put("developer_privacy_policy", this.developerPrivacyPolicy);
        if (this.termsOfUse != null) json.put("terms_of_use", this.termsOfUse);
        if (this.dynamicContent != null) json.put("dynamic_content", this.dynamicContent);
        if (this.tags != null && this.tags.size() > 0) json.put("tags", this.tags);
        return json;
    }

    public static void main(String[] args) {
        //Path data = FileSystems.getDefault().getPath("data");
        //Map<String, String> config;
        //try {config = SusiServer.readConfig(data);DAO.init(config, data);} catch (Exception e) {e.printStackTrace();}
        File system_skills_test = new File(new File(FileSystems.getDefault().getPath("conf").toFile(), "system_skills"), "test");
        File skill = new File(system_skills_test, "dialog.txt");
        //File model = new File(DAO.model_watch_dir, "general");
        //File skill = SusiSkill.getSkillFileInModel(model, "Westworld");
        System.out.println(skill);
        SusiSkill.ID skillid = new SusiSkill.ID(skill);
        SusiLanguage language = skillid.language();
        try {
            JSONObject lesson = SusiSkill.readLoTSkill(new BufferedReader(new FileReader(skill)), language, skillid.skillpath, false);
            System.out.println(lesson.toString(2));
        } catch (JSONException | FileNotFoundException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}