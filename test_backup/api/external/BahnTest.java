/*package api.external;

import java.io.IOException;

import org.junit.Test;

import api.external.transit.*;
import api.external.transit.BahnService.NoStationFoundException;

public class BahnTest {
	BahnService service = new BahnService();
	
	@Test
	public void testConnection() throws IOException, NoStationFoundException {
		System.out.println(service.getConnections("Alexanderplatz", "Griebnitzsee"));
		System.out.println(service.getConnections("München HBF", "Griebnitzsee", 10, 0).getData().getJSONObject(0).getString("description"));
	}
}*/
