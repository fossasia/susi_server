/**
 * FossasiaPushServlet
 * Copyright 09.04.2015 by Dang Hai An, @zyzo
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

public class FossasiaPushServlet extends AbstractPushServlet {

    private static final long serialVersionUID = 248613410547240115L;

    @Override
    protected SourceType getSourceType() {
        return SourceType.FOSSASIA_API;
    }

    @Override
    protected JsonValidator.JsonSchemaEnum getValidatorSchema() {
        return JsonValidator.JsonSchemaEnum.FOSSASIA;
    }

    @Override
    protected JsonFieldConverter.JsonConversionSchemaEnum getConversionSchema() {
        return JsonFieldConverter.JsonConversionSchemaEnum.FOSSASIA;
    }

    @Override
    protected JSONArray extractMessages(JSONObject data) {
        // each fossasia api file contains only 1 message
        JSONArray array = new JSONArray();
        array.put(data);
        return array;
    }

    @Override
    protected void customProcessing(JSONObject message) {

        JSONObject location = (JSONObject) message.get("location");

        final Double longitude = (Double) location.get("lon");
        final Double latitude = (Double) location.get("lat");

        JSONArray location_point = new JSONArray();
        location_point.put(longitude);
        location_point.put(latitude);
        message.put("location_point", location_point);
        message.put("location_mark", location_point);
    }
}