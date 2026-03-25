package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int PORT = 5000;

    // ✅ Shared client registry
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    // 🔥 Bounded thread pool (safer than cached)
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(50);

    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        System.out.println("[SERVER] [START] Server is starting...");

        try {
            serverSocket = new ServerSocket(PORT);

            System.out.println("[SERVER] [LISTENING] Port " + PORT);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();

                    System.out.println("[SERVER] [NEW CONNECTION] " + socket.getInetAddress());

                    ClientHandler handler = new ClientHandler(socket, clients);
                    threadPool.execute(handler);

                } catch (SocketException e) {
                    // 🔥 Happens when serverSocket is closed during shutdown
                    if (!running) break;
                    System.out.println("[SERVER] [ERROR] Socket issue: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("[SERVER] [ERROR] Failed to start server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    // ==============================
    // Graceful Shutdown
    // ==============================
    private static void shutdown() {
        System.out.println("[SERVER] [SHUTDOWN] Shutting down server...");

        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close(); // 🔥 unblocks accept()
            }
        } catch (IOException e) {
            System.out.println("[SERVER] [ERROR] Closing socket failed: " + e.getMessage());
        }

        try {
            threadPool.shutdownNow();
        } catch (Exception e) {
            System.out.println("[SERVER] [ERROR] Thread pool shutdown issue: " + e.getMessage());
        }

        System.out.println("[SERVER] [STOPPED]");
    }
}