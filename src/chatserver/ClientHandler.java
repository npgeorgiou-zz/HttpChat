package chatserver;
//Author: nikos

import static chatserver.ChatServer.chatLogger;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import shared.ProtocolStrings;

/**
 *
 * @author ksptsinplanet
 */
public class ClientHandler extends Thread {

    ChatServer mainServer;
    public Socket s;
    Scanner input;
    PrintWriter writer;
    public String clientUsername;

    //declare possible states for client
    //start the state of the client as connecting. We just instiated the object
    public ClientHandler(Socket socket, ChatServer mainServer) throws IOException {
        this.mainServer = mainServer;
        s = socket;
        input = new Scanner(s.getInputStream());
        writer = new PrintWriter(s.getOutputStream(), true);

        clientUsername = "";
    }

    //This is a thread-safe method. The problem that it solves, is that multiple threads can try to write and delete
    //shared data at the onlineUsers file and the users-clientHandlers HashMap, while other threads are trying to read these
    //data. This method creates a command handler, that will either write(CONNECT), read (ONLINE SEND) or delete (CLOSE) 
    //data. This way our shared resources are locked while being changed. Minor delay due to thread block is acceptable.
    public synchronized void handleIncomingProtocolCommand(String message) {
        ServerCommandParser cp = new ServerCommandParser(this, mainServer);
        cp.analyseCommand(message);//ASK TEACHER: make this a start() so we can return to execution? or this will break the
        //syncronised block?
    }

    @Override
    public void run() {
        String message = input.nextLine();
        //Logger is thread-safe, thank god

        while (!message.equals(ProtocolStrings.CLOSE)) {
            chatLogger.log(Level.INFO, this.getName() + " received the message: " + message);
            handleIncomingProtocolCommand(message);
            message = input.nextLine();

        }
        chatLogger.log(Level.INFO, this.getName() + " received the message: " + message);
        handleIncomingProtocolCommand(message);

        try {
            writer.close();
            s.close();
        } catch (IOException ex) {
            chatLogger.log(Level.SEVERE, null, ex);
        }
        chatLogger.log(Level.INFO, this.getName() + "closed a Connection");
    }

    public void send(String msg) {
        writer.println(msg);
        chatLogger.log(Level.INFO, this.getName() + "client received: " + msg);
    }
}
