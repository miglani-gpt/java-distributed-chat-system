package server;

import java.io.IOException;
import java.net.*;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 5000;

    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private static final RoomManager roomManager = new RoomManager();

    // 🔥 AI SERVICE (NEW)
    private static final AIService aiService = new AIService();

    private static final ThreadPoolExecutor threadPool =
            new ThreadPoolExecutor(
                    20,
                    50,
                    60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(100),
                    new NamedThreadFactory(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );

    private static volatile boolean running = true;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        addShutdownHook();
        startServer();
    }

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

                    ClientHandler handler;
                    try {
                        // 🔥 INJECT AI SERVICE HERE
                        handler = new ClientHandler(socket, clients, roomManager, aiService);
                    } catch (Exception e) {
                        log("ERROR", "Handler creation failed: " + e.getMessage());
                        safeClose(socket);
                        continue;
                    }

                    threadPool.execute(handler);
                    logStats();

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

    private static void configureSocket(Socket socket) throws SocketException {
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(0);
    }

    private static void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log("HOOK", "Shutdown signal received");
            shutdown();
        }));
    }

    private static void shutdown() {
        if (!running) return;
        running = false;

        log("SHUTDOWN", "Shutting down server...");

        safeClose(serverSocket);

        // 🔥 CLOSE ALL CLIENTS
        clients.values().forEach(client -> {
            try {
                client.closeConnection();
            } catch (Exception ignored) {}
        });

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

    private static void logStats() {
        log("STATS",
                "Active=" + threadPool.getActiveCount() +
                " | Pool=" + threadPool.getPoolSize() +
                " | Queue=" + threadPool.getQueue().size() +
                " | Clients=" + clients.size()
        );
    }

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

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("client-handler-" + count.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    }
}