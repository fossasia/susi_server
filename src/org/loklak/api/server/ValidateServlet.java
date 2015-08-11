/**
 * ValidateServlet
 * Copyright 05.08.2015 by Dang Hai An, @zyzo
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

package org.loklak.api.server;

import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.loklak.api.client.ClientConnection;
import org.loklak.data.DAO;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

public class ValidateServlet extends HttpServlet {

    enum ValidationStatus {
        offline,
        invalid,
        valid
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

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

        String source_type_str = post.get("source_type", "");
        if (source_type_str == null || source_type_str.length() == 0) {
            response.sendError(400, "your request does not contain a source_type parameter");
            return;
        }
        SourceType sourceType;
        try {
            sourceType = SourceType.valueOf(source_type_str.toUpperCase());
        } catch (IllegalArgumentException e) {
            response.sendError(400, "Invalid source_type parameter : " + source_type_str);
            return;
        }
        JsonValidator.JsonSchemaEnum jsonSchemaEnum;
        try {
            jsonSchemaEnum = JsonValidator.JsonSchemaEnum.valueOf(sourceType);
        } catch (IllegalArgumentException e) {
            response.sendError(400, "Current version of /api/validate.json doesn't support this source type: " + source_type_str);
            return;
        }
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;

        ValidationStatus status = null;
        String message = "";

        boolean offline = false;

        byte[] content = null;
        String contentToStr = null;
        try {
            content = ClientConnection.download(url);
        } catch (Exception e) {
            message = "Error reading json file from url";
            status = ValidationStatus.offline;
            offline = true;
        }

        if (content == null) {
            message = "Error reading json file from url";
            status = ValidationStatus.offline;
            offline = true;
        }

        if (!offline) {
            contentToStr = new String(content);
            JsonValidator validator = new JsonValidator(jsonSchemaEnum);
            ProcessingReport report;
            try {
                report = validator.validate(contentToStr);
            } catch(Exception e) {
                response.sendError(400, "The url does not contain valid json data");
                return;
            }
            if (!report.isSuccess()) {
                status = ValidationStatus.invalid;
                message = "json does not conform to schema : " + jsonSchemaEnum.name() + "\n" + report;
            } else {
                status = ValidationStatus.valid;
            }
        }

        post.setResponse(response, "application/javascript");

        // generate json
        XContentBuilder json = XContentFactory.jsonBuilder().prettyPrint().lfAtEnd();
        json.startObject();
        json.field("status", status.name());
        if (!(message.length() == 0))
            json.field("details", message);
        if (contentToStr != null)
            json.field("content", contentToStr);
        json.endObject();

        // write json
        ServletOutputStream sos = response.getOutputStream();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.string());
        if (jsonp) sos.println(");");
        sos.println();

        DAO.log("Validated url " + url + ". Result = " + status.name());
    }
}
