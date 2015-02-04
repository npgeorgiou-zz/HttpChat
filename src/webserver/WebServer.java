package webserver;
//Author: nikos
import chatserver.ClientHandler;
import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.Utils;

public class WebServer extends Thread {

    public static Logger webLogger;

    private static boolean keepRunning = true;
    private static final Properties properties = Utils.initProperties("server.properties");
    static String NL = System.getProperty("line.separator");
    static final String BEGIN = "<!DOCTYPE html>\n<HTML>\n<HEAD>\n<TITLE>Simple HTTP Server</TITLE></HEAD>\n<BODY>";
    static final String END = "</BODY>\n</HTML>";

    Socket socket = null;

    public WebServer(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        //create input and output streams
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());) {

            //see the uri that came in
            String request = in.readLine();
            if (request == null) {
                return;
            }
            String[] firstLineItems = request.split(" ");
            String URI = firstLineItems[1];
            webLogger.log(Level.INFO, "User asked for: " + URI);

            if (URI.equals("/")) {
                URI = "/index.html";
            }
            //see if the user request is valid or no
            String filePath = "";
            int statusCode = 0;
            String status = null;
            File file;

            filePath = "C:\\Users\\DBAdmin\\Desktop\\HttpChat" + URI;
            file = new File(filePath);
            if (file.isFile()) {
                statusCode = 200;
                status = "HTTP/1.1 200 OK" + NL;
            } else {
                // Couldnt find file, bad request, sent him error page
                webLogger.log(Level.INFO, "User requested something that I dont have");
                filePath = "C:\\Users\\DBAdmin\\Desktop\\HttpChat\\error.html";
                file = new File(filePath);
                statusCode = 301;
                status = "HTTP/1.1 301 IM WATCHING YOU" + NL;
            }

            //see if he asks for one of the dynamic pages
            boolean isFile = true;
            String fileHTML = "";
            if (URI.equals("/log.html") || URI.equals("/users.html")) {//he did
                isFile = false;
                BufferedReader filereader = new BufferedReader(new FileReader(file));
                String line = null;
                StringBuilder stringBuilderForHTML = new StringBuilder();
                while ((line = filereader.readLine()) != null) {
                    stringBuilderForHTML.append(line);
                    stringBuilderForHTML.append(NL);
                }
                fileHTML = stringBuilderForHTML.toString();

                //replace dummy text with dynamic content from files
                String dynamicText;
                if (URI.equals("/log.html")) {
                    file = new File("C:\\Users\\DBAdmin\\Desktop\\HttpChat\\chatServerLog.txt");
                    filereader = new BufferedReader(new FileReader(file));
                    line = null;
                    StringBuilder stringBuilderForTXT = new StringBuilder();
                    String color = "";

                    //coloring of text
                    int x = 2;
                    while ((line = filereader.readLine()) != null) {
                        if ((x % 2) == 0) {
                            color = "#1975D1";
                        } else {
                            color = "#00FF00";
                        }
                        stringBuilderForTXT.append("<span style=\"color:" + color + "\">" + line + "</span>" + "</br>");

                        x++;
                    }

                    dynamicText = stringBuilderForTXT.toString();
                    fileHTML = fileHTML.replace("DUMMY TEXT", dynamicText);
                } else {//else it is /users.html

                    file = new File("C:\\Users\\DBAdmin\\Desktop\\HttpChat\\onlineUsers.txt");
                    Scanner scanner = new Scanner(file);
                    // if there is somethign in the file, read it, otherwise put 0.
                    if (scanner.hasNext()) {
                        dynamicText = Integer.toString(scanner.nextInt());
                    } else {
                        dynamicText = Integer.toString(0);
                    }
                    
                    fileHTML = fileHTML.replace("DUMMY TEXT", dynamicText);
                }

            }

            //send response
            sendResponse(out, status, statusCode, isFile, file, URI, fileHTML);

        } catch (IOException ex) {
            webLogger.log(Level.SEVERE, "Could not create input and output streams", ex);
        }
        //close the socket. Not necessary (run method stop->thread stop->object to garbage collector) but here for good order
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(WebServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        webLogger.log(Level.INFO, "Closed socket");

    }

    public void sendResponse(DataOutputStream out, String status, int statusCode, boolean isFile, File file, String URI, String fileHTML) throws IOException {

        //send status
        out.writeBytes(status);

        //send the appropriate response headers
        out.writeBytes("Connection: Close" + NL);
        if (URI.equals("/style.css")) {
            out.writeBytes("Content-Type: text/css" + NL);
        } else {
            out.writeBytes("Content-Type: text/html" + NL);
        }
        if (statusCode == 301) {
            out.writeBytes("Location: http://npgeor.cloudapp.net:8080/error.html" + NL);
        }
        if (URI.equals("/webServerLog.txt") || URI.equals("/documentation.pdf")|| URI.equals("/HttpChat.jar")|| URI.equals("/chatServerLog.txt")) {
            out.writeBytes("Content-Disposition: attachment; filename=\"C:\\Users\\DBAdmin\\Desktop\\HttpChat\\" + URI + NL);                                                         
        }
        
        //separator to indicate body start
        out.writeBytes(NL);

        //send body
        if (isFile == true) {
            byte[] bytesToSend = new byte[(int) file.length()];
            try {
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(bytesToSend, 0, bytesToSend.length);
            } catch (IOException ie) {
                ie.printStackTrace();
            }
            out.write(bytesToSend, 0, bytesToSend.length);
        } else {
            out.writeBytes(fileHTML);
        }

        //close output stream.
        out.close();
        webLogger.log(Level.INFO, "Have sent the requested page and closed output stream");
    }

    public static void stopServer() {
        //close the chatLogger too
        keepRunning = false;
        webLogger.log(Level.INFO, "Server stopped");
        Utils.closeLogger(WebServer.class.getName());
    }

    public static void main(String args[]) throws Exception {
        //initialize webLogger
        Utils.setLogFile(properties.getProperty("weblogFile"), WebServer.class.getName());
        webLogger = Utils.getLogger(properties.getProperty("weblogFile"), WebServer.class.getName());
        //set port and IP
        int port = Integer.parseInt(properties.getProperty("webPort"));
        String ip = properties.getProperty("serverIp");

        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ip, port));
            webLogger.log(Level.INFO, "HTTP Server started, listening on port: " + port);

            do {
                Socket socket = serverSocket.accept();
                webLogger.log(Level.INFO, "New Client " + socket.getInetAddress() + ":" + socket.getPort() + " connected");
                (new WebServer(socket)).start();
            } while (keepRunning);
        } catch (IOException ex) {
            webLogger.log(Level.SEVERE, "Couldnt create ServerSocket", ex);
        }
    }
}
