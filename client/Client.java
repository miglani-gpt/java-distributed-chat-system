package client;

import common.Message;
import common.MessageFactory;
import common.MessageType;

import java.io.*;
import java.net.Socket;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;

    private static String username;
    private static volatile boolean running = true;

    // ❤️ HEARTBEAT STATE
    private static volatile long lastPongTime = System.currentTimeMillis();

    public static void main(String[] args) {

        Thread listener = null;
        Thread heartbeat = null;
        Thread monitor = null;

        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {

            System.out.println("[CONNECTED] Connected to server");

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
            );

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true
            );

            BufferedReader console = new BufferedReader(
                    new InputStreamReader(System.in)
            );

            // ==============================
            // Username Setup
            // ==============================
            while (true) {
                System.out.print("Enter username: ");
                username = console.readLine();

                if (username != null && !username.trim().isEmpty()) {
                    username = username.trim();
                    break;
                }

                System.out.println("[ERROR] Username cannot be empty.");
            }

            // INIT
            Message initMsg = MessageFactory.command(username, "INIT", null, "");
            out.println(initMsg.toJson());

            // ==============================
            // 🔥 LISTENER THREAD
            // ==============================
            listener = new Thread(() -> {
                try {
                    String response;

                    while (running && (response = in.readLine()) != null) {

                        Message msg = Message.fromJson(response);
                        if (msg == null) continue;

                        // ❤️ HEARTBEAT RESPONSE
                        if (msg.getType() == MessageType.PONG) {
                            lastPongTime = System.currentTimeMillis();
                            continue;
                        }

                        displayMessage(msg);
                    }

                } catch (IOException e) {
                    System.out.println("\n[DISCONNECTED] Server connection lost.");
                } finally {
                    running = false;
                }
            });

            listener.start();

            // ==============================
            // ❤️ HEARTBEAT THREAD
            // ==============================
            heartbeat = new Thread(() -> {
                try {
                    while (running) {
                        Thread.sleep(3000);

                        if (!running) break;

                        Message ping = MessageFactory.ping(username);
                        out.println(ping.toJson());
                    }
                } catch (InterruptedException ignored) {}
            });

            heartbeat.start();

            // ==============================
            // 🔥 MONITOR THREAD
            // ==============================
            monitor = new Thread(() -> {
                while (running) {
                    long now = System.currentTimeMillis();

                    if (now - lastPongTime > 15000) {
                        System.out.println("\n[DISCONNECTED] Server not responding (heartbeat failed).");
                        running = false;
                        break;
                    }

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                }
            });

            monitor.start();

            // ==============================
            // MAIN LOOP (FIXED)
            // ==============================
            String userInput;

            while (running) {

                if (!console.ready()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ignored) {}
                    continue;
                }

                userInput = console.readLine();
                if (userInput == null) break;

                userInput = userInput.trim();
                if (userInput.isEmpty()) continue;

                Message msg = buildMessage(userInput);

                System.out.println("[SENDING] " + msg.toJson());
                out.println(msg.toJson());

                if ("EXIT".equals(msg.getCommand())) {
                    running = false;
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("[ERROR] Unable to connect to server.");
        }

        // ==============================
        // 🔥 CLEAN SHUTDOWN
        // ==============================
        running = false;

        if (listener != null) listener.interrupt();
        if (heartbeat != null) heartbeat.interrupt();
        if (monitor != null) monitor.interrupt();

        System.out.println("[CLIENT CLOSED]");
    }

    // ==============================
    // Message Builder
    // ==============================
    private static Message buildMessage(String input) {

        if (input.startsWith("/msg ")) {
            String[] parts = input.split(" ", 3);

            if (parts.length < 3) {
                System.out.println("[ERROR] Usage: /msg <user> <message>");
                return MessageFactory.error("Invalid private message format");
            }

            return MessageFactory.privateMsg(
                username,
                parts[1],
                parts[2]
            );
        }

        if (input.equals("/list")) {
            return MessageFactory.command(username, "LIST", null, "");
        }

        if (input.startsWith("/name ")) {
            String newName = input.substring(6).trim();

            if (newName.isEmpty()) {
                System.out.println("[ERROR] Username cannot be empty.");
                return MessageFactory.error("Invalid username");
            }

            return MessageFactory.command(username, "NAME", null, newName);
        }

        if (input.equals("/exit")) {
            return MessageFactory.command(username, "EXIT", null, "");
        }

        return MessageFactory.chat(username, input);
    }

    // ==============================
    // Display
    // ==============================
    private static void displayMessage(Message msg) {

        if (msg == null || msg.getType() == null) return;

        switch (msg.getType()) {

            case CHAT:
                System.out.println("[CHAT][" + msg.getSender() + "]: " + msg.getContent());
                break;

            case PRIVATE:
                System.out.println("[PRIVATE][" + msg.getSender() + "]: " + msg.getContent());
                break;

            case SYSTEM:
                System.out.println("[SYSTEM]: " + msg.getContent());
                break;

            case ERROR:
                System.out.println("[ERROR]: " + msg.getContent());
                break;

            default:
                System.out.println("[UNKNOWN]: " + msg.getContent());
        }
    }
}