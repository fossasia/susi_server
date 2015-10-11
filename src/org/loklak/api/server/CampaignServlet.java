/**
 *  CampaignServlet
 *  Copyright 15.04.2015 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.api.server;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.data.Campaign;
import org.loklak.tools.DateParser;

public class CampaignServlet extends HttpServlet {
   
    private static final long serialVersionUID = 8578478303032749879L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
        
        boolean localhost = post.isLocalhostAccess();
        if (!localhost) {response.sendError(400, "campaigns can only be started by calls from localhost"); return;}
        
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;
        Calendar start_date, end_date;
        
        // get campaign definition
        String query =  post.get("query", "");
        if (query.length() == 0) {response.sendError(400, "you must submit a property 'query' with a search query to identify messages for the campaign"); return;}
        
        String name =  post.get("name", "");
        if (name.length() == 0) {response.sendError(400, "you must submit a property 'name' with a description string for the campaign"); return;}
        
        int timezoneOffset = post.get("timezoneOffset", -999);
        if (timezoneOffset == -999) {response.sendError(400, "you must submit a property 'timezoneOffset' with the offset of your time zone in minutes"); return;}
        
        String start_date_s =  post.get("start_date", "");
        if (start_date_s.length() == 0) {response.sendError(400, "you must submit a property 'start_date' in format 'yyyy-MM-dd_HH:mm'"); return;}
        try {
            start_date = DateParser.parse(start_date_s, timezoneOffset);
        } catch (ParseException e) {
            response.sendError(400, "the start_date '" + start_date_s + "' is not in format 'yyyy-MM-dd_HH:mm'"); return;
        }
        String end_date_s =  post.get("end_date", "");
        if (end_date_s.length() == 0) {response.sendError(400, " you must submit a property 'end_date' in format 'yyyy-MM-dd_HH:mm'"); return;}
        try {
            end_date = DateParser.parse(end_date_s, timezoneOffset);
        } catch (ParseException e) {
            response.sendError(400, "the end_date '" + end_date_s + "' is not in format 'yyyy-MM-dd_HH:mm'"); return;
        }
        
        final Date creation_date = new Date();
        
        Campaign campaign = new Campaign(
                query, name, creation_date,
                start_date.getTime(), end_date.getTime(), 
                timezoneOffset);
        
        post.setResponse(response, "application/javascript");
        
        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("campaign");
        json.startObject();
        campaign.toJSON(json);
        json.endObject();
        json.endObject();

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();
        post.finalize();
    }
    
}
