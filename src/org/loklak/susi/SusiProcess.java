/**
 *  SusiProcess
 *  Copyright 29.06.2016 by Michael Peter Christen, @0rb1t3r
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package org.loklak.susi;

import org.json.JSONObject;
import org.loklak.api.search.ConsoleService;

public class SusiProcess {
    
    public static enum Type {console;}
    
    private JSONObject json;

    public SusiProcess(JSONObject json) {
        this.json = json;
    }
    public Type getType() {
        return this.json.has("type") ? Type.valueOf(this.json.getString("type")) : null;
    }
    public String getExpression() {
        return this.json.has("expression") ? this.json.getString("expression") : "";
    }
    public SusiData apply(SusiData json) {
        Type type = this.getType();
        if (type == Type.console) {
            String expression = this.getExpression();
            expression = SusiAction.insertData(expression, json);
            json = ConsoleService.console(expression);
            return json;
        }

        return json;
    }
}
