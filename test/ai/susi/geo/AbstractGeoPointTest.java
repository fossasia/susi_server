package ai.susi.geo;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ai.susi.geo.AbstractGeoPoint;
/**
 * Test for AbstractGeoPoint.java
 */
public class AbstractGeoPointTest {
	@Test
	public void distanceTest() {
        final double lat1 = 28.7041;
        final double lon1 = 77.1025;
        final double lat2 = 19.0760;
        final double lon2 = 72.8777;

        double result = AbstractGeoPoint.distance(lat1, lon1, lat2, lon2);
		assertEquals("1154533.1893973786", result+"");
        assertEquals("6378137.0", AbstractGeoPoint.EQUATOR_EARTH_RADIUS+"");
        assertEquals("0.017453292519943295", AbstractGeoPoint.D2R+"");
	}
}