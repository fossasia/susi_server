package api.external.deliveroo;

import org.apache.http.client.utils.*;

public class Config {
	public static final String HOST = "deliveroo.co.uk";
	public static final String API_ENDPOINT = "/orderapp/v1";
	
	public static URIBuilder getURIBuilder(String path) {
		return (new URIBuilder()).setScheme("https").setHost(Config.HOST).setPath(Config.API_ENDPOINT + path);
	}
}
