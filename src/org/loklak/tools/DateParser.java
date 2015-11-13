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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class DateParser {

    public final static long HOUR_MILLIS = 60 * 60 * 1000;
    public final static long DAY_MILLIS = HOUR_MILLIS * 24;
    public final static long WEEK_MILLIS = DAY_MILLIS * 7;

    public final static String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // pattern for a W3C datetime variant of a non-localized ISO8601 date
    public final static String PATTERN_ISO8601MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; // same with milliseconds
    public final static String PATTERN_MONTHDAY = "yyyy-MM-dd"; // the twitter search modifier format
    public final static String PATTERN_MONTHDAYHOURMINUTE = "yyyy-MM-dd HH:mm"; // this is the format which morris.js understands for date-histogram graphs
    
    /** Date formatter/non-sloppy parser for W3C datetime (ISO8601) in GMT/UTC */
    public final static SimpleDateFormat iso8601Format = new SimpleDateFormat(PATTERN_ISO8601, Locale.US);
    public final static SimpleDateFormat iso8601MillisFormat = new SimpleDateFormat(PATTERN_ISO8601MILLIS, Locale.US);
    public final static DateFormat dayDateFormat = new SimpleDateFormat(PATTERN_MONTHDAY, Locale.US);
    public final static DateFormat minuteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    public final static DateFormat secondDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    
    public final static Calendar UTCCalendar = Calendar.getInstance();
    public final static TimeZone UTCtimeZone = TimeZone.getTimeZone("UTC");
    static {
        UTCCalendar.setTimeZone(UTCtimeZone);
        dayDateFormat.setCalendar(UTCCalendar);
        minuteDateFormat.setCalendar(UTCCalendar);
        secondDateFormat.setCalendar(UTCCalendar);
    }

    /**
     * parse a date string for a given time zone
     * @param dateString in format "yyyy-MM-dd", "yyyy-MM-dd HH:mm" or "yyyy-MM-dd_HH:mm"
     * @param timezoneOffset number of minutes, must be negative for locations east of UTC and positive for locations west of UTC
     * @return a calender object representing the parsed date
     * @throws ParseException if the format of the date string is not well-formed
     */
    public static Calendar parse(String dateString, final int timezoneOffset) throws ParseException {
        Calendar cal = Calendar.getInstance(UTCtimeZone);
        if ("now".equals(dateString)) return cal; 
        dateString = dateString.replaceAll("_", " ");
        int p = -1;
        if ((p = dateString.indexOf(':')) > 0) {
            if (dateString.indexOf(':', p + 1) > 0)
                synchronized (secondDateFormat) {
                    cal.setTime(secondDateFormat.parse(dateString));
                } else synchronized (minuteDateFormat) {
                    cal.setTime(minuteDateFormat.parse(dateString));
                }
        } else synchronized (dayDateFormat) {
            cal.setTime(dayDateFormat.parse(dateString));
        }
        cal.add(Calendar.MINUTE, timezoneOffset); // add a correction; i.e. for UTC+1 -60 minutes is added to patch a time given in UTC+1 to the actual time at UTC
        return cal;
    }
    
    public static String toPostDate(Date d) {
        return secondDateFormat.format(d).replace(' ', '_');
    }
    
    public static int getTimezoneOffset() {
        Calendar calendar = new GregorianCalendar();
        TimeZone timeZone = calendar.getTimeZone();
        return - (int) TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset()); // we negate the offset because thats the value which is provided by the browser as well 
    }
    
    /*
    public static String toPostDate(Date d, int timezoneOffset) {
        Calendar cal = Calendar.getInstance(DateParser.UTCtimeZone);
        org.joda.time.DateTime k = (org.joda.time.DateTime) bucket.getKey();
        cal.setTime(k.toDate());
        cal.add(Calendar.MINUTE, -timezoneOffset);
        
    }
    */
    public static void main(String[] args) {
        Calendar calendar = new GregorianCalendar();
        TimeZone timeZone = calendar.getTimeZone();
        System.out.println("the date is           : " + calendar.getTime().getTime());
        System.out.println("the timezoneOffset is : " + getTimezoneOffset());
        String postDate = toPostDate(calendar.getTime());
        System.out.println("the post date is      : " + postDate);
        try {
            System.out.println("post date to date     : " + parse(postDate, getTimezoneOffset()).getTime().getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
