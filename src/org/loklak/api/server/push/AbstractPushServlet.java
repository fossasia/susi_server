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

import com.github.fge.jsonschema.core.report.ProcessingReport;

import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.geo.LocationSource;
import org.loklak.geo.PlaceContext;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class AbstractPushServlet extends HttpServlet {

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

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        RemoteAccess.Post post = RemoteAccess.evaluate(request);
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

        Map<String, Object> map;
        byte[] jsonText;
        try {
            jsonText = ClientConnection.download(url);
            map = DAO.jsonMapper.readValue(jsonText, DAO.jsonTypeRef);
        } catch (Exception e) {
            response.sendError(400, "error reading json file from url");
            return;
        }

        // validation phase
        ProcessingReport report = this.validator.validate(new String(jsonText));
        if (!report.isSuccess()) {
            response.sendError(400, "json does not conform to schema : " + this.getValidatorSchema().name() + "\n" + report);
            return;
        }

        // conversion phase
        Object extractResults = extractMessages(map);
        List<Map<String, Object>> typedMessages = null;
        if (extractResults instanceof List) {
            typedMessages = (List<Map<String, Object>>) extractResults;
        } else if (extractResults instanceof Map) {
            typedMessages = new ArrayList<>();
            typedMessages.add((Map<String, Object>) extractResults);
        } else {
            throw new IOException("extractMessages must return either a List or a Map");
        }
        typedMessages = this.converter.convert(typedMessages);

        // custom treatment for each message
        for (Map<String, Object> message : typedMessages) {
            message.put("source_type", this.getSourceType().name());
            message.put("location_source", LocationSource.USER.name());
            message.put("place_context", PlaceContext.ABOUT.name());
            if (message.get("text") == null) {
                message.put("text", "");
            }
            customProcessing(message);
        }
        PushReport nodePushReport;
        try {
            nodePushReport = PushServletHelper.saveMessagesAndImportProfile(typedMessages, Arrays.hashCode(jsonText), post, getSourceType());
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
    protected abstract Object extractMessages(Map<String, Object> data);

    protected abstract void customProcessing(Map<String, Object> message);
}
