import java.net.*;
import java.io.*;
import java.util.*;

/**
*/
public class ChatServerThread extends Thread {
    private Socket socket = null; //reference to the client's socket

    public ChatServerThread(Socket socket) {
        super("ChatServer"); //construct the thread
        this.socket = socket;
    }

    /**
    */
    public void run() {

        try ( //try create the output and input stream readers with the input and output streams
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true); //write out to client
            Scanner in = new Scanner( //receive in from client
                new InputStreamReader(
                    socket.getInputStream()));
        ) {
            String inputLine, outputLine;
            ChatProtocol cp = new ChatProtocol();
            outputLine = cp.processInput(null);
            out.println(outputLine);

            while ((inputLine = in.nextLine()) != null) {
                outputLine = cp.processInput(inputLine);
                out.println(outputLine);
                if (outputLine.equals("Bye"))
                    break;
            }
            socket.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
