/*package org.json;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;
import org.junit.After;
import org.junit.Before;

public class JSONObjectTest extends TestCase {
    
    public static JSONObject testJson(boolean ordered) {
        JSONObject json = new JSONObject(ordered);

    public static JSONObject testJson() {
        JSONObject json = new JSONObject();
        Map<String,  Object> map = new HashMap<>();
        map.put("abc", 1);
        map.put("def", "Hello World");
/*@@ -25,12 +25,12 @@*//* public static JSONObject testJson(boolean ordered) {
        json.put("fuenf", 5);
        return json;
    }
    

    JSONObject testObject;
    

    @Before
    public void setUp() throws Exception {
        this.testObject = testJson(true);
        this.testObject = testJson();
    }

    @After
/*@@ -45,5 +45,4 @@*/ /*public void test() throws IOException {
        String t1 = j0.toString();
        assertEquals(t0, t1);
    }*/

/*}*/
