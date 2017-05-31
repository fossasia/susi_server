package api.external.deliveroo;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class Client {
	private CloseableHttpClient client = null;
	private CookieStore cookieStore = new BasicCookieStore();
	
	public CloseableHttpClient getHttpClient() {
		if (this.client == null) {
			this.client = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
		}
		return this.client;
	}
}
