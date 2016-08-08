/**
 *  SourceType
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.objects;

/**
 * The SourceType objects answers on the question "what kind of data format".
 * Do not mix up this with the ProviderType, because there can be many providers for the same SourceType.
 */
public enum SourceType {

    TWITTER(true),        // generated at twitter and scraped from there
    FOSSASIA_API(false),  // imported from FOSSASIA API data,
    OPENWIFIMAP(false),   // imported from OpenWifiMap API data
    NODELIST(false),      // imported from Freifunk Nodelist
    NETMON(false),        // imported from Freifunk Netmon
    FREIFUNK_NODE(false), // imported from Freifunk wifi router node (custom schema)
    NINUX(false),         // imported from Ninux http://map.ninux.org
    GEOJSON(false),       // geojson feature collection provided from remote peer
    GENERIC(false);       // no specific format
    
    private final boolean propagate;
    
    private SourceType(boolean propagate) throws RuntimeException {
        this.propagate = propagate;
    }
    
    /**
     * we want type names to be in uppercase. Persons who implement new types names must have a search-engine view of the meaning of types
     * and we want that type names are considered as constant name for similar services. The number of type names should be small and equal
     * to the number of services which loklak supports.
     * @param typeName
     * @return true if the name is valid
     */
    public static boolean isValid(String typeName) {
        if (typeName == null) return false;
        try {
            return SourceType.valueOf(typeName) != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    public static SourceType byName(String typeName) {
        if (typeName == null) return SourceType.GENERIC;
        try {
            return SourceType.valueOf(typeName);
        } catch (IllegalArgumentException e) {
            return SourceType.GENERIC;
        }
    }
    
    public boolean propagate() {
        return this.propagate;
    }
}
