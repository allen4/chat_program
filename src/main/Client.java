/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.twolattes.json.EntityMarshaller;
import com.twolattes.json.Json;
import com.twolattes.json.TwoLattes;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.Socket;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


/**
 *
 * @author Administrator
 */
public class Client {
    private DataInputStream sInput;
    private DataOutputStream sOutput;
    public Socket socket;
    
    private String server;
    private int port;
    
    KeyPairGen kp;
    PrivateKey pk;
    PublicKey puk;
    String sPuk;
    
    AES aes;
    
    Client(String server,int port){
        this.server = server;
        this.port = port;
    }
    
    public void start() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, GeneralSecurityException{
        
        try{
            System.setProperty("javax.net.ssl.trustStore", "cacerts.jks");

            SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
            socket = ssf.createSocket(this.server, this.port);

            sInput = new DataInputStream(socket.getInputStream());
            sOutput = new DataOutputStream(socket.getOutputStream());

            SSLSession session = ((SSLSocket) socket).getSession();
            Certificate[] cchain = session.getPeerCertificates();
            System.out.println("======================================================="
                    + "\n1. SSL establishment phase\n"
                    + "=======================================================");
            System.out.println("The Certificates used by peer");
            for (int i = 0; i < cchain.length; i++) {
              System.out.println(((X509Certificate) cchain[i]).getSubjectDN());
            }
            System.out.println("Peer host is " + session.getPeerHost());
            System.out.println("Cipher is " + session.getCipherSuite());
            System.out.println("Protocol is " + session.getProtocol());
            System.out.println("ID is " + new BigInteger(session.getId()));
            System.out.println("Session created in " + session.getCreationTime());
            System.out.println("Session accessed in " + session.getLastAccessedTime());
            System.out.println("======================================================="
                    + "\n2. Authentication phase\n"
                    + "=======================================================");

        }catch(IOException e){}
        
        BufferedReader stdIn =
                new BufferedReader(new InputStreamReader(System.in));
        
        String fromServer, fromUser;
        while((fromServer = sInput.readUTF())!=null){
            System.out.println(fromServer);
            if(fromServer.equals("Authenticate Successfully!")||fromServer.equals("You are logging on as a guest.")){
                break;
            }
            
            fromUser = stdIn.readLine();
            if(fromUser !=null){
                if(fromServer.equals("Please input your password: ")||fromServer.equals("Wrong password, please try it again: ")){
                   HashFunction hf = new HashFunction(fromUser);
                   fromUser = hf.generateHash();
                }
                sOutput.writeUTF(fromUser);
            }
        }
       System.out.println("======================================================="
               + "\n3. Data encryption key exchange phase\n"
               + "=======================================================");
       //Sending out client's public key
       kp = new KeyPairGen();
       pk = kp.privateKey;
       puk = kp.publicKey;
       
       sPuk = KeyConvert.savePublicKey(puk);
       
       sOutput.writeUTF(sPuk);
        System.out.println("Public key of client: \n"+puk);
       //Read the secret key
        //Read the encrypted message
        String eskey = sInput.readUTF();
        RSA rsa = new RSA(pk);
        String skey = rsa.decrypt(eskey);
        //Load the secret key from the decrypted string
        SecretKey sk = KeyConvert.loadSecretKey(skey);
        System.out.println("\nShared secret key for the client and server: \n"+sk);
        System.out.println("======================================================="
                + "\n4. Secured client-server communication\n"
                + "=======================================================");
        aes = new AES(sk);
        
        new ListenFromServer(aes).start();
    }
    
    void sendMessage(Json.Object o1){
        try{
            String out = o1+"";             
            String eOut = aes.encrypt(out);
 
            sOutput.writeUTF(eOut);
  
            sOutput.flush();
        }catch(IOException e){}
    }
    
