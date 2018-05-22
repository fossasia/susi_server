/**
 *  Copyright 19.05.2018 by Akshat Jain, @Akshat-Jain
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
import java.io.FileOutputStream;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;

public class AddNewDevice extends AbstractAPIHandler implements APIHandler{

    @Override
    public String getAPIPath() {
        return "/aaa/addNewDevice.json";
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

      FileWriter fw = null;
      BufferedWriter bw = null;
      PrintWriter pw = null;

      String email = post.get("email",null);
      String uid = post.get("uid",null);

      JSONObject result = new JSONObject(true);

      if(email == null || uid == null){
        throw new APIException(400, "Missing arguments!");
      }

      String path = DAO.data_dir.getPath()+"/devices/";

      File user = new File(path);
      File user_dir = new File(path, email);
      if (!user_dir.exists()) user_dir.mkdirs();

      String absoluteFilePath = path+email+"/"+uid+".txt";
      File file = new File(absoluteFilePath);

      String absoluteFilePathAll = path+email+"/"+"all.txt";
      File allFile = new File(absoluteFilePathAll);

      if(!file.exists()){
          
          try{
            file.createNewFile();
          }
          catch (IOException io)
          {
            DAO.severe(io);
          }

          result.put("EmailID", email);
          result.put("UID", uid);

          try{
            FileOutputStream fs = new FileOutputStream(absoluteFilePath);
            String s = result.toString();
            byte b[]=s.getBytes();//converting string into byte array
            fs.write(b);
          }
          catch(Exception e){
            DAO.severe(e);
          }

          try{
            fw = new FileWriter(absoluteFilePathAll, true);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);
            pw.println(result.toString());
            pw.flush();
            pw.close();
            bw.close();
            fw.close();
          }
          catch (IOException io) {
            DAO.severe(io);
          }

          result.put("accepted", true);
          result.put("message", "Device information added!");
          return new ServiceResponse(result);
      }
      else{
          result.put("EmailID", email);
          result.put("UID", uid);
          result.put("accepted", true);
          result.put("message", "Device information added!");
          return new ServiceResponse(result);
      }
      
  }
}
