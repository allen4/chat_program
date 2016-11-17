
package main;

import com.twolattes.json.Entity;
import com.twolattes.json.EntityMarshaller;
import com.twolattes.json.Json;
import com.twolattes.json.Marshaller;
import com.twolattes.json.TwoLattes;
import com.twolattes.json.Value;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import static main.Server.rooms;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server{
    public static int uniqueId;
    
    public static ArrayList<ChatRoom> rooms;
    
    public static ChatRoom mainHall = new ChatRoom("mainHall");
    
    public static HashMap<String,String> authUsers= new HashMap<String,String>(); 
    
    

    private int port;

    public Server(int port){
        
        rooms = new ArrayList<ChatRoom>();
        
        rooms.add(mainHall);
        
        this.port = port;
        authUsers.put("allen", "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4");
        
    }

    public synchronized void creatRoom(String name){
        ChatRoom cr = new ChatRoom(name);
        rooms.add(cr);
    }
    
    public void start() throws GeneralSecurityException{
        try{
        //ServerSocket serverSocket = new ServerSocket(port);
        
        
        System.setProperty("javax.net.ssl.keyStore", "keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "password");

        SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket serverSocket = ssf.createServerSocket(port);
        System.out.println("Server is up and running... ...");
        
        while(true){
            Socket socket = serverSocket.accept();
            Connection c = new Connection(socket);
            
            mainHall.roomMembers.add(c);
            c.start();
            
            System.out.println("Users the server are maintaining:");
            for (String key : Server.authUsers.keySet()) {
               System.out.println("key: " + key + " value: " + authUsers.get(key));
            }
            
            System.out.println("**********************************************************************************************************************");


        }
        }catch(IOException e){}
    }
    

}

class Connection extends Thread{

        Socket socket;

        DataInputStream sInput;
        DataOutputStream sOutput;

        int id;
        String username;

        Date date1 = null;
        int time;
        String rRoom ="";
        //room stores all the connections in the same chatroom
        public ArrayList<Connection> room;
        private int roomIndex = -1;
        
        String password ="";
        boolean authen = false;
        
        SecretKey key;
        AES aes;
        
        
        Connection(Socket socket) throws IOException, GeneralSecurityException{
            

            
            this.socket = socket;
            
            System.out.println("Thread trying to creat Data Input/Output Streams");
            
            try{

                sOutput = new DataOutputStream(socket.getOutputStream());
                sInput = new DataInputStream(socket.getInputStream());
                
                      SSLSession session = ((SSLSocket) socket).getSession();
                      Certificate[] cchain2 = session.getLocalCertificates();
                      System.out.println("");
                      for (int i = 0; i < cchain2.length; i++) {
                        System.out.println(((X509Certificate) cchain2[i]).getSubjectDN());
                      }
                      System.out.println("Peer host is " + session.getPeerHost());
                      System.out.println("Cipher is " + session.getCipherSuite());
                      System.out.println("Protocol is " + session.getProtocol());
                      System.out.println("ID is " + new BigInteger(session.getId()));
                      System.out.println("Session created in " + session.getCreationTime());
                      System.out.println("Session accessed in " + session.getLastAccessedTime());
                
            }catch(IOException e){}
            
            String inputLine, outputLine;
            
            AuthProtocol ap = new AuthProtocol();
            outputLine = ap.processInput(null);
            sOutput.writeUTF(outputLine);
            
            while((inputLine=sInput.readUTF())!=null){
                outputLine = ap.processInput(inputLine);
                sOutput.writeUTF(outputLine);
                
                if(ap.state == 3||ap.state ==4){
                    break;
                }
            }
            
            if(ap.state == 3){
                username = ap.username;
                this.authen = true;
            }else{
                id = ++Server.uniqueId;
                username = "guest"+id;
            }
            
            //key exchange phase
            
            //read the client's public key
            String sPub = sInput.readUTF();
            
            PublicKey puk = KeyConvert.loadPublicKey(sPub);
            
            System.out.println("\nPulic Key of the client: \n"+puk);
            
            //Use client's public key to encrypt the secret key
            key = KeyGenerator.getInstance("AES").generateKey();
            //Convert SecretKey object ot String
            String skey = KeyConvert.saveSecretKey(key);
            //Encrypt the conerted SecretKey
            RSA rsa = new RSA(puk);
            String eskey = rsa.encrypt(skey);
            sOutput.writeUTF(eskey);
            System.out.println("\nSecret Key for data encryption and decryption: \n" + key);
            System.out.println("");
            aes = new AES(key);
      
        }

        public static ChatRoom findRoom(String name){
            ChatRoom result = null;
            for(ChatRoom c:Server.rooms){

                if(c.name.equals(name)){
                    result = c;
                    break;
                }
            }
            return result;
        }

        public static boolean ifExist(String roomName){
            boolean result = false;
            for(ChatRoom c:Server.rooms){
                if(c.name.equals(roomName)){
                    result = true;
                    break;
                }
            }
            return result;
        }

    
        private boolean writeMsg(Json.Object o1){
            if(!socket.isConnected()){  
                return false;
            }

            try{
                String out = aes.encrypt(o1+"\n");
                sOutput.writeUTF(out);
            }catch(IOException e){}
            return true;
        }
        
        private synchronized void broadcast(Json.Object o1,ArrayList<Connection> connection){
            for(int i = 0;i<connection.size();i++){
                Connection c = connection.get(i);
                c.writeMsg(o1);
            }
        }
        
        private Json.Object sendInform(String content){
            Start message = new Start();
            message.content = content;
            Marshaller<Start> m = TwoLattes.createEntityMarshaller(Start.class);
            Json.Object o = (Json.Object)m.marshall(message);
            
            return o;
        }
        
        private void changeRoom(String roomName){
            
                    if(ifExist(roomName)){
                        
                        System.out.println("move out");
                        this.broadcast(sendInform(this.username + " has moved out to "+findRoom(roomName).name),this.room);
                        findRoom(roomName).addMember(this);
                        
                                         //for a user(connection) to find where to find their room
                                            for(int i=0;i<Server.rooms.size();i++){
                                                if(Server.rooms.get(i).name.equals(roomName)){
                                                    roomIndex=i;
                                                    break;
                                                }
                                            }
                                                                
                        room.remove(this);
                        
                        room = findRoom(roomName).roomMembers;
                        
                    }else{
                        this.writeMsg(sendInform("Room doesn't exist."));

                    }             

            
        }
        
        public void kick(String roomid,String id,int time){
                        //identify if the memeber the owner of the room
                        //looking for the room
                        int indexForRoom = -1;
                        for(int i=0;i<Server.rooms.size();i++){
                            if(Server.rooms.get(i).name.equals(roomid)){
                                indexForRoom = i;
                                break;
                            }
                        }
                        if(indexForRoom!=-1){
                            if(this.username.equals(Server.rooms.get(indexForRoom).owner)){
                                int indexForMem = -1;
                                for(int i=0;i<Server.rooms.get(indexForRoom).roomMembers.size();i++){
                                    if(Server.rooms.get(indexForRoom).roomMembers.get(i).username.equals(id)){
                                        indexForMem = i;
                                        break;
                                    }
                                }
                                //if the user doesn't exit, give feedback to client
                                if(indexForMem==-1){
                                    String content = "Identity doesn't exist..";
                                    Start message = new Start();
                                    message.content=content;
                                    Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                    Json.Object o = (Json.Object)m.marshall(message);

                                    writeMsg(o);
                                }else{
                                    //set up the timer
                                    //Server.rooms.get(indexForRoom).roomMembers.get(indexForMem).date1 = new Date();
                                    //set up the penalty minitues
                                    //Server.rooms.get(indexForRoom).roomMembers.get(indexForMem).time = time;
                                    //remember the room id
                                    //Server.rooms.get(indexForRoom).roomMembers.get(indexForMem).rRoom = Server.rooms.get(indexForRoom).name;
                                    
                                    String kickedName = Server.rooms.get(indexForRoom).roomMembers.get(indexForMem).username;
                                    //kick the user
                                    Server.rooms.get(indexForRoom).roomMembers.get(indexForMem).changeRoom("mainHall");
                                    
                                    //find the where does this kicked member in the mainHall arrayList
                                    
                                    int find = -1;
                                    for(int i=0;i<Server.rooms.get(0).roomMembers.size();i++){
                                        if(Server.rooms.get(0).roomMembers.get(i).username.equals(kickedName)){
                                            find = i;
                                            break;
                                        }
                                    }
                                    
                                    //set up the timer
                                    Server.rooms.get(0).roomMembers.get(find).date1 = new Date();
                                    //set up the penalty minitues
                                    Server.rooms.get(0).roomMembers.get(find).time = time;
                                    //remember the room id
                                    Server.rooms.get(0).roomMembers.get(find).rRoom = Server.rooms.get(indexForRoom).name;
                                    
                                    String content = "Sorry, you have been kicked out from this room.";
                                    Start message = new Start();
                                    message.content=content;
                                    Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                    Json.Object o = (Json.Object)m.marshall(message);

                                    Server.rooms.get(0).roomMembers.get(find).writeMsg(o);

                                    
                                }
                            }
                            else{
                                String content = "You don't have the access to do that.";
                                Start message = new Start();
                                message.content=content;
                                Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                Json.Object o = (Json.Object)m.marshall(message);

                                writeMsg(o);
                            }
                            //
                        }else{
                            String content = "Room doesn't exist.";
                            Start message = new Start();
                            message.content=content;
                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                            Json.Object o = (Json.Object)m.marshall(message);
                            
                            writeMsg(o);
                        }
            
        }
        
        public void deleteRoom(String roomid){
                        //find for the room index
                        int indexForRoom1 = -1;
                        for(int i=0;i<Server.rooms.size();i++){
                            if(Server.rooms.get(i).name.equals(roomid)){
                                indexForRoom1 = i;
                                break;
                            }
                        }
                        
                        if(indexForRoom1!=-1){
                            if(this.username.equals(Server.rooms.get(indexForRoom1).owner)){
                                  
                                        //move all the memebers to mainHall
                                        int number = Server.rooms.get(indexForRoom1).roomMembers.size();
                                        for(int i=0;i<number;i++){

                                            String content = "Your current room has been deleted, and you are moved to mainHall.";
                                            Start message = new Start();
                                            message.content=content;
                                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                            Json.Object o = (Json.Object)m.marshall(message);
                                            Server.rooms.get(indexForRoom1).roomMembers.get(0).writeMsg(o);

                                            Server.rooms.get(indexForRoom1).roomMembers.get(0).changeRoom("mainHall");


                                        }


                                    String content = "Delete room successfully.";
                                    Start message = new Start();
                                    message.content=content;
                                    Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                    Json.Object o = (Json.Object)m.marshall(message);

                                    //delete the room
                                    writeMsg(o);
                                    Server.rooms.remove(indexForRoom1);



                                    
                            }else{
                                
                            String content = "You don't have the access to do that.";
                            Start message = new Start();
                            message.content=content;
                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                            Json.Object o = (Json.Object)m.marshall(message);
                            
                            writeMsg(o);
                            
                            }
                        }else{
                            String content = "Room doesn't exist.";
                            Start message = new Start();
                            message.content=content;
                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                            Json.Object o = (Json.Object)m.marshall(message);
                            
                            writeMsg(o);
                        }
                        
                        
        }
        
        private void createRoom(String roomName, String owner){
            boolean right=true;
            for(int i=0;i<Server.rooms.size();i++){
                if(Server.rooms.get(i).name.equals(roomName)){
                   right = false;
                }
            }
            if(right==false){
                            String content = "Room already exist.";
                            Start message = new Start();
                            message.content=content;
                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                            Json.Object o = (Json.Object)m.marshall(message);
                            
                            writeMsg(o);                
            }else{
                ChatRoom chatRoom = new ChatRoom(roomName,owner);
                Server.rooms.add(chatRoom);
                String content = "Create room successfully.";
                Start message = new Start();
                message.content=content;
                Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                Json.Object o = (Json.Object)m.marshall(message);

                writeMsg(o);
            }
        }
        
        private ArrayList roomList(){
            ArrayList roomList = new ArrayList<String>();
            for(int i=0;i<Server.rooms.size();i++){
                roomList.add(Server.rooms.get(i).name);
            }
            return roomList;
        }
        
        public void run(){
            
            //find the index of mainHall in ArrayList
            for(int i=0;i<Server.rooms.size();i++){
                if(Server.rooms.get(i).name.equals("mainHall")){
                    roomIndex=i;
                    break;
                }
            }
            
            //all the conections(members) in the same chatroom
            room = Server.rooms.get(roomIndex).roomMembers;

            Start start = new Start();
            String info = "Connected to localhost as "+username
                    +"\n";
            for(int i=0;i<Server.rooms.size();i++){
                info = info + Server.rooms.get(i).name + ": " + 
                        Server.rooms.get(i).roomMembers.size() +" guest(s)" +"\n";
            }
            
            for(int i = 0;i<Server.rooms.size();i++){
                String members="";
                for(int j=0;j<Server.rooms.get(i).roomMembers.size();j++){
                    if(Server.rooms.get(i).roomMembers.get(j).username.equals(Server.rooms.get(i).owner)){
                        members = members + Server.rooms.get(i).roomMembers.get(j).username +"* ";
                    }else{
                    members = members + Server.rooms.get(i).roomMembers.get(j).username +" ";
                    }
                }
                info = info + Server.rooms.get(i).name + " contains: " + members+"\n";
            }
            
            start.content = info;
            
            Marshaller<Start> mm = TwoLattes.createMarshaller(Start.class);
            Json.Object oo = (Json.Object) mm.marshall(start);
                        
            writeMsg(oo);

            boolean keepGoing = true;
            while(keepGoing){
                try{                    

                    String edata = sInput.readUTF();
                  
                    String data = aes.decrypt(edata);

                    Json.Value v = Json.fromString(data);
                    
                    JSONParser parser = new JSONParser();
                    JSONObject object = (JSONObject) parser.parse(v.toString());
                    String type = (String)object.get("type");

                    if((type).equals("message")){

                        EntityMarshaller<ChatMessage> c = TwoLattes.createEntityMarshaller(ChatMessage.class);
                        ChatMessage message = c.unmarshall(v);
                        
                        String prefixS = "[" + Server.rooms.get(roomIndex).name+"]"+username + ">";

                        ChatMessage outMessage = new ChatMessage();
                        
                        outMessage.content = message.content;
                        outMessage.prefix = prefixS;
                        
                        
                        Marshaller<ChatMessage> m = TwoLattes.createMarshaller(ChatMessage.class);
                        Json.Object o = (Json.Object) m.marshall(outMessage);
                        
                        broadcast(o,room);
                        
                    }else if((type).equals("roomchange")){
   
                        EntityMarshaller<RoomChange> c = TwoLattes.createEntityMarshaller(RoomChange.class);
                        RoomChange message = c.unmarshall(v);
                        
                        String roomid = message.roomid;
                        
                                    if(this.rRoom!=""){
                                        if(!this.rRoom.equals(roomid)){
                                            //change the room for this client
                                            changeRoom(roomid);
                                            
                                            
                                        }else{
                                                        if(((new Date()).getTime()-this.date1.getTime())<(this.time*1000)){

                                                            String content = "You can't join this room again within the time limit";
                                                            Start message1 = new Start();
                                                            message1.content=content;
                                                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                                            Json.Object o = (Json.Object)m.marshall(message1);

                                                            writeMsg(o);
                                                        }
                                                        
                                                        else{
                                                                
                                                                this.rRoom = "";
                                                                
                                                                changeRoom(roomid);

                                                                RoomChange roomChange = new RoomChange();

                                                                Marshaller<RoomChange> m = TwoLattes.createMarshaller(RoomChange.class);
                                                                Json.Object o = (Json.Object) m.marshall(roomChange);

                                                                writeMsg(o);
                                                            
                                                        }
                
                                    }
                                    }else{
                        
                                            changeRoom(roomid);

                                            RoomChange roomChange = new RoomChange();

                                            Marshaller<RoomChange> m = TwoLattes.createMarshaller(RoomChange.class);
                                            Json.Object o = (Json.Object) m.marshall(roomChange);

                                            writeMsg(o);
                                    }
                    
                    }else if((type).equals("createroom")){

                        String roomOwner = this.username;
                        
                        if(!this.authen){
                            writeMsg(sendInform("You are not an authenticated user, so you don't have access to do that."));
                        }
                        else{
                            EntityMarshaller<CreateRoom> c = TwoLattes.createEntityMarshaller(CreateRoom.class);
                            CreateRoom message = c.unmarshall(v);

                            String roomid = message.roomid;

                            createRoom(roomid,roomOwner);
                        }
                        
                    }else if((type).equals("roomlist")){
                        String rooms = "";
                        ArrayList roomList = roomList();
                        
                        for(int i=0;i<roomList.size();i++){
                            rooms = rooms + roomList.get(i)+"\t";
                        }
                    }else if((type).equals("who")){
                        EntityMarshaller<Who> c = TwoLattes.createEntityMarshaller(Who.class);
                        Who who = c.unmarshall(v);

                        int room=-1;
                        for(int i=0;i<Server.rooms.size();i++){
                            if(Server.rooms.get(i).name.equals(who.roomid)){
                                room=i;
                                break;
                            }
                        }
                        
                        //can't find the room
                        if(room==-1){
                            String content = "Room doesn't exist.";
                            Start message = new Start();
                            message.content=content;
                            Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                            Json.Object o = (Json.Object)m.marshall(message);

                            this.writeMsg(o);
                        }
                        
                        //give back the room information
                        else{
                            ArrayList<String> identities = new ArrayList<String>();

                            for(int i=0;i<Server.rooms.get(room).roomMembers.size();i++){
                                identities.add(Server.rooms.get(room).roomMembers.get(i).username);
                            }
                            
                            RoomContents roomlist = new RoomContents();
                            roomlist.identities = identities;
                            
                            roomlist.owner = Server.rooms.get(room).owner;
                            
                            roomlist.roomid = Server.rooms.get(room).name;
                            
                            Marshaller<RoomContents> m = TwoLattes.createMarshaller(RoomContents.class);
                            Json.Object o = (Json.Object)m.marshall(roomlist);
                            
                            writeMsg(o);
                        }
                    }else if(type.equals("list")){
                        
                        RoomList roomlist = new RoomList();
                        
                        ArrayList<RoomContents> rcs = new ArrayList<RoomContents>(); 
                        
                        for(int i=0;i<Server.rooms.size();i++){
                            ChatRoom cr = Server.rooms.get(i);
                            
                            ArrayList<String> members = new ArrayList<String>();
                            
                            for(int j=0;j<cr.roomMembers.size();j++){
                                
                                members.add(cr.roomMembers.get(j).username);
                                
                            }
                            
                        String owner = cr.owner;
                        String id = cr.name;
                        
                        RoomContents rc = new RoomContents();
                        rc.identities = members;
                        rc.owner = owner;
                        rc.roomid = id;
                        
                        rcs.add(rc);
                            
                        }
                        
                        roomlist.rooms = rcs;
                        
                        Marshaller<RoomList> m = TwoLattes.createMarshaller(RoomList.class);
                        Json.Object o = (Json.Object)m.marshall(roomlist);
                        
                        writeMsg(o);
                    }
                    else if(type.equals("identitychange")){

                        EntityMarshaller<IdentityChange> c = TwoLattes.createEntityMarshaller(IdentityChange.class);
                        IdentityChange ic = c.unmarshall(v);
                        
                        String newIdentity = ic.identity;
                        boolean isTaken = false;


                        
                        for(int j=0;j<Server.rooms.size();j++){
                            
                                    for(int i=0;i<Server.rooms.get(j).roomMembers.size();i++){
                                        if(Server.rooms.get(j).roomMembers.get(i).username.equals(newIdentity)){
                                            isTaken=true;
                                            break;
                                        }
                                    }        
                        }
                        
                  if(!isTaken){
                                Pattern pattern = Pattern.compile("^[A-Za-z][A-Za-z0-9]+$");

                                    if (!pattern.matcher(newIdentity).matches()||newIdentity.length()<3||newIdentity.length()>13) {

                                                        String content = "WrongName!"
                                                                + "\nYour new identity :"
                                                                + "\n1. must be at least 3 characters and no more than 16 characters."
                                                                + "\n2. starts wit an upper or lower case character. "
                                                                + "\n3. has upper and lower case characters, and digits only.";
                                                        Start message = new Start();
                                                        message.content=content;
                                                        Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                                        Json.Object o = (Json.Object)m.marshall(message);

                                                        this.writeMsg(o);
                                    }else{
                                        //change the identity to the one user asked
                                        this.username = newIdentity;
                                        
                                        //give feedback
                                        String content = "Your identity has been changed!";
                                        Start message = new Start();
                                        message.content=content;
                                        Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                        Json.Object o = (Json.Object)m.marshall(message);

                                        this.writeMsg(o);
                                    }                        

                        
               }
                        else{
                                        String content = "This identity has been taken!";
                                        Start message = new Start();
                                        message.content=content;
                                        Marshaller<Start> m = TwoLattes.createMarshaller(Start.class);
                                        Json.Object o = (Json.Object)m.marshall(message);

                                        this.writeMsg(o);
                        }
                        

              

                    }else if(type.equals("kick")){
                        EntityMarshaller<Kick> c = TwoLattes.createEntityMarshaller(Kick.class);
                        Kick kick = c.unmarshall(v);
                        
                        String roomid = kick.roomid;
                        String identity = kick.identity;
                        int time = kick.time;
                        
                        kick(roomid,identity,time);
    
                    }else if(type.equals("delete")){
                        EntityMarshaller<Delete> c = TwoLattes.createEntityMarshaller(Delete.class);
                        Delete delete = c.unmarshall(v);
                        
                        if(!this.authen){
                            writeMsg(sendInform("You are not an authenticated user, so you don't have access to do that."));
                        }else{
//                            RequirePassword rp = new RequirePassword();
//                            Marshaller<RequirePassword> m = TwoLattes.createEntityMarshaller(RequirePassword.class);
//                            Json.Object o = (Json.Object)m.marshall(rp);
//                            writeMsg(o);
                            String roomid = delete.roomid;
                            deleteRoom(roomid);
                        }
                        
                        
                    }else if(type.equals("quit")){
                        
                        
                                                            
                                                            String content = this.username+ " has moved out from this room.";
                                                            Quit quit = new Quit();
                                                            quit.content=content;
                                                            Marshaller<Quit> m = TwoLattes.createMarshaller(Quit.class);
                                                            Json.Object o = (Json.Object)m.marshall(quit);
                                                            
                                                            writeMsg(o);
                                                            
                                        //broadcast the quit message                    
                                        String content1 = this.username+ " has moved out from this room.";
                                        Start message1 = new Start();
                                        message1.content=content;
                                        Marshaller<Start> m1 = TwoLattes.createMarshaller(Start.class);
                                        Json.Object o1 = (Json.Object)m1.marshall(message1);

                                        broadcast(o1,this.room);
                                                      
                                        room.remove(this);
                                        close();
                        
                        
                    }
                    else if(type.equals("authenticate")){
                        EntityMarshaller<Authenticate> au = TwoLattes.createEntityMarshaller(Authenticate.class);
                        Authenticate cau = au.unmarshall(v);
                        
                        String newId = cau.newIdentity;
                        String password = cau.password;
                        
                        boolean checkName = true;
                        
                        for(ChatRoom cr:Server.rooms){
                            for(Connection c:cr.roomMembers){
                                if(c.username.equals(newId)){
                                    checkName = false;
                                }
                            }
                        }
                        
                        
                        if(checkName){
                            this.password = password;
                            this.authen = true;
                            this.username = newId;
                            Server.authUsers.put(newId, password);
                            writeMsg(sendInform("You have been authenticated, please remember your password."));
                            
                        }else{
                            writeMsg(sendInform("this identity has been taken."));
                        }
                    }
                    
                    
                    
                }catch(IOException e){} catch (ParseException ex) {
                    Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }//finish of run
        
       private void close() {
			// try to close the connection
			try {
				if(sOutput != null) sOutput.close();
			}
			catch(Exception e) {}
			try {
				if(sInput != null) sInput.close();
			}
			catch(Exception e) {};
			try {
				if(socket != null) socket.close();
			}
			catch (Exception e) {}
		}
        
        
    }


@Entity
class ChatMessage{
    @Value 
    public String prefix;
    @Value
    public String type = "message";
    @Value 
    public String content;
    @Value
    public String id;
}

@Entity
class RoomChange{
    @Value
    public String type = "roomchange";
    @Value
    public String identity;
    @Value
    public String former;
    @Value
    public String roomid;
    @Value
    public String inform = "Change room successfully";
}

@Entity
class CreateRoom{
    @Value
    public String type = "createroom";
    @Value
    public String roomid;
    @Value
    public String inform = "Create room successfully";
}

@Entity
class Who{
    @Value
    public String type = "who";
    @Value
    public String roomid;
}

@Entity
class RoomContents{
    @Value
    public String type = "roomcontents";
    @Value
    public String roomid;
    @Value
    public ArrayList<String> identities; 
    @Value
    public String owner;
    @Value int numberOfMembers;
}

@Entity
class RoomList{
    @Value
    public String type = "roomlist";
    @Value
    public ArrayList<RoomContents> rooms;
    @Value
    public String inform = "Create room successfully";
    
}

@Entity
class List{
    @Value
    public String type = "list";
}

@Entity
class NewIdentity{
    @Value
    public String type = "newidentity";
    @Value
    public String former;
    @Value 
    public String identity;
}

@Entity
class Start{
    @Value
    public String type = "start";
    @Value
    public String content;
}

@Entity
class IdentityChange{
    @Value
    public String type="identitychange";
    @Value
    public String identity;
    @Value
    public String infor = "Change identity successfully!";
}

@Entity
class Kick{
    @Value
    public String type = "kick";
    @Value
    public String roomid;
    @Value
    public int time;
    @Value
    public String identity;
}

@Entity
class Delete{
    @Value
    public String type = "delete";
    @Value
    public String roomid;
}

@Entity
class Quit{
    @Value 
    public String type = "quit";
    @Value
    public String content;
}

@Entity
class Authenticate{
    @Value
    public String type = "authenticate";
    @Value
    public String newIdentity;
    @Value
    public String password;
}

@Entity
class RequirePassword{
    @Value
    public String type = "rp";
   
}