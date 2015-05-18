/**
 *  MessageEntry
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; wo even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.data;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.format.ISODateTimeFormat;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.loklak.harvester.SourceType;

public class MessageEntry extends AbstractIndexEntry implements IndexEntry {
    
    protected Date created_at;
    protected SourceType source_type; // where did the message come from
    protected ProviderType provider_type;  // who created the message
    protected String provider_hash, user_screen_name, id_str, text;
    protected URL status_id_url;
    protected long retweet_count, favourites_count;
    protected ArrayList<String> images;
    protected String place_name, place_id;

    // the following can be computed from the tweet data but is stored in the search index to provide statistical data and ranking attributes
    private int without_l_len, without_lu_len, without_luh_len; // the length of tweets without links, users, hashtags
    private String[] hosts, links, mentions, hashtags; // the arrays of links, users, hashtags

    public MessageEntry() throws MalformedURLException {
        this.created_at = new Date();
        this.source_type = SourceType.USER;
        this.provider_type = ProviderType.NOONE;
        this.provider_hash = "";
        this.user_screen_name = "";
        this.id_str = "";
        this.text = "";
        this.status_id_url = null;
        this.retweet_count = 0;
        this.favourites_count = 0;
        this.images = new ArrayList<String>();
        this.place_id = "";
        this.place_name = "";
        this.without_l_len = 0;
        this.without_lu_len = 0;
        this.without_luh_len = 0;
        this.hosts = new String[0];
        this.links = new String[0]; 
        this.mentions = new String[0];
        this.hashtags = new String[0];
    }

    public MessageEntry(Map<String, Object> map) {
        init(map);
    }
    
    @SuppressWarnings("unchecked")
    public void init(Map<String, Object> map) {
        String created_at_string = (String) map.get("created_at");
        if (created_at_string != null && created_at_string.length() > 0) {
            this.created_at = ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(created_at_string).toDate();
        } else {
            this.created_at = new Date(System.currentTimeMillis());
        }
        String source_type_string = (String) map.get("source_type"); if (source_type_string == null) source_type_string = SourceType.USER.name();
        try {
            this.source_type = SourceType.valueOf(source_type_string);
        } catch (IllegalArgumentException e) {
            this.source_type = SourceType.USER;
        }
        String provider_type_string = (String) map.get("provider_type"); if (provider_type_string == null) provider_type_string = ProviderType.GENERIC.name();
        try {
            this.provider_type = ProviderType.valueOf(provider_type_string);
        } catch (IllegalArgumentException e) {
            this.provider_type = ProviderType.GENERIC;
        }
        this.provider_hash = (String) map.get("provider_hash");
        this.user_screen_name = (String) map.get("screen_name");
        this.id_str = (String) map.get("id_str");
        this.text = (String) map.get("text");
        try {
            this.status_id_url = new URL((String) map.get("link"));
        } catch (MalformedURLException e) {
            this.status_id_url = null;
        }
        this.retweet_count = DAO.noNULL((Number) map.get("retweet_count"));
        this.favourites_count = DAO.noNULL((Number) map.get("favourites_count"));
        this.images = DAO.noNULL((ArrayList<String>) map.get("images"));
        this.place_id = DAO.noNULL((String) map.get("place_id"));
        this.place_name = DAO.noNULL((String) map.get("place_name"));
        
        // load enriched data
        enrich();
    }

    final static Pattern SPACEX_PATTERN = Pattern.compile("  +"); // two or more
    final static Pattern URL_PATTERN = Pattern.compile("(?:\\b|^)(https?://.*?)(?:[) ]|$)"); // right boundary must be space since others may appear in urls
    final static Pattern USER_PATTERN = Pattern.compile("(?:[ (]|^)(@..*?)(?:\\b|$)"); // left boundary must be space since the @ is itself a boundary
    final static Pattern HASHTAG_PATTERN = Pattern.compile("(?:[ (]|^)(#..*?)(?:\\b|$)"); // left boundary must be a space since the # is itself a boundary

    /**
     * create enriched data, useful for analytics and ranking:
     * - identify all mentioned users, hashtags and links
     * - count message size without links
     * - count message size without links and without users
     */
    protected void enrich() {
        StringBuilder t = new StringBuilder(this.text);

        // extract the links
        List<String> urls = extract(t, URL_PATTERN, 1);
        t = new StringBuilder(SPACEX_PATTERN.matcher(t).replaceAll(" ").trim());
        this.without_l_len = t.length(); // len_no_l
        
        // extract the users
        List<String> users = extract(t, USER_PATTERN, 1);
        t = new StringBuilder(SPACEX_PATTERN.matcher(t).replaceAll(" ").trim());
        this.without_lu_len = t.length(); // len_no_l_and_users
        
        // extract the hashtags
        List<String> hashtags = extract(t, HASHTAG_PATTERN, 1);
        t = new StringBuilder(SPACEX_PATTERN.matcher(t).replaceAll(" ").trim());
        this.without_luh_len = t.length(); // len_no_l_and_users_and_hashtags

        // extract the hosts from the links
        Set<String> hosts = new LinkedHashSet<String>();
        for (String u: urls) {
            try {
                URL url = new URL(u);
                hosts.add(url.getHost());
            } catch (MalformedURLException e) {}
        }

        this.hosts = new String[hosts.size()];
        int j = 0; for (String host: hosts) this.hosts[j++] = host.toLowerCase();

        this.links = new String[urls.size()];
        for (int i = 0; i < urls.size(); i++) this.links[i] = urls.get(i);
        
        this.mentions = new String[users.size()];
        for (int i = 0; i < users.size(); i++) this.mentions[i] = users.get(i).substring(1);
        
        this.hashtags = new String[hashtags.size()];
        for (int i = 0; i < hashtags.size(); i++) this.hashtags[i] = hashtags.get(i).substring(1).toLowerCase();
    }
    
    private static List<String> extract(StringBuilder s, Pattern p, int g) {
        Matcher m = p.matcher(s.toString());
        List<String> l = new ArrayList<String>();
        while (m.find()) l.add(m.group(g));
        for (String r: l) {int i = s.indexOf(r); s.replace(i, i + r.length(), "");}
        return l;
    }
    
    public URL getStatusIdUrl() {
        return this.status_id_url;
    }
    
    public String getIdStr() {
        return this.id_str;
    }
    
    public long getId() {
        return Long.parseLong(this.id_str);
    }

    public Date getCreatedAtDate() {
        return this.created_at;
    }

    public SourceType getSourceType() {
        return this.source_type;
    }

    @Override
    public void toJSON(XContentBuilder m) {
        toJSON(m, null, true); // very important to include calculated data here because that is written into the index using the abstract index factory
    }
    
    public void toJSON(XContentBuilder m, UserEntry user, boolean calculatedData) {
        try {
            m.startObject();

            // tweet data
            m.field("created_at", this.created_at);
            m.field("screen_name", this.user_screen_name);
            m.field("text", this.text); // the tweet
            m.field("link", this.status_id_url.toExternalForm());
            m.field("id_str", this.id_str);
            m.field("source_type", this.source_type.name());
            m.field("provider_type", this.provider_type.name());
            if (this.provider_hash != null && this.provider_hash.length() > 0) m.field("provider_hash", this.provider_hash);
            m.field("retweet_count", this.retweet_count);
            m.field("favourites_count", this.favourites_count); // there is a slight inconsistency here in the plural naming but thats how it is noted in the twitter api
            m.field("images", this.images);
            m.field("images_count", this.images.size());
            m.field("place_name", this.place_name);
            m.field("place_id", this.place_id);
  
            
            // add statistic/calculated data
            if (calculatedData) {
                m.field("hosts", this.hosts);
                m.field("hosts_count", this.hosts.length);
                m.field("links", this.links);
                m.field("links_count", this.links.length);
                m.field("mentions", this.mentions);
                m.field("mentions_count", this.mentions.length);
                m.field("hashtags", this.hashtags);
                m.field("hashtags_count", this.hashtags.length);
                // experimental, for ranking
                m.field("without_l_len", this.without_l_len);
                m.field("without_lu_len", this.without_lu_len);
                m.field("without_luh_len", this.without_luh_len);
            }
            
            // add user
            if (user != null) {
                m.field("user");
                user.toJSON(m);
            }
            
            //m.field("coordinates", ""); // like {"coordinates":[-75.14310264,40.05701649],"type":"Point"}
            //m.field("entities", ""); // like {"hashtags":[],"urls":[],"user_mentions":[]}
            //m.field("filter_level", "");
            //m.field("user", user.toJSON()); // {"profile_image_url":"...", "name":"Twitter API", "description":"blabla", "location":"San Francisco, CA", "followers_count":665829, "url":"http:\/\/dev.twitter.com", "screen_name":"twitterapi", "id_str":"6253282", "lang":"en", "id":6253282}
            m.endObject();
        } catch (IOException e) {
        }
    }

    public static String html2utf8(String s) {
        int p, q;
        // hex coding &#
        while ((p = s.indexOf("&#")) >= 0) {
            q = s.indexOf(';', p + 2);
            if (q < p) break;
            s = s.substring(0, p) + ((char) Integer.parseInt(s.substring(p + 2, q), 16)) + s.substring(q + 1);
        }
        // octal coding \\u
        while ((p = s.indexOf("\\u")) >= 0) {
            char r = ((char) Integer.parseInt(s.substring(p + 2, p + 6), 8));
            if (r < ' ') r = ' ';
            s = s.substring(0, p) + r + s.substring(p + 6);
        }
        // remove tags
        s = s.replaceAll("</a>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&");
        // remove funny symbols
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < ' ') s = s.substring(0, i) + ' ' + s.substring(i + 1);
        }
        // remove double spaces
        s = s.replaceAll("  ", " ");
        return s;
    }
    
    public String getUserScreenName() {
        return this.user_screen_name;
    }
    
    public String getText() {
        return this.text;
    }

    public String[] getMentions() {
        return this.mentions;
    }

    public String[] getHashtags() {
        return this.hashtags;
    }

    public String[] getLinks() {
        return this.links;
    }

    public ArrayList<String> getImages() {
        return this.images;
    }
    
    public String getPlaceName() {
        return this.place_name;
    }
    
    public String getPlaceId() {
        return this.place_id;
    }

}
