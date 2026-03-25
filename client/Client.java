package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;

public class Client {

    private static final String DEFAULT_SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;

    private static volatile boolean isRunning = true;

    public static void main(String[] args) {
        String serverAddress = args.length > 0 ? args[0] : DEFAULT_SERVER_ADDRESS;
        startClient(serverAddress);
    }

    private static void startClient(String serverAddress) {
        System.out.println("[CLIENT] [CONNECTING] Attempting to connect...");

        try (
            Socket socket = new Socket(serverAddress, PORT);
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

            Thread listener = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = serverReader.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("[CLIENT] [ERROR] Connection lost");
                    }
                } finally {
                    isRunning = false;
                    System.out.println("[CLIENT] [DISCONNECTED] Server connection closed");
                }
            });

            listener.setName("Server-Listener");
            listener.start();

            sendMessages(consoleReader, writer);

            // 🔥 Stop listener cleanly
            listener.interrupt();

        } catch (IOException e) {
            System.err.println("[CLIENT] [ERROR] Unable to connect to server at "
                + serverAddress + ":" + PORT + " (" + e.getMessage() + ")");
        } finally {
            System.out.println("[CLIENT] [SHUTDOWN] Client stopped");
        }
    }

    private static void sendMessages(BufferedReader consoleReader, PrintWriter writer) {
        try {
            String userInput;

            while (isRunning && (userInput = consoleReader.readLine()) != null) {

                // 🔥 Echo user input
                System.out.println("[YOU] " + userInput);

                writer.println(userInput);

                // 🔥 Detect send failure
                if (writer.checkError()) {
                    throw new IOException("Failed to send message");
                }

                // 🔥 Correct exit command
                if ("/exit".equalsIgnoreCase(userInput)) {
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