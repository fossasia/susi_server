package org.loklak.susi;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.loklak.SusiServer;
import org.loklak.data.DAO;
import org.loklak.server.ClientIdentity;

public class SusiTutorialTest {

    private final static String testFile = 
                    "# susi EzD tutorial playground\n" +
                    "::prior\n" +
                    "roses are red\n" +
                    "susi is a hack\n" +
                    "skynet is back\n";
    
    private final BufferedReader getTestReader() {
        ByteArrayInputStream bais = new ByteArrayInputStream(testFile.getBytes(StandardCharsets.UTF_8));
        return new BufferedReader(new InputStreamReader(bais, StandardCharsets.UTF_8));
    }
    
    public static String susiAnswer(String q, ClientIdentity identity) {
        SusiInteraction interaction = new SusiInteraction(DAO.susi, q, 0, 0, 0, 1, identity);
        JSONObject json = interaction.getJSON();
        DAO.susi.getLogs().addInteraction(identity.getClient(), interaction);
        String answer = json.getJSONArray("answers")
                .getJSONObject(0)
                .getJSONArray("actions")
                .getJSONObject(0)
                .getString("expression");
        return answer;
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
        try {
            System.setProperty("java.awt.headless", "true"); // no awt used here so we can switch off that stuff
            Path data = FileSystems.getDefault().getPath("data");
            Map<String, String> config = SusiServer.readConfig(data);
            
            // initialize all data        
            try{
                DAO.init(config, data);
                BufferedReader br = getTestReader();
                JSONObject lesson = DAO.susi.readEzDLesson(br);
                DAO.susi.learn(lesson);
                br.close();
            } catch(Exception e){
                e.printStackTrace();
                Log.getLog().warn(e.getMessage());
                Log.getLog().warn("Could not initialize DAO. Exiting.");
                System.exit(-1);
            }
            
            ClientIdentity identity = new ClientIdentity("host:localhost");
            //System.out.println(susiAnswer("ich möchte was verschenken", identity));
            //System.out.println(susiAnswer("meinen Vater", identity));
            System.out.println(susiAnswer("roses are red", identity));
            System.out.println(susiAnswer("susi is a hack", identity));
        } catch (Exception e) {
            e.printStackTrace();
        }        
    }

}
