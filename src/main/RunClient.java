/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import com.twolattes.json.Json;
import com.twolattes.json.Marshaller;
import com.twolattes.json.TwoLattes;
import java.io.IOException;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import org.kohsuke.args4j.Argument;

import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

/**
 *
 * @author Administrator
 */
public class RunClient {
    
    @Option(name="-p",usage="server port number")
    public String port="";
    @Argument
    public String host="";
    
    public static void main(String[] args) throws NoSuchAlgorithmException, IOException, GeneralSecurityException{
       
        RunClient rc = new RunClient();
        
        CmdLineParser parser = new CmdLineParser(rc);
        
   try {
                parser.parseArgument(args);
                rc.run();
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
        }catch(NullPointerException e)
        {
            e.getMessage();
            System.out.println("hhhhhhhhhhhhhhhhhhhhhhhhhh");
            System.out.println("Server not running properly.");}
    }
        
    public void run() throws NoSuchAlgorithmException, IOException, GeneralSecurityException{
        int portNumber;
        
        if(!port.isEmpty()){
            portNumber = Integer.parseInt(port);
        }else{portNumber = 4444;}
        

        
        //int portNumber = 4444;
        String serverAddress = host;
        
        
        Client client = new Client(serverAddress,portNumber);
        
        try{
        client.start();
        }
        catch(NullPointerException e){System.out.println("Server not running properly.");}
        
        Scanner s = new Scanner(System.in);

        boolean flag = true;
        while(flag){
            try{
            String input = s.nextLine();
            
            //parse the input
            
            
            String delims = "[ ]";
            String[] tokens = input.split(delims);
            
            try{

                            if(tokens[0].charAt(0)=='#'){

                                String cmd = tokens[0];
                                String command = "";

                                for(int n=1;n<tokens[0].length();n++){
                                   command = command + cmd.charAt(n);
                                }


                                int type=100;

                                if(command.equals("identitychange")){
                                    try{
                                        IdentityChange ic = new IdentityChange();

                                        ic.identity = tokens[1];

                                        Marshaller<IdentityChange> m = TwoLattes.createMarshaller(IdentityChange.class);
                                        Json.Object o = (Json.Object) m.marshall(ic);

                                        client.sendMessage(o);                                    
                                    }catch(ArrayIndexOutOfBoundsException e){
                                        System.out.println("Invalid command Parameters\nUsage is: #identitychange [new identity]");
                                    }
                                    


                                }else
                                if(command.equals("join")){
                                    try{
                                    RoomChange message = new RoomChange();

                                    message.type = "roomchange";
                                    message.roomid = tokens[1];

                                    Marshaller<RoomChange> m = TwoLattes.createMarshaller(RoomChange.class);
                                    Json.Object o = (Json.Object) m.marshall(message);
                                    client.sendMessage(o);                                        
                                    }catch(ArrayIndexOutOfBoundsException e){
                                        System.out.println("Invalid command Parameters\nUsage is: #join [room id]");                                        
                                    }


                                }else
                                if(command.equals("who")){
                                    Who who = new Who();
                                    
                                            try{
                                            who.roomid = tokens[1];

                                            Marshaller<Who> m = TwoLattes.createEntityMarshaller(Who.class);
                                            Json.Object o = (Json.Object)m.marshall(who);
                                            client.sendMessage(o);

                                            }catch(ArrayIndexOutOfBoundsException e){
                                                System.out.println("Invalid command Parameters\nUsage is: #who [roomname]");
                                            }


                                }else
                                if(command.equals("list")){

                                            List list = new List();
                                            Marshaller<List> m = TwoLattes.createEntityMarshaller(List.class);
                                            Json.Object o = (Json.Object)m.marshall(list);

                                            client.sendMessage(o);
                                            
                                }else
                                if(command.equals("createroom")){
                                                
                                            
                                                CreateRoom cr = new CreateRoom();
                                                try{
                                                    cr.roomid = tokens[1];
                                                    
                                                    Marshaller<CreateRoom> m = TwoLattes.createMarshaller(CreateRoom.class);
                                                    Json.Object o = (Json.Object)m.marshall(cr);
                                                    client.sendMessage(o);
                                                
                                                }catch(ArrayIndexOutOfBoundsException e){
                                                    System.out.println("Invalid command Parameters\nUsage is: #createroom [roomname]");
                                                }




                                    }else
                                if(command.equals("kick")){
                                    
                                                try{
                                                Kick kick = new Kick();
                                                String roomid = tokens[1];
                                                String identity = tokens[2];
                                                int time = Integer.parseInt(tokens[3]);

                                                kick.roomid = roomid;
                                                kick.identity = identity;
                                                kick.time = time;

                                                Marshaller<Kick> m = TwoLattes.createMarshaller(Kick.class);
                                                Json.Object o = (Json.Object)m.marshall(kick);
                                                client.sendMessage(o);   
                                                
                                                }catch(ArrayIndexOutOfBoundsException e){
                                                System.out.println("Invalid command Parameters\nUsage is: #kick [roomname] [username] [kick time]");                 
                                                }
                 

                                }else
                                if(command.equals("delete")){
                                    try{
                                                Delete delete = new Delete();
                                                delete.roomid = tokens[1];

                                                Marshaller<Delete> m = TwoLattes.createMarshaller(Delete.class);
                                                Json.Object o = (Json.Object)m.marshall(delete);
                                                client.sendMessage(o);   
                                                
                                    }catch(ArrayIndexOutOfBoundsException e){
                                        System.out.println("Invalid command Parameters\nUsage is: #delete [roomname]");   
                                    }


                                }else
                                if(command.equals("quit")){
                                    
                                    Quit rc = new Quit();
                                    

                                    Marshaller<Quit> m = TwoLattes.createMarshaller(Quit.class);
                                    Json.Object o = (Json.Object)m.marshall(rc);

                                    client.sendMessage(o);  
                                    break;

                                }else
                                if(command.equals("authenticate")){
                                    try{
                                        String newId = tokens[1];
                                        HashFunction hf = new HashFunction(tokens[2]);
                                        String password = hf.generateHash();
                                        
                                        Authenticate au = new Authenticate();
                                        au.newIdentity = newId;
                                        au.password = password;
                                        
                                        Marshaller<Authenticate> m = TwoLattes.createMarshaller(Authenticate.class);
                                        Json.Object o = (Json.Object)m.marshall(au);
                                        
                                        client.sendMessage(o);
                                        
                                    }
                                    catch(ArrayIndexOutOfBoundsException e){
                                        System.out.println("Invalid command Parameters\nUsage is: #authenticate [new identity] [password]");
                                    }
                                }
                                else
                                {System.out.println("Wrong command\n"+"Usage is:"
                                        + "\n \t#identitychange [new identity]"
                                        + "\n \t#authenticate [new identity] [password]"
                                        + "\n\t#join [roomid]"
                                        + "\n\t#who [roomid]"
                                        + "\n\t#list"
                                        + "\n\t#createroom [roomid]"
                                        + "\n\t#kick [roomid] [username] [kick time]"
                                        + "\n\t#delete [roomid]"
                                        + "\n\t#quit");}


                            }else{
                            ChatMessage message = new ChatMessage();
                            message.content = input;
                            message.type = "message";

                            Marshaller<ChatMessage> m = TwoLattes.createMarshaller(ChatMessage.class);
                            Json.Object o = (Json.Object) m.marshall(message);
                            client.sendMessage(o);
                            }

            }catch(StringIndexOutOfBoundsException e){
                System.out.println();
            }
            
            }catch(NoSuchElementException e){
                                    Quit rc = new Quit();

                                    Marshaller<Quit> m = TwoLattes.createMarshaller(Quit.class);
                                    Json.Object o = (Json.Object)m.marshall(rc);

                                    client.sendMessage(o);  
                                    break;
            }

        }//end of while
    }
}
