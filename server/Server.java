package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static final int PORT = 5000;
    private static final int MAX_CLIENTS = 100;

    // 🔥 Username → Client mapping
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        log("[START] Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            log("[LISTENING] Port: " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log("[SHUTDOWN] Server is shutting down...");
                isRunning = false;

                for (ClientHandler client : clients.values()) {
                    try {
                        client.sendMessage("[SERVER] Server is shutting down...");
                    } catch (Exception ignored) {}
                }
            }));

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();

                if (clients.size() >= MAX_CLIENTS) {
                    log("[REJECTED] Max clients reached: " + clientSocket.getInetAddress());
                    clientSocket.close();
                    continue;
                }

                logConnection(clientSocket);
                handleNewClient(clientSocket);
            }

        } catch (IOException e) {
            logError("[ERROR] Failed to start server: " + e.getMessage());
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

    public static void addClient(ClientHandler client) {
        clients.put(client.getUsername(), client);
        log("[CLIENT ADDED] " + client.getUsername() + " | Total: " + clients.size());
    }

    public static void removeClient(ClientHandler client) {
        clients.remove(client.getUsername());
        log("[CLIENT REMOVED] " + client.getUsername() + " | Total: " + clients.size());
    }

    // 🔥 IMPORTANT: username change support
    public static void updateUsername(String oldName, String newName, ClientHandler client) {
        clients.remove(oldName);
        clients.put(newName, client);
    }

    // ================= BROADCAST =================

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients.values()) {
            if (client != sender) {
                try {
                    client.sendMessage(message);
                } catch (Exception e) {
                    logError("[BROADCAST ERROR] Failed to send message");
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

    // ================= LOGGING =================

    private static void log(String message) {
        System.out.println("[SERVER] " + message);
    }

    private static void logError(String message) {
        System.err.println("[SERVER] " + message);
    }
}