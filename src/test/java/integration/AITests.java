package integration;

import common.MessageFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.*;

public class AITests extends BaseTest {

    private boolean aiBackendAvailable() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 8000), 500);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void testToxicMessageIsBlocked() throws Exception {
        Assumptions.assumeTrue(aiBackendAvailable(), "AI backend is not running");

        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.send(MessageFactory.chat("Alice", "you are stupid"));

            String response = alice.waitForContains("blocked", 5000);
            assertNotNull(response);
        }
    }

    @Test
    void testSummarizeReturnsSummary() throws Exception {
        Assumptions.assumeTrue(aiBackendAvailable(), "AI backend is not running");

        try (TestClient alice = new TestClient("Alice")) {
            alice.drain(400);

            alice.send(MessageFactory.chat("Alice", "hello"));
            alice.send(MessageFactory.chat("Alice", "how are you"));
            alice.send(MessageFactory.chat("Alice", "this is the third message"));

            alice.waitForContains("hello", 2000);
            alice.waitForContains("how are you", 2000);
            alice.waitForContains("third message", 2000);

            alice.send(MessageFactory.command("Alice", "SUMMARIZE", null, "3"));

            String response = alice.waitForContains("Summary", 6000);
            assertNotNull(response);
        }
    }
}