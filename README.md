# 🚀 Distributed Chat System (JavaFX)

A **production-style distributed chat system** built in Java using TCP sockets, multithreading, and a structured message protocol, featuring a **modern JavaFX GUI**.

This project demonstrates **real-world backend + frontend system design**, including fault tolerance, concurrency, session recovery, and real-time UI updates.

---

## 📌 Highlights

* 💬 Real-time multi-client chat system
* 🎨 Modern JavaFX GUI with chat bubbles
* 👥 Live user list panel
* 🏠 Room-based messaging system
* 🔁 Automatic reconnection with exponential backoff
* ❤️ Heartbeat-based failure detection (PING/PONG)
* 🧵 Thread-safe concurrent architecture
* 🧠 Structured message protocol (no string parsing)

---

## 🧠 Architecture

```text
        +-------------------+
        |      Clients      |
        | (JavaFX GUI)      |
        +---------+---------+
                  |
                  | TCP
                  |
        +---------v---------+
        |       Server      |
        |  Thread Pool      |
        +---------+---------+
                  |
        +---------v---------+
        | Concurrent Client |
        |   Registry Map    |
        +-------------------+
```

---

## ⚡ Features

### 🧱 Core System

* Multi-client handling using `ExecutorService`
* Thread-safe client registry (`ConcurrentHashMap`)
* Clean connection lifecycle management

---

### 💬 Messaging

* Public room-based messaging
* Private messaging (`/msg`)
* Structured message model:

  * `type`, `sender`, `receiver`, `content`, `command`
* Centralized message creation (`MessageFactory`)

---

### 🏠 Room System

* Dynamic room creation (`/join roomName`)
* Automatic room switching
* Room isolation (messages visible only within room)
* `/rooms` command to list rooms

---

### 🧭 Commands

| Command             | Description           |
| ------------------- | --------------------- |
| `/list`             | Show active users     |
| `/msg user message` | Private message       |
| `/name newname`     | Change username       |
| `/join room`        | Join/create a room    |
| `/leave`            | Return to global room |
| `/rooms`            | List available rooms  |
| `/exit`             | Disconnect            |

---

### 🎨 GUI Features (JavaFX)

* 💬 Chat bubbles (left/right aligned)
* 🕒 Message timestamps
* 👥 Live user list panel
* 🎯 Message styling (system / private / normal)
* ⚡ Smooth auto-scroll
* ✨ Message fade-in animation
* 🧠 Keyboard support (Enter to send)
* 🎨 Dark modern UI theme

---

### ❤️ Fault Tolerance

#### 🔄 Auto-Reconnect

* Automatic reconnection on failure
* Exponential backoff: `2s → 4s → 8s`
* Retry limit to prevent infinite loops

#### 💓 Heartbeat System

* Client sends `PING` every 3 seconds
* Server replies with `PONG`
* Detects connection loss within 15 seconds

#### 🔁 Session Recovery

* Username reuse after reconnect
* Old sessions safely replaced
* Prevents ghost users

---

### 🧵 Concurrency Design

* Thread pool for scalable server handling
* Client uses:

  * Listener thread
  * Heartbeat thread
  * Monitor thread
* Thread-safe shared state

---

### 📡 Protocol Design

* Custom structured message protocol
* Message types:

  * `CHAT`, `PRIVATE`, `SYSTEM`, `COMMAND`, `ERROR`, `PING`, `PONG`
* Eliminates fragile string parsing

---

## 🛠️ Tech Stack

* **Language:** Java
* **UI:** JavaFX
* **Networking:** TCP (Socket, ServerSocket)
* **Concurrency:** Thread, ExecutorService
* **Data Structures:** ConcurrentHashMap

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
│   ├── Client.java
│   ├── ChatFX.java
│   ├── ChatView.java
│   ├── ChatController.java
│   └── style.css
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

## ▶️ Running the Application

### 1️⃣ Compile

```bash
javac --module-path /usr/share/openjfx/lib \
      --add-modules javafx.controls,javafx.fxml \
      */*.java
```

### 2️⃣ Start Server

```bash
java server.Server
```

### 3️⃣ Start Client(s)

```bash
java --module-path /usr/share/openjfx/lib \
     --add-modules javafx.controls,javafx.fxml \
     client.ChatFX
```

---

## 🧪 Testing

### ✅ Functional

* Multi-client chat
* Private messaging
* Room switching

### 🔥 Fault Tolerance

* Server crash → auto-reconnect
* Network drop → retry logic
* Multiple reconnects handled safely

---

## ⚠️ Limitations

* No message persistence
* No authentication
* No encryption (plain TCP)

---

## 🔮 Future Improvements

* 💾 Message persistence (database)
* 🔐 Authentication system
* 📁 File sharing
* 📱 Mobile/web client
* 🌐 WebSocket support

---

## 🧠 Key Learnings

* Designing distributed systems
* Managing concurrency in Java
* Building real-time UI with JavaFX
* Handling fault tolerance and recovery

---

## 👤 Author

**Satvik Miglani**
