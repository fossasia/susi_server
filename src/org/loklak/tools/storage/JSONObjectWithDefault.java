/**
 *  UserManagement
 *  Copyright 23.06.2015 by Robert Mader, @treba13
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

package org.loklak.tools.storage;

import org.json.JSONArray;
import org.json.JSONObject;


public class JSONObjectWithDefault extends JSONObject {

    public JSONObjectWithDefault(){
        super();
    }

    public JSONObjectWithDefault(JSONObject src){
        this();
        putAll(src);
    }

    public boolean getBoolean(String key, boolean dftval){
        try{
            return getBoolean(key);
        }
        catch (Exception _){
            return dftval;
        }
    }

    public double getDouble(String key, double dftval){
        try{
            return getDouble(key);
        }
        catch (Exception _){
            return dftval;
        }
    }

    public int getInt(String key, int dftval){
        try{
            return getInt(key);
        }
        catch (Exception _){
            return dftval;
        }
    }
    public JSONArray getJSONArray(String key, JSONArray dftval){
        try{
            return getJSONArray(key);
        }
        catch (Exception _){
            return dftval;
        }
    }
    public JSONObject getJSONObject(String key, JSONObject dftval){
        try{
            return getJSONObject(key);
        }
        catch (Exception _){
            return dftval;
        }
    }
    public long getLong(String key, long dftval){
        try{
            return getLong(key);
        }
        catch (Exception _){
            return dftval;
        }
    }
    public String getString(String key, String dftval){
        try{
            return getString(key);
        }
        catch (Exception _){
            return dftval;
        }
    }
}
