package ai.susi.server;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chetankaushik on 16/06/17.
 */
public class Groups extends JSONObject {

    private final File file;

    public Groups(File file) throws IOException {
        super(true);
        if (file == null) throw new IOException("File must not be null");

        this.file = file;
        if (file.exists()) {
            JSONTokener tokener;
            tokener = new JSONTokener(new FileReader(file));
            putAll(new JSONObject(tokener));
        } else {
            file.createNewFile();
            commit();
        }
    }

    /**
     * static JSON reader which is able to read a json file written by JsonFile
     *
     * @param file
     * @return the json
     * @throws IOException
     */
    public static JSONObject readJson(File file) throws IOException {
        if (file == null) throw new IOException("file must not be null");
        JSONObject json = new JSONObject(true);
        JSONTokener tokener;
        tokener = new JSONTokener(new FileReader(file));
        json.putAll(new JSONObject(tokener));
        return json;
    }

    /**
     * write a json file in transaction style: first write a temporary file,
     * then rename the original file to another temporary file, then rename the
     * just written file to the target file name, then delete all other temporary files.
     *
     * @param file
     * @param json
     * @throws IOException
     */
    public static void writeJson(File file, JSONObject json) throws IOException {

    }

    public synchronized File getFile() {
        return this.file;
    }

    /**
     * Write changes to file. It is not required that the user calls this method,
     * however, if sub-objects of existing objects are modified, the user must handle
     * file writings themself.
     *
     * @throws JSONException
     */
    public synchronized void commit() throws JSONException {
        try {
            writeJson(this.file, this);
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
    }

    /**
     * Return a copy of the JSON content
     *
     * @return JSONObject json
     */
    public synchronized JSONObject toJSONObject() {
        JSONObject res = new JSONObject();
        res.putAll(this);
        return res;
    }

    @Override
    public synchronized JSONObject put(String key, boolean value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized JSONObject put(String key, double value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized JSONObject put(String key, Collection<?> value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized JSONObject put(String key, int value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized JSONObject put(String key, long value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized JSONObject put(String key, Map<?, ?> value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized JSONObject put(String key, Object value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    public synchronized JSONObject put(String key, JSONObject value) throws JSONException {
        super.put(key, value);
        commit();
        return this;
    }

    @Override
    public synchronized void putAll(JSONObject other) {
        super.putAll(other);
        commit();
    }

    @Override
    public synchronized Object remove(String key) {
        super.remove(key);
        commit();
        return this;
    }
}