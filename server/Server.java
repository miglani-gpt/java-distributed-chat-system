package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private static final int PORT = 5000;

    public static void main(String[] args) {
        startServer();
    }

    private static void startServer() {
        System.out.println("[SERVER] [START] Server is starting...");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] [LISTENING] Port: " + PORT);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[SERVER] [SHUTDOWN] Server is shutting down...");
            }));

            while (true) {
                Socket clientSocket = serverSocket.accept();

                logConnection(clientSocket);
                handleNewClient(clientSocket);
            }

        } catch (IOException e) {
            System.err.println("[SERVER] [ERROR] Failed to start server: " + e.getMessage());
        }
    }

    private static void handleNewClient(Socket clientSocket) {
        ClientHandler handler = new ClientHandler(clientSocket);

        Thread thread = new Thread(handler);
        thread.setName("Client-" + clientSocket.getPort());
        thread.start();
    }

    private static void logConnection(Socket clientSocket) {
        System.out.println("[SERVER] [CONNECTED] Client: "
                + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
    }
}