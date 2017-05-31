package api.external.deliveroo;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.http.client.ClientProtocolException;

public class DeliverooDemo {

	public static void main(String[] args) throws ClientProtocolException, URISyntaxException, IOException {
		Boolean once = true;
		System.out.println("Hello out there!");
		Client client = new Client();
		Session.begin(client);

		for (Restaurant restaurant : Restaurant.fetchRestaurants(client, new Coordinate("52.5166791", "13.4584727"))) {
			System.out.println(restaurant.toString());
			/*for (Restaurant.OpeningHours hours : restaurant.getOpeningHours()) {
				System.out.println("\t" + hours.toString());
			}*/
			/*for (MenuTag menu : restaurant.getMenuTags()) {
				System.out.println("\t" + menu.toString());
			}*/
			
			if (once) {
					restaurant.fetchMenuItems(client);
					for (MenuItem menuItem : restaurant.getMenuItems()) {
						System.out.println("\t" + menuItem.toString());
					}
				once = false;
			}
		}
	}

}
