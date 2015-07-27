/**
 * FreifunkNodePushServlet
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

import org.loklak.data.DAO;
import org.loklak.harvester.JsonFieldConverter;
import org.loklak.harvester.JsonValidator;
import org.loklak.harvester.SourceType;

import java.util.List;
import java.util.Map;

public class FreifunkNodePushServlet extends AbstractPushServlet {

    @Override
    protected SourceType getSourceType() {
        return SourceType.FREIFUNK_NODE;
    }

    @Override
    protected JsonValidator.JsonSchemaEnum getValidatorSchema() {
        return JsonValidator.JsonSchemaEnum.FREIFUNK_NODE;
    }

    @Override
    protected JsonFieldConverter.JsonConversionSchemaEnum getConversionSchema() {
        return JsonFieldConverter.JsonConversionSchemaEnum.FREIFUNK_NODE;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<Map<String, Object>> extractMessages(Map<String, Object> data) {
        return (List<Map<String, Object>>) data.get("nodes");
    }

    @Override
    protected void customProcessing(Map<String, Object> message) {
        try {
            message.put("id_str", PushServletHelper.computeMessageId(message, message.get("id"), getSourceType()));
        } catch (Exception e) {
            DAO.log("Problem computing id : " + e.getMessage());
        }
    }
}
