/**
 *  RSSMessage
 *  Copyright 2007 by Michael Peter Christen
 *  First released 16.7.2007 at http://yacy.net
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

package org.loklak.rss;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.loklak.tools.CharacterCoding;

public class RSSMessage {
    
    public final static Pattern SPACE       = Pattern.compile(" ");
    public final static Pattern COMMA       = Pattern.compile(",");
    public final static Pattern SEMICOLON   = Pattern.compile(";");
    
    /** pattern for a W3C datetime variant of a non-localized ISO8601 date */
    //private static final String PATTERN_ISO8601 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    protected static final TimeZone TZ_GMT = TimeZone.getTimeZone("GMT");
    
    /** Date formatter/non-sloppy parser for W3C datetime (ISO8601) in GMT/UTC */
    //private static final SimpleDateFormat FORMAT_ISO8601 = new SimpleDateFormat(PATTERN_ISO8601, Locale.US);

    /** Date formatter/parser for standard compliant HTTP header dates (RFC 1123) */
    private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss Z"; // with numeric time zone indicator as defined in RFC5322
    private static final String PATTERN_RFC1036 = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    private static final String PATTERN_ANSIC   = "EEE MMM d HH:mm:ss yyyy";
    public  static final SimpleDateFormat FORMAT_RFC1123      = new SimpleDateFormat(PATTERN_RFC1123, Locale.US);
    public  static final SimpleDateFormat FORMAT_RFC1036      = new SimpleDateFormat(PATTERN_RFC1036, Locale.US);
    public  static final SimpleDateFormat FORMAT_ANSIC        = new SimpleDateFormat(PATTERN_ANSIC, Locale.US);
    private static final String PATTERN_SHORT_SECOND = "yyyyMMddHHmmss";
    
    private static final SimpleDateFormat SHORT_SECOND_FORMATTER  = new SimpleDateFormat(PATTERN_SHORT_SECOND, Locale.US);
    
    /**
     * RFC 2616 requires that HTTP clients are able to parse all 3 different
     * formats. All times MUST be in GMT/UTC, but ...
     */
    private static final SimpleDateFormat[] FORMATS_HTTP = new SimpleDateFormat[] {
            // RFC 1123/822 (Standard) "Mon, 12 Nov 2007 10:11:12 GMT"
            FORMAT_RFC1123,
            // RFC 1036/850 (old)      "Monday, 12-Nov-07 10:11:12 GMT"
            FORMAT_RFC1036,
            // ANSI C asctime()        "Mon Nov 12 10:11:12 2007"
            FORMAT_ANSIC,
    };
    static {
        FORMAT_RFC1123.setTimeZone(TZ_GMT);
    }
    
    
    public static enum Token {

        title(new String[]{"title","atom:title","rss:title"}),
        link(new String[]{"link","atom:link","rss:link"}),
        description(new String[]{"description","subtitle","atom:subtitle","rss:description"}),
        pubDate(new String[]{"pubDate","lastBuildDate","updated","rss:lastBuildDate","rss:updated"}),
        copyright(new String[]{"copyright","publisher"}),
        author(new String[]{"author","creator"}),
        subject(new String[]{"subject"}),
        category(new String[]{"category"}),
        referrer(new String[]{"referrer","referer"}),
        language(new String[]{"language"}),
        guid(new String[]{"guid"}),
        ttl(new String[]{"ttl"}),
        docs(new String[]{"docs"}),
        size(new String[]{"size","length","yacy:size"}),
        lon(new String[]{"geo:lon"}),
        lat(new String[]{"geo:lat"});
        //point("gml:pos,georss:point,coordinates");
        
        private Set<String> keys;

        private Token(final String[] keylist) {
            this.keys = new HashSet<String>();
            this.keys.addAll(Arrays.asList(keylist));
        }

        public String valueFrom(final Map<String, String> map, final String dflt) {
            String value;
            for (final String key: this.keys) {
                value = map.get(key);
                if (value != null) return value;
            }
            return dflt;
        }

        public Set<String> keys() {
            return this.keys;
        }

        @Override
        public String toString() {
            return this.keys.size() == 0 ? "" : this.keys.iterator().next();
        }
    }
    
    private static Map<String, Token> tokenNick2Token = new HashMap<String, Token>();
    static {
        for (Token t: Token.values()) {
            for (String nick: t.keys) tokenNick2Token.put(nick, t);
        }
    }

    public static Token valueOfNick(String nick) {
        return tokenNick2Token.get(nick);
    }
    
    private static String artificialGuidPrefix = "c0_";
    private static String calculatedGuidPrefix = "c1_";
    public static final RSSMessage POISON = new RSSMessage("", "", "");

    public static final HashSet<String> tags = new HashSet<String>();
    static {
        for (final Token token: Token.values()) {
            tags.addAll(token.keys());
        }
    }

    private final Map<String, String> map;

    public RSSMessage(final String title, final String description, final String link) {
        this.map = new HashMap<String, String>();
        if (title.length() > 0) this.map.put(Token.title.name(), title);
        if (description.length() > 0) this.map.put(Token.description.name(), description);
        if (link.length() > 0) this.map.put(Token.link.name(), link);
        this.map.put(Token.pubDate.name(), FORMAT_RFC1123.format(new Date()));
        this.map.put(Token.guid.name(), artificialGuidPrefix + Integer.toHexString((title + description + link).hashCode()));
    }

    public RSSMessage(final String title, final String description, final URL link, final String guid) {
        this.map = new HashMap<String, String>();
        if (title.length() > 0) this.map.put(Token.title.name(), title);
        if (description.length() > 0) this.map.put(Token.description.name(), description);
        this.map.put(Token.link.name(), link.toExternalForm());
        this.map.put(Token.pubDate.name(), FORMAT_RFC1123.format(new Date()));
        if (guid.length() > 0) this.map.put(Token.guid.name(), guid);
    }

    public RSSMessage() {
        this.map = new HashMap<String, String>();
    }

    public void setValue(final Token token, final String value) {
        if (value.length() > 0) this.map.put(token.name(), value);
    }

    public String getTitle() {
        return Token.title.valueFrom(this.map, "");
    }

    public String getLink() {
        return Token.link.valueFrom(this.map, "");
    }

    public boolean equals(final Object o) {
        return (o instanceof RSSMessage) && ((RSSMessage) o).getLink().equals(getLink());
    }

    public int hashCode() {
        return getLink().hashCode();
    }

    public int compareTo(final RSSMessage o) {
        return getLink().compareTo(o.getLink());
    }

    public int compare(final RSSMessage o1, final RSSMessage o2) {
        return o1.compareTo(o2);
    }

    public List<String> getDescriptions() {
        List<String> ds = new ArrayList<String>();
        String d = Token.description.valueFrom(this.map, "");
        if (d.length() > 0) ds.add(d);
        return ds;
    }

    public String getAuthor() {
        return Token.author.valueFrom(this.map, "");
    }

    public String getCopyright() {
        return Token.copyright.valueFrom(this.map, "");
    }

    public String getCategory() {
        return Token.category.valueFrom(this.map, "");
    }

    public String[] getSubject() {
        final String subject = Token.subject.valueFrom(this.map, "");
        if (subject.indexOf(',') >= 0) return COMMA.split(subject);
        if (subject.indexOf(';') >= 0) return SEMICOLON.split(subject);
        return SPACE.split(subject);
    }

    public String getReferrer() {
        return Token.referrer.valueFrom(this.map, "");
    }

    public String getLanguage() {
        return Token.language.valueFrom(this.map, "");
    }

    public Date getPubDate() {
        final String dateString = Token.pubDate.valueFrom(this.map, "");
        Date date;
        try {
            date = FORMAT_RFC1123.parse(dateString);
        } catch (final ParseException e) {
            try {
                date = SHORT_SECOND_FORMATTER.parse(dateString);
            } catch (final ParseException e1) {
                date = parseHTTPDate(dateString);
            }
        }
        return date;
    }
    
    public static Date parseHTTPDate(String s) {
        s = s.trim();
        if (s == null || s.length() < 9) return null;
        for (final SimpleDateFormat format: FORMATS_HTTP) synchronized (format) {
            try { return format.parse(s); } catch (final ParseException e) {}
        }
        return null;
    }
    public String getGuid() {
        String guid = Token.guid.valueFrom(this.map, "");
        if ((guid.isEmpty() || guid.startsWith(artificialGuidPrefix)) &&
            (this.map.containsKey("title") || this.map.containsKey("description") || this.map.containsKey("link"))) {
            guid = calculatedGuidPrefix + Integer.toHexString(getTitle().hashCode() + getDescriptions().hashCode() + getLink().hashCode());
            this.map.put("guid", guid);
        }
        return guid;
    }

    public String getTTL() {
        return Token.ttl.valueFrom(this.map, "");
    }

    public String getDocs() {
        return Token.docs.valueFrom(this.map, "");
    }

    public long getSize() {
        final String size = Token.size.valueFrom(this.map, "-1");
        return (size == null || size.isEmpty()) ? -1 : Long.parseLong(size);
    }

    public String getFulltext() {
        final StringBuilder sb = new StringBuilder(300);
        for (final String s: this.map.values()) sb.append(s).append(' ');
        return sb.toString();
    }

    public float getLon() {
        return Float.parseFloat(Token.lon.valueFrom(this.map, "0.0"));
    }

    public float getLat() {
        return Float.parseFloat(Token.lat.valueFrom(this.map, "0.0"));
    }

    public String toString() {
        return this.map.toString();
    }

    public String toString(boolean withItemTag) {
        StringBuilder sb = new StringBuilder();
        if (withItemTag) sb.append("<item>\n");
        if (this.map.containsKey(Token.title.name())) sb.append("<title>").append(CharacterCoding.unicode2xml(this.map.get(Token.title.name()), false)).append("</title>\n");
        if (this.map.containsKey(Token.link.name())) sb.append("<link>").append(CharacterCoding.unicode2xml(this.map.get(Token.link.name()), false)).append("</link>\n");
        if (this.map.containsKey(Token.description.name())) sb.append("<description>").append(CharacterCoding.unicode2xml(this.map.get(Token.description.name()), false)).append("</description>\n");
        if (this.map.containsKey(Token.pubDate.name())) sb.append("<pubDate>").append(this.map.get(Token.pubDate.name())).append("</pubDate>\n");
        if (this.map.containsKey(Token.guid.name())) sb.append("<guid isPermaLink=\"false\">").append(this.map.get(Token.guid.name())).append("</guid>\n");
        if (withItemTag) sb.append("</item>\n");
        return sb.toString();
    }
    
    public void setAuthor(final String author) {
        setValue(Token.author, author);
    }

    public void setCategory(final String category) {
        setValue(Token.category, category);
    }

    public void setCopyright(final String copyright) {
        setValue(Token.copyright, copyright);
    }

    public void setSubject(final String[] tags) {
        final StringBuilder sb = new StringBuilder(tags.length * 10);
        for (final String tag: tags) sb.append(tag).append(',');
        if (sb.length() > 0) sb.setLength(sb.length() - 1);
        setValue(Token.subject, sb.toString());
    }

    public void setDescription(final String description) {
        setValue(Token.description, description);
    }

    public void setDocs(final String docs) {
        setValue(Token.docs, docs);
    }

    public void setGuid(final String guid) {
        setValue(Token.guid, guid);
    }

    public void setLanguage(final String language) {
        setValue(Token.language, language);
    }

    public void setLink(final String link) {
        setValue(Token.link, link);
    }

    public void setPubDate(final Date pubdate) {
        setValue(Token.pubDate, FORMAT_RFC1123.format(pubdate));
    }

    public void setReferrer(final String referrer) {
        setValue(Token.referrer, referrer);
    }

    public void setSize(final long size) {
        setValue(Token.size, Long.toString(size));
    }

    public void setTitle(final String title) {
        setValue(Token.title, title);
    }

    public static String sizename(int size) {
        if (size < 1024) return size + " bytes";
        size = size / 1024;
        if (size < 1024) return size + " kbyte";
        size = size / 1024;
        if (size < 1024) return size + " mbyte";
        size = size / 1024;
        return size + " gbyte";
    }
}
