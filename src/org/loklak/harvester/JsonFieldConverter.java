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
import java.util.regex.Pattern;

public class JsonFieldConverter {

    public enum JsonConversionSchemaEnum {
        FOSSASIA("fossasia.json"),
        OPENWIFIMAP("openwifimap.json"),
        NODELIST_NODE("nodelist-node.json"),
        FREIFUNK_NODE("freifunk-node.json"),
        ;
        private String filename;
        JsonConversionSchemaEnum(String filename) {
            this.filename = filename;
        }
        public String getFilename() { return filename; }
    }

    public List<Map<String, Object>> convert(List<Map<String, Object>> initialJson, JsonConversionSchemaEnum schema)
    throws IOException {
        List<Map<String, Object>> result = new ArrayList();
        for (Map<String, Object> o : initialJson) {
            result.add(this.convert(o, schema));
        }
        return result;
    }
    @SuppressWarnings("unchecked")
    public Map<String, Object> convert(Map<String, Object> initialJson, JsonConversionSchemaEnum schema) throws IOException {
        final Map<String, Object> convSchema = DAO.getConversionSchema(schema.getFilename());
        List<List> convRules = (List) convSchema.get("rules");

        Map<String, List<String>> convRulesMap = new HashMap<>();

        for (List rule : convRules) {
            List<String> toInsert = new ArrayList<>();
            convRulesMap.put((String) rule.get(0), toInsert);

            // the 2nd rule can be either a string
            if (rule.get(1) instanceof String) {
                toInsert.add((String) rule.get(1));
            } else {
            // or an array
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
            if (!result.containsKey(key)) {
                continue;
            }

            Object value = result.remove(key);
            for (String newKey : entry.getValue()) {
                // deep-mapping
                if (newKey.contains(".")) {
                    String[] deepFields = newKey.split(Pattern.quote("."));
                    Map<String, Object> currentLevel = result;
                    for (int lvl = 0; lvl < deepFields.length; lvl++) {
                        if (lvl == deepFields.length - 1) {
                            currentLevel.put(deepFields[lvl], value);
                        } else {
                            if (currentLevel.get(deepFields[lvl]) == null) {
                                currentLevel.put(deepFields[lvl], new HashMap<>());
                            }
                            currentLevel = (Map<String, Object>) currentLevel.get(deepFields[lvl]);
                        }
                    }
                // simple mapping, a.k.a put the value at the root level
                } else {
                    result.put(newKey, value);
                };
            }
        }
        return result;
    }
}
