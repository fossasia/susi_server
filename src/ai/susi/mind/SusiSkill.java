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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    public final static String SKILL_SOURCE_PREFIX_SUSI_SERVER = "/susi_server";
    public final static String SKILL_SOURCE_PREFIX_SUSI_SKILL_DATA = "/susi_skill_data";
    public final static String SKILL_SOURCE_PREFIX_INSTANT = "/instant";

    private String skillName, description, author, authorURL, authorEmail, image;
    private String termsOfUse, kickoff, developerPrivacyPolicy;
    private String[] on;
    private Boolean protectedSkill, dynamicContent;
    private Set<String> examples, tags;
    private List<SusiIntent> intents = new ArrayList<>();
    private SusiSkill.ID id;
    
    public static class ID implements Comparable<ID> {
        private String skillpath;
        private final String[] possible_path_prefixes = new String[] {
                SKILL_SOURCE_PREFIX_SUSI_SERVER, SKILL_SOURCE_PREFIX_SUSI_SKILL_DATA};

        /**
         * compute the skill path from the origin file
         * @param origin
         * @return a relative path to the skill location, based on the git repository
         */
        public ID(File origin) throws UnsupportedOperationException {
            this.skillpath = origin.getAbsolutePath().replace('\\', '/');
            // The skillpath must start with the root path of either the susi_skill_data git repository or of susi_server git repository.
            // In both cases the path must start with a "/".

            boolean found = false;
            prefixes: for (String prefix: possible_path_prefixes) {
                int i = this.skillpath.indexOf(prefix);
                if (i >= 0) {
                    this.skillpath = this.skillpath.substring(i);
                    found = true;
                    break prefixes;
                }
            }
            if (!found) {
                throw new UnsupportedOperationException("the file path does not point to a susi skill model repository: " + origin.getAbsolutePath());
            }
        }

        /**
         * create an instant skill id
         * @param language
         * @param name a name of the skill without path information
         * @throws UnsupportedOperationException
         */
        public ID(final SusiLanguage language, final String name) {
            this.skillpath = SKILL_SOURCE_PREFIX_INSTANT + "/" + language.name() + "/" + name.replace('/', '_');
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
            if (this.skillpath.startsWith(SKILL_SOURCE_PREFIX_SUSI_SERVER + "/conf/") ||
                this.skillpath.startsWith(SKILL_SOURCE_PREFIX_INSTANT)) {
                int p = this.skillpath.lastIndexOf('/');
                SusiLanguage language = SusiLanguage.parse(this.skillpath.substring(p - 2, p));
                return language;
            }
            if (this.skillpath.startsWith(SKILL_SOURCE_PREFIX_SUSI_SKILL_DATA)) {
                SusiLanguage language = SusiLanguage.unknown;
                String[] paths = this.skillpath.split("/");
                if (paths.length > 5) language = SusiLanguage.parse(paths[5]);
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

    public SusiSkill() {
        this.skillName = null;
        this.description = null;
        this.author = null;
        this.authorURL = null;
        this.authorEmail = null;
        this.image = null;
        this.termsOfUse = null;
        this.kickoff = null;
        this.developerPrivacyPolicy = null;
        this.id = null;
        this.on = null;
        this.protectedSkill = false;
        this.dynamicContent = false;
        this.examples = new LinkedHashSet<>();
        this.tags = new LinkedHashSet<>();
        this.intents = new ArrayList<>();
    }

    /**
     * read a text skill file (once called "easy dialog - EzD")
     * @param br a buffered reader
     * @return a skill object as JSON
     * @throws JSONException
     * @throws SusiActionException 
     */
    public SusiSkill(
            final BufferedReader br,
            final SusiSkill.ID skillid,
            boolean acceptWildcardIntent) throws JSONException, SusiActionException {
        this();
        this.id = skillid;
        // read the text file and turn it into a intent json; then learn that
        String lastLine = "", line = "";
        String bang_utterances = "", bang_type = "", bang_answers = "", example = "", expect = "", label = "", implication = "";
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
                if (!line.toLowerCase().equals("eol")) {
                    bang_bag.append(line).append('\n');
                    continue readloop;
                }

                // the line is "eol"; stop collection
                if (bang_type.equals("javascript")) {
                    // create a javascript intent
                    List<SusiUtterance> utterances = phrasesFromWildcard(skillid.getPath(), acceptWildcardIntent, bang_utterances, prior);
                    if (utterances == null) continue readloop;

                    // javascript process
                    SusiInference inference = new SusiInference(bang_bag.toString(), Type.javascript);
                    List<SusiInference> inferences = new ArrayList<>(1);
                    inferences.add(inference);

                    // answers; must contain $!$
                    SusiAction action = new SusiAction(SusiAction.answerAction(this.id.language(), bang_answers.split("\\|")));
                    List<SusiAction> actions = new ArrayList<>(1);
                    actions.add(action);

                    SusiIntent intent = new SusiIntent(utterances, inferences, actions, prior, depth, example, expect, label, implication, skillid);

                    extendParentWithAnswer(this.intents, intent);
                    this.intents.add(intent);
                } else if (bang_type.equals("console")) {
                    // create a console intent
                    List<SusiUtterance> utterances = phrasesFromWildcard(skillid.getPath(), acceptWildcardIntent, bang_utterances, prior);
                    if (utterances == null) continue readloop;

                    // console process
                    JSONObject definition;
                    SusiInference inference;
                    try {
                        definition = new JSONObject(new JSONTokener(bang_bag.toString()));
                        inference = new SusiInference(definition, Type.console);
                    } catch (JSONException e) {
                        throw new JSONException(e.getMessage() + " \"" + bang_bag.toString() + "\"");
                    }
                    List<SusiInference> inferences = new ArrayList<>(1);
                    inferences.add(inference);

                    // actions; we may have several actions here
                    List<SusiAction> actions = new ArrayList<>();

                    // verify actions
                    if (definition.has("actions")) {
                        JSONArray bo_actions = definition.getJSONArray("actions");
                        bo_actions.forEach(action -> {
                            try {
                                actions.add(new SusiAction((JSONObject) action));
                            } catch (SusiActionException e) {
                                DAO.severe(e.getMessage());
                            }
                        });
                    }

                    // validate additional data object: it must be an array
                    if (definition.has("data")) {
                        Object o = definition.get("data");
                        if (!(o instanceof JSONArray)) definition.remove("data");
                    }

                    // answers; must contain names from the console result array
                    if (bang_answers.length() > 0) try {
                        actions.add(new SusiAction(SusiAction.answerAction(this.id.language(), bang_answers.split("\\|"))));
                    } catch (SusiActionException e) {
                        DAO.severe(e.getMessage());
                    }

                    SusiIntent intent = new SusiIntent(utterances, inferences, actions, prior, depth, example, expect, label, implication, skillid);

                    extendParentWithAnswer(this.intents, intent);
                    this.intents.add(intent);
                }
                // if there is a different bang type, just ignore it.
                bang_utterances = "";
                bang_type = "";
                bang_answers = "";
                bang_bag.setLength(0);
                continue readloop;
            }

            // read metadata
            if (line.startsWith("::")) {
                int thenpos = -1;
                if (line.startsWith("::minor")) prior = false;
                if (line.startsWith("::prior")) prior = true;
                if (line.startsWith("::on") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    this.on = (meta.length() > 0) ? meta.split(",") : new String[0];
                }
                if (line.startsWith("::description") && (thenpos = line.indexOf(' ')) > 0) {
                    this.description = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::image") && (thenpos = line.indexOf(' ')) > 0) {
                    this.image = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::name") && (thenpos = line.indexOf(' ')) > 0) {
                    this.skillName = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::protected") && (thenpos = line.indexOf(' ')) > 0) {
                    if (line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) protectedSkill=true;
                    this.protectedSkill = protectedSkill;
                }
                if (line.startsWith("::author") && (!line.startsWith("::author_url")) && (!line.startsWith("::author_email")) && (thenpos = line.indexOf(' ')) > 0) {
                    this.author = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::author_email") && (thenpos = line.indexOf(' ')) > 0) {
                    this.authorEmail = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::author_url") && (thenpos = line.indexOf(' ')) > 0) {
                    this.authorURL = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::developer_privacy_policy") && (thenpos = line.indexOf(' ')) > 0) {
                    this.developerPrivacyPolicy = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::terms_of_use") && (thenpos = line.indexOf(' ')) > 0) {
                    this.termsOfUse = line.substring(thenpos + 1).trim();
                }
                if (line.startsWith("::dynamic_content") && (thenpos = line.indexOf(' ')) > 0) {
                    if (line.substring(thenpos + 1).trim().equalsIgnoreCase("yes")) dynamicContent = true;
                    this.dynamicContent = dynamicContent;
                }
                if (line.startsWith("::tags") && (thenpos = line.indexOf(' ')) > 0) {
                    String meta = line.substring(thenpos + 1).trim();
                    this.tags = new LinkedHashSet<>();
                    if (meta.length() > 0) {
                        for (String s: meta.split(",")) this.tags.add(s);
                    }
                }
                if (line.startsWith("::kickoff") && (thenpos = line.indexOf(' ')) > 0) {
                    this.kickoff = line.substring(thenpos + 1).trim();
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
                        if (addIntent(this.intents, acceptWildcardIntent, this.id, example, expect, label, implication, prior, depth, phrases, condition, ifsubstring))
                            continue readloop;
                    } else {
                        String ifsubstring = line.substring(thenpos + 1, elsepos).trim();
                        if (addIntent(this.intents, acceptWildcardIntent, this.id, example, expect, label, implication, prior, depth, phrases, condition, ifsubstring))
                            continue readloop;
                        String elsesubstring = line.substring(elsepos + 1).trim();
                        if (elsesubstring.length() > 0) {
                            String[] elseanswers = elsesubstring.split("\\|");
                            SusiIntent intentelse = new SusiIntent(phrases, "NOT " + condition, elseanswers, prior, depth, example, expect, label, implication, skillid);
                            if (!acceptWildcardIntent && intentelse.isCatchallIntent()) {
                                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillid.getPath());
                                continue readloop;
                            } else {
                                extendParentWithAnswer(this.intents, intentelse);
                                this.intents.add(intentelse);
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
                        bang_utterances = lastLine;
                        bang_type = head;
                        bang_answers = tail;
                        bang_bag.setLength(0);
                    }
                    continue readloop;
                } else {
                    String[] answers = line.split("\\|");
                    SusiIntent intent = new SusiIntent(phrases, condition, answers, prior, depth, example, expect, label, implication, skillid);
                    if (!acceptWildcardIntent && intent.isCatchallIntent()) {
                        DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillid.getPath());
                        continue readloop;
                    } else {
                        extendParentWithAnswer(this.intents, intent);
                        //System.out.println(intent.toString());
                        this.intents.add(intent);
                    }
                    example = ""; expect = ""; label = ""; implication = "";
                }
            }
            lastLine = line;
        }} catch (IOException e) {
            DAO.log(e.getMessage());
        }
    }

    /**
     * parse an utterance declaration
     * @param skillidname
     * @param acceptWildcardIntent
     * @param utterances_declaration
     * @param prior
     * @return a list of compiled utterances
     */
    private static List<SusiUtterance> phrasesFromWildcard(String skillidname, boolean acceptWildcardIntent, String utterances_declaration, boolean prior) {
        List<SusiUtterance> utterances = new ArrayList<>();
        for (String u: utterances_declaration.split("\\|")) {
            SusiUtterance utterance = new SusiUtterance(u.trim(), prior);
            if (!acceptWildcardIntent && utterance.isCatchallPhrase()) {
                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillidname);
                continue;
            } else {
                utterances.add(utterance);
            }
        }
        return utterances;
    }

    private static boolean addIntent(
            final List<SusiIntent> existing_intents_in_skill,
            final boolean acceptWildcardIntent,
            final SusiSkill.ID skillid,
            final String example,
            final String expect,
            final String label,
            final String implication,
            final boolean prior,
            final int depth,
            final String[] phrases,
            final String condition,
            final String ifsubstring) throws SusiActionException {
        if (ifsubstring.length() > 0) {
            String[] answers = ifsubstring.split("\\|");
            SusiIntent intent = new SusiIntent(phrases, "IF " + condition, answers, prior, depth, example, expect, label, implication, skillid);
            if (!acceptWildcardIntent && intent.isCatchallIntent()) {
                DAO.log("WARNING: skipping skill / wildcard not allowed here: " + skillid.getPath());
                return true;
            } else {
                extendParentWithAnswer(existing_intents_in_skill, intent);
                existing_intents_in_skill.add(intent);
            }
        }
        return false;
    }

    /**
     * Attach a child intent to a parent intent:
     *  - the parent gets a "cues" object which contains possible utterances
     * @param preceding_intents the array of parent intents
     * @param child_intent the child intent
     */
    private static void extendParentWithAnswer(List<SusiIntent> preceding_intents, SusiIntent child_intent) {

        // check if the child is qualified - if it is actually a child or a parent on root level
        int depth = child_intent.getDepth();
        if (depth <= 0) return; // not a child

        // find a parent which is on the previous depth level
        SusiIntent parent_intent = lastIntentWithDepth(preceding_intents, depth - 1);
        if (parent_intent == null) return; // no parent found

        // TODO: add hierarchy linking here:
        // - get ID of parent
        int parent_id = parent_intent.hashCode();

        // - set an invisible assignment in parent for linking variable to ID
        parent_intent.addInferences(new SusiInference("SET ", SusiInference.Type.memory));

        // - add a check rule in child so a child fires only if linking ID is set correctly

        // get expressions from child utterances.
        List<SusiUtterance> child_utterances = child_intent.getUtterances();
        if (child_utterances == null || child_utterances.size() != 1) return;
        String child_expression = child_utterances.get(0).getPattern().pattern();
        // We cannot accept utterances with wildcard expressions here because cues are answers that are presented inside the chat
        if (child_expression.indexOf('*') >= 0) return;
        parent_intent.addCues(child_expression);
    }

    /**
     * Find an intent with a specific depth
     * @param intents the set of intents
     * @param depth the required depth
     * @return the intent which has the given depth
     */
    private static SusiIntent lastIntentWithDepth(List<SusiIntent> intents, int depth) {
        // iterate from back to front
        for (int i = intents.size() - 1; i >= 0; i--) {
            SusiIntent intent = intents.get(i);
            int d = intent.getDepth();
            if (d > depth) continue; // not yet on required depth, continue
            if (d == depth) return intent; // found the match
            if (d < depth) return null; // we missed the parent, maybe there is none. Then we don't return one
        }
        // the list does not contain any appropriate parent
        return null;
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

    public void setOn(JSONArray on) {
        this.on = new String[on.length()];
        for (int i = 0; i < on.length(); i++) this.on[i] = on.getString(i);
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

    public String[] getOn() {
        return this.on;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }
    
    public List<SusiIntent> getIntents() {
        return this.intents;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getAuthorURL() {
        return this.authorURL;
    }

    public String getAuthorEmail() {
        return this.authorEmail;
    }

    public String getImage() {
        return this.image;
    }

    public String getSkillName() {
        return this.skillName;
    }

    public Boolean getProtectedSkill() {
        return this.protectedSkill;
    }

    public String getTermsOfUse() {
        return this.termsOfUse;
    }

    public SusiSkill.ID getID() {
        return this.id;
    }

    public Set<String> getExamples() {
        return this.examples;
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
        if (this.skillName != null && this.skillName.length() > 0) json.put("skill_name", this.skillName);
        if (this.description != null && this.description.length() > 0) json.put("description", this.description);
        if (this.author != null && this.author.length() > 0) json.put("author", this.author);
        if (this.authorURL != null && this.authorURL.length() > 0) json.put("author_url", this.authorURL);
        if (this.authorEmail != null && this.authorEmail.length() > 0) json.put("author_email", this.authorEmail);
        if (this.image != null && this.image.length() > 0) json.put("image", this.image);
        if (this.termsOfUse != null && this.termsOfUse.length() > 0) json.put("terms_of_use", this.termsOfUse);
        if (this.kickoff != null && this.kickoff.length() > 0) json.put("kickoff", this.kickoff);
        if (this.developerPrivacyPolicy != null && this.developerPrivacyPolicy.length() > 0) json.put("developer_privacy_policy", this.developerPrivacyPolicy);
        if (this.id != null) json.put("origin", this.id.getPath());
        if (this.on != null && this.on.length > 0) json.put("on", new JSONArray(this.on));
        if (this.protectedSkill != null) json.put("protected", this.protectedSkill.booleanValue());
        if (this.dynamicContent != null) json.put("dynamic_content", this.dynamicContent.booleanValue());
        if (this.examples != null && this.examples.size() > 0) json.put("examples", new JSONArray(this.examples));
        if (this.tags != null && this.tags.size() > 0) json.put("tags", new JSONArray(this.tags));
        if (this.intents != null && this.intents.size() > 0) {
            JSONArray i = new JSONArray();
            for (SusiIntent si: this.intents) i.put(si.toJSON());
            json.put("intents", i);
        }
        return json;
    }

    public static void main(String[] args) {
        //Path data = FileSystems.getDefault().getPath("data");
        //Map<String, String> config;
        //try {config = SusiServer.readConfig(data);DAO.init(config, data);} catch (Exception e) {e.printStackTrace();}
        File system_skills_test = new File(new File(FileSystems.getDefault().getPath("conf").toFile(), "os_skills"), "test");
        File skillFile = new File(system_skills_test, "dialog.txt");
        //File model = new File(DAO.model_watch_dir, "general");
        //File skill = SusiSkill.getSkillFileInModel(model, "Westworld");
        System.out.println(skillFile);
        SusiSkill.ID skillid = new SusiSkill.ID(skillFile);
        try {
            SusiSkill skill = new SusiSkill(new BufferedReader(new FileReader(skillFile)), skillid, false);
            System.out.println(skill.toString());
        } catch (JSONException | FileNotFoundException | SusiActionException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
