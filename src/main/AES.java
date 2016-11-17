/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;


import com.sun.xml.messaging.saaj.packaging.mime.util.BASE64DecoderStream;
import com.sun.xml.messaging.saaj.packaging.mime.util.BASE64EncoderStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 *
 * @author Xin
 */
public class AES {
    	public Cipher ecipher;
	public Cipher dcipher;

	public SecretKey key;
        
        public AES(SecretKey sk) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException{
            
                        this.key = sk;
			ecipher = Cipher.getInstance("AES");
			dcipher = Cipher.getInstance("AES"); 
                        
                        ecipher.init(Cipher.ENCRYPT_MODE, key);
                        dcipher.init(Cipher.DECRYPT_MODE, key);
        }
        
	public String encrypt(String str) {

              try {

                    // encode the string into a sequence of bytes using the named charset

                    // storing the result into a new byte array. 

                    byte[] utf8 = str.getBytes("UTF8");

                    byte[] enc = ecipher.doFinal(utf8);

                    // encode to base64

                    enc = BASE64EncoderStream.encode(enc);

                    return new String(enc);

              }

              catch (Exception e) {

                    e.printStackTrace();

              }

                return null;
    }   
        
 	public String decrypt(String str) {

              try {

                    // decode with base64 to get bytes

            byte[] dec = BASE64DecoderStream.decode(str.getBytes());

            byte[] utf8 = dcipher.doFinal(dec);

            // create new string based on the specified charset

            return new String(utf8, "UTF8");

              }

              catch (Exception e) {

                    e.printStackTrace();

              }

          return null;

    }       
}
