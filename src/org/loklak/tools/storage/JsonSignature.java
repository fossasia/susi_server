/**
 *  JsonSignature
 *  Copyright 17.08.2015 by Robert Mader, @treba13
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

package org.loklak.tools.storage;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

/**
 * This class offers functions to add and verify signatures of JSONObjects
 */
public class JsonSignature {

    public static String signatureString = "LOKLAK_SIGNATURE";

    /**
     * Create and add a signature to a JSONObject
     * @param obj the JSONObject
     * @param key the private key to use
     * @throws InvalidKeyException if the key is not valid (for example not RSA)
     * @throws SignatureException if something with the JSONObject is bogus
     */
    public static void addSignature(JSONObject obj, PrivateKey key) throws InvalidKeyException, SignatureException {

        removeSignature(obj);

        Signature signature;
        try {
            signature = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            return; //does not happen
        }

        signature.initSign(key);
        signature.update(obj.toString().getBytes(StandardCharsets.UTF_8));

        byte[] sigBytes = signature.sign();

        obj.put(signatureString, new String(Base64.getEncoder().encode(sigBytes)));
    }

    /**
     * Remove the signature
     * @param obj the JSONObject
     */
    public static void removeSignature(JSONObject obj){
        if(obj.has(signatureString)) obj.remove(signatureString);
    }

    /**
     * Verfies if the signature of a JSONObject is valid
     * @param obj the JSONObject
     * @param key the public key of the signature issuer
     * @return true if the signature is valid
     * @throws SignatureException if the JSONObject does not have a signature or something with the JSONObject is bogus
     * @throws InvalidKeyException if the key is not valid (for example not RSA)
     */
    public static boolean verify(JSONObject obj, PublicKey key) throws SignatureException, InvalidKeyException {

        if(!obj.has(signatureString)) throw new SignatureException("No signature supplied");

        Signature signature;
        try {
            signature = Signature.getInstance("SHA256withRSA");
        } catch (NoSuchAlgorithmException e) {
            return false; //does not happen
        }

        String sigString = obj.getString(signatureString);
        byte[] sig = Base64.getDecoder().decode(sigString);
        obj.remove(signatureString);

        signature.initVerify(key);
        signature.update(obj.toString().getBytes(StandardCharsets.UTF_8));
        boolean res = signature.verify(sig);

        obj.put(signatureString, sigString);

        return res;
    }

    public static boolean hasSignature(JSONObject obj){
        return obj.has(signatureString);
    }

    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.genKeyPair();

        JSONObject randomObj = new JSONObject("{\n" +
                "        \"_id\": \"57b44e738d9af9fa2df13b27\",\n" +
                "        \"index\": 0,\n" +
                "        \"guid\": \"13af6838-08c8-4709-8dff-5ecb20bbaaa7\",\n" +
                "        \"isActive\": false,\n" +
                "        \"balance\": \"$2,092.08\",\n" +
                "        \"picture\": \"http://placehold.it/32x32\",\n" +
                "        \"age\": 22,\n" +
                "        \"eyeColor\": \"blue\",\n" +
                "        \"name\": \"Wyatt Jefferson\",\n" +
                "        \"gender\": \"male\",\n" +
                "        \"company\": \"GEEKFARM\",\n" +
                "        \"email\": \"wyattjefferson@geekfarm.com\",\n" +
                "        \"phone\": \"+1 (855) 405-2375\",\n" +
                "        \"address\": \"506 Court Street, Gambrills, Minnesota, 8953\",\n" +
                "        \"about\": \"Ea sunt quis non occaecat aliquip sint eiusmod. Aliquip id non ut sunt est laboris proident reprehenderit incididunt velit. Quis deserunt dolore aliqua voluptate magna laborum minim. Pariatur voluptate ad consequat culpa sit veniam eiusmod et ex ipsum.\\r\\n\",\n" +
                "        \"registered\": \"2015-08-08T03:21:53 -02:00\",\n" +
                "        \"latitude\": -39.880621,\n" +
                "        \"longitude\": 44.053688,\n" +
                "        \"tags\": [\n" +
                "            \"non\",\n" +
                "            \"cupidatat\",\n" +
                "            \"in\",\n" +
                "            \"Lorem\",\n" +
                "            \"tempor\",\n" +
                "            \"fugiat\",\n" +
                "            \"aliqua\"\n" +
                "        ],\n" +
                "        \"friends\": [\n" +
                "            {\n" +
                "                \"id\": 0,\n" +
                "                \"name\": \"Gail Blevins\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 1,\n" +
                "                \"name\": \"Tricia Francis\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"id\": 2,\n" +
                "                \"name\": \"Letitia Winters\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"greeting\": \"Hello, Wyatt Jefferson! You have 1 unread messages.\",\n" +
                "        \"favoriteFruit\": \"strawberry\"\n" +
                "    }");

        addSignature(randomObj,keyPair.getPrivate());
        if(hasSignature(randomObj)) System.out.println("Verify: " + verify(randomObj,keyPair.getPublic()));
        removeSignature(randomObj);
    }
}
