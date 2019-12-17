/*package api.external.transit;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import de.schildbach.pte.dto.Fare;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.Trip;
import de.schildbach.pte.dto.Trip.Leg;
import de.schildbach.pte.dto.Line;

public class JSONConverter {
	public static JSONObject tripToJSON(Trip trip) {
		JSONObject tripObj = new JSONObject();
		tripObj.put("durationMinutes", trip.getDuration() / 60000.0);
		tripObj.put("departureTime", trip.getFirstDepartureTime());
		JSONObject legs = new JSONObject();
		for (Integer i = 0; i < trip.legs.size(); i++) {
			legs.put(i.toString(), JSONConverter.legToJSON(trip.legs.get(i)));
		}
		tripObj.put("legs", legs);
		tripObj.put("numChanges", trip.numChanges);
		JSONArray fares = new JSONArray();
		if (trip.fares != null) {
			for (Fare fare : trip.fares) {
				fares.put(JSONConverter.fareToJson(fare));
			}
			tripObj.put("fares", fares);
		}
		tripObj.put("description", JSONConverter.getDescription(trip.legs));
		return tripObj;
	}
	
	public static JSONObject fareToJson(Fare fare) {
		JSONObject fareObj = new JSONObject();
		if (fare == null) return fareObj;
		fareObj.put("currency", fare.currency.getCurrencyCode());
		fareObj.put("fare", fare.fare);
		fareObj.put("network", fare.network);
		fareObj.put("units", fare.units);
		fareObj.put("unitName", fare.unitName);
		fareObj.put("class", fare.getClass().getName());
		return fareObj;
	}
	
	public static JSONObject legToJSON(Leg leg) {
		JSONObject legObj = new JSONObject();
		if (leg == null) return legObj;
		legObj.put("departure", JSONConverter.locationToJSON(leg.departure));
		legObj.put("arrival", JSONConverter.locationToJSON(leg.arrival));
		legObj.put("departureTime", leg.getDepartureTime());
		legObj.put("arrivalTime", leg.getArrivalTime());
		if (leg instanceof Trip.Public) {
			Trip.Public pub = (Trip.Public)leg;
			if (pub.getDepartureDelay() != null) {
				legObj.putOpt("departureDelay", pub.getDepartureDelay() / 60000.0);
			}
			if (pub.getArrivalDelay() != null) {
				legObj.putOpt("arrivalDelay", pub.getArrivalDelay() / 60000.0);
			}
			legObj.putOpt("line", JSONConverter.lineToJSON(pub.line));
		}
		return legObj;
	}
	
	public static JSONObject lineToJSON(Line line) {
		JSONObject lineObj = new JSONObject();
		if (line == null) return lineObj;
		lineObj.put("id", line.id);
		lineObj.put("name", line.name);
		lineObj.put("label", line.label);
		lineObj.put("message", line.message);
		lineObj.put("network", line.network);
		lineObj.put("product", line.product.toString());
		return lineObj;
	}
	
	public static JSONObject locationToJSON(Location location) {
		JSONObject locationObj = new JSONObject();
		if (location == null) return locationObj;
		JSONObject coordinateObj = new JSONObject();
		coordinateObj.put("lat", location.lat);
		coordinateObj.put("lon", location.lon);
		locationObj.put("coords", coordinateObj);
		locationObj.put("name", location.name);
		locationObj.put("place", location.place);
		locationObj.put("id", location.id);
		return locationObj;
	}
	
	@SuppressWarnings("deprecation")
	public static String getDescription(List<Leg> legs) {
		String response = "";
		for (int i = 0; i < legs.size(); i++) {
			if (!(legs.get(i) instanceof Trip.Public)) continue;
			if (i == 0) {
				response += "Take the ";
			} else {
				response += " From there, take the ";
			}
			Trip.Public pub = (Trip.Public)legs.get(i);
			response += pub.line.label;
			response += " (departing at " + pub.getDepartureTime().getHours() + ":" + pub.getDepartureTime().getMinutes();
			if (pub.getDepartureDelay() != null) {
				response += ", +" + (pub.getDepartureDelay() / 60000.0) + " mins delay";
			}
			response += ") to ";
			response += pub.arrival.name;
			response += " (arriving at " + pub.getArrivalTime().getHours() + ":" + pub.getArrivalTime().getMinutes();
			if (pub.getArrivalDelay() != null) {
				response += ", +" + (pub.getArrivalDelay() / 60000.0) + " mins delay";
			}
			response += ").";
		}
		return response;
	}
}*/
