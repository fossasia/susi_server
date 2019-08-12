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


package ai.susi.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

public class DateParser {

    public final static long HOUR_MILLIS = 60 * 60 * 1000;
    public final static long DAY_MILLIS = HOUR_MILLIS * 24;
    public final static long WEEK_MILLIS = DAY_MILLIS * 7;
    public final static long MONTH_MILLIS = DAY_MILLIS * 30;

    public final static String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'"; // pattern for a W3C datetime variant of a non-localized ISO8601 date
    public final static String PATTERN_ISO8601MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"; // same with milliseconds
    public final static String PATTERN_MONTHDAY = "yyyy-MM-dd"; // the twitter search modifier format
    public final static String PATTERN_MONTHDAYHOURMINUTE = "yyyy-MM-dd HH:mm"; // this is the format which morris.js understands for date-histogram graphs
    public final static String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss Z"; // with numeric time zone indicator as defined in RFC5322

    /** Date formatter/non-sloppy parser for W3C datetime (ISO8601) in GMT/UTC */
    public final static SimpleDateFormat iso8601Format = new SimpleDateFormat(PATTERN_ISO8601, Locale.US);
    public final static SimpleDateFormat iso8601MillisFormat = new SimpleDateFormat(PATTERN_ISO8601MILLIS, Locale.US); // PREFERRED FORMAT!
    public final static DateFormat dayDateFormat = new SimpleDateFormat(PATTERN_MONTHDAY, Locale.US);
    public final static DateFormat dayMinuteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
    public final static DateFormat daySecondDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public final static DateFormat minuteDateFormat = new SimpleDateFormat("HH:mm", Locale.US);
    public final static DateFormat secondDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);
    public final static SimpleDateFormat FORMAT_RFC1123 = new SimpleDateFormat(PATTERN_RFC1123, Locale.US); // Do not use this format to format! Only for parsing!

    public final static DateTimeFormatter utcFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    public final static Calendar UTCCalendar = Calendar.getInstance();
    public final static TimeZone UTCtimeZone = TimeZone.getTimeZone("UTC");
    private final static List<DateFormat> tryFormats = new ArrayList<>();
    static {
        UTCCalendar.setTimeZone(UTCtimeZone);
        minuteDateFormat.setCalendar(UTCCalendar);    tryFormats.add(minuteDateFormat);
        secondDateFormat.setCalendar(UTCCalendar);    tryFormats.add(secondDateFormat);
        dayMinuteDateFormat.setCalendar(UTCCalendar); tryFormats.add(dayMinuteDateFormat);
        daySecondDateFormat.setCalendar(UTCCalendar); tryFormats.add(daySecondDateFormat);
        dayDateFormat.setCalendar(UTCCalendar);       tryFormats.add(dayDateFormat);
        iso8601Format.setCalendar(UTCCalendar);       tryFormats.add(iso8601Format);
        iso8601MillisFormat.setCalendar(UTCCalendar); tryFormats.add(iso8601MillisFormat);
        FORMAT_RFC1123.setCalendar(UTCCalendar);      tryFormats.add(FORMAT_RFC1123);
    }

    /**
     * parse a date string for a given time zone
     * @param dateString in format "yyyy-MM-dd", "yyyy-MM-dd HH:mm" or "yyyy-MM-dd_HH:mm"
     * @param timezoneOffset number of minutes, must be negative for locations east of UTC and positive for locations west of UTC
     * @return a calender object representing the parsed date
     * @throws ParseException if the format of the date string is not well-formed
     */
    public static Calendar parse(String dateString, final int timezoneOffset) throws ParseException {
        Calendar cal = null;
        dateString = dateString.replaceAll("_", " ").trim();
        // special cases for small time numbers
        if (dateString.endsWith("h") || dateString.endsWith("m") || dateString.endsWith("s")) {
            try {
                int nn = Integer.parseInt(dateString.substring(0, dateString.length() - 1));
                cal = Calendar.getInstance(UTCtimeZone);
                if (dateString.endsWith("h")) {
                    cal.add(Calendar.HOUR, nn);
                    return cal;
                }
                if (dateString.endsWith("m")) {
                    cal.add(Calendar.MINUTE, nn);
                    return cal;
                }
                if (dateString.endsWith("s")) {
                    cal.add(Calendar.SECOND, nn);
                    return cal;
                }
            } catch (NumberFormatException e) {}
        }

        // parse a full date format
        for (DateFormat df: tryFormats) {
            synchronized (df) {
                try {
                    Date td = df.parse(dateString);
                    cal = Calendar.getInstance(UTCtimeZone);
                    cal.setTime(td);
                    break;
                } catch (ParseException e) {
                    continue;
                }
            }
        }
        if (cal == null) throw new ParseException("cannot find parser for time format of " + dateString, 0);
        cal.add(Calendar.MINUTE, -timezoneOffset); // add a correction; i.e. for UTC+1 -60 minutes is added to patch a time given in UTC+1 to the actual time at UTC

        // fix partially given date
        if (cal.get(Calendar.YEAR) == 1970) {
            Calendar now = Calendar.getInstance(UTCtimeZone);
            cal.set(Calendar.YEAR, now.get(Calendar.YEAR));
            cal.set(Calendar.MONTH, now.get(Calendar.MONTH));
            cal.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
        }
        return cal;
    }

    public static Date iso8601MillisParser(String date) {
        try {
            return iso8601MillisFormat.parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    }

    public static String toPostDate(Date d) {
        return daySecondDateFormat.format(d).replace(' ', '_');
    }

    public static int getTimezoneOffset() {
        Calendar calendar = new GregorianCalendar();
        TimeZone timeZone = calendar.getTimeZone();
        return - (int) TimeUnit.MILLISECONDS.toMinutes(timeZone.getRawOffset()); // we negate the offset because thats the value which is provided by the browser as well 
    }

    public static Date oneHourAgo() {
        return new Date(System.currentTimeMillis() - HOUR_MILLIS);
    }

    public static Date oneDayAgo() {
        return new Date(System.currentTimeMillis() - DAY_MILLIS);
    }

    public static Date oneWeekAgo() {
        return new Date(System.currentTimeMillis() - WEEK_MILLIS);
    }

    public static Date oneMonthAgo() {
        return new Date(System.currentTimeMillis() - MONTH_MILLIS);
    }
/*
    private static long lastRFC1123long = 0;
    private static String lastRFC1123string = "";

    public static final String formatRFC1123(final Date date) {
        if (date == null) return "";
        if (Math.abs(date.getTime() - lastRFC1123long) < 1000) {
            //System.out.println("date cache hit - " + lastRFC1123string);
            return lastRFC1123string;
        }
        synchronized (FORMAT_RFC1123) {
            final String s = FORMAT_RFC1123.format(date);
            lastRFC1123long = date.getTime();
            lastRFC1123string = s;
            return s;
        }
    }
*/
    public static final String formatISO8601(final Date date) {
        if (date == null) return "";
        synchronized (iso8601MillisFormat) {
            return iso8601MillisFormat.format(date);
        }
    }

    public static Date parseAnyText(String text, long timezoneOffset) {
        // first try if this is simply a number. Then it is a time delay.
        try {
            long delay = Long.parseLong(text);
            return new Date(System.currentTimeMillis() + delay);
        } catch (NumberFormatException e) {} // we ignore the exception here, because it is expected

        Parser parser = new Parser();
        List<DateGroup> groups = parser.parse(text);
        if (groups.size() == 0) return null;
        DateGroup group = groups.get(0);
        List<Date> dates = group.getDates();
        if (dates.size() == 0) return null;
        return dates.get(0);
    }

    public static void main(String[] args) {
        Calendar calendar = new GregorianCalendar();
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
