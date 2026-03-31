package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.*;

public class Server {

    private static final int PORT = 5000;

    // ✅ Shared client registry
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();

    // ✅ Room Manager
    private static final RoomManager roomManager = new RoomManager();

    // 🔥 Bounded thread pool with queue (PRODUCTION SAFE)
    private static final ExecutorService threadPool =
            new ThreadPoolExecutor(
                    20,                 // core threads
                    50,                 // max threads
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100), // bounded queue
                    new ThreadPoolExecutor.AbortPolicy() // reject if overloaded
            );

    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        System.out.println("[SERVER] [START] Starting server...");

        try {
            serverSocket = new ServerSocket(PORT);

            System.out.println("[SERVER] [LISTENING] Port " + PORT);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();

                    // 🔥 Basic socket tuning
                    socket.setKeepAlive(true);
                    socket.setTcpNoDelay(true);

                    System.out.println("[SERVER] [NEW CONNECTION] " + socket.getInetAddress());

                    ClientHandler handler = new ClientHandler(socket, clients, roomManager);

                    try {
                        threadPool.execute(handler);
                    } catch (RejectedExecutionException e) {
                        System.out.println("[SERVER] [OVERLOAD] Too many connections. Rejecting client.");
                        socket.close();
                    }

                } catch (SocketException e) {
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
    // Graceful Shutdown (IMPROVED)
    // ==============================
    private static void shutdown() {
        System.out.println("[SERVER] [SHUTDOWN] Shutting down server...");

        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("[SERVER] [ERROR] Closing socket failed: " + e.getMessage());
        }

        // 🔥 Stop accepting new tasks
        threadPool.shutdown();

        try {
            // wait for active tasks to finish
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        System.out.println("[SERVER] [STOPPED]");
    }
}