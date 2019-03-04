import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;

public class ChatGUI extends JFrame implements ActionListener
{
    public static final int WIDTH = 650;
    public static final int HEIGHT = 400;
    public static final int SMALL_STRUT = 20;
    public static final int MEDIUM_STRUT = 25;
    public static final int LARGE_STRUT = 30;

    private static JTextArea txaDisplayChat;
    private static JComboBox <String> cmbOptions;
    private JTextField txfMessage;
    private JButton btnSend;
    private JButton btnTransferFile;
    private JButton btnLogout;

    private final static int serverPort = 60000;
    private static PrintWriter out;
    private static Scanner in;
    private static String name;

    /** Main method for running the application.*/
    public static void main(String [] args) throws UnknownHostException, IOException
    {
        ChatGUI gui = new ChatGUI();
        gui.setVisible(true);

        // establish the connection
        Socket socket = new Socket("196.42.105.163", serverPort);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new Scanner(socket.getInputStream());

        name = JOptionPane.showInputDialog(in.nextLine()); // This is a request from the server for the client's name
        out.println(name);
        String serverResponse = in.nextLine();
        while(!serverResponse.equals("Name successfully registered, you are now online"))
        {
            name = JOptionPane.showInputDialog(serverResponse); // Asks the client to enter another name
            out.println(name);
            serverResponse = in.nextLine();
        }
        JOptionPane.showMessageDialog(null, in.nextLine()); // This is a thanks from the server for a correct name

        gui.setTitle(name + "'s Chats");
        String users = in.nextLine();
        Scanner scUsers = new Scanner(users.substring(users.indexOf(":")+2)).useDelimiter("#");
        while(scUsers.hasNext())
        {
            String user = scUsers.next();
            if(!user.equalsIgnoreCase(name))
            {
                cmbOptions.addItem(user);
            }
        }
        if (cmbOptions.getItemCount() == 0)
        {
            JOptionPane.showMessageDialog(null,"No other users online yet");
            cmbOptions.setEnabled(false);
        }

        // readMessage thread
        Thread readMessage = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    // read the message sent to this client
                    String msg =  in.nextLine();
                    if(msg.equalsIgnoreCase("You have been logged out"))
                    {
                        System.exit(0);
                    }
                    else if(msg.startsWith("Online Users:"))
                    {
                        cmbOptions.removeAllItems();
                        Scanner scUsers = new Scanner(msg.substring(msg.indexOf(":")+2)).useDelimiter("#");
                        while(scUsers.hasNext())
                        {
                            String user = scUsers.next();
                            if(!user.equalsIgnoreCase(name))
                            {
                                cmbOptions.addItem(user);
                            }
                        }
                        cmbOptions.setEnabled(true);
                        if (cmbOptions.getItemCount() == 0)
                        {
                            JOptionPane.showMessageDialog(null,"No other users online yet");
                            cmbOptions.setEnabled(false);
                        }
                    }
                    else
                    {
                        txaDisplayChat.append(msg + "\n");
                    }
                }
            }
        });

        readMessage.start();
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
        btnTransferFile = new JButton("Paperclip"); // Transfer file
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
            String recipient = cmbOptions.getSelectedItem().toString();
            String message = txfMessage.getText();
            if(!message.equals(""))
            {
                txaDisplayChat.append("You: " + message + "\n");
                out.println(matchProtocol("MESSAGE", recipient, message));
                txfMessage.setText("");
            }
        }
        else if(e.getActionCommand().equals("Logout"))
        {
            int sure = JOptionPane.showConfirmDialog(null,"Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if(sure == JOptionPane.YES_OPTION)
            {
                out.println("LOGOUT");
            }
        }
        else if(e.getActionCommand().equals("Transfer"))
        {
            //handle file transfer
        }
    }

    /**
    Helper method that puts the protocol, recipient name and message into the format expected by the server.
    */
    private String matchProtocol(String protocol, String recipient, String message)
    {
        return protocol + "#" + recipient + "#" + message;
    }
}
