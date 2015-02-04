package chatclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import shared.ProtocolStrings;

public class ChatClient extends Thread {

    Socket socket;
    private int port;
    private InetAddress serverAddress;
    private Scanner input;
    private PrintWriter output;
    List<ChatListener> listeners = new ArrayList();

    String ClientState;

    public void connect(String address, int port, String username) throws UnknownHostException, IOException {
        this.port = port;
        serverAddress = InetAddress.getByName(address);
        socket = new Socket(serverAddress, port);
        input = new Scanner(socket.getInputStream());
        output = new PrintWriter(socket.getOutputStream(), true);

        send(ProtocolStrings.CONNECT + username);
        ClientState = "CONNECTING";
        start();
    }

    public void send(String msg) throws IOException {
        if (socket.isOutputShutdown()) {
            throw new IOException("Outbound socket is closed");
        }
        output.println(msg);
        output.flush();
    }

    public void sendToSome(String msg, String[] users) {
        String recipients = "";
        int i = 1;
        for (String s : users) {
            if (i == users.length) {
                recipients += s;
            } else {
                recipients = recipients + s + ",";
            }
            i++;
        }
        output.println(ProtocolStrings.SEND + recipients + "#" + msg);

    }

    public void sendToAll(String msg) {
        output.println(ProtocolStrings.SEND + "*" + "#" + msg);

    }

    public void stopClient() throws IOException {
        output.println(ProtocolStrings.CLOSE);
        ClientState = "DISCONNECTING";
        socket.shutdownOutput();
    }

    public void registerEchoListener(ChatListener l) {
        listeners.add(l);
    }

    public void unRegisterEchoListener(ChatListener l) {
        listeners.remove(l);
    }

    private void notifyListenersOfMsg(String msg) {
        String command = msg.split("#")[0].concat("#");
        switch (command) {
            case ProtocolStrings.ONLINE:
                String onlineUsers = msg.split("#")[1];
                msg = "Users online: " + onlineUsers;
                break;
            case ProtocolStrings.MESSAGE:
                String sender = msg.split("#")[1];
                String tweet = msg.split("#")[2];
                msg = sender + " sais: " + tweet;
                break;
            case ProtocolStrings.CLOSE://never, as it gets handled ouside loop, but i add it for good order of code. 
                break;
            default:
                throw new IllegalArgumentException("Invalid command: " + command);
        }

        for (ChatListener l : listeners) {
            l.messageArrived(msg);
        }
    }

    private void notifyListenersOfNewList(String data) {
        String[] commandArray = data.split("#");
        //take each user
        String[] users = commandArray[1].split(",");
        for (ChatListener l : listeners) {
            l.updateUserList(users);
        }
    }

    public void run() {
        receive();
    }

    public void receive() {
        String msg = input.nextLine();
        while (!msg.equals(ProtocolStrings.CLOSE)) {
            String serverCommand = msg.split("#")[0];


            String cs = ClientState;
            switch (cs) {

                case "CONNECTING":
                    if (serverCommand.concat("#").equals(ProtocolStrings.ONLINE)) {
                        ClientState = "ISONLINE";
                        notifyListenersOfMsg(msg);
                        notifyListenersOfNewList(msg);
                    } else {
                    }
                    break;
                case "ISONLINE":
                    if (serverCommand.concat("#").equals(ProtocolStrings.ONLINE)) {
                        notifyListenersOfNewList(msg);
                    } else {
                    }
                    notifyListenersOfMsg(msg);
                    break;
                case "DISCONNECTING":
                    if (!serverCommand.concat("#").equals(ProtocolStrings.CLOSE)) {
                    } else {
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid command: " + serverCommand);
            }
            msg = input.nextLine();
        }
        try {
            socket.close();
            input.close();
            input = null;
        } catch (IOException ex) {
            Logger.getLogger(ChatClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
