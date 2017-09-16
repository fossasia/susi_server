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

package ai.susi.server.api.aaa;

import ai.susi.DAO;
import ai.susi.json.JsonObjectWithDefault;
import ai.susi.server.*;
import ai.susi.tools.IO;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.stream.IntStream;

/**
 * This service allows users to register a public key for login.
 * It can either take a public key or create a new key-pair.
 * Users can also be granted the right to register keys for individual other users or whole user roles.
 *
 * To export your own PublikKey from java for registering, call:
 * - String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());
 *
 * To use the private key as generated in DER format in java, call:
 * - PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedPrivateKey));
 * - PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec);
 *
 * To sign a challenge as given by the login, call:
 * - Signature sig = Signature.getInstance("SHA256withRSA");
 * - sig.initSign(privateKey);
 * - sig.update(challengeString.getBytes());
 * - String result = new String(Base64.getEncoder().encode(sig.sign()));
 *
 * To create a signature with openssl (using a key in pem format), the following command should work:
 * - openssl dgst -sha256 -sign privkey.pem -out response.txt challenge.txt
 * - encode the content of response.in BASE64
 * - if necessary, encode it URL-friendly
 */
public class PublicKeyRegistrationService extends AbstractAPIHandler implements APIHandler {

	private static final long serialVersionUID = 8578478303032749879L;

	private static final int[] allowedKeySizesRSA = {1024, 2048, 4096};
	private static final int defaultKeySizeRSA = 2048;
	private static final String[] allowedFormats = {"DER", "PEM"};

	@Override
	public UserRole getMinimalUserRole() {
		return UserRole.ANONYMOUS;
	}

	@Override
	public JSONObject getDefaultPermissions(UserRole baseUserRole) {
		JSONObject result = new JSONObject();

		switch(baseUserRole){
			case BUREAUCRAT:
            case ADMIN:
            case ACCOUNTCREATOR:
            case REVIEWER:
			case USER:
				result.put("self", true);
				result.put("users", new JSONObject());
				break;
            case BOT:
            case ANONYMOUS:
			default:
				result.put("self", false);
				result.put("users", new JSONObject());
		}

		return result;
	}

	public String getAPIPath() {
		return "/aaa/pubkey_registration.json";
	}

