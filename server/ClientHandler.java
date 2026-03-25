package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientId = clientSocket.getInetAddress() + ":" + clientSocket.getPort();

        System.out.println("[SERVER] [THREAD " 
                + Thread.currentThread().getName() + "] Handling client: " + clientId);

        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream())
            )
        ) {
            String message;

            while ((message = reader.readLine()) != null) {

                if ("exit".equalsIgnoreCase(message)) {
                    System.out.println("[SERVER] [EXIT] Client requested disconnect: " + clientId);
                    break;
                }

                System.out.println("[SERVER] [MESSAGE] "
                        + clientId + " -> " + message);
            }

            System.out.println("[SERVER] [DISCONNECTED] Client: " + clientId);

        } catch (IOException e) {
            System.err.println("[SERVER] [ERROR] Client connection issue ("
                    + clientId + "): " + e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
                System.out.println("[SERVER] [CLEANUP] Connection closed for: " + clientId);
            } catch (IOException e) {
                System.err.println("[SERVER] [ERROR] Failed to close client socket");
            }
        }
    }
}