package server;

import common.Message;
import common.MessageFactory;
import common.MessageValidator;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final RoomManager roomManager;

    private volatile boolean active = false;
    private volatile boolean cleanedUp = false;

    public ClientHandler(Socket socket,
                         ConcurrentHashMap<String, ClientHandler> clients,
                         RoomManager roomManager) {
        this.socket = socket;
        this.clients = clients;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            System.out.println("[THREAD STARTED] Handling new client...");
            setupStreams();

            if (!registerClient()) return;

            active = true;
            processMessages();

        } catch (IOException e) {
            System.out.println("[ERROR] Client connection issue: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void setupStreams() throws IOException {
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
    }

    // ==============================
    // REGISTRATION
    // ==============================
    private boolean registerClient() throws IOException {

        String raw = in.readLine();
        System.out.println("[INIT RAW] " + raw);

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

        ClientHandler existing = clients.get(requestedName);

        if (existing != null) {
            System.out.println("[RECONNECT] Replacing old session for " + requestedName);
            existing.forceDisconnect();
        }

        username = requestedName;
        clients.put(username, this);

        // ✅ Join default room
        roomManager.joinRoom("global", this);

        send(MessageFactory.system("Welcome " + username + "!"));

        // 🔥 Notify global room
        broadcastToRoom("global",
                MessageFactory.system("[ROOM global]: " + username + " joined"));

        return true;
    }

    // ==============================
    // MAIN LOOP
    // ==============================
    private void processMessages() throws IOException {

        String input;

        while ((input = in.readLine()) != null) {

            if (!active) continue;

            System.out.println("[RECEIVED] " + username + ": " + input);

            Message msg = Message.fromJson(input);

            if (msg == null || !MessageValidator.isValid(msg)) {
                send(MessageFactory.error("Invalid message format."));
                continue;
            }

            switch (msg.getType()) {

                case CHAT:
                    broadcast(MessageFactory.chat(username, msg.getContent()));
                    break;

                case PRIVATE:
                    handlePrivate(msg);
                    break;

                case COMMAND:
                    handleCommand(msg);
                    break;

                case PING:
                    send(MessageFactory.pong());
                    break;

                default:
                    send(MessageFactory.error("Unsupported message type."));
            }
        }

        System.out.println("[DISCONNECTED] " + username);
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

            case "LIST":
                sendUserList();
                break;

            case "NAME":
                changeUsername(msg.getContent());
                break;

            case "EXIT":
                active = false;
                break;

            case "JOIN":
                joinRoom(msg.getContent());
                break;

            case "LEAVE":
                leaveRoom();
                break;

            case "ROOMS":
                listRooms();
                break;

            default:
                send(MessageFactory.error("Unknown command."));
        }
    }

    // ==============================
    // ROOM COMMANDS (UPGRADED)
    // ==============================
    private void joinRoom(String roomName) {

        if (roomName == null || roomName.trim().isEmpty()) {
            send(MessageFactory.error("Room name cannot be empty."));
            return;
        }

        String newRoom = roomName.trim();
        String oldRoom = roomManager.getClientRoom(this);

        // 🔥 Notify old room
        if (oldRoom != null) {
            broadcastToRoom(oldRoom,
                    MessageFactory.system("[ROOM " + oldRoom + "]: " + username + " left"));
        }

        roomManager.joinRoom(newRoom, this);

        send(MessageFactory.system("Joined room: " + newRoom));

        // 🔥 Notify new room
        broadcastToRoom(newRoom,
                MessageFactory.system("[ROOM " + newRoom + "]: " + username + " joined"));
    }

    private void leaveRoom() {

        String oldRoom = roomManager.getClientRoom(this);

        if (oldRoom != null) {
            broadcastToRoom(oldRoom,
                    MessageFactory.system("[ROOM " + oldRoom + "]: " + username + " left"));
        }

        roomManager.joinRoom("global", this);

        send(MessageFactory.system("Returned to global room"));

        broadcastToRoom("global",
                MessageFactory.system("[ROOM global]: " + username + " joined"));
    }

    private void listRooms() {
        send(MessageFactory.system("Rooms: " + roomManager.getAllRooms()));
    }

    // ==============================
    // USER MANAGEMENT
    // ==============================
    private void sendUserList() {
        send(MessageFactory.system(
                "Online Users: " + String.join(", ", clients.keySet())
        ));
    }

    private void changeUsername(String newName) {

        if (newName == null || newName.trim().isEmpty() || clients.containsKey(newName)) {
            send(MessageFactory.error("Invalid or taken username."));
            return;
        }

        String oldName = username;

        clients.remove(oldName, this);
        username = newName.trim();
        clients.put(username, this);

        broadcast(MessageFactory.system(oldName + " is now known as " + username));
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
    // ROOM BROADCAST (NEW)
    // ==============================
    private void broadcastToRoom(String room, Message msg) {
        Set<ClientHandler> members = roomManager.getRoomMembers(room);

        for (ClientHandler client : members) {
            try {
                client.send(msg);
            } catch (Exception ignored) {}
        }
    }

    // ==============================
    // DEFAULT BROADCAST
    // ==============================
    private void broadcast(Message msg) {
        String room = roomManager.getClientRoom(this);
        broadcastToRoom(room, msg);
    }

    private void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
        }
    }

    // ==============================
    // FORCE DISCONNECT
    // ==============================
    public void forceDisconnect() {
        System.out.println("[FORCE DISCONNECT] " + username);
        cleanup();
    }

    // ==============================
    // CLEANUP (UPGRADED)
    // ==============================
    private void cleanup() {

        if (cleanedUp) return;
        cleanedUp = true;

        active = false;

        try {
            if (username != null) {
                boolean removed = clients.remove(username, this);

                if (removed) {
                    System.out.println("[USER LEFT] " + username);

                    String room = roomManager.getClientRoom(this);

                    // 🔥 notify room BEFORE removal
                    if (room != null) {
                        broadcastToRoom(room,
                                MessageFactory.system("[ROOM " + room + "]: " + username + " left"));
                    }

                    roomManager.removeClient(this);

                    broadcast(MessageFactory.system(username + " left the chat."));
                }
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("[ERROR] Cleanup failed: " + e.getMessage());
        }
    }
}