	@Override
	public ServiceResponse serviceImpl(Query post, HttpServletResponse response, Authorization authorization, final JsonObjectWithDefault permissions)
			throws APIException {

		if(post.get("register",null) == null && !post.get("create",false) && !post.get("getParameters", false)){
			throw new APIException(400, "Accepted parameters: 'register', 'create' or 'getParameters'");
		}

		JSONObject result = new JSONObject(true);
		result.put("accepted", false);
		result.put("message", "Error: Unable to create and register keys");

		// return algorithm parameters and users for whom we are allowed to register a key
		if (post.get("getParameters", false)) {
			result.put("self", permissions.getBoolean("self", false));
			result.put("users", permissions.getJSONObject("users"));
			result.put("userRoles", permissions.getJSONObject("userRoles"));

			JSONObject algorithms = new JSONObject();

			JSONObject rsa = new JSONObject();
			JSONArray keySizes = new JSONArray();
			for(int i : allowedKeySizesRSA){
				keySizes.put(i);
			}
			rsa.put("sizes", keySizes);
			rsa.put("defaultSize", defaultKeySizeRSA);
			algorithms.put("RSA", rsa);
			result.put("algorithms", algorithms);

			JSONArray formats = new JSONArray();
			for(String format : allowedFormats){
				formats.put(format);
			}
			result.put("formats", formats);
			result.put("accepted", true);
			result.put("message", "Success : Showing parameters and users allowed");

			return new ServiceResponse(result);
		}

		// for which id?
		String id;
		if(post.get("id", null) != null) id = post.get("id", null);
		else id = authorization.getIdentity().getName();

		// check if we are allowed register a key
		if(!id.equals(authorization.getIdentity().getName())){ // if we don't want to register the key for the current user

			// create Authentication to check if the user id is a registered user
			ClientCredential credential = new ClientCredential(ClientCredential.Type.passwd_login, id);
			Authentication authentication = DAO.getAuthentication(credential);

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
				Authorization auth = DAO.getAuthorization(authentication.getIdentity());
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
				int keySize = 2048;
				if (post.get("key-size", null) != null) {
					int finalKeyLength = post.get("key-size", 0);
					if (!IntStream.of(allowedKeySizesRSA).anyMatch(x -> x == finalKeyLength)) {
						throw new APIException(400, "Invalid key size.");
					}
					keySize = finalKeyLength;
				}

				KeyPairGenerator keyGen;
				KeyPair keyPair;
				try {
					keyGen = KeyPairGenerator.getInstance(algorithm);
					keyGen.initialize(keySize);
					keyPair = keyGen.genKeyPair();
				} catch (NoSuchAlgorithmException e) {
					throw new APIException(500, "Server error");
				}

				DAO.registerKey(authorization.getIdentity(), keyPair.getPublic());

				String pubkey_pem = null, privkey_pem = null;
				try {
					StringWriter writer = new StringWriter();
					PemWriter pemWriter = new PemWriter(writer);
					pemWriter.writeObject(new PemObject("PUBLIC KEY", keyPair.getPublic().getEncoded()));
					pemWriter.flush();
					pemWriter.close();
					pubkey_pem = writer.toString();
				}
				catch (IOException e){}
				try {
					StringWriter writer = new StringWriter();
					PemWriter pemWriter = new PemWriter(writer);
					pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
					pemWriter.flush();
					pemWriter.close();
					privkey_pem = writer.toString();
				}
				catch (IOException e){}

				result.put("publickey_DER_BASE64", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
				result.put("privatekey_DER_BASE64", Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
				result.put("publickey_PEM", pubkey_pem);
				result.put("privatekey_PEM", privkey_pem);
				result.put("keyhash", IO.getKeyHash(keyPair.getPublic()));
				try{ result.put("keyhash_urlsave", URLEncoder.encode(IO.getKeyHash(keyPair.getPublic()), "UTF-8")); } catch (UnsupportedEncodingException e){}
				result.put("key-size", keySize);
				result.put("accepted", true);
				result.put("message", "Successfully created and registered key. Make sure to copy the private key, it won't be saved on the server");

				return new ServiceResponse(result);
			}
			throw new APIException(400, "Unsupported algorithm");
		}
		else if(post.get("register", null) != null){

			if(algorithm.equals("RSA")) {
				String type = post.get("type", null);
				if(type == null) type = "DER";

				RSAPublicKey pub;
				String encodedKey;
				try { encodedKey = URLDecoder.decode(post.get("register", null), "UTF-8");} catch (Throwable e){throw new APIException(500, "Server error");}
				DAO.log("Key (" + type + "): " + encodedKey);

				if(type.equals("DER")) {
					try {
						X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
						pub = (RSAPublicKey) KeyFactory.getInstance(algorithm).generatePublic(keySpec);
					} catch (Throwable e) {
						throw new APIException(400, "Public key not readable (DER)");
					}
				}
				else if(type.equals("PEM")){
					try {
						PemReader pemReader = new PemReader(new StringReader(encodedKey));
						PemObject pem = pemReader.readPemObject();
						X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pem.getContent());
						pub = (RSAPublicKey) KeyFactory.getInstance(algorithm).generatePublic(keySpec);
					} catch (Exception e) {
						throw new APIException(400, "Public key not readable (PEM)");
					}
				}
				else{
					throw new APIException(400, "Invalid value for 'type'.");
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
				if (!IntStream.of(allowedKeySizesRSA).anyMatch(x -> x == keySize)) {
					throw new APIException(400, "Invalid key length.");
				}

				DAO.registerKey(authorization.getIdentity(), pub);

				String pubkey_pem = null;
				try {
					StringWriter writer = new StringWriter();
					PemWriter pemWriter = new PemWriter(writer);
					pemWriter.writeObject(new PemObject("PUBLIC KEY", pub.getEncoded()));
					pemWriter.flush();
					pemWriter.close();
					pubkey_pem = writer.toString();
				}
				catch (IOException e){}

				result.put("publickey_DER_BASE64", Base64.getEncoder().encodeToString(pub.getEncoded()));
				result.put("publickey_PEM", pubkey_pem);
				result.put("keyhash", IO.getKeyHash(pub));
				try{ result.put("keyhash_urlsave", URLEncoder.encode(IO.getKeyHash(pub), "UTF-8")); } catch (UnsupportedEncodingException e){}
				result.put("message", "Successfully registered key.");
				result.put("accepted", true);

				return new ServiceResponse(result);
			}
			throw new APIException(400, "Unsupported algorithm");
		}

		throw new APIException(400, "Invalid parameter");
	}

}

