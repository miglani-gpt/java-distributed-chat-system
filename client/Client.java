package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;

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
            PrintWriter writer = new PrintWriter(
                    socket.getOutputStream(), true
            );
        ) {
            System.out.println("[CLIENT] [CONNECTED] Successfully connected");

            sendMessages(consoleReader, writer);

        } catch (Exception e) {
            System.err.println("[CLIENT] [ERROR] Unable to connect to server at "
                + SERVER_ADDRESS + ":" + PORT);
        } finally {
            System.out.println("[CLIENT] [SHUTDOWN] Client stopped");
        }
    }

    private static void sendMessages(BufferedReader consoleReader, PrintWriter writer) {
        try {
            String userInput;

            while ((userInput = consoleReader.readLine()) != null) {

                if (userInput.equalsIgnoreCase("exit")) {
                    System.out.println("[CLIENT] [DISCONNECTING] Closing connection...");
                    break;
                }

                System.out.println("[CLIENT] [SENDING] " + userInput);
                writer.println(userInput);
            }

        } catch (Exception e) {
            System.err.println("[CLIENT] [ERROR] Message sending failed: " + e.getMessage());
        }
    }
}