package org.json;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;

public class JSONObjectTest extends TestCase {
    
    public static JSONObject testJson(boolean ordered) {
        JSONObject json = new JSONObject(ordered);
        Map<String,  Object> map = new HashMap<>();
        map.put("abc", 1);
        map.put("def", "Hello World");
        map.put("ghj", new String[]{"Hello", "World"});
        json.putAll(new JSONObject(map));
        json.put("eins", 1);
        json.put("zwei", 2);
        json.put("drei", 3);
        json.put("vier", 4);
        json.put("fuenf", 5);
        return json;
    }
    
    JSONObject testObject;
    
    @Before
    public void setUp() throws Exception {
        this.testObject = testJson(true);
    }

    @After
    public void tearDown() throws Exception {
    }

    public void test() throws IOException {
        Object a = this.testObject.get("ghj");
        assertTrue(a instanceof JSONArray);
        String t0 = this.testObject.toString();
        JSONObject j0 = new JSONObject(t0);
        String t1 = j0.toString();
        assertEquals(t0, t1);
    }
    
}
