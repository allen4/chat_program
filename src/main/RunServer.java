/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;
import java.security.GeneralSecurityException;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;
/**
 *
 * @author s3931660
 */
public class RunServer {
    
    @Option(name="-p",usage="server port number")
    public String port="";
    
        public static void main(String[] args) throws GeneralSecurityException{
            RunServer rs = new RunServer();
            CmdLineParser parser = new CmdLineParser(rs);

       try {
                    parser.parseArgument(args);
                    rs.run();
            } catch (CmdLineException e) {
                // handling of wrong arguments
                System.err.println(e.getMessage());
                parser.printUsage(System.err);
            }
        }
        
    public void run() throws GeneralSecurityException{
        
       int portNumber = 4444;
       
       if(!port.isEmpty()){
            portNumber = Integer.parseInt(port);
        }

        Server chatRooms = new Server(portNumber);
        
        chatRooms.creatRoom("room1");
        chatRooms.creatRoom("comp90015");

        chatRooms.start();
    }
}
