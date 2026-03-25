package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;

    // 🔥 Shared flag for thread coordination
    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        startClient();
    }

    private static void startClient() {
        System.out.println("[CLIENT] [CONNECTING] Attempting to connect...");

        try (
            Socket socket = new Socket(SERVER_ADDRESS, PORT);
            BufferedReader consoleReader = new BufferedReader(
                    new InputStreamReader(System.in)
            );
            BufferedReader serverReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true
            );
        ) {
            System.out.println("[CLIENT] [CONNECTED] Successfully connected");

            // 🔥 Listener thread (receives messages from server)
            Thread listener = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println("[SERVER MESSAGE] " + serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("[CLIENT] [ERROR] Connection lost");
                } finally {
                    isRunning = false;
                    System.out.println("[CLIENT] [DISCONNECTED] Server connection closed");
                }
            });

            listener.setName("Server-Listener");
            listener.start();

            // Main thread → send messages
            sendMessages(consoleReader, writer);

        } catch (IOException e) {
            System.err.println("[CLIENT] [ERROR] Unable to connect to server at "
                + SERVER_ADDRESS + ":" + PORT + " (" + e.getMessage() + ")");
        } finally {
            System.out.println("[CLIENT] [SHUTDOWN] Client stopped");
        }
    }

    private static void sendMessages(BufferedReader consoleReader, PrintWriter writer) {
        try {
            String userInput;

            while (isRunning && (userInput = consoleReader.readLine()) != null) {

                writer.println(userInput);

                if ("exit".equalsIgnoreCase(userInput)) {
                    System.out.println("[CLIENT] [DISCONNECTING] Closing connection...");
                    isRunning = false;
                    break;
                }
            }

        } catch (IOException e) {
            if (isRunning) {
                System.err.println("[CLIENT] [ERROR] Message sending failed: " + e.getMessage());
            }
        }
    }
}