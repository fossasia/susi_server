package ai.susi.tools;

import ai.susi.DAO;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class IO {
	
	private static Map<Path,String> map;
	private static boolean initialized = false;

	public static ByteArrayOutputStream readFile(@Nonnull File file) throws IOException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte[] b = new byte[2048];
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		int c;
		while ((c = is.read(b)) >  0) {data.write(b, 0, c);}
		return  data;
	}

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
			DAO.severe(e);
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
			DAO.severe(e);
		}
		return null;
	}

	public static class IllegalPathAccessException extends IllegalArgumentException {
	    IllegalPathAccessException() {
	        super("User path escapes the base path");
        }
    }

	/**
	 * Resolves an untrusted user-specified path against the API's base directory.
	 * Paths that try to escape the base directory are rejected.
	 *
	 * @param baseDirPath  the absolute path of the base directory that all
	 * user-specified paths should be within
	 * @param userPath  the untrusted path provided by the API user, expected to be
	 * relative to {@code baseDirPath}
	 */
	public static Path resolvePath(final Path baseDirPath, final Path userPath) {
		if (!baseDirPath.isAbsolute()) {
			throw new IllegalArgumentException("Base path must be absolute");
		}

		if (userPath.isAbsolute()) {
			throw new IllegalArgumentException("User path must be relative");
		}

		// Join the two paths together, then normalize so that any ".." elements
		// in the userPath can remove parts of baseDirPath.
		// (e.g. "/foo/bar/baz" + "../attack" -> "/foo/bar/attack")
		final Path resolvedPath = baseDirPath.resolve(userPath).normalize();

		// Make sure the resulting path is still within the required directory.
		// (In the example above, "/foo/bar/attack" is not.)
		if (!resolvedPath.startsWith(baseDirPath)) {
			throw new IllegalPathAccessException();
		}

		return resolvedPath;
	}

	public static Path resolvePath(final Path baseDirPath, final String userPath) {
		return resolvePath(baseDirPath, Paths.get(userPath));
	}

	/**
	 * Checks each subsequent path to be strictly within the baseDirPath so that
	 * no path argument leads to directory traversal attack
	 *
	 * E.g. /models/ + req.model + '/' + req.lang + /images/ + req.image
	 * Should be checked for ('models', req.model, req.lang, 'images', req.image)
	 * that each subsequent element is within the previous and not breaking out by passing
	 * req.model => ..
	 * req.lang  => ..
	 * req.image => ../../private/data.json
	 *
	 * Since just checking the last argument isn't enough
	 *
	 * @param baseDirPath the absolute path of the base directory that all
	 * user-specified paths should be within
	 * @param paths the untrusted paths provided by the API user, expected to be
	 * relative to {@code baseDirPath}
	 */
	public static Path resolvePath(final Path baseDirPath, final String... paths) {
		Path resolved = baseDirPath;
		for (String path: paths) {
			resolved = resolvePath(resolved, path);
		}

		return resolved;
	}

}
