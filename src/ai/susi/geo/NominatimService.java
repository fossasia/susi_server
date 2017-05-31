package ai.susi.geo;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class NominatimService {
	@SuppressWarnings("serial")
	public static class LocationNotFoundException extends Exception {
		public LocationNotFoundException(String locationName) {
			super("No location named '" + locationName + "' was found");
		}
	}
	/*
	 * A list of providers implementing the Nominatim service (http://wiki.openstreetmap.org/wiki/Nominatim)
	 * Note: You can host a Nominatim service on your own
	 */
    private static final List<String> nominatimProviders = Arrays.<String>asList(
    		"https://nominatim.openstreetmap.org/search/"
    );
    
    /*
     * According to the Nominatim T&C's a proper user agent indentifying the application has to be send
     * See https://operations.osmfoundation.org/policies/nominatim/
     */
    private static final String userAgent = "susi_server/1.0 chatbot AI see http://github.com/fossasia/susi_server";
    
	public static IntegerGeoPoint integerGeoPoint(String locationName) throws URISyntaxException, ClientProtocolException, IOException, LocationNotFoundException {
		return NominatimService.integerGeoPoints(locationName, 1).get(0);
    }
	
	public static ArrayList<IntegerGeoPoint> integerGeoPoints(String locationName, int limit) throws LocationNotFoundException, ClientProtocolException, IOException, URISyntaxException {
		ArrayList<IntegerGeoPoint> ret = new ArrayList<>();
		String rawJson = NominatimService.getRawJsonResponse(NominatimService.getUri(locationName));
		assertThat(rawJson, notNullValue());
		
		// Maybe we should implement returning a list, so the user has the option to choose?
		JSONArray places = new JSONArray(rawJson);
		if (places.length() == 0) {
			throw new NominatimService.LocationNotFoundException(locationName);
		} else {
			JSONObject place = places.getJSONObject(0);
			for (int i = 0; i < places.length(); i++) {
				ret.add(new IntegerGeoPoint(place.getDouble("lat"), place.getDouble("lon")));
			}
		}
		return ret;
	}
	
	private static URI getUri(String locationName) throws URISyntaxException {
		// Some load balancing
    	// TODO: Choose another provider if one is down or we get an exception
		String provider = NominatimService.nominatimProviders.get((new Random()).nextInt(NominatimService.nominatimProviders.size()));
    	URIBuilder uri = new URIBuilder(provider);
    	uri.setParameter("q", locationName);
    	uri.setParameter("format", "json");
    	uri.setParameter("limit", "1");
    	return uri.build();
	}
	
	private static String getRawJsonResponse(URI uri) throws ClientProtocolException, IOException {
		HttpGet httpGet = new HttpGet(uri);
    	
    	CloseableHttpClient client = HttpClientBuilder.create().setUserAgent(NominatimService.userAgent).build();
    	CloseableHttpResponse httpResponse = client.execute(httpGet);
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
		
		String contentMimeType = ContentType.getOrDefault(httpResponse.getEntity()).getMimeType();
		assertThat(contentMimeType, equalTo(ContentType.APPLICATION_JSON.getMimeType()));
		
		return EntityUtils.toString(httpResponse.getEntity());
	}
}
