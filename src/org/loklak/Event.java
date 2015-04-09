/**
 *  Event
 *  Copyright 09.04.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class Event {
    
    private LinkedHashMap<String, Object> map; // we use a linked hashmap to preserve the order of the entries for better representation in saved lists
    private long last_harvested_time, next_harvesting_time;

    public Event() {
        this.map = new LinkedHashMap<>();
        this.last_harvested_time = 0;
        this.next_harvesting_time = System.currentTimeMillis();
    }

    public Event(
            final String name, final String harvest_query,
            final Date creation_date,
            final Date prep_start, final Date start,
            final Date end, final Date trail_end,
            final int timezoneOffset
            ) {
        this();
        this.map.put("start_date", start); // this must be first to make lists sortable in dump lists
        this.map.put("end_date", end);
        this.map.put("prep_date", prep_start);
        this.map.put("trail_date", trail_end);
        this.map.put("timezoneOffset", timezoneOffset);
        this.map.put("creation_date", creation_date);
        this.map.put("name", name);
        this.map.put("harvest_query", harvest_query);
    }
    
    public Event(
            final String name, final String harvest_query,
            final String prep_date, String start_date,
            final String end_date, final String trail_date,
            final int timezoneOffset
            ) throws ParseException {
        this(
            name,
            harvest_query,
            new Date(),
            DAO.parseDateModifier(prep_date, timezoneOffset).getTime(),
            DAO.parseDateModifier(start_date, timezoneOffset).getTime(),
            DAO.parseDateModifier(end_date, timezoneOffset).getTime(),
            DAO.parseDateModifier(trail_date, timezoneOffset).getTime(),
            timezoneOffset
        );
    }
    
    public Event(final Map<String, Object> map) {
        this();
        this.map.putAll(map);
    }

    public String getName() {
        return (String) this.map.get("name");
    }

    public String getHarvestQuery() {
        return (String) this.map.get("harvest_query");
    }

    public Date getCreationDate() {
        return (Date) this.map.get("creation_date");
    }
    
    public Date getPrepDate() {
        return (Date) this.map.get("prep_date");
    }

    public Date getStartDate() {
        return (Date) this.map.get("start_date");
    }

    public Date getEndDate() {
        return (Date) this.map.get("end_date");
    }

    public Date getTrailDate() {
        return (Date) this.map.get("trail_date");
    }
    
    public int getTimezoneOffset() {
        return (Integer) this.map.get("timezoneOffset");
    }

    public long getLastHarvested() {
        return this.last_harvested_time;
    }

    public void setLastHarvested(long d) {
        this.last_harvested_time = d;
    }

    public long getNextHarvesting() {
        return this.next_harvesting_time;
    }

    public void setNextHarvesting(long d) {
        this.next_harvesting_time = d;
    }
    
    public void toJSON(XContentBuilder m) {
        try {
            m.map(this.map);
        } catch (IOException e) {
        }
    }
    
    public XContentBuilder toJSON() {
        try {
            XContentBuilder m = XContentFactory.jsonBuilder();
            toJSON(m);
            return m;
        } catch (IOException e) {
            return null;
        }
    }
    
    public String toString() {
        return toJSON().bytes().toUtf8();
    }
    
    public static void main(String args[]) {
        try {
            Event event = new Event("FOSSASIA", "#fossasia", "2015-01-01", "2015-03-13 09:00", "2015-03-15 18:00", "2015-03-17", -420);
            System.out.println(event.toString());
        } catch (ParseException e1) {
        }
    }
}
