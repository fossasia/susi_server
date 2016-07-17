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
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import org.eclipse.jetty.util.log.Log;
import org.loklak.tools.IO;
import org.loklak.tools.storage.JsonFile;

import javax.annotation.Nonnull;

public class Settings extends JsonFile {
    

    public Settings(@Nonnull File file) throws IOException {
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
    public synchronized PrivateKey getPrivateKey(){
        return private_key;
    }
    
    /**
     * Get the private key as String
     * @return String representation of the private key
     */
    public synchronized String getPrivateKeyAsString(){
        return IO.getKeyAsString(private_key);
    }
    
    /**
     * Get the public key as PublicKey
     * @return PublicKey public_key
     */
    public synchronized PublicKey getPublicKey(){
        return public_key;
    }
    
    /**
     * Get the public key as String
     * @return String representation of the public key
     */
    public synchronized String getPublicKeyAsString(){
        return IO.getKeyAsString(public_key);
    }
    
    /**
     * Get the key algorithm e.g. RSA
     * @return String algorithm
     */
    public synchronized String getKeyAlgorithm(){
        return new String(key_algorithm);
    }
    
    /**
     * Get the hash of the public key
     * @return String hash
     */
    public synchronized String getPeerHash(){
        return new String(peer_hash);
    }
    
    /**
     * Get the hash algorithm for the public key
     * @return String hash_algorithm
     */
    public synchronized String getPeerHashAlgorithm(){
        return new String(hash_algorithm);
    }
    
    /**
     * Calculate the hash of the public key
     */
    private synchronized void setPeerHash(){
        hash_algorithm = "SHA-256";
        peer_hash = IO.getKeyHash(public_key, hash_algorithm);
        put("peer_hash",peer_hash);
        put("peer_hash_algorithm",hash_algorithm);
        
    }
    
    /**
     * Load private key from file
     * @return true if a valid key is found in file
     */
    public synchronized boolean loadPrivateKey(){
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
        	Log.getLog().warn(e);
        }
        return false;
    }
    
    /**
     * Load public key from file
     * @return true if a valid key is found in file
     */
    public synchronized boolean loadPublicKey(){
        if(!has("public_key") || !has("key_algorithm")) return false;
        
        String encodedKey = getString("public_key");
        String algorithm = getString("key_algorithm");
        
        PublicKey pub = IO.decodePublicKey(encodedKey, algorithm);
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
    public synchronized void setPrivateKey(@Nonnull PrivateKey key, @Nonnull String algorithm){
        put("private_key", IO.getKeyAsString(key));
        private_key = key;
        put("key_algorithm",algorithm);
        key_algorithm = algorithm;
    }
    
    /**
     * Set the public key
     * @param key
     * @param algorithm
     */
    public synchronized void setPublicKey(@Nonnull PublicKey key, @Nonnull String algorithm){
        put("public_key", IO.getKeyAsString(key));
        public_key = key;
        put("key_algorithm",algorithm);
        key_algorithm = algorithm;
        setPeerHash();
    }
}
