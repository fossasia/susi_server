package api.external.deliveroo;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;
import org.json.*;

public class Restaurant {
	public class OpeningHours {
		public LocalDateTime start;
		public LocalDateTime end;
		
		public OpeningHours(String day, String start, String end) {
			LocalDate localDate;
			switch (day) {
			case "today":
				localDate = LocalDate.now();
				break;
			case "tomorrow":
				localDate = LocalDate.now().plusDays(1);
				break;
			default:
				throw new IllegalArgumentException("Day must be 'today' or 'tomorrow'");
			}
			LocalTime startLocalTime = LocalTime.parse(start);
			LocalTime endLocalTime = LocalTime.parse(end);
			this.start = LocalDateTime.of(localDate, startLocalTime);
			this.end = LocalDateTime.of(localDate, endLocalTime);
		}
		
		/**
		 * @Override
		 */
		public String toString() {
			return this.start.toString() + " - " + this.end.toString();
		}
	}
	
	public class TargetDeliveryTime {
		public String range;
		public String value;
		
		public TargetDeliveryTime(String range, String value) {
			this.range = range;
			this.value = value;
		}
	}
	
	private static final String API_PATH = "/restaurants/";
	
	// Numeric id
	private int id;
	
	// The restaurant name
	private String name;
	
	// Seems to be identical to name
	private String nameWithBranch;
	
	// Lowercase unique name (without special chars)
	private String uname;
	
	// Seems to range from 1 to 3
	private int priceCategory;
	
	private String currencySymbol;
	
	private String primaryImageUrl;
	
	private String imageUrl;
	
	private Coordinate coordinate;
	
	private Boolean newlyAdded;
	
	private String category;
	
	// Maybe how busy the restaurant currently is?
	private double currentPreparationTime;
	
	private int delayTime;
	
	// Maybe average delivery time?
	private float baselineDeliverTime;
	
	// Maybe time until delivery?
	private int totalTime;
	
	// Distance in meters
	private int distance;
	
	private int travelTime;
	
	// no idea what this means
	private int kitchenOpenAdvance;
	
	// TODO delivery time slots (ASAP, time range)
	
	private List<OpeningHours> openingHours;
	
	private TargetDeliveryTime targetDeliveryTime;
	
	private List<MenuTag> menuTags;
	
	private List<MenuItem> menuItems = null;
	
	private Boolean open;
	
