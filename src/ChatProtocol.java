import java.net.*;
import java.io.*;

public class ChatProtocol {
    private static final int WAITING = 0;
    private static final int CONNECTED = 1;

    private int state = WAITING;

    public String processInput(String theInput)
    {
        String theOutput = null;

        if (state == WAITING)
        {
            theOutput = "Connection established Harambe";
            state = CONNECTED;
        }
        else if (state == CONNECTED)
        {
            // if (theInput.equalsIgnoreCase("Who's there?")) {
            //     theOutput = clues[currentJoke];
            //     state = SENTCLUE;
            // } else {
            //     theOutput = "You're supposed to say \"Who's there?\"! " +
			//     "Try again. Knock! Knock!";
            // }
        }
        return theOutput;
    }
}
