
package main;

import java.util.ArrayList;


public class ChatRoom {
    public ArrayList<Connection> roomMembers;
    public String name;
    public String owner;
    
    public ChatRoom(String name){
        roomMembers = new ArrayList<Connection>();
        this.name = name;
    }
    
    public ChatRoom(String name,String owner){
        roomMembers = new ArrayList<Connection>();
        this.name = name;
        this.owner = owner;
    }
    
    public String getName(){
        return this.name;
    }
    
    public void addMember(Connection c){
        roomMembers.add(c);
    }
    
    public void removeMember(String name){
        int index = -1;
        for(int i=0;i<roomMembers.size();i++){
            if(roomMembers.get(i).username.equals(name)){
                index = i;
                break;
            }
        }
        if(index!=-1){
             roomMembers.remove(index);
        }else{
            System.out.println("Can't find member in this room!");
        }      
    }
    
    public Connection findMember(String name){
        Connection result = null;
     
        for(Connection c : this.roomMembers){
            if(c.username.equals(name)){
                result=c;
                break;
            }
        }
        return result;      
    }
    
    public boolean ifExist(String name){
        
        boolean result = false;
        for(Connection c:this.roomMembers){
            if(c.username.equals(name)){
                result = true;
            }
        }
        return result;    
    }
    
}
