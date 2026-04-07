package server;

import common.Message;
import common.MessageFactory;
import common.MessageValidator;

import java.io.*;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final ConcurrentHashMap<String, ClientHandler> clients;
    private final RoomManager roomManager;

    private volatile boolean active = false;
    private final AtomicBoolean cleanedUp = new AtomicBoolean(false);

    public ClientHandler(Socket socket,
            ConcurrentHashMap<String, ClientHandler> clients,
            RoomManager roomManager) {
        this.socket = socket;
        this.clients = clients;
        this.roomManager = roomManager;
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
        if (raw == null)
            return false;

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
            log("RECONNECT", "Replacing old session for " + requestedName);
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

        log("DISCONNECTED", username);
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
    // ROOM COMMANDS
    // ==============================
    private void joinRoom(String roomName) {

        if (!valid(roomName)) {
            send(MessageFactory.error("Room name cannot be empty."));
            return;
        }

        String newRoom = roomName.trim();
        String oldRoom = roomManager.getClientRoom(this);

        if (oldRoom != null && !oldRoom.equals(newRoom)) {
            broadcastToRoom(oldRoom,
                    MessageFactory.system("[ROOM " + oldRoom + "] " + username + " left"));
        }

        roomManager.joinRoom(newRoom, this);

        send(MessageFactory.system("Joined room: " + newRoom));

        broadcastToRoom(newRoom,
                MessageFactory.system("[ROOM " + newRoom + "] " + username + " joined"));
    }

    private void leaveRoom() {

        String oldRoom = roomManager.getClientRoom(this);

        if (oldRoom != null) {
            broadcastToRoom(oldRoom,
                    MessageFactory.system("[ROOM " + oldRoom + "] " + username + " left"));
        }

        roomManager.joinRoom("global", this);

        send(MessageFactory.system("Returned to global room"));

        broadcastToRoom("global",
                MessageFactory.system("[ROOM global] " + username + " joined"));
    }

    private void listRooms() {
        send(MessageFactory.system("Rooms: " + roomManager.getAllRooms()));
    }

    // ==============================
    // USER MANAGEMENT
    // ==============================
    private void sendUserList() {
        send(MessageFactory.system("Online Users: " + String.join(", ", clients.keySet())));
    }

    private synchronized void changeUsername(String newName) {

        if (!valid(newName)) {
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

        if (members == null)
            return;

        for (ClientHandler client : members.toArray(new ClientHandler[0])) {
            try {
                client.send(msg);
            } catch (Exception ignored) {
            }
        }
    }

    private void broadcast(Message msg) {
        String room = roomManager.getClientRoom(this);
        if (room != null) {
            broadcastToRoom(room, msg);
        }
    }

    private synchronized void send(Message msg) {
        try {
            if (out != null && msg != null && !socket.isClosed()) {
                out.println(msg.toJson());
                out.flush();
            }
        } catch (Exception e) {
            log("ERROR", "Send failed: " + e.getMessage());
            cleanup();
        }
    }

    // ==============================
    // FORCE DISCONNECT
    // ==============================
    public void forceDisconnect() {
        log("FORCE", username);
        cleanup();
    }

    // ==============================
    // CLEANUP
    // ==============================
    private void cleanup() {

        if (!cleanedUp.compareAndSet(false, true))
            return;

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

            // 🔥 CLOSE STREAMS PROPERLY
            try {
                if (in != null)
                    in.close();
            } catch (Exception ignored) {
            }
            try {
                if (out != null)
                    out.close();
            } catch (Exception ignored) {
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            log("ERROR", "Cleanup failed: " + e.getMessage());
        }
    }

    public void closeConnection() {
        cleanup();
    }

    // ==============================
    // UTIL
    // ==============================
    private boolean valid(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private void log(String tag, String msg) {
        System.out.println("[CLIENT " + (username != null ? username : "?") + "] [" + tag + "] " + msg);
    }
}