package chatserver;
//Author: nikos
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import utils.Utils;

public class ChatServer {

    public static Logger chatLogger;

    private static boolean keepRunning = true;
    private static ServerSocket serverSocket;
    private static final Properties properties = Utils.initProperties("server.properties");
    public static HashMap<String, ClientHandler> clientHandlersMap = new HashMap<>();

    public static void stopServer() {
        //close the chatLogger too
        keepRunning = false;
        chatLogger.log(Level.INFO, "Server stopped");
        Utils.closeLogger(ChatServer.class.getName());
    }

    public static void addUserClientToHashMap(String username, ClientHandler ch) {
        clientHandlersMap.put(username, ch);
    }

    public static void removeUserClientFromHashMap(String username) {
        clientHandlersMap.remove(username);
    }

    public static void sendMessageToClients(ArrayList<String> recipients, String msg) {
        for (String r : recipients) {
            ClientHandler ch = clientHandlersMap.get(r);
            ch.send(msg);
        }
    }

    public static void main(String args[]) {
        //initialize chatLogger
        Utils.setLogFile(properties.getProperty("chatlogFile"), ChatServer.class.getName());
        chatLogger = Utils.getLogger(properties.getProperty("chatlogFile"), ChatServer.class.getName());
        //set port and IP
        int port = Integer.parseInt(properties.getProperty("chatPort"));
        String ip = properties.getProperty("serverIp");

        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ip, port));
            
            chatLogger.log(Level.INFO, "Chat Server started, listening on port: " + port);
            do {
                Socket socket = serverSocket.accept();
                chatLogger.log(Level.INFO, "Connected to a client");

                //TODO: pass a reference of echoServer to handler. If later we delete main, pass arg with (this)
                ChatServer dummy = new ChatServer();
                ClientHandler ch = new ClientHandler(socket, dummy);
                ch.start();

                //handleClient(socket);
            } while (keepRunning);
        } catch (IOException ex) {
            chatLogger.log(Level.SEVERE, "Couldnt create ServerSocket", ex);
        }
    }

}
