package client;

import common.Message;
import common.MessageFactory;
import common.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int PORT = 5000;

    private String username;

    private final AtomicBoolean running = new AtomicBoolean(false);

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private Thread listener;
    private Thread heartbeat;
    private Thread monitor;

    private MessageListener guiListener;

    private volatile long lastPongTime = System.currentTimeMillis();

    private static final int MAX_RETRIES = 5;
    private static final int INITIAL_DELAY = 2000;

    private final AtomicBoolean cleaningUp = new AtomicBoolean(false);

    public Client(String username) {
        this.username = username;
    }

    public void setMessageListener(MessageListener listener) {
        this.guiListener = listener;
    }

    public String getUsername() {
        return username;
    }

    // ==============================
    // START
    // ==============================
    public void start() {
        if (!connect()) {
            emit(MessageFactory.system("Disconnected"));
            emit(MessageFactory.error("Initial connection failed."));
            return;
        }

        running.set(true);
        startThreads();
    }

    // ==============================
    // SEND
    // ==============================
    public void send(String input) {
        if (!running.get() || out == null) return;

        Message msg = buildMessage(input);
        if (msg == null) return;

        out.println(msg.toJson());

        // ✅ Show own message instantly (clean)
        if (msg.getType() == MessageType.CHAT) {
            emit(msg);
        }

        if ("EXIT".equals(msg.getCommand())) {
            shutdown();
        }
    }

    // ==============================
    // CONNECTION
    // ==============================
    private boolean connect() {
        try {
            socket = new Socket(SERVER_ADDRESS, PORT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

            emit(MessageFactory.system("Connected to server"));

            Message initMsg = MessageFactory.chat(username, "INIT");
            out.println(initMsg.toJson());

            lastPongTime = System.currentTimeMillis();
            return true;

        } catch (IOException e) {
            emit(MessageFactory.system("Disconnected"));
            emit(MessageFactory.error("Connection failed."));
            return false;
        }
    }

    // ==============================
    // THREADS
    // ==============================
    private void startThreads() {

        listener = new Thread(() -> {
            try {
                String response;

                while (running.get() && (response = in.readLine()) != null) {

                    Message msg = Message.fromJson(response);
                    if (msg == null) continue;

                    if (msg.getType() == MessageType.PONG) {
                        lastPongTime = System.currentTimeMillis();
                        continue;
                    }

                    displayMessage(msg);
                }

            } catch (IOException e) {
                emit(MessageFactory.system("Server connection lost."));
            } finally {
                handleDisconnect();
            }
        }, "listener");

        heartbeat = new Thread(() -> {
            try {
                while (running.get()) {
                    Thread.sleep(3000);

                    if (out != null) {
                        out.println(MessageFactory.ping(username).toJson());
                    }
                }
            } catch (InterruptedException ignored) {}
        }, "heartbeat");

        monitor = new Thread(() -> {
            while (running.get()) {

                if (System.currentTimeMillis() - lastPongTime > 15000) {
                    emit(MessageFactory.system("Heartbeat timeout."));
                    handleDisconnect();
                    break;
                }

                sleep(2000);
            }
        }, "monitor");

        listener.start();
        heartbeat.start();
        monitor.start();
    }

    // ==============================
    // DISCONNECT
    // ==============================
    private void handleDisconnect() {
        if (!running.get()) return;

        emit(MessageFactory.system("Reconnecting..."));
        running.set(false);

        cleanupThreads();
        cleanupSocket();

        if (!reconnect()) {
            emit(MessageFactory.system("Disconnected"));
            emit(MessageFactory.error("Could not reconnect."));
            shutdown();
        }
    }

    private boolean reconnect() {
        int attempts = 0;
        int delay = INITIAL_DELAY;

        while (attempts < MAX_RETRIES) {
            try {
                emit(MessageFactory.system("Reconnect attempt " + (attempts + 1)));

                Thread.sleep(delay);

                if (connect()) {
                    running.set(true);
                    startThreads();

                    emit(MessageFactory.system("Reconnected successfully"));
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
    private void cleanupSocket() {
        if (!cleaningUp.compareAndSet(false, true)) return;

        try { if (socket != null) socket.close(); } catch (IOException ignored) {}

        cleaningUp.set(false);
    }

    private void cleanupThreads() {
        interrupt(listener);
        interrupt(heartbeat);
        interrupt(monitor);
    }

    private void interrupt(Thread t) {
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }

    public void shutdown() {
        running.set(false);
        cleanupThreads();
        cleanupSocket();

        emit(MessageFactory.system("Client closed"));
    }

    // ==============================
    // BUILD MESSAGE
    // ==============================
    private Message buildMessage(String input) {

        if (input.startsWith("/msg ")) {
            String[] parts = input.split(" ", 3);
            if (parts.length < 3) {
                emit(MessageFactory.error("Usage: /msg <user> <message>"));
                return null;
            }
            return MessageFactory.privateMsg(username, parts[1], parts[2]);
        }

        if (input.equals("/list"))
            return MessageFactory.command(username, "LIST", null, "");

        if (input.startsWith("/name ")) {
            String newName = input.substring(6).trim();
            if (newName.isEmpty()) {
                emit(MessageFactory.error("Username cannot be empty."));
                return null;
            }
            username = newName;
            return MessageFactory.command(username, "NAME", null, newName);
        }

        if (input.equals("/exit"))
            return MessageFactory.command(username, "EXIT", null, "");

        if (input.startsWith("/join ")) {
            String room = input.substring(6).trim();
            if (room.isEmpty()) {
                emit(MessageFactory.error("Usage: /join <room>"));
                return null;
            }
            return MessageFactory.command(username, "JOIN", null, room);
        }

        if (input.equals("/leave"))
            return MessageFactory.command(username, "LEAVE", null, "");

        if (input.equals("/rooms"))
            return MessageFactory.command(username, "ROOMS", null, "");

        return MessageFactory.chat(username, input);
    }

    // ==============================
    // DISPLAY
    // ==============================
    private void displayMessage(Message msg) {

    // ❌ Ignore own messages from server
    if (msg.getType() == MessageType.CHAT &&
        msg.getSender() != null &&
        msg.getSender().equals(username)) {
        return;
    }

    emit(msg);
}

    // ==============================
    // EMIT
    // ==============================
    private void emit(Message message) {
        if (guiListener != null) {
            guiListener.onMessage(message);
        } else {
            System.out.println(message);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }
}