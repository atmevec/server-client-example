/*
*  Server.java
*
*  Copyright (c) 2014 Alexander Mevec
*/

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    
    //Where we store the client threads
    private static ArrayList<ClientThread> ct;
    //Port number
    private final int port;
    //Are we running?
    private static boolean running;
    //IDs for clients
    private int id = 0;

    public Server(int port) {
        //Let them set the port number
        this.port = port;
        //Instantiation!
        setAl(new ArrayList<>());
    }

    public void start() {
        //We are up and running!
        running = true;
        System.out.println("Chat room opened!");
        try {
            //Open the socket on the port
            ServerSocket serverSocket = new ServerSocket(port);
            while(running) {
                //Let the socket accept connections at will
                Socket socket = serverSocket.accept();
                if(!running)
                    //Stop everything
                    break;
                //Open a new client thread for this socket connection
                ClientThread t = new ClientThread(socket);
                //Give them an ID
                t.id = this.id;
                id++;
                //Add the client to our list
                getCT().add(t);
                t.start();
            }
            try {
                //We aren't running so close everything!
                serverSocket.close();
                for(int i = 0; i < getCT().size(); ++i) {
                    //Iterate through client threads and close them
                    ClientThread tc = getCT().get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    }
                    catch(IOException ioE) {}
                }
            }
            catch(IOException e) {
                //I don't know what you did to pull this off, but we can't close the connection
                System.out.println("Exception closing the server and clients: " + e);
            }
        }
        catch (IOException e) {
            String msg = " Exception on new ServerSocket: " + e;
            System.out.println(msg);
        }
    }

    protected void stop() {
        //We are not running anymore, stop!
        running = false;
        try {
            Socket socket = new Socket("localhost", port);
        }
        catch(IOException e) {}
    }

    //Remove the person at the ID
    synchronized void remove(int id) {
        getCT().remove(id);
    }

        
    public static void main(String args[]){
        //Check their input for format
        if (args.length == 1) {
            try {
                //Try to open a new Server
                Server chatServer = new Server(Integer.parseInt(args[0]));
                chatServer.start();
            } catch (NumberFormatException e) {
                //Sadly, we cannot open a port on "fish sticks" right now. Maybe some day.
                System.out.println("The port number must be an integer!");
                System.exit(1);
            }
        } else {
            //This is how you use the server
            System.out.println("Correct usage: server PORT_NUMBER");
        }
    }

    //Broadcast the message
    public void broadcast(String message) {
        //Print it so the person running the server can read it
        System.out.println(message);
        for (int i = 0; i < getCT().size(); i++) {
            //Iterate through client threads and send the message to each one at a time
            ClientThread tempCT = getCT().get(i);
            tempCT.sendMessage(new Protocol(Protocol.MESSAGE, message));
        }
    }
    
    //Getter
    public static ArrayList<ClientThread> getCT() {
        return ct;
    }

    //Setter
    public static void setAl(ArrayList<ClientThread> al) {
        Server.ct = al;
    }

    //The amazing ClientThread class
    public class ClientThread extends Thread {
        //Our socket
        Socket socket;
        //IOstreams
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        //ID so we can close this thread later
        int id;
        //Username for the thread
        String username;
        //Our input coming in through the Protocol object
        Protocol input;

        ClientThread(Socket socket) {
            //Set our socket
            this.socket = socket;
            try {
                //Open a connection
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput  = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                //Let everyone know your beautiful face has joined theirs
                broadcast(username + " just joined. ");
            }
            //Error catching
            catch (IOException e) {
                System.out.println(e);
            }
            catch (ClassNotFoundException e) {}
        }

        @Override
        public void run() {
            boolean running = true;
            //When this thread is running
            while(running) {
                try {
                    //Read the input
                    input = (Protocol) sInput.readObject();
                }
                catch (IOException e) {
                    //If we cannot read the input they are not connected!
                    broadcast(username + " was disconnected.");
                    remove(id);
                    break;
                }
                catch(ClassNotFoundException e2) {}
                //Set the message
                String message = input.getMessage();
                //Switch based on the protocol
                switch(input.getType()) {
                    //MESSAGE type
                    case Protocol.MESSAGE:
                        //They sent a message, pass it on!
                        broadcast(username + ": " + message);
                    break;
                }
            }
            close();
        }

        private void close() {
            //Try disconnecting them
            try {
                if(sOutput != null) sOutput.close();
            }
            catch(IOException e) {}
            try {
                if(sInput != null) sInput.close();
            }
            catch(IOException e) {};
            try {
                if(socket != null) socket.close();
            }
            catch (IOException e) {}
        }

        //Send the message object
        void sendMessage(Protocol msg) {
            try {
                //Write it to the output stream
                sOutput.writeObject(msg);
            }
            catch(IOException e) {
                System.out.println("Exception writing to server: " + e);
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