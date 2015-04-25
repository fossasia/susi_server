/**
 *  Campaign
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
import java.util.Map;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.tools.DateParser;

public class Campaign implements Comparator<Campaign>, Comparable<Campaign> {
    
    private LinkedHashMap<String, Object> map; // we use a linked hashmap to preserve the order of the entries for better representation in saved lists
    private long start_time, end_time;
    private String id;
    
    public Campaign() {
        this.map = new LinkedHashMap<>();
    }

    /**
     * create an campaign with a dumped map
     * @param campaignMap
     */
    public Campaign(Map<String, Object> campaignMap) {
        this();
        this.map.putAll(campaignMap);
        this.start_time = ((Date) this.map.get("start_date")).getTime();
        this.end_time = ((Date) this.map.get("end_date")).getTime();
        this.id = DateParser.minuteDateFormat.format((Date) this.map.get("start_date")).replace(' ', '_') + "-" + DateParser.minuteDateFormat.format((Date) this.map.get("end_date")).replace(' ', '_') + "-" + Math.abs(((String) this.map.get("query")).hashCode()) + "-" + Math.abs(((String) this.map.get("name")).hashCode());
    }
    
    /**
     * create an campaign
     * @param query the query which can be used to harvest the campaign
     * @param name the name of the campaign
     * @param creation_date the creation time of the campaign
     * @param start_date the start of the campaign
     * @param end_date   the end of the campaign
     * @param timezoneOffset the offset of the time zone of the creating client
     */
    public Campaign(
            final String query, final String name, 
            final Date creation_date,
            final Date start_date, final Date end_date,
            final int timezoneOffset
            ) {
        this();
        this.map.put("start_date", start_date); // this must be first to make lists sortable in dump lists
        this.map.put("end_date", end_date);
        this.map.put("timezoneOffset", timezoneOffset);
        this.map.put("creation_date", creation_date);
        this.map.put("query", query);
        this.map.put("name", name);
        this.start_time = start_date.getTime();
        this.end_time = end_date.getTime();
        this.id = DateParser.minuteDateFormat.format(start_date).replace(' ', '_') + "-" + DateParser.minuteDateFormat.format(end_date).replace(' ', '_') + "-" + Math.abs(query.hashCode()) + "-" + Math.abs(name.hashCode());
    }

    /**
     * create an campaign
     * @param query the query which can be used to harvest the campaign
     * @param name the name of the campaign
     * @param start_date "YYYY-MM-dd HH:mm" the start of the campaign
     * @param end_date   "YYYY-MM-dd HH:mm" the end of the campaign
     * @param timezoneOffset the offset of the time zone of the creating client, used to parse the date
     */
    public Campaign(
            final String query, final String name,
            final String start_date, final String end_date,
            final int timezoneOffset
            ) throws ParseException {
        this(
            name,
            query,
            new Date(),
            DateParser.parse(start_date, timezoneOffset).getTime(),
            DateParser.parse(end_date, timezoneOffset).getTime(),
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

    public long getStartTime() {
        return this.start_time;
    }

    public long getEndTime() {
        return this.end_time;
    }
    
    public int getTimezoneOffset() {
        return (Integer) this.map.get("timezoneOffset");
    }
    
    public String getID() {
        return this.id;
    }
    
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public int compareTo(Campaign o) {
        return compare(this, o);
    }

    @Override
    public int compare(Campaign o1, Campaign o2) {
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
            Campaign campaign = new Campaign("FOSSASIA", "#fossasia", "2015-03-13 09:00", "2015-03-15 18:00", -420);
            System.out.println(campaign.toString());
        } catch (ParseException e1) {
        }
    }
}
