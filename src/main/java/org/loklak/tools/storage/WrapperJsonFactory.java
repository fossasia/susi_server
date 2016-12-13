package org.loklak.tools.storage;

import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by devenv on 12/9/16.
 */
public class WrapperJsonFactory implements JsonFactory {
    final JSONObject json;
    public WrapperJsonFactory(final JSONObject json) {
        this.json = json;
    }

    @Override
    public JSONObject getJSON() throws IOException {
        return this.json;
    }
}