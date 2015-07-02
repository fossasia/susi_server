/**
 *  AbstractIndexEntry
 *  Copyright 26.04.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.data;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.loklak.tools.UTF8;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public abstract class AbstractIndexEntry implements IndexEntry {

    public AbstractIndexEntry() {
    }
    
    public String toString() {
        try {
            ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
            return ow.writeValueAsString(this.toMap());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }

    public byte[] toJSON() {
        String s = toString();
        return s == null ? null : UTF8.getBytes(s);
    }
    
    // helper methods to write json
    
    public final static DateTimeFormatter utcFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
    
    public static void writeDate(JsonGenerator json, String fieldName, long time) throws IOException {
        json.writeObjectField(fieldName, utcFormatter.print(time));
    }
    
    public static void writeArray(JsonGenerator json, String fieldName, Collection<String> array) throws IOException {
        json.writeArrayFieldStart(fieldName);
        for (String o: array) json.writeObject(o);
        json.writeEndArray();
    }
    
    public static void writeArray(JsonGenerator json, String fieldName, String[] array) throws IOException {
        json.writeArrayFieldStart(fieldName);
        for (String o: array) json.writeObject(o);
        json.writeEndArray();
    }
    
    public static void writeArray(JsonGenerator json, String fieldName, double[] array) throws IOException {
        json.writeArrayFieldStart(fieldName);
        for (double o: array) json.writeObject(o);
        json.writeEndArray();
    }
    
    // helper methods to read json

    public static Date parseDate(Object d) {
        if (d == null) return new Date();
        if (d instanceof Date) return (Date) d;
        if (d instanceof Long) return new Date(((Long) d).longValue());
        if (d instanceof String) return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) d).toDate();
        assert false;
        return new Date();
    }
    
    public static Date parseDate(Object d, Date dflt) {
        if (d == null) return dflt;
        if (d instanceof Long) return new Date(((Long) d).longValue());
        if (d instanceof String) return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime((String) d).toDate();
        assert false;
        return dflt;
    }

    public static String parseString(Object s) {
        assert s instanceof String;
        return s == null ? "" : (String) s;
    }
    
    public static long parseLong(Object n) {
        assert n instanceof Number;
        return n == null ? 0 : ((Number) n).longValue();
    }
    
    @SuppressWarnings("unchecked")
    public static LinkedHashSet<String> parseArrayList(Object l) {
        assert l == null || l instanceof String  || l instanceof String[] || l instanceof Collection<?>;
        if (l == null) return new LinkedHashSet<String>();
        if (l instanceof String) {
            LinkedHashSet<String> a = new LinkedHashSet<>();
            a.add((String) l);
            return a;
        }
        if (l instanceof String[]) {
            LinkedHashSet<String> a = new LinkedHashSet<>();
            for (String s: ((String[]) l)) a.add(s);
            return a;
        }
        if (l instanceof LinkedHashSet<?>) {
            return (LinkedHashSet<String>) l;
        }
        LinkedHashSet<String> a = new LinkedHashSet<>();
        for (Object s: ((Collection<?>) l)) a.add((String) s);
        return a;
    }
}
