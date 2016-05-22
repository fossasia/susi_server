/**
 * AbstractPushServlet
 * Copyright 27.07.2015 by Dang Hai An, @zyzo
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program in the file lgpl21.txt
 * If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.api.server.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.fge.jsonschema.core.report.ProcessingReport;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.geo.LocationSource;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.MessageEntry;
import org.loklak.objects.QueryEntry.PlaceContext;
import org.loklak.server.Query;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class AbstractPushServlet extends HttpServlet {

    private static final long serialVersionUID = 8849199146929807638L;
    private JsonValidator validator;
    private JsonFieldConverter converter;

    @Override
    public void init() throws ServletException {
        try {
            validator = new JsonValidator(this.getValidatorSchema());
        } catch (IOException e) {
            DAO.log("Unable to initialize push servlet validator : " + e.getMessage());
            e.printStackTrace();
        }
        try {
            converter = new JsonFieldConverter(this.getConversionSchema());
        } catch (IOException e) {
            DAO.log("Unable to initialize push servlet field converter : " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        String remoteHash = Integer.toHexString(Math.abs(post.getClientHost().hashCode()));

        // manage DoS
        if (post.isDoS_blackout()) {
            response.sendError(503, "your request frequency is too high");
            return;
        }

        String url = post.get("url", "");
        if (url == null || url.length() == 0) {
            response.sendError(400, "your request does not contain an url to your data object");
            return;
        }
        String screen_name = post.get("screen_name", "");
        if (screen_name == null || screen_name.length() == 0) {
            response.sendError(400, "your request does not contain required screen_name parameter");
            return;
        }

        JSONObject map;
        String jsonString = "";
        try {
            jsonString = new String(ClientConnection.download(url), StandardCharsets.UTF_8);
            map = new JSONObject(jsonString);
        } catch (Exception e) {
            response.sendError(400, "error reading json file from url");
            return;
        }

        // validation phase
        ProcessingReport report = this.validator.validate(jsonString);
        if (!report.isSuccess()) {
            response.sendError(400, "json does not conform to schema : " + this.getValidatorSchema().name() + "\n" + report);
            return;
        }

        // conversion phase
        Object extractResults = extractMessages(map);
        JSONArray messages;
        if (extractResults instanceof JSONArray) {
            messages = (JSONArray) extractResults;
        } else if (extractResults instanceof JSONObject) {
            messages = new JSONArray();
            messages.put((JSONObject) extractResults);
        } else {
            throw new IOException("extractMessages must return either a List or a Map. Get " + (extractResults == null ? "null" : extractResults.getClass().getCanonicalName()) + " instead");
        }
        JSONArray convertedMessages = this.converter.convert(messages);

        PushReport nodePushReport = new PushReport();
        ObjectWriter ow = new ObjectMapper().writerWithDefaultPrettyPrinter();
        // custom treatment for each message
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = (JSONObject) convertedMessages.get(i);
            message.put("source_type", this.getSourceType().name());
            message.put("location_source", LocationSource.USER.name());
            message.put("place_context", PlaceContext.ABOUT.name());
            if (message.get("text") == null) {
                message.put("text", "");
            }

            // append rich-text attachment
            String jsonToText = ow.writeValueAsString(messages.get(i));
            message.put("text", message.get("text") + MessageEntry.RICH_TEXT_SEPARATOR + jsonToText);
            customProcessing(message);

            if (message.get("mtime") == null) {
                String existed = PushServletHelper.checkMessageExistence(message);
                // message known
                if (existed != null) {
                    messages.remove(i);
                    nodePushReport.incrementKnownCount(existed);
                    continue;
                }
                // updated message -> save with new mtime value
                message.put("mtime", Long.toString(System.currentTimeMillis()));
            }

            try {
                message.put("id_str", PushServletHelper.computeMessageId(message, getSourceType()));
            } catch (Exception e) {
                DAO.log("Problem computing id : " + e.getMessage());
            }
        }
        try {
            PushReport savingReport = PushServletHelper.saveMessagesAndImportProfile(convertedMessages, jsonString.hashCode(), post, getSourceType(), screen_name);
            nodePushReport.combine(savingReport);
        } catch (IOException e) {
            response.sendError(404, e.getMessage());
            return;
        }
        String res = PushServletHelper.buildJSONResponse(post.get("callback", ""), nodePushReport);

        post.setResponse(response, "application/javascript");
        response.getOutputStream().println(res);
        DAO.log(request.getServletPath()
                + " -> records = " + nodePushReport.getRecordCount()
                + ", new = " + nodePushReport.getNewCount()
                + ", known = " + nodePushReport.getKnownCount()
                + ", error = " + nodePushReport.getErrorCount()
                + ", from host hash " + remoteHash);
    }

    protected abstract SourceType getSourceType();

    protected abstract JsonValidator.JsonSchemaEnum getValidatorSchema();

    protected abstract JsonFieldConverter.JsonConversionSchemaEnum getConversionSchema();

    // return either a list or a map of <String,Object>
    protected abstract JSONArray extractMessages(JSONObject data);

    protected abstract void customProcessing(JSONObject message);
}
