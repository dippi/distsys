# Distributed System 2014/15 <br/> Project on Secure Group Communication

The task was to implement a simple secure group messaging system to showcase the process of key distribution using the Centralized Flat Table approach.

## Secure Group Communication

By secure group communication we mean the possibility of exchanging encrypted messages within a set of authorized clients, that may join or leave the group.

The requirements are:
* All the members of the group should be able to send and receive messages.
* No one outside the group should be able to decrypt the messages.
* When a client joins the group, it shouldn't be able to decrypt previous messages (backward secrecy).
* When a client leaves the group, it shouldn't be able to decrypt following messages (forward secrecy).

To fulfill these requirements there are different cryptographic approaches, from individual symmetric or asymmetric encryption that require a large number of keys and are inefficient in terms of computations and bandwidth, to symmetric encryption with a single shared key that introduces the problem of key distribution.

## Centralized Flat Table

Centralized Flat Table is a key distribution technique that uses a single shared Data Encryption Key (DEK) for group communication and 2*log(N) Key Encryption Keys (KEKs) for secure and efficient keys redistribution, where N is the maximum number of clients supported.

Explanation:
* The keys distribution server initialize a DEK and a table of KEKs of 2 rows and log(N) columns.
* Whenever a client asks for joining the group an integer ID is assigned to it.
* Using the binary representation of the ID as a selector, log(N) KEKs are associated to the client.
* The server regenerate the DEK and the log(N) KEKs associated to the new user.
* The new DEK is encoded separately with the log(N) remaining KEKs and sent to the previous clients.
* Each client is able to decrypt DEK from at least one of the encryption since all the IDs differs for at least a bit.
* Each new KEK is encoded first with the corresponding previous one and then with the new DEK.
* Only the clients that used to know those KEKs and are still part of the group are now able to decrypt the new KEKs.
* The new client is accepted to the group and receives the new DEK and KEKs with asymmetric encryption.
* Whenever a client leaves the DEK and its associated KEKs are regenerate and distributed in the same way.

## Implementation Details

The implementation is written in Java 7 and it is organized in two packages: `it.polimi.distsys.server` and `it.polimi.distsys.client`.

The server package contains four classes:
* Main: the entry point for the program, that lift the server and waits input from stdin to shut everything down.
* Server: the core class, that listen for socket connections and controls keys distribution and messages broadcasting.
* ClientHandler: is a utility class to handle multi-threaded read and writes from multiple client connections.
* EncryptionManager: is a utility class to manage the keys generation and encryption.

The client package contains three classes:
* Main: the entry point for the program, that launch the client and waits input from stdin to send messages in the group chat.
* Client: the core class, that connects to the server and exchange messages and keys
* EncryptionManager: is a utility class to manage keys and handle encryption and decryption.

Considering the goal of the project some simplification have been made:
* All the transmissions over socket are textual and binary messages are Base64 encoded.
* All the communications are based on a simple ad-hoc "protocol" (or better convention).
* Corner cases of multiple clients simultaneous (dis)connection are not handled.
* Corner cases of messages sent/received during keys redistribution are not handled.
* Process and links are assumed reliable.
