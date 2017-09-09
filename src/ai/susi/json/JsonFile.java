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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * This extends JSONObject to be a file which gets loaded and written to disk
 * It also offers some key management tools
 *
 */
public class JsonFile extends JSONObject {
	
	private final File file;
	private long file_date;

	public JsonFile(File file) throws IOException {
		super(true);
		if (file == null) throw new IOException("File must not be null");
		
		this.file = file;
		this.file_date = 0;
		updateToFile();
	}

	public void updateToFile() throws JSONException {
		if (this.file.lastModified() == this.file_date) return;
		JSONObject json;
		try {
			json = readJson(this.file);
		} catch (IOException e) {
			throw new JSONException(e.getMessage());
		}
		super.putAll(json);
		this.file_date = file.lastModified();
	}
	
	/**
	 * static JSON reader which is able to read a json file written by JsonFile
	 * @param file
	 * @return the json
	 * @throws IOException
	 */
	public static JSONObject readJson(File file) throws IOException {
	    JSONObject json = new JSONObject(true);
        if (file == null || !file.exists()) {
        	file.createNewFile();
        	writeJson(file, json);
        } else {
        	JSONTokener tokener;
        	tokener = new JSONTokener(new FileReader(file));
        	json.putAll(new JSONObject(tokener));
        }
    	return json;
	}
	
	/**
	 * write a json file in transaction style: first write a temporary file,
	 * then rename the original file to another temporary file, then rename the
	 * just written file to the target file name, then delete all other temporary files.
	 * @param file
	 * @param json
	 * @throws IOException
	 */
	public static void writeJson(File file, JSONObject json) throws IOException {
        if (file == null) throw new IOException("file must not be null");
        if (json == null) throw new IOException("json must not be null");
        if (!file.exists()) file.createNewFile();
        File tmpFile0 = new File(file.getParentFile(), file.getName() + "." + System.currentTimeMillis());
        File tmpFile1 = new File(tmpFile0.getParentFile(), tmpFile0.getName() + "1");
        FileWriter writer = new FileWriter(tmpFile0);
        writer.write(json.toString(2));
        writer.close();
        
        // start of critical phase: these operations must not be interrupted
        file.renameTo(tmpFile1);
        tmpFile0.renameTo(file);
        // end of critical phase
        
        tmpFile1.delete();
    }
	
	public synchronized File getFile() {
	    return this.file;
	}
	
	/**
	 * Write changes to file. It is not required that the user calls this method,
	 * however, if sub-objects of existing objects are modified, the user must handle
	 * file writings themself.
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
	 * @return JSONObject json
	 */
	public synchronized JSONObject toJSONObject(){
		JSONObject res = new JSONObject();
        res.putAll(this);
        return res;
	}

	@Override
	public synchronized JSONObject put(String key, boolean value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}
	
	@Override
	public synchronized JSONObject put(String key, double value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}
	
	@Override
	public synchronized JSONObject put(String key, Collection<?> value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}
	
	@Override
	public synchronized JSONObject put(String key, int value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}
	
	@Override
	public synchronized JSONObject put(String key, long value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}
	
	@Override
	public synchronized JSONObject put(String key, Map<?, ?> value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}
	
	@Override
	public synchronized JSONObject put(String key, Object value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}

	public synchronized JSONObject put(String key, JSONObject value) throws JSONException {
		updateToFile();
		super.put(key, value);
		commit();
		return this;
	}

	@Override
	public synchronized void putAll(JSONObject other) {
		updateToFile();
		super.putAll(other);
		commit();
	}
	
	@Override
	public synchronized Object remove(String key) {
		updateToFile();
		super.remove(key);
		commit();
		return this;
	}
}
