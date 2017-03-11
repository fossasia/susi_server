/**
 *  SusiAwareness
 *  Copyright 11.03.2017 by Michael Peter Christen, @0rb1t3r
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


package org.loklak.susi;

import java.util.ArrayList;

/**
 * Awareness is a sequence of cognitions.
 */
public class SusiAwareness extends ArrayList<SusiCognition> {

    private static final long serialVersionUID = -8152874284351943539L;

    /**
     * perception of time
     * this is a silly method that defines the perception of time as the length of the cognition sequence
     * @return an abstract number regarding the perception of time
     */
    public int getTime() {
        return this.size();
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[\n");
        this.forEach(cognition -> sb.append(cognition.toString()).append('\n'));
        sb.append("]\n");
        return sb.toString();
    }
}
