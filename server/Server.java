package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 100;

    // 🔥 Username → Client mapping
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private static volatile boolean isRunning = true;
    private static ServerSocket serverSocket; // 🔥 needed for graceful shutdown

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        log("[START] Server is starting...");

        try {
            serverSocket = new ServerSocket(PORT);
            log("[LISTENING] Port: " + PORT);

            // 🔥 Shutdown Hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                shutdownServer();
            }));

            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();

                    if (clients.size() >= MAX_CLIENTS) {
                        log("[REJECTED] Max clients reached: " + clientSocket.getInetAddress());
                        clientSocket.close();
                        continue;
                    }

                    logConnection(clientSocket);
                    handleNewClient(clientSocket);

                } catch (IOException e) {
                    if (isRunning) {
                        logError("[ERROR] Accept failed: " + e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            logError("[FATAL] Failed to start server: " + e.getMessage());
        }
    }

    // 🔥 Clean shutdown logic
    private static void shutdownServer() {
        log("[SHUTDOWN] Server is shutting down...");
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // 🔥 breaks accept()
            }
        } catch (IOException e) {
            logError("[SHUTDOWN ERROR] " + e.getMessage());
        }

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage("[SERVER] Server is shutting down...");
            } catch (Exception ignored) {}
        }
    }

    private static void handleNewClient(Socket clientSocket) {
        ClientHandler handler = new ClientHandler(clientSocket);

        Thread thread = new Thread(handler);
        thread.setName("Client-" + clientSocket.getPort());
        thread.start();
    }

    private static void logConnection(Socket clientSocket) {
        log("[CONNECTED] Client: "
                + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
    }

    // ================= CLIENT MANAGEMENT =================

    public static boolean addClient(ClientHandler client) {
        boolean added = clients.putIfAbsent(client.getUsername(), client) == null;

        if (added) {
            log("[CLIENT ADDED] " + client.getUsername() + " | Total: " + clients.size());
            broadcast("[SERVER] " + client.getUsername() + " joined the chat", null);
        }

        return added;
    }

    public static void removeClient(ClientHandler client) {
        if (client.getUsername() != null && clients.remove(client.getUsername()) != null) {
            log("[CLIENT REMOVED] " + client.getUsername() + " | Total: " + clients.size());
            broadcast("[SERVER] " + client.getUsername() + " left the chat", null);
        }
    }

    // 🔥 FIXED: atomic username update
    public static synchronized boolean updateUsername(String oldName, String newName, ClientHandler client) {
        if (clients.containsKey(newName)) {
            return false;
        }

        clients.remove(oldName);
        clients.put(newName, client);

        log("[USERNAME UPDATED] " + oldName + " → " + newName);
        return true;
    }

    // ================= BROADCAST =================

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (client != sender) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    logError("[BROADCAST ERROR] Removing dead client: " + client.getUsername());
                    removeClient(client); // 🔥 cleanup
                }
            }
        }
    }

    // ================= LOOKUPS =================

    public static boolean isUsernameTaken(String username) {
        return clients.containsKey(username);
    }

    public static ClientHandler findClientByUsername(String username) {
        return clients.get(username);
    }

    public static Set<String> getAllUsernames() {
        return clients.keySet();
    }

    // ================= LOGGING =================

    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private static void log(String message) {
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] [SERVER] "
                + Thread.currentThread().getName() + " → " + message);
    }

    private static void logError(String message) {
        System.err.println("[" + LocalDateTime.now().format(formatter) + "] [SERVER] "
                + Thread.currentThread().getName() + " → " + message);
    }
}