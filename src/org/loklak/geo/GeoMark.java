/**
 *  GeoMark
 *  Copyright 03.06.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.geo;

public class GeoMark extends GeoLocation implements GeoPoint {

    private double mlon, mlat; // coordinates for the marker
    
    public GeoMark(final GeoLocation loc, final double mlat, final double mlon) {
        super(loc.lat(), loc.lon(), loc.getNames(), loc.getIO3166cc());
        super.setPopulation(loc.getPopulation());
        this.mlat = mlat;
        this.mlon = mlon;
    }
    
    public GeoMark(final GeoLocation loc, final int salt) {
        super(loc.lat(), loc.lon(), loc.getNames(), loc.getIO3166cc());
        super.setPopulation(loc.getPopulation());
        
        // using the population, we compute a location radius
        // example city: middle-high density city (10,000 persons per square kilometer)
        // a large city in that density is Seoul, South Korea: 23,480,000 persons on 2,266 square kilometer
        // a circle with the area of 1 km^2 has the radius:
        // double r = Math.sqrt(1000000 / Math.PI); // meter
        // the radius required to hold the population is
        double r = Math.sqrt(loc.getPopulation() * 1000000 / 10000 / Math.PI); // meter
        // we don't compute a random number for the actual fuzzy location of the marker
        // to make this reproducible, we use a hash of the name and location
        int h = Math.abs((loc.getNames().iterator().next() + loc.lat() + loc.lon() + Integer.toString(salt)).hashCode());
        if (h == Integer.MIN_VALUE) h = 0; // correction of the case that Math.abs is not possible
        // with that hash we compute an actual distance and an angle
        double dist = (h & 0xff) * r / 255.0d / 40000000 * 360; // 40 million meter (the earth) has an angle of 360 degree
        double angle = 2 * Math.PI * ((double) ((h & 0xfff00) >> 8)) / ((double) 0xfff);        
        // now compute a point around the location on a circle for the mark
        this.mlat = this.lat() + Math.sin(angle) * dist;
        this.mlon = this.lon() + Math.cos(angle) * dist;
    }
    
    public double mlon() {
        return this.mlon;
    }
    
    public double mlat() {
        return this.mlat;
    }
    
}
