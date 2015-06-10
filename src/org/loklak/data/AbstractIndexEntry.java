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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.loklak.tools.UTF8;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

public abstract class AbstractIndexEntry implements IndexEntry {

    public AbstractIndexEntry() {
    }
    
    public String toString() {
        try {
            final StringWriter s = new StringWriter();
            final JsonGenerator g = DAO.jsonFactory.createGenerator(s);
            g.setPrettyPrinter(new DefaultPrettyPrinter());
            this.toJSON(g);
            g.close();
            return s.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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
    public static ArrayList<String> parseArrayList(Object l) {
        assert l == null || l instanceof String  || l instanceof String[] || l instanceof ArrayList<?>;
        if (l == null) return new ArrayList<String>(0);
        if (l instanceof String) {
            ArrayList<String> a = new ArrayList<>();
            a.add((String) l);
            return a;
        }
        if (l instanceof String[]) {
            ArrayList<String> a = new ArrayList<>();
            for (String s: ((String[]) l)) a.add(s);
            return a;
        }
        return (ArrayList<String>) l;
    }
}
