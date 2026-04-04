package server;

import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 5000;

    // ==============================
    // Shared State
    // ==============================
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final RoomManager roomManager = new RoomManager();

    // ==============================
    // Thread Pool (Improved)
    // ==============================
    private static final ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(
                    20,
                    50,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new NamedThreadFactory(),
                    new ThreadPoolExecutor.AbortPolicy()
            );

    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        addShutdownHook();
        startServer();
    }

    // ==============================
    // Start Server
    // ==============================
    private static void startServer() {
        log("START", "Starting server...");

        try {
            serverSocket = new ServerSocket(PORT);
            log("LISTENING", "Port " + PORT);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    configureSocket(socket);

                    log("CONNECT", socket.getInetAddress().toString());

                    ClientHandler handler = new ClientHandler(socket, clients, roomManager);

                    try {
                        threadPool.execute(handler);
                        logStats(); // 🔥 monitor system
                    } catch (RejectedExecutionException e) {
                        log("OVERLOAD", "Too many clients. Rejecting connection.");
                        safeClose(socket);
                    }

                } catch (SocketException e) {
                    if (!running) break;
                    log("ERROR", "Socket issue: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            log("FATAL", "Failed to start server: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    // ==============================
    // Socket Configuration
    // ==============================
    private static void configureSocket(Socket socket) throws SocketException {
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(0);
    }

    // ==============================
    // Shutdown Hook
    // ==============================
    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("HOOK", "Shutdown signal received");
            shutdown();
        }));
    }

    // ==============================
    // Shutdown Logic
    // ==============================
    private static void shutdown() {
        if (!running) return;
        running = false;

        log("SHUTDOWN", "Shutting down server...");

        safeClose(serverSocket);

        threadPool.shutdown();

        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                log("FORCE", "Forcing shutdown...");
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log("STOPPED", "Server stopped.");
    }

    // ==============================
    // Monitoring (🔥 BIG UPGRADE)
    // ==============================
    private static void logStats() {
        log("STATS",
                "Active=" + threadPool.getActiveCount() +
                " | Pool=" + threadPool.getPoolSize() +
                " | Queue=" + threadPool.getQueue().size() +
                " | Clients=" + clients.size()
        );
    }

    // ==============================
    // Utilities
    // ==============================
    private static void safeClose(ServerSocket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private static void safeClose(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }

    private static void log(String tag, String message) {
        System.out.println(
                "[" + LocalTime.now() + "] [SERVER] [" + tag + "] " + message
        );
    }

    // ==============================
    // Thread Factory (FIXED)
    // ==============================
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("client-handler-" + count.incrementAndGet());
            return t;
        }
    }
}