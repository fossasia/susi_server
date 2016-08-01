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

package org.loklak.api.iot;

import com.github.fge.jsonschema.core.report.ProcessingReport;

import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.harvester.JsonValidator;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.objects.SourceType;
import org.loklak.server.Query;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

public class ValidateServlet extends HttpServlet {

    private static final long serialVersionUID = -7325042684311478289L;

    enum ValidationStatus {
        offline,
        invalid,
        unsupported,
        valid
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
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

        String source_type_str = post.get("source_type", "");
        if (source_type_str == null || source_type_str.length() == 0) {
            response.sendError(400, "your request does not contain a source_type parameter");
            return;
        }
        SourceType sourceType = null;
        // treat geojson as a special source_type
        if (!source_type_str.toUpperCase().equals("GEOJSON")) {
            try {
                sourceType = SourceType.byName(source_type_str.toUpperCase());
            } catch (IllegalArgumentException e) {
                response.sendError(400, "Invalid source_type parameter : " + source_type_str);
                return;
            }
        }

        ValidationStatus status = null;
        String message = "";

        boolean unsupported = false;
        JsonValidator.JsonSchemaEnum jsonSchemaEnum = null;
        try {
            jsonSchemaEnum = JsonValidator.JsonSchemaEnum.valueOf(sourceType);
        } catch (IllegalArgumentException e) {
            DAO.log("Current version of /api/validate.json doesn't support this source type: " + source_type_str);
            unsupported = true;
            status = ValidationStatus.unsupported;
            message = "Current version of /api/validate.json doesn't support this source type: " + source_type_str;
        }
        String callback = post.get("callback", "");
        boolean jsonp = callback != null && callback.length() > 0;


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
            if (!unsupported) {
                JsonValidator validator = new JsonValidator(jsonSchemaEnum);
                ProcessingReport report;
                try {
                    report = validator.validate(contentToStr);
                } catch (Exception e) {
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
        }

        post.setResponse(response, "application/javascript");

        // generate json
        JSONObject json = new JSONObject(true);
        json.put("status", status.name());
        if (!(message.length() == 0)) json.put("details", message);
        if (contentToStr != null) json.put("content", contentToStr);

        // write json
        response.setCharacterEncoding("UTF-8");
        PrintWriter sos = response.getWriter();
        if (jsonp) sos.print(callback + "(");
        sos.print(json.toString(2));
        if (jsonp) sos.println(");");
        sos.println();

        DAO.log("Validated url " + url + ". Result = " + status.name());
        post.finalize();
    }
}
