/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Xin
 */
public class KeyConvert {
    
        public static PrivateKey loadPrivateKey(String key64) throws GeneralSecurityException {
            //byte[] clear = base64Decode(key64);
            byte [] clear = Base64.getDecoder().decode(key64); 
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PrivateKey priv = fact.generatePrivate(keySpec);
            Arrays.fill(clear, (byte) 0);
            return priv;
        }


        public static PublicKey loadPublicKey(String stored) throws GeneralSecurityException {
//            byte[] data = base64Decode(stored);
            byte [] data = Base64.getDecoder().decode(stored); 
            X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
            KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePublic(spec);
        }
        
        public static SecretKey loadSecretKey(String s){
            // decode the base64 encoded string
            byte[] decodedKey = Base64.getDecoder().decode(s);
            // rebuild key using SecretKeySpec
            SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES"); 
            
            return originalKey;
        }

        public static String savePrivateKey(PrivateKey priv) throws GeneralSecurityException {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec spec = fact.getKeySpec(priv,
                    PKCS8EncodedKeySpec.class);
            byte[] packed = spec.getEncoded();
//            String key64 = base64Encode(packed);

            String key64 = Base64.getEncoder().encodeToString(packed);
            Arrays.fill(packed, (byte) 0);
            return key64;
        }


        public static String savePublicKey(PublicKey publ) throws GeneralSecurityException {
            KeyFactory fact = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec spec = fact.getKeySpec(publ,
                    X509EncodedKeySpec.class);
            //return base64Encode(spec.getEncoded());
            return Base64.getEncoder().encodeToString(spec.getEncoded());
        }
        
        public static String saveSecretKey(SecretKey sk){
            // get base64 encoded version of the key
            String encodedKey = Base64.getEncoder().encodeToString(sk.getEncoded());
            return encodedKey;
        }
}
