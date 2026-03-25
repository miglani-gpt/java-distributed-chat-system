package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        log("[THREAD STARTED] " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        try {
            clientSocket.setSoTimeout(300000);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );

            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            if (!handleUsernameSetup(reader)) return;

            handleMessages(reader);

        } catch (Exception e) {
            logError("[ERROR] Client issue: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ================= USERNAME SETUP =================

    private boolean handleUsernameSetup(BufferedReader reader) throws Exception {
        sendMessage("[SYSTEM] Enter your username:");

        while (true) {
            String inputName = reader.readLine();
            if (inputName == null) return false;

            inputName = inputName.trim();

            if (!isValidUsername(inputName)) {
                sendMessage("[ERROR] Username must be 3-15 characters (letters, numbers, _)");
                continue;
            }

            username = inputName;

            if (Server.addClient(this)) {
                sendMessage("[SYSTEM] Welcome " + username + "!");
                sendOnlineUsers();
                return true;
            } else {
                sendMessage("[ERROR] Username already taken. Try again:");
            }
        }
    }

    // ================= MESSAGE LOOP =================

    private void handleMessages(BufferedReader reader) throws Exception {
        String message;

        while ((message = reader.readLine()) != null) {
            message = message.trim();

            if (message.isEmpty()) {
                sendMessage("[ERROR] Message cannot be empty");
                continue;
            }

            if (processMessage(message)) break;
        }
    }

    // ================= MESSAGE PROCESSOR =================

    private boolean processMessage(String message) {

        if (message.startsWith("/")) {
            return handleCommand(message);
        }

        broadcastChat(message);
        return false;
    }

    // ================= CHAT =================

    private void broadcastChat(String message) {
        String formatted = "[CHAT][" + username + "]: " + message;
        log("[MESSAGE] " + formatted);
        Server.broadcast(formatted);
    }

    // ================= COMMAND HANDLER =================

    private boolean handleCommand(String message) {

        String[] parts = message.split(" ", 2);
        String command = parts[0].toLowerCase();

        switch (command) {

            case "/exit":
                return true;

            case "/name":
                if (parts.length < 2) {
                    sendMessage("[ERROR] Usage: /name <new_name>");
                    return false;
                }
                return handleUsernameChange(parts[1]);

            case "/msg":
                return handlePrivateMessage(message);

            case "/list":
                sendOnlineUsers();
                return false;

            case "/help":
                sendHelp();
                return false;

            default:
                sendMessage("[ERROR] Unknown command. Type /help for available commands.");
                return false;
        }
    }

    // ================= COMMAND HELPERS =================

    private boolean handleUsernameChange(String newName) {
        newName = newName.trim();

        if (!isValidUsername(newName)) {
            sendMessage("[ERROR] Invalid username format.");
            return false;
        }

        String oldName = username;

        if (!Server.updateUsername(oldName, newName, this)) {
            sendMessage("[ERROR] Username already taken.");
            return false;
        }

        username = newName;

        Server.broadcast("[SYSTEM] " + oldName + " is now known as " + username);
        sendMessage("[SYSTEM] You are now known as " + username);

        return false;
    }

    private boolean handlePrivateMessage(String message) {
        String[] parts = message.split(" ", 3);

        if (parts.length < 3) {
            sendMessage("[ERROR] Usage: /msg <username> <message>");
            return false;
        }

        String targetUsername = parts[1];
        String privateMessage = parts[2];

        ClientHandler target = Server.findClientByUsername(targetUsername);

        if (target == null) {
            sendMessage("[ERROR] User not found.");
            return false;
        }

        target.sendMessage("[PRIVATE][" + username + "]: " + privateMessage);
        sendMessage("[PRIVATE → " + targetUsername + "] " + privateMessage);

        return false;
    }

    private void sendOnlineUsers() {
        Set<String> users = Server.getAllUsernames();

        if (users.size() == 1) {
            sendMessage("[SYSTEM] You are the only user online");
        } else {
            sendMessage("[SYSTEM] Online users: " + String.join(", ", users));
        }
    }

    private void sendHelp() {
        sendMessage("[SYSTEM] === Available Commands ===");
        sendMessage(" /name <new_name>    → Change your username");
        sendMessage(" /msg <user> <msg>   → Send private message");
        sendMessage(" /list               → Show online users");
        sendMessage(" /help               → Show this help menu");
        sendMessage(" /exit               → Disconnect");
    }

    // ================= VALIDATION =================

    private boolean isValidUsername(String name) {
        return name.matches("^[a-zA-Z0-9_]{3,15}$");
    }

    // ================= UTIL =================

    public void sendMessage(String message) {
        if (writer != null) {
            writer.println(message);

            if (writer.checkError()) {
                throw new RuntimeException("Client disconnected");
            }
        }
    }

    public String getUsername() {
        return username;
    }

    // ================= CLEANUP =================

    private void cleanup() {
        try {
            Server.removeClient(this);

            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }

            log("[CLEANUP] " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        } catch (Exception e) {
            logError("[ERROR] Cleanup failed");
        }
    }

    // ================= LOGGING =================

    private void log(String message) {
        System.out.println("[SERVER][" + Thread.currentThread().getName() + "] " + message);
    }

    private void logError(String message) {
        System.err.println("[SERVER][" + Thread.currentThread().getName() + "] " + message);
    }
}