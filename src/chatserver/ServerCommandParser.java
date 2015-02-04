
package chatserver;
//Author: nikos
import chatserver.ClientHandler;
import chatserver.ChatServer;
import static chatserver.ChatServer.chatLogger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import shared.ProtocolStrings;

/**
 *
 * @author ksptsinplanet
 */
public class ServerCommandParser {

    ClientHandler ch;
    ChatServer mainServer;

    public ServerCommandParser(ClientHandler ch, ChatServer mainServer) {
        this.ch = ch;
        this.mainServer = mainServer;
    }

    public void analyseCommand(String message) {
        String[] commandArray = message.split("#");
        switch (commandArray[0]) {
            case "CONNECT":
                handleCONNECTcommand(commandArray[1]);
                break;
            case "SEND":
                handleSENDcommand(commandArray[1], commandArray[2]);
                break;
            case "CLOSE":
                handleCLOSEcommand();
                break;
            default:
                chatLogger.log(Level.SEVERE, String.format(ch.getName() + " got an invalid command: " + commandArray[0]) + "from " + ch.clientUsername);
                try {
                    ch.s.close();
                } catch (IOException ex) {
                    chatLogger.log(Level.SEVERE, " at " + ch.getName() + " ", ex);
                }
                chatLogger.log(Level.INFO, ch.getName() + "closed a Connection");

        }

    }

    public void handleCLOSEcommand() {
        //send CLOSE# reply
        ch.send(ProtocolStrings.CLOSE);

        //remove clienthandler and user from HashMap
        mainServer.removeUserClientFromHashMap(ch.clientUsername);

        //update file holding online users number
        int size = mainServer.clientHandlersMap.size();
        System.out.println("#### size: " + size);
        File file = new File("onlineUsers.txt");
        FileWriter fw;
        try {
            fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(size);
            pw.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerCommandParser.class.getName()).log(Level.SEVERE, null, ex);
        }

        //sent a message to all online uses, get all users from hashmap
        ArrayList<String> onlineUsersList = new ArrayList<>(mainServer.clientHandlersMap.keySet());
        String newlist = "";
        int i = 1;
        for (String l : onlineUsersList) {
            if (i == onlineUsersList.size()) {
                newlist += l;
            } else {
                newlist = newlist + l + ",";
            }
            i++;
        }
        mainServer.sendMessageToClients(onlineUsersList, ProtocolStrings.ONLINE + newlist);
    }

    public void handleSENDcommand(String recipients, String tweet) {
        //get the sender
        String sender = ch.clientUsername;

        //get the recipient(s) and the message(tweet)
        ArrayList<String> recipientsList = new ArrayList<>();
        if (recipients.equals("*")) {
            //get all online users
            recipientsList = new ArrayList<>(mainServer.clientHandlersMap.keySet());

        } else {//get recipient parameters from command
            String[] recipientsAsArray = recipients.split(",");
            recipientsList.addAll(Arrays.asList(recipientsAsArray));
        }
        mainServer.sendMessageToClients(recipientsList, ProtocolStrings.MESSAGE + sender + "#" + tweet);

    }

    public void handleCONNECTcommand(String username) {
        // set user name 
        ch.clientUsername = username;

        // add to hashmap in mainserver
        mainServer.addUserClientToHashMap(username, ch);

        //update file holding online users number
        int size = mainServer.clientHandlersMap.size();
        File file = new File("onlineUsers.txt");
        FileWriter fw;
        try {
            fw = new FileWriter(file);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(size);
            System.out.println("###########" + size);
            pw.close();
        } catch (IOException ex) {
            Logger.getLogger(ServerCommandParser.class.getName()).log(Level.SEVERE, null, ex);
        }

        //sent a message to all online uses, get all users from hashmap
        ArrayList<String> onlineUsersList = new ArrayList<>(mainServer.clientHandlersMap.keySet());
        String newlist = "";
        int i = 1;
        for (String l : onlineUsersList) {
            if (i == onlineUsersList.size()) {
                newlist += l;
            } else {
                newlist = newlist + l + ",";
            }
            i++;
        }
        mainServer.sendMessageToClients(onlineUsersList, ProtocolStrings.ONLINE + newlist);

    }
}
