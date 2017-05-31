package api.external.deliveroo;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;

public class Session {
	private static final String API_PATH = "/session";
	
	public static void begin(Client client) {
		try {
			URIBuilder uri = Config.getURIBuilder(Session.API_PATH);
			HttpGet httpGet = new HttpGet(uri.build());
			client.getHttpClient().execute(httpGet);
		} catch (Exception ex) {
			System.out.println("Could not start session" + ex.toString());
		}
	}
}
