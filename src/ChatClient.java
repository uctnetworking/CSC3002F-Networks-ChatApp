import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.file.*; //Import for file transfer
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.nio.charset.StandardCharsets;

public class ChatClient extends JFrame implements ActionListener
{
    private final static String ipAddress = "localhost";
    private final static int serverPort = 60000;

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
    private static PrintWriter out;
    private static Scanner in;
    private static String name;
    private static Socket socket;

    /** Main method for running the application.*/
    public static void main(String [] args) throws UnknownHostException, IOException
    {
        ChatClient gui = new ChatClient();
        gui.setVisible(true);

        // establish the connection
        socket = new Socket(ipAddress, serverPort);

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
                gui.setDefaultCloseOperation(EXIT_ON_CLOSE);
                socket.close();
                loggedIn = false;
                gui.setTitle(name + "'s Chats (Offline)");
                saveChatHistoriesToStorage();
            }
            else if(msg.startsWith("Online Users:"))
            {
                updateOnlineUsers(msg);
            }
            else if(msg.startsWith(ProtocolRequests.MESSAGE))
            {
                saveAndDisplayMessageFromSender(msg);
            }
            else if(msg.startsWith(ProtocolRequests.FILE)) //not currently supported
            {
                //processFileFromServer(msg);
            }
        }
    }

    /**
    * Constructor that sets up the GUI.
    */
    public ChatClient()
    {
        super("Chat App");
        setSize(WIDTH,HEIGHT);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
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
        String thanksMessage = in.nextLine();
        //JOptionPane.showMessageDialog(null, thanksMessage); // This is a thanks from the server for a correct name
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
                String restoredChatHistory = restoreChatHistoryFromStorage(user);
                if(restoredChatHistory.equals("File not found"))
                {
                    chatHistories.add(user + "#");
                }
                else
                {
                    chatHistories.add(user + "#" + restoredChatHistory);
                }
            }
        }
        scUsers.close();
        displayCorrectChatHistory();
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
                    // get time the message was sent
                    Calendar calendar = Calendar.getInstance();
                    Date time = calendar.getTime();
                    DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                    String formattedTime = dateFormat.format(time);

                    String chatHistory = chatHistories.get(i);
                    chatHistories.set(i, chatHistory += formattedTime + " | You: " + message + "\n");
                    chatHistory = chatHistories.get(i);
                    txaDisplayChat.setText(chatHistory.substring(chatHistory.indexOf("#")+1));
                    break;
                }
            }
            out.println(matchProtocol(ProtocolRequests.MESSAGE, recipient, message));
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

    private static void displayCorrectChatHistory()
    {
        if(cmbOptions.getItemCount() > 0)
        {
            String selectedChat = cmbOptions.getSelectedItem().toString();
            for(String userHistory : chatHistories)
            {
                if(userHistory.startsWith(selectedChat))
                {
                    txaDisplayChat.setText(userHistory.substring(userHistory.indexOf('#')+1));
                }
            }
        }
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
            if(!user.equalsIgnoreCase(name)) // ensures the user cannot chat with themself
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
                if(!inList) // if not in memory, check if in storage, otherwise doesnt exist at all
                {
                    String restoredChatHistory = restoreChatHistoryFromStorage(user);
                    if(restoredChatHistory.equals("File not found"))
                    {
                        chatHistories.add(user + "#");
                    }
                    else
                    {
                        chatHistories.add(user + "#" + restoredChatHistory);
                    }
                }
            }
        }
        scUsers.close();
        displayCorrectChatHistory();
        cmbOptions.setEnabled(true);
        if (cmbOptions.getItemCount() == 0)
        {
            txaDisplayChat.setText(""); //empty the text area from previous chat if no more users online
            JOptionPane.showMessageDialog(null,"No other users online :(");
            cmbOptions.setEnabled(false);
        }
    }

    private static void saveAndDisplayMessageFromSender(String msg)
    {
        String cmbOption = cmbOptions.getSelectedItem().toString();
        for(int i=0; i<chatHistories.size(); i++)
        {
            Scanner scLine =  new Scanner(msg).useDelimiter("#");
            scLine.next(); // MESSAGE or FILE
            String senderName = scLine.next(); // the person to send to
            String message = scLine.next();// the actual message
            scLine.close();
            if(chatHistories.get(i).startsWith(senderName))
            {
                // get time the message was sent
                Calendar calendar = Calendar.getInstance();
                Date time = calendar.getTime();
                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                String formattedTime = dateFormat.format(time);

                String chatHistory = chatHistories.get(i);
                chatHistories.set(i, chatHistory += formattedTime + " | " + senderName + ": " + message + "\n");
                if(senderName.equalsIgnoreCase(cmbOption)) //if currently on the sender's chat view, update the text area
                {
                    chatHistory = chatHistories.get(i);
                    txaDisplayChat.setText(chatHistory.substring(chatHistory.indexOf("#")+1));
                }
                JOptionPane.showMessageDialog(null, name + ", you have a new message from " + senderName);
                break;
            }
        }
    }

    private static void disableGUI()
    {
        //JOptionPane.showMessageDialog(null,"You have been logged out...");
        txaDisplayChat.setEnabled(true); // allow them to still select and view old chats
        cmbOptions.setEnabled(true); // allow them to still select and view old chats
        txfMessage.setEnabled(false);
        btnSend.setEnabled(false);
        btnLogout.setEnabled(false);
        btnTransferFile.setEnabled(false);
    }

    private static void sendFile()
    {
        JFileChooser filePicker = new JFileChooser();     //JFiler chooser for the user to graphically pick the file they want to send.
        Path fl = null;                                   //Path used to get the contents of the file and convert to bytes
        byte[] fileContent = null;                         //bytes array of file to send
        int response = filePicker.showOpenDialog(null);
        filePicker.setApproveButtonText("Select File");
        if(response == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                //dir = filePicker.getSelectedFile().toString();
                 fl = filePicker.getSelectedFile().toPath();
                //Path p = filePicker.getS

                fileContent = Files.readAllBytes(fl);
                String recipientName = cmbOptions.getSelectedItem().toString();
                String protocol = ProtocolRequests.FILE;
                String sendPrefix = protocol + recipientName;
                for (int i =sendPrefix.length()-1; i<31; i++)
                  {
                      sendPrefix = sendPrefix+"*";
                  }
                  //JOptionPane.showMessageDialog(null, sendPrefix );
                 sendPrefix = sendPrefix +fl.getFileName().toString();
                 if(fl.getFileName().toString().length()>32){JOptionPane.showMessageDialog(null, "Error: File name to long. Name should be less than 32 Characters long" ); return;}
                 //JOptionPane.showMessageDialog(null, sendPrefix );
                for (int i =sendPrefix.length()-1; i<63; i++)
                  {
                      sendPrefix = sendPrefix+"*";
                  }
                long size = Files.size(fl);
                if(size>20000000){JOptionPane.showMessageDialog(null, "Error: File size to big. Please select a file less than 20MB" ); return;}
                String ssize = String.format("%08d", size);  // 0009
                sendPrefix = sendPrefix+ssize;
                JOptionPane.showMessageDialog(null, sendPrefix );
                byte[] pre = sendPrefix.getBytes();
                ByteArrayOutputStream dataOut = new ByteArrayOutputStream( );
                dataOut.write(pre);
                dataOut.write(fileContent);
                byte combined[] = dataOut.toByteArray( );
                socket.getOutputStream().write(combined);
                JOptionPane.showMessageDialog(null, "File found and converted" );
            }
            catch (IOException ex) {

                JOptionPane.showMessageDialog(null, "File cannot be transfered", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else
        {
            JOptionPane.showMessageDialog(null, "File selection cancelled" );
        }
    }

    private static void processFileFromServer(byte[] message)
    {
      String type= new String(Arrays.copyOfRange(message, 0, 1), StandardCharsets.US_ASCII);
      String client = new String(Arrays.copyOfRange(message, 1, 32), StandardCharsets.US_ASCII);

        System.out.println(type);
        System.out.println(client);
        byte[] fileBytes = Arrays.copyOfRange(message, 72, message.length);

        JFileChooser filePicker = new JFileChooser();
        int response = filePicker.showSaveDialog(null);
         //filePicker.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        filePicker.setApproveButtonText("Select Save Destination");

        if(response == JFileChooser.APPROVE_OPTION)
        {
            String dir = filePicker.getSelectedFile().toString();
             File file = new File((dir));
           try
           {
             OutputStream os = new FileOutputStream(file); // Initialize a pointer in file using OutputStream
             os.write(fileBytes);  // Starts writing the bytes in it
             os.close();   // Close the file
        }

        catch (Exception e)
        {
            JOptionPane.showMessageDialog(null, "File cannot be transfered", "Error", JOptionPane.ERROR_MESSAGE);
        }
        }
        else
        {
            JOptionPane.showMessageDialog(null, "File transfer cancelled" );
        }
    }

    private static void saveChatHistoriesToStorage()
    {
        for(String chatHistory : chatHistories)
        {
            String user = chatHistory.substring(0,chatHistory.indexOf("#")); //retrieve name from [name]#[history]
            String history = chatHistory.substring(chatHistory.indexOf("#") + 1);
            if(!history.equals("")) // if the chat with the person isn't a blank chat
            {
                PrintWriter printer = null;
                try
                {
                    if (Files.notExists(FileSystems.getDefault().getPath(name + "'s chats")))
                    {
                        Files.createDirectory(FileSystems.getDefault().getPath(name + "'s chats"));
                    }
                    printer = new PrintWriter(new FileOutputStream(name + "'s chats/" + user.toUpperCase() + ".txt", false));
                    printer.print(history); //retrieve history from [name]#[history]

                    //prints a line of -------- to indicate end of chat session
                    history = history.substring(0,history.length()-1); // remove the final \n
                    String lastLine = history.substring(history.lastIndexOf("\n")+1);
                    if(lastLine.charAt(0) != '-') // to avoid a repeated line of ------
                    {
                        int lengthOfLastLine = lastLine.length();
                        for(int i = 0; i < lengthOfLastLine; i++)
                        {
                            printer.print("-");
                        }
                        printer.print("\n");
                    }
                    printer.close();
                }
                catch(FileNotFoundException fe)
                {
                   System.out.println("Error -  file could not be opened");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    private static String restoreChatHistoryFromStorage(String otherUser)
    {
        try
        {
            String fileName = name + "'s chats/" + otherUser.toUpperCase() + ".txt";
            Scanner scFile = new Scanner(new File(fileName));
            String contents = "";
            while(scFile.hasNextLine())
            {
                contents += scFile.nextLine() + "\n";
            }
            scFile.close();
            return contents;
        }
        catch(FileNotFoundException fe)
        {
            return "File not found";
        }
    }
}
