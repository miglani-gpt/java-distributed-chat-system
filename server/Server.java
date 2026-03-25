package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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

            Socket clientSocket = serverSocket.accept();
            System.out.println("[SERVER] [CONNECTED] Client: " + clientSocket.getInetAddress());

            handleClient(clientSocket);

        } catch (Exception e) {
            System.err.println("[SERVER] [ERROR] Failed to start server: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
    try (
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(clientSocket.getInputStream())
        )
    ) {
        String message;

        while (true) {
            message = reader.readLine();

            if (message == null) {
                System.out.println("[SERVER] [DISCONNECTED] Client: "
                        + clientSocket.getInetAddress());
                break;
            }

            System.out.println("[SERVER] [MESSAGE] " + message);
        }

    } catch (Exception e) {
        System.err.println("[SERVER] [ERROR] Client connection issue: " + e.getMessage());
    } finally {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (Exception e) {
            System.err.println("[SERVER] [ERROR] Failed to close client socket");
        }
    }
}
}