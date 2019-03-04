import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class ChatGUI extends JFrame implements ActionListener
{
    private static final int WIDTH = 650;
    private static final int HEIGHT = 400;
    private static final int MEDIUM_STRUT = 25;
    private static final int LARGE_STRUT = 30;

    private static JTextArea txaDisplayChat;
    private static JComboBox <String> cmbOptions;
    private static JTextField txfMessage;
    private static JButton btnSend;
    private static JButton btnTransferFile;
    private static JButton btnLogout;

    private static ArrayList<String> chatHistories = new ArrayList<String>();
    private final static int serverPort = 60000;
    private static PrintWriter out;
    private static Scanner in;
    private static String name;
    private static Socket socket;

    /** Main method for running the application.*/
    public static void main(String [] args) throws UnknownHostException, IOException
    {
        ChatGUI gui = new ChatGUI();
        gui.setVisible(true);

        // establish the connection
        socket = new Socket("localhost", serverPort);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new Scanner(socket.getInputStream());

        provideServerWithValidName(); //note that this reads from and writes to the socket
        gui.setTitle(name + "'s Chats");

        String users = in.nextLine();
        populateOnlineUsers(users);

        boolean loggedIn = true;
        while (loggedIn)
        {
            // read the message sent to this client
            String msg =  in.nextLine(); //comes in format [Name]: [Message]
            System.out.println("Message from server: " + msg);
            if(msg.equalsIgnoreCase(ProtocolResponses.NOTIFY_LOGOUT))
            {
                disableGUI();
                socket.close();
                loggedIn = false;
            }
            else if(msg.startsWith("Online Users:"))
            {
                updateOnlineUsers(msg);
            }
            else if(msg.startsWith("FILE")) //not currently supported
            {
                processFileFromServer(msg);
            }
            else
            {
                saveAndDisplayMessage(msg);
            }
        }
    }

    /**
    * Constructor that sets up the GUI.
    */
    public ChatGUI()
    {
        super("Chat App");
        setSize(WIDTH,HEIGHT);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.BLACK);

        Box usersBox = new Box(BoxLayout.LINE_AXIS);
        usersBox.add(Box.createHorizontalStrut(MEDIUM_STRUT));
        JLabel usersLabel = new JLabel("Choose an online user to chat to:");
        usersLabel.setForeground(Color.WHITE);
        usersBox.add(usersLabel);
        cmbOptions = new JComboBox<>();
        cmbOptions.setActionCommand("Chat Selection");
        cmbOptions.addActionListener(this);
        usersBox.add(cmbOptions);
        btnLogout = new JButton("Logout");
        btnLogout.setActionCommand("Logout");
        btnLogout.addActionListener(this);
        usersBox.add(btnLogout);
        usersBox.add(Box.createHorizontalStrut(MEDIUM_STRUT));
        add(usersBox, BorderLayout.NORTH);

        Box chatBox = new Box(BoxLayout.LINE_AXIS);
        chatBox.add(Box.createHorizontalStrut(MEDIUM_STRUT));
        txaDisplayChat = new JTextArea();
        txaDisplayChat.setEditable(false);
        txaDisplayChat.setLineWrap(true);
        JScrollPane scroller = new JScrollPane(txaDisplayChat);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatBox.add(scroller);
        chatBox.add(Box.createHorizontalStrut(LARGE_STRUT));
        add(chatBox, BorderLayout.CENTER);

        Box messageBox = new Box(BoxLayout.LINE_AXIS);
        messageBox.add(Box.createHorizontalStrut(MEDIUM_STRUT));
        txfMessage = new JTextField();
        messageBox.add(txfMessage);
        messageBox.add(Box.createHorizontalStrut(MEDIUM_STRUT));
        btnSend = new JButton("Send Message");
        btnSend.setActionCommand("Send");
        btnSend.addActionListener(this);
        messageBox.add(btnSend);
        messageBox.add(Box.createHorizontalStrut(MEDIUM_STRUT));
        btnTransferFile = new JButton("Attach"); // Transfer file
        btnTransferFile.setActionCommand("Transfer");
        btnTransferFile.addActionListener(this);
        messageBox.add(btnTransferFile);
        messageBox.add(Box.createHorizontalStrut(LARGE_STRUT));
        add(messageBox, BorderLayout.SOUTH);
    }

    /**
    * @param e An ActionEvent object that represents an item from the combo box being selected.
    */
    public void actionPerformed(ActionEvent e)
    {
        if(e.getActionCommand().equals("Send"))//send message button pressed
        {
            sendMessage();
        }
        else if(e.getActionCommand().equals("Chat Selection"))
        {
            displayCorrectChatHistory();
        }
        else if(e.getActionCommand().equals("Logout"))
        {
            sendLogoutRequest();
        }
        else if(e.getActionCommand().equals("Transfer"))
        {
            sendFile();
        }
    }

    private static void provideServerWithValidName()
    {
        name = JOptionPane.showInputDialog(in.nextLine()); // This is a request from the server for the client's name
        out.println(name);
        String serverResponse = in.nextLine();
        while(!serverResponse.equals(ProtocolResponses.NAME_SUCCESS))
        {
            name = JOptionPane.showInputDialog(serverResponse); // Asks the client to enter another name
            out.println(name);
            serverResponse = in.nextLine();
        }
        JOptionPane.showMessageDialog(null, in.nextLine()); // This is a thanks from the server for a correct name
    }

    private static void populateOnlineUsers(String users)
    {
        Scanner scUsers = new Scanner(users.substring(users.indexOf(":")+2)).useDelimiter("#");
        while(scUsers.hasNext())
        {
            String user = scUsers.next();
            if(!user.equalsIgnoreCase(name))
            {
                cmbOptions.addItem(user);
                chatHistories.add(user + "#");
            }
        }
        scUsers.close();
        if (cmbOptions.getItemCount() == 0)
        {
            JOptionPane.showMessageDialog(null,"No other users online :(");
            cmbOptions.setEnabled(false);
        }
    }

    private static void sendMessage()
    {
        String recipient = cmbOptions.getSelectedItem().toString();
        String message = txfMessage.getText();
        if(!message.equals(""))
        {
            for(int i=0; i<chatHistories.size(); i++)
            {
                if(chatHistories.get(i).startsWith(recipient))
                {
                    String chatHistory = chatHistories.get(i);
                    chatHistories.set(i, chatHistory += "You: " + message + "\n");
                    txaDisplayChat.setText(chatHistories.get(i));
                    break;
                }
            }
            out.println(matchProtocol("MESSAGE", recipient, message));
            txfMessage.setText("");
        }
    }

    /**
    Helper method that puts the protocol, recipient name and message into the format expected by the server.
    */
    private static String matchProtocol(String protocol, String recipient, String message)
    {
        return protocol + "#" + recipient + "#" + message;
    }

    private static void displayCorrectChatHistory() //still broken
    {
        // if(cmbOptions.getItemCount() > 0)
        // {
        //     String selectedChat = cmbOptions.getSelectedItem().toString();
        //     for(String userHistory : chatHistories)
        //     {
        //         if(userHistory.startsWith(selectedChat))
        //         {
        //             txaDisplayChat.setText(userHistory.substring(userHistory.indexOf('#')+1));
        //         }
        //     }
        // }
    }

    private static void sendLogoutRequest()
    {
        int sure = JOptionPane.showConfirmDialog(null,"Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
        if(sure == JOptionPane.YES_OPTION)
        {
            out.println(ProtocolRequests.REQUEST_LOGOUT);
        }
    }

    private static void updateOnlineUsers(String msg)
    {
        cmbOptions.removeAllItems();
        Scanner scUsers = new Scanner(msg.substring(msg.indexOf(":")+2)).useDelimiter("#");
        while(scUsers.hasNext())
        {
            String user = scUsers.next();
            if(!user.equalsIgnoreCase(name))
            {
                cmbOptions.addItem(user);
                //check here for if the user's chat is in the array list of chat histories
                boolean inList = false;
                for (String chat : chatHistories)
                {
                    if(chat.startsWith(user))
                    {
                        inList = true;
                    }
                }
                if(!inList)
                {
                    chatHistories.add(user + "#");
                }
            }
        }
        scUsers.close();
        cmbOptions.setEnabled(true);
        if (cmbOptions.getItemCount() == 0)
        {
            JOptionPane.showMessageDialog(null,"No other users online :(");
            cmbOptions.setEnabled(false);
        }
    }

    private static void saveAndDisplayMessage(String msg)
    {
        String cmbOption = cmbOptions.getSelectedItem().toString();
        for(int i=0; i<chatHistories.size(); i++)
        {
            String recipientName = msg.substring(0, msg.indexOf(":"));
            if(chatHistories.get(i).startsWith(recipientName));
            {
                String chatHistory = chatHistories.get(i);
                chatHistories.set(i, chatHistory += msg + "\n");
                if(recipientName.equalsIgnoreCase(cmbOption))
                {
                    txaDisplayChat.setText(chatHistories.get(i));
                }
                break;
            }
        }
    }

    private static void disableGUI(){
        txaDisplayChat.append("You have been logged out...");
        txaDisplayChat.setEnabled(false);
        cmbOptions.setEnabled(false);
        txfMessage.setEnabled(false);
        btnSend.setEnabled(false);
        btnLogout.setEnabled(false);
        btnTransferFile.setEnabled(false);
    }

    private static void sendFile()
    {
        //Nic to implement
    }

    private static void processFileFromServer(String message)
    {
        //Nic to implement
    }
}
