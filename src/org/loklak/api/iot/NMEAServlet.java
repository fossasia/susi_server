/**
 *  NMEA Servlet
 *  Copyright 02.07.2016 by Sudheesh Singanamalla, @sudheesh001
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.api.iot;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.*;
import java.io.*;

import org.json.JSONObject;
import org.loklak.server.Query;
import org.loklak.http.RemoteAccess;
import org.loklak.http.ClientConnection;

/*
Sample Read Format
==================
  1      2    3        4 5         6 7     8     9      10    11
$xxxxx,123519,A,4807.038,N,01131.000,E,022.4,084.4,230394,003.1,W*6A
  |      |    |        | |         | |     |     |      |     |
  |      |    |        | |         | |     |     |      |     Variation sign
  |      |    |        | |         | |     |     |      Variation value
  |      |    |        | |         | |     |     Date DDMMYY
  |      |    |        | |         | |     COG
  |      |    |        | |         | SOG
  |      |    |        | |         Longitude Sign
  |      |    |        | Longitude Value
  |      |    |        Latitude Sign
  |      |    Latitude value
  |      Active or Void
  UTC HHMMSS
 */
public class NMEAServlet extends HttpServlet {

	interface SentenceParser {
		public boolean parse(String [] tokens, GPSPosition position);
	}
	
	// utilities for conversion of minute based latitutde and longitude data
	// from GPS Chips to lat longitudes
	static float Latitude2Decimal(String lat, String NS) {
		float med = Float.parseFloat(lat.substring(2))/60.0f;
		med +=  Float.parseFloat(lat.substring(0, 2));
		if(NS.startsWith("S")) {
			med = -med;
		}
		return med;
	}

	static float Longitude2Decimal(String lon, String WE) {
		float med = Float.parseFloat(lon.substring(3))/60.0f;
		med +=  Float.parseFloat(lon.substring(0, 3));
		if(WE.startsWith("W")) {
			med = -med;
		}
		return med;
	}

	// Parsers for different type of GPS record sentences.
	class GPGGA implements SentenceParser {
		public boolean parse(String [] tokens, GPSPosition position) {
			position.time = Float.parseFloat(tokens[1]);
			position.lat = Latitude2Decimal(tokens[2], tokens[3]);
			position.lon = Longitude2Decimal(tokens[4], tokens[5]);
			position.quality = Integer.parseInt(tokens[6]);
			position.altitude = Float.parseFloat(tokens[9]);
			return true;
		}
	}
	
	class GPGGL implements SentenceParser {
		public boolean parse(String [] tokens, GPSPosition position) {
			position.lat = Latitude2Decimal(tokens[1], tokens[2]);
			position.lon = Longitude2Decimal(tokens[3], tokens[4]);
			position.time = Float.parseFloat(tokens[5]);
			return true;
		}
	}
	
	class GPRMC implements SentenceParser {
		public boolean parse(String [] tokens, GPSPosition position) {
			position.time = Float.parseFloat(tokens[1]);
			position.lat = Latitude2Decimal(tokens[3], tokens[4]);
			position.lon = Longitude2Decimal(tokens[5], tokens[6]);
			position.velocity = Float.parseFloat(tokens[7]);
			position.dir = Float.parseFloat(tokens[8]);
			return true;
		}
	}
	
	class GPVTG implements SentenceParser {
		public boolean parse(String [] tokens, GPSPosition position) {
			position.dir = Float.parseFloat(tokens[3]);
			return true;
		}
	}
	
	class GPRMZ implements SentenceParser {
		public boolean parse(String [] tokens, GPSPosition position) {
			position.altitude = Float.parseFloat(tokens[1]);
			return true;
		}
	}

	// Add a new parser format here.
	
	public class GPSPosition {
		public float time = 0.0f;
		public float lat = 0.0f;
		public float lon = 0.0f;
		public boolean fixed = false;
		public int quality = 0;
		public float dir = 0.0f;
		public float altitude = 0.0f;
		public float velocity = 0.0f;
		
		public void updatefix() {
			fixed = quality > 0;
		}

		public JSONObject toJSONString() {
			JSONObject rJsonObject = new JSONObject(true);
			rJsonObject.put("lat", lat);
			rJsonObject.put("long", lon);
			rJsonObject.put("time", time);
			rJsonObject.put("Q", quality);
			rJsonObject.put("dir", dir);
			rJsonObject.put("alt", altitude);
			rJsonObject.put("vel", velocity);
			return rJsonObject;
		}
		
		public String toString() {
			return String.format("POSITION: lat: %f, lon: %f, time: %f, Q: %d, dir: %f, alt: %f, vel: %f", lat, lon, time, quality, dir, altitude, velocity);
		}
	}
	
	GPSPosition position = new GPSPosition();
	
	private static final Map<String, SentenceParser> sentenceParsers = new HashMap<String, SentenceParser>();
	
    public NMEAServlet() {
    	sentenceParsers.put("GPGGA", new GPGGA());
    	sentenceParsers.put("GPGGL", new GPGGL());
    	sentenceParsers.put("GPRMC", new GPRMC());
    	sentenceParsers.put("GPRMZ", new GPRMZ());
    	sentenceParsers.put("GPVTG", new GPVTG());
    }
    
	public GPSPosition parse(String line) {
		
		if(line.startsWith("$")) {
			String nmea = line.substring(1);
			String[] tokens = nmea.split(",");
			String type = tokens[0];
			//TODO check crc
			if(sentenceParsers.containsKey(type)) {
				sentenceParsers.get(type).parse(tokens, position);
			}
			position.updatefix();
		}
		
		return position;
	}

	// Object Response Structure
	// -------------------------
	// { "idNumbers" : { "lat", "long", "time" , "Q", "dir", "alt", "vel" } , ... }
	// -------------------------
	/* Sample Response
		{
		  "1": {
		    "lat": 60.073490142822266,
		    "long": 19.67548179626465,
		    "time": 94154,
		    "Q": 2,
		    "dir": 82.0999984741211,
		    "alt": 0.699999988079071,
		    "vel": 1.7999999523162842
		  },
		  "2": {
		    "lat": 60.07347106933594,
		    "long": 19.675262451171875,
		    "time": 94140,
		    "Q": 2,
		    "dir": 82.0999984741211,
		    "alt": 0.10000000149011612,
		    "vel": 1.7999999523162842
		  }
		}
	*/

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Query post = RemoteAccess.evaluate(request);

    	// Sample usage for each line of NMEA Sentence data
    	// GPSPosition a = new GPSPosition();
    	// a = parse("$GPGGA,220550,4124.7580,N,08152.2565,W,2,04,4.4,235.1,M,-34.0,M,,*7B");

    	if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

    	// Send the stream URL as stream parameter.
    	String stream = post.get("stream", "");
    	String minified = post.get("minified", "");

    	// Download the file/Stream of NMEA data as a buffered stream.
    	URL url = new URL(stream);
    	BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
    	String NMEALine;

    	JSONObject json = new JSONObject(true);
    	int lines = 0;

    	// Read each buffered stream line by line and parse and convert NMEA to an actual object.
    	while ((NMEALine = in.readLine()) != null) {
    		lines++;
    		GPSPosition gpsObject = new GPSPosition();
    		gpsObject = parse(NMEALine);
    		json.put(String.valueOf(lines), gpsObject.toJSONString());
    	}

    	in.close();

    	// Start the printwriter and get ready to respond with json
    	PrintWriter sos = response.getWriter();
    	if (minified != "true") {
    		sos.print(json.toString(2));
    	}
    	else {
    		sos.print(json.toString());
    	}
    	sos.println();

    	post.finalize();
    }

}
