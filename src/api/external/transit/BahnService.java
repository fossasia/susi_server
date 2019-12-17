/*package api.external.transit;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.*;

import ai.susi.mind.SusiThought;
import de.schildbach.pte.*;
import de.schildbach.pte.BahnProvider;
import de.schildbach.pte.dto.Location;
import de.schildbach.pte.dto.QueryTripsResult;
import de.schildbach.pte.dto.Trip;

public class BahnService {

	private final BahnProvider provider = new BahnProvider();

	@SuppressWarnings("serial")
	public static class NoStationFoundException extends Exception {
		public NoStationFoundException(String stationName) {
			super("No station named '" + stationName + "' found.");
		}
	}

	
	public SusiThought getConnections(String from, String to) throws IOException, NoStationFoundException {
		Calendar cal = Calendar.getInstance();
		return this.getConnections(from, to, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
	}
	
	@SuppressWarnings("deprecation")
	public SusiThought getConnections(String from, String to, int hoursDep, int minutesDep) throws IOException, NoStationFoundException {
		// Yes, Date is deprecated, but the referenced library needs it...
		Date date = new Date();
		date.setHours(hoursDep);
		date.setMinutes(minutesDep);
		SusiThought response = new SusiThought();
		JSONArray data = new JSONArray();
		
		List<Location> fromLocations = provider.suggestLocations(from).getLocations();
		List<Location> toLocations = provider.suggestLocations(to).getLocations();
		if (fromLocations.size() == 0) {
			throw new NoStationFoundException(from);
		}
		if (toLocations.size() == 0) {
			throw new NoStationFoundException(to);
		}
		
		QueryTripsResult tripsResult = provider.queryTrips(fromLocations.get(0), null, toLocations.get(0), date, true, null, null, null, null, null);
		response.setHits(tripsResult.trips.size());
		
		for (Trip trip : tripsResult.trips) {
			data.put(JSONConverter.tripToJSON(trip));
		}
		response.setData(data);
		
		return response;
	}
}*/
