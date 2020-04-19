/**
 *  LoginServlet
 *  Copyright 27.05.2015 by Robert Mader, @treba13
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
import ai.susi.server.*;
import ai.susi.json.JsonTray;
import ai.susi.tools.IO;
import org.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import ai.susi.tools.DateParser;
import ai.susi.tools.VerifyRecaptcha;

/**
 * This service allows users to login, logout or to check their login status.
 *
 * The following parameter combinations are valid:
 * - checkLogin=true    # check the login status
 * - logout=true        # end the current session
 * - login,password,type(session | cookie | access_token | check_password)    # login with password. session starts a browser session, cookie sets a long living cookie, access_token creates an access_token and returns it with the server reply,
 * -                                                                          check_password tells the client about whether password supplied by it is correct or not
 * - login,keyhash    # first part of login via public/private key handshake. The keyhash is displayed on key registration
 * - sessionID,response    # second part. sessionID is part of the server reply of the first part. response is a signature of the challenge, also part of the server reply.
 * At the moment, only SHA256withRSA is supported as signature algorithm
 *
 * example:
 * http://localhost:4000/aaa/login.json?login=bob@the.builder&type=access-token&password=yeshecan
 * http://localhost:4000/aaa/login.json?checkLogin=true&access_token=6O7cqoMbzlClxPwg1is31Tz5pjVwo3
 */
public class LoginService extends AbstractAPIHandler implements APIHandler {

    private static final long serialVersionUID = 8578478303032749879L;
    private static final long defaultAccessTokenExpireTime = 7 * 24 * 60 * 60; // one week

    @Override
    public UserRole getMinimalUserRole() {
        return UserRole.ANONYMOUS;
    }

    @Override
    public JSONObject getDefaultPermissions(UserRole baseUserRole) {
        JSONObject result = new JSONObject();
        result.put("maxInvalidLogins", 10);
        result.put("blockTimeSeconds", 120);
        result.put("periodSeconds", 60);
        result.put("blockedUntil", 0);
        return result;
    }

    public String getAPIPath() {
        return "/aaa/login.json";
    }

