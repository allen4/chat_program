/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;


import com.sun.xml.messaging.saaj.packaging.mime.util.BASE64DecoderStream;
import com.sun.xml.messaging.saaj.packaging.mime.util.BASE64EncoderStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;

/**
 *
 * @author Xin
 */
public class RSA {
    public PublicKey pubKey;
    public PrivateKey privKey;
    
    public RSA(PublicKey pu){
        this.pubKey = pu;
    }
    public RSA(PrivateKey pri){
        this.privKey = pri;
    }
    
      public String encrypt(String text) {
        byte[] cipherText = null;
        try {
          // get an RSA cipher object and print the provider
          final Cipher cipher = Cipher.getInstance("RSA");
          // encrypt the plain text using the public key
          cipher.init(Cipher.ENCRYPT_MODE, this.pubKey);
          cipherText = cipher.doFinal(text.getBytes("UTF8"));
          cipherText = BASE64EncoderStream.encode(cipherText);
        } catch (Exception e) {
          e.printStackTrace();
        }
        return new String(cipherText);
      }
      
      
      
        public String decrypt(String text) {
            byte[] dectyptedText = null;
            try {
              // get an RSA cipher object and print the provider
              final Cipher cipher = Cipher.getInstance("RSA");

              // decrypt the text using the private key
              cipher.init(Cipher.DECRYPT_MODE, this.privKey);
              byte[] bText = BASE64DecoderStream.decode(text.getBytes());
              dectyptedText = cipher.doFinal(bText);

            } catch (Exception ex) {
              ex.printStackTrace();
            }

            return new String(dectyptedText);
       }
}
