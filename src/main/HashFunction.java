/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author s3931660
 */
public class HashFunction {
    
    String plainText=null;
    
    public HashFunction(String plainText){
        this.plainText = plainText;
    }
    
    public String generateHash() throws NoSuchAlgorithmException{
        
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(this.plainText.getBytes());
        
        byte byteData[] = md.digest();
        
        StringBuffer hexString = new StringBuffer();
    	for (int i=0;i<byteData.length;i++) {
    		String hex=Integer.toHexString(0xff & byteData[i]);
   	     	if(hex.length()==1) hexString.append('0');
   	     	hexString.append(hex);
    	}
        
        return hexString.toString();
    }
}
