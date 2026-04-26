package integration;

import common.Message;
import common.MessageType;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class TestClient implements AutoCloseable {

    private final Socket socket;
    private final BufferedReader in;
    private final java.io.PrintWriter out;

    public TestClient(String username) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress("localhost", 5000), 3000);
        socket.setSoTimeout(250);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new java.io.PrintWriter(
                new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)),
                true
        );

        // Initial handshake expected by your server
        out.println(new Message(MessageType.CHAT, username, null, "INIT", null).toJson());
        out.flush();
    }

    public void send(Message msg) {
        out.println(msg.toJson());
        out.flush();
    }

    public void sendRaw(String json) {
        out.println(json);
        out.flush();
    }

    public String waitForContains(String needle, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            String line = readLineOnce();
            if (line == null) continue;
            if (line.contains(needle)) return line;
        }

        return null;
    }

    public Message waitForType(MessageType type, long timeoutMs) throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            String line = readLineOnce();
            if (line == null) continue;

            Message msg = Message.fromJson(line);
            if (msg != null && msg.getType() == type) {
                return msg;
            }
        }

        return null;
    }

    public boolean awaitDisconnect(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try {
                String line = in.readLine();
                if (line == null) {
                    return true; // EOF means socket closed
                }
            } catch (IOException e) {
                return true; // read failed because connection dropped
            }
        }

        return false;
    }

    public void drain(long quietMs) throws IOException {
        long deadline = System.currentTimeMillis() + quietMs;

        while (System.currentTimeMillis() < deadline) {
            readLineOnce();
        }
    }

    private String readLineOnce() throws IOException {
        try {
            return in.readLine();
        } catch (SocketTimeoutException e) {
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        try {
            socket.close();
        } catch (Exception ignored) {
        }
    }
}