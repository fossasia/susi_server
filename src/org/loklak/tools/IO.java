package org.loklak.tools;

import org.eclipse.jetty.util.log.Log;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class IO {
	
	private static Map<Path,String> map;
	private static boolean initialized = false;

	public static String readFile(@Nonnull Path path) throws IOException
	{
		byte[] encoded = Files.readAllBytes(path);
		return new String(encoded);
	}
	
	public static String readFileCached(@Nonnull Path path) throws IOException
	{
		Path absPath = path.toAbsolutePath();
		if(!initialized) init();
		if(map.containsKey(absPath)){
			return map.get(absPath);
		}
		else{
			String result = readFile(absPath);
			map.put(absPath, result);
			return result;
		}
	}
	
	private static void init(){
		map = new HashMap<Path,String>();
		initialized = true;
	}

	/**
	 * Create hash for a key
	 * @param pubkey
	 * @param algorithm
	 * @return String hash
	 */
	public static String getKeyHash(@Nonnull PublicKey pubkey, @Nonnull String algorithm){
		try {
			MessageDigest md = MessageDigest.getInstance(algorithm);
			md.update(pubkey.getEncoded());
			return Base64.getEncoder().encodeToString(md.digest());
		} catch (NoSuchAlgorithmException e) {
			Log.getLog().warn(e);
		}
		return null;
	}

	/**
	 * Create hash for a key, use default algorithm SHA-256
	 * @param pubkey
	 * @return String hash
	 */
	public static String getKeyHash(@Nonnull PublicKey pubkey){
		return getKeyHash(pubkey, "SHA-256");
	}

	/**
	 * Get String representation of a key
	 * @param key
	 * @return String representation of a key
	 */
	public static String getKeyAsString(@Nonnull Key key){
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	/**
	 * Create PublicKey from String representation
	 * @param encodedKey
	 * @param algorithm
	 * @return PublicKey public_key
	 */
	public synchronized static PublicKey decodePublicKey(@Nonnull String encodedKey, @Nonnull String algorithm){
		try{
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
			PublicKey pub = KeyFactory.getInstance(algorithm).generatePublic(keySpec);
			return pub;
		}
		catch(NoSuchAlgorithmException | InvalidKeySpecException e){
			Log.getLog().warn(e);
		}
		return null;
	}
}
