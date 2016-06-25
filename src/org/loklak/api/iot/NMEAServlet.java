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

import org.json.JSONObject;
import org.loklak.server.Query;
import org.loklak.http.RemoteAccess;

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

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	Query post = RemoteAccess.evaluate(request);

    	if (post.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;}

    	JSONObject json = new JSONObject(true);
    	json.put("success", "Hello");
    	PrintWriter sos = response.getWriter();
    	sos.print(json.toString(2));
    	sos.println();

    	post.finalize();
    }


}
