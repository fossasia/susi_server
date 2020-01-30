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

package ai.susi.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Random;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;

@SuppressWarnings("serial")
public abstract class AbstractAPIHandler extends HttpServlet implements APIHandler {

    private String[] serverProtocolHostStub = null;
    public static final Long defaultCookieTime = (long) (60 * 60 * 24 * 7);
    public static final Long defaultAnonymousTime = (long) (60 * 60 * 24);

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
    public abstract UserRole getMinimalUserRole();

	@Override
	public abstract JSONObject getDefaultPermissions(UserRole baseUserRole);
    
    public abstract ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization rights, final JsonObjectWithDefault permissions) throws APIException;
    
    
    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doHead(request, response);
        setCORS(response);
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query post = RemoteAccess.evaluate(request);
        process(request, response, post);
        setCORS(response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Query query = RemoteAccess.evaluate(request);
        query.initPOST(RemoteAccess.getPostMap(request));
        process(request, response, query);
        setCORS(response);
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        super.doOptions(request, response);
        setCORS(response); // required by angular framework; detailed CORS can be set within the servlet
    }
    
    public static void setCORS(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "POST");
        response.setHeader("Access-Control-Allow-Headers", "accept, content-type");
    }
    
    private void process(HttpServletRequest request, HttpServletResponse response, Query query) throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        
        // basic protection
        UserRole minimalUserRole = getMinimalUserRole();
        if (minimalUserRole == null) minimalUserRole = UserRole.ANONYMOUS;

        if (query.isDoS_blackout()) {
            String message = "your request frequency is too high";
            logClient(startTime, query, null, 503, message);
            response.sendError(503, message); return;
        } // DoS protection
        if (DAO.getConfig("users.admin.localonly", true) && (minimalUserRole == UserRole.ADMIN || minimalUserRole == UserRole.SUPERADMIN) && !query.isLocalhostAccess()) {
            String message = "access only allowed from localhost, your request comes from " + query.getClientHost();
            logClient(startTime, query, null, 503, message);
            response.sendError(503, message); return;
        } // danger! do not remove this!
        
        // user identification
        ClientIdentity identity = getIdentity(startTime, request, response, query);
        
        // user authorization: we use the identification of the user to get the assigned authorization
        Authorization authorization = DAO.getAuthorization(identity);

        if (authorization.getUserRole().ordinal() < minimalUserRole.ordinal()) {
            String message = "Base user role not sufficient. Your base user role is '" + authorization.getUserRole().name() + "', minimum user role required is '" + minimalUserRole.getName() + "'";
            logClient(startTime, query, identity, 401, message);
            response.sendError(401, message);
			return;
        }
        
        // extract standard query attributes
        String callback = query.get("callback", "");
        boolean jsonp = callback.length() > 0;
        boolean minified = query.get("minified", false);
        
        try {
            ServiceResponse serviceResponse = serviceImpl(query, response, authorization, new JsonObjectWithDefault(authorization.getPermission()));
            if  (serviceResponse == null) {
                String message = "your request does not contain the required data";
                logClient(startTime, query, identity, 400, message);
                response.sendError(400, message);
                return;
            }
            
            if (serviceResponse.hasCORS()) {
                setCORS(response);
            }
            
            // write json
            query.setResponse(response, serviceResponse.getMimeType());
            response.setCharacterEncoding("UTF-8");
            
            if (serviceResponse.isObject() || serviceResponse.isArray()) {
                if (serviceResponse.isObject()) {
                    JSONObject json = serviceResponse.getObject();
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
                }
                PrintWriter sos = response.getWriter();
                if (jsonp) sos.print(callback + "(");
                String out = serviceResponse.toString(minified);
                sos.print(out);
                if (jsonp) sos.println(");");
                sos.println();
                logClient(startTime, query, identity, 200, "ok: " + (minified ? out : serviceResponse.toString(true)));
            } else if (serviceResponse.isString()) {
                PrintWriter sos = response.getWriter();
                String out = serviceResponse.toString(false);
                sos.print(out);
                logClient(startTime, query, identity, 200, "ok: " + out);
            } else if (serviceResponse.isByteArray()) {
                response.getOutputStream().write(serviceResponse.getByteArray());
                response.setHeader("Access-Control-Allow-Origin", "*");
                logClient(startTime, query, identity, 200, "ok (ByteArray)");
            }
            query.finalize();
        } catch (APIException e) {
            String message = e.getMessage();
            logClient(startTime, query, identity, e.getStatusCode(), message);
            response.sendError(e.getStatusCode(), message);
            return;
        }
    }
    
    /**
     * Checks a request for valid login data, either a existing session, a cookie or an access token
     * @return user identity if some login is active, anonymous identity otherwise
     */
    private ClientIdentity getIdentity(long startTime, HttpServletRequest request, HttpServletResponse response, Query query) {
    	
    	if (getLoginCookie(request) != null) { // check if login cookie is set
			
			Cookie loginCookie = getLoginCookie(request);
			
			ClientCredential credential = new ClientCredential(ClientCredential.Type.cookie, loginCookie.getValue());
			Authentication authentication = DAO.getAuthentication(credential);
			
			if (authentication.getIdentity() != null && authentication.checkExpireTime()) {

				//reset cookie validity time
				authentication.setExpireTime(defaultCookieTime);
				loginCookie.setMaxAge(defaultCookieTime.intValue());
				loginCookie.setPath("/"); // bug. The path gets reset
				response.addCookie(loginCookie);

				ClientIdentity identity = authentication.getIdentity();
	            logClient(startTime, query, identity, 0, "user request using cookie");
				return identity;
			}

			authentication.delete();

			// delete cookie if set
			deleteLoginCookie(response);

			DAO.log("Invalid login try via cookie from host: " + query.getClientHost());
		} else if (request.getSession().getAttribute("identity") != null) { // check session is set
		    ClientIdentity identity = (ClientIdentity) request.getSession().getAttribute("identity");
            DAO.log("USER REQUEST using browser session: " + identity.getClient());
            return identity;
		} else if (request.getParameter("access_token") != null) { // access tokens can be used by api calls, somehow the stateless equivalent of sessions for browsers
    		ClientCredential credential = new ClientCredential(ClientCredential.Type.access_token, request.getParameter("access_token"));
    		Authentication authentication = DAO.getAuthentication(credential);
			
    		// check if access_token is valid
    		if (authentication.getIdentity() != null) {
    			ClientIdentity identity = authentication.getIdentity();
    			
    			if (authentication.checkExpireTime()) {
                    logClient(startTime, query, identity, 0, "user login via access token");
    				
    				if ("true".equals(request.getParameter("request_session"))) {
            			request.getSession().setAttribute("identity", identity);
                        logClient(startTime, query, identity, 0, "user requests a session");
            		}
    				if (authentication.has("one_time") && authentication.getBoolean("one_time")) {
    					authentication.delete();
                        logClient(startTime, query, identity, 0, "user requests a one-time session, authentication deleted");
    				}
    				return identity;
    			}
    		}
            logClient(startTime, query, null, 0, "invalid access token from client");
    		return getAnonymousIdentity(query.getClientHost(), getRequestHeaderSalt(request));
    	}
    	
        return getAnonymousIdentity(query.getClientHost(), getRequestHeaderSalt(request));
    }
    
    public void logClient(
            long startTime,
            Query query,
            ClientIdentity identity,
            int httpResponseCode,
            String message) {
        String username = identity == null ? "anon" : identity.getName();
        String host = query.getClientHost();
        String q = query.toString();
        if (q.length() > 512) q = q.substring(0, 512) + "...";
        long t = System.currentTimeMillis() - startTime;
        String path = getAPIPath();
        if (q.length() > 0) path = path + "?" + q;
        if (message.length() > 512) message = message.substring(0, 512) + "...";
        DAO.log(host + " - " + username + " - " + httpResponseCode + " - " + t + "ms - " + path + " - " + message);
    }
    
    public static String getRequestHeaderSalt(HttpServletRequest request) {
        String idhint = request.getHeader("User-Agent") + request.getHeader("Accept-Encoding") + request.getHeader("Accept-Language");
        return Integer.toHexString(idhint.hashCode());
    }
    
    /**
     * Create or fetch an anonymous identity
     * @return the anonymous ClientIdentity
     */
    private static ClientIdentity getAnonymousIdentity(String remoteHost, String salt) {
    	ClientCredential credential = new ClientCredential(ClientCredential.Type.host, remoteHost + "_" + salt);
    	Authentication authentication = DAO.getAuthentication(credential);
    	
    	if (authentication.getIdentity() == null) authentication.setIdentity(credential);
    	authentication.setExpireTime(Instant.now().getEpochSecond() + defaultAnonymousTime);
    	
        return authentication.getIdentity();
    }
    
    /**
     * Create a hash for an input an salt
     * @param input Input String to generate hash
     * @param salt Salt String for encryption
     * @return String hash
     */
    public static String getHash(String input, String salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update((salt + input).getBytes());
			return Base64.getEncoder().encodeToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			DAO.severe(e);
		}
		return null;
	}
    
    /**
     * Creates a random alphanumeric string
     * @param length Length of the random string
     * @return Randomly generated string
     */
    public static String createRandomString(Integer length) {
    	char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    	StringBuilder sb = new StringBuilder();
    	Random random = new Random();
    	for (int i = 0; i < length; i++) {
    	    char c = chars[random.nextInt(chars.length)];
    	    sb.append(c);
    	}
    	return sb.toString();
    }

    /**
     * Returns a login cookie if present in the request
     * @param request Request for checking login cookie
     * @return the login cookie if present, null otherwise
     */
    private static Cookie getLoginCookie(HttpServletRequest request) {
    	if (request.getCookies() != null) {
	    	for(Cookie cookie : request.getCookies()) {
				if ("login".equals(cookie.getName())) {
					return cookie;
				}
	    	}
    	}
    	return null;
    }

    /**
     * Delete the login cookie if present
     * @param response Response to delete login cookie
     */
    protected static void deleteLoginCookie(HttpServletResponse response) {
    	Cookie deleteCookie = new Cookie("login", null);
		deleteCookie.setPath("/");
		deleteCookie.setMaxAge(0);
		response.addCookie(deleteCookie);
    }
}
