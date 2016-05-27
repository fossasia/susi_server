/**
 *  ProviderType
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; wo even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.objects;

public enum ProviderType {
        
    NOONE,   // value assigned during instantiation phase
    SCRAPED, // scraped with this peer from a remote service
    IMPORT,  // external resource imported with special reader
    GENERIC, // pushed as single message at this peer
    REMOTE,  // pushed as message bulk from a remote peer
    GEOJSON; // geojson feature collection provided from remote peer

}
