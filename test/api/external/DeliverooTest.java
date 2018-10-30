package api.external;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;
import org.junit.Test;

import api.external.deliveroo.*;

public class DeliverooTest {
	private static String rawJson = "{\"id\":36242,\"name\":\"Sigiriya\",\"name_with_branch\":\"Sigiriya\",\"uname\":\"sigiriya\",\"price_category\":2,\"currency_symbol\":\"\u20AC\",\"primary_image_url\":\"https://cdn1.deliveroo.co.uk/media/menus/28019/320x180.jpg?v=1481188440\",\"image_url\":\"https://cdn1.deliveroo.co.uk/media/menus/28019/{w}x{h}.jpg?v=1481188440{&filters}{&quality}\",\"neighborhood\":{\"id\":519,\"name\":\"Friedrichshain\",\"uname\":\"friedrichshain\"},\"coordinates\":[13.4579792,52.5113786],\"newly_added\":false,\"category\":\"Nur Bei Uns\",\"curr_prep_time\":20,\"delay_time\":0,\"baseline_deliver_time\":21,\"total_time\":20,\"distance_m\":590,\"travel_time\":10,\"kitchen_open_advance\":0,\"hours\":{\"today\":[],\"tomorrow\":[]},\"opening_hours\":{\"today\":[[\"12:00\",\"22:30\"]],\"tomorrow\":[[\"12:00\",\"22:30\"]]},\"target_delivery_time\":{\"minutes\":\"15 - 25\"},\"menu\":{\"menu_tags\":[{\"id\":293,\"type\":\"Collection\",\"name\":\"Nur Bei Uns\"},{\"id\":33,\"type\":\"Locale\",\"name\":\"S\u00FCdindisch\"},{\"id\":188,\"type\":\"Locale\",\"name\":\"Sri Lankisch\"},{\"id\":86,\"type\":\"Dietary\",\"name\":\"Vegetarier\"},{\"id\":71,\"type\":\"Food\",\"name\":\"Meeresfr\u00FCchte\"}]},\"open\":false,\"delivery_hours\":{\"today\":[],\"tomorrow\":[]}}";
	
	@Test
	public void testParsing() {
		Restaurant restaurant = new Restaurant(new JSONObject(rawJson));
		assertTrue(restaurant.getName().equals("Sigiriya"));
		assertTrue(restaurant.getCoordinate().getLatitude().equals("13.4579792"));
	}

	// test the exptected behaviour of the deliveroo API
	@Test
	public void testAPI() throws ClientProtocolException, URISyntaxException, IOException {
		Client client = new Client();
		List<Restaurant> restaurants = Restaurant.fetchRestaurants(client, new Coordinate("52.5166791", "13.4584727"));
		if(!restaurants.isEmpty()) {
			restaurants.get(0).fetchMenuItems(client);
		}
	}
	
	// test the demo
	@Test
	public void testDemo() throws ClientProtocolException, URISyntaxException, IOException {
		DeliverooDemo.main(null);
	}
}
