//author Yifeng Zhu

package main;
import java.security.GeneralSecurityException;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.CmdLineException;

public class RunServer {
    
    //parse the command line arguments
    @Option(name="-p",usage="server port number")
    public String port="";
    
    public static void main(String[] args) throws GeneralSecurityException, CmdLineException {
        new RunServer().doMain(args);
    }
    
    public void doMain(String[] args) throws CmdLineException, GeneralSecurityException{
        CmdLineParser parser = new CmdLineParser(this);
        parser.parseArgument(args);
        this.run(port);
    }
    
    
    // initiate the server program
    public void run(String port) throws GeneralSecurityException {

        int portNumber = 4444;

        if (port!="") {
            portNumber = Integer.parseInt(port);
        }

        Server chatRooms = new Server(portNumber);

        chatRooms.creatRoom("room1");
        chatRooms.creatRoom("comp90015");

        chatRooms.start();
    }
}


