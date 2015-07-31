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

package org.loklak.api.server.push;

import org.loklak.data.DAO;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FossasiaPushServlet extends AbstractPushServlet {

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
    protected Map<String, Object> extractMessages(Map<String, Object> data) {
        // each fossasia api file contains only 1 message
        return data;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void customProcessing(Map<String, Object> message) {

        Map<String, Object> location = (Map<String, Object>) message.get("location");

        final Double longitude = (Double) location.get("lon");
        final Double latitude = (Double) location.get("lat");

        List<Double> location_point = new ArrayList<>();
        location_point.add(longitude);
        location_point.add(latitude);
        message.put("location_point", location_point);
        message.put("location_mark", location_point);

        try {
            message.put("id_str", PushServletHelper.computeMessageId(message, message.get("id"), getSourceType()));
        } catch (Exception e) {
            DAO.log("Problem computing id : " + e.getMessage());
        }
    }
}