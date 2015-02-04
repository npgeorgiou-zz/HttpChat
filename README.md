# HttpChat

This is an exercise in building a Java server and using the HTTP protocol for client-server communication.


## Getting Started
If you want to download it and see how it works,  you can simply clone the seed repository.


### How it works
The seed is 2 in 1: A Java webserver that hosts a page from which you can download the chat client, and a Java chatserver, which
listens for http requests from the chat clients, connects, and responds (redistributes) messages back to the appropriate online 
clients, until a close command is sent, which maked the server close the connection with that client.

This client-server interaction happens via a custom protocol which builds on top of the HTTP protocol. You can observe this
protocol in the file 'ProtocolStrings'.


### Run the Application
Run the 2 .bat files to start the web and chat server. Before doing so, remember to change the IP to "localhost".
