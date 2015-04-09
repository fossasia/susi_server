/**
 *  DateParser
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


package org.loklak.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class DateParser {

    public final static DateFormat dayDateFormat = new SimpleDateFormat("yyyy-MM-dd"); // this is the twitter search modifier format
    public final static DateFormat minuteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm"); // this is the format which morris.js understands for date-histogram graphs
    
    public final static Calendar UTCCalendar = Calendar.getInstance();
    public final static TimeZone UTCtimeZone = TimeZone.getTimeZone("UTC");
    static {
        UTCCalendar.setTimeZone(UTCtimeZone);
        dayDateFormat.setCalendar(UTCCalendar);
        minuteDateFormat.setCalendar(UTCCalendar);
    }

    /**
     * parse a date string for a given time zone
     * @param dateString in format "yyyy-MM-dd", "yyyy-MM-dd HH:mm" or "yyyy-MM-dd_HH:mm"
     * @param timezoneOffset number of minutes, must be negative for locations east of UTC and positive for locations west of UTC
     * @return a calender object representing the parsed date
     * @throws ParseException if the format of the date string is not well-formed
     */
    public static Calendar parse(String dateString, final int timezoneOffset) throws ParseException {
        dateString = dateString.replaceAll("_", " ");
        Calendar cal = Calendar.getInstance(UTCtimeZone);
        cal.setTime(dateString.indexOf(':') > 0 ? minuteDateFormat.parse(dateString) : dayDateFormat.parse(dateString));
        cal.add(Calendar.MINUTE, timezoneOffset); // add a correction; i.e. for UTC+1 -60 minutes is added to patch a time given in UTC+1 to the actual time at UTC
        return cal;
    }
}
