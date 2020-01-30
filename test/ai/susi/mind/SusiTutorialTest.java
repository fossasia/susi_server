package ai.susi.mind;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ai.susi.DAO;
import ai.susi.SusiServer;
import ai.susi.mind.SusiCognition;
import ai.susi.server.ClientIdentity;

public class SusiTutorialTest {

    private final BufferedReader getTestReader() {
        ByteArrayInputStream bais = new ByteArrayInputStream(testFile.getBytes(StandardCharsets.UTF_8));
        return new BufferedReader(new InputStreamReader(bais, StandardCharsets.UTF_8));
    }

    public static String susiAnswer(String q, ClientIdentity identity) {
        // creating a cognition means that an answer is computed
        SusiCognition cognition = new SusiCognition(q, "", 0, 0, 0, "", "", "en", "Others", identity, true, DAO.susi);
        // evaluate the cognition, the answer is already inside!
        try {
            // memorize the cognition, this is needed to compute context-aware intents.
            // cognitions must be stored together with answers because they may influence further cognitions
            DAO.susi_memory.addCognition(identity.getClient(), cognition, true);
            // get the answer
            List<SusiThought> answers = cognition.getAnswers();
            assert answers.size() > 0 : "no answer for q = " + q;
            assertTrue("no answer for q = " + q, answers.size() > 0);
            SusiThought thought = answers.iterator().next();
            List<SusiAction> actions = thought.getActions(false);
            assert actions.size() > 0;
            assertTrue("no actions computed", actions.size() > 0);
            SusiAction action = actions.iterator().next();
            String answer = action.getPhrases().iterator().next();
            return answer;
        } catch (JSONException | IOException e) {
            System.out.println("json not well-formed: " + cognition.getJSON());
            e.printStackTrace();
            return null;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        // System.out.println(testFile); // helper to generate a test dream "testdream"
        try {
            System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff
            Path data = FileSystems.getDefault().getPath("data");
            Map<String, String> config = SusiServer.readConfig(data);

            // initialize all data
            try{
                DAO.init(config, data, false);
                BufferedReader br = getTestReader();
                SusiSkill.ID skillid = new SusiSkill.ID(SusiLanguage.en, "");
                SusiSkill skill = new SusiSkill(br, skillid, true);
                //System.out.println(skill.getIntents().get(0).clone().toString());
                System.out.println(skill.toJSON().toString(2));
                DAO.susi.learn(skill, skillid, true);
                br.close();
            } catch(Exception e){
                e.printStackTrace();
                DAO.severe(e.getMessage());
                DAO.severe("Could not initialize DAO. Exiting.");
                System.exit(-1);
            }

            ClientIdentity identity = ClientIdentity.ANONYMOUS;
            test("[token]", "token is working", identity);
            test("[token2] test", "token2 handles test", identity);
            test("reset test.", "ok", identity);
            test("roses are red", "SUSI is a hack", identity);
            //test("susi is a hack", "skynet is back", identity);
            assertTrue("Potatoes|Vegetables|Fish".indexOf(susiAnswer("What is your favorite dish", identity)) >= 0);
            test("Bonjour", "Hello", identity);
            test("Buenos días", "Hello", identity);
            test("May I work for you?", "Yes you may", identity);
            test("May I get a beer?", "Yes you may get a beer!", identity);
            test("For two dollars I can buy a beer", "Yeah, I believe two dollars is a good price for a beer", identity);
            test("Someday I buy a car", "Sure, you should buy a car!", identity);
            test("I really like bitburger beer", "You then should have one bitburger!", identity);
            test("What beer is the best?", "I bet you like bitburger beer!", identity);
            test("How do I feel?", "I don't know your mood.", identity);
            test("I am getting bored.", "Make something!", identity);
            test("How do I feel?", "You are inactive.", identity);
            test("I am so happy!", "Good for you!", identity);
            test("Shall I eat?", "You will be happy, whatever I say!", identity);
            test("javascript hello", "Hello world from Nashorn", identity);
            test("compute 10 to the power of 3", "10^3 = 1000.0", identity);
            test("who is susi", "nlu success", identity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void test(String q, String e, ClientIdentity i) {
        try {
            String a = susiAnswer(q, i);
            boolean r = a.equals(e);
            if (r) {
                System.out.println("** success for: " + q);
            } else {
                System.out.println("** fail for: " + q);
                System.out.println("** expected: " + e);
                System.out.println("** returned: " + a);
            }
            assertTrue("fail for: " + q + ", expected: " + e + ", returned: " + a, r);
        } catch (JSONException x) {
            x.printStackTrace();
            assertTrue(false);
        }
    }

    private final static String testFile = 
                    "# susi EzD tutorial playground\n" +
                    "::prior\n" +
                    "\n" +
                    "[token]\n" +
                    "token is working\n" +
                    "\n" +
                    "[token2] *\n" +
                    "token2 handles $1$\n" +
                    "\n" +
                    "reset test.\n" +
                    "ok^^>_mood\n" +
                    "\n" +
                    "roses are red\n" +
                    "SUSI is a hack\n" +
                    "skynet is back\n" +
                    "\n" +
                    "What is your favorite dish\n" +
                    "Potatoes|Vegetables|Fish\n" +
                    "\n" +
                    "Bonjour|Buenos días\n" +
                    "Hello\n" +
                    "\n" +
                    "May I * you\n" +
                    "Yes you may\n" +
                    "\n" +
                    "May I get a *?\n" +
                    "Yes you may get a $1$!\n" +
                    "\n" +
                    "For * I can buy a *\n" +
                    "Yeah, I believe $1$ is a good price for a $2$\n" +
                    "\n" +
                    "* buy a *\n" +
                    "Sure, you should buy a $2$!\n" +
                    "\n" +
                    "I * like * beer\n" +
                    "You then should have one $2$>_beerbrand!\n" +
                    "\n" +
                    "* beer * best?\n" +
                    "I bet you like $_beerbrand$ beer!\n" +
                    "\n" +
                    "I am so happy!\n" +
                    "Good for you!^excited^>_mood\n" +
                    "\n" +
                    "I am getting bored.\n" +
                    "Make something!^inactive^>_mood\n" +
                    "\n" +
                    "How do I feel?\n" +
                    "?$_mood$:You are $_mood$.:I don't know your mood.\n" +
                    "\n" +
                    "Shall I *?\n" +
                    "?$_mood$=excited:You will be happy, whatever I say!\n" +
                    "\n" +
                    "javascript hello\n" +
                    "!javascript:$!$ from Nashorn\n" +
                    "print('Hello world');\n" +
                    "eol\n" +
                    "\n" +
                    "compute * to the power of *\n" +
                    "!javascript:$1$^$2$ = $!$\n" +
                    "Math.pow($1$, $2$)\n" +
                    "eol\n" +
                    "\n" +
                    "generic actions *\n" +
                    "!actions:done\n" +
                    "["+
                    "{\"type\":\"answer\", \"select\":\"random\", \"phrases\":[\"ok\"]}," +
                    "{\"type\":\"table\", \"columns\":[\"title\"]}," +
                    "{\"type\":\"piechart\", \"columns\":[\"title\"], \"count\":10}," +
                    "{\"type\":\"rss\"}," +
                    "{\"type\":\"self\"}," +
                    "{\"type\":\"websearch\"}," +
                    "{\"type\":\"anchor\"}," +
                    "{\"type\":\"map\"}," +
                    "{\"type\":\"timer_set\"}," +
                    "{\"type\":\"timer_reset\"}," +
                    "{\"type\":\"audio_record\", \"concurrent\":true}," +
                    "{\"type\":\"audio_play\", \"concurrent\":true}," +
                    "{\"type\":\"audio_stop\"}," +
                    "{\"type\":\"video_record\", \"concurrent\":true}," +
                    "{\"type\":\"video_play\", \"concurrent\":true}," +
                    "{\"type\":\"video_stop\"}," +
                    "{\"type\":\"image_take\"}," +
                    "{\"type\":\"image_show\"}," +
                    "{\"type\":\"emotion\"}," +
                    "{\"type\":\"button_push\"}," +
                    "{\"type\":\"io\"}" +
                    "]" +
                    "eol\n" +
                    "\n" +
                    "set alarm\n" +
                    "!queue: 1234 `play metallica`\n" +
                    "alarm set\n" +
                    "\n" +
                    "who is *\n" +
                    "`[who] $1$`\n" +
                    "\n" +
                    "[who] *\n" +
                    "nlu success\n" +
                    "\n" +
                    "\n" +
                    "\n";

}
