package integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class BaseTest {

    protected static Process server;

    @BeforeAll
    static void startServer() throws Exception {
        server = new ProcessBuilder("java", "-cp", "target/classes", "server.Server")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();

        waitForPort("localhost", 5000, 5000);
    }

    @AfterAll
    static void stopServer() throws Exception {
        if (server != null) {
            server.destroy();
            if (!server.waitFor(3, TimeUnit.SECONDS)) {
                server.destroyForcibly();
            }
        }
    }

    private static void waitForPort(String host, int port, long timeoutMs) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 250);
                return;
            } catch (IOException ignored) {
                Thread.sleep(100);
            }
        }

        throw new IOException("Server did not start on " + host + ":" + port);
    }
}