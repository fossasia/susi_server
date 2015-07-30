/**
 * NodelistPushServlet
 * Copyright 16.07.2015 by Dang Hai An, @zyzo
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

import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.loklak.api.client.ClientConnection;
import org.loklak.api.server.RemoteAccess;
import org.loklak.data.DAO;
import org.loklak.geo.LocationSource;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodelistPushServlet extends AbstractPushServlet {

    @Override
    protected SourceType getSourceType() {
        return SourceType.NODELIST;
    }

    @Override
    protected JsonValidator.JsonSchemaEnum getValidatorSchema() {
        return JsonValidator.JsonSchemaEnum.NODELIST;
    }

    @Override
    protected JsonFieldConverter.JsonConversionSchemaEnum getConversionSchema() {
        return JsonFieldConverter.JsonConversionSchemaEnum.NODELIST_NODE;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Map<String, Object>> extractMessages(Map<String, Object> data) {
        return (List<Map<String, Object>>) data.get("nodes");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void customProcessing(Map<String, Object> message) {
        Map<String, Object> location = (Map<String, Object>) message.get("position");
        if (location == null) return;

        final Double longitude = Double.parseDouble((String) location.get("long"));
        final Double latitude = Double.parseDouble((String) location.get("lat"));
        List<Double> location_point = new ArrayList<>();
        location_point.add(longitude);
        location_point.add(latitude);
        message.put("location_point", location_point);
        message.put("location_mark", location_point);

        Map<String, Object> user = new HashMap<>();
        user.put("screen_name", "freifunk_" + message.get("name"));
        user.put("name", message.get("name"));
        message.put("user", user);
        try {
            message.put("id_str", PushServletHelper.computeMessageId(message, message.get("id"), getSourceType()));
        } catch (Exception e) {
            DAO.log("Problem computing id : " + e.getMessage());
        }
    }
}
