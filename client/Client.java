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
        log("[CONNECTING] " + serverAddress + ":" + PORT);

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
            log("[CONNECTED]");

            // 🔥 Listener thread
            Thread listener = new Thread(() -> listenToServer(serverReader));
            listener.setName("Server-Listener");
            listener.start();

            // 🔥 Sender loop
            sendMessages(consoleReader, writer);

            listener.join();

        } catch (IOException e) {
            logError("[ERROR] Connection failed: " + e.getMessage());
        } catch (InterruptedException ignored) {
        } finally {
            isRunning = false;
            log("[SHUTDOWN]");
        }
    }

    // ================= LISTENER =================

    private static void listenToServer(BufferedReader serverReader) {
        try {
            String serverMessage;

            while (isRunning && (serverMessage = serverReader.readLine()) != null) {
                printMessage(serverMessage);
                printPrompt(); // 🔥 keep input line clean
            }

        } catch (IOException e) {
            if (isRunning) {
                logError("[ERROR] Connection lost");
            }
        } finally {
            isRunning = false;
            log("[DISCONNECTED]");
        }
    }

    // ================= SENDER =================

    private static void sendMessages(BufferedReader consoleReader, PrintWriter writer) {
        try {
            String userInput;

            printPrompt();

            while (isRunning && (userInput = consoleReader.readLine()) != null) {

                writer.println(userInput);

                if (writer.checkError()) {
                    throw new IOException("Send failed");
                }

                if ("/exit".equalsIgnoreCase(userInput)) {
                    log("[DISCONNECTING]");
                    isRunning = false;
                    break;
                }

                printPrompt(); // 🔥 show prompt again after sending
            }

        } catch (IOException e) {
            if (isRunning) {
                logError("[ERROR] Sending failed: " + e.getMessage());
            }
        }
    }

    // ================= OUTPUT FORMAT =================

    private static void printMessage(String message) {

        if (message.startsWith("[SYSTEM]")) {
            System.out.println("\n" + message);
            return;
        }

        if (message.startsWith("[PRIVATE]")) {
            System.out.println("\n" + message);
            return;
        }

        if (message.startsWith("[CHAT]")) {
            System.out.println(message);
            return;
        }

        // fallback
        System.out.println(message);
    }

    // ================= PROMPT =================

    private static void printPrompt() {
        System.out.print("> ");
        System.out.flush();
    }

    // ================= LOGGING =================

    private static void log(String message) {
        System.out.println("[CLIENT] " + message);
    }

    private static void logError(String message) {
        System.err.println("[CLIENT] " + message);
    }
}