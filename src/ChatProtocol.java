import java.net.*;
import java.io.*;
import java.util.*;

public class ChatProtocol
{
    private static final String WAITING = "Waiting";
    private static final String VALIDATE_NAME = "ValidateName";
    private static final String ONLINE = "Online";

    private String state = WAITING;

    public String processInput(String theInput)
    {
        if (state == WAITING) //waiting for a client to connect
        {
            //a client has connected
            state = VALIDATE_NAME;
            return ProtocolResponses.ENTER_YOUR_NAME;
        }
        else if (state == VALIDATE_NAME)
        {
            String attemptedName = theInput;

            if(attemptedName.length() == 0) //name not entered
            {
                return ProtocolResponses.NO_NAME_ENTERED;
            }

            if(attemptedName.length() > 24) //if name too long (24 chars)
            {
                return ProtocolResponses.NAME_TOO_LONG;
            }

            for (ClientHandlerThread c : ChatServer.clientHandlers)
            {
                if(c.getClientName().equalsIgnoreCase(attemptedName)) //compare to current names check if unique
                {
                    return ProtocolResponses.NAME_NOT_UNIQUE; //if name not unique, don't progress to next state - and ask client to try again
                }
            }

            // if we get here, the name is valid.
            state = ONLINE; //Valid name, change the client state to ONLINE
            return ProtocolResponses.NAME_SUCCESS; //if valid name return "NAME_SUCCESS" to tell the ClientHandlerThread to save the name
        }
        else if (state == ONLINE)
        {
            //input will be in format: <MESSAGE or FILE#<name of recipient>#<message or file name>

            int countHashes = 0;
            for(int i = 0; i < theInput.length(); i++) // counts the number of hashes in the input
            {
                if (theInput.charAt(i) == '#')
                {
                    countHashes++;
                }
            }
            if(countHashes != 2) // checks there are two hashes in the input so that the message format is correct
            {
                return ProtocolResponses.INVALID_MESSAGE_FORMAT;
            }

            Scanner scLine =  new Scanner(theInput).useDelimiter("#");
            String messageType = scLine.next(); // MESSAGE or FILE
            String recipientName = scLine.next(); // the person to send to
            String message = scLine.next();// the actual message

            if(messageType == "MESSAGE")
            {
                for (ClientHandlerThread c : ChatServer.clientHandlers)
                {
                    if(c.getClientName().equalsIgnoreCase(recipientName))
                    {
                        return ProtocolResponses.MESSAGE_FORMAT_SUCCESS; // success if recipient name is amongst online users
                    }
                }
                // if we get here, the recipient name is not amongst the online users
                return ProtocolResponses.INVALID_RECIPIENT_NAME;
            }
            else if(messageType == "FILE")
            {
                // deal with file stuff later
            }
            else
            {
                return ProtocolResponses.INVALID_MESSAGE_TYPE; // not one of MESSAGE or FILE
            }
        }
        return null;
    }
}
