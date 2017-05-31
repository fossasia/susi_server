package api.external.deliveroo;

import org.json.JSONObject;

public class MenuTag {
	private int id;
	private String type;
	private String name;
	
	public MenuTag(int id, String type, String name) {
		this.id = id;
		this.type = type;
		this.name = name;
	}
	
	public MenuTag(JSONObject jsonObject) {
		this(jsonObject.getInt("id"), jsonObject.getString("type"), jsonObject.getString("name"));
	}
	
	/**
	 * @Override
	 */
	public String toString() {
		return this.type + " " + this.name + " (" + this.id + ")";
	}
}