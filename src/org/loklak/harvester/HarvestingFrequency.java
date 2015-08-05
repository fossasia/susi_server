/**
 * HarvestingFrequency
 * Copyright 01.08.2015 by Dang Hai An, @zyzo
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.harvester;

public enum HarvestingFrequency {
    THIRTY_MIN(30),           // every half-hour
    AN_HOUR(60),              // every hour
    THREE_HOUR(180),          // every three hour
    A_DAY(1440),              // every day
    NEVER(Integer.MAX_VALUE); // never update

    int frequency;
    HarvestingFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * @return int : frequency in minutes
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * @throws IllegalArgumentException when frequency value is not permitted (not declared as an enum)
     */
    public static HarvestingFrequency valueOf(int frequency) {
        for (HarvestingFrequency f: HarvestingFrequency.values()) {
            if (f.getFrequency() == frequency) {
                return f;
            }
        }
        throw new IllegalArgumentException();
    }
}
