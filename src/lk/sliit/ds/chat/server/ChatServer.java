package lk.sliit.ds.chat.server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

/**
 * A multithreaded chat room server.  When a client connects the
 * server requests a screen name by sending the client the
 * text "SUBMITNAME", and keeps requesting a name until
 * a unique one is received.  After a client submits a unique
 * name, the server acknowledges with "NAMEACCEPTED".  Then
 * all messages from that client will be broadcast to all other
 * clients that have submitted a unique screen name.  The
 * broadcast messages are prefixed with "MESSAGE ".
 *
 * Because this is just a teaching example to illustrate a simple
 * chat server, there are a few features that have been left out.
 * Two are very useful and belong in production code:
 *
 *     1. The protocol should be enhanced so that the client can
 *        send clean disconnect messages to the server.
 *
 *     2. The server should do some logging.
 */
public class ChatServer {

    /**
     * The port that the server listens on.
     */
    private static final int PORT = 9001;

    /**
     * The set of all names of clients in the chat room.  Maintained
     * so that we can check that new clients are not registering name
     * already in use.
     */
    private static HashSet<String> names = new HashSet<String>();

    /**
     * The set of all the print writers for all the clients.  This
     * set is kept so we can easily broadcast messages.
     */
    
    /*	
     ***********************************************
       
     8. Using a HashMap to index the PrintWriter
     	with the corresponding client name
     	
     ***********************************************
     */
    
    private static HashMap<String, PrintWriter> writers = new HashMap<String, PrintWriter>();

    /**
     * The appplication main method, which just listens on a port and
     * spawns handler threads.
     */
    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
            	Socket socket  = listener.accept();
                Thread handlerThread = new Thread(new Handler(socket));
                handlerThread.start();
            }
        } finally {
            listener.close();
        }
    }

    /**
     * A handler thread class.  Handlers are spawned from the listening
     * loop and are responsible for a dealing with a single client
     * and broadcasting its messages.
     */
    private static class Handler implements Runnable {
        private String name;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        /**
         * Constructs a handler thread, squirreling away the socket.
         * All the interesting work is done in the run method.
         */
        public Handler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Services this thread's client by repeatedly requesting a
         * screen name until a unique one has been submitted, then
         * acknowledges the name and registers the output stream for
         * the client in a global set, then repeatedly gets inputs and
         * broadcasts them.
         */
        public void run() {
            try {

                // Create character streams for the socket.
                in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Request a name from this client.  Keep requesting until
                // a name is submitted that is not already used.  Note that
                // checking for the existence of a name and adding the name
                // must be done while locking the set of names.
                while (true) {
                    out.println("SUBMITNAME");
                    name = in.readLine();
                    if (name == null) {
                        return;
                    }
                    

                    /*	
                    ***********************************************
                      
                    7. shared variable 'names' is now thread safe
                    	
                    ***********************************************
                    */
                    
                    synchronized(names) {
						 if (!names.contains(name)) {
						        names.add(name);
						        break;
						    }
                    }
                 }

                // Now that a successful name has been chosen, add the
                // socket's print writer to the set of all writers so
                // this client can receive broadcast messages.
                out.println("NAMEACCEPTED");
                writers.put(name, out);
                
                
                /*	
                 ***********************************************
                   
                 9.	Broadcasting the newly added client to the
                 	remaining clients in the chat room
                 	
                 	CLIENT:- Header that contains the name of the
                 	new client
                 	
                 ***********************************************
                 */
               
                
                for(Entry<String, PrintWriter> writer: writers.entrySet()) {
                	if(writer.getKey().equals(name))
                		continue;
                	writer.getValue().println("CLIENT " + name);
                }
                
                /*	
                 ***********************************************
                   
                 9.	Unicasting the currently available clients
                 	in the chat room to the newly joined client
                 	
                 	CLIENT:- Header that contains the name of the
                 	existing clients in the chat rooms exept this
                 	
                 ***********************************************
                 */
                
                for(String n: names) {
                	if(!n.equals(name))
                		out.println("CLIENT " + n);
                }
                
                
                // Accept messages from this client and broadcast them.
                // Ignore other clients that cannot be broadcasted to.
                String unicastMessage = "";
                while (true) {
                    String input = in.readLine();
                    

                    if (input == null) {
                        return;
                    } else if (input.startsWith("BROADCAST")) {
                    	
                    	/*	
                         ***********************************************
                           
                         9.	Multicasting/Unicasting the message
                         	message by repeating the same message depending
                         	on the case
                         	
                         	BROADCAST:- Header to identify this is a
                         	broadcast type message
                         	
                         ***********************************************
                         */
                    	
                        for (Entry<String, PrintWriter> writer : writers.entrySet()) {
                            writer.getValue().println("MESSAGE " + name + ": " + input.substring(10));
                        }
                    } else if (input.startsWith("UNICAST")) {
                    	
                    	/*	
                         ***********************************************
                           
                         9.	Multicasting/Unicasting the message
                         	message by repeating the same message depending
                         	on the case
                         	
                         	UNICAST:- Header to identify this is a
                         	point-to-point message type and as the tail it
                         	contains the name of the client
                         	
                         	BODY:- Header that contains the message body to be sent
                         	
                         	END:- Header that informs the end of the unicast/multicast
                         	message
                         ***********************************************
                         */
                    	
                    	String user = input.substring(8);
                    	String line2 = in.readLine();

                    	if (line2.startsWith("BODY")) {
                    	
                    		String msg = line2.substring(5);
                    		unicastMessage = "MESSAGE " + name + ": " + msg;
	                    	writers.get(user).println(unicastMessage);
                    	}
                    	
                    } else if (input.startsWith("END")) {
                    	out.println(unicastMessage);
                    }
                    
                  

                }
            }
            catch (IOException e) {
            	
            	/*	
                 ***********************************************
                   
                 9.	Broadcasting the client name to all
                 	remaining clients in the chat room to remove
                 	it when this client is going down
                 	
                 	REMOVECLIENT:- Header that contains the name
                 	of this client going down
                 	
                 ***********************************************
                 */
            	
            	for(Entry<String, PrintWriter> writer: writers.entrySet()) {
                	if(writer.getKey().equals(name))
                		continue;
                	writer.getValue().println("REMOVECLIENT " + name);
                }
            	
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                if (name != null) {
                    names.remove(name);
                }
                if (out != null) {
                    writers.remove(name);
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }

            }
        }
        
    }
}