package server;

import common.Message;
import common.MessageFactory;
import common.MessageValidator;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    private final ConcurrentHashMap<String, ClientHandler> clients;

    private volatile boolean active = false;
    private volatile boolean cleanedUp = false; // 🔥 prevent double cleanup

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
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
    // Registration
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

        if (requestedName.isEmpty() || clients.containsKey(requestedName)) {
            send(MessageFactory.error("Invalid or duplicate username."));
            return false;
        }

        username = requestedName;
        clients.put(username, this);

        System.out.println("[USER REGISTERED] " + username);
        System.out.println("[STATE] Clients: " + clients.keySet());

        send(MessageFactory.system("Welcome " + username + "!"));
        broadcast(MessageFactory.system(username + " joined the chat."));

        return true;
    }

    // ==============================
    // Main Loop
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

                case PING: // ❤️ heartbeat
                    send(MessageFactory.pong());
                    break;

                default:
                    send(MessageFactory.error("Unsupported message type."));
            }
        }

        // 🔥 graceful disconnect log
        System.out.println("[DISCONNECTED] " + username);
    }

    // ==============================
    // Handlers
    // ==============================
    private void handlePrivate(Message msg) {

        String target = msg.getReceiver();
        ClientHandler receiver = clients.get(target);

        if (receiver == null) {
            send(MessageFactory.error("User not found: " + target));
            return;
        }

        Message privateMsg = MessageFactory.privateMsg(
                username,
                target,
                msg.getContent()
        );

        receiver.send(privateMsg);
        send(privateMsg);
    }

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
                cleanup();
                break;

            default:
                send(MessageFactory.error("Unknown command."));
        }
    }

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

        clients.remove(oldName);
        username = newName.trim();
        clients.put(username, this);

        broadcast(MessageFactory.system(oldName + " is now known as " + username));
    }

    // ==============================
    // Messaging
    // ==============================
    private void broadcast(Message msg) {
        for (ClientHandler client : clients.values()) {
            try {
                client.send(msg);
            } catch (Exception ignored) {}
        }
    }

    private void send(Message msg) {
        if (out != null) {
            out.println(msg.toJson());
        }
    }

    // ==============================
    // Cleanup
    // ==============================
    private void cleanup() {

        if (cleanedUp) return;
        cleanedUp = true;

        try {
            if (username != null && clients.containsKey(username)) {
                clients.remove(username);
                System.out.println("[USER LEFT] " + username);
                broadcast(MessageFactory.system(username + " left the chat."));
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

        } catch (IOException e) {
            System.out.println("[ERROR] Cleanup failed: " + e.getMessage());
        }
    }
}