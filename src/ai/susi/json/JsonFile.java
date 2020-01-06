/**
 *  JsonFile
 *  Copyright 22.02.2015 by Robert Mader, @treba123
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

package ai.susi.json;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * This extends JSONObject to be a file which gets loaded and written to disk
 * It also offers some key management tools
 *
 */
public class JsonFile {

    private static final char LF = (char) 10; // we don't use '\n' or System.getProperty("line.separator"); here to be consistent over all systems.

    private final File file;
    private long file_date;
    private boolean lineByLineStorage;
    private JSONObject object; // a copy of the file in RAM

    /**
     * 
     * @param file the file where this JSON shall be written to.
     * @param lineByLine if true, each property in the object is written to a single line. If false, the file is pretty-printed
     * @throws IOException
     */
    public JsonFile(File file, boolean lineByLineStorage) throws IOException {
        this.object = null;
        if (file == null) throw new IOException("File must not be null");

        this.file = file;
        this.file_date = 0;
        this.lineByLineStorage = lineByLineStorage;
    }

    public int size() {
        updateToFile();
        int size = this.object.length();
        this.object = null; // because size is logged during initialization we do not store the data afterwards to support lazy initialization
        return size;
    }

    public boolean has(String key) {
        updateToFile();
        return this.object.has(key);
    }

    public Object get(String key) {
        updateToFile();
        return this.object.get(key);
    }

    public String getString(String key) {
        updateToFile();
        return this.object.getString(key);
    }

    public JSONObject getJSONObject(String key) {
        updateToFile();
        return this.object.getJSONObject(key);
    }

    public Set<String> keySet() {
        updateToFile();
        return this.object.keySet();
    }

    /**
     * Write changes to file. It is not required that the user calls this method,
     * however, if sub-objects of existing objects are modified, the user must handle
     * file writings themself.
     * @throws JSONException
     */
    public synchronized void commit() throws JSONException {
        try {
            writeJson(this.file, this.object, this.lineByLineStorage);
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
        this.file_date = file.lastModified();
    }

    /**
     * Return a copy of the JSON content
     * @return JSONObject json
     */
    public synchronized JSONObject toJSONObject() {
        updateToFile();
        JSONObject res = new JSONObject();
        res.putAll(this.object);
        return res;
    }

    public synchronized JsonFile put(String key, Object value) throws JSONException {
        updateToFile();
        this.object.put(key, value);
        if (this.lineByLineStorage) addJsonProperty(key, value); else commit();
        return this;
    }

    public synchronized void putAll(JSONObject other) {
        updateToFile();
        this.object.putAll(other);
        commit();
    }

    public synchronized JsonFile remove(String key) {
        updateToFile();
        this.object.remove(key);
        commit();
        return this;
    }

    /**
     * In case of concurrent file access it might be that the current data basis is outdated.
     * To keep up with changes, this method provides an update function to the local copy of the database.
     * @throws JSONException
     */
    private synchronized void updateToFile() throws JSONException {
        if (this.object != null && this.file.lastModified() == this.file_date) return;
        try {
            JSONObject json = readJson(this.file);
            this.object = new JSONObject(true);
            this.object.putAll(json);
            this.file_date = file.lastModified();
        } catch (IOException e) {
            throw new JSONException(e.getMessage());
        }
    }

    public void addJsonProperty(String key, Object value) throws JSONException {
        if (this.object != null) {
            this.object.put(key, value);
        }
        try {
            if (this.file == null) throw new IOException("file must not be null");
            RandomAccessFile raf = new RandomAccessFile(this.file, "rw");
            long pos = raf.length();
            if (pos == 0) {
                raf.write('{');
                raf.write(LF);
            } else if (pos < 3) {
                // it looks like this will be the first entry
                raf.seek(pos - 1);
            } else {
                raf.seek(pos - 2); // this includes a CR and the '}' character
                raf.write(',');
                raf.write(LF);
            }

            // write the key: object
            writeProperty(raf, key, value);
            raf.write(LF);
            raf.write('}');
            raf.close();

            // our object is up-to-date, no need to load it again
            this.file_date = this.file.lastModified();
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }

    public synchronized File getFile() {
        return this.file;
    }

    /** STATIC HELPER METHODS BELOW **/


    /**
     * static JSON reader which is able to read a json file written by JsonFile
     * @param file
     * @return the json
     * @throws IOException
     */
    public static JSONObject readJson(File file) throws IOException {
        JSONObject json = new JSONObject(true);
        if (!file.exists()) return json;
        // The file can be written in either of two ways:
        // - as a simple toString() or toString(2) from a JSONObject
        // - as a list of properties, one in each line with one line in the front starting with "{" and one in the end, starting with "}"
        // If the file was written in the first way, all property keys must be unique because we must use the JSONTokener to parse it.
        // if the file was written in the second way, we can apply a reader which reads the file line by line, overwriting a property
        // if it appears a second (third..) time. This has a big advantage: we can append new properties just at the end of the file.
        byte[] b = IOUtils.toByteArray(new FileInputStream(file));
        if (b.length == 0) return json;
        // check which variant is in b[]:
        // in a toString() output, there is no line break
        // in a toString(2) output, there is a line break but also two spaces in front
        // in a line-by-line output, there is a line break at b[1] and no spaces after that because the first char is a '"' and the second a letter
        boolean lineByLine = (b.length == 3 && b[0] == '{' && b[1] == LF && b[2] == '}') || (b[1] < ' ' && b[2] != ' ' && b[3] != ' ');
        InputStream is = new ByteArrayInputStream(b);
        if (lineByLine) {
            int a = is.read();
            assert (a == '{');
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("}")) break;
                if (line.length() == 0) continue;
                int p = line.indexOf("\":");
                if (p < 0) continue;
                String key = line.substring(1, p).trim();
                String value = line.substring(p + 2).trim();
                if (value.endsWith(",")) value = value.substring(0, value.length() - 1);
                if (value.charAt(0) == '{') {
                    json.put(key, new JSONObject(new JSONTokener(value)));
                } else if (value.charAt(0) == '[') {
                    json.put(key, new JSONArray(new JSONTokener(value)));
                } else if (value.charAt(0) == '"') {
                    json.put(key, value.substring(1, value.length() - 1));
                } else if (value.indexOf('.') > 0) {
                    try {
                        json.put(key, Double.parseDouble(value));
                    } catch (NumberFormatException e) {
                        json.put(key, value);
                    }
                } else {
                    try {
                        json.put(key, Long.parseLong(value));
                    } catch (NumberFormatException e) {
                        json.put(key, value);
                    }
                }
            }
        } else {
            try {
                json.putAll(new JSONObject(new JSONTokener(is)));
            } catch (JSONException e) {
                // could be a double key problem. In that case we should repeat the process with another approach
                throw new IOException(e);
            }
        }
        return json;
    }

