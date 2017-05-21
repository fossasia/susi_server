package api.external.deliveroo;

public class Coordinate {
	private String latitude = "";
	private String longitude = "";
	
	// TODO check the input inside the getters
	// TODO offer different kinds of output (e.g. precision)
	
	/**
	 * @param latitude
	 * @param longitude
	 */
	public Coordinate(String latitude, String longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
	
	public Coordinate(Double latitude, Double longitude) {
		this.latitude = latitude.toString();
		this.longitude = longitude.toString();
	}
	
	/**
	 * @Override
	 */
	public String toString() {
		return "Lat: " + this.latitude + ", Long: " + this.longitude; 
	}
	
	/**
	 * @return the latitude
	 */
	public String getLatitude() {
		return latitude;
	}
	
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	
	/**
	 * @return the longitude
	 */
	public String getLongitude() {
		return longitude;
	}
	
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	
	
}
