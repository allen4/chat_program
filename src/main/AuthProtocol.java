/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

/**
 *
 * @author s3931660
 */
public class AuthProtocol {
    private static final int WAITTING = 0;
    private static final int SENTUSERNAME = 1;
    private static final int SENTPASSWORD = 2;
    private static final int uEND = 3;
    private static final int gEND = 4;
    private static final int ASK=100;
    
    public int state = ASK;
    public String username = "";
    
    public String processInput(String theInput){
        String theOutput = null;
        
        if(state==ASK){
            theOutput = "Are you an authenticated user? [Y/N] ";
            state = WAITTING;
        }
        else if(state==WAITTING){
            if(theInput.equals("Y")||theInput.equals("y")){
                 theOutput = "Please input your user name: ";
                 state = SENTUSERNAME;
            }else{
                theOutput = "You are logging on as a guest.";
                state = gEND;     
            }
           
        }else if(state==SENTUSERNAME){
            if(Server.authUsers.containsKey(theInput)){
                this.username = theInput;
                theOutput = "Please input your password: ";
                state = SENTPASSWORD;                
            }else{
                theOutput = "Sorry, this user name doesn't exsit" + 
                        "\nPlease input you user name: ";
            }
        }else if(state==SENTPASSWORD){
            if((Server.authUsers.get(this.username)).equals(theInput)){
                theOutput = "Authenticate Successfully!";
                state = uEND;
            }else{
                theOutput = "Wrong password, please try it again: ";
                state = SENTPASSWORD;
            }
        }
        
        return theOutput;
    }
}
