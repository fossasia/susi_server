/**
 *  GeoLocation
 *  Copyright 2009 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 *  first published 08.10.2009 on http://yacy.net
 *
 *  This file is part of YaCy Content Integration
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

import java.util.Collection;
import java.util.Comparator;

public class GeoLocation extends IntegerGeoPoint implements Comparable<GeoLocation>, Comparator<GeoLocation> {

    private Collection<String> names;
    private String iso3166cc; 
    private long population;

    public GeoLocation(final double lat, final double lon, final Collection<String> names, final String iso3166cc) {
        super(lat, lon);
        this.names = names;
        this.iso3166cc = iso3166cc;
    }

    public Collection<String> getNames() {
        return this.names;
    }
    
    public String getIO3166cc() {
        return this.iso3166cc;
    }

    public void setPopulation(long population2) {
        this.population = population2;
    }

    public long getPopulation() {
        return this.population;
    }

    @Override
    public String toString() {
        return this.names.iterator().next() + "@" + super.toString();
    }

    @Override
    public boolean equals(Object loc) {
        if (!(loc instanceof GeoLocation)) return false;
        if (!super.equals(loc)) return false;
        for (String name: this.names) {
            if (((GeoLocation) loc).names.contains(name)) return true;
        }
        return false;
    }

    /**
     * comparator that is needed to use the object inside TreeMap/TreeSet
     * a Location is smaller than another if it has a _greater_ population
     * this order is used to get sorted lists of locations where the first elements
     * have the greatest population
     */
    @Override
    public int compareTo(GeoLocation o) {
        if (this.equals(o)) return 0;
        long s = (ph(this.getPopulation()) << 30) + this.hashCode();
        long t = (ph(o.getPopulation()) << 30) + o.hashCode();
        if (s > t) return -1;
        if (s < t) return  1;
        return 0;
    }

    private static long ph(long population) {
        if (population > 10000) population -= 10000;
        return population;
    }

    @Override
    public int compare(GeoLocation o1, GeoLocation o2) {
        return o1.compareTo(o2);
    }

    public static int degreeToKm(double degree) {
        return (int) (degree * 111.32d);
    }

}
