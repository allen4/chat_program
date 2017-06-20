# chat_program

This java project creates a chat system using the client/server architectural model.  Client and server may run on different hosts in the network.

Chat clients are java programs which can connect to a chat server.

The chat server is a java application which can accept multiple incoming TCP connections.

This program run by command line, each line of input is interpreted by the client as either a command or a message.  If the line of input starts with a hash character "#" then it is interpreted as a command, otherwise it is iterpreted as a message.

All the connections between client and server are secured by TSL protocal and AES encrption.
