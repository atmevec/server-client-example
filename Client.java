/*
*  Client.java
*
*  Copyright (c) 2014 Alexander Mevec
*/

import java.net.*;
import java.io.*;

public class Client  {
    //Our IOstreams
    private ObjectInputStream sInput;
    private ObjectOutputStream sOutput;
    //Socket
    private Socket socket;
    //Port
    int port;
    //Server address and username (loopback because this is just for local testing)
    private final String server = "loopback", username;
    //Are we running?
    private boolean running = false;


    Client(int port, String username) {
        //Lets a user input their own port and username
        this.port = port;
        this.username = username;
    }
    
    public boolean start() {
        running = true;
        try {
            //Open a socket
            socket = new Socket(server, port);
        } 
        catch(IOException ec) {
            System.out.println("There was a problem connecting with the server.");
            return false;
        }
        try {
            //Open up our IOstreams on the socket
            sInput  = new ObjectInputStream(socket.getInputStream());
            sOutput = new ObjectOutputStream(socket.getOutputStream());
        }
        catch (IOException eIO) {
            System.out.println("Exception creating new Input/output Streams: " + eIO);
            return false;
        }
        //Start our listening thread
        new ListenFromServer().start();
        try {
            //Send the server our username (also tests the connection!)
            sOutput.writeObject(username);
        }
        catch (IOException eIO) {
            //We are obviously not connected if we can't send our username!
            System.out.println("Exception doing login : " + eIO);
            //Disconnect
            disconnect();
            return false;
        }
        //If we are running
        while (running) {
            //Please check for user input (messages)
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String Message;
            try {
                //Read the line
                Message = br.readLine();
                //Send it using the MESSAGE protocol
                sendMessage(new Protocol(Protocol.MESSAGE, Message));
            } catch (IOException ioe) {
                System.out.println("IO error trying to read your name!");
                System.exit(1);
            }
        }
        return true;
    }

    void sendMessage(Protocol msg) {
        try {
            //Write our protocol object out to the server
            sOutput.writeObject(msg);
        }
        catch(IOException e) {
            //We couldn't manage that message, try again next time
            System.out.println("Exception writing to server: " + e);
        }
    }
    
    private void disconnect() {
        try { 
            if(sInput != null)
                //If the input is not closed, close it!
                sInput.close();
        }
        catch(IOException e) {}
        try {
            if(sOutput != null)
                //If the output is still open, close it!
                sOutput.close();
        }
        catch(IOException e) {}
        try {
            if(socket != null)
                //Close the socket!
                socket.close();
	}
	catch(IOException e) {}
        //We are officially disconnected!
        System.out.println("Disconnected");
        System.exit(1);
    }
    
    public static void main(String[] args) {
        //Make sure it's in the right format
        if (args.length == 2) {
            //Try to start the client, if they're messing with up then don't connect them
            try {
                Client chatClient = new Client(Integer.parseInt(args[0]), args[1]);
                chatClient.start();
            } catch (NumberFormatException e) {
                //We certainly can't open a port on number "doughnut"
                System.out.println("The port number must be an integer!");
                System.exit(1);
            }
        } else {
            //This is how you run the client
            System.out.println("Correct usage: client PORT_NUMBER USER_NAME");
        }    
    }
    
    class ListenFromServer extends Thread {
        //This is our protocol object that gets sent to us from the server
        private Protocol input;
        @Override
        public void run() {
            while(true) {
                try {
                    //Reads the input from the server when possible
                    input = (Protocol) sInput.readObject();
		}
		catch (IOException e) {
                    //Something has gone terribly wrong, disconnect!
                    System.out.println("You either logged out or lost connection!");
                    disconnect();
                    break;				
		}
		catch(ClassNotFoundException e2) {}
                
                //Switch based on type of message being received
                switch(input.getType()) {
                    //It's a MESSAGE
                    case Protocol.MESSAGE:
                        //Get the message then display it
                        String message = input.getMessage();
                        System.out.println(message);
                    break;
                }
            }
        }
    }
}
class Protocol implements Serializable {

    //This is how we tell what the contents are going to be
    static final int MESSAGE = 0;
    //This is where the types defined above are stored
    private int type;
    //This is where the messages go
    private String message;

    //Constructor
    Protocol(int type, String message) {
        this.type = type;
        this.message = message;
    }

    //Getters
    int getType() {
        return type;
    }

    String getMessage() {
        return message;
    }
}