package server;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import common.Message;
import common.MessageFactory;
import common.MessageValidator;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final RoomManager roomManager;
    private final AIService aiService;

    private volatile boolean active = false;
    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    public ClientHandler(Socket socket,
                         ConcurrentHashMap<String, ClientHandler> clients,
                         RoomManager roomManager,
                         AIService aiService) {

        this.socket = socket;
        this.clients = clients;
        this.roomManager = roomManager;
        this.aiService = aiService;
    }

    @Override
    public void run() {
        log("START", "Handling new client");

        try {
            setupStreams();

            if (!registerClient())
                return;

            active = true;
            processMessages();

        } catch (IOException e) {
            log("ERROR", "Connection issue: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void setupStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
    }

    // ==============================
    // REGISTRATION
    // ==============================
    private boolean registerClient() throws IOException {

        String raw = in.readLine();
        if (raw == null) return false;

        Message initMsg = Message.fromJson(raw);

        if (initMsg == null || initMsg.getSender() == null) {
            send(MessageFactory.error("Invalid initialization message."));
            return false;
        }

        String requestedName = initMsg.getSender().trim();

        if (requestedName.isEmpty()) {
            send(MessageFactory.error("Username cannot be empty."));
            return false;
        }

        ClientHandler old = clients.put(requestedName, this);

        if (old != null && old != this) {
            old.forceDisconnect();
        }

        username = requestedName;

        roomManager.joinRoom("global", this);

        send(MessageFactory.system("Welcome " + username + "!"));
        broadcastToRoom("global",
                MessageFactory.system("[ROOM global] " + username + " joined"));

        return true;
    }

    // ==============================
    // MAIN LOOP
    // ==============================
    private void processMessages() throws IOException {

        String input;

        while (active && (input = in.readLine()) != null) {

            Message msg = Message.fromJson(input);

            if (msg == null || !MessageValidator.isValid(msg)) {
                send(MessageFactory.error("Invalid message format."));
                continue;
            }

            switch (msg.getType()) {

                case CHAT -> handleChat(msg);
                case PRIVATE -> handlePrivate(msg);
                case COMMAND -> handleCommand(msg);
                case PING -> send(MessageFactory.pong());

                default -> send(MessageFactory.error("Unsupported message type."));
            }
        }

        log("DISCONNECTED", username);
    }

    // ==============================
    // 🔥 CHAT WITH SAFE AI PIPELINE
    // ==============================
    private void handleChat(Message msg) {

        String content = msg.getContent();
        String room = roomManager.getClientRoom(this);

        if (content == null || content.isEmpty()) return;

        aiService.isToxicAsync(content)
                .thenAccept(isToxic -> {

                    if (!active) return;

                    if (isToxic) {
                        sendSafe(MessageFactory.error("⚠️ Message blocked (toxic)"));
                        return;
                    }

                    if (room != null) {
                        roomManager.addMessage(room, username + ": " + content);
                    }

                    broadcast(MessageFactory.chat(username, content));
                })
                .exceptionally(e -> {
                    log("AI", "Toxicity check failed: " + e.getMessage());

                    // fallback: allow message instead of blocking system
                    broadcast(MessageFactory.chat(username, content));
                    return null;
                });
    }

    // ==============================
    // COMMANDS
    // ==============================
    private void handleCommand(Message msg) {

        String command = msg.getCommand();
        if (command == null) {
            send(MessageFactory.error("Invalid command."));
            return;
        }

        switch (command) {

            case "LIST" -> sendUserList();
            case "NAME" -> changeUsername(msg.getContent());
            case "EXIT" -> {
                send(MessageFactory.system("Goodbye!"));
                cleanup();   // 🔥 immediately close everything
                return;      // 🔥 exit thread instantly
            }
            case "JOIN" -> joinRoom(msg.getContent());
            case "LEAVE" -> leaveRoom();
            case "ROOMS" -> listRooms();
            case "SUMMARIZE" -> summarizeRoom(msg.getContent());

            default -> send(MessageFactory.error("Unknown command."));
        }
    }

    // ==============================
    // 🔥 SAFE SUMMARIZE
    // ==============================
    private void summarizeRoom(String input) {

        int n = 10;

        try {
            if (input != null && !input.isEmpty()) {
                n = Integer.parseInt(input.trim());
            }
        } catch (Exception e) {
            send(MessageFactory.error("Usage: /summarize <N>"));
            return;
        }

        String room = roomManager.getClientRoom(this);

        if (room == null) {
            send(MessageFactory.error("Not in a room"));
            return;
        }

        var messages = roomManager.getRecentMessages(room, n);

        if (messages.isEmpty()) {
            send(MessageFactory.system("No messages to summarize"));
            return;
        }

        send(MessageFactory.system("⏳ Generating summary..."));

        aiService.summarizeAsync(messages)
                .thenAccept(summary -> {
                    if (!active) return;
                    sendSafe(MessageFactory.system("🧠 Summary:\n" + summary));
                })
                .exceptionally(e -> {
                    sendSafe(MessageFactory.error("Summary failed."));
                    return null;
                });
    }

    // ==============================
    // PRIVATE MESSAGE
    // ==============================
    private void handlePrivate(Message msg) {

        String target = msg.getReceiver();
        ClientHandler receiver = clients.get(target);

        if (receiver == null) {
            send(MessageFactory.error("User not found: " + target));
            return;
        }

        Message privateMsg = MessageFactory.privateMsg(username, target, msg.getContent());

        receiver.send(privateMsg);
        send(privateMsg);
    }

    // ==============================
    // BROADCAST
    // ==============================
    private void broadcastToRoom(String room, Message msg) {
        Set<ClientHandler> members = roomManager.getRoomMembers(room);

        if (members == null) return;

        for (ClientHandler client : members.toArray(new ClientHandler[0])) {
            client.send(msg);
        }
    }

    private void broadcast(Message msg) {
        String room = roomManager.getClientRoom(this);
        if (room != null) {
            broadcastToRoom(room, msg);
        }
    }

    // ==============================
    // SAFE SEND (important)
    // ==============================
    private synchronized void send(Message msg) {
        try {
            if (out != null && msg != null && !socket.isClosed()) {
                out.println(msg.toJson());
                out.flush();
            }
        } catch (Exception e) {
            cleanup();
        }
    }

    private void sendSafe(Message msg) {
        if (active) {
            send(msg);
        }
    }

    public void forceDisconnect() {
        cleanup();
    }

    private void cleanup() {

        if (!cleanedUp.compareAndSet(false, true)) return;

        active = false;

        try {
            if (username != null) {

                String room = roomManager.getClientRoom(this);

                if (clients.remove(username, this)) {

                    if (room != null) {
                        broadcastToRoom(room,
                                MessageFactory.system("[ROOM " + room + "] " + username + " left"));
                    }

                    roomManager.removeClient(this);
                }
            }

            if (in != null) in.close();
            if (out != null) out.close();

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException ignored) {
        }
    }

    private void sendUserList() {
        send(MessageFactory.system("Online Users: " + String.join(", ", clients.keySet())));
    }

    private synchronized void changeUsername(String newName) {

        if (newName == null || newName.trim().isEmpty()) {
            send(MessageFactory.error("Invalid username."));
            return;
        }

        newName = newName.trim();

        if (clients.putIfAbsent(newName, this) != null) {
            send(MessageFactory.error("Username already taken."));
            return;
        }

        String oldName = username;

        clients.remove(oldName, this);
        username = newName;

        broadcast(MessageFactory.system(oldName + " is now known as " + username));
    }

    private void joinRoom(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            send(MessageFactory.error("Room name cannot be empty."));
            return;
        }

        roomManager.joinRoom(roomName.trim(), this);
        send(MessageFactory.system("Joined room: " + roomName));
    }

    private void leaveRoom() {
        roomManager.joinRoom("global", this);
        send(MessageFactory.system("Returned to global room"));
    }

    private void listRooms() {
        send(MessageFactory.system("Rooms: " + roomManager.getAllRooms()));
    }

    private void log(String tag, String msg) {
        System.out.println("[CLIENT " + (username != null ? username : "?") + "] [" + tag + "] " + msg);
    }

    public void closeConnection() {
        cleanup();
    }
}