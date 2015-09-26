/**
 *  TwitterScraper
 *  Copyright 22.02.2015 by Michael Peter Christen, @0rb1t3r
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.loklak.api.client.ClientConnection;
import org.loklak.data.DAO;
import org.loklak.data.ProviderType;
import org.loklak.data.Timeline;
import org.loklak.data.MessageEntry;
import org.loklak.data.UserEntry;
import org.loklak.tools.UTF8;

public class TwitterScraper {

    public static ExecutorService executor = Executors.newFixedThreadPool(20);
    
    public static Timeline search(final String query, final Timeline.Order order) {
        // check
        // https://twitter.com/search-advanced for a better syntax
        // https://support.twitter.com/articles/71577-how-to-use-advanced-twitter-search#
        String https_url = "";
        try {
            StringBuilder t = new StringBuilder(query.length());
            for (String s: query.replace('+', ' ').split(" ")) {
                t.append(' ');
                if (s.startsWith("since:") || s.startsWith("until:")) {
                    int u = s.indexOf('_');
                    t.append(u < 0 ? s : s.substring(0, u));
                } else {
                    t.append(s);
                }
            }
            String q = t.length() == 0 ? "*" : URLEncoder.encode(t.substring(1), "UTF-8");
            //https://twitter.com/search?q=from:yacy_search&src=typd
            https_url = "https://twitter.com/search?q=" + q + "&src=typd&vertical=default&f=tweets";
        } catch (UnsupportedEncodingException e) {}
        Timeline timeline = null;
        try {
            ClientConnection connection = new ClientConnection(https_url);
            if (connection.inputStream == null) return null;
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.inputStream, UTF8.charset));
                timeline = search(br, order);
            } catch (IOException e) {
               e.printStackTrace();
            } finally {
                connection.close();
            }
        } catch (IOException e) {
            // this could mean that twitter rejected the connection (DoS protection?)
            e.printStackTrace();
            if (timeline == null) timeline = new Timeline(order);
        };

        // wait until all messages in the timeline are ready
        if (timeline == null) {
            // timeout occurred
            timeline = new Timeline(order);
        }
        return timeline;
    }
    
    private static Timeline search(final BufferedReader br, final Timeline.Order order) throws IOException {
        Timeline timeline = new Timeline(order);
        String input;
        Map<String, prop> props = new HashMap<String, prop>();
        Set<String> images = new LinkedHashSet<>();
        Set<String> videos = new LinkedHashSet<>();
        String place_id = "", place_name = "";
        boolean parsing_favourite = false, parsing_retweet = false;
        while ((input = br.readLine()) != null){
            input = input.trim();
            //System.out.println(input); // uncomment temporary to debug or add new fields
            int p;
            if ((p = input.indexOf("class=\"account-group")) > 0) {
                props.put("userid", new prop(input, p, "data-user-id"));
                continue;
            }
            if ((p = input.indexOf("class=\"avatar")) > 0) {
                props.put("useravatarurl", new prop(input, p, "src"));
                continue;
            }
            if ((p = input.indexOf("class=\"fullname")) > 0) {
                props.put("userfullname", new prop(input, p, null));
                continue;
            }
            if ((p = input.indexOf("class=\"username")) > 0) {
                props.put("usernickname", new prop(input, p, null));
                continue;
            }
            if ((p = input.indexOf("class=\"tweet-timestamp")) > 0) {
                props.put("tweetstatusurl", new prop(input, 0, "href"));
                props.put("tweettimename", new prop(input, p, "title"));
                // don't continue here because "class=\"_timestamp" is in the same line 
            }
            if ((p = input.indexOf("class=\"_timestamp")) > 0) {
                props.put("tweettimems", new prop(input, p, "data-time-ms"));
                continue;
            }
            if ((p = input.indexOf("class=\"ProfileTweet-action--retweet")) > 0) {
                parsing_retweet = true;
                continue;
            }
            if ((p = input.indexOf("class=\"ProfileTweet-action--favorite")) > 0) {
                parsing_favourite = true;
                continue;
            }
            if ((p = input.indexOf("class=\"TweetTextSize")) > 0) {
                props.put("tweettext", new prop(input, p, null));
                continue;
            }
            if ((p = input.indexOf("class=\"ProfileTweet-actionCount")) > 0) {
                if (parsing_retweet) {
                    props.put("tweetretweetcount", new prop(input, p, "data-tweet-stat-count"));
                    parsing_retweet = false;
                }
                if (parsing_favourite) {
                    props.put("tweetfavouritecount", new prop(input, p, "data-tweet-stat-count"));
                    parsing_favourite = false;
                }
                continue;
            }
            // get images
            if ((p = input.indexOf("class=\"media media-thumbnail twitter-timeline-link media-forward is-preview")) > 0 ||
                (p = input.indexOf("class=\"multi-photo")) > 0) {
                images.add(new prop(input, p, "data-resolved-url-large").value);
                continue;
            }
            // we have two opportunities to get video thumbnails == more images; images in the presence of video content should be treated as thumbnail for the video
            if ((p = input.indexOf("class=\"animated-gif-thumbnail\"")) > 0) {
                images.add(new prop(input, 0, "src").value);
                continue;
            }
            if ((p = input.indexOf("class=\"animated-gif\"")) > 0) {
                images.add(new prop(input, p, "poster").value);
                continue;
            }
            if ((p = input.indexOf("<source video-src")) >= 0 && input.indexOf("type=\"video/") > p) {
                videos.add(new prop(input, p, "video-src").value);
                continue;
            }
            if ((p = input.indexOf("class=\"Tweet-geo")) > 0) {
                prop place_name_prop = new prop(input, p, "title");
                place_name = place_name_prop.value;
                continue;
            }
            if ((p = input.indexOf("class=\"ProfileTweet-actionButton u-linkClean js-nav js-geo-pivot-link")) > 0) {
                prop place_id_prop = new prop(input, p, "data-place-id");
                place_id = place_id_prop.value;
                continue;
            }
            if (props.size() == 10) {
                // the tweet is complete, evaluate the result
                UserEntry user = new UserEntry(
                        props.get("userid").value,
                        props.get("usernickname").value,
                        props.get("useravatarurl").value,
                        MessageEntry.html2utf8(props.get("userfullname").value)
                        );
                ArrayList<String> imgs = new ArrayList<String>(images.size()); imgs.addAll(images);
                ArrayList<String> vids = new ArrayList<String>(videos.size()); vids.addAll(videos);
                TwitterTweet tweet = new TwitterTweet(
                        user.getScreenName(),
                        Long.parseLong(props.get("tweettimems").value),
                        props.get("tweettimename").value,
                        props.get("tweetstatusurl").value,
                        props.get("tweettext").value,
                        Long.parseLong(props.get("tweetretweetcount").value),
                        Long.parseLong(props.get("tweetfavouritecount").value),
                        imgs, vids, place_name, place_id
                        );
                //new Thread(tweet).start(); // todo: use thread pools
                //tweet.run(); // for debugging
                executor.execute(tweet);
                timeline.add(tweet, user);
                images.clear();
                props.clear();
                continue;
            }
        }
        //for (prop p: props.values()) System.out.println(p);
        br.close();
        return timeline;
    }
    
    private static class prop {
        public String key, value = null;
        public prop(String line, int start, String key) {
            this.key = key;
            if (key == null) {
                int p = line.indexOf('>', start);
                if (p > 0) {
                    int c = 1;
                    int q = p + 1;
                    while (c > 0 && q < line.length()) {
                        char a = line.charAt(q);
                        if (a == '<') {
                            if (line.charAt(q+1) != 'i') {
                                if (line.charAt(q+1) == '/') c--; else c++;
                            }
                        }
                        q++;
                    }
                    value = line.substring(p + 1, q - 1);
                }
            } else {
                int p  = line.indexOf(key + "=\"", start);
                if (p > 0) {
                    int q = line.indexOf('"', p + key.length() + 2);
                    if (q > 0) {
                        value = line.substring(p + key.length() + 2, q);
                    }
                }
            }
        }
        
        @SuppressWarnings("unused")
        public boolean success() {
            return value != null;
        }
        
        public String toString() {
            return this.key + "=" + (this.value == null ? "unknown" : this.value);
        }
    }
    

    final static Pattern timeline_link_pattern = Pattern.compile("<a .*?href=\"(.*?)\".*?data-expanded-url=\"(.*?)\".*?twitter-timeline-link.*title=\"(.*?)\".*?>.*?</a>");
    final static Pattern timeline_embed_pattern = Pattern.compile("<a .*?href=\"(.*?)\".*?twitter-timeline-link.*?>pic.twitter.com/(.*?)</a>");
    final static Pattern emoji_pattern = Pattern.compile("<img .*?class=\"twitter-emoji\".*?alt=\"(.*?)\".*?>");
    
    
    public static class TwitterTweet extends MessageEntry implements Runnable {

        private Semaphore ready = null;
        private Boolean exists = null;
        
        public TwitterTweet(
                final String user_screen_name_raw,
                final long created_at_raw,
                final String created_at_name_raw, // not used here but should be compared to created_at_raw
                final String status_id_url_raw,
                final String text_raw,
                final long retweets,
                final long favourites,
                final Collection<String> images,
                final Collection<String> videos,
                final String place_name,
                final String place_id) throws MalformedURLException {
            super();
            this.source_type = SourceType.TWITTER;
            this.provider_type = ProviderType.SCRAPED;
            this.screen_name = user_screen_name_raw;
            this.created_at = new Date(created_at_raw);
            this.status_id_url = new URL("https://twitter.com" + status_id_url_raw);
            int p = status_id_url_raw.lastIndexOf('/');
            this.id_str = p >= 0 ? status_id_url_raw.substring(p + 1) : "-1";
            this.retweet_count = retweets;
            this.favourites_count = favourites;
            this.place_name = place_name;
            this.place_id = place_id;
            this.images = new LinkedHashSet<>(); for (String image: images) this.images.add(image);
            this.videos = new LinkedHashSet<>(); for (String video: videos) this.videos.add(video);

            //Date d = new Date(timemsraw);
            //System.out.println(d);
            
            /* failed to reverse-engineering the place_id :(
            if (place_id.length() == 16) {
                String a = place_id.substring(0, 8);
                String b = place_id.substring(8, 16);
                long an = Long.parseLong(a, 16);
                long bn = Long.parseLong(b, 16);
                System.out.println("place = " + place_name + ", a = " + an + ", b = " + bn);
                // Frankfurt a = 3314145750, b = 3979907708, http://www.openstreetmap.org/#map=15/50.1128/8.6835
                // Singapore a = 1487192992, b = 3578663936
            }
            */
            
            this.text = text_raw; // this MUST be analysed with analyse(); this is not done here because it should be started concurrently; run run();
        }

        private void analyse() {
            this.text = this.text.replaceAll("</?(s|b|strong)>", "").replaceAll("<a href=\"/hashtag.*?>", "").replaceAll("<a.*?class=\"twitter-atreply.*?>", "").replaceAll("<span.*?span>", "").replaceAll("  ", " ");
            while (true) {
                try {
                    Matcher m = timeline_link_pattern.matcher(this.text);
                    if (m.find()) {
                        //String href = m.group(1);
                        String expanded = RedirectUnshortener.unShorten(m.group(2));
                        //String title = m.group(3);
                        this.text = m.replaceFirst(expanded);
                        continue;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    break;
                }
                try {
                    Matcher m = timeline_embed_pattern.matcher(this.text);
                    if (m.find()) {
                        //String href = resolveShortURL(m.group(1));
                        String shorturl = RedirectUnshortener.unShorten(m.group(2));
                        this.text = m.replaceFirst("https://pic.twitter.com/" + shorturl + " ");
                        continue;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    break;
                }
                try {
                    Matcher m = emoji_pattern.matcher(this.text);
                    if (m.find()) {
                        String emoji = m.group(1);
                        this.text = m.replaceFirst(emoji);
                        continue;
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    break;
                }
                break;
            }
            this.text = html2utf8(this.text).replaceAll("  ", " ").trim();
        }
        
        @Override
        public void run() {
            this.ready = new Semaphore(0);
            try {
                this.exists = new Boolean(DAO.existMessage(this.getIdStr()));
                // only analyse and enrich the message if it does not actually exist in the search index because it will be abandoned otherwise anyway
                //if (!this.exists) {
                    this.analyse();
                    this.enrich();
                //}
            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                this.ready.release(1000);
            }
        }

        public boolean isReady() {
            return this.ready == null || this.ready.availablePermits() > 0;
        }
        
        public void waitReady() {
            if (this.ready != null) try {
                this.ready.acquire();
            } catch (InterruptedException e) {}
        }
        
        /**
         * the exist method has a 3-value boolean logic: false, true and NULL for: don't know
         * @return
         */
        public Boolean exist() {
            return this.exists;
        }
        
    }
    
    /**
     * Usage: java twitter4j.examples.search.SearchTweets [query]
     *
     * @param args search query
     */
    public static void main(String[] args) {
        //wget --no-check-certificate "https://twitter.com/search?q=eifel&src=typd&f=realtime"
        Timeline result = TwitterScraper.search(args[0], Timeline.Order.CREATED_AT);
        for (MessageEntry tweet : result) {
            if (tweet instanceof TwitterTweet) {
                ((TwitterTweet) tweet).waitReady();
            }
            System.out.println("@" + tweet.getScreenName() + " - " + tweet.getText());
        }
        System.exit(0);
    }
}


