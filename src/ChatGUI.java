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
    private JComboBox <String> cmbOptions;
    private JTextField txfMessage;
    private JButton btnSend;
    private JButton btnTransferFile;

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
        Socket socket = new Socket("192.168.0.109", serverPort);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new Scanner(socket.getInputStream());

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

        // These threads must send and receive messages
        // sendMessage thread
        // Thread sendMessage = new Thread(new Runnable()
        // {
        //     @Override
        //     public void run()
        //     {
        //         while (true)
        //         {
        //             // read the message to deliver.
        //             //String msg = scn.nextLine();
        //             //out.println(msg);
        //         }
        //     }
        // });

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
                    txaDisplayChat.append(msg + "\n");
                    if(msg.equalsIgnoreCase(ProtocolResponses.REQUEST_LOGOUT))
                    {
                        System.exit(0);
                    }
                }
            }
        });

        //sendMessage.start();
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
        cmbOptions.addItem("SHANE");
        cmbOptions.addItem("JONO");
        cmbOptions.addActionListener(this);
        usersBox.add(cmbOptions);
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
            txaDisplayChat.append(name + ": " + message + "\n");
            txfMessage.setText("");
            out.println(matchProtocol("MESSAGE", recipient, message));
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
