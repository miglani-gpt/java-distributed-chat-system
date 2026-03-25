# Distributed Chat System

A Java-based client-server application that demonstrates TCP socket communication, clean separation of concerns, and foundational backend system design.

---

## Overview

This project implements a basic chat system using a client-server architecture over TCP. A client connects to a server, sends messages via standard input, and the server processes and logs those messages in real time.

The implementation focuses on correctness, clarity, and maintainability rather than feature complexity.

---

## Features

* TCP communication using Java sockets
* Client-server architecture
* Console-based message input
* Real-time message handling on the server
* Graceful client disconnection using a command (`exit`)
* Structured logging for observability
* Automatic resource management using try-with-resources
* Basic error handling for connection and runtime failures

---

## Technology Stack

* Language: Java
* Networking: Socket, ServerSocket (TCP)
* I/O: BufferedReader, PrintWriter

---

## Project Structure

```
java-distributed-chat-system/
│
├── server/
│   └── Server.java
│
├── client/
│   └── Client.java
│
├── common/
│
└── README.md
```

---

## Communication Model

```
Client → Socket → ServerSocket → Server
```

* The client sends messages using a PrintWriter
* The server reads messages using a BufferedReader
* Communication is line-oriented and blocking

---

## Running the Application

### Compile

```
javac server/Server.java
javac client/Client.java
```

### Start the Server

```
java server.Server
```

### Start the Client

```
java client.Client
```

---

## Usage

* Enter messages in the client terminal
* Messages are displayed on the server
* Enter `exit` to terminate the client connection

---

## Testing

The following scenarios have been validated:

* Normal message exchange between client and server
* Graceful client disconnection
* Abrupt termination of client (process kill)
* Client connection failure when server is unavailable

---

## Limitations

* Supports a single client connection
* Uses blocking I/O
* No message routing or broadcasting

---

## Design Considerations

* Uses try-with-resources to ensure deterministic cleanup of sockets and streams
* Keeps client and server responsibilities clearly separated
* Uses structured logging to improve debuggability
* Maintains minimal complexity while ensuring correctness

---

## Author

Satvik Miglani

