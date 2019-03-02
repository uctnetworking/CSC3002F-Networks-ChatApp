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

    public ClientHandlerThread(Socket socket) throws IOException
    {
        super("ChatServer"); //construct the thread
        this.socket = socket;
        this.inStream = socket.getInputStream();
        this.outStream = socket.getOutputStream();
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
            String inputLine, outputLine;
            ChatProtocol cp = new ChatProtocol();
            outputLine = cp.processInput(null);
            out.println(outputLine);

            while ((inputLine = in.nextLine()) != null)
            {
                outputLine = cp.processInput(inputLine);
                out.println(outputLine);
                if (outputLine.equals("Bye"))
                    break;
            }
            socket.close();
            in.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getClientName()
    {
        return this.clientName;
    }
}