	public static List<Restaurant> fetchRestaurants(Client client, Coordinate coordinate) throws URISyntaxException, ClientProtocolException, IOException {
		List<Restaurant> restaurants = new ArrayList<>();

		URIBuilder uri = Config.getURIBuilder(Restaurant.API_PATH);
		uri.setParameter("lat", coordinate.getLatitude());
		uri.setParameter("lng", coordinate.getLongitude());

		try {
			HttpGet httpGet = new HttpGet(uri.build());
			CloseableHttpResponse httpResponse = client.getHttpClient().execute(httpGet);
			if(httpResponse.getStatusLine().getStatusCode()==200) {
				String contentMimeType = ContentType.getOrDefault(httpResponse.getEntity()).getMimeType();
				assertThat(contentMimeType, equalTo(ContentType.APPLICATION_JSON.getMimeType()));

				// The output can be quite large, so storing it in a String is not ideal.
				// Maybe it's possible to parse a Stream?
				String rawJson = EntityUtils.toString(httpResponse.getEntity());
				assertThat(rawJson, notNullValue());

				JSONObject jsonRoot = new JSONObject(rawJson);
				assertThat(jsonRoot.has("restaurants"), equalTo(true));
				JSONArray restaurantsJsonArray = jsonRoot.getJSONArray("restaurants");

				Iterator<Object> restaurantsIterator = restaurantsJsonArray.iterator();

				while (restaurantsIterator.hasNext()) {
					Object obj = restaurantsIterator.next();
					if (obj instanceof JSONObject) {
						restaurants.add(new Restaurant((JSONObject) obj));
					}
				}
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return restaurants;
	}
	
	public Restaurant(JSONObject jsonObject) {
		this.id = jsonObject.getInt("id");
		this.name = jsonObject.getString("name");
		this.nameWithBranch = jsonObject.getString("name_with_branch");
		this.uname = jsonObject.getString("uname");
		this.priceCategory = jsonObject.getInt("price_category");
		this.currencySymbol = jsonObject.getString("currency_symbol");
		this.primaryImageUrl = jsonObject.optString("primary_image_url", null);
		this.imageUrl = jsonObject.optString("image_url", null);
		
		JSONArray coordinateJsonArray = jsonObject.getJSONArray("coordinates");
		this.coordinate = new Coordinate(coordinateJsonArray.getDouble(0), coordinateJsonArray.getDouble(1));
		this.newlyAdded = jsonObject.getBoolean("newly_added");
		this.category = jsonObject.optString("category", "");
		this.currentPreparationTime = jsonObject.getDouble("curr_prep_time");
		this.baselineDeliverTime = jsonObject.getInt("baseline_deliver_time");
		this.totalTime = jsonObject.getInt("total_time");
		this.distance = jsonObject.getInt("distance_m");
		this.travelTime = jsonObject.getInt("travel_time");
		this.kitchenOpenAdvance = jsonObject.getInt("kitchen_open_advance");
		this.openingHours = this.parseOpeningHours(jsonObject.getJSONObject("opening_hours"));
		
		JSONObject targetDeliveryTimeJson = jsonObject.getJSONObject("target_delivery_time");
		String range = targetDeliveryTimeJson.keys().next();
		this.targetDeliveryTime = new TargetDeliveryTime(range, targetDeliveryTimeJson.getString(range));
		this.menuTags = this.parseMenuTags(jsonObject.getJSONObject("menu").getJSONArray("menu_tags"));
		this.open = jsonObject.getBoolean("open");
	}
	
	private List<OpeningHours> parseOpeningHours(JSONObject openingHoursJsonObject) {
		List<OpeningHours> openingHours = new ArrayList<>();
		
		Iterator<String> openingHoursDayIterator = openingHoursJsonObject.keys();
		
		while (openingHoursDayIterator.hasNext()) {
			String day = openingHoursDayIterator.next();
			JSONArray openingHoursArray = openingHoursJsonObject.getJSONArray(day);
			for (Object openingHoursObject : openingHoursArray) {
				if (openingHoursObject instanceof JSONArray) {
					String start = ((JSONArray)openingHoursObject).getString(0);
					String end = ((JSONArray)openingHoursObject).getString(1);
					openingHours.add(new OpeningHours(day, start, end));
				}
			}
		}
		return openingHours;
	}
	
	
	private List<MenuTag> parseMenuTags(JSONArray menuTagsJson) {
		List<MenuTag> menuTags = new ArrayList<>();
		Iterator<Object> menuTagsIterator = menuTagsJson.iterator();
		
		while (menuTagsIterator.hasNext()) {
			Object menuTag = menuTagsIterator.next();
			if (menuTag instanceof JSONObject) {
				menuTags.add(new MenuTag((JSONObject)menuTag));
			}
		}
		return menuTags;
	}
	
	public Restaurant fetchMenuItems(Client client) throws URISyntaxException, ClientProtocolException, IOException {
		this.menuItems = new ArrayList<>();
		URIBuilder uri = Config.getURIBuilder(Restaurant.API_PATH + this.id);
		
		HttpGet httpGet = new HttpGet(uri.build());
		CloseableHttpResponse httpResponse = client.getHttpClient().execute(httpGet);
		assertThat(httpResponse.getStatusLine().getStatusCode(), equalTo(200));
		
		String contentMimeType = ContentType.getOrDefault(httpResponse.getEntity()).getMimeType();
		assertThat(contentMimeType, equalTo(ContentType.APPLICATION_JSON.getMimeType()));
		
		String rawJson = EntityUtils.toString(httpResponse.getEntity());
		assertThat(rawJson, notNullValue());
		
		JSONArray menuItemsJsonArray = (new JSONObject(rawJson)).getJSONObject("menu").getJSONArray("menu_items");
		for (Object menuItemObject : menuItemsJsonArray) {
			if (menuItemObject instanceof JSONObject) {
				this.menuItems.add(new MenuItem((JSONObject)menuItemObject));
			}
		}
		
		return this;
	}
	
	/**
	 * @Override
	 */
	public String toString() {
		return this.name + "(" + this.category + ")";
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getNameWithBranch() {
		return nameWithBranch;
	}

	public String getUname() {
		return uname;
	}

	public int getPriceCategory() {
		return priceCategory;
	}

	public String getCurrencySymbol() {
		return currencySymbol;
	}

	public String getPrimaryImageUrl() {
		return primaryImageUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public Coordinate getCoordinate() {
		return coordinate;
	}

	public Boolean getNewlyAdded() {
		return newlyAdded;
	}

	public String getCategory() {
		return category;
	}

	public double getCurrentPreparationTime() {
		return currentPreparationTime;
	}

	public int getDelayTime() {
		return delayTime;
	}

	public float getBaselineDeliverTime() {
		return baselineDeliverTime;
	}

	public int getTotalTime() {
		return totalTime;
	}

	public int getDistance() {
		return distance;
	}

	public int getTravelTime() {
		return travelTime;
	}

	public int getKitchenOpenAdvance() {
		return kitchenOpenAdvance;
	}

	public List<OpeningHours> getOpeningHours() {
		return openingHours;
	}

	public TargetDeliveryTime getTargetDeliveryTime() {
		return targetDeliveryTime;
	}

	public List<MenuTag> getMenuTags() {
		return menuTags;
	}
	
	public List<MenuItem> getMenuItems() {
		return menuItems;
	}

	public Boolean getOpen() {
		return open;
	}
}
