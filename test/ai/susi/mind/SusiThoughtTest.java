package ai.susi.mind;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ai.susi.mind.SusiThought;

public class SusiThoughtTest {

    public static SusiThought generateTestThought(String key, String value) {
        return new SusiThought().addObservation(key, value);
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
        SusiThought t = generateTestThought("a", "1");
        t.addObservation("b", "2");
        assertTrue(t.getCount() == 1);
        t.addObservation("a", "100");
        assertTrue(t.getCount() == 2);
        
    }

}