    @Override
    public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions)
            throws APIException {

        long valid_seconds;

        // login check for app
        if(post.get("checkLogin", false)) {
            JSONObject result = new JSONObject(true);
            if (!authorization.getIdentity().isAnonymous()) {
                result.put("loggedIn", true);
                result.put("accepted", true);
                result.put("message", "You are logged in as " + authorization.getIdentity().getName());
            }
            else{
                result.put("loggedIn", false);
                result.put("accepted", false);
                result.put("message", "Not logged in");
            }
            return new ServiceResponse(result);
        }

        // do logout if requested
        boolean logout = post.get("logout", false);
        boolean delete = post.get("delete", false);
        if (logout || delete) {    // logout if requested

            if(post.get("access_token") == null || authorization.getIdentity() == null) {
                throw new APIException(401, "Unauthorized");
            }
            // invalidate session
            post.getRequest().getSession().invalidate();

            // delete cookie if set
            deleteLoginCookie(response);

            if (delete) {
                ClientCredential pwcredential = new ClientCredential(authorization.getIdentity());
                delete = DAO.hasAuthentication(pwcredential);
                delete = DAO.hasAuthorization(authorization.getIdentity());
                if (delete) {
                    DAO.deleteAuthorization(authorization.getIdentity());
                    DAO.deleteAuthentication(pwcredential);
                }
            }
            
            JSONObject result = new JSONObject(true);
            result.put("accepted", true);
            result.put("message", delete ? "Account deletion successful" : "Logout successful");
            return new ServiceResponse(result);
        }

        // check login type by checking which parameters are set
        boolean passwordLogin = false;
        boolean pubkeyHello = false;
        boolean pubkeyLogin = false;

        if(post.get("login", null) != null && post.get("password", null) != null && post.get("type", null ) != null){
            passwordLogin = true;
        }
        else if(post.get("login", null) != null && post.get("keyhash", null) != null){
            pubkeyHello = true;
        }
        else if(post.get("sessionID", null) != null && post.get("response", null) != null){
            pubkeyLogin = true;
        }
        else{
            throw new APIException(400, "Bad login parameters.");
        }

        // check if user is blocked because of too many invalid login attempts
        checkInvalidLogins(post, authorization, permissions);

        if (passwordLogin) { // do login via password

            String login = post.get("login", null);
            String password = post.get("password", null);
            String type = post.get("type", null);
            ClientCredential pwcredential = new ClientCredential(ClientCredential.Type.passwd_login, login);
            Authentication authentication = getAuthentication(post, authorization, pwcredential);
            ClientIdentity identity = authentication.getIdentity();

            // check if the password is valid
            String passwordHash;
            String salt;
            
            try {
                passwordHash = authentication.getString("passwordHash");
                salt = authentication.getString("salt");
            } catch (Throwable e) {
                DAO.log("Invalid login try for user: " + identity.getName() + " from host: " + post.getClientHost() + " : password or salt missing in database");
                throw new APIException(422, "Invalid credentials");
            }

            if (!passwordHash.equals(getHash(password, salt))) {

                // save invalid login in accounting object
                Accounting accouting = DAO.getAccounting(identity);
                accouting.getRequests().addRequest(this.getClass().getCanonicalName(), "invalid login");

                DAO.log("Invalid login try for user: " + identity.getName() + " via passwd from host: " + post.getClientHost());
                throw new APIException(422, "Invalid credentials");
            }

            JSONObject result = new JSONObject(true);
            result.put("accepted", false);

            switch (type) {
                case "session": // create a browser session
                    post.getRequest().getSession().setAttribute("identity", identity);
                    break;
                case "cookie": // set a long living cookie
                    // create random string as token
                    String loginToken = createRandomString(30);

                    // create cookie
                    Cookie loginCookie = new Cookie("login", loginToken);
                    loginCookie.setPath("/");
                    loginCookie.setMaxAge(defaultCookieTime.intValue());

                    // write cookie to database
                    ClientCredential cookieCredential = new ClientCredential(ClientCredential.Type.cookie, loginToken);
                    Authentication cookieAuth = DAO.getAuthentication(cookieCredential);
                    cookieAuth.setIdentity(identity);
                    cookieAuth.setExpireTime(defaultCookieTime);

                    response.addCookie(loginCookie);
                    break;
                case "access-token": // create and display an access token

                    try {
                        valid_seconds = post.get("valid_seconds", defaultAccessTokenExpireTime);
                    } catch (Throwable e) {
                        throw new APIException(422, "Invalid value for 'valid_seconds'");
                    }

                    JsonTray CaptchaConfig = DAO.captchaConfig;
                    JSONObject captchaObj = CaptchaConfig.has("config") ? CaptchaConfig.getJSONObject("config") : new JSONObject();
                    Boolean isLoginCaptchaEnabled = captchaObj.has("login") ? captchaObj.getBoolean("login") : true;

                    if(isLoginCaptchaEnabled && checkOneOrMoreInvalidLogins(post, authorization, permissions) ){
                        String gRecaptchaResponse = post.get("g-recaptcha-response", null);
                        boolean isRecaptchaVerified = VerifyRecaptcha.verify(gRecaptchaResponse);
                        if(!isRecaptchaVerified){
                            result.put("message", "Please verify recaptcha");
                            result.put("accepted", false);
                            return new ServiceResponse(result);
                        }
                    }
                    String token = createAccessToken(identity, valid_seconds);

                    if(valid_seconds == -1) result.put("valid_seconds", "forever");
                    else result.put("valid_seconds", valid_seconds);

                    result.put("uuid", identity.getUuid());
                    result.put("access_token", token);
                    result.put("accepted", true);

                    break;

                case "check_password" : // only tell the client about correct password
                    result.put("accepted", true);
                    break;

                default:
                    throw new APIException(400, "Invalid type");
            }

            DAO.log("login for user: " + identity.getName() + " via passwd from host: " + post.getClientHost());

            result.put("message", "You are logged in as " + identity.getName());
            result.put("accepted", true);

            // store the IP of last login in accounting object
            Accounting accounting = DAO.getAccounting(identity);
            accounting.getJSON().put("lastLoginIP", post.getClientHost());

            // store the time of last login in accounting object
            Date currentTime = new Date();
            accounting.getJSON().put("lastLoginTime", DateParser.formatISO8601(currentTime));

            accounting.commit();

            return new ServiceResponse(result);
        }
        else if(pubkeyHello){ // first part of pubkey login: if the key hash is known, create a challenge

            String login = post.get("login", null);
            String keyHash = post.get("keyhash", null);

            Authentication authentication = getAuthentication(post, authorization, new ClientCredential(ClientCredential.Type.passwd_login, login));
            ClientIdentity identity = authentication.getIdentity();

            String key = DAO.loadKey(identity, keyHash);
            if (key == null) throw new APIException(400, "Unknown key");

            String challengeString = createRandomString(30);
            String newSessionID = createRandomString(30);

            ClientCredential credential = new ClientCredential(ClientCredential.Type.pubkey_challange, newSessionID);
            Authentication challenge_auth = DAO.getAuthentication(credential);
            challenge_auth.setIdentity(identity);
            challenge_auth.put("activated", true);

            challenge_auth.put("challenge", challengeString);
            challenge_auth.put("key", key);
            challenge_auth.setExpireTime(60 * 10);

            JSONObject result = new JSONObject();
            result.put("accepted", true);
            result.put("challenge", challengeString);
            result.put("sessionID", newSessionID);
            result.put("message", "Found valid key for this user. Sign the challenge with you public key and send it back, together with the sessionID");
            return new ServiceResponse(result);
        }
        else if(pubkeyLogin){ // second part of pubkey login: verify if the response to the challange is valid

            String sessionID = post.get("sessionID", null);
            String challangeResponse = post.get("response", null);

            Authentication authentication = getAuthentication(post, authorization, new ClientCredential(ClientCredential.Type.pubkey_challange, sessionID));
            ClientIdentity identity = authentication.getIdentity();

            String challenge = authentication.getString("challenge");
            PublicKey key = IO.decodePublicKey(authentication.getString("key"), "RSA");

            Signature sig;
            boolean verified;
            try {
                sig = Signature.getInstance("SHA256withRSA");
                sig.initVerify(key);
                sig.update(challenge.getBytes());
                verified = sig.verify(Base64.getDecoder().decode(challangeResponse));
            } catch (NoSuchAlgorithmException e){
                throw new APIException(404, "No such algorithm");
            } catch (InvalidKeyException e){
                throw new APIException(422, "Invalid key");
            } catch (Throwable e){
                throw new APIException(400, "Bad signature");
            }

            if(verified){

                try {
                    valid_seconds = post.get("valid_seconds", defaultAccessTokenExpireTime);
                } catch (Throwable e) {
                    throw new APIException(400, "Invalid value for 'valid_seconds'");
                }

                String token = createAccessToken(identity, valid_seconds);

                JSONObject result = new JSONObject();

                if(valid_seconds == -1) result.put("valid_seconds", "forever");
                else result.put("valid_seconds", valid_seconds);

                result.put("access_token", token);
                result.put("accepted", true);
                return new ServiceResponse(result);
            }
            else {
                Accounting accouting = DAO.getAccounting(identity);
                accouting.getRequests().addRequest(this.getClass().getCanonicalName(), "invalid login");
                throw new APIException(400, "Bad Signature");
            }
        }
        throw new APIException(500, "Server error");
    }

    /**
     * little helper function to avoid code duplication
     * @param post
     * @param authorization
     * @param credential
     * @return
     * @throws APIException
     */
    private Authentication getAuthentication(Query post, Authorization authorization, ClientCredential credential) throws APIException{
        // create Authentication
        Authentication authentication = DAO.getAuthentication(credential);
        
        if (authentication.getIdentity() == null) { // check if identity is valid
            authentication.delete();

            DAO.log("Invalid login try for unknown user: " + credential.getName() + " via passwd from host: " + post.getClientHost());
            throw new APIException(401, "Invalid credentials");
        }

        else if (!authentication.getBoolean("activated", false)) { // check if identity is valid
            DAO.log("Invalid login try for user: " + credential.getName() + " from host: " + post.getClientHost() + " : user not activated yet");
            throw new APIException(403, "User not yet activated");
        }

        return authentication;
    }

    private String createAccessToken(ClientIdentity identity, long valid_seconds) throws APIException{
        String token = createRandomString(30);

        ClientCredential accessToken = new ClientCredential(ClientCredential.Type.access_token, token);
        Authentication tokenAuthentication = DAO.getAuthentication(accessToken);

        tokenAuthentication.setIdentity(identity);

        if (valid_seconds == 0 || valid_seconds < -1) { // invalid values
            throw new APIException(422, "Invalid value for 'valid_seconds'");
        } else if (valid_seconds != -1){
            tokenAuthentication.setExpireTime(valid_seconds);
        }

        return token;
    }

    /**
     * Check if the requesting user is blocked because of too many invalid login attempts
     * @post the query as used in serviceImpl
     * @param authorization the authorization as used in serviceImpl
     * @param permissions the permissions as used in serviceImpl
     * @throws APIException if the user is blocked
     */
    private void checkInvalidLogins(Query post, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {

        // is already blocked?
        long blockedUntil = permissions.getLong("blockedUntil", 0);
        if(blockedUntil != 0) {
            if (blockedUntil > Instant.now().getEpochSecond()) {
                DAO.log("Blocked ip " + post.getClientHost() + " because of too many invalid login attempts.");
                throw new APIException(403, "Too many invalid login attempts. Try again in "
                        + (blockedUntil - Instant.now().getEpochSecond()) + " seconds");
            }
            else{
                authorization.setPermission(this, "blockedUntil", 0);
            }
        }

        // check if too many invalid login attempts were made already

        Accounting accouting = DAO.getAccounting(authorization.getIdentity());
        JSONObject invalidLogins = accouting.getRequests().getRequests(this.getClass().getCanonicalName());
        long period = permissions.getLong("periodSeconds", 600) * 1000; // get time period in which wrong logins are counted (e.g. the last 10 minutes)
        int counter = 0;
        for(String key : invalidLogins.keySet()){
            if(Long.parseLong(key, 10) > System.currentTimeMillis() - period) counter++;
        }
        if(counter > permissions.getInt("maxInvalidLogins", 10)){
            authorization.setPermission(this, "blockedUntil", Instant.now().getEpochSecond() + permissions.getInt("blockTimeSeconds", 120));
            throw new APIException(403, "Too many invalid login attempts. Try again in "
                    + permissions.getInt("blockTimeSeconds", 120) + " seconds");
        }
    }

    private boolean checkOneOrMoreInvalidLogins(Query post, Authorization authorization, JsonObjectWithDefault permissions) throws APIException {
        Accounting accouting = DAO.getAccounting(authorization.getIdentity());
        JSONObject invalidLogins = accouting.getRequests().getRequests(this.getClass().getCanonicalName());
        long period = permissions.getLong("periodSeconds", 600) * 1000; // get time period in which wrong logins are counted (e.g. the last 10 minutes)
        int counter = 0;
        for(String key : invalidLogins.keySet()){
            if(Long.parseLong(key, 10) > System.currentTimeMillis() - period) counter++;
        }
        if(counter > 0){
            return true;
        }
        return false;
    }
}
