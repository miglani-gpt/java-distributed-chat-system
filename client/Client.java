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

    // Connection
    private static Socket socket;
    private static BufferedReader in;
    private static PrintWriter out;

    // Threads
    private static Thread listener;
    private static Thread heartbeat;
    private static Thread monitor;

    // Heartbeat
    private static volatile long lastPongTime = System.currentTimeMillis();

    // Reconnect config
    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_DELAY = 2000;

    private static volatile boolean isCleaningUp = false;

    public static void main(String[] args) {

        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        try {
            setupUsername(console);

            if (!connect()) {
                System.out.println("[ERROR] Initial connection failed.");
                return;
            }

            startThreads();

            mainLoop(console);

        } catch (Exception e) {
            System.out.println("[ERROR] Unexpected error: " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    // ==============================
    // CONNECTION
    // ==============================
    private static boolean connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("[CONNECTED] Connected to server");

            Message initMsg = MessageFactory.command(username, "INIT", null, "");
            out.println(initMsg.toJson());

            lastPongTime = System.currentTimeMillis();
            running = true;

            return true;

        } catch (IOException e) {
            System.out.println("[ERROR] Connection failed.");
            return false;
        }
    }

    // ==============================
    // THREADS
    // ==============================
    private static void startThreads() {

        listener = new Thread(() -> {
            try {
                String response;

                while (running && (response = in.readLine()) != null) {

                    Message msg = Message.fromJson(response);
                    if (msg == null) continue;

                    if (msg.getType() == MessageType.PONG) {
                        lastPongTime = System.currentTimeMillis();
                        continue;
                    }

                    displayMessage(msg);
                }

            } catch (IOException e) {
                System.out.println("\n[DISCONNECTED] Server connection lost.");
            } finally {
                handleDisconnect();
            }
        });

        heartbeat = new Thread(() -> {
            try {
                while (running) {
                    Thread.sleep(3000);

                    Message ping = MessageFactory.ping(username);
                    out.println(ping.toJson());
                }
            } catch (InterruptedException ignored) {}
        });

        monitor = new Thread(() -> {
            while (running) {

                if (System.currentTimeMillis() - lastPongTime > 15000) {
                    System.out.println("\n[DISCONNECTED] Heartbeat timeout.");
                    handleDisconnect();
                    break;
                }

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {}
            }
        });

        listener.start();
        heartbeat.start();
        monitor.start();
    }

    // ==============================
    // DISCONNECT HANDLER
    // ==============================
    private static void handleDisconnect() {
        if (!running) return;

        System.out.println("[CLIENT] Connection lost. Attempting reconnect...");
        running = false;

        cleanup();

        boolean success = reconnect();

        if (!success) {
            System.out.println("[CLIENT] Could not reconnect. Exiting...");
            System.exit(0);
        }
    }

    // ==============================
    // RECONNECT
    // ==============================
    private static boolean reconnect() {
        int attempts = 0;
        int delay = INITIAL_DELAY;

        while (attempts < MAX_RETRIES) {
            try {
                System.out.println("[RECONNECT] Attempt " + (attempts + 1));

                Thread.sleep(delay);

                if (connect()) {
                    startThreads();
                    System.out.println("[RECONNECT] Success!");
                    return true;
                }

            } catch (InterruptedException ignored) {}

            attempts++;
            delay *= 2;
        }

        return false;
    }

    // ==============================
    // CLEANUP
    // ==============================
    private static synchronized void cleanup() {
        if (isCleaningUp) return;
        isCleaningUp = true;

        try { if (socket != null) socket.close(); } catch (IOException ignored) {}

        isCleaningUp = false;
    }

    // ==============================
    // MAIN LOOP
    // ==============================
    private static void mainLoop(BufferedReader console) throws IOException, InterruptedException {

        String userInput;

        while (true) {

            if (!running) {
                Thread.sleep(100);
                continue;
            }

            if (!console.ready()) {
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                continue;
            }

            userInput = console.readLine();
            if (userInput == null) break;

            userInput = userInput.trim();
            if (userInput.isEmpty()) continue;

            Message msg = buildMessage(userInput);
            out.println(msg.toJson());

            if ("EXIT".equals(msg.getCommand())) {
                running = false;
                break;
            }
        }
    }

    // ==============================
    // USERNAME SETUP
    // ==============================
    private static void setupUsername(BufferedReader console) throws IOException {
        while (true) {
            System.out.print("Enter username: ");
            username = console.readLine();

            if (username != null && !username.trim().isEmpty()) {
                username = username.trim();
                return;
            }

            System.out.println("[ERROR] Username cannot be empty.");
        }
    }

    // ==============================
    // SHUTDOWN
    // ==============================
    private static void shutdown() {
        running = false;

        if (listener != null) listener.interrupt();
        if (heartbeat != null) heartbeat.interrupt();
        if (monitor != null) monitor.interrupt();

        cleanup();

        System.out.println("[CLIENT CLOSED]");
    }

    // ==============================
    // MESSAGE BUILDING (UNCHANGED)
    // ==============================
    private static Message buildMessage(String input) {

        if (input.startsWith("/msg ")) {
            String[] parts = input.split(" ", 3);

            if (parts.length < 3) {
                System.out.println("[ERROR] Usage: /msg <user> <message>");
                return MessageFactory.error("Invalid private message format");
            }

            return MessageFactory.privateMsg(username, parts[1], parts[2]);
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
    // DISPLAY (UNCHANGED)
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