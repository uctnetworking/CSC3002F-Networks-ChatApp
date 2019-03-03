import java.net.*;
import java.io.*;
import java.util.*;

/**
Client Handler Class to manage the interactions between an individual client and the server on a seperate thread on the server,
*/
public class ClientHandlerThread extends Thread
{
    private Socket socket = null; //reference to the client's socket
    private String clientName;
    private InputStream inStream;
    private OutputStream outStream;
    private boolean online;

    public ClientHandlerThread(Socket socket) throws IOException
    {
        super("ChatServer"); //construct the thread
        this.socket = socket;
        this.inStream = socket.getInputStream();
        this.outStream = socket.getOutputStream();
        this.online = false;
        this.clientName = "NAME NOT YET ASSIGNED"; // Default value that will be changed as soon as the user enters their name when prompted
    }

    /**
    Runs when the thread is created and handles all the client-server interactions in its logic.
    */
    public void run()
    {
        try
        (
            //try create the output and input stream readers with the input and output streams
            PrintWriter out = new PrintWriter(outStream, true); //write out to client
            Scanner in = new Scanner(new InputStreamReader(inStream)); //receive in from client
        )
        {
            ChatProtocol cp = new ChatProtocol(); // controls the protocol that client-server interactions must follow
            String askForName = cp.processInput(null);
            out.println(askForName); // print to client screen a request for their name

            String attemptedName = in.nextLine(); // recieve the inputted name from the client
            String protocolResponse = cp.processInput(attemptedName); // response from the server regarding the validity of the name inputted
            out.println(protocolResponse); // tells the client if the name is valid, and if not then what the error is
            while (protocolResponse.equals(ProtocolResponses.NAME_NOT_UNIQUE) || protocolResponse.equals(ProtocolResponses.NAME_TOO_LONG) || protocolResponse.equals(ProtocolResponses.NO_NAME_ENTERED))
            {
                attemptedName = in.nextLine(); // get a new name from the client
                protocolResponse = cp.processInput(attemptedName);
                out.println(protocolResponse);
            }
            // if we get here, the name has been accepted and the user should be made ONLINE
            this.clientName = attemptedName;
            this.online = true;
            out.println("Thanks for your name: " + this.clientName);
            System.out.println(this.clientName + " is now online."); // Write on server terminal just for monitoring purposes

            // TO DO: Now we need a while(true) loop to constantly wait for a message that needs to be routed

            while (online)
            {
                String received = in.nextLine();
                protocolResponse = cp.processInput(received);
                if(!protocolResponse.equals(ProtocolResponses.MESSAGE_FORMAT_SUCCESS))
                {
                    out.println(protocolResponse);
                }
                else
                {
                    Scanner scLine =  new Scanner(received).useDelimiter("#");
                    String messageType = scLine.next(); // MESSAGE or FILE
                    String recipientName = scLine.next(); // the person to send to
                    String message = scLine.next();// the actual message
                    for (ClientHandlerThread c : ChatServer.clientHandlers)
                    {
                        if(c.getClientName().equalsIgnoreCase(recipientName))
                        {
                            PrintWriter outToRecipient = new PrintWriter(c.getSocket().getOutputStream(), true);
                            outToRecipient.println(message);
                        }
                    }
                }
            }
            socket.close();
            in.close();
            out.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getClientName()
    {
        return clientName;
    }

    public Socket getSocket()
    {
        return socket;
    }
}
