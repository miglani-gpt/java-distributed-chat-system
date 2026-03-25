package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 100;

    // 🔥 Username → Client mapping
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private static volatile boolean isRunning = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        log("[START] Server is starting...");

        try {
            serverSocket = new ServerSocket(PORT);
            log("[LISTENING] Port: " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(Server::shutdownServer));

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

    // ================= SHUTDOWN =================

    private static void shutdownServer() {
        log("[SHUTDOWN] Server is shutting down...");
        isRunning = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logError("[SHUTDOWN ERROR] " + e.getMessage());
        }

        broadcast("[SYSTEM] Server is shutting down...");
    }

    // ================= CONNECTION =================

    private static void handleNewClient(Socket clientSocket) {
        ClientHandler handler = new ClientHandler(clientSocket);

        Thread thread = new Thread(handler);
        thread.setName("Client-" + clientSocket.getPort());
        thread.start();
    }

    private static void logConnection(Socket clientSocket) {
        log("[CONNECTED] " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
    }

    // ================= CLIENT MANAGEMENT =================

    public static boolean addClient(ClientHandler client) {
        boolean added = clients.putIfAbsent(client.getUsername(), client) == null;

        if (added) {
            log("[CLIENT ADDED] " + client.getUsername() + " | Total: " + clients.size());
            broadcast("[SYSTEM] " + client.getUsername() + " joined the chat");
        }

        return added;
    }

    public static void removeClient(ClientHandler client) {
        if (client.getUsername() != null &&
                clients.remove(client.getUsername()) != null) {

            log("[CLIENT REMOVED] " + client.getUsername() + " | Total: " + clients.size());
            broadcast("[SYSTEM] " + client.getUsername() + " left the chat");
        }
    }

    // 🔥 Atomic username update
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

    /**
     * 🔥 Broadcast to ALL clients (including sender)
     * Explicit design: sender receives their own message (echo)
     */
    public static void broadcast(String message) {

        List<ClientHandler> toRemove = new ArrayList<>();

        for (ClientHandler client : clients.values()) {
            try {
                client.sendMessage(message);
            } catch (Exception e) {
                logError("[BROADCAST ERROR] Marking dead client: " + client.getUsername());
                toRemove.add(client);
            }
        }

        // 🔥 Remove after iteration (safe)
        for (ClientHandler deadClient : toRemove) {
            removeClient(deadClient);
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