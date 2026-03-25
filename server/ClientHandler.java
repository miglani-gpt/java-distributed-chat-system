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

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            )
        ) {
            writer = new PrintWriter(clientSocket.getOutputStream(), true);

            username = "User-" + clientSocket.getPort();

            Server.addClient(this);
            Server.broadcast("[JOINED] " + username, this);

            String message;

            while ((message = reader.readLine()) != null) {

                if (handleCommand(message)) {
                    break;
                }

                String formattedMessage = username + ": " + message;

                log("[MESSAGE] " + formattedMessage);
                Server.broadcast(formattedMessage, this);
            }

            log("[DISCONNECTED] Client: "
                    + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

        } catch (Exception e) {
            logError("[ERROR] Client connection issue ("
                    + clientSocket.getInetAddress() + "): " + e.getMessage());
        } finally {
            try {
                if (username != null) {
                    Server.broadcast("[LEFT] " + username, this);
                }

                Server.removeClient(this);

                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }

                log("[CLEANUP] Connection closed for: "
                        + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

            } catch (Exception e) {
                logError("[ERROR] Failed to close client socket");
            }
        }
    }

    // ================= COMMAND HANDLER =================

    private boolean handleCommand(String message) {

        // 🔴 EXIT
        if (message.equalsIgnoreCase("/exit")) {
            return true;
        }

        // 🔵 NAME CHANGE
        if (message.startsWith("/name ")) {
            String newName = message.substring(6).trim();

            if (!isValidUsername(newName)) {
                sendMessage("[ERROR] Invalid username. Use 3-15 alphanumeric characters or underscores.");
                return false;
            }

            synchronized (Server.class) {
                if (Server.isUsernameTaken(newName)) {
                    sendMessage("[ERROR] Username already taken.");
                    return false;
                }

                String oldName = username;
                username = newName;

                // 🔥 Update map safely
                Server.updateUsername(oldName, newName, this);

                Server.broadcast("[SYSTEM] " + oldName + " is now known as " + username, this);
            }

            // 🔥 Self feedback
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

            target.sendMessage("[PRIVATE] " + username + ": " + privateMessage);

            // 🔥 Sender confirmation
            sendMessage("[PRIVATE to " + targetUsername + "] " + privateMessage);

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

                // 🔥 Detect broken connection
                if (writer.checkError()) {
                    throw new RuntimeException("Write failed");
                }

            } catch (Exception e) {
                logError("[SEND FAILED] Removing client: " + username);
                Server.removeClient(this);
            }
        }
    }

    public String getUsername() {
        return username;
    }

    private void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    private void logError(String message) {
        System.err.println("[SERVER] " + message);
    }
}