    	private void disconnect() {
		try { 
			if(sInput != null) sInput.close();
		}
		catch(Exception e) {} // not much else I can do
		try {
			if(sOutput != null) sOutput.close();
		}
		catch(Exception e) {} // not much else I can do
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {} // not much else I can do	
			
	}
    
    class ListenFromServer extends Thread{
        boolean run = true;
        AES aes;
        public ListenFromServer(AES a){
            this.aes = a;
        };
        public void run(){
            
            while(run){
                try {
                    String einput = sInput.readUTF();
                    
                    String input = aes.decrypt(einput);
                    Json.Value v = Json.fromString(input);
                    
                    JSONParser parser = new JSONParser();
                    JSONObject object = (JSONObject) parser.parse(v.toString());

                    String type = (String)object.get("type");

                    if(type.equals("message")){
                        
                        EntityMarshaller<ChatMessage> c = TwoLattes.createEntityMarshaller(ChatMessage.class);
                        ChatMessage message = c.unmarshall(v);

                        String prefix = message.prefix;
                        String content = message.content;
                        System.out.println(prefix+content);
                    }else if(type.equals("roomchange")){
                        EntityMarshaller<RoomChange> c = TwoLattes.createEntityMarshaller(RoomChange.class);
                        RoomChange rc = c.unmarshall(v);
                        /*if(rc.roomid==null){
                            this.run=false;
                            this.stop();
                        }else*/
                        //System.out.println("Change room successfully!");
                    }else if(type.equals("createroom")){
                        System.out.println("Create room successfully!");
                    }else if(type.equals("roomlist")){
                        EntityMarshaller<RoomList> c = TwoLattes.createEntityMarshaller(RoomList.class);
                        RoomList roomlist = c.unmarshall(v);
                        
                        String output = "";
                        String rooms = "";
                        
                        for(int i=0;i<roomlist.rooms.size();i++){
                               
                            //System.out.println(roomlist.rooms.get(i).roomid + ":");
                            rooms = rooms + roomlist.rooms.get(i).roomid + ": ";
                            for(int j=0;j<roomlist.rooms.get(i).identities.size();j++){
                                if(roomlist.rooms.get(i).identities.get(j).equals(roomlist.rooms.get(i).owner)){
                                    rooms = rooms + roomlist.rooms.get(i).identities.get(j)+"* ";
                                }else{
                                rooms = rooms + roomlist.rooms.get(i).identities.get(j)+" ";
                                }
                                //System.out.print(roomlist.rooms.get(i).identities.get(j)+", ");
                            }
                            
                            rooms = rooms+"\n";
                        }
                        
                        System.out.println(rooms);
                    }else if (type.equals("roomcontents")){
                        EntityMarshaller<RoomContents> c = TwoLattes.createEntityMarshaller(RoomContents.class);
                        RoomContents roomcontents = c.unmarshall(v);    
                        
                        ArrayList identities = roomcontents.identities;
                        String owner = roomcontents.owner;
                        String roomName = roomcontents.roomid;
                        
                        System.out.print(roomName +" contaions: ");
                        for(int i=0;i<identities.size();i++){
                            if(identities.get(i).equals(owner)){
                                System.out.print(identities.get(i) + "* ");
                            }else{
                                System.out.print(identities.get(i) + " ");
                            }
                        }
                        System.out.println();
                    }else if(type.equals("start")){
                        
                        EntityMarshaller<Start> c = TwoLattes.createEntityMarshaller(Start.class);
                        Start start = c.unmarshall(v);   
                        
                        System.out.println(start.content);
                    }else if(type.equals("quit")){
                        System.out.println("Good bye!");
                        break;
                    }else if(type.equals("rp")){
                        System.out.println("Please input your password: ");
                        Scanner s = new Scanner(System.in);
                    }

                    //System.out.print(">");
                } 
                catch(SocketException e){
                    System.out.println("Lost connection...");
                    break;
                }
                catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }

            }//finish the loop
            disconnect();	
        }
        
        
        
    }
}