    private static void writeProperty(RandomAccessFile writer, String key, Object object) throws IOException {
        writer.write('"'); writer.write(key.getBytes(StandardCharsets.UTF_8)); writer.write('"'); writer.write(':');
        if (object instanceof JSONObject) {
            writer.write(((JSONObject) object).toString().getBytes(StandardCharsets.UTF_8));
        } else if (object instanceof Map) {
            writer.write(new JSONObject((Map<?,?>) object).toString().getBytes(StandardCharsets.UTF_8));
        } else if (object instanceof JSONArray) {
            writer.write(((JSONArray) object).toString().getBytes(StandardCharsets.UTF_8));
        } else if (object instanceof Collection) {
            writer.write(new JSONArray((Collection<?>) object).toString().getBytes(StandardCharsets.UTF_8));
        } else if (object instanceof String) {
            writer.write('"'); writer.write(((String) object).getBytes(StandardCharsets.UTF_8)); writer.write('"');
        } else {
            writer.write(object.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * write a json file in transaction style: first write a temporary file,
     * then rename the original file to another temporary file, then rename the
     * just written file to the target file name, then delete all other temporary files.
     * @param file the storage file
     * @param json the json to be stored
     * @param lineByLine if true, each property in the object is written to a single line. If false, the file is pretty-printed
     * @throws IOException
     */
    public static void writeJson(File file, JSONObject json, boolean lineByLine) throws IOException {
        if (file == null) throw new IOException("file must not be null");
        if (json == null) throw new IOException("json must not be null");
        if (!file.exists()) file.createNewFile();
        File tmpFile0 = new File(file.getParentFile(), file.getName() + "." + System.currentTimeMillis());
        File tmpFile1 = new File(tmpFile0.getParentFile(), tmpFile0.getName() + "1");
        RandomAccessFile writer = new RandomAccessFile(tmpFile0, "rw");
        if (lineByLine) {
            writer.write('{');
            writer.write(LF);
            String[] keys = new String[json.length()];
            int p = 0;
            for (String key: json.keySet()) keys[p++] = key;
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                writeProperty(writer, key, json.get(key));
                if (i < keys.length - 1) writer.write(',');
                writer.write(LF);
            }
            writer.write('}');
        } else {
            writer.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
        }
        writer.close();

        // start of critical phase: these operations must not be interrupted
        file.renameTo(tmpFile1);
        tmpFile0.renameTo(file);
        // end of critical phase

        tmpFile1.delete();
    }

    public static void serialize(File f, JSONObject json) throws IOException {
    	PrintWriter pw = new PrintWriter(f);
        pw.print(json.toString(2));
        pw.close();
    }

    public static JSONObject deserialize(File f) throws IOException {
    	JSONObject json = new JSONObject(new JSONTokener(new FileInputStream(f)));
        return json;
    }

    public static void main(String[] args) {
        try {
            File f = File.createTempFile("JsonFileTest", "json");
            JsonFile json = new JsonFile(f, true);
            json.put("a", "a").put("b", new JSONObject(true).put("x", "1")).put("c", new JSONArray().put(1).put(2).put(3)).put("d", 7).put("e", 8.4);
            json.updateToFile();
            JSONObject test = readJson(f);
            System.out.println(test.toString(2));
        } catch (IOException e) {
        }
    }
}
