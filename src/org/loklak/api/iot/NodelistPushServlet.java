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
package org.loklak.api.iot;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.objects.SourceType;

import java.util.ArrayList;
import java.util.List;

public class NodelistPushServlet extends AbstractPushServlet {

    private static final long serialVersionUID = -7526015654376919340L;

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

    @Override
    protected JSONArray extractMessages(JSONObject data) {
        return data.getJSONArray("nodes");
    }

    @Override
    protected void customProcessing(JSONObject message) {
        JSONObject location = (JSONObject) message.get("position");

        final Double longitude = Double.parseDouble((String) location.get("long"));
        final Double latitude = Double.parseDouble((String) location.get("lat"));
        List<Double> location_point = new ArrayList<>();
        location_point.add(longitude);
        location_point.add(latitude);
        message.put("location_point", location_point);
        message.put("location_mark", location_point);

        JSONObject user = new JSONObject(true);
        user.put("screen_name", "freifunk_" + message.get("name"));
        user.put("name", message.get("name"));
        message.put("user", user);
    }
}
