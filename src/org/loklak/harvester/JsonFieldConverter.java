/**
 * JsonFieldConverter
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


package org.loklak.harvester;

import org.loklak.data.DAO;

import java.io.IOException;
import java.util.*;

public class JsonFieldConverter {

    public enum JsonConversionSchemaEnum {
        FOSSASIA("fossasia.json")
        ;
        private String filename;
        JsonConversionSchemaEnum(String filename) {
            this.filename = filename;
        }
        public String getFilename() { return filename; }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> convert(Map<String, Object> initialJson, JsonConversionSchemaEnum schema) throws IOException {
        final Map<String, Object> convSchema = DAO.getConversionSchema(schema.getFilename());
        List<List> convRules = (List) convSchema.get("rules");

        Map<String, List<String>> convRulesMap = new HashMap<>();

        for (List rule : convRules) {
            List<String> toInsert = new ArrayList<>();
            convRulesMap.put((String) rule.get(0), toInsert);
            if (rule.get(1) instanceof String) {
                toInsert.add((String) rule.get(1));
            } else {
                for (String afterField : (List<String>) rule.get(1)) {
                    toInsert.add(afterField);
                }
            }
        }

        Map<String, Object> result = initialJson;
        Iterator it = convRulesMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, List<String>> entry = (Map.Entry) it.next();
            String key = entry.getKey();
            if (result.containsKey(key)) {
                Object value = result.get(key);
                for (String newKey : entry.getValue()) {
                    result.put(newKey, value);
                }
            }
        }
        return result;
    }
}
