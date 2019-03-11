package lk.sliit.ds.chatserver;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A simple Swing-based client for the chat server.  Graphically
 * it is a frame with a text field for entering messages and a
 * textarea to see the whole dialog.
 *
 * The client follows the Chat Protocol which is as follows.
 * When the server sends "SUBMITNAME" the client replies with the
 * desired screen name.  The server will keep sending "SUBMITNAME"
 * requests as long as the client submits screen names that are
 * already in use.  When the server sends a line beginning
 * with "NAMEACCEPTED" the client is now allowed to start
 * sending the server arbitrary strings to be broadcast to all
 * chatters connected to the server.  When the server sends a
 * line beginning with "MESSAGE " then all characters following
 * this string should be displayed in its message area.
 */
public class ChatClient {

    BufferedReader in;
    PrintWriter out;
    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(40);
    JTextArea messageArea = new JTextArea(8, 40);
    DefaultListModel<String> model = new DefaultListModel<>();
    JList<String> listBox = new JList<>(model);
    JCheckBox checkBox = new JCheckBox("Multicast Message");
    // TODO: Add a list box

    /**
     * Constructs the client by laying out the GUI and registering a
     * listener with the textfield so that pressing Return in the
     * listener sends the textfield contents to the server.  Note
     * however that the textfield is initially NOT editable, and
     * only becomes editable AFTER the client receives the NAMEACCEPTED
     * message from the server.
     */
    public ChatClient() {

        // Layout GUI
        textField.setEditable(false);
        messageArea.setEditable(false);
        listBox.setEnabled(false);
        checkBox.setEnabled(false);
        frame.getContentPane().add(textField, "North");
        frame.getContentPane().add(checkBox, "South");
        frame.getContentPane().add(new JScrollPane(listBox), "West");
        frame.getContentPane().add(new JScrollPane(messageArea), "Center");
        frame.pack();

        // TODO: You may have to edit this event handler to handle point to point messaging,
        // where one client can send a message to a specific client. You can add some header to 
        // the message to identify the recipient. You can get the receipient name from the listbox.
        textField.addActionListener(new ActionListener() {
            /**
             * Responds to pressing the enter key in the textfield by sending
             * the contents of the text field to the server.    Then clear
             * the text area in preparation for the next message.
             */
            public void actionPerformed(ActionEvent e) {
            	
            	if(!checkBox.isSelected()) {
            	
            		
            		String msg = textField.getText();
            		
            		/*	
                     ***********************************************
                       
                     8. Check the message format and identify the user
                     	section and the message section using '>>'.
                     	Then creating the Unicast message to the server
                     	
                     ***********************************************
                     */
            		
            		if(msg.matches("([a-zA-Z0-9_]+>>[^\\n]+)")) {
            			String user = msg.split(">>")[0];
                		String message = msg.split(">>")[1];
                
                		out.println("UNICAST " + user);
            			out.println("BODY " + message);
            		}else {
            			
            			
            			/*	
                         ***********************************************
                           
                         8. Default message format generate the Broadcast
                         	message to the server
                         	
                         ***********************************************
                         */
            			
            			out.println("BROADCAST " + textField.getText());
            		}
            		
            		

            	}else {
            		
            		
            		/*	
                     ***********************************************
                       
                     10. Getting the clients from the list box and
                     	 generating the unicast message accordingly
                     	  
                     	
                     ***********************************************
                     */
            		
            		List<String> nameList = listBox.getSelectedValuesList();

            		if(nameList.size() > 0) {
            		
	            		for(String n: nameList) {
	            			
	            			out.println("UNICAST " + n);
	            			out.println("BODY " + textField.getText());
	            		}
	            		
	            		out.println("END");
            		
            		} else {
            			
            			JOptionPane.showMessageDialog(frame, "You should select at least one client to multicast the message !");
            			
            		}
                    
            	}
            	
            	textField.setText("");
            }
        });
        
        checkBox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
                if(checkBox.isSelected())
                	listBox.setEnabled(true); 
                else 
                	listBox.setEnabled(false);
				
			}
        	
        	
        });
        
        
    }

    /**
     * Prompt for and return the address of the server.
     */
    private String getServerAddress() {
        return JOptionPane.showInputDialog(
            frame,
            "Enter IP Address of the Server:",
            "Welcome to the Chatter",
            JOptionPane.QUESTION_MESSAGE);
    }

    /**
     * Prompt for and return the desired screen name.
     */
    private String getName() {
        return JOptionPane.showInputDialog(
            frame,
            "Choose a screen name:",
            "Screen name selection",
            JOptionPane.PLAIN_MESSAGE);
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {

        // Make connection and initialize streams
        String serverAddress = getServerAddress();
        Socket socket = new Socket(serverAddress, 9001);
        in = new BufferedReader(new InputStreamReader(
            socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Process all messages from server, according to the protocol.
        
        /*	
         ***********************************************
           
         9. Handling the Broadcast/Unicast messages that
         	informs the new client joins, client leaves
         	and chat messages.
         	
         ***********************************************
         */
        try {
        
        while (true) {
            String line = in.readLine();
            if (line.startsWith("SUBMITNAME")) {
                out.println(getName());
            } else if (line.startsWith("NAMEACCEPTED")) {
                textField.setEditable(true);
                checkBox.setEnabled(true);
            } else if (line.startsWith("MESSAGE")) {
                messageArea.append(line.substring(8) + "\n");
            } else if (line.startsWith("CLIENT")) {
            	String data = line.substring(7);
            	model.addElement(data);
            } else if (line.startsWith("REMOVECLIENT")) {
            	String data = line.substring(13);
            	model.removeElement(data);
            }
        }
        
        } finally {
        	socket.close();
        }
        
        
    }

    /**
     * Runs the client as an application with a closeable frame.
     */
    public static void main(String[] args) throws Exception {
        ChatClient client = new ChatClient();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);
        client.run();
    }
}