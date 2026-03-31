# 🚀 Distributed Chat System

A production-style distributed chat system built in Java using TCP sockets, multithreading, and a custom message protocol.

This project goes beyond a basic chat app by implementing **fault tolerance, auto-reconnect, session recovery, and concurrent client handling**, making it a strong demonstration of backend system design.

---

## 📌 Overview

This system follows a **client-server architecture** where multiple clients communicate through a central server over TCP.

Key focus areas:

* Concurrency and thread safety
* Fault tolerance and recovery
* Clean protocol design
* Maintainable and modular architecture

---

## ⚡ Key Features

### 🧱 Core System

* Multi-client support using `ExecutorService`
* Thread-safe client registry using `ConcurrentHashMap`
* Clean client lifecycle management (connect, disconnect, cleanup)

---

### 💬 Messaging System

* Broadcast messaging
* Private messaging (`/msg`)
* Structured message model:

  * `type`, `sender`, `receiver`, `content`, `command`
* Centralized message creation (`MessageFactory`)
* Message validation layer (`MessageValidator`)

---

### 🧭 Command System

* `/list` → list active users
* `/msg <user> <message>` → private messaging
* `/name <newname>` → change username
* `/exit` → disconnect

---

### ❤️ Fault Tolerance (Advanced)

#### 🔄 Auto-Reconnect Mechanism

* Client automatically reconnects on connection loss
* Exponential backoff strategy (2s → 4s → 8s...)
* Max retry limit to prevent infinite loops

#### 💓 Heartbeat Monitoring

* Client sends `PING` every 3 seconds
* Server responds with `PONG`
* Client detects failure after 15 seconds

#### 🔁 Session Recovery

* Username reused after reconnect
* Server replaces stale sessions safely
* Prevents duplicate users and ghost clients

---

### 🧵 Concurrency Design

* Thread pool (`ExecutorService`) for scalable client handling
* Separate client-side threads:

  * Listener thread
  * Heartbeat thread
  * Monitor thread
* Safe shared state using concurrent collections

---

### 📡 Protocol Design

* Custom JSON-based message protocol
* Message types:

  * `CHAT`, `PRIVATE`, `SYSTEM`, `COMMAND`, `ERROR`, `PING`, `PONG`
* Clean separation between commands and message types

---

## 🛠️ Technology Stack

* **Language:** Java
* **Networking:** TCP (Socket, ServerSocket)
* **Concurrency:** Thread, ExecutorService
* **Data Structures:** ConcurrentHashMap
* **I/O:** BufferedReader, PrintWriter

---

## 📁 Project Structure

```
java-distributed-chat-system/
│
├── server/
│   ├── Server.java
│   └── ClientHandler.java
│
├── client/
│   └── Client.java
│
├── common/
│   ├── Message.java
│   ├── MessageType.java
│   ├── MessageFactory.java
│   └── MessageValidator.java
│
└── README.md
```

---

## 🧠 Architecture Overview

```
        +-------------------+
        |      Clients      |
        | (Multiple Nodes)  |
        +---------+---------+
                  |
                  | TCP
                  |
        +---------v---------+
        |       Server      |
        |  Thread Pool      |
        |  (ExecutorService)|
        +---------+---------+
                  |
        +---------v---------+
        | Concurrent Client |
        |   Registry Map    |
        +-------------------+
```

---

## ▶️ Running the Application

### 1. Compile

```bash
javac server/*.java
javac client/*.java
javac common/*.java
```

---

### 2. Start Server

```bash
java server.Server
```

---

### 3. Start Clients (multiple terminals)

```bash
java client.Client
```

---

## 💻 Usage

* Enter username on startup
* Send messages directly
* Use commands:

  * `/list`
  * `/msg user message`
  * `/name newname`
  * `/exit`

---

## 🧪 Testing Scenarios

### ✅ Functional

* Multi-client messaging
* Private messaging
* Username changes

### 🔥 Fault Tolerance

* Server crash and restart → clients auto-reconnect
* Network failure simulation → retry with backoff
* Multiple clients reconnect simultaneously

### 🧹 Stability

* No duplicate users after reconnect
* No ghost clients
* No thread leaks

---

## ⚠️ Limitations

* No persistent message storage
* No group chat / rooms (planned)
* No GUI (CLI-based interaction)
* No encryption (plain TCP)

---

## 🔮 Future Improvements

* Group chat / rooms
* Message persistence (file/database)
* GUI (JavaFX/Swing)
* Protocol versioning
* Authentication system

---

## 🏁 Key Learnings

* Designing fault-tolerant distributed systems
* Handling concurrency safely in Java
* Building resilient client-server communication
* Managing connection lifecycle and recovery

---

## 👤 Author

**Satvik Miglani**
