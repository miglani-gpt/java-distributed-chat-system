package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private PrintWriter writer;
    private String username;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        log("[THREAD STARTED] Handling client: "
                + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        try {
            clientSocket.setSoTimeout(300000); // 🔥 Prevent infinite blocking (5 min)

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );

            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            // 🔹 Default username
            username = "User-" + clientSocket.getPort();

            // 🔥 Safe add
            if (!Server.addClient(this)) {
                sendMessage("[ERROR] Username already taken.");
                return;
            }

            String message;

            while ((message = reader.readLine()) != null) {

                if (handleCommand(message)) {
                    break;
                }

                String formattedMessage = "[" + username + "] " + message;

                log("[MESSAGE] " + formattedMessage);
                Server.broadcast(formattedMessage, this);
            }

        } catch (Exception e) {
            logError("[ERROR] Client issue (" 
                    + clientSocket.getInetAddress() + "): " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    // ================= COMMAND HANDLER =================

    private boolean handleCommand(String message) {

        // 🔴 EXIT
        if (message.equalsIgnoreCase("/exit")) {
            return true;
        }

        // 🔵 USERNAME CHANGE
        if (message.startsWith("/name ")) {
            String newName = message.substring(6).trim();

            if (!isValidUsername(newName)) {
                sendMessage("[ERROR] Username must be 3-15 characters (letters, numbers, _)");
                return false;
            }

            String oldName = username;

            boolean success = Server.updateUsername(oldName, newName, this);

            if (!success) {
                sendMessage("[ERROR] Username already taken.");
                return false;
            }

            username = newName;

            Server.broadcast("[SYSTEM] " + oldName + " is now known as " + username, this);
            sendMessage("[SYSTEM] You are now known as " + username);

            return false;
        }

        // 🟣 PRIVATE MESSAGE
        if (message.startsWith("/msg ")) {
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

            target.sendMessage("[PRIVATE] [" + username + "]: " + privateMessage);
            sendMessage("[PRIVATE → " + targetUsername + "] " + privateMessage);

            return false;
        }

        // 🟢 LIST USERS
        if (message.equalsIgnoreCase("/list")) {
            sendMessage("[USERS] " + String.join(", ", Server.getAllUsernames()));
            return false;
        }

        return false;
    }

    // ================= VALIDATION =================

    private boolean isValidUsername(String name) {
        return name.matches("^[a-zA-Z0-9_]{3,15}$");
    }

    // ================= UTIL =================

    public void sendMessage(String message) {
        if (writer != null) {
            try {
                writer.println(message);

                if (writer.checkError()) {
                    throw new RuntimeException("Write failed");
                }

            } catch (Exception e) {
                logError("[SEND FAILED] Client likely disconnected: " + username);
                throw new RuntimeException("Client disconnected");
            }
        }
    }

    public String getUsername() {
        return username;
    }

    // 🔥 Clean centralized cleanup
    private void cleanup() {
        try {
            Server.removeClient(this);

            if (!clientSocket.isClosed()) {
                clientSocket.close();
            }

            log("[CLEANUP] Connection closed for: "
                    + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        } catch (Exception e) {
            logError("[ERROR] Cleanup failed for client");
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