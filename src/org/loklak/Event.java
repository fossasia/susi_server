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
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

public class Event implements Comparator<Event>, Comparable<Event> {
    
    private LinkedHashMap<String, Object> map; // we use a linked hashmap to preserve the order of the entries for better representation in saved lists
    private long last_harvested_time, next_harvesting_time;
    private long start_time, end_time, prep_time, trail_time;
    private String id;
    
    public Event() {
        this.map = new LinkedHashMap<>();
        this.last_harvested_time = 0;
        this.next_harvesting_time = System.currentTimeMillis();
    }

    /**
     * create an event
     * @param query the query which can be used to harvest the event
     * @param name the name of the event
     * @param creation_date the creation time of the event
     * @param prep_date  a date when preparations for the event started, first time when harvesting shall start
     * @param start_date the start of the event
     * @param end_date   the end of the event
     * @param trail_date the final date when harvesting shall stop
     * @param timezoneOffset the offset of the time zone of the creating client
     */
    public Event(
            final String query, final String name, 
            final Date creation_date,
            final Date prep_date, final Date start_date,
            final Date end_date, final Date trail_date,
            final int timezoneOffset
            ) {
        this();
        this.map.put("start_date", start_date); // this must be first to make lists sortable in dump lists
        this.map.put("end_date", end_date);
        this.map.put("prep_date", prep_date);
        this.map.put("trail_date", trail_date);
        this.map.put("timezoneOffset", timezoneOffset);
        this.map.put("creation_date", creation_date);
        this.map.put("query", query);
        this.map.put("name", name);
        this.start_time = start_date.getTime();
        this.end_time = end_date.getTime();
        this.prep_time = prep_date.getTime();
        this.trail_time = trail_date.getTime();
        this.id = DAO.responseDateFormat.format(start_date).replace(' ', '_') + "-" + DAO.responseDateFormat.format(end_date).replace(' ', '_') + "-" + Math.abs(query.hashCode()) + "-" + Math.abs(name.hashCode());
    }

    /**
     * create an event
     * @param query the query which can be used to harvest the event
     * @param name the name of the event
     * @param prep_date  "YYYY-MM-dd HH:mm" a date when preparations for the event started, first time when harvesting shall start
     * @param start_date "YYYY-MM-dd HH:mm" the start of the event
     * @param end_date   "YYYY-MM-dd HH:mm" the end of the event
     * @param trail_date "YYYY-MM-dd HH:mm" the final date when harvesting shall stop
     * @param timezoneOffset the offset of the time zone of the creating client, used to parse the date
     */
    public Event(
            final String query, final String name,
            final String prep_date, String start_date,
            final String end_date, final String trail_date,
            final int timezoneOffset
            ) throws ParseException {
        this(
            name,
            query,
            new Date(),
            DAO.parseDateModifier(prep_date, timezoneOffset).getTime(),
            DAO.parseDateModifier(start_date, timezoneOffset).getTime(),
            DAO.parseDateModifier(end_date, timezoneOffset).getTime(),
            DAO.parseDateModifier(trail_date, timezoneOffset).getTime(),
            timezoneOffset
        );
    }

    public String getName() {
        return (String) this.map.get("name");
    }

    public String getQuery() {
        return (String) this.map.get("query");
    }

    public Date getCreationDate() {
        return (Date) this.map.get("creation_date");
    }
    
    public long getPrepTime() {
        return this.prep_time;
    }

    public long getStartTime() {
        return this.start_time;
    }

    public long getEndTime() {
        return this.end_time;
    }

    public long getTrailTime() {
        return this.trail_time;
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
    
    public String getID() {
        return this.id;
    }
    
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int compareTo(Event o) {
        return compare(this, o);
    }

    @Override
    public int compare(Event o1, Event o2) {
        int c = o1.getStartTime() < o2.getStartTime() ? -1 : o1.getStartTime() > o2.getStartTime() ? 1 : 0;
        if (c == 0) c = o1.getEndTime() < o2.getEndTime() ? -1 : o1.getEndTime() > o2.getEndTime() ? 1 : 0;
        if (c == 0) c = o1.hashCode() < o2.hashCode() ? -1 : o1.hashCode() > o2.hashCode() ? 1 : 0;
        return c;
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
