/**
 *  YoutubeScraper
 *  Copyright 22.03.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.harvester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.loklak.tools.CharacterCoding;

public class YoutubeScraper {

    public final static ExecutorService executor = Executors.newFixedThreadPool(40);

    private final static String[] html_tags = new String[]{"title"};
    private final static String[] microformat_vocabularies = new String[]{"og", "twitter"};

    public static JSONObject parseVideo(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        JSONObject json = parseVideo(fis);
        fis.close();
        return json;
    }
    
    public static JSONObject parseVideo(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        JSONObject json = parseVideo(reader);
        reader.close();
        return json;
    }
    
    public static JSONObject parseVideo(final BufferedReader br) throws IOException {
        String input;
        JSONObject json = new JSONObject(true);
        boolean parse_span = false, parse_license = false;
        String itemprop= "", itemtype = ""; // values for span
        while ((input = br.readLine()) != null) try {
            input = input.trim();
            //System.out.println(input); // uncomment temporary to debug or add new fields
            int p;

            if (parse_license) {
                if ((p = input.indexOf("<li")) >= 0) {
                    String tag = parseTag(input, p);
                    if (tag == null) continue;
                    if (tag.startsWith("<a ")) {
                        tag = parseTag(tag, 0);
                        addRDF(new String[]{"youtube", "category", tag}, json);
                    } else {
                        addRDF(new String[]{"youtube", "license", tag}, json);
                    }
                    parse_license = false;
                    continue;
                }
            } else if (parse_span) {
                if ((p = input.indexOf("itemprop=\"")) >= 0) {
                    String[] token = parseItemprop(input, p, new String[]{"href", "content"}, "");
                    if (token == null) continue;
                    int q = itemtype.indexOf("//"); if (q < 0) continue;
                    String subject = itemtype.substring(q + 2).replace('.', '_').replace('/', '_');
                    String predicate = itemprop + "_" + token[1];
                    String object = token[2];
                    addRDF(new String[]{subject, predicate, object}, json);
                    continue;
                }
                if (input.indexOf("</span>") >= 0) {
                    parse_span = false;
                    continue;
                }
            } else {
                tags: for (String tag: html_tags) {
                    if ((p = input.indexOf("<" + tag)) >= 0) {
                        addRDF(new String[]{"html", tag, parseTag(input, p)}, json);
                        continue tags;
                    }
                }
                vocs: for (String subject: microformat_vocabularies) {
                    if ((p = input.indexOf("property=\"" + subject + ":")) >= 0) {
                        addRDF(parseMicroformat(input, "property", p), json);
                        continue vocs;
                    }
                    if ((p = input.indexOf("name=\"" + subject + ":")) >= 0) {
                        addRDF(parseMicroformat(input, "name", p), json);
                        continue vocs;
                    }
                }
                if ((p = input.indexOf("span itemprop=\"")) >= 0) {
                    String[] token = parseItemprop(input, p, new String[]{"itemtype"}, "");
                    if (token == null) continue;
                    itemprop = token[1];
                    itemtype = token[2];
                    parse_span = true;
                    continue;
                }
                if ((p = input.indexOf("itemprop=\"")) >= 0) {
                    String[] token = parseItemprop(input, p, new String[]{"content"}, "youtube");
                    if (token == null) continue;
                    addRDF(token, json);
                    continue;
                }
                if ((p = input.indexOf("class=\"content watch-info-tag-list")) >= 0) {
                    parse_license = true;
                    continue;
                }
                if ((p = input.indexOf("yt-subscriber-count")) >= 0) {
                    String subscriber_string = parseProp(input, p, "title");
                    if (subscriber_string == null) continue;
                    json.put("youtube_subscriber", parseNumber(subscriber_string));
                    continue;
                }
                if (input.indexOf("\"like this") > 0 && (p = input.indexOf("yt-uix-button-content")) >= 0) {
                    String likes_string = parseTag(input, p);
                    json.put("youtube_likes", parseNumber(likes_string));
                    continue;
                }
                if (input.indexOf("\"dislike this") > 0 && (p = input.indexOf("yt-uix-button-content")) >= 0) {
                    String dislikes_string = parseTag(input, p);
                    json.put("youtube_dislikes", parseNumber(dislikes_string));
                    continue;
                }
                if ((p = input.indexOf("watch-view-count")) >= 0) {
                    String viewcount_string = parseTag(input, p);
                    if (viewcount_string == null) continue;
                    viewcount_string = viewcount_string.replace(" views", "");
                    if (viewcount_string.length() == 0) continue;
                    long viewcount = 0;
                    // if there are no views, there may be a string saying "No". But this is done in all languages, so we just catch a NumberFormatException
                    try {viewcount = parseNumber(viewcount_string);} catch (NumberFormatException e) {}
                    json.put("youtube_viewcount", viewcount);
                    continue;
                }
                if ((p = input.indexOf("watch?v=")) >= 0) {
                    p += 8;
                    int q = input.indexOf("\"", p);
                    if (q > 0) {
                        String videoid = input.substring(p, q);
                        int r = videoid.indexOf('&');
                        if (r > 0) videoid = videoid.substring(0, r);
                        addRDF(new String[]{"youtube", "next", videoid}, json);
                        continue;
                    }
                }
                if ((p = input.indexOf("playlist-header-content")) >= 0) {
                    String playlist_title = parseProp(input, p, "data-list-title");
                    if (playlist_title == null) continue;
                    addRDF(new String[]{"youtube", "playlist_title", playlist_title}, json);
                    continue;
                }
                if ((p = input.indexOf("yt-uix-scroller-scroll-unit")) >= 0) {
                    String playlist_videoid = parseProp(input, p, "data-video-id");
                    if (playlist_videoid == null) continue;
                    addRDF(new String[]{"youtube", "playlist_videoid", playlist_videoid}, json);
                    continue;
                }
                if ((p = input.indexOf("watch-description-text")) >= 0) {
                    p = input.indexOf('>', p);
                    int q = input.indexOf("</div", p);
                    String text = input.substring(p + 1, q < 0 ? input.length() : q);
                    text = paragraph.matcher(brend.matcher(text).replaceAll("\n")).replaceAll("").trim();
                    Matcher m;
                    anchor_loop: while ((m = anchor_pattern.matcher(text)).find()) try {
                        text = m.replaceFirst(m.group(1) + " ");
                    } catch (IllegalArgumentException  e) {text = ""; break anchor_loop;}
                    text = CharacterCoding.html2unicode(text);
                    json.put("youtube_description", text);
                    continue;
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("error in video " + json.toString(2));
            System.err.println("current line: " + input);
            System.exit(0);
        }
        br.close();
        return json;
    }
    
    private static long parseNumber(String n) throws NumberFormatException {
        return Long.parseLong(numberfix.matcher(n).replaceAll(""));
    }
    
    private final static Pattern numberfix = Pattern.compile(",|\\.");
    private final static Pattern paragraph = Pattern.compile("<p.*>|</p.*>");
    private final static Pattern brend = Pattern.compile("<br />");
    private final static Pattern anchor_pattern = Pattern.compile("<a .*?>(.*?)</a>");
    
    private static String[] parseMicroformat(String line, String key, int start) {
        int p  = line.indexOf(key + "=\"", start); if (p < 0) return null; p += key.length() + 2;
        int c  = line.indexOf(":", p); if (c < 0) return null;
        int q  = line.indexOf("\"", c); if (q < 0) return null;
        int r  = line.indexOf("content=\"", q); if (r < 0) return null; r += 9;
        int s  = line.indexOf("\"", r); if (s < 0) return null;
        // this is a rdf statement
        String subject = line.substring(p, c).replace(':', '_');
        String predicate = line.substring(c + 1, q).replace(':', '_');
        String object = line.substring(r, s);
        return new String[]{subject, predicate, object};
    }

    private static String[] parseItemprop(String line, int start, String[] objectnames, String subject) {
        int p  = line.indexOf("itemprop=\"", start); if (p < 0) return null; p += 10;
        int q  = line.indexOf("\"", p); if (q < 0) return null;
        int r = -1;
        objectscan: for (String objectname: objectnames) {
            r  = line.indexOf(objectname + "=\"", q);
            if (r < 0) continue objectscan;
            r += objectname.length() + 2;
            break;
        }
        if (r < 0) return null;
        int s  = line.indexOf("\"", r); if (s < 0) return null;
        // this becomes a rdf statement
        String predicate = line.substring(p, q).replace(':', '_');
        String object = line.substring(r, s);
        return new String[]{subject, predicate, object};
    }
    
    private static void addRDF(String[] spo, JSONObject json) {
        if (spo == null) return;
        String subject = spo[0];
        String predicate = spo[1];
        String object = CharacterCoding.html2unicode(spo[2]);
        if (subject.length() == 0 || predicate.length() == 0 || object.length() == 0) return;
        String key = subject + "_" + predicate;
        JSONArray objects = null;
        try {
            objects = json.getJSONArray(key);
        } catch (JSONException e) {
            objects = new JSONArray();
            json.put(key, objects);
        }
        // double-check (wtf why is ths that complex?)
        for (Object o: objects) {
            if (o instanceof String && ((String) o).equals(object)) return;
        }
        // add the object to the objects
        objects.put(object);
    }
    
    private static String parseProp(String line, int start, String key) {
        int p  = line.indexOf(key + "=\"", start);
        if (p > 0) {
            int q = line.indexOf('"', p + key.length() + 2);
            if (q > 0) {
                return line.substring(p + key.length() + 2, q);
            }
        }
        return null;
    }
    
    private static String parseTag(String line, int start) {
        int p = line.indexOf('>', start);
        if (p < 0) return null;
        int c = 1; // we count the number of open tags and stop if the number is zero. We already passed the first tag which is c = 1
        int q = p + 1; // start scan at the next position
        while (c > 0 && q < line.length() - 1) {
            char a = line.charAt(q);
            if (a == '<') {
                if (line.charAt(q + 1) != 'i') {
                    if (line.charAt(q + 1) == '/') c--; else c++;
                }
            }
            q++;
        }
        if (c != 0) return "";
        return line.substring(p + 1, q - 1).trim();
    }
    
}
