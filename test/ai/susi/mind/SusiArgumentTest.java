package ai.susi.mind;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ai.susi.mind.SusiArgument;
import ai.susi.mind.SusiThought;
import ai.susi.server.ClientIdentity;

public class SusiArgumentTest {

    public static SusiArgument generateTestArgument(String key, String value) {
        return new SusiArgument(ClientIdentity.ANONYMOUS, SusiLanguage.en).think(SusiThoughtTest.generateTestThought(key, value));
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
        SusiArgument a = generateTestArgument("a", "22");
        a.think(SusiThoughtTest.generateTestThought("b", "33"));
        SusiThought t = a.mindmeld(true);
        assertTrue(t.getCount() == 1);
        a.think(SusiThoughtTest.generateTestThought("b", "44"));
        assertTrue(a.mindmeld(true).getObservations("b").get(0).equals("44"));
    }

}
