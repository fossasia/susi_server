package ai.susi.geo;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ai.susi.geo.NominatimService.LocationNotFoundException;

public class GeocodingTest {
	@Test
	public void testGeocoding() throws LocationNotFoundException, ClientProtocolException, URISyntaxException, IOException {
		// Brandenburger Tor Berlin
		IntegerGeoPoint pointA = new IntegerGeoPoint(52.5162746, 13.377704);
		IntegerGeoPoint pointB = NominatimService.integerGeoPoint("Brandenburger Tor");
		assertTrue(pointA.distance(pointB) < 100);
	}
	
	@Rule
    public ExpectedException thrown = ExpectedException.none();
	@Test
	public void testNotFound() throws IllegalArgumentException, ClientProtocolException, URISyntaxException, IOException, LocationNotFoundException {
		thrown.expect(NominatimService.LocationNotFoundException.class);
		NominatimService.integerGeoPoint("This place does not exist");
	}
	
	@Test
	public void testDistance() {
		IntegerGeoPoint pointA = new IntegerGeoPoint(52.0,13.0);
		IntegerGeoPoint pointB = new IntegerGeoPoint(53.0,14.0);
		double distanceKM = pointA.distance(pointB) / 1000;
		assertTrue(distanceKM > 130);
		assertTrue(distanceKM < 131);
	}
}
