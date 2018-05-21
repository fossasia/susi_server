/**
 *  Copyright 21.05.2018 by Akshat Jain, @Akshat-Jain
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

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.json.JsonTray;
import ai.susi.mind.SusiSkill;
import ai.susi.server.*;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public class GetAllDevices extends AbstractAPIHandler implements APIHandler{

    @Override
    public String getAPIPath() {
        return "/aaa/getAllDevices.json";
    }

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.USER;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        return null;
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, JsonObjectWithDefault permissions) throws APIException {

      String email = post.get("email",null);

      JSONObject obj = new JSONObject(true);
      JSONArray array = new JSONArray();

      File f = new File(DAO.data_dir.getPath()+"/devices/"+email);

      if(email == null){
        throw new APIException(400, "Missing arguments!");
      }

      if(!f.exists() || email.isEmpty()){
        JSONObject newObj = new JSONObject(true);
        newObj.put("accepted",false);
        newObj.put("message","No connected device found for this email.");
        return new ServiceResponse(newObj);
      }

      String path = DAO.data_dir.getPath()+"/devices/"+email+"/all.txt";

      try{

        BufferedReader in = new BufferedReader(new FileReader(path));
        String str;

        List<String> list = new ArrayList<String>();
        while((str = in.readLine()) != null){
            list.add(str);
        }
        String[] stringArr = list.toArray(new String[0]);

        for (int i = 0; i < list.size(); i++) {
          JSONObject jsonObj = new JSONObject(list.get(i));
          array.put(jsonObj);
        }

        try {
            obj.put("devices", array);
        } 
        catch (JSONException e) {
            e.printStackTrace();
        }

      }
      catch(FileNotFoundException e){
        DAO.severe(e);
      }
      catch(IOException e){
        DAO.severe(e);
      }

      obj.put("accepted", true);
      obj.put("message", "Information of all devices received!");
      
      return new ServiceResponse(obj);

    }
}
