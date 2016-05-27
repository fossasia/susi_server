/**
 * JsonValidator
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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.loklak.data.DAO;
import org.loklak.objects.SourceType;

import java.io.IOException;

/**
 * Validate json file against json schema
 */
public class JsonValidator {

    public enum JsonSchemaEnum {
        FOSSASIA("fossasia.json", SourceType.FOSSASIA_API),
        OPENWIFIMAP("openwifimap.json", SourceType.OPENWIFIMAP),
        NODELIST("nodelist-1.0.1.json", SourceType.NODELIST),
        FREIFUNK_NODE("freifunk-node.json", SourceType.FREIFUNK_NODE),
        ;
        private String filename;
        private SourceType sourceType;
        JsonSchemaEnum(String filename, SourceType sourceType) {
            this.filename = filename;
            this.sourceType = sourceType;
        }
        public String getFilename() { return filename; }

        public SourceType getSourceType() {
            return sourceType;
        }

        public static JsonSchemaEnum valueOf(SourceType sourceType) {
            for (JsonSchemaEnum schema : JsonSchemaEnum.values()) {
                if (schema.getSourceType().equals(sourceType)) {
                    return schema;
                }
            }
            throw new IllegalArgumentException("Invalid sourceType value : " + sourceType);
        }

    }

    private JsonSchema schema;

    public JsonValidator(JsonSchemaEnum schemaEnum) throws IOException {
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.byDefault();
        JsonNode schemaInJson;
        try {
            schemaInJson = DAO.getSchema(schemaEnum.getFilename());
            this.schema = schemaFactory.getJsonSchema(schemaInJson);
        } catch (ProcessingException e) {
            throw new IOException("Unable to parse json schema " + schemaEnum.getFilename());
        }
    }

    public ProcessingReport validate(String jsonText) throws IOException {

        ProcessingReport report;
        JsonNode toValidate = JsonLoader.fromString(jsonText);
        try {
            report = this.schema.validate(toValidate);
        } catch (ProcessingException e) {
            throw new IOException("Error validating json text : " +  e.getMessage());
        }
        return report;
    }
}
