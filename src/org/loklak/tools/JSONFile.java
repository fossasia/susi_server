package org.loklak.tools;

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
 *
 */
public class JSONFile extends JSONObject {
	
	private File file;

	public JSONFile(File file) throws IOException{
		super();
		this.file = file;
		if(this.file.exists()){
			JSONTokener tokener;
			tokener = new JSONTokener(new FileReader(file));
			putAll(new JSONObject(tokener));
		}
		else{
			this.file.createNewFile();
			writeFile();
		}
	}
	
	private void writeFile() throws JSONException{
		FileWriter writer;
		try {
			writer = new FileWriter(file);
			writer.write(this.toString());
			writer.close();
		} catch (IOException e) {
			throw new JSONException(e.getMessage());
		}
	}

	@Override
	public JSONObject put(String key, boolean value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public JSONObject put(String key, double value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public JSONObject put(String key, Collection<?> value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public JSONObject put(String key, int value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public JSONObject put(String key, long value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public JSONObject put(String key, Map<?, ?> value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public JSONObject put(String key, Object value) throws JSONException {
		super.put(key, value);
		writeFile();
		return this;
	}
	
	@Override
	public Object remove(String key) {
		super.remove(key);
		writeFile();
		return this;
	}
}
