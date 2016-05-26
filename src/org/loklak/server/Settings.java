/**
 *  JsonFile
 *  Copyright 26.02.2015 by Robert Mader, @treba123
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

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.loklak.tools.storage.JsonFile;

public class Settings extends JsonFile {
    

    public Settings(File file) throws IOException {
        super(file);
    }

    private PrivateKey private_key = null;
    private PublicKey public_key = null;
    private String key_algorithm = null;
    private String peer_hash = null;
    private String hash_algorithm = null;

    /**
     * Get the private key as PrivateKey
     * @return PrivateKey private_key
     */
    public PrivateKey getPrivateKey(){
        return private_key;
    }
    
    /**
     * Get the private key as String
     * @return String representation of the private key
     */
    public String getPrivateKeyAsString(){
        return getKeyAsString(private_key);
    }
    
    /**
     * Get the public key as PublicKey
     * @return PublicKey public_key
     */
    public PublicKey getPublicKey(){
        return public_key;
    }
    
    /**
     * Get the public key as String
     * @return String representation of the public key
     */
    public String getPublicKeyAsString(){
        return getKeyAsString(public_key);
    }
    
    /**
     * Get the key algorithm e.g. RSA
     * @return String algorithm
     */
    public String getKeyAlgorithm(){
        return new String(key_algorithm);
    }
    
    /**
     * Get the hash of the public key
     * @return String hash
     */
    public String getPeerHash(){
        return new String(peer_hash);
    }
    
    /**
     * Get the hash algorithm for the public key
     * @return String hash_algorithm
     */
    public String getPeerHashAlgorithm(){
        return new String(hash_algorithm);
    }
    
    /**
     * Get String representation of a key
     * @param key
     * @return String representation of a key
     */
    public String getKeyAsString(Key key){
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    /**
     * Calculate the hash of the public key
     */
    private void setPeerHash(){
        hash_algorithm = "SHA-256";
        peer_hash = getKeyHash(public_key, hash_algorithm);
        put("peer_hash",peer_hash);
        put("peer_hash_algorithm",hash_algorithm);
        
    }
    
    /**
     * Create hash for a key
     * @param pubkey
     * @param algorithm
     * @return String hash
     */
    public static String getKeyHash(PublicKey pubkey, String algorithm){
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.update(pubkey.getEncoded());
            return Base64.getEncoder().encodeToString(md.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Load private key from file
     * @return true if a valid key is found in file
     */
    public boolean loadPrivateKey(){
        if(!has("private_key") || !has("key_algorithm")) return false;
        
        String encodedKey = getString("private_key");
        String algorithm = getString("key_algorithm");
        
        try{
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
            PrivateKey priv = KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
            private_key = priv;
            key_algorithm = algorithm;
            return true;
        }
        catch(NoSuchAlgorithmException | InvalidKeySpecException e){
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Load public key from file
     * @return true if a valid key is found in file
     */
    public boolean loadPublicKey(){
        if(!has("public_key") || !has("key_algorithm")) return false;
        
        String encodedKey = getString("public_key");
        String algorithm = getString("key_algorithm");
        
        PublicKey pub = decodePublicKey(encodedKey, algorithm);
        if(pub != null){
            public_key = pub;
            key_algorithm = algorithm;
            setPeerHash();
            return true;
        }
        return false;
    }
    
    /**
     * Set the private key
     * @param key
     * @param algorithm
     */
    public void setPrivateKey(PrivateKey key, String algorithm){
        put("private_key", getKeyAsString(key));
        private_key = key;
        put("key_algorithm",algorithm);
        key_algorithm = algorithm;
    }
    
    /**
     * Set the public key
     * @param key
     * @param algorithm
     */
    public void setPublicKey(PublicKey key, String algorithm){
        put("public_key", getKeyAsString(key));
        public_key = key;
        put("key_algorithm",algorithm);
        key_algorithm = algorithm;
        setPeerHash();
    }
    
    /**
     * Create PublicKey from String representation
     * @param encodedKey
     * @param algorithm
     * @return PublicKey public_key
     */
    public static PublicKey decodePublicKey(String encodedKey, String algorithm){
        try{
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(encodedKey));
            PublicKey pub = KeyFactory.getInstance(algorithm).generatePublic(keySpec);
            return pub;
        }
        catch(NoSuchAlgorithmException | InvalidKeySpecException e){
            e.printStackTrace();
        }
        return null;
    }
    

}
