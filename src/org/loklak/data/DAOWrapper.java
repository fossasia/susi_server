package org.loklak.data;

/**
 * Created by Deng Yiping on 16-7-31.
 */


import org.loklak.objects.MessageEntry;
import org.loklak.objects.QueryEntry;
import org.loklak.objects.SourceType;
import org.loklak.objects.UserEntry;

import java.net.MalformedURLException;
import java.util.*;

/**
 * The json below is the minimum json
 * {
 "statuses": [
 {
 "id_str": "yourmessageid_1234",
 "screen_name": "testuser",
 "created_at": "2016-07-22T07:53:24.000Z",
 "text": "The rain is spain stays always in the plain",
 "source_type": "GENERIC",
 "place_name": "Georgia, USA",
 "location_point": [3.058579854228782,50.63296878274201],
 "location_radius": 0,
 "user": {
 "user_id": "youruserid_5678",
 "name": "Mr. Bob",
 }
 }
 ]
 }
 */
public class DAOWrapper {
    public static final class GenericJSONBuilder{
        private String id_str = null;
        private String screen_name = "unknown";
        private Date created_at = null;
        private String text = "";
        private String place_name = "unknown";
        private String user_name = "unknown@unknown";
        private String user_id = "unknown";
        private String image = null;
        private double lng = 0.0;
        private double lat = 0.0;
        private int loc_radius = 0;
        private ArrayList<String> extras = new ArrayList<String>();


        /**
         * Not required
         * @param author
         * @param domain
         * @return
         */
        public GenericJSONBuilder setAuthor(String author, String domain){
            user_name = author + "@" + domain;
            screen_name = author;
            return this;
        }

        /**
         * Not required
         * @param user_id_
         * @return
         */
        public GenericJSONBuilder setUserid(String user_id_){
            user_id = user_id_;
            return this;
        }

        /**
         * Not required
         * @param id_str_
         * @return
         */
        public GenericJSONBuilder setIDstr(String id_str_){
            id_str = id_str_;
            return this;
        }

        /**
         * Not required
         * @param createdTime
         * @return
         */
        public GenericJSONBuilder setCreatedTime(Date createdTime){
            created_at = createdTime;
            return this;
        }

        /**
         * Required
         * This is the text field. You can use JSON style in this field
         * @param text_
         * @return
         */
        public GenericJSONBuilder addText(String text_){
            text = text + text_;
            return this;
        }

        /**
         * Not required
         * @param name
         * @return
         */
        public GenericJSONBuilder setPlaceName(String name){
            place_name = name;
            return this;
        }

        /**
         * Not required
         * @param longtitude
         * @param latitude
         * @return
         */
        public GenericJSONBuilder setCoordinate(double longtitude, double latitude){
            lng = longtitude;
            lat = latitude;
            return this;
        }

        /**
         * Not required
         * @param radius
         * @return
         */
        public GenericJSONBuilder setCoordinateRadius(int radius){
            loc_radius = radius;
            return this;
        }


        /**
         * Not required
         * @param key
         * @param value
         * @return
         */
        public GenericJSONBuilder addField(String key, String value){
            String pair_string = "\"" + key + "\": \"" + value + "\"";
            extras.add(pair_string);
            return this;
        }

        private String buildFieldJSON(){
            String extra_json = "";
            for(String e:extras){
                extra_json =  extra_json + e + ",";
            }
            if(extra_json.length() > 2) extra_json = "{" + extra_json.substring(0, extra_json.length() -1) + "}";
            return extra_json;
        }

        /**
         * Not required
         * @param link_
         * @return
         */
        public GenericJSONBuilder setImage(String link_){
            image = link_;
            return this;
        }

        public void persist(){
            try{
                //building message entry
                MessageEntry message = new MessageEntry();

                /**
                 * Use hash of text if id of message is not set
                 */
                if(id_str == null)
                    id_str = String.valueOf(text.hashCode());

                message.setIdStr(id_str);

                /**
                 * Get current time if not set
                 */
                if(created_at == null)
                    created_at = new Date();
                message.setCreatedAt(created_at);


                /**
                 * Append the field as JSON text
                 */
                message.setText(text + buildFieldJSON());

                double[] locPoint = new double[2];
                locPoint[0] = lng;
                locPoint[1] = lat;

                message.setLocationPoint(locPoint);

                message.setLocationRadius(loc_radius);

                message.setPlaceName(place_name, QueryEntry.PlaceContext.ABOUT);
                message.setSourceType(SourceType.GENERIC);

                /**
                 * Insert if there is a image field
                 */
                if(image != null) message.setImages(image);

                //building user
                UserEntry user = new UserEntry(user_id, screen_name, "", user_name);

                //build message and user wrapper
                DAO.MessageWrapper wrapper = new DAO.MessageWrapper(message,user, true);

                DAO.writeMessage(wrapper);
            } catch (MalformedURLException e){
            }
        }
    }

    public static GenericJSONBuilder builder(){
        return new GenericJSONBuilder();
    }

    public static void insert(Insertable msg){
        GenericJSONBuilder bd = builder()
        .setAuthor(msg.getUsername(), msg.getDomain())
        .addText(msg.getText())
        .setUserid(msg.getUserID());

        /**
         * Insert the fields
         */
        msg.getExtraField().forEach(entry -> bd.addField(entry.getKey(), entry.getValue()));
    }
}
