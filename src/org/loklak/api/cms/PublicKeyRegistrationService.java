/**
 *  PublicKeyRegistrationService
 *  Copyright 29.06.2015 by Robert Mader, @treba13
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

package org.loklak.api.cms;

import org.json.JSONArray;
import org.json.JSONObject;
import org.loklak.data.DAO;
import org.loklak.server.*;
import org.loklak.tools.IO;
import org.loklak.tools.storage.JSONObjectWithDefault;

import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.IntStream;

/**
 * This service allows users to register a public key for login.
 * It can either take a public key or create a new key-pair.
 * Users can also be granted the right to register keys for individual other users or whole user roles.
 *
 * To convert a ssh-key into a readable format, call:
 * - openssl rsa -in id_rsa -pubout -outform DER -out public_key.der (for some reason needs the private key as input)
 * - convert it to BASE64
 * - if necessary, encode it URL-friendly
 */
public class PublicKeyRegistrationService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 8578478303032749879L;

	private static final int[] allowedKeyLengthRSA = {1024, 2048, 4096};

	@Override
	public BaseUserRole getMinimalBaseUserRole() {
		return BaseUserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(BaseUserRole baseUserRole) {
		JSONObject result = new JSONObject();

		switch(baseUserRole){
			case ADMIN:
				result.put("self", true);
				result.put("users", new JSONObject());
				JSONObject userRoles = new JSONObject();
				for(String userRole : DAO.userRoles.getUserRoles().keySet()){
					userRoles.put(userRole, true);
				}
				result.put("userRoles", userRoles);
				break;
			case PRIVILEGED:
			case USER:
				result.put("self", true);
				result.put("users", new JSONObject());
				result.put("userRoles", new JSONObject());
				break;
			case ANONYMOUS:
			default:
				result.put("self", false);
				result.put("users", new JSONObject());
				result.put("userRoles", new JSONObject());
		}

		return result;
	}

	public String getAPIPath() {
		return "/api/pubkey_registration.json";
	}

	@Override
	public JSONObject serviceImpl(Query post, HttpServletResponse response, Authorization authorization, final JSONObjectWithDefault permissions)
			throws APIException {

		if(post.get("register",null) == null && !post.get("create",false) && !post.get("getParameters", false)){
			throw new APIException(400, "Accepted parameters: 'register', 'create' or 'getParameters'");
		}

		JSONObject result = new JSONObject();

		// return algorithm parameters and users for whom we are allowed to register a key
		if (post.get("getParameters", false)) {
			result.put("self", permissions.getBoolean("self", false));
			result.put("users", permissions.getJSONObject("users"));
			result.put("userRoles", permissions.getJSONObject("userRoles"));

			JSONArray algorithms = new JSONArray();

			JSONObject rsa = new JSONObject();
			rsa.put("algorithm", "RSA");
			JSONArray keyLength = new JSONArray();
			for(int i : allowedKeyLengthRSA){
				keyLength.put(i);
			}
			rsa.put("lengths", keyLength);
			algorithms.put(rsa);

			result.put("algorithms", algorithms);

			return result;
		}

		// for which id?
		String id;
		if(post.get("id", null) != null) id = post.get("id", null);
		else id = authorization.getIdentity().getName();

		// check if we are allowed register a key
		if(!id.equals(authorization.getIdentity().getName())){ // if we don't want to register the key for the current user

			// create Authentication to check if the user id is a registered user
			ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, id);
			Authentication authentication = new Authentication(credential, DAO.authentication);

			if (authentication.getIdentity() == null) { // check if identity is valid
				authentication.delete();
				throw new APIException(400, "Bad request"); // do not leak if user exists or not
			}

			// check if the current user is allowed to create a key for the user in question
			boolean allowed = false;
			// check if the user in question is in 'users'
			if(permissions.getJSONObject("users", null).has(id) && permissions.getJSONObjectWithDefault("users", null).getBoolean(id, false)){
				allowed = true;
			}
			else { // check if the user role of the user in question is in 'userRoles'
				Authorization auth = new Authorization(authentication.getIdentity(), DAO.authorization, DAO.userRoles);
				for(String key : permissions.getJSONObject("userRoles").keySet()){
					if(key.equals(auth.getUserRole().getName()) && permissions.getJSONObject("userRoles").getBoolean(key)){
						allowed = true;
					}
				}
			}
			if(!allowed) throw new APIException(400, "Bad request"); // do not leak if user exists or not
		}
		else{ // if we want to register a key for this user, bad are not allowed to (for example anonymous users)
			if(!permissions.getBoolean("self", false)) throw new APIException(403, "You are not allowed to register a public key");
		}

		// set algorithm. later, we maybe want to support other algorithms as well
		String algorithm = "RSA";
		if(post.get("algorithm", null) != null){
			algorithm = post.get("algorithm", null);
		}


		if(post.get("create", false)){ // create a new key pair on the server

			if(algorithm.equals("RSA")) {
				int keyLength = 2048;
				if (post.get("key-length", null) != null) {
					int finalKeyLength = post.get("key-length", 0);
					if (!IntStream.of(allowedKeyLengthRSA).anyMatch(x -> x == finalKeyLength)) {
						throw new APIException(400, "Invalid key length.");
					}
					keyLength = finalKeyLength;
				}

				KeyPairGenerator keyGen;
				KeyPair keyPair;
				try {
					keyGen = KeyPairGenerator.getInstance(algorithm);
					keyGen.initialize(keyLength);
					keyPair = keyGen.genKeyPair();
				} catch (NoSuchAlgorithmException e) {
					throw new APIException(500, "Server error");
				}

				registerKey(authorization.getIdentity(), keyPair.getPublic());

				String pubKeyPem =	"-----BEGIN RSA PUBLIC KEY-----\n"
									+ DatatypeConverter.printBase64Binary(keyPair.getPublic().getEncoded())
									+ "\n-----END RSA PUBLIC KEY-----\n";
				String privKeyPem =	"-----BEGIN RSA PRIVATE KEY-----\n"
									+ DatatypeConverter.printBase64Binary(keyPair.getPrivate().getEncoded())
									+ "\n-----END RSA PRIVATE KEY-----\n";

				result.put("publickey_DER_BASE64", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
				result.put("privatekey_DER_BASE64", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
				result.put("publickey_PEM", pubKeyPem);
				result.put("privatekey_PEM", privKeyPem);
				result.put("keyhash", IO.getKeyHash(keyPair.getPublic()));
				try{ result.put("keyhash_urlsave", URLEncoder.encode(IO.getKeyHash(keyPair.getPublic()), "UTF-8")); } catch (UnsupportedEncodingException e){}
				result.put("message", "Successfully created and registered key. Make sure to copy the private key, it won't be saved on the server");

				return result;
			}
			throw new APIException(400, "Unsupported algorithm");
		}
		else if(post.get("register", null) != null){

			if(algorithm.equals("RSA")) {
				RSAPublicKey pub;
				String encodedKey = post.get("register", null);
				try {
					X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
					pub = (RSAPublicKey) KeyFactory.getInstance(algorithm).generatePublic(keySpec);
				} catch (Throwable e) {
					throw new APIException(400, "Public key not readable");
				}

				// check key size (not really perfect yet)
				int keySize;
				int bitLength = pub.getModulus().bitLength();
				if (bitLength <= 512) {
					keySize = 512;
				}
				else if (bitLength <= 1024) {
					keySize = 1024;
				}
				else if (bitLength <= 2048) {
					keySize = 2048;
				}
				else if (bitLength <= 4096) {
					keySize = 4096;
				}
				else {
					keySize = 8192;
				}
				if (!IntStream.of(allowedKeyLengthRSA).anyMatch(x -> x == keySize)) {
					throw new APIException(400, "Invalid key length.");
				}

				registerKey(authorization.getIdentity(), pub);

				String keypem =	"-----BEGIN RSA PUBLIC KEY-----\n"
								+ DatatypeConverter.printBase64Binary(pub.getEncoded())
								+ "\n-----END RSA PUBLIC KEY-----\n";

				result.put("publickey_DER_BASE64", Base64.getEncoder().encodeToString(pub.getEncoded()));
				result.put("publickey_PEM", keypem);
				result.put("keyhash", IO.getKeyHash(pub));
				try{ result.put("keyhash_urlsave", URLEncoder.encode(IO.getKeyHash(pub), "UTF-8")); } catch (UnsupportedEncodingException e){}
				result.put("message", "Successfully registered key.");

				return result;
			}
			throw new APIException(400, "Unsupported algorithm");
		}

		throw new APIException(400, "Invalid parameter");
	}

	/**
	 * Registers a key for an identity.
	 * TODO: different algorithms
	 * @param id
	 * @param key
     */
	private void registerKey(ClientIdentity id, PublicKey key){
		JSONObject user_obj;

		try{
			user_obj = DAO.login_keys.getJSONObject(id.toString());
		} catch (Throwable e){
			user_obj = new JSONObject();
			DAO.login_keys.put(id.toString(), user_obj);
		}

		user_obj.put(IO.getKeyHash(key),IO.getKeyAsString(key));

		DAO.login_keys.commit();
	}
}

