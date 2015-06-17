/**
 *  GeoNames.java
 *  Copyright 2010 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
 *  first published 16.05.2010 on http://yacy.net
 *
 *  This file is part of YaCy Content Integration
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

package org.loklak.geo;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.loklak.tools.CommonPattern;
import org.loklak.tools.UTF8;

public class GeoNames {
    
    private final Map<Integer, GeoLocation> id2loc;
    private final HashMap<Integer, List<Integer>> hash2ids;
    
    
    public GeoNames(final File file, long minPopulation) {
        // this is a processing of the cities1000.zip file from http://download.geonames.org/export/dump/

        this.id2loc = new HashMap<>();
        this.hash2ids = new HashMap<>();

        if ( file == null || !file.exists() ) {
            return;
        }
        ZipFile zf = null;
        BufferedReader reader = null;
        try {
            zf = new ZipFile(file);
            String entryName = file.getName();
            entryName = entryName.substring(0, entryName.length() - 3) + "txt";
            final ZipEntry ze = zf.getEntry(entryName);
            final InputStream is = zf.getInputStream(ze);
            reader = new BufferedReader(new InputStreamReader(is, UTF8.charset));
        } catch (final IOException e ) {
            return;
        }

/* parse this fields:
---------------------------------------------------
00 geonameid         : integer id of record in geonames database
01 name              : name of geographical point (utf8) varchar(200)
02 asciiname         : name of geographical point in plain ascii characters, varchar(200)
03 alternatenames    : alternatenames, comma separated varchar(5000)
04 latitude          : latitude in decimal degrees (wgs84)
05 longitude         : longitude in decimal degrees (wgs84)
06 feature class     : see http://www.geonames.org/export/codes.html, char(1)
07 feature code      : see http://www.geonames.org/export/codes.html, varchar(10)
08 country code      : ISO-3166 2-letter country code, 2 characters
09 cc2               : alternate country codes, comma separated, ISO-3166 2-letter country code, 60 characters
10 admin1 code       : fipscode (subject to change to iso code), see exceptions below, see file admin1Codes.txt for display names of this code; varchar(20)
11 admin2 code       : code for the second administrative division, a county in the US, see file admin2Codes.txt; varchar(80)
12 admin3 code       : code for third level administrative division, varchar(20)
13 admin4 code       : code for fourth level administrative division, varchar(20)
14 population        : bigint (8 byte int)
15 elevation         : in meters, integer
16 dem               : digital elevation model, srtm3 or gtopo30, average elevation of 3''x3'' (ca 90mx90m) or 30''x30'' (ca 900mx900m) area in meters, integer. srtm processed by cgiar/ciat.
17 timezone          : the timezone id (see file timeZone.txt) varchar(40)
18 modification date : date of last modification in yyyy-MM-dd format
*/
        try {
            String line;
            String[] fields;
            Set<String> locnames;
            while ( (line = reader.readLine()) != null ) {
                if ( line.isEmpty() ) {
                    continue;
                }
                fields = CommonPattern.TAB.split(line);
                final long population = Long.parseLong(fields[14]);
                if (minPopulation > 0 && population < minPopulation) continue;
                final int geonameid = Integer.parseInt(fields[0]);
                locnames = new HashSet<>();
                locnames.add(fields[1]);
                locnames.add(fields[2]);
                for (final String s : CommonPattern.COMMA.split(fields[3])) locnames.add(s);
                
                final GeoLocation geoLocation = new GeoLocation(Float.parseFloat(fields[4]), Float.parseFloat(fields[5]), fields[1]);
                geoLocation.setPopulation(population);
                this.id2loc.put(geonameid, geoLocation);
                for (final String name : locnames) {
                    if (name.length() < 4) continue;
                    int lochash = name.toLowerCase().hashCode();
                    List<Integer> locs = this.hash2ids.get(lochash);
                    if (locs == null) {locs = new ArrayList<Integer>(1); this.hash2ids.put(lochash, locs);}
                    locs.add(geonameid);
                }
            }
            if (reader != null) reader.close();
            if (zf != null) zf.close();
        } catch (final IOException e ) {
        }
    }
    
    public GeoLocation analyse(String text, int maxlength) {
        LinkedHashMap<Integer, String> mix = mix(split(text), maxlength);
        for (Map.Entry<Integer, String> entry: mix.entrySet()) {
            List<Integer> locs = this.hash2ids.get(entry.getKey());
            if (locs == null || locs.size() == 0) continue;
            TreeMap<Long, GeoLocation> cand = new TreeMap<>();
            for (Integer i: locs) {
                GeoLocation loc = this.id2loc.get(i);
                if (loc != null && entry.getValue().toLowerCase().equals(loc.getName().toLowerCase())) cand.put(-loc.getPopulation(), loc);
            }
            if (cand.size() > 0) return cand.values().iterator().next(); // the first entry is the location with largest population
        }
        return null;
    }
    
    public static LinkedHashMap<Integer, String> mix(final ArrayList<Map.Entry<String, String>> text, final int maxlength) {
        Map<Integer, Map<Integer, String>> a = new TreeMap<>(); // must be a TreeMap provide order on reverse word count
        for (int i = 0; i < text.size(); i++) {
            for (int x = 1; x <= Math.min(text.size() - i, maxlength); x++) {
                StringBuilder o = new StringBuilder(10 * x);
                StringBuilder l = new StringBuilder(10 * x);
                for (int j = 0; j < x; j++) {
                    Map.Entry<String, String> word = text.get(i + j);
                    if (j != 0) {
                        l.append(' ');
                        o.append(' ');
                    }
                    l.append(word.getKey());
                    o.append(word.getValue());
                }
                Map<Integer, String> m = a.get(-x);
                if (m == null) {m = new HashMap<>(); a.put(-x, m);}
                m.put(l.toString().hashCode(), o.toString());
            }
        }
        
        // now order the maps by the number of words
        LinkedHashMap<Integer, String> r = new LinkedHashMap<>();
        for (Map<Integer,String> m: a.values())r.putAll(m);
        return r;
    }

    public static ArrayList<Map.Entry<String, String>> split(final String text) {
        ArrayList<Map.Entry<String, String>> a = new ArrayList<>(1 + text.length() / 6);
        final StringBuilder o = new StringBuilder();
        final StringBuilder l = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == ' ') {
                if (o.length() > 0) {a.add(new AbstractMap.SimpleEntry<String, String>(l.toString(), o.toString())); o.setLength(0); l.setLength(0);}
                continue;
            }
            if (Character.isLetterOrDigit(c)) {
                o.append(c);
                l.append(Character.toLowerCase(c));
            }
        }
        if (o.length() > 0) {a.add(new AbstractMap.SimpleEntry<String, String>(l.toString(), o.toString())); o.setLength(0); l.setLength(0);}
        return a;
    }
    
    public static void main(String[] args) {
        ArrayList<Map.Entry<String, String>> split = split("Hoc est Corpus meus");
        LinkedHashMap<Integer, String> mix = mix(split, 3);
        for (Map.Entry<Integer, String> entry: mix.entrySet()) {
            System.out.println("code:" + entry.getKey() + "; string:" + entry.getValue());
        }
    }
    
}
