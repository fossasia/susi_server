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

import java.io.IOException;

/**
 * Validate json file against json schema
 */
public class JsonValidator {

    public enum JsonSchemaEnum {
        FOSSASIA("fossasia.json"),
        OPENWIFIMAP("openwifimap.json"),
        NODELIST("nodelist-1.0.1.json"),
        FREIFUNK_NODE("freifunk-node.json"),
        ;
        private String filename;
        JsonSchemaEnum(String filename) {
            this.filename = filename;
        }
        public String getFilename() { return filename; }
    }

    private JsonSchemaFactory schemaFactory;

    public JsonValidator() {
        this.schemaFactory = JsonSchemaFactory.byDefault();
    }

    public ProcessingReport validate(String jsonText, JsonSchemaEnum schema) throws IOException {
        final JsonNode fossasiaSchema= DAO.getSchema(schema.getFilename());
        JsonSchema jsonSchema;
        try {
            jsonSchema = this.schemaFactory.getJsonSchema(fossasiaSchema);
        } catch (ProcessingException e) {
            throw new IOException("Unable to parse json schema " + schema.getFilename());
        }

        ProcessingReport report = null;

        JsonNode toValidate = JsonLoader.fromString(new String(jsonText));
        try {
            report= jsonSchema.validate(toValidate);
        } catch (ProcessingException e) {
            e.printStackTrace();
        }
        return report;
    }
}
