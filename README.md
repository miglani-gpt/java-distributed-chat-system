# 🚀 Distributed Chat System

A real-time distributed chat system built with **Java**, **TCP sockets**, **multithreading**, and a structured message protocol.  
The project features a modern **JavaFX client**, room-based messaging, private chat, automatic reconnect, heartbeat monitoring, and an optional **Python AI backend** for toxicity detection and conversation summarization.

---

## ✨ Highlights

- 💬 Real-time multi-client chat  
- 🎨 Modern JavaFX UI with chat bubbles  
- 👥 Live user list panel  
- 🏠 Room-based messaging  
- 🔁 Automatic reconnect with exponential backoff  
- ❤️ Heartbeat monitoring (PING/PONG)  
- 🧵 Thread-safe server architecture  
- 🧠 Structured message protocol (no fragile string parsing)  
- 🤖 AI-powered toxicity filtering & summarization  
- 🧪 Automated integration and protocol tests  

---

## 🧠 Architecture

```
JavaFX Client
      ↓ TCP
Java Server
      ↓
ClientHandler + RoomManager + AIService
      ↓
Python AI Backend (FastAPI)
```

---

## 📦 Project Layers

- `common` → Message protocol, types, validation  
- `server` → Networking, concurrency, rooms, AI integration  
- `client` → JavaFX UI and client networking  
- `main.py` → AI backend (FastAPI)  
- `tests` → Integration and protocol testing  

---

## ⚡ Features

### 💬 Chat System
- Public room-based messaging  
- Private messaging using `/msg`  
- Username management using `/name`  
- Active users list using `/list`  

### 🏠 Room System
- Join or create rooms using `/join`  
- Leave room using `/leave`  
- List rooms using `/rooms`  
- Room-based message isolation  
- Recent message history support  

### 🤖 AI Features
- Toxicity detection for chat messages  
- Chat summarization using `/summarize N`  
- Asynchronous AI calls (non-blocking)  
- Safe fallback if AI backend is offline  

### ❤️ Reliability
- Heartbeat system using PING/PONG  
- Auto-reconnect with exponential backoff  
- Clean connection lifecycle  
- Thread-safe shared state  

### 🎨 UI (JavaFX)
- Chat bubbles (left/right alignment)  
- System and error message styling  
- Auto-scroll to latest messages  
- Fade-in animations  
- Live user list panel  
- Keyboard support (Enter to send)  

---

## 🧭 Commands

| Command | Description |
|--------|------------|
| `/list` | Show active users |
| `/msg user message` | Send private message |
| `/name newname` | Change username |
| `/join room` | Join/create a room |
| `/leave` | Return to global room |
| `/rooms` | List available rooms |
| `/summarize N` | Summarize last N messages |
| `/exit` | Disconnect |

---

## 🛠️ Tech Stack

- Java 17+  
- JavaFX  
- TCP Sockets  
- Multithreading (ExecutorService)  
- ConcurrentHashMap  
- Maven  
- JUnit 5  
- FastAPI (Python AI backend)  

---

## 📁 Project Structure

```
java-distributed-chat-system/
├── pom.xml
├── main.py
├── src/
│   ├── main/java/
│   │   ├── client/
│   │   ├── server/
│   │   └── common/
│   └── test/java/integration/
```

---

## 🚀 Running the Project

### 1. Build the Project
```
mvn clean package
```

### 2. Start AI Backend
```
uvicorn main:app --port 8000
```

Check:
```
curl http://localhost:8000/health
```

### 3. Start Server
```
java -cp target/classes server.Server
```

### 4. Start Client
```
mvn javafx:run
```

Run multiple clients for multi-user testing.

---

## 🤖 AI API Endpoints

### Toxicity Check
```
curl -X POST http://localhost:8000/toxicity \
-H "Content-Type: application/json" \
-d '{"text":"you are stupid"}'
```

Response:
```
{"toxic": true}
```

---

### Summarization
```
curl -X POST http://localhost:8000/summarize \
-H "Content-Type: application/json" \
-d '{"messages":["hello","how are you","fine"]}'
```

Response:
```
{"summary":"hello | how are you | fine"}
```

---

### Health Check
```
curl http://localhost:8000/health
```

---

## 🧪 Testing

Run all tests:
```
mvn test
```

### Test Coverage
- Message parsing and validation  
- Public chat broadcast  
- Private messaging  
- Room join and isolation  
- Command handling  
- Heartbeat system  
- Exit handling  
- AI toxicity filtering  
- AI summarization  
- Edge cases and invalid input  

---

## ⚠️ Limitations

- No database persistence  
- No authentication system  
- No encryption (plain TCP)  
- AI is rule-based (not ML-powered)  
- UI tests not automated  

---

## 🔮 Future Improvements

- Database persistence (MongoDB / PostgreSQL)  
- Authentication system  
- File sharing  
- WebSocket support  
- Web/mobile client  
- Real ML-based AI models  
- Message history UI  
- Notifications  

---

## 🧠 Key Learnings

- Distributed system design  
- Multithreading and concurrency  
- Socket programming in Java  
- Building real-time UI with JavaFX  
- Designing structured protocols  
- Integrating Java backend with Python AI  
- Writing automated integration tests  

---

## 👤 Author

Satvik Miglani