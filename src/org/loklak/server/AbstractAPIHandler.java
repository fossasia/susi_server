/**
 *  AbstractAPIHandler
 *  Copyright 17.05.2016 by Michael Peter Christen, @0rb1t3r and Robert Mader, @treba123
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
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

import javax.security.auth.login.LoginException;
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
    private static Long defaultCookieTime = (long) (60 * 60 * 24 * 7);
    private static Long defaultAnonymousTime = (long) (60 * 60 * 24);

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
    public abstract BaseUserRole getMinimalBaseUserRole();

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
            	Log.getLog().warn(e);
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
        BaseUserRole serviceLevel = getMinimalBaseUserRole();
        if (query.isDoS_blackout()) {response.sendError(503, "your request frequency is too high"); return;} // DoS protection
        if (serviceLevel == BaseUserRole.ADMIN && !query.isLocalhostAccess()) {response.sendError(503, "access only allowed from localhost, your request comes from " + query.getClientHost()); return;} // danger! do not remove this!
        
        
        // user identification
        ClientIdentity identity;
		try {
			identity = getIdentity(request, response);
		} catch (LoginException e) {
			response.sendError(422, e.getMessage());
			return;
		}
        
        // user authorization: we use the identification of the user to get the assigned authorization
        Authorization authorization = new Authorization(identity, DAO.authorization);

        if(authorization.getBaseUserRole().ordinal() < serviceLevel.ordinal()){
        	response.sendError(401, "Unauthorized");
			return;
        }

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
        
            // add session information
            JSONObject session = new JSONObject(true);
            session.put("identity", identity.toJSON());
            json.put("session", session);
            
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
    private ClientIdentity getIdentity(HttpServletRequest request, HttpServletResponse response) throws LoginException{
    	
    	// check for login information
		if("true".equals(request.getParameter("logout"))){	// logout if requested
			
			// invalidate session
			request.getSession().invalidate();
			
			// delete cookie if set
			deleteLoginCookie(response);
		}
		else if(getLoginCookie(request) != null){
			
			Cookie loginCookie = getLoginCookie(request);
			
			ClientCredential credential = new ClientCredential(ClientCredential.Type.cookie, loginCookie.getValue());
			Authentication authentication = new Authentication(credential, DAO.authentication);
			
			if(authentication.getIdentity() != null){
				
				if(authentication.checkExpireTime()){
					
					//reset cookie validity time
					authentication.setExpireTime(defaultCookieTime);
					loginCookie.setMaxAge(defaultCookieTime.intValue());
					loginCookie.setPath("/"); // bug. The path gets reset
					response.addCookie(loginCookie);
						
					return authentication.getIdentity();
				}
				else{
					authentication.delete();
					
					// delete cookie if set
					deleteLoginCookie(response);
					
					Log.getLog().info("Invalid login try via cookie from host: " + request.getRemoteHost());
				}
			}
			else{
				authentication.delete();
				
				// delete cookie if set
				deleteLoginCookie(response);
				
				Log.getLog().info("Invalid login try via cookie from host: " + request.getRemoteHost());
			}
		}
		else if(request.getSession().getAttribute("identity") != null){ // if identity is registered for session			
			return (ClientIdentity) request.getSession().getAttribute("identity");
		}
		else if (request.getParameter("login") != null && request.getParameter("password") != null ){ // check if login parameters are set
    		
    		
    		String login = null;
    		String password = null;
			try {
				login = URLDecoder.decode(request.getParameter("login"),"UTF-8");
				password = URLDecoder.decode(request.getParameter("password"),"UTF-8");
			} catch (UnsupportedEncodingException e) {}

    		ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, login);
    		Authentication authentication = new Authentication(credential, DAO.authentication);
    		
    		// check if password is valid
    		if(authentication.getIdentity() != null){
    			
    			if(authentication.has("activated") && authentication.getBoolean("activated")){
    			
	    			if(authentication.has("passwordHash") && authentication.has("salt")){
	    				
						String passwordHash = authentication.getString("passwordHash");
						String salt = authentication.getString("salt");

		    			if(getHash(password, salt).equals(passwordHash)){
		    				
		    				ClientIdentity identity = authentication.getIdentity();

		    				// only create a cookie or session if requested (by login page)
		    				if("true".equals(request.getParameter("request_cookie"))){
	            				
	            				// create random string as token
	            				String loginToken = createRandomString(30);
	            				
	            				// create cookie
	            				Cookie loginCookie = new Cookie("login", loginToken);
	            				loginCookie.setPath("/");
	            				loginCookie.setMaxAge(defaultCookieTime.intValue());
	            				
	            				// write cookie to database
	            				ClientCredential cookieCredential = new ClientCredential(ClientCredential.Type.cookie, loginToken);
	            				JSONObject user_obj = new JSONObject();
	            				user_obj.put("id",identity.toString());
	            				user_obj.put("expires_on", Instant.now().getEpochSecond() + defaultCookieTime);
	            				DAO.authentication.put(cookieCredential.toString(), user_obj, cookieCredential.isPersistent());
	        	    			
	            				response.addCookie(loginCookie);
	        	    		}
		    				else if("true".equals(request.getParameter("request_session"))){
		            			request.getSession().setAttribute("identity",identity);
		            		}
		    				
		    				Log.getLog().info("login for user: " + credential.getName() + " via passwd from host: " + request.getRemoteHost());
		            		
		            		return identity;
		    			}
		    			Log.getLog().info("Invalid login try for user: " + credential.getName() + " via passwd from host: " + request.getRemoteHost());
		    			throw new LoginException("Invalid credentials");
	    			}
	    			Log.getLog().info("Invalid login try for user: " + credential.getName() + " from host: " + request.getRemoteHost() + " : password or salt missing in database");
	    			throw new LoginException("Invalid credentials");
    			}
    			Log.getLog().info("Invalid login try for user: " + credential.getName() + " from host: " + request.getRemoteHost() + " : user not activated yet");
    			throw new LoginException("User not yet activated");
    		}
    		else{
    			authentication.delete();
    			Log.getLog().info("Invalid login try for unknown user: " + credential.getName() + " via passwd from host: " + request.getRemoteHost());
    			throw new LoginException("Invalid credentials");
    		}
    	}
    	else if (request.getParameter("access_token") != null){
    		ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, request.getParameter("access_token"));
    		Authentication authentication = new Authentication(credential, DAO.authentication);
			
    		
    		// check if access_token is valid
    		if(authentication.getIdentity() != null){
    			ClientIdentity identity = authentication.getIdentity();
    			
    			if(authentication.checkExpireTime()){
    				Log.getLog().info("login for user: " + identity.getName() + " via access token from host: " + request.getRemoteHost());
    				
    				if("true".equals(request.getParameter("request_session"))){
            			request.getSession().setAttribute("identity",identity);
            		}
    				if(authentication.has("one_time") && authentication.getBoolean("one_time")){
    					authentication.delete();
    				}
    				return identity;
    			}
    			Log.getLog().info("Invalid login try for user: " + identity.getName() + " via expired access token from host: " + request.getRemoteHost());
    			authentication.delete();
    			throw new LoginException("Invalid access token");
    		}
    		Log.getLog().info("Invalid access token from host: " + request.getRemoteHost());
    		authentication.delete();
    		throw new LoginException("Invalid access token");
    	}
    	
        return getAnonymousIdentity(request);
    }
    
    /**
     * Create or get a anonymous identity
     * @return
     */
    private ClientIdentity getAnonymousIdentity(HttpServletRequest request){
    	ClientCredential credential = new ClientCredential(ClientCredential.Type.host, request.getRemoteHost());
    	Authentication authentication = new Authentication(credential, DAO.authentication);
    	
    	if(authentication.getIdentity() == null) authentication.setIdentity(new ClientIdentity(credential.toString()));
    	authentication.setExpireTime(Instant.now().getEpochSecond() + defaultAnonymousTime);
    	
        return authentication.getIdentity();
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
			Log.getLog().warn(e);
		}
		return null;
	}
    
    /**
     * Creates a random alphanumeric string
     * @param length
     * @return
     */
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
    
    private Cookie getLoginCookie(HttpServletRequest request){
    	if(request.getCookies() != null){
	    	for(Cookie cookie : request.getCookies()){
				if("login".equals(cookie.getName())){
					return cookie;
				}
	    	}
    	}
    	return null;
    }
    
    private void deleteLoginCookie(HttpServletResponse response){
    	Cookie deleteCookie = new Cookie("login", null);
		deleteCookie.setPath("/");
		deleteCookie.setMaxAge(0);
		response.addCookie(deleteCookie);
    }
}
