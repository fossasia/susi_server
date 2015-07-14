/**
 *  LocationSource
 *  Copyright 10.06.2015 by Michael Peter Christen, @0rb1t3r
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

public enum LocationSource {
    
    USER,       // the (loklak) user has set the location, this is a hint that this is a rich tweet.
    REPORT,     // location came from another source in identical way. This may be a IoT import.
    ANNOTATION; // location was detected from annotation text (this applies also to tweets from twitter as they don't send coordinates)
    
}
