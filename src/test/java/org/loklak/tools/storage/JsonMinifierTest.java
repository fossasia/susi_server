package org.loklak.tools.storage;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.json.JSONObjectTest;
import org.junit.Test;

public class JsonMinifierTest {

    @Test
    public void string2byte() throws Exception {
        JSONObject json = JSONObjectTest.testJson(true);
        JsonMinifier minifier = new JsonMinifier();
        JsonMinifier.JsonCapsuleFactory capsule = minifier.minify(json);
        JSONObject challenge = capsule.getJSON();
        assertEquals(json.toString(), challenge.toString());
    }

}
