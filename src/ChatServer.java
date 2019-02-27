import java.net.*;
import java.io.*;

/**
Server class to assign a port to the server and continuously listen for new connections and spawn
a new thread in the case that a client requests a connection with the server
*/
public class ChatServer
{
    /**
    Main method to carry out the server functionality
    @param String[]args - port number specified to host the server. Port numbers from 49152 to 65535 can be used
    @throws IOException - throws an exception in the case that an IO error occurs
    */
    public static void main(String[] args) throws IOException
    {
        if (args.length != 1) //Check that one argument with the port number was passed
        {
            System.err.println("Use Syntax: java Server <port number>");
            System.exit(1); //not System.exit(0) since an error occured
        }

        int portNumber = Integer.parseInt(args[0]); //take the port number from the command line arguments
        boolean serverListening = true;

        try (ServerSocket serverSocket = new ServerSocket(portNumber))
        {
            System.out.println("Server is running...");
            while (serverListening)
            {
	            new ChatServerThread(serverSocket.accept()).start(); //accept() method blocks the thread until connection made
	        }
	    } catch (IOException e)
        {
            System.err.println("Failed to listen on port " + portNumber + "\nPlease try a different port...");
            System.exit(2); //not System.exit(0) since an error occured
        }
    }
}
