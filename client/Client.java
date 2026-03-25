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
        log("[CONNECTING] Attempting to connect to " + serverAddress + ":" + PORT);

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
            log("[CONNECTED] Successfully connected to server");

            // 🔥 Listener thread (receives messages)
            Thread listener = new Thread(() -> listenToServer(serverReader));
            listener.setName("Server-Listener");
            listener.start();

            // 🔥 Sender loop (user input)
            sendMessages(consoleReader, writer);

            // 🔥 Wait for listener to finish (clean shutdown)
            try {
                listener.join();
            } catch (InterruptedException ignored) {}

        } catch (IOException e) {
            logError("[ERROR] Unable to connect to server at "
                    + serverAddress + ":" + PORT + " (" + e.getMessage() + ")");
        } finally {
            isRunning = false;
            log("[SHUTDOWN] Client stopped");
        }
    }

    // ================= LISTENER =================

    private static void listenToServer(BufferedReader serverReader) {
        try {
            String serverMessage;

            while (isRunning && (serverMessage = serverReader.readLine()) != null) {
                System.out.println(serverMessage);
            }

        } catch (IOException e) {
            if (isRunning) {
                logError("[ERROR] Connection lost");
            }
        } finally {
            isRunning = false;
            log("[DISCONNECTED] Server connection closed");
        }
    }

    // ================= SENDER =================

    private static void sendMessages(BufferedReader consoleReader, PrintWriter writer) {
        try {
            String userInput;

            while (isRunning && (userInput = consoleReader.readLine()) != null) {

                // 🔥 Send to server
                writer.println(userInput);

                if (writer.checkError()) {
                    throw new IOException("Failed to send message");
                }

                // 🔥 Exit handling
                if ("/exit".equalsIgnoreCase(userInput)) {
                    log("[DISCONNECTING] Closing connection...");
                    isRunning = false;
                    break;
                }
            }

        } catch (IOException e) {
            if (isRunning) {
                logError("[ERROR] Message sending failed: " + e.getMessage());
            }
        }
    }

    // ================= LOGGING =================

    private static void log(String message) {
        System.out.println("[CLIENT][" + Thread.currentThread().getName() + "] " + message);
    }

    private static void logError(String message) {
        System.err.println("[CLIENT][" + Thread.currentThread().getName() + "] " + message);
    }
}