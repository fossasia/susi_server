/**
 *  AbstractAPIHandler
 *  Copyright 17.05.2016 by Michael Peter Christen, @0rb1t3r
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

package org.loklak.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.log.Log;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.http.ClientConnection;
import org.loklak.http.RemoteAccess;
import org.loklak.tools.UTF8;

@SuppressWarnings("serial")
public abstract class AbstractAPIHandler extends HttpServlet implements APIHandler {

    private String[] serverProtocolHostStub = null;

    public AbstractAPIHandler() {
        this.serverProtocolHostStub = null;
    }
    
    public AbstractAPIHandler(String[] serverProtocolHostStub) {
        this.serverProtocolHostStub = serverProtocolHostStub;
    }

    @Override
    public String[] getServerProtocolHostStub() {
        return this.serverProtocolHostStub;
    }

    @Override
    public abstract APIServiceLevel getDefaultServiceLevel();

    @Override
    public abstract APIServiceLevel getCustomServiceLevel(Authorization auth);

    @Override
    public JSONObject[] service(Query call, Authorization rights) throws APIException {

        // make call to the embedded api
        if (this.serverProtocolHostStub == null) return new JSONObject[]{serviceImpl(call, rights)};
        
        // make call(s) to a remote api(s)
        JSONObject[] results = new JSONObject[this.serverProtocolHostStub.length];
        for (int rc = 0; rc < results.length; rc++) {
            try {
                StringBuilder urlquery = new StringBuilder();
                for (String key: call.getKeys()) {
                    urlquery.append(urlquery.length() == 0 ? '?' : '&').append(key).append('=').append(call.get(key, ""));
                }
                String urlstring = this.serverProtocolHostStub[rc] + this.getAPIPath() + urlquery.toString();
                byte[] jsonb = ClientConnection.download(urlstring);
                if (jsonb == null || jsonb.length == 0) throw new IOException("empty content from " + urlstring);
                String jsons = UTF8.String(jsonb);
                JSONObject json = new JSONObject(jsons);
                if (json == null || json.length() == 0) {
                    results[rc] = null;
                    continue;
                };
                results[rc] = json;
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return results;
    }
    
    public abstract JSONObject serviceImpl(Query call, Authorization rights) throws APIException;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        process(request, response, post);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query query = RemoteAccess.evaluate(request);
        query.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, query);
    }
    
    private void process(HttpServletRequest request, HttpServletResponse response, Query query) throws ServletException, IOException {
        
        // basic protection
        APIServiceLevel serviceLevel = getDefaultServiceLevel();
        if (query.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        if (serviceLevel == APIServiceLevel.ADMIN && !query.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost, your request comes from " + query.getClientHost()); return;} // danger! do not remove this!
        
        
        // user identification
        Identity identity = getIdentity(request, response);
        
        // user authorization: we use the identification of the user to get the assigned authorization
        JSONObject authorization_obj = null;
        if (DAO.authorization.has(identity.toString())) {
            authorization_obj = DAO.authorization.getJSONObject(identity.toString());
        } else {
            authorization_obj = new JSONObject();
            DAO.authorization.put(identity.toString(), authorization_obj);
        }
        Authorization authorization = new Authorization(authorization_obj, DAO.authorization, identity);

        // user accounting: we maintain static and persistent user data; we again search the accounts using the usder identity string
        //JSONObject accounting_persistent_obj = DAO.accounting_persistent.has(user_id) ? DAO.accounting_persistent.getJSONObject(anon_id) : DAO.accounting_persistent.put(user_id, new JSONObject()).getJSONObject(user_id);
        Accounting accounting_temporary = DAO.accounting_temporary.get(identity.toString());
        if (accounting_temporary == null) {
            accounting_temporary = new Accounting();
            DAO.accounting_temporary.put(identity.toString(), accounting_temporary);
        }
        
        // the accounting data is assigned to the authorization
        authorization.setAccounting(accounting_temporary);
        
        // extract standard query attributes
        String callback = query.get("callback", "");
        boolean jsonp = callback.length() > 0;
        boolean minified = query.get("minified", false);
        
        try {
            JSONObject json = serviceImpl(query, authorization);
            if  (json == null) {
                response.sendError(400, "your request does not contain the required data");
                return;
             }
    
            // evaluate special fields
            if (json.has("$EXPIRES")) {
                int expires = json.getInt("$EXPIRES");
                FileHandler.setCaching(response, expires);
                json.remove("$EXPIRES");
            }
        
            // write json
            query.setResponse(response, "application/javascript");
            response.setCharacterEncoding("UTF-8");
            PrintWriter sos = response.getWriter();
            if (jsonp) sos.print(callback + "(");
            sos.print(json.toString(minified ? 0 : 2));
            if (jsonp) sos.println(");");
            sos.println();
            query.finalize();
        } catch (APIException e) {
            response.sendError(e.getStatusCode(), e.getMessage());
            return;
        }
    }
    
    /**
     * Checks a request for valid login data, send via cookie or parameters
     * @return user identity if some login is active, anonymous identity otherwise
     */
    private Identity getIdentity(HttpServletRequest request, HttpServletResponse response){
    	
    	// check for login information
		if("true".equals(request.getParameter("logout"))){
			request.getSession().invalidate();
		}
		if(request.getSession().getAttribute("identity") != null){
			Log.getLog().info("login via session");
			return (Identity) request.getSession().getAttribute("identity");
		}
		if(request.getCookies() != null){
			for(Cookie cookie : request.getCookies()){
				if("login".equals(cookie.getName())){
					
					Log.getLog().info("login cookie found");
					Credential credential = new Credential(Credential.Type.cookie, cookie.getValue());
					
					if(DAO.authentication.has(credential.toString())){
						Log.getLog().info("cookie valid");
						
						Authentication authentication = new Authentication(DAO.authentication.getJSONObject(credential.toString()), DAO.authentication);
						Identity identity = authentication.getIdentity();
						
						if(identity != null){
							request.getSession().setAttribute("identity",identity);
							//TODO: set validity time up again
							return identity;
						}
					}
					break;
				}
			}
		}
    	if (request.getParameter("login") != null && request.getParameter("password") != null ){ // check if login parameters are set
    		Log.getLog().info("login via passwd");
    		
    		String login = null;
    		String password = null;
			try {
				login = URLDecoder.decode(request.getParameter("login"),"UTF-8");
				password = URLDecoder.decode(request.getParameter("password"),"UTF-8");
			} catch (UnsupportedEncodingException e) {}

    		Credential credential = new Credential(Credential.Type.passwd_login, login);
    		
    		// check if password is valid
    		if(DAO.authentication.has(credential.toString())){
    			Log.getLog().info("user found");
    			
    			JSONObject authentication_obj = DAO.authentication.getJSONObject(credential.toString());
    			
    			if(authentication_obj.has("passwordHash") && authentication_obj.has("salt")){
    				Log.getLog().info("testing passwd");
					String passwordHash = authentication_obj.getString("passwordHash");
					String salt = authentication_obj.getString("salt");
					
	    			if(getHash(password, salt).equals(passwordHash)){
	    				Log.getLog().info("passwd valid");
	    				
	    				Authentication authentication = new Authentication(authentication_obj, DAO.authentication);
	    				Identity identity = authentication.getIdentity();
	    				
	    				// only create a session if requested (by login page)
	            		if("true".equals(request.getParameter("request_session"))){
	            			Log.getLog().info("session requested");
	            			
	            			request.getSession().setAttribute("identity",identity);
	            			
	            			if("true".equals(request.getParameter("request_cookie"))){
	            				Log.getLog().info("cookie requested");
	            				
	            				String loginToken = createRandomString(30);
	            				
	            				Cookie loginCookie = new Cookie("login", loginToken);
	            				loginCookie.setPath("/");
	            				loginCookie.setMaxAge(60 * 60 * 24 * 7);
	            				
	            				Credential cookieCredential = new Credential(Credential.Type.cookie, loginToken);
	            				JSONObject user_obj = new JSONObject();
	            				user_obj.put("id",identity.toString());
	            				DAO.authentication.put(cookieCredential.toString(), user_obj);
	        	    			
	            				response.addCookie(loginCookie);
	        	    		}
	            		}
	            		
	            		return identity;
	    			}
	    			Log.getLog().info("passwd invalid");
    			}
    		}
    		else{
    			Log.getLog().info("user not found");
    		}
    	}
    	else if (request.getParameter("login_token") != null){
    		Credential credential = new Credential(Credential.Type.login_token, request.getParameter("login_token"));
    		if(DAO.authentication.has(credential.toString())){
    			Authentication authentication = new Authentication(DAO.authentication.getJSONObject(credential.toString()), DAO.authentication);
				return authentication.getIdentity();
    		}
    	}
    	
        return getAnonymousIdentity(request);
    }
    
    /**
     * Create or get a anonymous identity
     * @return
     */
    private Identity getAnonymousIdentity(HttpServletRequest request){
    	Credential credential = new Credential(Credential.Type.host, request.getRemoteHost());
    	
        if (!DAO.authentication.has(credential.toString())) {
            DAO.authentication.put(credential.toString(), new JSONObject());
        }
        return new Identity(credential.toString());
    }
    
    /**
     * Create a hash for an input an salt
     * @param input
     * @param salt
     * @return String hash
     */
    public static String getHash(String input, String salt){
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update((salt + input).getBytes());
			return Base64.getEncoder().encodeToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}
    
    public static String createRandomString(Integer length){
    	char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    	StringBuilder sb = new StringBuilder();
    	Random random = new Random();
    	for (int i = 0; i < length; i++) {
    	    char c = chars[random.nextInt(chars.length)];
    	    sb.append(c);
    	}
    	return sb.toString();
    }
}
